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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
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

import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

@Composable
fun ActiveCallScreen(
    contactName: String,
    contactNumber: String,
    preferredSim: String,
    quickResponses: List<String>,
    onHangUp: () -> Unit,
    onQuickDecline: (String) -> Unit,
    isIncoming: Boolean = false,
    contacts: List<Contact> = emptyList(),
    onAnswer: () -> Unit = {},
    callState: Int = android.telecom.Call.STATE_DISCONNECTED
) {
    val haptic = LocalHapticFeedback.current
    var callDuration by remember { mutableStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isSpeakerOn by remember { mutableStateOf(false) }
    var isBluetoothOn by remember { mutableStateOf(false) }
    var isOnHold by remember { mutableStateOf(false) }
    var isAddCallDialogOpen by remember { mutableStateOf(false) }
    var addCallNumberInput by remember { mutableStateOf("") }
    var selectedAddCallContactName by remember { mutableStateOf("") }

    LaunchedEffect(callState) {
        if (callState == android.telecom.Call.STATE_ACTIVE) {
            while (true) {
                delay(1000)
                callDuration++
            }
        }
    }

    val formattedTime = remember(callDuration) {
        val mins = callDuration / 60
        val secs = callDuration % 60
        "%02d:%02d".format(mins, secs)
    }

    // Dynamic Surface Color based on state
    val surfaceColor = MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
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
    var participants by remember(contactName, contactNumber) {
        mutableStateOf(listOf(Pair(contactName, contactNumber)))
    }

    val context = LocalContext.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = surfaceColor
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
                val displayHeader = when {
                    isOnHold || callState == android.telecom.Call.STATE_HOLDING -> "Call on hold"
                    callState == android.telecom.Call.STATE_DIALING -> "Dialing..."
                    callState == android.telecom.Call.STATE_RINGING -> "Incoming call..."
                    callState == android.telecom.Call.STATE_CONNECTING -> "Connecting..."
                    participants.size > 1 -> "Conference call via $preferredSim"
                    else -> "Ongoing call via $preferredSim"
                }

                Text(
                    text = displayHeader,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

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
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

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
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.displaySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Middle Call Screen options
            if (isIncoming && isQuickDeclineMenuOpen) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Quick Responses",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(quickResponses, key = { it }) { resp ->
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            try {
                                                val smsManager = context.getSystemService(android.telephony.SmsManager::class.java)
                                                smsManager.sendTextMessage(contactNumber, null, resp, null, null)
                                                Toast.makeText(context, "SMS Sent", Toast.LENGTH_SHORT).show()
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Failed to send SMS", Toast.LENGTH_SHORT).show()
                                            }
                                            onQuickDecline(resp)
                                        }
                                ) {
                                    Text(
                                        resp,
                                        modifier = Modifier.padding(16.dp),
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(
                            onClick = { isQuickDeclineMenuOpen = false },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            } else if (isInCallDialpadOpen) {
                // Interactive In-Call Keypad for DTMF entry
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(28.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = inCallDialpadInput.ifEmpty { "Dialpad" },
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 16.dp),
                            textAlign = TextAlign.Center
                        )

                        val inCallKeys = listOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "*", "0", "#")
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (r in 0 until 4) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    for (c in 0 until 3) {
                                        val key = inCallKeys[r * 3 + c]
                                        Surface(
                                            modifier = Modifier
                                                .size(width = 72.dp, height = 48.dp)
                                                .clickable {
                                                    inCallDialpadInput += key
                                                    CallManager.playDtmf(key[0])
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(key, style = MaterialTheme.typography.titleLarge)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        TextButton(onClick = { isInCallDialpadOpen = false }) {
                            Text("Close")
                        }
                    }
                }
            } else {
                Surface(
                    modifier = Modifier.size(160.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (participants.size > 1) {
                            Text(text = "👥", fontSize = 64.sp)
                        } else {
                            val pName = participants.firstOrNull()?.first ?: contactName
                            Text(
                                text = if (pName.length >= 2) pName.substring(0, 2).uppercase() else "📞",
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Add Call Dialog
            if (isAddCallDialogOpen) {
                AlertDialog(
                    onDismissRequest = { isAddCallDialogOpen = false },
                    confirmButton = {
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
                            }
                        ) {
                            Text("Add & Merge")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { isAddCallDialogOpen = false }) {
                            Text("Cancel")
                        }
                    },
                    title = {
                        Text(
                            "Add Call",
                            style = MaterialTheme.typography.headlineSmall
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            OutlinedTextField(
                                value = addCallNumberInput,
                                onValueChange = {
                                    addCallNumberInput = it
                                    selectedAddCallContactName = ""
                                },
                                label = { Text("Search contact or enter number") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            val filteredContacts = if (addCallNumberInput.isBlank()) {
                                contacts
                            } else {
                                contacts.filter {
                                    it.name.contains(addCallNumberInput, ignoreCase = true) ||
                                            it.number.contains(addCallNumberInput)
                                }
                            }

                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredContacts) { contact ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        onClick = {
                                            addCallNumberInput = contact.number
                                            selectedAddCallContactName = contact.name
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (addCallNumberInput == contact.number) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Surface(
                                                modifier = Modifier.size(32.dp),
                                                shape = CircleShape,
                                                color = contact.avatarBg
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Text(
                                                        text = if (contact.name.isNotEmpty()) contact.name.take(1).uppercase() else "?",
                                                        color = contact.avatarTextColor,
                                                        style = MaterialTheme.typography.labelSmall
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = contact.name,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                Text(
                                                    text = contact.number,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(28.dp)
                )
            }

            // Call Option buttons: keypad, mute, speaker, hold, bluetooth
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
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isInCallDialpadOpen = !isInCallDialpadOpen
                        }
                    )
                    InCallButton(
                        icon = "🔇",
                        label = "Mute",
                        isActive = isMuted,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isMuted = !isMuted
                            CallManager.setMuted(isMuted)
                        }
                    )
                    InCallButton(
                        icon = "🔊",
                        label = "Speaker",
                        isActive = isSpeakerOn,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isSpeakerOn = !isSpeakerOn
                            CallManager.setSpeaker(isSpeakerOn)
                        }
                    )
                }

                // Row 2: Hold, Bluetooth
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    InCallButton(
                        icon = "⏸️",
                        label = "Hold",
                        isActive = isOnHold,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isOnHold = !isOnHold
                            CallManager.setHold(isOnHold)
                        }
                    )
                    InCallButton(
                        icon = "🎧",
                        label = "Bluetooth",
                        isActive = isBluetoothOn,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isBluetoothOn = !isBluetoothOn
                            CallManager.setBluetooth(isBluetoothOn)
                        }
                    )
                    Spacer(modifier = Modifier.size(64.dp))
                }

                // Styled "Quick Decline SMS" text link/button below the main buttons
                if (isIncoming) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = { isQuickDeclineMenuOpen = !isQuickDeclineMenuOpen }
                        ) {
                            Text(
                                text = "💬 Send Quick Response",
                                style = MaterialTheme.typography.labelLarge
                            )
                        }
                    }
                }
            }

            // Actions: Answer and Hang Up
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isIncoming) {
                    // Green Answer Button
                    LargeFloatingActionButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onAnswer()
                        },
                        containerColor = Color(0xFF4CAF50),
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.testTag("answer_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Answer",
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Red Hang Up / Decline Button
                LargeFloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onHangUp()
                    },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    shape = CircleShape,
                    modifier = Modifier.testTag("hangup_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Hang up",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun InCallButton(
    icon: String, // Kept for logic if needed, but we draw clean symbols
    label: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val btnColor = if (isActive) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val contentColor = if (isActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier
                .size(64.dp)
                .clickable { onClick() },
            shape = CircleShape,
            color = btnColor
        ) {
            Box(contentAlignment = Alignment.Center) {
                val iconVector = when (label) {
                    "Keypad" -> Icons.Default.Dialpad
                    "Mute" -> Icons.Default.MicOff
                    "Speaker" -> Icons.Default.VolumeUp
                    "Hold" -> Icons.Default.Pause
                    "Bluetooth" -> Icons.Default.Bluetooth
                    else -> Icons.Default.QuestionMark
                }
                Icon(
                    imageVector = iconVector,
                    contentDescription = label,
                    tint = contentColor,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
