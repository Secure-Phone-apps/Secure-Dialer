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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyInCallService : InCallService() {
    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        CallManager.inCallService = this
        CallManager.addCall(call)
        
        val handle = call.details?.handle
        val number = handle?.schemeSpecificPart ?: ""
        val cnapName = call.details?.callerDisplayName
        if (!cnapName.isNullOrBlank() && number.isNotEmpty()) {
            ContactCache.putCnapName(number, cnapName)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = com.example.data.AppDatabase.getDatabase(this@MyInCallService)
                    db.dialerDao().insertSetting(com.example.model.AppSetting("cnap_" + number.filter { it.isDigit() }, cnapName))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        
        if (call.state == Call.STATE_RINGING) {
            showIncomingCallNotification(call)
            com.example.util.FlashLightManager.startFlashing(this)
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
                    com.example.util.FlashLightManager.stopFlashing(this@MyInCallService)
                }
                if (state == Call.STATE_DISCONNECTED) {
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
            .setContentTitle(getString(R.string.call_type_incoming))
            .setContentText("${getString(R.string.call_type_incoming)}: $displayName")
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
            data = Uri.parse("tel:$number")
        }
        val callBackPendingIntent = PendingIntent.getActivity(
            this, 
            System.currentTimeMillis().toInt() + 1, 
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
        }
    }

    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        CallManager.updateAudioState(audioState)
    }
}
