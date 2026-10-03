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

package com.example

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import com.example.data.AppDatabase
import com.example.model.AppSetting
import com.example.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

object ContactCache {
    private val fullNumMap = ConcurrentHashMap<String, Contact>()
    private val suffix10Map = ConcurrentHashMap<String, Contact>()
    private val suffix8Map = ConcurrentHashMap<String, Contact>()
    private val suffix7Map = ConcurrentHashMap<String, Contact>()

    private val cnapMap = ConcurrentHashMap<String, String>()
    private val cnapSuffix10Map = ConcurrentHashMap<String, String>()
    private val cnapSuffix8Map = ConcurrentHashMap<String, String>()
    private val cnapSuffix7Map = ConcurrentHashMap<String, String>()

    fun init(contacts: List<Contact>, settings: List<AppSetting>) {
        fullNumMap.clear()
        suffix10Map.clear()
        suffix8Map.clear()
        suffix7Map.clear()

        for (contact in contacts) {
            for (labeledNum in contact.getAllNumbers()) {
                val clean = labeledNum.number.filter { it.isDigit() }
                if (clean.isEmpty()) continue
                fullNumMap[clean] = contact
                val len = clean.length
                if (len >= 10) suffix10Map[clean.takeLast(10)] = contact
                if (len >= 8) suffix8Map[clean.takeLast(8)] = contact
                if (len >= 7) suffix7Map[clean.takeLast(7)] = contact
            }
        }
        initCnapFromSettings(settings)
    }

    fun initCnapFromSettings(settings: List<AppSetting>) {
        cnapMap.clear()
        cnapSuffix10Map.clear()
        cnapSuffix8Map.clear()
        cnapSuffix7Map.clear()
        val prefix = "cnap_"

        for (setting in settings) {
            if (setting.key.startsWith(prefix)) {
                val cleanKey = setting.key.substring(prefix.length).filter { it.isDigit() }
                if (cleanKey.isNotEmpty()) {
                    val name = setting.value
                    cnapMap[cleanKey] = name
                    val len = cleanKey.length
                    if (len >= 10) cnapSuffix10Map[cleanKey.takeLast(10)] = name
                    if (len >= 8) cnapSuffix8Map[cleanKey.takeLast(8)] = name
                    if (len >= 7) cnapSuffix7Map[cleanKey.takeLast(7)] = name
                }
            }
        }
    }

    fun putCnapName(number: String, cnapName: String) {
        val clean = number.filter { it.isDigit() }
        if (clean.isEmpty() || cnapName.isBlank()) return

        cnapMap[clean] = cnapName
        val len = clean.length
        if (len >= 10) cnapSuffix10Map[clean.takeLast(10)] = cnapName
        if (len >= 8) cnapSuffix8Map[clean.takeLast(8)] = cnapName
        if (len >= 7) cnapSuffix7Map[clean.takeLast(7)] = cnapName
    }

    fun getContact(number: String): Contact? {
        val clean = number.filter { it.isDigit() }
        if (clean.isEmpty()) return null
        val len = clean.length

        // FIXED: Resilient cascading fallback (does not short-circuit on null 10-digit matches)
        return fullNumMap[clean]
            ?: (if (len >= 10) suffix10Map[clean.takeLast(10)] else null)
            ?: (if (len >= 8) suffix8Map[clean.takeLast(8)] else null)
            ?: (if (len >= 7) suffix7Map[clean.takeLast(7)] else null)
    }

    fun getCnapName(number: String): String? {
        val clean = number.filter { it.isDigit() }
        if (clean.isEmpty()) return null
        val len = clean.length

        // FIXED: Resilient cascading fallback
        return cnapMap[clean]
            ?: (if (len >= 10) cnapSuffix10Map[clean.takeLast(10)] else null)
            ?: (if (len >= 8) cnapSuffix8Map[clean.takeLast(8)] else null)
            ?: (if (len >= 7) cnapSuffix7Map[clean.takeLast(7)] else null)
    }
}

fun getContactNameFromNumber(context: Context, number: String): String? {
    if (number.isBlank()) return null

    // 1. O(1) in-memory cache lookup (non-blocking)
    ContactCache.getContact(number)?.name?.let { return it }

    // 2. Query System Contacts Provider
    return try {
        val safeContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try { context.createAttributionContext("default") } catch (_: Exception) { context }
        } else {
            context
        }
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        safeContext.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }
    } catch (_: Exception) {
        null
    }
}

suspend fun getSavedCnapName(context: Context, number: String): String? {
    if (number.isBlank()) return null
    ContactCache.getCnapName(number)?.let { return it }

    return withContext(Dispatchers.IO) {
        try {
            val clean = number.filter { it.isDigit() }
            val db = AppDatabase.getDatabase(context)
            // FIXED: Targeted single-row lookup instead of dumping the whole settings table
            val direct = db.dialerDao().getSetting("cnap_$clean")
            if (!direct.isNullOrBlank()) {
                ContactCache.putCnapName(clean, direct)
                return@withContext direct
            }
            val settings = db.dialerDao().getAllSettingsList()
            ContactCache.initCnapFromSettings(settings)
            ContactCache.getCnapName(number)
        } catch (_: Exception) {
            null
        }
    }
}

fun getSavedCnapNameSync(context: Context, number: String): String? {
    if (number.isBlank()) return null
    // FIXED: Non-blocking in-memory resolution only on synchronous Telephony path
    return ContactCache.getCnapName(number)
}