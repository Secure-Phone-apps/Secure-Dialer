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

import android.accounts.AccountManager
import android.content.Context
import android.database.Cursor
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.telephony.SubscriptionManager
import com.example.model.*
import com.example.ui.theme.*
import com.example.util.CallAudioRecorder
import com.example.util.MultiSimManager
import com.example.util.SimCallTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Normalizer
import java.util.Locale

internal fun nameToT9(name: String): String {
    // FIXED: Strips diacritics/accents (e.g. É -> E, Ç -> C) for global T9 search support
    val normalized = Normalizer.normalize(name, Normalizer.Form.NFD)
        .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        .uppercase(Locale.ROOT)

    return buildString(normalized.length) {
        for (char in normalized) {
            append(
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
            )
        }
    }
}

internal fun getNormalizedPhoneNumberKey(number: String): String {
    val digits = number.filter { it.isDigit() }
    return if (digits.length >= 10) digits.takeLast(10) else digits
}

internal fun getPhoneNumberQualityScore(number: String): Int {
    var score = 0
    val trimmed = number.trim()
    if (trimmed.startsWith("+")) score += 100
    if (trimmed.contains(" ")) score += 10
    if (!trimmed.startsWith("0")) score += 5
    return score
}

suspend fun DialerRepository.fetchSystemContacts(): List<Contact> = withContext(Dispatchers.IO) {
    val contactMap = LinkedHashMap<String, Contact>()
    val colors = listOf(AvatarBlue to AvatarBlueText, AvatarOrange to AvatarOrangeText, AvatarGreen to AvatarGreenText)

    val emailMap = mutableMapOf<String, MutableList<LabeledEmail>>()
    try {
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Email.CONTACT_ID,
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.TYPE,
                ContactsContract.CommonDataKinds.Email.LABEL
            ),
            null, null, null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.CONTACT_ID)
            val addrIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS)
            val typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.TYPE)
            val labelIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.LABEL)
            while (cursor.moveToNext()) {
                if (idIdx != -1 && addrIdx != -1) {
                    val cid = cursor.getString(idIdx) ?: ""
                    val email = cursor.getString(addrIdx) ?: ""
                    val type = if (typeIdx != -1) cursor.getInt(typeIdx) else ContactsContract.CommonDataKinds.Email.TYPE_HOME
                    val label = if (type == ContactsContract.CommonDataKinds.Email.TYPE_CUSTOM && labelIdx != -1) {
                        cursor.getString(labelIdx) ?: "Custom"
                    } else when (type) {
                        ContactsContract.CommonDataKinds.Email.TYPE_WORK -> "Work"
                        ContactsContract.CommonDataKinds.Email.TYPE_OTHER -> "Other"
                        else -> "Home"
                    }
                    if (cid.isNotEmpty() && email.isNotEmpty()) {
                        val list = emailMap.getOrPut(cid) { mutableListOf() }
                        if (list.none { it.email.equals(email, ignoreCase = true) }) {
                            list.add(LabeledEmail(email = email, label = label))
                        }
                    }
                }
            }
        }
    } catch (_: Exception) {}

    val addressMap = mutableMapOf<String, MutableList<LabeledAddress>>()
    try {
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID,
                ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
                ContactsContract.CommonDataKinds.StructuredPostal.TYPE,
                ContactsContract.CommonDataKinds.StructuredPostal.LABEL
            ),
            null, null, null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.CONTACT_ID)
            val addrIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS)
            val typeIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.TYPE)
            val labelIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.StructuredPostal.LABEL)
            while (cursor.moveToNext()) {
                if (idIdx != -1 && addrIdx != -1) {
                    val cid = cursor.getString(idIdx) ?: ""
                    val addr = cursor.getString(addrIdx) ?: ""
                    val type = if (typeIdx != -1) cursor.getInt(typeIdx) else ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME
                    val label = if (type == ContactsContract.CommonDataKinds.StructuredPostal.TYPE_CUSTOM && labelIdx != -1) {
                        cursor.getString(labelIdx) ?: "Custom"
                    } else when (type) {
                        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_WORK -> "Work"
                        ContactsContract.CommonDataKinds.StructuredPostal.TYPE_OTHER -> "Other"
                        else -> "Home"
                    }
                    if (cid.isNotEmpty() && addr.isNotEmpty()) {
                        val list = addressMap.getOrPut(cid) { mutableListOf() }
                        if (list.none { it.address.equals(addr, ignoreCase = true) }) {
                            list.add(LabeledAddress(address = addr, label = label))
                        }
                    }
                }
            }
        }
    } catch (_: Exception) {}

    val noteMap = mutableMapOf<String, String>()
    try {
        context.contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(
                ContactsContract.Data.CONTACT_ID,
                ContactsContract.CommonDataKinds.Note.NOTE
            ),
            "${ContactsContract.Data.MIMETYPE} = ?",
            arrayOf(ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE),
            null
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(ContactsContract.Data.CONTACT_ID)
            val noteIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Note.NOTE)
            while (cursor.moveToNext()) {
                if (idIdx != -1 && noteIdx != -1) {
                    val cid = cursor.getString(idIdx) ?: ""
                    val noteVal = cursor.getString(noteIdx)?.trim() ?: ""
                    if (cid.isNotEmpty() && noteVal.isNotEmpty()) {
                        val existing = noteMap[cid]
                        noteMap[cid] = if (existing.isNullOrBlank()) noteVal else "$existing\n$noteVal"
                    }
                }
            }
        }
    } catch (_: Exception) {}

    // Safe cursor query that falls back to standard projection if OEM ROM rejects RawContacts columns
    val primaryProjection = arrayOf(
        Phone._ID,
        Phone.RAW_CONTACT_ID,
        Phone.CONTACT_ID,
        Phone.DISPLAY_NAME,
        Phone.NUMBER,
        Phone.STARRED,
        Phone.PHOTO_THUMBNAIL_URI,
        Phone.TYPE,
        Phone.LABEL,
        ContactsContract.RawContacts.ACCOUNT_NAME,
        ContactsContract.RawContacts.ACCOUNT_TYPE
    )

    val safeFallbackProjection = arrayOf(
        Phone._ID,
        Phone.RAW_CONTACT_ID,
        Phone.CONTACT_ID,
        Phone.DISPLAY_NAME,
        Phone.NUMBER,
        Phone.STARRED,
        Phone.PHOTO_THUMBNAIL_URI,
        Phone.TYPE,
        Phone.LABEL
    )

    var cursor: Cursor? = null
    try {
        cursor = context.contentResolver.query(Phone.CONTENT_URI, primaryProjection, null, null, "${Phone.DISPLAY_NAME} ASC")
    } catch (_: Exception) {
        try {
            cursor = context.contentResolver.query(Phone.CONTENT_URI, safeFallbackProjection, null, null, "${Phone.DISPLAY_NAME} ASC")
        } catch (_: Exception) {}
    }

    cursor?.use { c ->
        val idIdx = c.getColumnIndex(Phone._ID)
        val rawIdIdx = c.getColumnIndex(Phone.RAW_CONTACT_ID)
        val cidIdx = c.getColumnIndex(Phone.CONTACT_ID)
        val nameIdx = c.getColumnIndex(Phone.DISPLAY_NAME)
        val numIdx = c.getColumnIndex(Phone.NUMBER)
        val favIdx = c.getColumnIndex(Phone.STARRED)
        val photoIdx = c.getColumnIndex(Phone.PHOTO_THUMBNAIL_URI)
        val typeIdx = c.getColumnIndex(Phone.TYPE)
        val labelIdx = c.getColumnIndex(Phone.LABEL)
        val accNameIdx = c.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
        val accTypeIdx = c.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)

        while (c.moveToNext()) {
            val idVal = if (idIdx != -1) c.getLong(idIdx) else 0L
            val rawIdVal = if (rawIdIdx != -1) c.getLong(rawIdIdx) else 0L
            val contactIdVal = if (cidIdx != -1) c.getLong(cidIdx) else 0L
            val num = if (numIdx != -1) c.getString(numIdx) ?: "" else ""
            val rawName = if (nameIdx != -1) c.getString(nameIdx) else null
            val name = if (rawName.isNullOrBlank()) (if (num.isBlank()) "Unknown" else num) else rawName
            val fav = favIdx != -1 && c.getInt(favIdx) == 1
            val photoUri = if (photoIdx != -1) c.getString(photoIdx) ?: "" else ""
            val contactIdStr = if (cidIdx != -1) c.getString(cidIdx) ?: "" else ""
            val emailsForContact = emailMap[contactIdStr] ?: emptyList()
            val addressesForContact = addressMap[contactIdStr] ?: emptyList()
            val noteForContact = noteMap[contactIdStr] ?: ""
            val accName = if (accNameIdx != -1) c.getString(accNameIdx) ?: "" else ""
            val accType = if (accTypeIdx != -1) c.getString(accTypeIdx) ?: "" else ""
            val phoneType = if (typeIdx != -1) c.getInt(typeIdx) else Phone.TYPE_MOBILE
            val phoneLabel = if (phoneType == Phone.TYPE_CUSTOM && labelIdx != -1) {
                c.getString(labelIdx) ?: "Custom"
            } else when (phoneType) {
                Phone.TYPE_HOME -> "Home"
                Phone.TYPE_WORK -> "Work"
                Phone.TYPE_OTHER -> "Other"
                else -> "Mobile"
            }

            val pair = colors[Math.abs(name.hashCode()) % colors.size]
            val uniqueId = if (contactIdVal != 0L) contactIdVal else if (idVal != 0L) idVal else (contactMap.size + 1).toLong()
            val personKey = if (contactIdVal != 0L) "c-$contactIdVal" else if (rawIdVal != 0L) "r-$rawIdVal" else "n-${name.lowercase(Locale.ROOT).trim()}"
            val labeledNum = LabeledNumber(number = num, label = phoneLabel, isPrimary = true)

            val existing = contactMap[personKey]
            if (existing == null) {
                contactMap[personKey] = Contact(
                    id = uniqueId,
                    rawContactId = rawIdVal,
                    contactId = contactIdVal,
                    number = num,
                    name = name,
                    label = phoneLabel,
                    favorite = fav,
                    avatarText = getInitials(name),
                    avatarBgValue = pair.first.value.toLong(),
                    avatarTextColorValue = pair.second.value.toLong(),
                    t9Mapping = nameToT9(name),
                    email = emailsForContact.firstOrNull()?.email ?: "",
                    photoUri = photoUri,
                    accountName = accName,
                    accountType = accType,
                    numbers = if (num.isNotBlank()) listOf(labeledNum) else emptyList(),
                    emails = emailsForContact,
                    addresses = addressesForContact,
                    note = noteForContact
                )
            } else {
                val existingNumbers = existing.numbers.toMutableList()
                val numDigits = num.filter { it.isDigit() }
                val alreadyExists = existingNumbers.any { it.number.filter { c -> c.isDigit() } == numDigits }

                if (!alreadyExists && num.isNotBlank()) {
                    existingNumbers.add(LabeledNumber(number = num, label = phoneLabel, isPrimary = false))
                }

                val existingScore = getPhoneNumberQualityScore(existing.number)
                val newScore = getPhoneNumberQualityScore(num)
                val preferredNum = if (newScore > existingScore && num.isNotBlank()) num else existing.number
                val preferredLabel = if (newScore > existingScore && num.isNotBlank()) phoneLabel else existing.label

                contactMap[personKey] = existing.copy(
                    number = preferredNum,
                    label = preferredLabel,
                    favorite = existing.favorite || fav,
                    photoUri = existing.photoUri.ifEmpty { photoUri },
                    email = existing.email.ifEmpty { emailsForContact.firstOrNull()?.email ?: "" },
                    accountName = existing.accountName.ifEmpty { accName },
                    accountType = existing.accountType.ifEmpty { accType },
                    numbers = existingNumbers,
                    emails = if (existing.emails.isNotEmpty()) existing.emails else emailsForContact,
                    addresses = if (existing.addresses.isNotEmpty()) existing.addresses else addressesForContact,
                    note = existing.note.ifEmpty { noteForContact }
                )
            }
        }
    }
    return@withContext contactMap.values.toList()
}

