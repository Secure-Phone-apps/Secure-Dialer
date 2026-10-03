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

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val appContext = context.applicationContext

        val prefs = appContext.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("is_callback_reminders_enabled", true)) return

        val reminderId = intent.getIntExtra("reminder_id", -1)
        if (reminderId == -1) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ANR GUARD: Ensure DB operations never exceed the OS BroadcastReceiver execution budget
                withTimeoutOrNull(8000L) {
                    val db = AppDatabase.getDatabase(appContext)
                    val reminder = db.dialerDao().getAllRemindersList().find { it.id == reminderId } ?: return@withTimeoutOrNull

                    // Mark reminder as completed
                    db.dialerDao().updateReminder(reminder.copy(isCompleted = true))

                    val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    val channelId = "call_reminders_channel"
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        nm.createNotificationChannel(
                            NotificationChannel(channelId, "Call Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                                description = "Notifications for scheduled callback reminders"
                                enableVibration(true)
                            }
                        )
                    }

                    // Intent to open Main Dialer Screen
                    val mainPendingIntent = PendingIntent.getActivity(
                        appContext,
                        reminderId,
                        Intent(appContext, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                        },
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    // FIXED: Use ACTION_DIAL to eliminate ActivityNotFoundException crashes
                    val callPendingIntent = PendingIntent.getActivity(
                        appContext,
                        reminderId + 100000,
                        Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:${reminder.number}")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        },
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )

                    val targetLabel = if (reminder.name.isNotBlank()) "${reminder.name} (${reminder.number})" else reminder.number
                    val notification = NotificationCompat.Builder(appContext, channelId)
                        .setSmallIcon(android.R.drawable.sym_action_chat)
                        .setContentTitle(appContext.getString(R.string.callback_reminders_title))
                        .setContentText(appContext.getString(R.string.remind_call_back_prompt, targetLabel))
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .setCategory(NotificationCompat.CATEGORY_REMINDER)
                        .setContentIntent(mainPendingIntent)
                        .setAutoCancel(true)
                        .addAction(
                            android.R.drawable.sym_action_call,
                            appContext.getString(R.string.btn_call_back),
                            callPendingIntent
                        )
                        .build()

                    nm.notify(reminderId, notification)
                }
            } catch (_: Exception) {
            } finally {
                pendingResult.finish()
            }
        }
    }
}