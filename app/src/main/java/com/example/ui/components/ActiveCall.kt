package com.example.ui.components

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.CallManager
import com.example.model.Contact
import kotlinx.coroutines.delay

@Composable
fun ActiveCallScreen(
    contactName: String,
    contactNumber: String,
    preferredSim: String,
    quickResponses: List<String>,
    onHangUp: () -> Unit,
    onQuickDecline: (String) -> Unit,
    isDarkTheme: Boolean = true,
    isIncoming: Boolean = false,
    contacts: List<Contact> = emptyList(),
    activePill: Color = Color.Unspecified,
    onAnswer: () -> Unit = {},
    callState: Int = android.telecom.Call.STATE_DISCONNECTED
) {
    var callDuration by remember { mutableStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isBluetoothOn by remember { mutableStateOf(false) }
    var isOnHold by remember { mutableStateOf(false) }

    // Observe audio state for UI synchronization
    val audioState by CallManager.audioState.collectAsStateWithLifecycle()
    LaunchedEffect(audioState) {
        audioState?.let {
            isBluetoothOn = (it.route == android.telecom.CallAudioState.ROUTE_BLUETOOTH)
            isSpeakerOn = (it.route == android.telecom.CallAudioState.ROUTE_SPEAKER)
        }
    }

    var isQuickDeclineMenuOpen by remember { mutableStateOf(false) }
    var isInCallDialpadOpen by remember { mutableStateOf(false) }
    var inCallDialpadInput by remember { mutableStateOf("") }
    var isRecording by remember { mutableStateOf(false) }
    var isAddCallDialogOpen by remember { mutableStateOf(false) }
    var addCallNumberInput by remember { mutableStateOf("") }
    var selectedAddCallContactName by remember { mutableStateOf("") }
    var participants by remember(contactName, contactNumber) {
        mutableStateOf(listOf(Pair(contactName, contactNumber)))
    }

    val context = LocalContext.current

    // Manage standard audio routing cleanup when ActiveCallScreen is entered and disposed
    DisposableEffect(Unit) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val originalMode = audioManager?.mode ?: AudioManager.MODE_NORMAL
        val originalSpeaker = audioManager?.isSpeakerphoneOn ?: false
        val originalMute = audioManager?.isMicrophoneMute ?: false
        onDispose {
            try {
                audioManager?.let { am ->
                    am.isMicrophoneMute = originalMute
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        am.clearCommunicationDevice()
                    }
                    am.isSpeakerphoneOn = originalSpeaker
                    am.mode = originalMode
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // DTMF Tone Generator for In-Call Keypad
    val inCallToneGenerator = remember {
        try {
            ToneGenerator(AudioManager.STREAM_DTMF, 80)
        } catch (e: Exception) {
            null
        }
    }

    // Release ToneGenerator on dispose to prevent native audio resource leaks
    DisposableEffect(Unit) {
        onDispose {
            try {
                inCallToneGenerator?.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun playInCallDtmf(key: String) {
        inCallToneGenerator?.let {
            val tone = when (key) {
                "1" -> ToneGenerator.TONE_DTMF_1
                "2" -> ToneGenerator.TONE_DTMF_2
                "3" -> ToneGenerator.TONE_DTMF_3
                "4" -> ToneGenerator.TONE_DTMF_4
                "5" -> ToneGenerator.TONE_DTMF_5
                "6" -> ToneGenerator.TONE_DTMF_6
                "7" -> ToneGenerator.TONE_DTMF_7
                "8" -> ToneGenerator.TONE_DTMF_8
                "9" -> ToneGenerator.TONE_DTMF_9
                "0" -> ToneGenerator.TONE_DTMF_0
                "*" -> ToneGenerator.TONE_DTMF_S
                "#" -> ToneGenerator.TONE_DTMF_P
                else -> -1
            }
            if (tone != -1) {
                it.startTone(tone, 120)
            }
        }
    }

    LaunchedEffect(key1 = callState) {
        if (callState == android.telecom.Call.STATE_ACTIVE) {
            while (true) {
                delay(1000)
                callDuration++
            }
        }
    }

    val formattedTime = String.format("%02d:%02d", callDuration / 60, callDuration % 60)

    // Color theme adapters
    val backgroundColor = if (isDarkTheme) Color(0xFF111318) else Color(0xFFF4F6FA)
    val textColor = if (isDarkTheme) Color.White else Color(0xFF1A1C1E)
    val subTextColor = if (isDarkTheme) Color.LightGray.copy(alpha = 0.8f) else Color(0xFF43474E)
    val cardContainerColor = if (isDarkTheme) Color(0xFF1E2025) else Color(0xFFE1E2EC)
    val listBgColor = if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = backgroundColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Status Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 40.dp)
            ) {
                val displayHeader = if (isOnHold) {
                    "Call on hold"
                } else if (participants.size > 1) {
                    "Conference call via $preferredSim"
                } else {
                    "Ongoing call via $preferredSim"
                }

                Text(
                    text = displayHeader,
                    color = subTextColor,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (isRecording) {
                        Box(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Red)
                        )
                    }

                    val displayName = if (participants.size > 1) {
                        if (participants.size == 2) {
                            "${participants[0].first.ifEmpty { participants[0].second }} & ${participants[1].first.ifEmpty { participants[1].second }}"
                        } else {
                            "Conference (${participants.size})"
                        }
                    } else {
                        contactName.ifEmpty { contactNumber }
                    }

                    Text(
                        text = displayName,
                        color = textColor,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                val displaySubtitle = if (participants.size > 1) {
                    if (participants.size > 2) {
                        participants.joinToString(", ") { it.first.ifEmpty { it.second } }
                    } else {
                        "${participants[0].second} • ${participants[1].second}"
                    }
                } else {
                    if (contactName.isNotEmpty() && contactNumber.isNotEmpty()) contactNumber else ""
                }

                if (displaySubtitle.isNotEmpty()) {
                    Text(
                        text = displaySubtitle,
                        color = subTextColor.copy(alpha = 0.7f),
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = formattedTime,
                    color = textColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Middle Call Screen options
            if (isIncoming && isQuickDeclineMenuOpen) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardContainerColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Select Quick Rejection Message:",
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(quickResponses, key = { it }) { resp ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(listBgColor)
                                        .clickable { onQuickDecline(resp) }
                                        .padding(12.dp)
                                ) {
                                    Text(resp, color = textColor, fontSize = 14.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { isQuickDeclineMenuOpen = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.8f)),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                    }
                }
            } else if (isInCallDialpadOpen) {
                // Interactive In-Call Keypad for DTMF entry
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardContainerColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = inCallDialpadInput.ifEmpty { "Touch Tones" },
                            color = textColor,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp),
                            textAlign = TextAlign.Center
                        )

                        val inCallKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            for (r in 0 until 4) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    for (c in 0 until 3) {
                                        val key = inCallKeys[r * 3 + c]
                                        Box(
                                            modifier = Modifier
                                                .size(width = 64.dp, height = 44.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(listBgColor)
                                                .clickable {
                                                    inCallDialpadInput += key
                                                    CallManager.playDtmf(key[0])
                                                },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(key, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        TextButton(onClick = { isInCallDialpadOpen = false }) {
                            Text("Close Keypad", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(CircleShape)
                        .background(if (isDarkTheme) Color.White.copy(alpha = 0.1f) else Color.Black.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (participants.size > 1) {
                        Text(
                            text = "👥",
                            fontSize = 54.sp
                        )
                    } else {
                        val pName = participants.firstOrNull()?.first ?: contactName
                        Text(
                            text = if (pName.length >= 2) pName.substring(0, 2).uppercase() else "📞",
                            fontSize = 48.sp,
                            color = textColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Add Call Dialog
            if (isAddCallDialogOpen) {
                Dialog(onDismissRequest = { isAddCallDialogOpen = false }) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardContainerColor),
                        shape = RoundedCornerShape(24.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "➕ Add Call",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = addCallNumberInput,
                                onValueChange = {
                                    addCallNumberInput = it
                                    selectedAddCallContactName = ""
                                },
                                label = { Text("Search contact or enter number") },
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = subTextColor.copy(alpha = 0.5f),
                                    focusedLabelColor = MaterialTheme.colorScheme.primary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            val filteredContacts = remember(addCallNumberInput, contacts) {
                                if (addCallNumberInput.isBlank()) {
                                    contacts
                                } else {
                                    contacts.filter {
                                        it.name.contains(addCallNumberInput, ignoreCase = true) ||
                                                it.number.contains(addCallNumberInput)
                                    }
                                }
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 180.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(filteredContacts) { contact ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isDarkTheme) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.03f))
                                            .clickable {
                                                addCallNumberInput = contact.number
                                                selectedAddCallContactName = contact.name
                                            }
                                            .padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(contact.avatarBg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = if (contact.name.isNotEmpty()) contact.name.take(1).uppercase() else "?",
                                                color = contact.avatarTextColor,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = contact.name,
                                                color = textColor,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Text(
                                                text = contact.number,
                                                color = subTextColor,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                TextButton(onClick = { isAddCallDialogOpen = false }) {
                                    Text("Cancel", color = subTextColor)
                                }
                                Button(
                                    onClick = {
                                        if (addCallNumberInput.isNotBlank()) {
                                            val finalName = if (selectedAddCallContactName.isNotBlank()) {
                                                selectedAddCallContactName
                                            } else {
                                                contacts.find { it.number == addCallNumberInput }?.name ?: addCallNumberInput
                                            }
                                            participants = participants + Pair(finalName, addCallNumberInput)
                                            Toast.makeText(context, "📞 Merged call with $finalName", Toast.LENGTH_LONG).show()
                                            isAddCallDialogOpen = false
                                            addCallNumberInput = ""
                                            selectedAddCallContactName = ""
                                        } else {
                                            Toast.makeText(context, "Please select or enter a valid number", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Text("Add & Merge", color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }
                }
            }

            // Call Option buttons: keypad, mute, speaker, hold, add call, record
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Row 1: Keypad, Mute, Speaker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InCallButton(
                        icon = "⌨️",
                        label = "Keypad",
                        isActive = isInCallDialpadOpen,
                        onClick = { isInCallDialpadOpen = !isInCallDialpadOpen },
                        isDarkTheme = isDarkTheme
                    )
                    InCallButton(
                        icon = "🔇",
                        label = "Mute",
                        isActive = isMuted,
                        onClick = {
                            isMuted = !isMuted
                            CallManager.setMuted(isMuted)
                            Toast.makeText(context, if (isMuted) "🎤 Microphone Muted" else "🎤 Microphone Active", Toast.LENGTH_SHORT).show()
                        },
                        isDarkTheme = isDarkTheme
                    )
                    InCallButton(
                        icon = "🔊",
                        label = "Speaker",
                        isActive = isSpeakerOn,
                        onClick = {
                            isSpeakerOn = !isSpeakerOn
                            CallManager.setSpeaker(isSpeakerOn)
                            Toast.makeText(context, if (isSpeakerOn) "🔊 Speakerphone On" else "🔈 Speakerphone Off", Toast.LENGTH_SHORT).show()
                        },
                        isDarkTheme = isDarkTheme
                    )
                }

                // Row 2: Hold, Add Call, Record
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InCallButton(
                        icon = "⏸️",
                        label = "Hold",
                        isActive = isOnHold,
                        onClick = {
                            isOnHold = !isOnHold
                            CallManager.setHold(isOnHold)
                            Toast.makeText(context, if (isOnHold) "⏸️ Call placed on hold" else "▶️ Call resumed", Toast.LENGTH_SHORT).show()
                        },
                        isDarkTheme = isDarkTheme
                    )
                    InCallButton(
                        icon = "🎧",
                        label = "Bluetooth",
                        isActive = isBluetoothOn,
                        onClick = {
                            isBluetoothOn = !isBluetoothOn
                            CallManager.setBluetooth(isBluetoothOn)
                            Toast.makeText(context, if (isBluetoothOn) "🎧 Bluetooth On" else "🎧 Bluetooth Off", Toast.LENGTH_SHORT).show()
                        },
                        isDarkTheme = isDarkTheme
                    )
                    Spacer(modifier = Modifier.size(60.dp))
                }

                // Styled "Quick Decline SMS" text link/button below the 6 main buttons
                if (isIncoming) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDarkTheme) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                                .clickable { isQuickDeclineMenuOpen = !isQuickDeclineMenuOpen }
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "💬 Send Quick SMS",
                                color = if (isDarkTheme) Color(0xFFA8C7FA) else Color(0xFF0B57D0),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Actions: Answer and Hang Up
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isIncoming) {
                    // Green Answer Button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)
                            .clickable { onAnswer() }
                            .testTag("answer_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "📞",
                            fontSize = 22.sp,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Red Hang Up / Decline Button
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onSurface)
                        .clickable { onHangUp() }
                        .testTag("hangup_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "❌",
                        fontSize = 22.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun InCallButton(
    icon: String, // Kept for backwards compatibility but we now draw clean vector symbols
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    isDarkTheme: Boolean = true
) {
    val btnBg = if (isActive) {
        if (isDarkTheme) Color(0xFFD3E3FD) else Color(0xFF004A77)
    } else {
        if (isDarkTheme) Color(0xFF004A77) else Color(0xFFD3E3FD)
    }
    val contentColor = if (isActive) {
        if (isDarkTheme) Color(0xFF041E49) else Color.White
    } else {
        if (isDarkTheme) Color.White else Color(0xFF041E49)
    }
    val labelColor = if (isDarkTheme) Color.LightGray.copy(alpha = 0.9f) else Color(0xFF43474E)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(btnBg)
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            when (label) {
                "Keypad" -> {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val dotRadius = 1.8f.dp.toPx()
                        val spacing = 6.dp.toPx()
                        val startX = size.width / 2 - spacing
                        val startY = size.height / 2 - spacing
                        for (row in 0..2) {
                            for (col in 0..2) {
                                drawCircle(
                                    color = contentColor,
                                    radius = dotRadius,
                                    center = androidx.compose.ui.geometry.Offset(
                                        startX + col * spacing,
                                        startY + row * spacing
                                    )
                                )
                            }
                        }
                    }
                }
                "Mute" -> {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val w = size.width
                        val h = size.height
                        // Speaker bell
                        val speakerPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w * 0.12f, h * 0.38f)
                            lineTo(w * 0.32f, h * 0.38f)
                            lineTo(w * 0.55f, h * 0.16f)
                            lineTo(w * 0.55f, h * 0.84f)
                            lineTo(w * 0.32f, h * 0.62f)
                            lineTo(w * 0.12f, h * 0.62f)
                            close()
                        }
                        drawPath(path = speakerPath, color = contentColor)

                        // X symbol on the right
                        val cx = w * 0.78f
                        val cy = h * 0.50f
                        val halfSize = 3.5f.dp.toPx()
                        drawLine(
                            color = contentColor,
                            start = androidx.compose.ui.geometry.Offset(cx - halfSize, cy - halfSize),
                            end = androidx.compose.ui.geometry.Offset(cx + halfSize, cy + halfSize),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = contentColor,
                            start = androidx.compose.ui.geometry.Offset(cx - halfSize, cy + halfSize),
                            end = androidx.compose.ui.geometry.Offset(cx + halfSize, cy - halfSize),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
                "Speaker" -> {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val w = size.width
                        val h = size.height
                        // Speaker bell
                        val speakerPath = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w * 0.12f, h * 0.38f)
                            lineTo(w * 0.32f, h * 0.38f)
                            lineTo(w * 0.55f, h * 0.16f)
                            lineTo(w * 0.55f, h * 0.84f)
                            lineTo(w * 0.32f, h * 0.62f)
                            lineTo(w * 0.12f, h * 0.62f)
                            close()
                        }
                        drawPath(path = speakerPath, color = contentColor)

                        // Wave 1
                        drawArc(
                            color = contentColor,
                            startAngle = -40f,
                            sweepAngle = 80f,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(w * 0.35f, h * 0.30f),
                            size = androidx.compose.ui.geometry.Size(w * 0.32f, h * 0.40f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                        // Wave 2
                        drawArc(
                            color = contentColor,
                            startAngle = -40f,
                            sweepAngle = 80f,
                            useCenter = false,
                            topLeft = androidx.compose.ui.geometry.Offset(w * 0.25f, h * 0.18f),
                            size = androidx.compose.ui.geometry.Size(w * 0.56f, h * 0.64f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
                "Hold" -> {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val w = size.width
                        val h = size.height
                        val barWidth = 4.dp.toPx()
                        val barHeight = 13.dp.toPx()
                        val topY = h * 0.23f

                        // Bar 1
                        drawRoundRect(
                            color = contentColor,
                            topLeft = androidx.compose.ui.geometry.Offset(w * 0.33f, topY),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
                        )
                        // Bar 2
                        drawRoundRect(
                            color = contentColor,
                            topLeft = androidx.compose.ui.geometry.Offset(w * 0.55f, topY),
                            size = androidx.compose.ui.geometry.Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.dp.toPx(), 1.dp.toPx())
                        )
                    }
                }
                "Bluetooth" -> {
                    Canvas(modifier = Modifier.size(24.dp)) {
                        val w = size.width
                        val h = size.height
                        val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        
                        val path = androidx.compose.ui.graphics.Path().apply {
                            moveTo(w * 0.25f, h * 0.35f)
                            lineTo(w * 0.75f, h * 0.65f)
                            lineTo(w * 0.50f, h * 0.85f)
                            lineTo(w * 0.50f, h * 0.15f)
                            lineTo(w * 0.75f, h * 0.35f)
                            lineTo(w * 0.25f, h * 0.65f)
                        }
                        drawPath(path = path, color = contentColor, style = stroke)
                    }
                }
                else -> {
                    // Fallback just in case
                    Text(
                        text = icon,
                        fontSize = 22.sp,
                        color = contentColor
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = label,
            color = labelColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