suspend fun DialerRepository.fetchAvailableAccounts(): List<ContactAccount> = withContext(Dispatchers.IO) {
    val list = mutableListOf<ContactAccount>()
    val seen = mutableSetOf<Pair<String, String>>()
    try {
        context.contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            arrayOf(ContactsContract.RawContacts.ACCOUNT_NAME, ContactsContract.RawContacts.ACCOUNT_TYPE),
            "${ContactsContract.RawContacts.DELETED} = 0",
            null,
            null
        )?.use { cursor ->
            val nameIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
            val typeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)
            while (cursor.moveToNext()) {
                val accName = if (nameIdx != -1) cursor.getString(nameIdx) ?: "" else ""
                val accType = if (typeIdx != -1) cursor.getString(typeIdx) ?: "" else ""
                if (accName.isNotBlank() || accType.isNotBlank()) {
                    val key = Pair(accName, accType)
                    if (key !in seen) {
                        seen.add(key)
                        val display = when {
                            accType.equals("com.google", ignoreCase = true) -> "Google • $accName"
                            accType.contains("exchange", ignoreCase = true) -> "Exchange • $accName"
                            accType.contains("whatsapp", ignoreCase = true) -> "WhatsApp • $accName"
                            accType.contains("sim", ignoreCase = true) -> "SIM • $accName"
                            accName.isBlank() -> context.getString(R.string.contact_source_phone)
                            else -> "$accName (${accType.substringAfterLast('.')})"
                        }
                        list.add(ContactAccount(name = accName, type = accType, displayName = display))
                    }
                }
            }
        }
    } catch (_: Exception) {}

    try {
        val am = AccountManager.get(context)
        for (acc in am.accounts) {
            val key = Pair(acc.name, acc.type)
            if (acc.name.isNotBlank() && key !in seen) {
                seen.add(key)
                val display = if (acc.type == "com.google") "Google • ${acc.name}" else "${acc.name} (${acc.type.substringAfterLast('.')})"
                list.add(ContactAccount(name = acc.name, type = acc.type, displayName = display))
            }
        }
    } catch (_: Exception) {}

    if (list.none { it.name.isBlank() || it.type.contains("local") || it.name == "Phone" }) {
        list.add(ContactAccount(name = "Phone", type = "com.android.localphone", displayName = context.getString(R.string.contact_source_phone)))
    }
    list
}

