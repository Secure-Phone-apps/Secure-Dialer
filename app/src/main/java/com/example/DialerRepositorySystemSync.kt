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
import android.provider.CallLog
import android.provider.ContactsContract
import android.provider.ContactsContract.CommonDataKinds.Phone
import com.example.model.AppSetting
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.Contact
import com.example.model.ContactAccount
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
        val projection = arrayOf(
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
        context.contentResolver.query(
            Phone.CONTENT_URI,
            projection,
            null, null, "${Phone.DISPLAY_NAME} ASC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(Phone._ID)
            val rawIdIdx = cursor.getColumnIndex(Phone.RAW_CONTACT_ID)
            val cidIdx = cursor.getColumnIndex(Phone.CONTACT_ID)
            val nameIdx = cursor.getColumnIndex(Phone.DISPLAY_NAME)
            val numIdx = cursor.getColumnIndex(Phone.NUMBER)
            val favIdx = cursor.getColumnIndex(Phone.STARRED)
            val photoIdx = cursor.getColumnIndex(Phone.PHOTO_THUMBNAIL_URI)
            val typeIdx = cursor.getColumnIndex(Phone.TYPE)
            val labelIdx = cursor.getColumnIndex(Phone.LABEL)
            val accNameIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME)
            val accTypeIdx = cursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE)

            while (cursor.moveToNext()) {
                val idVal = if (idIdx != -1) cursor.getLong(idIdx) else 0L
                val rawIdVal = if (rawIdIdx != -1) cursor.getLong(rawIdIdx) else 0L
                val contactIdVal = if (cidIdx != -1) cursor.getLong(cidIdx) else 0L
                val num = cursor.getString(numIdx) ?: ""
                val rawName = cursor.getString(nameIdx)
                val name = if (rawName.isNullOrBlank()) (if (num.isBlank()) "Unknown" else num) else rawName
                val fav = cursor.getInt(favIdx) == 1
                val photoUri = if (photoIdx != -1) cursor.getString(photoIdx) ?: "" else ""
                val contactIdStr = if (cidIdx != -1) cursor.getString(cidIdx) ?: "" else ""
                val email = emailMap[contactIdStr] ?: ""
                val accName = if (accNameIdx != -1) cursor.getString(accNameIdx) ?: "" else ""
                val accType = if (accTypeIdx != -1) cursor.getString(accTypeIdx) ?: "" else ""
                val phoneType = if (typeIdx != -1) cursor.getInt(typeIdx) else Phone.TYPE_MOBILE
                val phoneLabel = if (phoneType == Phone.TYPE_CUSTOM && labelIdx != -1) {
                    cursor.getString(labelIdx) ?: "Custom"
                } else when (phoneType) {
                    Phone.TYPE_HOME -> "Home"
                    Phone.TYPE_WORK -> "Work"
                    Phone.TYPE_MOBILE -> "Mobile"
                    Phone.TYPE_OTHER -> "Other"
                    else -> "Mobile"
                }

                val pair = colors[Math.abs(name.hashCode()) % colors.size]
                val uniqueId = if (idVal != 0L) idVal else (contactMap.size + 1).toLong()
                
                val newContact = Contact(
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
                    email = email,
                    photoUri = photoUri,
                    accountName = accName,
                    accountType = accType
                )

                val normKey = getNormalizedPhoneNumberKey(num)
                val personKey = if (contactIdVal != 0L) "$contactIdVal-$normKey" else "${name.lowercase().trim()}-$normKey"

                val existing = contactMap[personKey]
                if (existing == null) {
                    contactMap[personKey] = newContact
                } else {
                    val existingScore = getPhoneNumberQualityScore(existing.number)
                    val newScore = getPhoneNumberQualityScore(newContact.number)

                    val preferredContact = if (newScore > existingScore) newContact else existing
                    val mergedContact = preferredContact.copy(
                        favorite = preferredContact.favorite || existing.favorite,
                        photoUri = preferredContact.photoUri.ifEmpty { existing.photoUri },
                        email = preferredContact.email.ifEmpty { existing.email },
                        accountName = preferredContact.accountName.ifEmpty { existing.accountName },
                        accountType = preferredContact.accountType.ifEmpty { existing.accountType }
                    )
                    contactMap[personKey] = mergedContact
                }
            }
        }
    } catch (e: SecurityException) {
        e.printStackTrace()
    } catch (e: Exception) {
        e.printStackTrace()
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
                            accName.isBlank() -> "Phone storage"
                            else -> "$accName (${accType.substringAfterLast('.')})"
                        }
                        list.add(ContactAccount(name = accName, type = accType, displayName = display))
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    try {
        val am = android.accounts.AccountManager.get(context)
        val accounts = am.accounts
        for (acc in accounts) {
            val key = Pair(acc.name, acc.type)
            if (acc.name.isNotBlank() && key !in seen) {
                seen.add(key)
                val display = if (acc.type == "com.google") "Google • ${acc.name}" else "${acc.name} (${acc.type.substringAfterLast('.')})"
                list.add(ContactAccount(name = acc.name, type = acc.type, displayName = display))
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    if (list.none { it.name.isBlank() || it.type.contains("local") || it.name == "Phone" }) {
        list.add(ContactAccount(name = "Phone", type = "com.android.localphone", displayName = "Phone storage"))
    }
    list
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
    val cnapSuffix10 = HashMap<String, String>()
    val cnapSuffix8 = HashMap<String, String>()
    val cnapSuffix7 = HashMap<String, String>()

    for (entry in cnapMap.entries) {
        val cleanKey = entry.key.filter { it.isDigit() }
        if (cleanKey.isNotEmpty()) {
            val cnapName = entry.value
            cnapIndex[cleanKey] = cnapName
            val len = cleanKey.length
            if (len >= 10) cnapSuffix10[cleanKey.takeLast(10)] = cnapName
            if (len >= 8) cnapSuffix8[cleanKey.takeLast(8)] = cnapName
            if (len >= 7) cnapSuffix7[cleanKey.takeLast(7)] = cnapName
        }
    }

    val subIdToSlotMap = mutableMapOf<String, Int>()
    try {
        val subManager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? android.telephony.SubscriptionManager
        subManager?.let { sm ->
            val subList = try {
                sm.activeSubscriptionInfoList
            } catch (e: SecurityException) {
                null
            }
            subList?.forEach { subInfo ->
                val slot = subInfo.simSlotIndex + 1
                val subIdStr = subInfo.subscriptionId.toString()
                subIdToSlotMap[subIdStr] = slot
                subInfo.iccId?.let { if (it.isNotBlank()) subIdToSlotMap[it] = slot }
                if (subInfo.cardId > 0) subIdToSlotMap[subInfo.cardId.toString()] = slot
                subIdToSlotMap["sub_${subInfo.subscriptionId}"] = slot
                subIdToSlotMap["slot_${subInfo.simSlotIndex}"] = slot
                subIdToSlotMap["sim_${subInfo.simSlotIndex}"] = slot
                subIdToSlotMap["sim_${slot}"] = slot
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }

    try {
        val queryUri = CallLog.Calls.CONTENT_URI
        context.contentResolver.query(
            queryUri,
            arrayOf(
                CallLog.Calls._ID,
                CallLog.Calls.CACHED_NAME,
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DATE,
                CallLog.Calls.DURATION,
                CallLog.Calls.PHONE_ACCOUNT_ID
            ),
            null, null, "${CallLog.Calls.DATE} DESC"
        )?.use { cursor ->
            val idIdx = cursor.getColumnIndex(CallLog.Calls._ID)
            val nameIdx = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val numIdx = cursor.getColumnIndex(CallLog.Calls.NUMBER)
            val typeIdx = cursor.getColumnIndex(CallLog.Calls.TYPE)
            val dateIdx = cursor.getColumnIndex(CallLog.Calls.DATE)
            val durIdx = cursor.getColumnIndex(CallLog.Calls.DURATION)
            val accountIdIdx = cursor.getColumnIndex(CallLog.Calls.PHONE_ACCOUNT_ID)

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
                    val len = cleanNum.length
                    cnapIndex[cleanNum] ?: when {
                        len >= 10 -> cnapSuffix10[cleanNum.takeLast(10)]
                        len >= 8 -> cnapSuffix8[cleanNum.takeLast(8)]
                        len >= 7 -> cnapSuffix7[cleanNum.takeLast(7)]
                        else -> null
                    }
                } else null

                val isSavedContact = (matchingContact != null)
                val isVerified = !isSavedContact && (!cachedName.isNullOrBlank() || !matchingCnapName.isNullOrBlank())

                val name = when {
                    matchingContact != null -> matchingContact.name
                    !cachedName.isNullOrBlank() -> cachedName
                    !matchingCnapName.isNullOrBlank() -> matchingCnapName
                    num.isBlank() -> "Unknown"
                    else -> num
                }

                val dateVal = if (dateIdx != -1) cursor.getLong(dateIdx) else 0L
                val phoneAccountId = if (accountIdIdx != -1) cursor.getString(accountIdIdx) ?: "" else ""
                val trackedSimSlot = com.example.util.SimCallTracker.getSimSlotForCall(context, num, dateVal)

                val simSlot = when {
                    trackedSimSlot != null -> trackedSimSlot
                    phoneAccountId in subIdToSlotMap -> subIdToSlotMap[phoneAccountId] ?: 1
                    subIdToSlotMap.entries.any { (key, _) -> key.isNotEmpty() && phoneAccountId.contains(key) } -> {
                        subIdToSlotMap.entries.first { (key, _) -> key.isNotEmpty() && phoneAccountId.contains(key) }.value
                    }
                    phoneAccountId.contains("sim_2", ignoreCase = true) ||
                    phoneAccountId.contains("sim2", ignoreCase = true) ||
                    phoneAccountId.contains("sub_2", ignoreCase = true) ||
                    phoneAccountId.contains("slot_2", ignoreCase = true) ||
                    phoneAccountId.contains("slot2", ignoreCase = true) ||
                    phoneAccountId.contains(", 2,") ||
                    phoneAccountId.endsWith("_1") ||
                    (phoneAccountId.equals("1") && !subIdToSlotMap.containsKey("1")) -> 2
                    phoneAccountId.contains("sim_1", ignoreCase = true) ||
                    phoneAccountId.contains("sim1", ignoreCase = true) ||
                    phoneAccountId.contains("sub_1", ignoreCase = true) ||
                    phoneAccountId.contains("slot_1", ignoreCase = true) ||
                    phoneAccountId.contains("slot1", ignoreCase = true) ||
                    phoneAccountId.contains(", 1,") ||
                    phoneAccountId.equals("0") ||
                    phoneAccountId.endsWith("_0") -> 1
                    else -> 1
                }

                val typeVal = if (typeIdx != -1) cursor.getInt(typeIdx) else CallLog.Calls.INCOMING_TYPE
                val type = when (typeVal) {
                    CallLog.Calls.MISSED_TYPE -> CallType.MISSED
                    CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                    else -> CallType.INCOMING
                }
                val idVal = if (idIdx != -1) cursor.getInt(idIdx) else 0
                val durVal = if (durIdx != -1) cursor.getLong(durIdx) else 0L

                val contactLabel = matchingContact?.label ?: ""
                val pair = colors[Math.abs(name.hashCode()) % colors.size]
                val photoUriVal = matchingContact?.photoUri ?: ""
                logs.add(
                    CallRecord(
                        id = idVal,
                        name = name,
                        number = num,
                        label = contactLabel,
                        timestamp = sdf.format(Date(dateVal)),
                        type = type,
                        avatarText = getInitials(name),
                        avatarBgValue = pair.first.value.toLong(),
                        avatarTextColorValue = pair.second.value.toLong(),
                        duration = durVal,
                        hasVoicemail = false,
                        photoUri = photoUriVal,
                        timestampMs = dateVal,
                        isVerified = isVerified,
                        simSlot = simSlot
                    )
                )
            }
        }
    } catch (e: SecurityException) {
        e.printStackTrace()
    } catch (e: Exception) {
        e.printStackTrace()
    }
    logs
}
