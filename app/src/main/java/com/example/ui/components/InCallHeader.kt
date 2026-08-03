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

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.CallManager
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
    heldCall: android.telecom.Call?,
    contacts: List<Contact>
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
            isOnHold || callState == android.telecom.Call.STATE_HOLDING -> stringResource(R.string.call_status_hold)
            callState == android.telecom.Call.STATE_DIALING -> stringResource(R.string.call_status_dialing)
            callState == android.telecom.Call.STATE_RINGING -> stringResource(R.string.call_status_ringing)
            callState == android.telecom.Call.STATE_CONNECTING -> stringResource(R.string.call_status_connecting)
            participants.size > 1 -> "${stringResource(R.string.call_status_conference)} • $simDisplay"
            else -> "${stringResource(R.string.call_status_ongoing)} • $simDisplay"
        }

        Text(
            text = displayHeader,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        val displayName = if (participants.size > 1) {
            if (participants.size == 2) {
                "${participants[0].first.ifEmpty { participants[0].second }} & ${participants[1].first.ifEmpty { participants[1].second }}"
            } else {
                stringResource(R.string.conference, participants.size)
            }
        } else {
            val rawName = contactName.ifEmpty { contactNumber }
            if (rawName == "Unknown") stringResource(R.string.unknown) else rawName
        }

        Text(
            text = displayName,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        val displaySubtitle = if (participants.size > 1) {
            if (participants.size > 2) {
                participants.joinToString(", ") { it.first.ifEmpty { it.second } }
            } else {
                "${participants[0].second} • ${participants[1].second}"
            }
        } else {
            if (contactName.isNotEmpty() && contactNumber.isNotEmpty() && contactName != contactNumber) contactNumber else ""
        }

        if (displaySubtitle.isNotEmpty()) {
            Text(
                text = displaySubtitle,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = formattedTime,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )

        if (heldCall != null) {
            Spacer(modifier = Modifier.height(16.dp))
            val heldNumber = heldCall.details?.handle?.schemeSpecificPart ?: ""
            val heldName = contacts.find { it.number == heldNumber }?.name ?: heldNumber

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
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            if (heldName != heldNumber) {
                                Text(
                                    text = heldNumber,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    Button(
                        onClick = {
                            try {
                                val activeCall = CallManager.currentCall.value
                                activeCall?.hold()
                                heldCall.unhold()
                                CallManager.updateCall(heldCall)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(stringResource(R.string.btn_swap), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
