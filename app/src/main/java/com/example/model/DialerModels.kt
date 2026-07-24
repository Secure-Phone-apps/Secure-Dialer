package com.example.model

import androidx.compose.ui.graphics.Color
import androidx.room.*

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
    val hasVoicemail: Boolean
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

