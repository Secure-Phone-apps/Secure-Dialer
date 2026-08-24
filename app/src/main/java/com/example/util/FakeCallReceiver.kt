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
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class FakeCallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val name = intent.getStringExtra("caller_name") ?: "Unknown"
        val number = intent.getStringExtra("caller_number") ?: "Unknown"

        try {
            // 1. Wake screen if device is locked/sleeping
            try {
                val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
                @Suppress("DEPRECATION")
                val wakeLock = powerManager?.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK or
                    PowerManager.ACQUIRE_CAUSES_WAKEUP or
                    PowerManager.ON_AFTER_RELEASE,
                    "SecureDialer:FakeCallWakeLock"
                )
                wakeLock?.acquire(15000L)
            } catch (_: Exception) {
            }

            val mainIntent = Intent(context, MainActivity::class.java).apply {
                setPackage(context.packageName)
                action = "com.example.TRIGGER_FAKE_CALL"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra("TRIGGER_FAKE_CALL", true)
                putExtra("FAKE_CALLER_NAME", name)
                putExtra("FAKE_CALLER_NUMBER", number)
            }

            // 2. Try starting activity directly
            try {
                context.startActivity(mainIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 3. Post a high-priority notification with full-screen intent and content intent to guarantee full screen call display.
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
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                    setSound(
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE),
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                            .build()
                    )
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
                .setContentTitle(name)
                .setContentText("Incoming fake call ($number)")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
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
