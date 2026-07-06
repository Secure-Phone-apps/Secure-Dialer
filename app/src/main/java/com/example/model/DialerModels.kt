package com.example.model

import androidx.compose.ui.graphics.Color

val AvatarOrange = Color(0xFFFFDBCB)
val AvatarOrangeText = Color(0xFF311300)
val AvatarBlue = Color(0xFFD1E4FF)
val AvatarBlueText = Color(0xFF001D36)
val AvatarGreen = Color(0xFFD9E7CB)
val AvatarGreenText = Color(0xFF141E0D)

data class CallRecord(
    val id: Int,
    val name: String,
    val number: String,
    val label: String,
    val timestamp: String,
    val type: CallType,
    val avatarText: String,
    val avatarBg: Color,
    val avatarTextColor: Color,
    val duration: Long,
    val hasVoicemail: Boolean
)

enum class CallType {
    MISSED, OUTGOING, INCOMING
}

data class Contact(
    val name: String,
    val number: String,
    val label: String,
    val favorite: Boolean = false,
    val avatarBg: Color,
    val avatarTextColor: Color
)
