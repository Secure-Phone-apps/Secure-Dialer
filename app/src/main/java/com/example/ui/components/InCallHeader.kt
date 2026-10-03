/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example.ui.components

import android.os.Build
import android.telecom.Call
import android.telecom.Connection
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.CallManager
import com.example.ContactCache
import com.example.R
import com.example.model.Contact

@Composable
fun InCallHeader(
    isOnHold: Boolean,
    callState: Int,
    participants: List<Pair<String, String>>,
    preferredSim: String,
    contactName: String,
    contactNumber: String,
    formattedTime: String,
    heldCall: Call?,
    contacts: List<Contact>,
    onMerge: (() -> Unit)? = null,
    isConference: Boolean = false,
    isRecording: Boolean = false
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 8.dp)
    ) {
        val simDisplay = if (preferredSim.equals("Ask", ignoreCase = true)) {
            stringResource(R.string.sim_ask)
        } else {
            preferredSim
        }

        val displayHeader = when {
            callState == Call.STATE_DISCONNECTED -> stringResource(R.string.call_status_ended)
            isOnHold || callState == Call.STATE_HOLDING -> stringResource(R.string.call_status_hold)
            callState == Call.STATE_DIALING -> stringResource(R.string.call_status_dialing)
            callState == Call.STATE_RINGING -> stringResource(R.string.call_status_ringing)
            callState == Call.STATE_CONNECTING -> stringResource(R.string.call_status_connecting)
            isConference || participants.size > 1 -> "${stringResource(R.string.call_status_conference)} • $simDisplay"
            else -> "${stringResource(R.string.call_status_ongoing)} • $simDisplay"
        }

        Text(
            text = displayHeader,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        val displayName = if (isConference || participants.size > 1) {
            if (participants.size >= 2) {
                "${participants[0].first.ifEmpty { participants[0].second }} & ${participants[1].first.ifEmpty { participants[1].second }}"
            } else if (participants.size == 1 && participants[0].first.isNotEmpty()) {
                participants[0].first
            } else {
                stringResource(R.string.call_status_conference)
            }
        } else {
            val rawName = contactName.ifEmpty { contactNumber }
            if (rawName == "Unknown") stringResource(R.string.unknown) else rawName
        }

        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        val displaySubtitle = if (isConference || participants.size > 1) {
            if (participants.size > 2) {
                participants.joinToString(", ") { it.first.ifEmpty { it.second } }
            } else if (participants.size == 2) {
                "${participants[0].second} • ${participants[1].second}"
            } else if (participants.size == 1 && participants[0].second.isNotEmpty()) {
                participants[0].second
            } else {
                ""
            }
        } else {
            if (contactName.isNotEmpty() && contactNumber.isNotEmpty() && contactName != contactNumber) contactNumber else ""
        }

        if (displaySubtitle.isNotEmpty()) {
            Text(
                text = displaySubtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // STIR/SHAKEN Anti-Spoofing Detection (Android 11+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && callState == Call.STATE_RINGING) {
            val activeCallObj = CallManager.currentCall.value
            val verificationStatus = activeCallObj?.details?.callerNumberVerificationStatus
            if (verificationStatus == Connection.VERIFICATION_STATUS_FAILED) {
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Potential Spoofed Number",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isRecording) {
                RecordingBadge()
            }

            if (callState == Call.STATE_ACTIVE || callState == Call.STATE_HOLDING) {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        if (heldCall != null) {
            Spacer(modifier = Modifier.height(16.dp))
            val heldNumber = heldCall.details?.handle?.schemeSpecificPart ?: ""
            val heldName = ContactCache.getContact(heldNumber)?.name
                ?: contacts.find { it.number == heldNumber }?.name
                ?: heldNumber

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text("⏸️", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${stringResource(R.string.on_hold_prefix)} $heldName",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (heldName != heldNumber && heldNumber.isNotEmpty()) {
                                Text(
                                    text = heldNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                try {
                                    val activeCall = CallManager.currentCall.value
                                    activeCall?.hold()
                                    CallManager.updateCall(heldCall)
                                } catch (_: Exception) {}
                            },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary,
                                contentColor = MaterialTheme.colorScheme.onSecondary
                            )
                        ) {
                            Text(stringResource(R.string.btn_swap), style = MaterialTheme.typography.labelMedium)
                        }
                        if (onMerge != null) {
                            Button(
                                onClick = onMerge,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.height(32.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Text(stringResource(R.string.btn_add_merge), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecordingBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "header_rec_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "header_rec_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "header_rec_alpha"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFB71C1C).copy(alpha = 0.15f),
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935).copy(alpha = pulseAlpha))
                )
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE53935))
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "REC",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE53935),
                fontSize = 11.sp
            )
        }
    }
}