internal fun mapSystemCallType(systemType: Int): CallType {
    return when (systemType) {
        CallLog.Calls.MISSED_TYPE -> CallType.MISSED
        CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
        CallLog.Calls.REJECTED_TYPE -> CallType.REJECTED
        CallLog.Calls.BLOCKED_TYPE -> CallType.BLOCKED
        else -> CallType.INCOMING
    }
}

suspend fun DialerRepository.fetchSystemCallLogs(): List<CallRecord> = withContext(Dispatchers.IO) {
    val logs = mutableListOf<CallRecord>()
    val colors = listOf(AvatarBlue to AvatarBlueText, AvatarOrange to AvatarOrangeText, AvatarGreen to AvatarGreenText)

    val subIdToSlotMap = mutableMapOf<String, Int>()
    try {
        val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
        val subList = try {
            subManager?.activeSubscriptionInfoList ?: emptyList()
        } catch (_: SecurityException) {
            emptyList()
        }
        if (subList.isNotEmpty()) {
            subList.forEachIndexed { index, subInfo ->
                val slot = (subInfo.simSlotIndex.takeIf { it >= 0 } ?: index) + 1
                val subIdStr = subInfo.subscriptionId.toString()
                subIdToSlotMap[subIdStr] = slot
                try {
                    subInfo.iccId?.takeIf { it.length >= 6 }?.let { subIdToSlotMap[it] = slot }
                } catch (_: Exception) {}
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q && subInfo.cardId > 0) {
                        subIdToSlotMap[subInfo.cardId.toString()] = slot
                    }
                } catch (_: Exception) {}
                subIdToSlotMap["sub_${subInfo.subscriptionId}"] = slot
                subIdToSlotMap["slot_${subInfo.simSlotIndex}"] = slot
                subIdToSlotMap["sim_${subInfo.simSlotIndex}"] = slot
                subIdToSlotMap["sim_$slot"] = slot
            }
        }
    } catch (_: Exception) {}

    val baseProjection = arrayOf(
        CallLog.Calls._ID,
        CallLog.Calls.CACHED_NAME,
        CallLog.Calls.NUMBER,
        CallLog.Calls.TYPE,
        CallLog.Calls.DATE,
        CallLog.Calls.DURATION,
        CallLog.Calls.PHONE_ACCOUNT_ID,
        CallLog.Calls.NUMBER_PRESENTATION
    )
    val api29Projection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
        arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.PHONE_ACCOUNT_ID,
            CallLog.Calls.NUMBER_PRESENTATION,
            CallLog.Calls.BLOCK_REASON,
            CallLog.Calls.CALL_SCREENING_APP_NAME
        )
    } else {
        baseProjection
    }
    val minimalFallbackProjection = arrayOf(
        CallLog.Calls._ID,
        CallLog.Calls.CACHED_NAME,
        CallLog.Calls.NUMBER,
        CallLog.Calls.TYPE,
        CallLog.Calls.DATE,
        CallLog.Calls.DURATION,
        CallLog.Calls.PHONE_ACCOUNT_ID
    )

    var cursor: Cursor? = null
    try {
        cursor = context.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            api29Projection,
            null,
            null,
            "${CallLog.Calls.DATE} DESC"
        )
    } catch (_: Exception) {
        try {
            cursor = context.contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                baseProjection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC"
            )
        } catch (_: Exception) {
            try {
                cursor = context.contentResolver.query(
                    CallLog.Calls.CONTENT_URI,
                    minimalFallbackProjection,
                    null,
                    null,
                    "${CallLog.Calls.DATE} DESC"
                )
            } catch (_: Exception) {}
        }
    }

    try {
        cursor?.use { c ->
            val idIdx = c.getColumnIndex(CallLog.Calls._ID)
            val nameIdx = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val numIdx = c.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx = c.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = c.getColumnIndex(CallLog.Calls.DATE)
            val durIdx = c.getColumnIndex(CallLog.Calls.DURATION)
            val accountIdIdx = c.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)
            val presentationIdx = c.getColumnIndex(CallLog.Calls.NUMBER_PRESENTATION)
            val blockReasonIdx = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                c.getColumnIndex(CallLog.Calls.BLOCK_REASON)
            } else {
                -1
            }
            val screeningAppIdx = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                c.getColumnIndex(CallLog.Calls.CALL_SCREENING_APP_NAME)
            } else {
                -1
            }

            while (c.moveToNext()) {
                val num = if (numIdx != -1) c.getString(numIdx) ?: "" else ""
                val cachedName = if (nameIdx != -1) c.getString(nameIdx) else null
                val presentationVal = if (presentationIdx != -1) c.getInt(presentationIdx) else CallLog.Calls.PRESENTATION_ALLOWED
                val blockReasonVal = if (blockReasonIdx != -1) c.getInt(blockReasonIdx) else 0
                val screeningAppVal = if (screeningAppIdx != -1) c.getString(screeningAppIdx)?.trim() ?: "" else ""

                val rawNumTrimmed = num.trim()
                val isNonAllowedPresentation = presentationVal == CallLog.Calls.PRESENTATION_RESTRICTED ||
                        presentationVal == CallLog.Calls.PRESENTATION_UNKNOWN ||
                        presentationVal == CallLog.Calls.PRESENTATION_PAYPHONE
                val isUnknownNum = isNonAllowedPresentation ||
                        rawNumTrimmed.isBlank() ||
                        rawNumTrimmed == "-1" || rawNumTrimmed == "-2" || rawNumTrimmed == "-3" ||
                        rawNumTrimmed.equals("unknown", ignoreCase = true) ||
                        rawNumTrimmed.equals("private", ignoreCase = true) ||
                        rawNumTrimmed.equals("restricted", ignoreCase = true) ||
                        rawNumTrimmed.equals("anonymous", ignoreCase = true) ||
                        rawNumTrimmed.equals("p", ignoreCase = true) ||
                        rawNumTrimmed.equals("0", ignoreCase = true)

                // Uses instant in-memory ContactCache instead of querying SQLite on every loop
                val matchingContact = if (!isUnknownNum && num.isNotBlank()) ContactCache.getContact(num) else null
                val matchingCnapName = if (!isUnknownNum && num.isNotBlank()) ContactCache.getCnapName(num) else null

                val isSavedContact = (matchingContact != null) || (!cachedName.isNullOrBlank() && !cachedName.equals(num, ignoreCase = true) && !isUnknownNum)
                val isVerified = !isSavedContact && !matchingCnapName.isNullOrBlank()

                val name = when {
                    matchingContact != null -> matchingContact.name
                    !cachedName.isNullOrBlank() && !cachedName.equals(num, ignoreCase = true) && !isUnknownNum -> cachedName
                    !matchingCnapName.isNullOrBlank() -> matchingCnapName
                    isUnknownNum -> context.getString(R.string.unknown)
                    else -> num
                }

                val finalNum = if (isUnknownNum) "" else num
                val dateVal = if (dateIdx != -1) c.getLong(dateIdx) else 0L
                val phoneAccountId = if (accountIdIdx != -1) c.getString(accountIdIdx) ?: "" else ""
                val trackedSimSlot = SimCallTracker.getSimSlotForCall(context, num, dateVal)

                val simSlot = MultiSimManager.resolveCallLogSimSlot(
                    trackedSimSlot = trackedSimSlot,
                    phoneAccountId = phoneAccountId,
                    subIdToSlotMap = subIdToSlotMap
                )

                val typeVal = if (typeIdx != -1) c.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                val type = mapSystemCallType(typeVal)
                val idVal = if (idIdx != -1) c.getInt(idIdx) else 0
                val durVal = if (durIdx != -1) c.getLong(durIdx) else 0L

                val contactLabel = matchingContact?.label ?: ""
                val pair = colors[Math.abs(name.hashCode()) % colors.size]
                val photoUriVal = matchingContact?.photoUri ?: ""
                logs.add(
                    CallRecord(
                        id = idVal,
                        name = name,
                        number = finalNum,
                        label = contactLabel,
                        timestamp = CallAudioRecorder.formatShortDateTime(context, dateVal),
                        type = type,
                        avatarText = getInitials(name),
                        avatarBgValue = pair.first.value.toLong(),
                        avatarTextColorValue = pair.second.value.toLong(),
                        duration = durVal,
                        hasVoicemail = false,
                        photoUri = photoUriVal,
                        timestampMs = dateVal,
                        isVerified = isVerified,
                        simSlot = simSlot,
                        numberPresentation = presentationVal,
                        blockReason = blockReasonVal,
                        callScreeningAppName = screeningAppVal
                    )
                )
            }
        }
    } catch (_: Exception) {}
    logs
}