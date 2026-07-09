package com.example.model

import androidx.compose.ui.graphics.Color

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
