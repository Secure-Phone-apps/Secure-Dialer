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
import android.provider.ContactsContract

object ContactCache {
    @Volatile
    private var fullNumMap = HashMap<String, com.example.model.Contact>()
    @Volatile
    private var suffix10Map = HashMap<String, com.example.model.Contact>()
    @Volatile
    private var suffix8Map = HashMap<String, com.example.model.Contact>()
    @Volatile
    private var suffix7Map = HashMap<String, com.example.model.Contact>()

    @Volatile
    private var cnapMap = HashMap<String, String>()
    @Volatile
    private var cnapSuffix10Map = HashMap<String, String>()
    @Volatile
    private var cnapSuffix8Map = HashMap<String, String>()
    @Volatile
    private var cnapSuffix7Map = HashMap<String, String>()

    fun init(contacts: List<com.example.model.Contact>, settings: List<com.example.model.AppSetting>) {
        val fMap = HashMap<String, com.example.model.Contact>()
        val s10Map = HashMap<String, com.example.model.Contact>()
        val s8Map = HashMap<String, com.example.model.Contact>()
        val s7Map = HashMap<String, com.example.model.Contact>()

        for (contact in contacts) {
            val allNums = contact.getAllNumbers()
            for (labeledNum in allNums) {
                val clean = labeledNum.number.filter { it.isDigit() }
                if (clean.isEmpty()) continue
                fMap[clean] = contact
                val len = clean.length
                if (len >= 10) s10Map[clean.takeLast(10)] = contact
                if (len >= 8) s8Map[clean.takeLast(8)] = contact
                if (len >= 7) s7Map[clean.takeLast(7)] = contact
            }
        }

        fullNumMap = fMap
        suffix10Map = s10Map
        suffix8Map = s8Map
        suffix7Map = s7Map

        initCnapFromSettings(settings)
    }

    fun initCnapFromSettings(settings: List<com.example.model.AppSetting>) {
        val cMap = HashMap<String, String>()
        val cs10Map = HashMap<String, String>()
        val cs8Map = HashMap<String, String>()
        val cs7Map = HashMap<String, String>()
        val cnapPrefix = "cnap_"

        for (setting in settings) {
            if (setting.key.startsWith(cnapPrefix)) {
                val cleanKey = setting.key.substring(cnapPrefix.length).filter { it.isDigit() }
                if (cleanKey.isNotEmpty()) {
                    val name = setting.value
                    cMap[cleanKey] = name
                    val len = cleanKey.length
                    if (len >= 10) cs10Map[cleanKey.takeLast(10)] = name
                    if (len >= 8) cs8Map[cleanKey.takeLast(8)] = name
                    if (len >= 7) cs7Map[cleanKey.takeLast(7)] = name
                }
            }
        }

        cnapMap = cMap
        cnapSuffix10Map = cs10Map
        cnapSuffix8Map = cs8Map
        cnapSuffix7Map = cs7Map
    }

    @Synchronized
    fun putCnapName(number: String, cnapName: String) {
        val clean = number.filter { it.isDigit() }
        if (clean.isEmpty() || cnapName.isBlank()) return

        val newCnapMap = HashMap(cnapMap)
        val newS10Map = HashMap(cnapSuffix10Map)
        val newS8Map = HashMap(cnapSuffix8Map)
        val newS7Map = HashMap(cnapSuffix7Map)

        newCnapMap[clean] = cnapName
        val len = clean.length
        if (len >= 10) newS10Map[clean.takeLast(10)] = cnapName
        if (len >= 8) newS8Map[clean.takeLast(8)] = cnapName
        if (len >= 7) newS7Map[clean.takeLast(7)] = cnapName

        cnapMap = newCnapMap
        cnapSuffix10Map = newS10Map
        cnapSuffix8Map = newS8Map
        cnapSuffix7Map = newS7Map
    }

    fun getContact(number: String): com.example.model.Contact? {
        val clean = number.filter { it.isDigit() }
        if (clean.isEmpty()) return null
        val len = clean.length
        return fullNumMap[clean] ?: when {
            len >= 10 -> suffix10Map[clean.takeLast(10)]
            len >= 8 -> suffix8Map[clean.takeLast(8)]
            len >= 7 -> suffix7Map[clean.takeLast(7)]
            else -> null
        }
    }

    fun getCnapName(number: String): String? {
        val clean = number.filter { it.isDigit() }
        if (clean.isEmpty()) return null
        val len = clean.length
        return cnapMap[clean] ?: when {
            len >= 10 -> cnapSuffix10Map[clean.takeLast(10)]
            len >= 8 -> cnapSuffix8Map[clean.takeLast(8)]
            len >= 7 -> cnapSuffix7Map[clean.takeLast(7)]
            else -> null
        }
    }
}

fun getContactNameFromNumber(context: Context, number: String): String? {
    if (number.isBlank()) return null
    
    // 1. O(1) in-memory cache lookup (Ultra-fast, zero-IO, non-blocking)
    val cached = ContactCache.getContact(number)
    if (cached != null) return cached.name

    // 2. Fast query of the system provider (ContentResolver)
    val attributionContext = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
        context.createAttributionContext("default")
    } else {
        context
    }
    try {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        attributionContext.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) return cursor.getString(0)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    return null
}

suspend fun getSavedCnapName(context: Context, number: String): String? {
    if (number.isBlank()) return null
    val memoryName = ContactCache.getCnapName(number)
    if (!memoryName.isNullOrBlank()) return memoryName

    return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        try {
            val db = com.example.data.AppDatabase.getDatabase(context)
            val settings = db.dialerDao().getAllSettingsList()
            ContactCache.initCnapFromSettings(settings)
            ContactCache.getCnapName(number)
        } catch (e: Exception) {
            null
        }
    }
}

fun getSavedCnapNameSync(context: Context, number: String): String? {
    if (number.isBlank()) return null
    val memoryName = ContactCache.getCnapName(number)
    if (!memoryName.isNullOrBlank()) return memoryName

    return try {
        val db = com.example.data.AppDatabase.getDatabase(context)
        val settings = kotlinx.coroutines.runBlocking { db.dialerDao().getAllSettingsList() }
        ContactCache.initCnapFromSettings(settings)
        ContactCache.getCnapName(number)
    } catch (e: Exception) {
        null
    }
}
