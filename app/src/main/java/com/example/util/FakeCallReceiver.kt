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

package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class FakeCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val name = intent.getStringExtra("caller_name") ?: "Unknown"
        val number = intent.getStringExtra("caller_number") ?: "Unknown"

        try {
            val mainIntent = Intent(context, MainActivity::class.java).apply {
                setPackage(context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("TRIGGER_FAKE_CALL", true)
                putExtra("FAKE_CALLER_NAME", name)
                putExtra("FAKE_CALLER_NUMBER", number)
            }

            // 1. Try starting activity directly
            try {
                context.startActivity(mainIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Also post a high-priority notification with full-screen intent to bypass Android 10+ background activity launch restrictions.
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "fake_call_simulation_channel"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    channelId,
                    "Fake Call Simulation",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Interactive incoming fake calls"
                    enableVibration(true)
                    setBypassDnd(true)
                }
                nm.createNotificationChannel(channel)
            }

            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                9999,
                mainIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val notification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle("Incoming Fake Call")
                .setContentText("$name ($number)")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setAutoCancel(true)
                .build()

            nm.notify(9999, notification)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        fun scheduleFakeCall(
            context: Context,
            name: String,
            number: String,
            delaySeconds: Int,
            repeatCount: Int = 1,
            repeatIntervalSeconds: Int = 0
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            
            // Always cancel any existing scheduled fake calls first to clear the schedule
            cancelFakeCall(context)

            val baseTriggerTime = System.currentTimeMillis() + (delaySeconds * 1000L)

            for (i in 0 until repeatCount) {
                val intent = Intent(context, FakeCallReceiver::class.java).apply {
                    setPackage(context.packageName)
                    putExtra("caller_name", name)
                    putExtra("caller_number", number)
                }
                
                // Use unique request code per sequential call
                val requestCode = 9999 + i
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val triggerTime = baseTriggerTime + (i * repeatIntervalSeconds * 1000L)
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    } else {
                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                    }
                } catch (e: SecurityException) {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun cancelFakeCall(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, FakeCallReceiver::class.java).apply {
                setPackage(context.packageName)
            }
            
            // Cancel up to 16 repeating scheduled alarms to cover any scheduled chain
            for (i in 0..15) {
                val requestCode = 9999 + i
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
                )
                if (pendingIntent != null) {
                    try {
                        alarmManager.cancel(pendingIntent)
                        pendingIntent.cancel()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }
}
