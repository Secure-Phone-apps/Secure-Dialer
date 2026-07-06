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
        CallManager.updateCall(call)
        
        showIncomingCallNotification(call)

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
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "incoming_call_channel"
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Incoming Calls", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.sym_call_incoming)
            .setContentTitle("Incoming Call")
            .setContentText("Incoming call from ${call.details.handle}")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1, notification)
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        if (CallManager.currentCall.value == call) {
            CallManager.updateCall(null)
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(1)
        }
    }
}

