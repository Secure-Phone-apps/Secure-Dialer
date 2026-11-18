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

package com.example.model

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.room.*
import java.util.Locale

// UNIFIED SQUIRCLE ARCHITECTURE: Strictly enforce Squircle (percent = 32)
fun getAvatarShape(shapeType: String = "squircle"): Shape {
    return when (shapeType.lowercase(Locale.ROOT)) {
        "square" -> RoundedCornerShape(percent = 12)
        "rounded" -> RoundedCornerShape(percent = 24)
        else -> RoundedCornerShape(percent = 32) // Strict Squircle percent = 32
    }
}

private val initialsCache = object : android.util.LruCache<String, String>(2048) {}

// FIXED: Handles Unicode surrogate pairs, emojis, and multi-script initials with thread-safe LRU caching
fun getInitials(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"
    val cached = initialsCache.get(trimmed)
    if (cached != null) return cached

    val parts = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
    val result = if (parts.size >= 2) {
        val firstGlyph = getFirstGrapheme(parts[0])
        val secondGlyph = getFirstGrapheme(parts[1])
        (firstGlyph + secondGlyph).uppercase(Locale.ROOT)
    } else {
        getFirstTwoGraphemes(trimmed).uppercase(Locale.ROOT)
    }
    initialsCache.put(trimmed, result)
    return result
}

private fun getFirstGrapheme(text: String): String {
    if (text.isEmpty()) return ""
    val firstChar = text[0]
    return if (Character.isHighSurrogate(firstChar) && text.length >= 2) {
        text.substring(0, 2)
    } else {
        firstChar.toString()
    }
}

private fun getFirstTwoGraphemes(text: String): String {
    if (text.isEmpty()) return "?"
    var count = 0
    var index = 0
    while (index < text.length && count < 2) {
        val char = text[index]
        index += if (Character.isHighSurrogate(char) && index + 1 < text.length) 2 else 1
        count++
    }
    return text.substring(0, index)
}

internal fun decodeStoredColor(storedValue: Long): Color {
    if (storedValue == 0L) return Color.Transparent
    if ((storedValue ushr 32) == 0L) {
        return Color(storedValue.toInt())
    }
    val colorSpaceId = (storedValue and 0x3FL).toInt()
    return if (colorSpaceId in 0..17) {
        Color(storedValue.toULong())
    } else {
        Color((storedValue ushr 32).toInt())
    }
}

@Immutable
@Entity(
    tableName = "call_history",
    indices = [Index(value = ["number"]), Index(value = ["timestamp"])]
)
data class CallRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val number: String,
    val label: String,
    val timestamp: String,
    val type: CallType,
    val avatarText: String,
    val avatarBgValue: Long,
    val avatarTextColorValue: Long,
    val duration: Long,
    val hasVoicemail: Boolean,
    val photoUri: String = "",
    val timestampMs: Long = 0L,
    val isVerified: Boolean = false,
    val simSlot: Int = 1
) {
    @Ignore val avatarBg: Color = decodeStoredColor(avatarBgValue)
    @Ignore val avatarTextColor: Color = decodeStoredColor(avatarTextColorValue)
}

enum class CallType {
    MISSED, OUTGOING, INCOMING
}

@Immutable
data class ContactAccount(
    val name: String,
    val type: String,
    val displayName: String
)

@Immutable
data class LabeledNumber(
    val number: String,
    val label: String = "Mobile",
    val isPrimary: Boolean = false
)

@Immutable
data class LabeledEmail(
    val email: String,
    val label: String = "Home"
)

@Immutable
data class LabeledAddress(
    val address: String,
    val label: String = "Home"
)

const val PRIVATE_VAULT_ACCOUNT_NAME = "__secure_private_vault__"
const val PRIVATE_VAULT_ACCOUNT_TYPE = "com.securedialer.privatevault"

@Immutable
@Entity(
    tableName = "contacts",
    indices = [
        Index(value = ["number"]),
        Index(value = ["name"]),
        Index(value = ["t9Mapping"]),
        Index(value = ["accountName"]),
        Index(value = ["rawContactId"]),
        Index(value = ["contactId"])
    ]
)
data class Contact(
    @PrimaryKey val id: Long = 0L,
    val rawContactId: Long = 0L,
    val contactId: Long = 0L,
    val number: String,
    val name: String,
    val label: String,
    val favorite: Boolean = false,
    val avatarText: String,
    val avatarBgValue: Long,
    val avatarTextColorValue: Long,
    val t9Mapping: String = "",
    val email: String = "",
    val photoUri: String = "",
    val accountName: String = "",
    val accountType: String = "",
    val numbers: List<LabeledNumber> = emptyList(),
    val emails: List<LabeledEmail> = emptyList(),
    val addresses: List<LabeledAddress> = emptyList(),
    val note: String = ""
) {
    @Ignore val avatarBg: Color = decodeStoredColor(avatarBgValue)
    @Ignore val avatarTextColor: Color = decodeStoredColor(avatarTextColorValue)
    val isPrivateVault: Boolean
        get() = accountType == PRIVATE_VAULT_ACCOUNT_TYPE || id < 0L

    fun getAllNumbers(): List<LabeledNumber> {
        if (numbers.isNotEmpty()) return numbers
        if (number.isNotBlank()) {
            return listOf(LabeledNumber(number = number, label = label.ifBlank { "Mobile" }, isPrimary = true))
        }
        return emptyList()
    }

    fun getAllEmails(): List<LabeledEmail> {
        if (emails.isNotEmpty()) return emails
        if (email.isNotBlank()) {
            return listOf(LabeledEmail(email = email, label = "Home"))
        }
        return emptyList()
    }

    fun getAllAddresses(): List<LabeledAddress> = addresses
}

