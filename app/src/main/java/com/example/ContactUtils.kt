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

    fun init(contacts: List<com.example.model.Contact>, settings: List<com.example.model.AppSetting>) {
        val fMap = HashMap<String, com.example.model.Contact>()
        val s10Map = HashMap<String, com.example.model.Contact>()
        val s8Map = HashMap<String, com.example.model.Contact>()
        val s7Map = HashMap<String, com.example.model.Contact>()

        for (contact in contacts) {
            val clean = contact.number.filter { it.isDigit() }
            if (clean.isEmpty()) continue
            fMap[clean] = contact
            val len = clean.length
            if (len >= 10) s10Map[clean.takeLast(10)] = contact
            if (len >= 8) s8Map[clean.takeLast(8)] = contact
            if (len >= 7) s7Map[clean.takeLast(7)] = contact
        }

        val cMap = HashMap<String, String>()
        val cnapPrefix = "cnap_"
        for (setting in settings) {
            if (setting.key.startsWith(cnapPrefix)) {
                val cleanKey = setting.key.substring(cnapPrefix.length).filter { it.isDigit() }
                if (cleanKey.isNotEmpty()) {
                    cMap[cleanKey] = setting.value
                }
            }
        }

        fullNumMap = fMap
        suffix10Map = s10Map
        suffix8Map = s8Map
        suffix7Map = s7Map
        cnapMap = cMap
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
        return cnapMap[clean]
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

fun getSavedCnapName(context: Context, number: String): String? {
    if (number.isBlank()) return null
    return ContactCache.getCnapName(number)
}
