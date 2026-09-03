/*
 * Copyright (C) 2026 MovStore
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.example

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.DisconnectCause
import android.telecom.InCallService
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class MyInCallService : InCallService() {
    private val serviceScope = CoroutineScope(kotlinx.coroutines.SupervisorJob() + Dispatchers.IO)
    private var wakeLock: PowerManager.WakeLock? = null

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
            releaseWakeLock()
            @Suppress("DEPRECATION")
            wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
                "SecureDialer:IncomingCallWakeLock"
            ).apply {
                setReferenceCounted(false)
                acquire(20000L) // 20 seconds timeout to keep screen on while ringing
            }
        } catch (e: Exception) {
            try {
                val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return
                wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "SecureDialer:IncomingCallWakeLock"
                ).apply {
                    setReferenceCounted(false)
                    acquire(20000L)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (_: Exception) {
        }
        wakeLock = null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when (intent.action) {
                ACTION_HANG_UP, ACTION_DECLINE -> {
                    releaseWakeLock()
                    CallManager.disconnect()
                }
                ACTION_ANSWER -> {
                    releaseWakeLock()
                    CallManager.answer()
                    try {
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            setPackage(packageName)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                            putExtra("SHOW_CALL_SCREEN", true)
                        }
                        startActivity(mainIntent)
                    } catch (_: Exception) {
                    }
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onDestroy() {
        releaseWakeLock()
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.inCallService = this
        CallManager.addCall(call)
        
        val handle = call.details?.handle
        val number = handle?.schemeSpecificPart ?: ""
        val cnapName = call.details?.callerDisplayName
        if (!cnapName.isNullOrBlank() && number.isNotEmpty()) {
            ContactCache.putCnapName(number, cnapName)
            serviceScope.launch {
                try {
                    val db = com.example.data.AppDatabase.getDatabase(this@MyInCallService)
                    db.dialerDao().insertSetting(com.example.model.AppSetting("cnap_" + number.filter { it.isDigit() }, cnapName))
                } catch (_: Exception) {
                }
            }
        }
        
        if (call.state == Call.STATE_RINGING) {
            acquireWakeLock()
            showIncomingCallNotification(call)
            com.example.util.FlashLightManager.startFlashing(this)
        } else if (call.state == Call.STATE_ACTIVE || call.state == Call.STATE_DIALING || call.state == Call.STATE_CONNECTING || call.state == Call.STATE_HOLDING) {
            releaseWakeLock()
            showActiveCallNotification(call)
        }

        // Register callback to track call status and show missed call notifications if applicable
        call.registerCallback(object : Call.Callback() {
            private var wasRinging = (call.state == Call.STATE_RINGING)

            override fun onStateChanged(c: Call, state: Int) {
                super.onStateChanged(c, state)
                if (state == Call.STATE_RINGING) {
                    wasRinging = true
                }
                if (state == Call.STATE_ACTIVE || state == Call.STATE_DISCONNECTED) {
                    releaseWakeLock()
                    // Cancel incoming call notification when call becomes active or disconnects
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(1)
                    com.example.util.FlashLightManager.stopFlashing(this@MyInCallService)
                }
                if (state == Call.STATE_ACTIVE || state == Call.STATE_DIALING || state == Call.STATE_CONNECTING || state == Call.STATE_HOLDING) {
                    showActiveCallNotification(c)
                }
                if (state == Call.STATE_DISCONNECTED) {
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(3)
                    if (wasRinging) {
                        val causeCode = c.details?.disconnectCause?.code
                        if (causeCode != DisconnectCause.REJECTED &&
                            causeCode != DisconnectCause.LOCAL) {
                            showMissedCallNotification(c)
                        }
                    }
                    if (CallManager.waitingCall.value == c) {
                        CallManager.updateWaitingCall(null)
                    }
                    c.unregisterCallback(this)
                }
                if (state == Call.STATE_ACTIVE) {
                    wasRinging = false
                }
            }
        })

        // Start MainActivity to display the call screen for outgoing calls.
        // For incoming ringing calls, rely on showIncomingCallNotification's fullScreenIntent
        // so Android displays a compact heads-up banner if the phone is actively in use (unlocked).
        if (call.state != Call.STATE_RINGING) {
            try {
                val intent = Intent(this, MainActivity::class.java).apply {
                    setPackage(packageName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    putExtra("SHOW_CALL_SCREEN", true)
                }
                startActivity(intent)
            } catch (_: Exception) {
            }
        }
    }

    private fun showIncomingCallNotification(call: Call) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "incoming_call_channel_v2"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                nm.deleteNotificationChannel("incoming_call_channel")
            } catch (_: Exception) {}

            val channel = NotificationChannel(channelId, "Incoming Calls", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Full screen and heads-up notifications for incoming phone calls"
                importance = NotificationManager.IMPORTANCE_HIGH
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
                enableVibration(false)
                setSound(null, null)
            }
            nm.createNotificationChannel(channel)
        }

        val handle = call.details?.handle
        val number = handle?.schemeSpecificPart ?: ""
        val cnapName = call.details?.callerDisplayName
        val contactName = if (number.isNotEmpty()) getContactNameFromNumber(this, number) else null
        val savedCnap = if (number.isNotEmpty() && contactName == null && cnapName.isNullOrBlank()) {
            getSavedCnapNameSync(this, number)
        } else null
        
        val displayName = when {
            contactName != null -> contactName
            !cnapName.isNullOrBlank() -> cnapName
            !savedCnap.isNullOrBlank() -> savedCnap
            number.isNotEmpty() -> number
            else -> "Unknown"
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            setPackage(packageName)
            action = "com.example.INCOMING_CALL"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("SHOW_CALL_SCREEN", true)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 
            101, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // Direct action pending intents for quick response from heads-up / lockscreen notification
        val declineIntent = Intent(this, MyInCallService::class.java).apply {
            setPackage(packageName)
            action = ACTION_DECLINE
        }
        val declinePendingIntent = PendingIntent.getService(
            this,
            102,
            declineIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val answerIntent = Intent(this, MyInCallService::class.java).apply {
            setPackage(packageName)
            action = ACTION_ANSWER
        }
        val answerPendingIntent = PendingIntent.getService(
            this,
            103,
            answerIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle(displayName)
            .setContentText(getString(R.string.call_type_incoming))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setSound(null)
            .setVibrate(null)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.btn_decline),
                declinePendingIntent
            )
            .addAction(
                android.R.drawable.sym_action_call,
                getString(R.string.btn_answer),
                answerPendingIntent
            )
            .build()

        nm.notify(1, notification)
    }

    private fun showMissedCallNotification(call: Call) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "missed_call_channel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Missed Calls", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        val handle = call.details?.handle
        val number = handle?.schemeSpecificPart ?: "Unknown"
        val cnapName = call.details?.callerDisplayName
        val contactName = getContactNameFromNumber(this, number)
        val savedCnap = if (number != "Unknown" && number.isNotEmpty() && contactName == null && cnapName.isNullOrBlank()) {
            getSavedCnapNameSync(this, number)
        } else null
        
        val name = when {
            contactName != null -> contactName
            !cnapName.isNullOrBlank() -> cnapName
            !savedCnap.isNullOrBlank() -> savedCnap
            else -> number
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("SHOW_CALL_LOG", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 
            202, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val callBackIntent = Intent(Intent.ACTION_DIAL).apply {
            setPackage(packageName)
            data = Uri.parse("tel:$number")
        }
        val callBackPendingIntent = PendingIntent.getActivity(
            this, 
            203, 
            callBackIntent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentTitle(getString(R.string.call_type_missed))
            .setContentText("${getString(R.string.call_type_missed)}: $name")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.sym_action_call, getString(R.string.btn_call_back), callBackPendingIntent)
            .build()

        notificationManager.notify(2, notification)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallManager.removeCall(call)
        com.example.util.FlashLightManager.stopFlashing(this)
        if (CallManager.calls.value.isEmpty()) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(1)
            notificationManager.cancel(3)
        }
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        CallManager.updateAudioState(audioState)
    }

    private fun showActiveCallNotification(call: Call) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "active_call_channel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Active Calls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Ongoing active call controls"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }

        val handle = call.details?.handle
        val number = handle?.schemeSpecificPart ?: ""
        val cnapName = call.details?.callerDisplayName
        val contactName = if (number.isNotEmpty()) getContactNameFromNumber(this, number) else null
        val savedCnap = if (number.isNotEmpty() && contactName == null && cnapName.isNullOrBlank()) {
            getSavedCnapNameSync(this, number)
        } else null
        
        val displayName = when {
            contactName != null -> contactName
            !cnapName.isNullOrBlank() -> cnapName
            !savedCnap.isNullOrBlank() -> savedCnap
            number.isNotEmpty() -> number
            else -> "Unknown"
        }

        val returnIntent = Intent(this, MainActivity::class.java).apply {
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("SHOW_CALL_SCREEN", true)
        }
        val returnPendingIntent = PendingIntent.getActivity(
            this, 
            200, 
            returnIntent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val hangUpIntent = Intent(this, MyInCallService::class.java).apply {
            setPackage(packageName)
            action = ACTION_HANG_UP
        }
        val hangUpPendingIntent = PendingIntent.getService(
            this,
            201,
            hangUpIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.sym_action_call)
            .setContentTitle("Ongoing Call")
            .setContentText("Call with $displayName is active")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setContentIntent(returnPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Hang Up", hangUpPendingIntent)
            .build()

        nm.notify(3, notification)
    }

    companion object {
        const val ACTION_HANG_UP = "com.example.ACTION_HANG_UP"
        const val ACTION_ANSWER = "com.example.ACTION_ANSWER"
        const val ACTION_DECLINE = "com.example.ACTION_DECLINE"
    }
}
