/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.Manifest
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.DisconnectCause
import android.telecom.InCallService
import android.telephony.PhoneNumberUtils
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import com.example.data.AppDatabase
import com.example.model.AppSetting
import com.example.model.CallRecording
import com.example.ui.components.getCurrentLocale
import com.example.util.BlockedNumberContractManager
import com.example.util.CallAudioHelper
import com.example.util.CallAudioRecorder
import com.example.util.CallAudioRouteCoordinator
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
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.text.SimpleDateFormat
import java.util.Collections
import java.util.Date
import java.util.IdentityHashMap

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
    private var isAudioMonitorsRegistered = false
    private val serviceCallbacks = IdentityHashMap<Call, Call.Callback>()
    private val blockedRejectedCalls: MutableSet<Call> =
        Collections.synchronizedSet(Collections.newSetFromMap(IdentityHashMap()))
    private val mainHandler = Handler(Looper.getMainLooper())
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_OFF) {
                silence()
            }
        }
    }

    private val scoStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED) {
                val state = intent.getIntExtra(
                    AudioManager.EXTRA_SCO_AUDIO_STATE,
                    AudioManager.SCO_AUDIO_STATE_DISCONNECTED
                )
                val previousState = intent.getIntExtra(
                    AudioManager.EXTRA_SCO_AUDIO_PREVIOUS_STATE,
                    AudioManager.SCO_AUDIO_STATE_DISCONNECTED
                )
                if (state == AudioManager.SCO_AUDIO_STATE_DISCONNECTED &&
                    previousState == AudioManager.SCO_AUDIO_STATE_CONNECTED
                ) {
                    handleHardwareAudioDisconnect(isBluetooth = true)
                }
            }
        }
    }

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            val removed = removedDevices ?: return
            val lostBt = removed.any { CallAudioRouteCoordinator.isCommunicationBluetoothDevice(it.type) }
            val lostWired = removed.any { CallAudioRouteCoordinator.isWiredHeadsetDevice(it.type) }
            if (lostBt) {
                handleHardwareAudioDisconnect(isBluetooth = true)
            } else if (lostWired) {
                handleHardwareAudioDisconnect(isBluetooth = false)
            }
        }
    }

    private fun handleHardwareAudioDisconnect(isBluetooth: Boolean) {
        if (CallManager.calls.value.none { it.state != Call.STATE_DISCONNECTED }) return
        if (isBluetooth) {
            CallAudioRouteCoordinator.clearStaleBluetoothHal(this)
        }
        val state = CallManager.audioState.value ?: return
        val expectedRoute = if (isBluetooth) CallAudioState.ROUTE_BLUETOOTH else CallAudioState.ROUTE_WIRED_HEADSET
        if (state.route != expectedRoute) return

        val fallback = CallAudioRouteCoordinator.selectScoDropRecoveryRoute(
            currentRoute = state.route,
            supportedRouteMask = state.supportedRouteMask
        ) ?: return

        serviceScope.launch {
            delay(200)
            val nowState = CallManager.audioState.value
            val hasLiveCall = CallManager.calls.value.any { it.state != Call.STATE_DISCONNECTED }
            if (hasLiveCall && nowState != null && nowState.route == expectedRoute) {
                withContext(Dispatchers.Main) {
                    CallAudioRouteCoordinator.routeAudio(
                        inCallService = this@MyInCallService,
                        targetRoute = fallback,
                        availableEndpoints = CallManager.getAvailableEndpointsSnapshot(),
                        isUserAction = false
                    )
                }
            }
        }
    }

    private fun registerActiveCallAudioMonitors() {
        if (isAudioMonitorsRegistered) return
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am?.registerAudioDeviceCallback(audioDeviceCallback, mainHandler)
            val filter = IntentFilter(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(scoStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(scoStateReceiver, filter)
            }
            isAudioMonitorsRegistered = true
        } catch (_: Exception) {}
    }

    private fun unregisterActiveCallAudioMonitors() {
        if (!isAudioMonitorsRegistered) return
        try {
            val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            am?.unregisterAudioDeviceCallback(audioDeviceCallback)
        } catch (_: Exception) {}
        try {
            unregisterReceiver(scoStateReceiver)
        } catch (_: Exception) {}
        isAudioMonitorsRegistered = false
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
                val hasMicPermission = ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                val fgsType = if (isRecording && hasMicPermission) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                }
                try {
                    startForeground(NOTIFICATION_ID_ACTIVE, notification, fgsType)
                } catch (_: SecurityException) {
                    startForeground(
                        NOTIFICATION_ID_ACTIVE,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_PHONE_CALL
                    )
                }
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
        unregisterActiveCallAudioMonitors()
        stopRinging()
        stopCallWaitingTone()
        releaseAllWakeLocks()
        stopInCallForeground()
        DynamicIslandOverlayManager.stopCallMonitoring()
        FlashLightManager.stopFlashing(this)
        nm.cancel(NOTIFICATION_ID_INCOMING)
        nm.cancel(NOTIFICATION_ID_ACTIVE)
        CallManager.onInCallServiceUnbound(this)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        unregisterScreenOffReceiver()
        unregisterActiveCallAudioMonitors()
        stopRinging()
        stopCallWaitingTone()
        releaseAllWakeLocks()
        stopInCallForeground()
        DynamicIslandOverlayManager.stopCallMonitoring()
        FlashLightManager.stopFlashing(this)
        CallManager.onInCallServiceUnbound(this)
        return super.onUnbind(intent)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HANG_UP, ACTION_DECLINE -> {
                stopRinging()
                FlashLightManager.stopFlashing(this)
                nm.cancel(NOTIFICATION_ID_INCOMING)
                CallManager.disconnect()

                val hasPendingTelecomCalls = try {
                    calls.any { it.state != Call.STATE_DISCONNECTED } ||
                        CallManager.calls.value.any { it.state != Call.STATE_DISCONNECTED }
                } catch (_: Exception) {
                    CallManager.calls.value.any { it.state != Call.STATE_DISCONNECTED }
                }

                if (!hasPendingTelecomCalls) {
                    releaseAllWakeLocks()
                    stopInCallForeground()
                    nm.cancel(NOTIFICATION_ID_ACTIVE)
                } else {
                    serviceScope.launch {
                        delay(6000)
                        val stillAlive = try {
                            calls.any { it.state != Call.STATE_DISCONNECTED }
                        } catch (_: Exception) {
                            false
                        }
                        if (!stillAlive) {
                            releaseAllWakeLocks()
                            stopInCallForeground()
                            nm.cancel(NOTIFICATION_ID_ACTIVE)
                        }
                    }
                }
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
        CallManager.inCallService = this
        CallManager.addCall(call)

        val wasAutoCancelled = CallManager.isPreCancelledCall(call) || CallManager.isDisconnectRequested(call)

        val callback = object : Call.Callback() {
            private var wasRinging = (call.state == Call.STATE_RINGING)

            override fun onStateChanged(c: Call, state: Int) {
                super.onStateChanged(c, state)
                if (state == Call.STATE_RINGING) wasRinging = true

                if (blockedRejectedCalls.contains(c) && state != Call.STATE_DISCONNECTED) {
                    return
                }

                if (CallManager.isPreCancelledCall(c) && state != Call.STATE_DISCONNECTED) {
                    return
                }

                if (state == Call.STATE_ACTIVE || state == Call.STATE_DISCONNECTING || state == Call.STATE_DISCONNECTED) {
                    unregisterScreenOffReceiver()
                    stopRinging()
                    nm.cancel(NOTIFICATION_ID_INCOMING)
                    FlashLightManager.stopFlashing(this@MyInCallService)
                }

                if (state in listOf(
                        Call.STATE_ACTIVE,
                        Call.STATE_DIALING,
                        Call.STATE_CONNECTING,
                        Call.STATE_NEW,
                        Call.STATE_SELECT_PHONE_ACCOUNT,
                        Call.STATE_HOLDING
                    ) && !CallManager.isDisconnectRequested(c)
                ) {
                    stopRinging()
                    stopCallWaitingTone()
                    if (CallManager.waitingCall.value === c) {
                        CallManager.updateWaitingCall(null)
                    }
                    showActiveCallNotification(c)
                }

                if (state == Call.STATE_DISCONNECTED) {
                    stopRinging()
                    val wasBlocked = blockedRejectedCalls.remove(c)
                    val remaining = CallManager.calls.value.filter { it !== c && it.state != Call.STATE_DISCONNECTED }
                    if (remaining.isEmpty()) {
                        releaseAllWakeLocks()
                        stopInCallForeground()
                        nm.cancel(NOTIFICATION_ID_ACTIVE)
                    }
                    if (wasRinging && !wasBlocked && !CallManager.isPreCancelledCall(c)) {
                        val cause = c.details?.disconnectCause?.code
                        if (cause != DisconnectCause.REJECTED && cause != DisconnectCause.LOCAL) {
                            val disconnectedAtMs = System.currentTimeMillis()
                            val callerNum = c.details?.handle?.schemeSpecificPart ?: ""
                            val resolvedName = resolveCallerDisplayName(c, callerNum)
                            serviceScope.launch {
                                delay(450L)
                                val prefs = applicationContext.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
                                val lastTelecomBroadcastMs = prefs.getLong(
                                    com.example.util.MissedCallNotificationReceiver.PREF_LAST_TELECOM_MISSED_BROADCAST_MS,
                                    0L
                                )
                                if (lastTelecomBroadcastMs < disconnectedAtMs - 500L) {
                                    com.example.util.MissedCallNotificationReceiver.postMissedCallNotification(
                                        context = applicationContext,
                                        nm = nm,
                                        count = 1,
                                        rawNumber = callerNum,
                                        displayName = resolvedName
                                    )
                                }
                            }
                        }
                    }
                    if (CallManager.waitingCall.value === c) {
                        CallManager.updateWaitingCall(null)
                    }
                    serviceCallbacks.remove(c)
                    try { c.unregisterCallback(this) } catch (_: Exception) {}
                }

                if (state == Call.STATE_ACTIVE) wasRinging = false
            }
        }
        serviceCallbacks[call] = callback
        call.registerCallback(callback)

        if (wasAutoCancelled) {
            return
        }

        val num = call.details?.handle?.schemeSpecificPart ?: ""
        val isEmergency = num.isNotEmpty() && CallManager.isEmergencyNumber(num, this)

        if (call.state == Call.STATE_RINGING && !isEmergency && num.isNotBlank()) {
            // Pre-presentation blocklist + offline spam screening fallback across Android 9-17 and all OEMs
            serviceScope.launch {
                val shouldBlock = try {
                    withTimeoutOrNull(350L) {
                        val dao = AppDatabase.getDatabase(applicationContext).dialerDao()
                        BlockedNumberContractManager.shouldBlockIncomingCall(applicationContext, num, dao)
                    } ?: false
                } catch (_: Exception) {
                    false
                }

                withContext(Dispatchers.Main) {
                    if (call.state == Call.STATE_DISCONNECTED || CallManager.isPreCancelledCall(call)) {
                        return@withContext
                    }
                    if (shouldBlock) {
                        blockedRejectedCalls.add(call)
                        stopRinging()
                        FlashLightManager.stopFlashing(this@MyInCallService)
                        nm.cancel(NOTIFICATION_ID_INCOMING)
                        try {
                            call.reject(false, null)
                        } catch (_: Exception) {
                            try { call.disconnect() } catch (_: Exception) {}
                        }
                        return@withContext
                    }
                    presentAllowedCall(call, num)
                }
            }
        } else {
            presentAllowedCall(call, num)
        }
    }

    private fun presentAllowedCall(call: Call, num: String) {
        if (call.state == Call.STATE_DISCONNECTED || blockedRejectedCalls.contains(call)) return
        acquireScreenWakeLock()
        acquireCpuWakeLock()
        registerActiveCallAudioMonitors()
        val resolvedName = resolveCallerDisplayName(call, num)
        if (resolvedName.isNotBlank() && resolvedName != num && resolvedName != "Unknown") {
            CallManager.setCallerName(resolvedName)
        }
        DynamicIslandOverlayManager.startCallMonitoring(this)
        persistCnapIfPresent(call)
        handleCallState(call)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        blockedRejectedCalls.remove(call)
        stopRinging()
        serviceCallbacks.remove(call)?.let { cb ->
            try { call.unregisterCallback(cb) } catch (_: Exception) {}
        }
        CallManager.removeCall(call)
        FlashLightManager.stopFlashing(this)
        nm.cancel(NOTIFICATION_ID_INCOMING)

        val remainingActiveCalls = CallManager.calls.value.filter { it.state != Call.STATE_DISCONNECTED }
        if (remainingActiveCalls.isEmpty()) {
            unregisterActiveCallAudioMonitors()
            CallAudioRouteCoordinator.clearStaleBluetoothHal(this)
            CallAudioRouteCoordinator.clearUserIntent()
            lastAudioRoute = CallAudioState.ROUTE_EARPIECE
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
            if (nextCall != null && !CallManager.isPreCancelledCall(nextCall)) {
                showActiveCallNotification(nextCall)
            } else {
                nm.cancel(NOTIFICATION_ID_ACTIVE)
            }
        }
    }

    private var lastAudioRoute = CallAudioState.ROUTE_EARPIECE
    private val cachedApi34Endpoints = ArrayList<Any>()

    override fun onAvailableCallEndpointsChanged(availableEndpoints: MutableList<CallEndpoint>) {
        super.onAvailableCallEndpointsChanged(availableEndpoints)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            cachedApi34Endpoints.clear()
            cachedApi34Endpoints.addAll(availableEndpoints)
            CallManager.updateAvailableEndpoints(cachedApi34Endpoints.toList())
        }
    }

    override fun onCallEndpointChanged(callEndpoint: CallEndpoint) {
        super.onCallEndpointChanged(callEndpoint)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val typedList = cachedApi34Endpoints.filterIsInstance<CallEndpoint>()
            val synthesized = CallAudioRouteCoordinator.Api34EndpointBridge.synthesizeCallAudioState(
                previousState = CallManager.audioState.value,
                currentEndpoint = callEndpoint,
                availableEndpoints = typedList
            )
            processUpdatedCallAudioState(synthesized)
        }
    }

    @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        processUpdatedCallAudioState(audioState)
    }

    private fun processUpdatedCallAudioState(audioState: CallAudioState?) {
        val currentRoute = audioState?.route ?: CallAudioState.ROUTE_EARPIECE
        val supportedMask = audioState?.supportedRouteMask ?: 0

        if (lastAudioRoute == CallAudioState.ROUTE_BLUETOOTH && currentRoute != CallAudioState.ROUTE_BLUETOOTH) {
            CallAudioRouteCoordinator.clearStaleBluetoothHal(this)
        }

        val recoveryRoute = CallAudioRouteCoordinator.selectRecoveryRoute(
            previousRoute = lastAudioRoute,
            currentRoute = currentRoute,
            supportedRouteMask = supportedMask
        )
        if (recoveryRoute != null) {
            CallAudioRouteCoordinator.routeAudio(
                inCallService = this,
                targetRoute = recoveryRoute,
                availableEndpoints = CallManager.getAvailableEndpointsSnapshot(),
                isUserAction = false
            )
            serviceScope.launch {
                delay(250)
                try {
                    val stateNow = CallManager.audioState.value
                    val hasLiveCall = CallManager.calls.value.any { it.state != Call.STATE_DISCONNECTED }
                    if (hasLiveCall && stateNow != null && stateNow.route != recoveryRoute &&
                        stateNow.route != CallAudioState.ROUTE_BLUETOOTH &&
                        stateNow.route != CallAudioState.ROUTE_WIRED_HEADSET &&
                        stateNow.route != CallAudioState.ROUTE_SPEAKER
                    ) {
                        withContext(Dispatchers.Main) {
                            CallAudioRouteCoordinator.routeAudio(
                                inCallService = this@MyInCallService,
                                targetRoute = recoveryRoute,
                                availableEndpoints = CallManager.getAvailableEndpointsSnapshot(),
                                isUserAction = false
                            )
                        }
                    }
                } catch (_: Exception) {}
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
                    // Screen OFF or Locked: Present dedicated lockscreen-safe InCallActivity without forcing keyguard unlock.
                    // Unlocked & inside MainActivity: MainActivity's inline ActiveCallScreen presents immediately.
                    // Unlocked & inside another app: Heads-Up CallStyle notification + Dynamic Island present non-intrusively.
                    val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
                    val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                    val isScreenInteractive = pm?.isInteractive ?: false
                    val isKeyguardLocked = km?.isKeyguardLocked ?: false

                    if (!isScreenInteractive || isKeyguardLocked) {
                        launchCallScreen(answerOnLaunch = false)
                    }
                }
            }
            Call.STATE_ACTIVE,
            Call.STATE_DIALING,
            Call.STATE_CONNECTING,
            Call.STATE_NEW,
            Call.STATE_SELECT_PHONE_ACCOUNT,
            Call.STATE_HOLDING -> {
                unregisterScreenOffReceiver()
                stopRinging()
                stopCallWaitingTone()
                if (CallManager.waitingCall.value === call) {
                    CallManager.updateWaitingCall(null)
                }
                FlashLightManager.stopFlashing(this)
                if (!CallManager.isDisconnectRequested(call)) {
                    showActiveCallNotification(call)
                    val km = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                    val isKeyguardLocked = km?.isKeyguardLocked ?: false
                    if (isKeyguardLocked) {
                        launchCallScreen(answerOnLaunch = false)
                    }
                }
            }
            Call.STATE_DISCONNECTING, Call.STATE_DISCONNECTED -> {
                unregisterScreenOffReceiver()
                stopRinging()
                stopCallWaitingTone()
                FlashLightManager.stopFlashing(this)
                nm.cancel(NOTIFICATION_ID_INCOMING)
            }
        }
    }

    private fun showIncomingCallNotification(call: Call, isCallWaiting: Boolean = false) {
        val number = call.details?.handle?.schemeSpecificPart ?: ""
        val displayName = resolveCallerDisplayName(call, number)

        val contentIntent = PendingIntent.getActivity(
            this, 101,
            Intent(this, InCallActivity::class.java).apply {
                action = ACTION_INCOMING_CALL
                setPackage(packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION
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
                Intent(this, InCallActivity::class.java).apply {
                    action = ACTION_ANSWER
                    setPackage(packageName)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_SINGLE_TOP
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
            Intent(this, InCallActivity::class.java).apply {
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
        val number = call.details?.handle?.schemeSpecificPart ?: ""
        val displayName = resolveCallerDisplayName(call, number)
        com.example.util.MissedCallNotificationReceiver.postMissedCallNotification(
            context = applicationContext,
            nm = nm,
            count = 1,
            rawNumber = number,
            displayName = displayName
        )
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
                val timestamp = CallAudioRecorder.formatRecordingTimestamp(this)

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
            val intent = Intent(this, InCallActivity::class.java).apply {
                action = if (answerOnLaunch) ACTION_ANSWER else ACTION_INCOMING_CALL
                setPackage(packageName)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_NO_USER_ACTION
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