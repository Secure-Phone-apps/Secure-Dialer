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

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val reminderId = intent.getIntExtra("reminder_id", -1)
        if (reminderId == -1) return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val reminders = db.dialerDao().getAllRemindersList()
                val reminder = reminders.find { it.id == reminderId } ?: return@launch

                // Mark as completed
                db.dialerDao().updateReminder(reminder.copy(isCompleted = true))

                // Post Notification
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channelId = "call_reminders_channel"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    nm.createNotificationChannel(
                        NotificationChannel(channelId, "Call Reminders", NotificationManager.IMPORTANCE_HIGH).apply {
                            description = "Notifications for scheduled callback reminders"
                        }
                    )
                }

                // Intent to open Main Dialer Screen
                val mainIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                val mainPendingIntent = PendingIntent.getActivity(
                    context,
                    reminderId,
                    mainIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                // Intent to initiate Call
                val callIntent = Intent(Intent.ACTION_CALL).apply {
                    data = Uri.parse("tel:${reminder.number}")
                }
                val callPendingIntent = PendingIntent.getActivity(
                    context,
                    reminderId + 100000,
                    callIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                val notification = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.sym_action_chat)
                    .setContentTitle(context.getString(R.string.callback_reminders_title))
                    .setContentText(context.getString(R.string.remind_call_back_prompt, "${reminder.name} (${reminder.number})"))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(mainPendingIntent)
                    .setAutoCancel(true)
                    .addAction(android.R.drawable.sym_action_call, context.getString(R.string.btn_call_back), callPendingIntent)
                    .build()

                nm.notify(reminderId, notification)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
