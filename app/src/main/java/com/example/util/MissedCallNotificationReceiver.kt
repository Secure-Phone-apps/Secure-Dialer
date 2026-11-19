/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.CallLog
import android.provider.ContactsContract
import android.telecom.TelecomManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.MyInCallService
import com.example.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Handles Android Telecom's ACTION_SHOW_MISSED_CALLS_NOTIFICATION delegation broadcast
 * when Secure Dialer is the default phone app, and manages clearing missed-call state.
 */
class MissedCallNotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != TelecomManager.ACTION_SHOW_MISSED_CALLS_NOTIFICATION) return

        val count = intent.getIntExtra(TelecomManager.EXTRA_NOTIFICATION_COUNT, 0)
        val rawNumber = intent.getStringExtra(TelecomManager.EXTRA_NOTIFICATION_PHONE_NUMBER)?.trim()

        val appContext = context.applicationContext
        appContext.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
            .edit()
            .putLong(PREF_LAST_TELECOM_MISSED_BROADCAST_MS, System.currentTimeMillis())
            .apply()

        val nm = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        ensureMissedCallChannel(nm)

        if (count <= 0) {
            nm.cancel(MyInCallService.NOTIFICATION_ID_MISSED)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val displayName = withTimeoutOrNull(1200L) {
                    resolveDisplayName(appContext, rawNumber)
                } ?: rawNumber.orEmpty().ifBlank { appContext.getString(R.string.unknown) }

                postMissedCallNotification(
                    context = appContext,
                    nm = nm,
                    count = count,
                    rawNumber = rawNumber,
                    displayName = displayName
                )
            } catch (_: Exception) {
            } finally {
                try {
                    pendingResult.finish()
                } catch (_: Exception) {}
            }
        }
    }

    private fun resolveDisplayName(context: Context, rawNumber: String?): String {
        val cleaned = rawNumber?.trim().orEmpty()
        if (cleaned.isEmpty() || cleaned in listOf("-1", "-2", "-3")) {
            return context.getString(R.string.unknown)
        }
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            try {
                val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(cleaned))
                context.contentResolver.query(
                    uri,
                    arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                    null,
                    null,
                    null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val name = cursor.getString(0)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            } catch (_: Exception) {}
        }
        return cleaned
    }

    companion object {
        const val PREF_LAST_TELECOM_MISSED_BROADCAST_MS = "last_telecom_missed_broadcast_ms"

        fun ensureMissedCallChannel(nm: NotificationManager) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        MyInCallService.CHANNEL_MISSED,
                        "Missed Calls",
                        NotificationManager.IMPORTANCE_DEFAULT
                    ).apply {
                        description = "Missed call notifications"
                    }
                )
            }
        }

        fun buildViewCallLogIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                type = CallLog.Calls.CONTENT_TYPE
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("SHOW_CALL_LOG", true)
                putExtra("MISSED_CALL", true)
            }
        }

        fun postMissedCallNotification(
            context: Context,
            nm: NotificationManager,
            count: Int,
            rawNumber: String?,
            displayName: String
        ) {
            ensureMissedCallChannel(nm)

            val contentPendingIntent = PendingIntent.getActivity(
                context,
                301,
                buildViewCallLogIntent(context),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val title = if (count > 1) {
                context.getString(R.string.missed_calls_notification_title, count)
            } else {
                context.getString(R.string.call_type_missed)
            }

            val text = if (count > 1) {
                if (!rawNumber.isNullOrBlank() && displayName != context.getString(R.string.unknown)) {
                    "$displayName • ${context.getString(R.string.tap_to_view_call_log)}"
                } else {
                    context.getString(R.string.tap_to_view_call_log)
                }
            } else {
                "${context.getString(R.string.call_type_missed)}: $displayName"
            }

            val builder = NotificationCompat.Builder(context, MyInCallService.CHANNEL_MISSED)
                .setSmallIcon(android.R.drawable.sym_call_missed)
                .setContentTitle(title)
                .setContentText(text)
                .setNumber(count)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setCategory(NotificationCompat.CATEGORY_MISSED_CALL)
                .setContentIntent(contentPendingIntent)
                .setAutoCancel(true)

            val validCallbackNumber = rawNumber?.takeIf {
                it.isNotBlank() && it !in listOf("-1", "-2", "-3") && !it.equals("Unknown", ignoreCase = true)
            }
            if (count == 1 && validCallbackNumber != null) {
                val callBackPendingIntent = PendingIntent.getActivity(
                    context,
                    302,
                    Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:$validCallbackNumber")
                    },
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
                builder.addAction(
                    android.R.drawable.sym_action_call,
                    context.getString(R.string.btn_call_back),
                    callBackPendingIntent
                )
            }

            nm.notify(MyInCallService.NOTIFICATION_ID_MISSED, builder.build())
        }

        fun clearMissedCallNotifications(context: Context) {
            try {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                nm?.cancel(MyInCallService.NOTIFICATION_ID_MISSED)
            } catch (_: Exception) {}

            try {
                val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
                if (telecomManager != null && telecomManager.defaultDialerPackage == context.packageName) {
                    telecomManager.cancelMissedCallsNotification()
                }
            } catch (_: SecurityException) {
            } catch (_: Exception) {}
        }
    }
}