@Immutable
@Entity(
    tableName = "private_contacts",
    indices = [
        Index(value = ["number"]),
        Index(value = ["name"]),
        Index(value = ["t9Mapping"])
    ]
)
data class PrivateContact(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val number: String,
    val name: String,
    val label: String,
    val favorite: Boolean = false,
    val avatarText: String,
    val avatarBgValue: Long,
    val avatarTextColorValue: Long,
    val t9Mapping: String = "",
    val email: String = "",
    val photoUri: String = "",
    val accountName: String = PRIVATE_VAULT_ACCOUNT_NAME,
    val accountType: String = PRIVATE_VAULT_ACCOUNT_TYPE,
    val numbers: List<LabeledNumber> = emptyList(),
    val emails: List<LabeledEmail> = emptyList(),
    val addresses: List<LabeledAddress> = emptyList(),
    val note: String = ""
) {
    fun toContact(): Contact = Contact(
        id = if (id > 0L) -id else 0L,
        rawContactId = 0L,
        contactId = 0L,
        number = number,
        name = name,
        label = label,
        favorite = favorite,
        avatarText = avatarText,
        avatarBgValue = avatarBgValue,
        avatarTextColorValue = avatarTextColorValue,
        t9Mapping = t9Mapping,
        email = email,
        photoUri = photoUri,
        accountName = PRIVATE_VAULT_ACCOUNT_NAME,
        accountType = PRIVATE_VAULT_ACCOUNT_TYPE,
        numbers = numbers,
        emails = emails,
        addresses = addresses,
        note = note
    )
}

fun Contact.toPrivateContact(preserveLocalId: Boolean = false): PrivateContact = PrivateContact(
    id = if (preserveLocalId && id < 0L) -id else 0L,
    number = number,
    name = name,
    label = label,
    favorite = favorite,
    avatarText = avatarText,
    avatarBgValue = avatarBgValue,
    avatarTextColorValue = avatarTextColorValue,
    t9Mapping = t9Mapping,
    email = email,
    photoUri = photoUri,
    accountName = PRIVATE_VAULT_ACCOUNT_NAME,
    accountType = PRIVATE_VAULT_ACCOUNT_TYPE,
    numbers = getAllNumbers(),
    emails = getAllEmails(),
    addresses = getAllAddresses(),
    note = note
)

@Immutable
@Entity(tableName = "call_notes")
data class CallNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val note: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Immutable
@Entity(tableName = "spam_numbers")
data class SpamNumber(
    @PrimaryKey val number: String,
    val label: String = "Spam"
)

@Immutable
@Entity(tableName = "call_reminders")
data class CallReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val number: String,
    val name: String,
    val reminderTime: Long,
    val isCompleted: Boolean = false,
    val note: String = ""
) {
    @Ignore val contactName: String = name
}

@Immutable
@Entity(tableName = "call_recordings")
data class CallRecording(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val number: String,
    val name: String,
    val timestamp: String,
    val duration: Long,
    val filePath: String,
    val note: String = ""
)

@Immutable
@Entity(tableName = "blocked_numbers")
data class BlockedNumber(
    @PrimaryKey val number: String
)

@Immutable
@Entity(tableName = "speed_dial")
data class SpeedDial(
    @PrimaryKey val key: Int,
    val number: String,
    val name: String
)

@Immutable
@Entity(tableName = "quick_responses")
data class QuickResponse(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val message: String
)

@Immutable
@Entity(tableName = "app_settings")
data class AppSetting(
    @PrimaryKey val key: String,
    val value: String
)

@Immutable
data class DialpadMatch(
    val number: String,
    val name: String,
    val label: String,
    val avatarText: String,
    val avatarBgValue: Long,
    val avatarTextColorValue: Long,
    val isFromContacts: Boolean,
    val isFromRecents: Boolean,
    val photoUri: String = ""
) {
    val avatarBg: Color get() = decodeStoredColor(avatarBgValue)
    val avatarTextColor: Color get() = decodeStoredColor(avatarTextColorValue)
}

@Immutable
data class PostCallData(
    val number: String,
    val name: String,
    val photoUri: String = "",
    val durationSeconds: Long = 0L,
    val isUnsaved: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)