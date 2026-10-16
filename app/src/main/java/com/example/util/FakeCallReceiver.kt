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
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.model.ScheduledFakeCall

class FakeCallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("is_fake_call_simulator_enabled", true)) return

        val callId = intent.getStringExtra("call_id")
        val name = intent.getStringExtra("caller_name") ?: "Unknown"
        val number = intent.getStringExtra("caller_number") ?: "Unknown"
        val audioUri = intent.getStringExtra("audio_uri")
        val audioVoiceType = intent.getStringExtra("audio_voice_type") ?: "NONE"

        // Mark triggered in manager so pending list updates accurately
        FakeCallManager.onCallTriggered(appContext, callId)

        try {
            val mainIntent = Intent(appContext, MainActivity::class.java).apply {
                action = "com.example.TRIGGER_FAKE_CALL"
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("TRIGGER_FAKE_CALL", true)
                putExtra("FAKE_CALLER_NAME", name)
                putExtra("FAKE_CALLER_NUMBER", number)
                putExtra("FAKE_CALL_AUDIO_URI", audioUri)
                putExtra("FAKE_CALL_VOICE_TYPE", audioVoiceType)
            }

            val fullScreenPendingIntent = PendingIntent.getActivity(
                appContext,
                NOTIFICATION_ID,
                mainIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channelId = "incoming_calls_channel"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Delete obsolete legacy channel name to eradicate any traces of "Fake Call Simulation"
                try {
                    nm.deleteNotificationChannel("fake_call_simulation_channel")
                } catch (_: Exception) {}

                val channel = NotificationChannel(
                    channelId,
                    "Incoming Calls",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Incoming call alerts"
                    enableVibration(true)
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

            // Authentic incoming call appearance: zero "fake" branding on lockscreen or status bar
            val notification = NotificationCompat.Builder(appContext, channelId)
                .setSmallIcon(android.R.drawable.sym_action_call)
                .setContentTitle(name)
                .setContentText("Incoming call ($number)")
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .setAutoCancel(true)
                .build()

            nm.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) {}
    }

    companion object {
        const val NOTIFICATION_ID = 9999

        fun scheduleFakeCall(
            context: Context,
            name: String,
            number: String,
            delaySeconds: Int,
            repeatCount: Int = 1,
            repeatIntervalSeconds: Int = 0,
            audioUri: String? = null,
            audioVoiceType: String = "NONE"
        ) {
            val safeRepeatCount = repeatCount.coerceIn(1, 16)
            val baseTriggerTime = System.currentTimeMillis() + (delaySeconds * 1000L)

            for (i in 0 until safeRepeatCount) {
                val triggerTime = baseTriggerTime + (i * repeatIntervalSeconds * 1000L)
                val call = ScheduledFakeCall(
                    name = name,
                    number = number,
                    triggerTimeMillis = triggerTime,
                    audioUri = audioUri,
                    audioVoiceType = audioVoiceType
                )
                FakeCallManager.scheduleCall(context, call)
            }
        }

        fun cancelFakeCall(context: Context) {
            FakeCallManager.cancelAll(context)
        }
    }
}