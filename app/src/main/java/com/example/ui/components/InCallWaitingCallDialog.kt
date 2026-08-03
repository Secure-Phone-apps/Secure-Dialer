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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.CallManager
import com.example.R
import com.example.model.Contact

@Composable
fun InCallWaitingCallDialog(
    waitingCall: android.telecom.Call,
    contacts: List<Contact>
) {
    val waitingNumber = waitingCall.details?.handle?.schemeSpecificPart ?: ""
    val waitingName = remember(waitingNumber, contacts) {
        contacts.find { it.number == waitingNumber }?.name ?: waitingNumber
    }

    AlertDialog(
        onDismissRequest = { /* Force explicit choice */ },
        title = { Text(stringResource(R.string.call_waiting_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.call_waiting_incoming_from), style = MaterialTheme.typography.bodyMedium)
                Text(waitingName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (waitingName != waitingNumber) {
                    Text(waitingNumber, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(stringResource(R.string.call_waiting_notice), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    try {
                        val activeCall = CallManager.currentCall.value
                        activeCall?.hold()
                        waitingCall.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
                        CallManager.updateCall(waitingCall)
                        CallManager.updateWaitingCall(null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text(stringResource(R.string.btn_answer_hold))
            }
        },
        dismissButton = {
            Button(
                onClick = {
                    try {
                        waitingCall.reject(false, null)
                        CallManager.updateWaitingCall(null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text(stringResource(R.string.btn_decline))
            }
        }
    )
}
