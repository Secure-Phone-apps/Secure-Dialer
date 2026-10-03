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

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.room.*
import java.util.Locale

// FIXED: Honour user selection across Circular, Squircle, Rounded, and Square
fun getAvatarShape(shapeType: String): Shape {
    return when (shapeType.lowercase(Locale.ROOT)) {
        "circular", "circle" -> CircleShape
        "squircle" -> RoundedCornerShape(24.dp)
        "square" -> RoundedCornerShape(4.dp)
        else -> RoundedCornerShape(16.dp)
    }
}

// FIXED: Handles Unicode surrogate pairs, emojis, and multi-script initials without  corruption
fun getInitials(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"

    val parts = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
    return if (parts.size >= 2) {
        val firstGlyph = getFirstGrapheme(parts[0])
        val secondGlyph = getFirstGrapheme(parts[1])
        (firstGlyph + secondGlyph).uppercase(Locale.ROOT)
    } else {
        getFirstTwoGraphemes(trimmed).uppercase(Locale.ROOT)
    }
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
    @Ignore val avatarBg: Color = Color(avatarBgValue.toULong())
    @Ignore val avatarTextColor: Color = Color(avatarTextColorValue.toULong())
}

enum class CallType {
    MISSED, OUTGOING, INCOMING
}

data class ContactAccount(
    val name: String,
    val type: String,
    val displayName: String
)

data class LabeledNumber(
    val number: String,
    val label: String = "Mobile",
    val isPrimary: Boolean = false
)

data class LabeledEmail(
    val email: String,
    val label: String = "Home"
)

data class LabeledAddress(
    val address: String,
    val label: String = "Home"
)

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
    @Ignore val avatarBg: Color = Color(avatarBgValue.toULong())
    @Ignore val avatarTextColor: Color = Color(avatarTextColorValue.toULong())

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

@Entity(tableName = "call_notes")
data class CallNote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val number: String,
    val note: String,
    val lastUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "spam_numbers")
data class SpamNumber(
    @PrimaryKey val number: String,
    val label: String = "Spam"
)

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

@Entity(tableName = "blocked_numbers")
data class BlockedNumber(
    @PrimaryKey val number: String
)

@Entity(tableName = "speed_dial")
data class SpeedDial(
    @PrimaryKey val key: Int,
    val number: String,
    val name: String
)

@Entity(tableName = "quick_responses")
data class QuickResponse(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val message: String
)

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
    val avatarBg: Color get() = Color(avatarBgValue.toULong())
    val avatarTextColor: Color get() = Color(avatarTextColorValue.toULong())
}