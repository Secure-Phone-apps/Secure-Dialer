package com.example

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract
import kotlinx.coroutines.runBlocking

fun getContactNameFromNumber(context: Context, number: String): String? {
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
    
    // Fallback: Query our local database contacts using robust matching
    try {
        val db = com.example.data.AppDatabase.getDatabase(context)
        val dao = db.dialerDao()
        val cleanInput = number.filter { it.isDigit() }
        if (cleanInput.isNotEmpty()) {
            val contacts = runBlocking { dao.getAllContactsList() }
            val match = contacts.firstOrNull { contact ->
                val cleanContact = contact.number.filter { it.isDigit() }
                if (cleanContact.isEmpty()) false
                else if (cleanInput == cleanContact) true
                else {
                    val minLen = minOf(cleanInput.length, cleanContact.length)
                    if (minLen >= 7) {
                        val matchLen = if (minLen >= 10) 10 else if (minLen >= 8) 8 else 7
                        cleanInput.takeLast(matchLen) == cleanContact.takeLast(matchLen)
                    } else false
                }
            }
            if (match != null) return match.name
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}

fun getSavedCnapName(context: Context, number: String): String? {
    if (number.isBlank()) return null
    val cleanInput = number.filter { it.isDigit() }
    if (cleanInput.isEmpty()) return null
    try {
        val db = com.example.data.AppDatabase.getDatabase(context)
        val dao = db.dialerDao()
        val allSettings = runBlocking { dao.getAllSettingsList() }
        val cnapPrefix = "cnap_"
        val cnapMap = allSettings.filter { it.key.startsWith(cnapPrefix) }
            .associate { it.key.substring(cnapPrefix.length) to it.value }
        
        val match = cnapMap.entries.firstOrNull { entry ->
            val cleanKey = entry.key.filter { it.isDigit() }
            if (cleanKey.isEmpty()) false
            else if (cleanInput == cleanKey) true
            else {
                val minLen = minOf(cleanInput.length, cleanKey.length)
                if (minLen >= 7) {
                    val matchLen = if (minLen >= 10) 10 else if (minLen >= 8) 8 else 7
                    cleanInput.takeLast(matchLen) == cleanKey.takeLast(matchLen)
                } else false
            }
        }
        if (match != null && match.value.isNotBlank()) {
            return match.value
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return null
}
