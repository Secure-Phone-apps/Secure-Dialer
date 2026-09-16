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

/**
 * Tracks SIM selection (SIM 1 vs SIM 2) for placed and logged calls.
 * Ensures consistent SIM attribution across system syncs and emulator environments.
 */
object SimCallTracker {
    private const val PREFS_NAME = "dialer_sim_tracker_prefs"
    private val memoryCache = ConcurrentHashMap<String, Int>()

    private fun hashNumber(number: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val hashBytes = digest.digest(number.toByteArray(Charsets.UTF_8))
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            number.hashCode().toString()
        }
    }

    fun recordOutgoingCall(context: Context, number: String, simSlot: Int, timestampMs: Long = System.currentTimeMillis()) {
        val clean = number.filter { it.isDigit() }
        val minuteBucket = timestampMs / 60000L

        if (clean.isNotEmpty()) {
            memoryCache["${clean}_${minuteBucket}"] = simSlot
        }

        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = prefs.edit()
            if (clean.isNotEmpty()) {
                val hashed = hashNumber(clean)
                editor.putInt("sim_${hashed}_${minuteBucket}", simSlot)
            }
            editor.putInt("sim_time_${timestampMs}", simSlot)
            editor.apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getSimSlotForCall(context: Context, number: String, timestampMs: Long): Int? {
        val clean = number.filter { it.isDigit() }
        val minuteBucket = timestampMs / 60000L

        // 1. Check in-memory cache for exact minute bucket
        if (clean.isNotEmpty()) {
            memoryCache["${clean}_${minuteBucket}"]?.let { return it }
        }

        // 2. Check SharedPreferences for exact minute bucket or timestamp
        try {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            if (clean.isNotEmpty()) {
                val hashed = hashNumber(clean)
                val direct = prefs.getInt("sim_${hashed}_${minuteBucket}", 0)
                if (direct in 1..2) {
                    memoryCache["${clean}_${minuteBucket}"] = direct
                    return direct
                }
                // Fallback for pre-migration entries
                val legacyDirect = prefs.getInt("sim_${clean}_${minuteBucket}", 0)
                if (legacyDirect in 1..2) {
                    memoryCache["${clean}_${minuteBucket}"] = legacyDirect
                    return legacyDirect
                }
            }
            val timeDirect = prefs.getInt("sim_time_${timestampMs}", 0)
            if (timeDirect in 1..2) {
                if (clean.isNotEmpty()) {
                    memoryCache["${clean}_${minuteBucket}"] = timeDirect
                }
                return timeDirect
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun isMultiSimActive(context: Context): Boolean {
        try {
            val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            if (subManager != null) {
                val count = try {
                    subManager.activeSubscriptionInfoCount
                } catch (e: SecurityException) {
                    0
                }
                if (count > 1) return true
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }
}
