package com.example

import android.content.Context
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.example.model.AppSetting
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.Contact
import com.example.model.getInitials
import com.example.ui.theme.AvatarBlue
import com.example.ui.theme.AvatarBlueText
import com.example.ui.theme.AvatarGreen
import com.example.ui.theme.AvatarGreenText
import com.example.ui.theme.AvatarOrange
import com.example.ui.theme.AvatarOrangeText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun nameToT9(name: String): String {
    return name.uppercase().map { char ->
        when (char) {
            in 'A'..'C' -> '2'
            in 'D'..'F' -> '3'
            in 'G'..'I' -> '4'
            in 'J'..'L' -> '5'
            in 'M'..'O' -> '6'
            in 'P'..'S' -> '7'
            in 'T'..'V' -> '8'
            in 'W'..'Z' -> '9'
            else -> char
        }
    }.joinToString("")
}

suspend fun DialerRepository.fetchSystemContacts(): List<Contact> = withContext(Dispatchers.IO) {
    val contacts = mutableListOf<Contact>()
    val colors = listOf(AvatarBlue to AvatarBlueText, AvatarOrange to AvatarOrangeText, AvatarGreen to AvatarGreenText)

    val emailMap = mutableMapOf<String, String>()
    try {
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Email.CONTACT_ID, ContactsContract.CommonDataKinds.Email.ADDRESS),
            null, null, null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
            val addrIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            while (cursor.moveToNext()) {
                if (idIdx != -1 && addrIdx != -1) {
                    val cid = cursor.getString(idIdx) ?: ""
                    val email = cursor.getString(addrIdx) ?: ""
                    if (cid.isNotEmpty() && email.isNotEmpty()) {
                        emailMap[cid] = email
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    try {
        context.contentResolver.query(
            Phone.CONTENT_URI,
            arrayOf(Phone.DISPLAY_NAME, Phone.NUMBER, Phone.STARRED, Phone.PHOTO_THUMBNAIL_URI, Phone.CONTACT_ID),
            null, null, "${Phone.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndex(Phone.NUMBER)
            val favIdx = cursor.getColumnIndex(Phone.STARRED)
            val photoIdx = cursor.getColumnIndex(Phone.PHOTO_THUMBNAIL_URI)
            val cidIdx = cursor.getColumnIndex(Phone.CONTACT_ID)
            while (cursor.moveToNext()) {
                val num = cursor.getString(numIdx) ?: ""
                val rawName = cursor.getString(nameIdx)
                val name = if (rawName.isNullOrBlank()) (if (num.isBlank()) "Unknown" else num) else rawName
                val fav = cursor.getInt(favIdx) == 1
                val photoUri = if (photoIdx != -1) cursor.getString(photoIdx) ?: "" else ""
                val contactId = if (cidIdx != -1) cursor.getString(cidIdx) ?: "" else ""
                val email = emailMap[contactId] ?: ""
                val pair = colors[Math.abs(name.hashCode()) % colors.size]
                contacts.add(Contact(num, name, "Mobile", fav, getInitials(name), pair.first.value.toLong(), pair.second.value.toLong(), nameToT9(name), email, photoUri))
            }
        }
    } catch (e: SecurityException) {
        e.printStackTrace()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    contacts.distinctBy { it.number }
}

suspend fun DialerRepository.fetchSystemCallLogs(): List<CallRecord> = withContext(Dispatchers.IO) {
    val logs = mutableListOf<CallRecord>()
    val colors = listOf(AvatarBlue to AvatarBlueText, AvatarOrange to AvatarOrangeText, AvatarGreen to AvatarGreenText)
    val sdf = SimpleDateFormat("MMM d, HH:mm", com.example.ui.components.getCurrentLocale(context))

    val allContacts = try {
        dao.getAllContactsList()
    } catch (e: Exception) {
        emptyList<Contact>()
    }

    val allSettings = try {
        dao.getAllSettingsList()
    } catch (e: Exception) {
        emptyList<AppSetting>()
    }

    val cnapPrefix = "cnap_"
    val cnapMap = allSettings.filter { it.key.startsWith(cnapPrefix) }
        .associate { it.key.substring(cnapPrefix.length) to it.value }

    val fullNumMap = HashMap<String, Contact>()
    val suffix10Map = HashMap<String, Contact>()
    val suffix8Map = HashMap<String, Contact>()
    val suffix7Map = HashMap<String, Contact>()

    for (contact in allContacts) {
        val clean = contact.number.filter { it.isDigit() }
        if (clean.isEmpty()) continue
        fullNumMap[clean] = contact
        val len = clean.length
        if (len >= 10) suffix10Map[clean.takeLast(10)] = contact
        if (len >= 8) suffix8Map[clean.takeLast(8)] = contact
        if (len >= 7) suffix7Map[clean.takeLast(7)] = contact
    }

    val cnapIndex = HashMap<String, String>()
    for (entry in cnapMap.entries) {
        val cleanKey = entry.key.filter { it.isDigit() }
        if (cleanKey.isNotEmpty()) {
            cnapIndex[cleanKey] = entry.value
        }
    }

    try {
        val queryUri = CallLog.Calls.CONTENT_URI.buildUpon()
            .appendQueryParameter("limit", "200")
            .build()
        context.contentResolver.query(
            queryUri,
            arrayOf(CallLog.Calls._ID, CallLog.Calls.CACHED_NAME, CallLog.Calls.NUMBER, CallLog.Calls.TYPE, CallLog.Calls.DATE, CallLog.Calls.DURATION),
            null, null, "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
            val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
            val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)

            while (cursor.moveToNext()) {
                val num = if (numIdx != -1) cursor.getString(numIdx) ?: "" else ""
                val cachedName = if (nameIdx != -1) cursor.getString(nameIdx) else null
                val cleanNum = num.filter { it.isDigit() }

                val matchingContact = if (cleanNum.isNotEmpty()) {
                    val len = cleanNum.length
                    fullNumMap[cleanNum] ?: when {
                        len >= 10 -> suffix10Map[cleanNum.takeLast(10)]
                        len >= 8 -> suffix8Map[cleanNum.takeLast(8)]
                        len >= 7 -> suffix7Map[cleanNum.takeLast(7)]
                        else -> null
                    }
                } else null

                val matchingCnapName = if (cleanNum.isNotEmpty()) {
                    cnapIndex[cleanNum]
                } else null

                val name = when {
                    matchingContact != null -> matchingContact.name
                    !cachedName.isNullOrBlank() -> cachedName
                    !matchingCnapName.isNullOrBlank() -> matchingCnapName
                    num.isBlank() -> "Unknown"
                    else -> num
                }

                val typeVal = if (typeIdx != -1) cursor.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                val type = when (typeVal) {
                    CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                    CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                    else -> CallType.INCOMING
                }
                val idVal = if (idIdx != -1) cursor.getInt(idIdx) else 0
                val dateVal = if (dateIdx != -1) cursor.getLong(dateIdx) else 0L
                val durVal = if (durIdx != -1) cursor.getLong(durIdx) else 0L

                val pair = colors[Math.abs(name.hashCode()) % colors.size]
                val photoUriVal = matchingContact?.photoUri ?: ""
                logs.add(CallRecord(idVal, name, num, "Mobile", sdf.format(Date(dateVal)), type, getInitials(name), pair.first.value.toLong(), pair.second.value.toLong(), durVal, false, photoUriVal))
            }
        }
    } catch (e: SecurityException) {
        e.printStackTrace()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    logs
}
