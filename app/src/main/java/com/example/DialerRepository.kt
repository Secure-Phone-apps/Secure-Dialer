package com.example

import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CallLog
import android.provider.ContactsContract
import android.text.format.DateFormat
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.Contact
import java.util.Date
import com.example.model.AvatarOrange
import com.example.model.AvatarBlue
import com.example.model.AvatarGreen
import com.example.model.AvatarOrangeText
import com.example.model.AvatarBlueText
import com.example.model.AvatarGreenText

// CallLog Helpers for Android OS Call Log Database
fun getContactNameFromNumber(context: Context, number: String): String? {
    if (number.isEmpty()) return null
    val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
    val cursor = context.contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)
    cursor?.use {
        if (it.moveToFirst()) {
            val nameIndex = it.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME)
            if (nameIndex != -1) return it.getString(nameIndex)
        }
    }
    return null
}

fun loadRealCallLog(context: Context): List<CallRecord> {
    val list = mutableListOf<CallRecord>()
    try {
        val cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.NUMBER,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.CACHED_NUMBER_TYPE,
                CallLog.Calls.DURATION
            ),
            null,
            null,
            CallLog.Calls.DATE + " DESC"
        )

        val colors = listOf(AvatarOrange, AvatarBlue, AvatarGreen)
        val textColors = listOf(AvatarOrangeText, AvatarBlueText, AvatarGreenText)

        cursor?.use {
            val idCol = it.getColumnIndex(CallLog.Calls._ID)
            val numCol = it.getColumnIndex(CallLog.Calls.NUMBER)
            val nameCol = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeCol = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateCol = it.getColumnIndex(CallLog.Calls.DATE)
            val numTypeCol = it.getColumnIndex(CallLog.Calls.CACHED_NUMBER_TYPE)
            val durationCol = it.getColumnIndex(CallLog.Calls.DURATION)

            while (it.moveToNext()) {
                val id = if (idCol != -1) it.getInt(idCol) else 0
                val number = if (numCol != -1) it.getString(numCol) ?: "" else ""
                val nameRaw = if (nameCol != -1) it.getString(nameCol) else null
                val name = nameRaw?.takeIf { it.isNotBlank() } ?: getContactNameFromNumber(context, number) ?: "Unknown"
                val typeInt = if (typeCol != -1) it.getInt(typeCol) else CallLog.Calls.INCOMING_TYPE
                val dateMs = if (dateCol != -1) it.getLong(dateCol) else 0L
                val numType = if (numTypeCol != -1) it.getInt(numTypeCol) else -1

                val duration = if (durationCol != -1) it.getLong(durationCol) else 0L
                val isVoicemail = typeInt == CallLog.Calls.VOICEMAIL_TYPE

                val label = when (numType) {
                    ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                    else -> "Other"
                }

                val type = when (typeInt) {
                    CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                    CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                    else -> CallType.INCOMING
                }

                val timestamp = if (dateMs > 0) {
                    DateFormat.format("MMM dd, h:mm a", Date(dateMs)).toString()
                } else {
                    "Unknown"
                }

                val avatarText = if (name != "Unknown" && name.isNotEmpty()) {
                    if (name.length >= 2) name.substring(0, 2).uppercase() else name.take(1).uppercase()
                } else {
                    "?"
                }

                val hashCodeVal = name.hashCode()
                val posHashCode = if (hashCodeVal < 0) -hashCodeVal else hashCodeVal
                val colorIdx = posHashCode % colors.size
                val avatarBg = colors[colorIdx]
                val avatarTextColor = textColors[colorIdx]

                list.add(
                    CallRecord(
                        id = id,
                        name = name,
                        number = number,
                        label = label,
                        timestamp = timestamp,
                        type = type,
                        avatarText = avatarText,
                        avatarBg = avatarBg,
                        avatarTextColor = avatarTextColor,
                        duration = duration,
                        hasVoicemail = isVoicemail
                    )
                )
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

// Contact CRUD Helpers for Android OS Contacts Database
fun loadRealContacts(context: Context): List<Contact> {
    val list = mutableListOf<Contact>()
    try {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.LABEL,
                ContactsContract.CommonDataKinds.Phone.STARRED
            ),
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )

        val colors = listOf(AvatarOrange, AvatarBlue, AvatarGreen)
        val textColors = listOf(AvatarOrangeText, AvatarBlueText, AvatarGreenText)

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val typeIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE)
            val labelIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.LABEL)
            val starredIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)

            while (it.moveToNext()) {
                val name = if (nameIndex != -1) it.getString(nameIndex) ?: "Unknown" else "Unknown"
                val number = if (numberIndex != -1) it.getString(numberIndex) ?: "" else ""
                val type = if (typeIndex != -1) it.getInt(typeIndex) else -1
                val labelStr = if (labelIndex != -1) it.getString(labelIndex) ?: "" else ""
                val favorite = if (starredIndex != -1) it.getInt(starredIndex) == 1 else false

                val label = when (type) {
                    ContactsContract.CommonDataKinds.Phone.TYPE_HOME -> "Home"
                    ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE -> "Mobile"
                    ContactsContract.CommonDataKinds.Phone.TYPE_WORK -> "Work"
                    else -> if (labelStr.isNotEmpty()) labelStr else "Mobile"
                }

                if (number.isNotEmpty() && list.none { it.number == number && it.name == name }) {
                    val colorIdx = (name.hashCode() and 0x7FFFFFFF) % colors.size
                    list.add(
                        Contact(
                            name = name,
                            number = number,
                            label = label,
                            favorite = favorite,
                            avatarBg = colors[colorIdx],
                            avatarTextColor = textColors[colorIdx]
                        )
                    )
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

fun addRealContact(context: Context, name: String, number: String, label: String): Boolean {
    return try {
        val ops = arrayListOf<ContentProviderOperation>()

        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
            .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
            .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
            .build())

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
            .build())

        val type = when (label.lowercase()) {
            "home" -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
            "work" -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
            else -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
        }

        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
            .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
            .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
            .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
            .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, type)
            .build())

        context.contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun deleteRealContact(context: Context, name: String): Boolean {
    return try {
        val resolver = context.contentResolver
        resolver.delete(
            ContactsContract.RawContacts.CONTENT_URI,
            "${ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY} = ?",
            arrayOf(name)
        )
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun updateRealContact(context: Context, oldName: String, newName: String, newNumber: String, newLabel: String): Boolean {
    return try {
        val resolver = context.contentResolver

        val cursor = resolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data.RAW_CONTACT_ID),
            "${ContactsContract.Data.DISPLAY_NAME} = ?",
            arrayOf(oldName),
            null
        )
        var rawContactId: Long? = null
        cursor?.use {
            if (it.moveToFirst()) {
                rawContactId = it.getLong(0)
            }
        }

        if (rawContactId == null) return false

        val nameValues = ContentValues().apply {
            put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, newName)
        }
        resolver.update(
            ContactsContract.Data.CONTENT_URI,
            nameValues,
            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        )

        val phoneValues = ContentValues().apply {
            put(ContactsContract.CommonDataKinds.Phone.NUMBER, newNumber)
            val type = when (newLabel.lowercase()) {
                "home" -> ContactsContract.CommonDataKinds.Phone.TYPE_HOME
                "work" -> ContactsContract.CommonDataKinds.Phone.TYPE_WORK
                else -> ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
            }
            put(ContactsContract.CommonDataKinds.Phone.TYPE, type)
        }
        resolver.update(
            ContactsContract.Data.CONTENT_URI,
            phoneValues,
            "${ContactsContract.Data.RAW_CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(rawContactId.toString(), ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
        )

        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

fun toggleRealContactFavorite(context: Context, name: String, isFavorite: Boolean): Boolean {
    return try {
        val resolver = context.contentResolver

        val cursor = resolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            arrayOf(ContactsContract.Contacts._ID),
            "${ContactsContract.Contacts.DISPLAY_NAME} = ?",
            arrayOf(name),
            null
        )
        var contactId: Long? = null
        cursor?.use {
            if (it.moveToFirst()) {
                contactId = it.getLong(0)
            }
        }

        if (contactId == null) return false

        val values = ContentValues().apply {
            put(ContactsContract.Contacts.STARRED, if (isFavorite) 1 else 0)
        }
        resolver.update(
            ContactsContract.Contacts.CONTENT_URI,
            values,
            "${ContactsContract.Contacts._ID} = ?",
            arrayOf(contactId.toString())
        )
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
