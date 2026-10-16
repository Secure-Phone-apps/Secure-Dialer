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
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.model.ScheduledFakeCall
import org.json.JSONArray
import org.json.JSONObject

object FakeCallManager {

    private const val PREFS_KEY = "scheduled_fake_calls_json"
    private const val NOTIFICATION_BASE_ID = 9999

    fun getScheduledCalls(context: Context): List<ScheduledFakeCall> {
        val prefs = context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(PREFS_KEY, "[]") ?: "[]"
        val list = mutableListOf<ScheduledFakeCall>()
        try {
            val arr = JSONArray(jsonStr)
            val now = System.currentTimeMillis()
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                val item = ScheduledFakeCall.fromJson(obj)
                // Filter out calls that are more than 5 minutes in the past
                if (item.triggerTimeMillis > now - 300_000L) {
                    list.add(item)
                }
            }
        } catch (_: Exception) {}
        return list.sortedBy { it.triggerTimeMillis }
    }

    fun saveScheduledCalls(context: Context, calls: List<ScheduledFakeCall>) {
        val prefs = context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
        val arr = JSONArray()
        calls.forEach { arr.put(it.toJson()) }
        prefs.edit().putString(PREFS_KEY, arr.toString()).apply()
    }

    fun scheduleCall(context: Context, call: ScheduledFakeCall) {
        val currentList = getScheduledCalls(context).toMutableList()
        currentList.removeAll { it.id == call.id }
        currentList.add(call)
        saveScheduledCalls(context, currentList)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, FakeCallReceiver::class.java).apply {
            setPackage(context.packageName)
            putExtra("call_id", call.id)
            putExtra("caller_name", call.name)
            putExtra("caller_number", call.number)
            putExtra("audio_uri", call.audioUri)
            putExtra("audio_voice_type", call.audioVoiceType)
        }

        val requestCode = (call.id.hashCode() and 0x7FFFFFFF)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        try {
            if (canScheduleExact) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, call.triggerTimeMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, call.triggerTimeMillis, pendingIntent)
                }
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, call.triggerTimeMillis, pendingIntent)
                } else {
                    alarmManager.set(AlarmManager.RTC_WAKEUP, call.triggerTimeMillis, pendingIntent)
                }
            }
        } catch (_: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, call.triggerTimeMillis, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, call.triggerTimeMillis, pendingIntent)
            }
        } catch (_: Exception) {}
    }

    fun cancelCall(context: Context, callId: String) {
        val currentList = getScheduledCalls(context).toMutableList()
        currentList.removeAll { it.id == callId }
        saveScheduledCalls(context, currentList)

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, FakeCallReceiver::class.java).apply {
            setPackage(context.packageName)
        }
        val requestCode = (callId.hashCode() and 0x7FFFFFFF)
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
            } catch (_: Exception) {}
        }
    }

    fun cancelAll(context: Context) {
        val currentList = getScheduledCalls(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, FakeCallReceiver::class.java).apply {
            setPackage(context.packageName)
        }

        currentList.forEach { call ->
            val requestCode = (call.id.hashCode() and 0x7FFFFFFF)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
            if (pendingIntent != null && alarmManager != null) {
                try {
                    alarmManager.cancel(pendingIntent)
                    pendingIntent.cancel()
                } catch (_: Exception) {}
            }
        }

        // Also clean up legacy base notification IDs
        if (alarmManager != null) {
            for (i in 0..15) {
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    NOTIFICATION_BASE_ID + i,
                    intent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
                )
                if (pendingIntent != null) {
                    try {
                        alarmManager.cancel(pendingIntent)
                        pendingIntent.cancel()
                    } catch (_: Exception) {}
                }
            }
        }

        saveScheduledCalls(context, emptyList())
    }

    fun onCallTriggered(context: Context, callId: String?) {
        if (callId.isNullOrEmpty()) return
        val currentList = getScheduledCalls(context).toMutableList()
        currentList.removeAll { it.id == callId }
        saveScheduledCalls(context, currentList)
    }
}
