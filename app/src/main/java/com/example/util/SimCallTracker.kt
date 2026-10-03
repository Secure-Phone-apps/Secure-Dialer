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

import android.content.Context
import android.telephony.SubscriptionManager
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

/**
 * Tracks SIM selection (SIM 1 vs SIM 2) for placed and logged calls.
 * Ensures consistent SIM attribution across system syncs with automatic
 * minute-boundary bridging and storage pruning.
 */
object SimCallTracker {
    private const val PREFS_NAME = "dialer_sim_tracker_prefs"
    private const val MAX_RETENTION_MS = 48 * 60 * 60 * 1000L // 48-hour retention prevents unbounded XML disk growth
    private val memoryCache = ConcurrentHashMap<String, Int>()

    private fun hashNumber(number: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(number.toByteArray(Charsets.UTF_8))
            buildString(hashBytes.size * 2) {
                for (b in hashBytes) {
                    append(String.format("%02x", b))
                }
            }
        } catch (_: Exception) {
            number.hashCode().toString()
        }
    }

    fun recordOutgoingCall(context: Context, number: String, simSlot: Int, timestampMs: Long = System.currentTimeMillis()) {
        val clean = number.filter { it.isDigit() }
        val minuteBucket = timestampMs / 60000L

        if (clean.isNotEmpty()) {
            if (memoryCache.size > 150) memoryCache.clear()
            memoryCache["${clean}_$minuteBucket"] = simSlot
        }

        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()

            if (clean.isNotEmpty()) {
                val hashed = hashNumber(clean)
                editor.putInt("sim_${hashed}_$minuteBucket", simSlot)
            }
            editor.putInt("sim_time_$timestampMs", simSlot)
            editor.putLong("sim_ts_$timestampMs", timestampMs)

            // AUTO-PRUNE: Periodically cleans up keys older than 48 hours to keep XML < 10KB
            val allEntries = prefs.all
            if (allEntries.size > 80) {
                val cutoff = System.currentTimeMillis() - MAX_RETENTION_MS
                for ((key, value) in allEntries) {
                    if (key.startsWith("sim_ts_") && (value as? Long ?: 0L) < cutoff) {
                        val ts = key.removePrefix("sim_ts_")
                        editor.remove(key)
                        editor.remove("sim_time_$ts")
                    }
                }
            }
            editor.apply()
        } catch (_: Exception) {}
    }

    fun getSimSlotForCall(context: Context, number: String, timestampMs: Long): Int? {
        val clean = number.filter { it.isDigit() }
        val minuteBucket = timestampMs / 60000L

        // FIXED: Check current minute and adjacent +/- 1 minute buckets to eliminate network setup latency desyncs
        val candidateBuckets = listOf(minuteBucket, minuteBucket - 1, minuteBucket + 1)

        // 1. Check in-memory cache
        if (clean.isNotEmpty()) {
            for (bucket in candidateBuckets) {
                memoryCache["${clean}_$bucket"]?.let { return it }
            }
        }

        // 2. Check SharedPreferences
        try {
            val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (clean.isNotEmpty()) {
                val hashed = hashNumber(clean)
                for (bucket in candidateBuckets) {
                    val direct = prefs.getInt("sim_${hashed}_$bucket", 0)
                    if (direct in 1..2) {
                        memoryCache["${clean}_$minuteBucket"] = direct
                        return direct
                    }
                    val legacyDirect = prefs.getInt("sim_${clean}_$bucket", 0)
                    if (legacyDirect in 1..2) {
                        memoryCache["${clean}_$minuteBucket"] = legacyDirect
                        return legacyDirect
                    }
                }
            }

            // Check exact or near timestamp (within 15 seconds)
            val timeDirect = prefs.getInt("sim_time_$timestampMs", 0)
            if (timeDirect in 1..2) return timeDirect

            for ((key, value) in prefs.all) {
                if (key.startsWith("sim_time_") && value is Int && value in 1..2) {
                    val recordedTs = key.removePrefix("sim_time_").toLongOrNull() ?: continue
                    if (abs(recordedTs - timestampMs) <= 15000L) {
                        return value
                    }
                }
            }
        } catch (_: Exception) {}

        return null
    }

    fun isMultiSimActive(context: Context): Boolean {
        return try {
            val subManager = context.applicationContext.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            val count = subManager?.activeSubscriptionInfoList?.size ?: 0
            count > 1
        } catch (_: Exception) {
            false
        }
    }
}