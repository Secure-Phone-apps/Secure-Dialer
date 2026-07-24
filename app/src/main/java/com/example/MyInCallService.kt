package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import androidx.core.app.NotificationCompat

class MyInCallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.inCallService = this
        CallManager.addCall(call)
        
        if (call.state == Call.STATE_RINGING) {
            showIncomingCallNotification(call)
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
                    // Cancel incoming call notification when call becomes active or disconnects
                    val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    notificationManager.cancel(1)
                }
                if (state == Call.STATE_DISCONNECTED) {
                    if (wasRinging) {
                        val causeCode = c.details?.disconnectCause?.code
                        if (causeCode != android.telecom.DisconnectCause.REJECTED &&
                            causeCode != android.telecom.DisconnectCause.LOCAL) {
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

        // Start the MainActivity to display the incoming/outgoing call screen
        try {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("SHOW_CALL_SCREEN", true)
            }
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showIncomingCallNotification(call: Call) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "incoming_call_channel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(NotificationChannel(channelId, "Incoming Calls", NotificationManager.IMPORTANCE_HIGH))
        }

        val handle = call.details?.handle
        val number = handle?.schemeSpecificPart ?: ""
        val cnapName = call.details?.callerDisplayName
        val contactName = if (number.isNotEmpty()) {
            getContactNameFromNumber(this, number)
        } else {
            null
        }
        val displayName = if (contactName != null) {
            if (!cnapName.isNullOrBlank()) "$contactName (CNAP: $cnapName)" else contactName
        } else if (!cnapName.isNullOrBlank()) {
            "$cnapName (Verified Carrier Name)"
        } else if (number.isNotEmpty()) {
            number
        } else {
            "Unknown"
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("Incoming Call")
            .setContentText("Incoming call from $displayName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
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
        val context = this
        val contactName = getContactNameFromNumber(context, number)
        val name = if (contactName != null) {
            if (!cnapName.isNullOrBlank()) "$contactName (CNAP: $cnapName)" else contactName
        } else if (!cnapName.isNullOrBlank()) {
            "$cnapName (Verified Carrier Name)"
        } else {
            number
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("SHOW_CALL_LOG", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 
            System.currentTimeMillis().toInt(), 
            intent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val callBackIntent = Intent(Intent.ACTION_CALL).apply {
            data = android.net.Uri.parse("tel:$number")
        }
        val callBackPendingIntent = PendingIntent.getActivity(
            this, 
            System.currentTimeMillis().toInt() + 1, 
            callBackIntent, 
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.sym_call_missed)
            .setContentTitle("Missed Call")
            .setContentText("Missed call from $name")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(android.R.drawable.sym_action_call, "Call Back", callBackPendingIntent)
            .build()

        notificationManager.notify(2, notification)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        CallManager.removeCall(call)
        if (CallManager.calls.value.isEmpty()) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(1)
        }
    }

    override fun onCallAudioStateChanged(audioState: android.telecom.CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        CallManager.updateAudioState(audioState)
    }
}

