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

package com.example.ui.components

import android.telecom.Call
import android.telecom.VideoProfile
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.CallManager
import com.example.ContactCache
import com.example.R
import com.example.model.Contact
import com.example.model.getAvatarShape
import com.example.model.getInitials
import com.example.ui.theme.getCallGreenColor
import com.example.ui.theme.getDeclineRedColor
import com.example.ui.theme.getOnCallGreenColor
import com.example.ui.theme.getOnDeclineRedColor

@Composable
fun InCallWaitingCallDialog(
    waitingCall: Call,
    contacts: List<Contact>,
    avatarShapeType: String = "circular"
) {
    val waitingNumber = waitingCall.details?.handle?.schemeSpecificPart ?: ""
    // FIXED: Multi-number & normalized cache lookup with CNAP fallback
    val waitingName = remember(waitingNumber, contacts) {
        ContactCache.getContact(waitingNumber)?.name
            ?: contacts.find { it.number == waitingNumber }?.name
            ?: waitingCall.details?.callerDisplayName?.takeIf { it.isNotBlank() }
            ?: waitingNumber
    }

    val initials = getInitials(waitingName)
    val avatarShape = getAvatarShape(avatarShapeType)

    AlertDialog(
        // FIXED: Dismissing dialog allows user to continue using keypad/controls without rejecting caller
        onDismissRequest = { CallManager.updateWaitingCall(null) },
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        icon = {
            Icon(
                imageVector = Icons.Default.Call,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
        },
        title = {
            Text(
                text = stringResource(R.string.call_waiting_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = avatarShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.call_waiting_incoming_from),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = waitingName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (waitingName != waitingNumber && waitingNumber.isNotBlank()) {
                        Text(
                            text = waitingNumber,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.call_waiting_notice),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val activeCall = CallManager.currentCall.value
                        activeCall?.hold()
                        waitingCall.answer(VideoProfile.STATE_AUDIO_ONLY)
                        CallManager.updateCall(waitingCall)
                        CallManager.updateWaitingCall(null)
                    } catch (_: Exception) {}
                },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = getCallGreenColor(),
                    contentColor = getOnCallGreenColor()
                )
            ) {
                Text(stringResource(R.string.btn_answer_hold), fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Button(
                onClick = {
                    try {
                        waitingCall.reject(false, null)
                        CallManager.updateWaitingCall(null)
                    } catch (_: Exception) {}
                },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = getDeclineRedColor(),
                    contentColor = getOnDeclineRedColor()
                )
            ) {
                Text(stringResource(R.string.btn_decline), fontWeight = FontWeight.Bold)
            }
        }
    )
}