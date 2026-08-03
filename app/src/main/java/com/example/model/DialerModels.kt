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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.room.*

fun getAvatarShape(shapeType: String): Shape {
    return RoundedCornerShape(16.dp)
}

fun getInitials(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "?"
    
    val parts = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
    return if (parts.size >= 2) {
        val firstChar = parts[0].firstOrNull()?.toString()?.uppercase() ?: ""
        val secondChar = parts[1].firstOrNull()?.toString()?.uppercase() ?: ""
        firstChar + secondChar
    } else {
        if (trimmed.length >= 2) {
            trimmed.substring(0, 2).uppercase()
        } else {
            trimmed.take(1).uppercase()
        }
    }
}

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
    val photoUri: String = ""
) {
    @Ignore val avatarBg: Color = Color(avatarBgValue.toULong())
    @Ignore val avatarTextColor: Color = Color(avatarTextColorValue.toULong())
}

enum class CallType {
    MISSED, OUTGOING, INCOMING
}

@Entity(
    tableName = "contacts",
    indices = [Index(value = ["number"], unique = true), Index(value = ["name"]), Index(value = ["t9Mapping"])]
)
data class Contact(
    @PrimaryKey val number: String,
    val name: String,
    val label: String,
    val favorite: Boolean = false,
    val avatarText: String,
    val avatarBgValue: Long,
    val avatarTextColorValue: Long,
    val t9Mapping: String = "",
    val email: String = "",
    val photoUri: String = ""
) {
    @Ignore val avatarBg: Color = Color(avatarBgValue.toULong())
    @Ignore val avatarTextColor: Color = Color(avatarTextColorValue.toULong())
}

@Entity(tableName = "call_notes")
data class CallNote(
    @PrimaryKey val number: String,
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
)

@Entity(tableName = "call_recordings")
data class CallRecording(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val number: String,
    val name: String,
    val timestamp: String,
    val duration: Long, // in seconds
    val filePath: String
)

@Entity(tableName = "blocked_numbers")
data class BlockedNumber(
    @PrimaryKey val number: String
)

@Entity(tableName = "speed_dial")
data class SpeedDial(
    @PrimaryKey val key: Int, // 2-9
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

