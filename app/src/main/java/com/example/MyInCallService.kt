/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.DisconnectCause
import android.telecom.InCallService
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
import com.example.util.RecordingFeedbackHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date

class MyInCallService : InCallService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val nm by lazy { getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }

    override fun onCreate() {
        super.onCreate()
        initNotificationChannels()
    }

    override fun onDestroy() {
        DynamicIslandOverlayManager.stopCallMonitoring()
        FlashLightManager.stopFlashing(this)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_HANG_UP, ACTION_DECLINE -> CallManager.disconnect()
            ACTION_ANSWER -> {
                CallManager.answer()
                launchCallScreen(answerOnLaunch = true)
            }
            ACTION_TOGGLE_MUTE -> {
                val isMuted = CallManager.audioState.value?.isMuted ?: false
                CallManager.setMuted(!isMuted)
                CallManager.currentCall.value?.let { showActiveCallNotification(it) }
            }
            ACTION_TOGGLE_SPEAKER -> {
                val isSpeaker = (CallManager.audioState.value?.route ?: CallAudioState.ROUTE_EARPIECE) == CallAudioState.ROUTE_SPEAKER
                CallManager.setSpeaker(!isSpeaker)
                CallManager.currentCall.value?.let { showActiveCallNotification(it) }
            }
            ACTION_TOGGLE_RECORD -> toggleCallRecordingFromNotification()
        }
        return START_NOT_STICKY
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.inCallService = this
        CallManager.addCall(call)
        DynamicIslandOverlayManager.startCallMonitoring(this)

        persistCnapIfPresent(call)
        handleCallState(call)

        call.registerCallback(object : Call.Callback() {
            private var wasRinging = (call.state == Call.STATE_RINGING)

            override fun onStateChanged(c: Call, state: Int) {
                super.onStateChanged(c, state)
                if (state == Call.STATE_RINGING) wasRinging = true

                if (state == Call.STATE_ACTIVE || state == Call.STATE_DISCONNECTED) {
                    nm.cancel(NOTIFICATION_ID_INCOMING)
                    FlashLightManager.stopFlashing(this@MyInCallService)
                }

                if (state in listOf(Call.STATE_ACTIVE, Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_HOLDING)) {
                    showActiveCallNotification(c)
                }

                if (state == Call.STATE_DISCONNECTED) {
                    nm.cancel(NOTIFICATION_ID_ACTIVE)
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
        CallManager.removeCall(call)
        FlashLightManager.stopFlashing(this)
        if (CallManager.calls.value.isEmpty()) {
            DynamicIslandOverlayManager.stopCallMonitoring()
            nm.cancel(NOTIFICATION_ID_INCOMING)
            nm.cancel(NOTIFICATION_ID_ACTIVE)
        }
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        CallManager.updateAudioState(audioState)
        CallManager.currentCall.value?.let { showActiveCallNotification(it) }
    }

    private fun handleCallState(call: Call) {
        when (call.state) {
            Call.STATE_RINGING -> {
                FlashLightManager.startFlashing(this)
                showIncomingCallNotification(call)
            }
            Call.STATE_ACTIVE, Call.STATE_DIALING, Call.STATE_CONNECTING, Call.STATE_HOLDING -> {
                FlashLightManager.stopFlashing(this)
                showActiveCallNotification(call)
                launchCallScreen(answerOnLaunch = false)
            }
        }
    }

    private fun showIncomingCallNotification(call: Call) {
        val number = call.details?.handle?.schemeSpecificPart ?: ""
        val displayName = resolveCallerDisplayName(call, number)

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 101,
            Intent(this, MainActivity::class.java).apply {
                action = ACTION_INCOMING_CALL
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_SHOW_CALL, true)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val declinePendingIntent = PendingIntent.getService(
            this, 102,
            Intent(this, MyInCallService::class.java).apply { action = ACTION_DECLINE },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val answerPendingIntent = PendingIntent.getActivity(
            this, 103,
            Intent(this, MainActivity::class.java).apply {
                action = ACTION_ANSWER
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_SHOW_CALL, true)
                putExtra(EXTRA_ANSWER, true)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val caller = Person.Builder().setName(displayName).setKey(number).build()
        val style = NotificationCompat.CallStyle.forIncomingCall(caller, declinePendingIntent, answerPendingIntent)

        val notification = NotificationCompat.Builder(this, CHANNEL_INCOMING)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setStyle(style)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()

        nm.notify(NOTIFICATION_ID_INCOMING, notification)
    }

    private fun showActiveCallNotification(call: Call) {
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

        val recordTitle = if (isRecording) "■ Stop Rec" else "● Record"
        val speakerTitle = if (isSpeaker) "Earpiece" else "Speaker"
        val muteTitle = if (isMuted) "Unmute" else "Mute"

        val notification = NotificationCompat.Builder(this, CHANNEL_ACTIVE)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle(if (isRecording) "Ongoing Call • [REC]" else "Ongoing Call")
            .setContentText("Call with $displayName is active")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(contentPendingIntent)
            .addAction(android.R.drawable.ic_btn_speak_now, recordTitle, recordPendingIntent)
            .addAction(android.R.drawable.stat_notify_call_mute, muteTitle, mutePendingIntent)
            .addAction(android.R.drawable.stat_sys_speakerphone, speakerTitle, speakerPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hang Up", hangUpPendingIntent)
            .build()

        nm.notify(NOTIFICATION_ID_ACTIVE, notification)
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
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_SHOW_CALL, true)
                if (answerOnLaunch) putExtra(EXTRA_ANSWER, true)
            }
            startActivity(intent)
        } catch (_: Exception) {}
    }

    private fun initNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannels(listOf(
                NotificationChannel(CHANNEL_INCOMING, "Incoming Calls", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Incoming call alerts"
                    setSound(null, null)
                    enableVibration(false)
                },
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
        const val CHANNEL_INCOMING = "incoming_call_channel_v2"
        const val CHANNEL_ACTIVE = "active_call_channel"
        const val CHANNEL_MISSED = "missed_call_channel"

        const val NOTIFICATION_ID_INCOMING = 1
        const val NOTIFICATION_ID_MISSED = 2
        const val NOTIFICATION_ID_ACTIVE = 3

        const val ACTION_HANG_UP = "com.example.ACTION_HANG_UP"
        const val ACTION_ANSWER = "com.example.ACTION_ANSWER"
        const val ACTION_DECLINE = "com.example.ACTION_DECLINE"
        const val ACTION_TOGGLE_RECORD = "com.example.ACTION_TOGGLE_RECORD"
        const val ACTION_TOGGLE_SPEAKER = "com.example.ACTION_TOGGLE_SPEAKER"
        const val ACTION_TOGGLE_MUTE = "com.example.ACTION_TOGGLE_MUTE"
        const val ACTION_INCOMING_CALL = "com.example.INCOMING_CALL"

        const val EXTRA_SHOW_CALL = "SHOW_CALL_SCREEN"
        const val EXTRA_ANSWER = "ANSWER_ON_LAUNCH"
    }
}