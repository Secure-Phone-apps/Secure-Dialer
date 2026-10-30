/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.DisconnectCause
import android.telecom.InCallService
import android.telephony.PhoneNumberUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.example.data.AppDatabase
import com.example.model.AppSetting
import com.example.model.CallRecording
import com.example.ui.components.getCurrentLocale
import com.example.util.CallAudioHelper
import com.example.util.CallAudioRecorder
import com.example.util.DynamicIslandOverlayManager
import com.example.util.FlashLightManager
import com.example.util.PhoneFormatHelper
import com.example.util.RecordingFeedbackHelper
import com.example.util.RichHapticEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

class MyInCallService : InCallService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val nm by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    private var screenWakeLock: PowerManager.WakeLock? = null
    private var cpuWakeLock: PowerManager.WakeLock? = null
    private var isForegroundServiceRunning = false
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null
    private var callWaitingToneGenerator: ToneGenerator? = null
    private var isScreenOffReceiverRegistered = false
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                silence()
            }
        }
    }

    private fun registerScreenOffReceiver() {
        if (!isScreenOffReceiverRegistered) {
            try {
                registerReceiver(screenOffReceiver, IntentFilter(Intent.ACTION_SCREEN_OFF))
                isScreenOffReceiverRegistered = true
            } catch (_: Exception) {}
        }
    }

    private fun unregisterScreenOffReceiver() {
        if (isScreenOffReceiverRegistered) {
            try {
                unregisterReceiver(screenOffReceiver)
            } catch (_: Exception) {}
            isScreenOffReceiverRegistered = false
        }
    }

    @Synchronized
    private fun playCallWaitingTone() {
        try {
            if (callWaitingToneGenerator == null) {
                callWaitingToneGenerator = ToneGenerator(AudioManager.STREAM_VOICE_CALL, 75)
            }
            callWaitingToneGenerator?.startTone(ToneGenerator.TONE_SUP_CALL_WAITING, 400)
            RichHapticEngine.performHaptic(this, RichHapticEngine.HapticStyle.DOUBLE_TICK)
        } catch (_: Exception) {}
    }

    @Synchronized
    private fun stopCallWaitingTone() {
        try {
            callWaitingToneGenerator?.stopTone()
            callWaitingToneGenerator?.release()
            callWaitingToneGenerator = null
        } catch (_: Exception) {}
    }

    @Synchronized
    private fun startRinging() {
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
            if (ringerMode == AudioManager.RINGER_MODE_SILENT) return

            // 1. Native AOSP ringtone player
            if (ringerMode == AudioManager.RINGER_MODE_NORMAL) {
                if (ringtone == null) {
                    val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_RINGTONE)
                        ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
                    ringtone = RingtoneManager.getRingtone(this, uri)?.apply {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            audioAttributes = AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                .setLegacyStreamType(AudioManager.STREAM_RING)
                                .build()
                        } else {
                            @Suppress("DEPRECATION")
                            streamType = AudioManager.STREAM_RING
                        }
                    }
                }
                if (ringtone?.isPlaying == false) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        ringtone?.isLooping = true
                    }
                    ringtone?.play()
                }
            }

            // 2. Native hardware vibration
            if (ringerMode == AudioManager.RINGER_MODE_NORMAL || ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
                if (vibrator == null) {
                    vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                    }
                }
                val pattern = longArrayOf(0, 1000, 1000, 1000)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val effect = VibrationEffect.createWaveform(pattern, 0)
                    val audioAttrs = AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setLegacyStreamType(AudioManager.STREAM_RING)
                        .build()
                    vibrator?.vibrate(effect, audioAttrs)
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            }
        } catch (_: Exception) {}
    }

    @Synchronized
    fun silence() {
        stopRinging()
        FlashLightManager.stopFlashing(this)
        unregisterScreenOffReceiver()
    }

    override fun onSilenceRinger() {
        super.onSilenceRinger()
        silence()
    }

    @Synchronized
    private fun stopRinging() {
        try {
            if (ringtone?.isPlaying == true) {
                ringtone?.stop()
            }
            ringtone = null
        } catch (_: Exception) {}

        try {
            vibrator?.cancel()
            vibrator = null
        } catch (_: Exception) {}

        stopCallWaitingTone()
    }

    @Synchronized
    private fun acquireScreenWakeLock() {
        try {
            if (screenWakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                @Suppress("DEPRECATION")
                val flags = PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                        PowerManager.ACQUIRE_CAUSES_WAKEUP or
                        PowerManager.ON_AFTER_RELEASE
                screenWakeLock = pm?.newWakeLock(flags, "SecureDialer:InCallScreenWakeLock")?.apply {
                    setReferenceCounted(false)
                }
            }
            screenWakeLock?.acquire(30_000L)
        } catch (_: Exception) {}
    }

    @Synchronized
    private fun acquireCpuWakeLock() {
        try {
            if (cpuWakeLock == null) {
                val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                cpuWakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SecureDialer:ActiveCallCpuWakeLock")?.apply {
                    setReferenceCounted(false)
                }
            }
            if (cpuWakeLock?.isHeld != true) {
                cpuWakeLock?.acquire()
            }
        } catch (_: Exception) {}
    }

    @Synchronized
    private fun releaseAllWakeLocks() {
        try {
            if (screenWakeLock?.isHeld == true) {
                screenWakeLock?.release()
            }
        } catch (_: Exception) {}
        try {
            if (cpuWakeLock?.isHeld == true) {
                cpuWakeLock?.release()
            }
        } catch (_: Exception) {}
    }

    @Synchronized
    private fun startInCallForeground(notification: Notification) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val isRecording = CallAudioRecorder.isRecording.value
                val fgsType = if (isRecording) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                }
                startForeground(NOTIFICATION_ID_ACTIVE, notification, fgsType)
            } else {
                startForeground(NOTIFICATION_ID_ACTIVE, notification)
            }
            isForegroundServiceRunning = true
        } catch (_: Exception) {
            nm.notify(NOTIFICATION_ID_ACTIVE, notification)
        }
    }

    @Synchronized
    private fun stopInCallForeground() {
        try {
            if (isForegroundServiceRunning) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                isForegroundServiceRunning = false
            }
        } catch (_: Exception) {}
    }

    override fun onCreate() {
        super.onCreate()
        initNotificationChannels()
    }

    override fun onDestroy() {
        unregisterScreenOffReceiver()
        stopRinging()
        releaseAllWakeLocks()
        stopInCallForeground()
        DynamicIslandOverlayManager.stopCallMonitoring()
        FlashLightManager.stopFlashing(this)
        nm.cancel(NOTIFICATION_ID_INCOMING)
        nm.cancel(NOTIFICATION_ID_ACTIVE)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HANG_UP, ACTION_DECLINE -> {
                stopRinging()
                CallManager.disconnect()
                releaseAllWakeLocks()
                stopInCallForeground()
                nm.cancel(NOTIFICATION_ID_INCOMING)
                nm.cancel(NOTIFICATION_ID_ACTIVE)
            }
            ACTION_DECLINE_WAITING -> {
                stopCallWaitingTone()
                CallManager.rejectWaitingCall()
                nm.cancel(NOTIFICATION_ID_INCOMING)
            }
            ACTION_ANSWER_WAITING -> {
                stopCallWaitingTone()
                CallManager.answerWaitingCall(holdCurrent = true)
                nm.cancel(NOTIFICATION_ID_INCOMING)
                launchCallScreen(answerOnLaunch = false)
            }
            ACTION_ANSWER -> {
                stopRinging()
                CallManager.answer()
                launchCallScreen(answerOnLaunch = true)
            }
            ACTION_TOGGLE_MUTE -> {
                val isMuted = CallManager.audioState.value?.isMuted ?: false
                CallManager.setMuted(!isMuted)
                val activeCall = CallManager.currentCall.value
                if (activeCall != null && activeCall.state in listOf(Call.STATE_ACTIVE, Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_HOLDING)) {
                    showActiveCallNotification(activeCall)
                } else {
                    nm.cancel(NOTIFICATION_ID_ACTIVE)
                }
            }
            ACTION_TOGGLE_SPEAKER -> {
                val isSpeaker = (CallManager.audioState.value?.route ?: CallAudioState.ROUTE_EARPIECE) == CallAudioState.ROUTE_SPEAKER
                CallManager.setSpeaker(!isSpeaker)
                val activeCall = CallManager.currentCall.value
                if (activeCall != null && activeCall.state in listOf(Call.STATE_ACTIVE, Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_HOLDING)) {
                    showActiveCallNotification(activeCall)
                } else {
                    nm.cancel(NOTIFICATION_ID_ACTIVE)
                }
            }
            ACTION_TOGGLE_RECORD -> toggleCallRecordingFromNotification()
        }
        return START_STICKY
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        acquireScreenWakeLock()
        acquireCpuWakeLock()
        CallManager.inCallService = this
        CallManager.addCall(call)
        val num = call.details?.handle?.schemeSpecificPart ?: ""
        val resolvedName = resolveCallerDisplayName(call, num)
        if (resolvedName.isNotBlank() && resolvedName != num && resolvedName != "Unknown") {
            CallManager.setCallerName(resolvedName)
        }
        DynamicIslandOverlayManager.startCallMonitoring(this)

        persistCnapIfPresent(call)
        handleCallState(call)

        call.registerCallback(object : Call.Callback() {
            private var wasRinging = (call.state == Call.STATE_RINGING)

            override fun onStateChanged(c: Call, state: Int) {
                super.onStateChanged(c, state)
                if (state == Call.STATE_RINGING) wasRinging = true

                if (state == Call.STATE_ACTIVE || state == Call.STATE_DISCONNECTED) {
                    unregisterScreenOffReceiver()
                    stopRinging()
                    nm.cancel(NOTIFICATION_ID_INCOMING)
                    FlashLightManager.stopFlashing(this@MyInCallService)
                }

                if (state in listOf(Call.STATE_ACTIVE, Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_HOLDING)) {
                    stopRinging()
                    stopCallWaitingTone()
                    if (CallManager.waitingCall.value == c) {
                        CallManager.updateWaitingCall(null)
                    }
                    showActiveCallNotification(c)
                }

                if (state == Call.STATE_DISCONNECTED) {
                    stopRinging()
                    val remaining = CallManager.calls.value.filter { it != c && it.state != Call.STATE_DISCONNECTED }
                    if (remaining.isEmpty()) {
                        releaseAllWakeLocks()
                        stopInCallForeground()
                        nm.cancel(NOTIFICATION_ID_ACTIVE)
                    }
                    if (wasRinging) {
                        val cause = c.details?.disconnectCause?.code
                        if (cause != DisconnectCause.REJECTED && cause != DisconnectCause.LOCAL) {
                            showMissedCallNotification(c)
                        }
                    }
                    if (CallManager.waitingCall.value == c) {
                        CallManager.updateWaitingCall(null)
                    }
                    c.unregisterCallback(this)
                }

                if (state == Call.STATE_ACTIVE) wasRinging = false
            }
        })
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        stopRinging()
        CallManager.removeCall(call)
        FlashLightManager.stopFlashing(this)
        nm.cancel(NOTIFICATION_ID_INCOMING)

        val remainingActiveCalls = CallManager.calls.value.filter { it.state != Call.STATE_DISCONNECTED }
        if (remainingActiveCalls.isEmpty()) {
            releaseAllWakeLocks()
            stopInCallForeground()
            DynamicIslandOverlayManager.stopCallMonitoring()
            nm.cancel(NOTIFICATION_ID_ACTIVE)

            if (CallAudioRecorder.isRecording.value) {
                try {
                    CallAudioRecorder.stopRecording()
                    CallAudioHelper.restoreAudioState(this, this)
                } catch (_: Exception) {}
            }
        } else {
            val nextCall = remainingActiveCalls.firstOrNull()
            if (nextCall != null) {
                showActiveCallNotification(nextCall)
            } else {
                nm.cancel(NOTIFICATION_ID_ACTIVE)
            }
        }
    }

    private var lastAudioRoute = CallAudioState.ROUTE_EARPIECE

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        val currentRoute = audioState?.route ?: CallAudioState.ROUTE_EARPIECE
        if (lastAudioRoute == CallAudioState.ROUTE_BLUETOOTH && currentRoute != CallAudioState.ROUTE_BLUETOOTH) {
            // Defensive fallback retry to ROUTE_EARPIECE upon Bluetooth disconnect events
            if (currentRoute != CallAudioState.ROUTE_SPEAKER && currentRoute != CallAudioState.ROUTE_WIRED_HEADSET) {
                try {
                    setAudioRoute(CallAudioState.ROUTE_EARPIECE)
                } catch (_: Exception) {}
                serviceScope.launch {
                    delay(250)
                    try {
                        val stateNow = CallManager.audioState.value
                        if (stateNow != null && stateNow.route != CallAudioState.ROUTE_SPEAKER &&
                            stateNow.route != CallAudioState.ROUTE_WIRED_HEADSET &&
                            stateNow.route != CallAudioState.ROUTE_EARPIECE) {
                            setAudioRoute(CallAudioState.ROUTE_EARPIECE)
                        }
                    } catch (_: Exception) {}
                }
            }
        }
        lastAudioRoute = currentRoute

        CallManager.updateAudioState(audioState)
        val activeCall = CallManager.currentCall.value
        if (activeCall != null && activeCall.state in listOf(Call.STATE_ACTIVE, Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_HOLDING)) {
            showActiveCallNotification(activeCall)
        } else {
            nm.cancel(NOTIFICATION_ID_ACTIVE)
        }
    }

    private fun handleCallState(call: Call) {
        when (call.state) {
            Call.STATE_RINGING -> {
                registerScreenOffReceiver()
                val activeCalls = CallManager.calls.value.filter {
                    it != call && it.state in listOf(Call.STATE_ACTIVE, Call.STATE_HOLDING, Call.STATE_DIALING, Call.STATE_CONNECTING)
                }
                val current = CallManager.currentCall.value
                val isCurrentlyOnCall = activeCalls.isNotEmpty() || (current != null && current != call && current.state in listOf(Call.STATE_ACTIVE, Call.STATE_HOLDING, Call.STATE_DIALING, Call.STATE_CONNECTING))

                val isSameCallerAsActive = activeCalls.any {
                    PhoneFormatHelper.isSameCallOrCaller(call, it, CallManager.callerNumber.value, CallManager.callerName.value, this)
                } || (current != null && current != call && PhoneFormatHelper.isSameCallOrCaller(call, current, CallManager.callerNumber.value, CallManager.callerName.value, this))

                if (isCurrentlyOnCall) {
                    if (isSameCallerAsActive) {
                        // SAME CALLER as active call: NEVER present call waiting!
                        CallManager.updateWaitingCall(null)
                        stopCallWaitingTone()
                    } else {
                        // DIFFERENT CALLER: Genuine call waiting alert
                        CallManager.updateWaitingCall(call)
                        playCallWaitingTone()
                        showIncomingCallNotification(call, isCallWaiting = true)
                    }
                } else {
                    FlashLightManager.startFlashing(this)
                    startRinging()
                    showIncomingCallNotification(call, isCallWaiting = false)

                    // INDUSTRY-STANDARD INCOMING CALL PRESENTATION (Google Phone / Samsung One UI / iOS):
                    // Determine if the user is actively interacting with the device or if it is sleeping/locked.
                    val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                    val isScreenInteractive = pm?.isInteractive ?: false
                    val isKeyguardLocked = km?.isKeyguardLocked ?: false
                    val isDialerInForeground = CallManager.isAppInForeground.value

                    // If device screen is OFF, or device is LOCKED on keyguard, or user is already inside Secure Dialer:
                    // Present full-screen incoming call UI immediately.
                    // If user is actively using ANOTHER application (screen is ON & UNLOCKED):
                    // Do NOT steal screen focus! The system Heads-Up Notification (HUN) + Dynamic Island will handle it non-intrusively.
                    if (!isScreenInteractive || isKeyguardLocked || isDialerInForeground) {
                        launchCallScreen(answerOnLaunch = false)
                    }
                }
            }
            Call.STATE_ACTIVE, Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_HOLDING -> {
                unregisterScreenOffReceiver()
                stopRinging()
                stopCallWaitingTone()
                if (CallManager.waitingCall.value == call) {
                    CallManager.updateWaitingCall(null)
                }
                FlashLightManager.stopFlashing(this)
                showActiveCallNotification(call)

                val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                val isKeyguardLocked = km?.isKeyguardLocked ?: false
                val isDialerInForeground = CallManager.isAppInForeground.value
                if (isKeyguardLocked || isDialerInForeground) {
                    launchCallScreen(answerOnLaunch = false)
                }
            }
        }
    }

    private fun showIncomingCallNotification(call: Call, isCallWaiting: Boolean = false) {
        val number = call.details?.handle?.schemeSpecificPart ?: ""
        val displayName = resolveCallerDisplayName(call, number)

        val contentIntent = PendingIntent.getActivity(
            this, 101,
            Intent(this, MainActivity::class.java).apply {
                action = ACTION_INCOMING_CALL
                setPackage(packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(EXTRA_SHOW_CALL, true)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val declinePendingIntent = if (isCallWaiting) {
            PendingIntent.getService(
                this, 104,
                Intent(this, MyInCallService::class.java).apply {
                    setPackage(packageName)
                    action = ACTION_DECLINE_WAITING
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getService(
                this, 102,
                Intent(this, MyInCallService::class.java).apply {
                    setPackage(packageName)
                    action = ACTION_DECLINE
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val answerPendingIntent = if (isCallWaiting) {
            PendingIntent.getService(
                this, 105,
                Intent(this, MyInCallService::class.java).apply {
                    setPackage(packageName)
                    action = ACTION_ANSWER_WAITING
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        } else {
            PendingIntent.getActivity(
                this, 103,
                Intent(this, MainActivity::class.java).apply {
                    action = ACTION_ANSWER
                    setPackage(packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                            Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                    putExtra(EXTRA_SHOW_CALL, true)
                    putExtra(EXTRA_ANSWER, true)
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val notificationBuilder = if (isCallWaiting) {
            val title = getString(R.string.call_waiting_title)
            val text = "$displayName\n${getString(R.string.call_waiting_notice)}"
            NotificationCompat.Builder(this, CHANNEL_INCOMING)
                .setSmallIcon(android.R.drawable.sym_call_incoming)
                .setContentTitle(title)
                .setContentText("Incoming call from $displayName")
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(contentIntent)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", declinePendingIntent)
                .addAction(android.R.drawable.sym_action_call, "Answer", answerPendingIntent)
        } else {
            val caller = Person.Builder().setName(displayName).setKey(number).build()
            val style = NotificationCompat.CallStyle.forIncomingCall(caller, declinePendingIntent, answerPendingIntent)
            NotificationCompat.Builder(this, CHANNEL_INCOMING)
                .setSmallIcon(android.R.drawable.sym_call_incoming)
                .setStyle(style)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setOngoing(true)
                .setAutoCancel(false)
                .setContentIntent(contentIntent)
                .also {
                    it.setFullScreenIntent(contentIntent, true)
                }
        }

        nm.notify(NOTIFICATION_ID_INCOMING, notificationBuilder.build())
    }

    private fun showActiveCallNotification(call: Call) {
        if (call.state == Call.STATE_DISCONNECTED || CallManager.calls.value.none { it == call && it.state != Call.STATE_DISCONNECTED }) {
            nm.cancel(NOTIFICATION_ID_ACTIVE)
            return
        }
        val number = call.details?.handle?.schemeSpecificPart ?: ""
        val displayName = resolveCallerDisplayName(call, number)

        val contentPendingIntent = PendingIntent.getActivity(
            this, 200,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra(EXTRA_SHOW_CALL, true)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val hangUpPendingIntent = PendingIntent.getService(
            this, 201,
            Intent(this, MyInCallService::class.java).apply { action = ACTION_HANG_UP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val recordPendingIntent = PendingIntent.getService(
            this, 202,
            Intent(this, MyInCallService::class.java).apply { action = ACTION_TOGGLE_RECORD },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val speakerPendingIntent = PendingIntent.getService(
            this, 203,
            Intent(this, MyInCallService::class.java).apply { action = ACTION_TOGGLE_SPEAKER },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val mutePendingIntent = PendingIntent.getService(
            this, 204,
            Intent(this, MyInCallService::class.java).apply { action = ACTION_TOGGLE_MUTE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val isMuted = CallManager.audioState.value?.isMuted ?: false
        val isSpeaker = (CallManager.audioState.value?.route ?: CallAudioState.ROUTE_EARPIECE) == CallAudioState.ROUTE_SPEAKER
        val isRecording = CallAudioRecorder.isRecording.value
        val isConference = CallManager.isConference(call) || CallManager.isConferenceActive.value

        val recordTitle = if (isRecording) "■ Stop Rec" else "● Record"
        val speakerTitle = if (isSpeaker) "Earpiece" else "Speaker"
        val muteTitle = if (isMuted) "Unmute" else "Mute"

        val titlePrefix = if (isConference) getString(R.string.call_status_conference) else "Ongoing Call"
        val contentTitle = if (isRecording) "$titlePrefix • [REC]" else titlePrefix
        val contentText = if (isConference) "Conference call with $displayName is active" else "Call with $displayName is active"

        val notification = NotificationCompat.Builder(this, CHANNEL_ACTIVE)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(contentTitle)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_btn_speak_now, recordTitle, recordPendingIntent)
            .addAction(android.R.drawable.stat_notify_call_mute, muteTitle, mutePendingIntent)
            .addAction(android.R.drawable.stat_sys_speakerphone, speakerTitle, speakerPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hang Up", hangUpPendingIntent)
            .build()

        startInCallForeground(notification)
    }

    private fun showMissedCallNotification(call: Call) {
        val number = call.details?.handle?.schemeSpecificPart ?: "Unknown"
        val displayName = resolveCallerDisplayName(call, number)

        val contentPendingIntent = PendingIntent.getActivity(
            this, 301,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("SHOW_CALL_LOG", true)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val callBackPendingIntent = PendingIntent.getActivity(
            this, 302,
            Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$number")
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_MISSED)
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentTitle(getString(R.string.call_type_missed))
            .setContentText("${getString(R.string.call_type_missed)}: $displayName")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.sym_action_call, getString(R.string.btn_call_back), callBackPendingIntent)
            .build()

        nm.notify(NOTIFICATION_ID_MISSED, notification)
    }

    private fun toggleCallRecordingFromNotification() {
        val isRec = CallAudioRecorder.isRecording.value
        val prefs = getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        val chimeEnabled = prefs.getBoolean("recording_chime_enabled", false)
        val autoTune = prefs.getBoolean("auto_tune_recording_volume", true)

        if (isRec) {
            RecordingFeedbackHelper.triggerRecordingStopFeedback(this, chimeEnabled)
            val result = CallAudioRecorder.stopRecording()
            if (autoTune) {
                CallAudioHelper.restoreAudioState(this, this)
            }
            val file = result.file
            if (file != null && file.exists() && file.length() > 0L) {
                val durationSec = result.durationSeconds.coerceAtLeast(1L)
                val number = CallManager.callerNumber.value.ifEmpty { "Unknown" }
                val name = CallManager.callerName.value.ifEmpty { number }
                val locale = getCurrentLocale(this)
                val timestamp = SimpleDateFormat("MMM d, HH:mm", locale).format(Date())

                val recording = CallRecording(
                    number = number,
                    name = name,
                    timestamp = timestamp,
                    duration = durationSec,
                    filePath = file.absolutePath
                )

                serviceScope.launch {
                    try {
                        AppDatabase.getDatabase(this@MyInCallService).dialerDao().insertCallRecording(recording)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        } else {
            RecordingFeedbackHelper.triggerRecordingStartFeedback(this, chimeEnabled)
            if (autoTune) {
                CallAudioHelper.prepareSpeakerForRecording(this, this, CallManager.audioState.value)
            }
            val number = CallManager.callerNumber.value.ifEmpty { "Unknown" }
            CallAudioRecorder.startRecording(this, number)
        }
        CallManager.currentCall.value?.let { showActiveCallNotification(it) }
    }

    private fun resolveCallerDisplayName(call: Call, number: String): String {
        if (number.isEmpty()) return "Unknown"
        val cnapName = call.details?.callerDisplayName
        val contactName = getContactNameFromNumber(this, number)
        val savedCnap = if (contactName == null && cnapName.isNullOrBlank()) {
            getSavedCnapNameSync(this, number)
        } else null

        return when {
            !contactName.isNullOrBlank() -> contactName
            !cnapName.isNullOrBlank() -> cnapName
            !savedCnap.isNullOrBlank() -> savedCnap
            else -> number
        }
    }

    private fun persistCnapIfPresent(call: Call) {
        val number = call.details?.handle?.schemeSpecificPart ?: return
        val cnapName = call.details?.callerDisplayName ?: return
        if (cnapName.isNotBlank() && number.isNotBlank()) {
            ContactCache.putCnapName(number, cnapName)
            serviceScope.launch {
                try {
                    AppDatabase.getDatabase(this@MyInCallService)
                        .dialerDao()
                        .insertSetting(AppSetting("cnap_" + number.filter { it.isDigit() }, cnapName))
                } catch (_: Exception) {}
            }
        }
    }

    private fun launchCallScreen(answerOnLaunch: Boolean) {
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                action = if (answerOnLaunch) ACTION_ANSWER else ACTION_INCOMING_CALL
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                putExtra(EXTRA_SHOW_CALL, true)
                if (answerOnLaunch) putExtra(EXTRA_ANSWER, true)
            }
            startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun initNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try { nm.deleteNotificationChannel("incoming_call_channel_v2") } catch (_: Exception) {}
            try { nm.deleteNotificationChannel("incoming_call_channel_v3") } catch (_: Exception) {}

            val incomingChannel = NotificationChannel(CHANNEL_INCOMING, "Incoming Calls", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Incoming call alerts and full screen notifications"
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }

            nm.createNotificationChannels(listOf(
                incomingChannel,
                NotificationChannel(CHANNEL_ACTIVE, "Active Calls", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Ongoing call controls"
                    setShowBadge(false)
                },
                NotificationChannel(CHANNEL_MISSED, "Missed Calls", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Missed call notifications"
                }
            ))
        }
    }

    companion object {
        const val CHANNEL_INCOMING = "incoming_call_channel_v4"
        const val CHANNEL_ACTIVE = "active_call_channel"
        const val CHANNEL_MISSED = "missed_call_channel"

        const val NOTIFICATION_ID_INCOMING = 1
        const val NOTIFICATION_ID_MISSED = 2
        const val NOTIFICATION_ID_ACTIVE = 3

        const val ACTION_HANG_UP = "com.example.ACTION_HANG_UP"
        const val ACTION_ANSWER = "com.example.ACTION_ANSWER"
        const val ACTION_DECLINE = "com.example.ACTION_DECLINE"
        const val ACTION_DECLINE_WAITING = "com.example.ACTION_DECLINE_WAITING"
        const val ACTION_ANSWER_WAITING = "com.example.ACTION_ANSWER_WAITING"
        const val ACTION_TOGGLE_RECORD = "com.example.ACTION_TOGGLE_RECORD"
        const val ACTION_TOGGLE_SPEAKER = "com.example.ACTION_TOGGLE_SPEAKER"
        const val ACTION_TOGGLE_MUTE = "com.example.ACTION_TOGGLE_MUTE"
        const val ACTION_INCOMING_CALL = "com.example.INCOMING_CALL"

        const val EXTRA_SHOW_CALL = "SHOW_CALL_SCREEN"
        const val EXTRA_ANSWER = "ANSWER_ON_LAUNCH"
    }
}