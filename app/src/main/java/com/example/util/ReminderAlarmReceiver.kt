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
                    .setContentTitle("Callback Reminder")
                    .setContentText("Reminding you to call back ${reminder.name} (${reminder.number})")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(mainPendingIntent)
                    .setAutoCancel(true)
                    .addAction(android.R.drawable.sym_action_call, "Call Now", callPendingIntent)
                    .build()

                nm.notify(reminderId, notification)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
