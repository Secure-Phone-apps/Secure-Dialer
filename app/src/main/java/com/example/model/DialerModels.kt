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
    val t9Mapping: String = ""
) {
    @Ignore val avatarBg: Color = Color(avatarBgValue.toULong())
    @Ignore val avatarTextColor: Color = Color(avatarTextColorValue.toULong())
}
