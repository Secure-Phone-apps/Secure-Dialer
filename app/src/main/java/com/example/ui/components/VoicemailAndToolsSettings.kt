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

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.viewmodel.DialerViewModel

@Composable
fun VoicemailAndToolsSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val scrollState = rememberScrollState()
    val recordings by viewModel.recordingsFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Feature Container 1: Carrier Voicemail
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                VoicemailSection(viewModel = viewModel, cardBgColor = cardBgColor)
            }
        }

        // Feature Container 2: Call Recording (Expandable)
        ExpandableSettingsCard(
            title = stringResource(R.string.header_call_recording),
            subtitle = stringResource(R.string.settings_call_recording_sub),
            icon = Icons.Default.Mic,
            iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
            iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
            cardBgColor = cardBgColor,
            badgeText = if (recordings.isNotEmpty()) "${recordings.size} files" else null,
            initiallyExpanded = true
        ) {
            CallRecordingSection(viewModel = viewModel, cardBgColor = cardBgColor)
        }
    }
}

@Composable
private fun VoicemailSection(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val context = LocalContext.current
    var voicemailInput by remember { mutableStateOf(viewModel.voicemailNumber.value) }
    var showVoicemailDialog by remember { mutableStateOf(false) }

    SettingsRowNav(
        title = stringResource(R.string.header_carrier_voicemail),
        subtitle = if (voicemailInput.isNotBlank()) voicemailInput else stringResource(R.string.settings_voicemail_num_sub),
        onClick = { showVoicemailDialog = true },
        icon = Icons.Default.Voicemail,
        iconBgColor = MaterialTheme.colorScheme.primaryContainer,
        iconTint = MaterialTheme.colorScheme.onPrimaryContainer
    )

    if (showVoicemailDialog) {
        AlertDialog(
            onDismissRequest = { showVoicemailDialog = false },
            title = { Text(stringResource(R.string.settings_voicemail_num)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        stringResource(R.string.settings_voicemail_num_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = voicemailInput,
                        onValueChange = { voicemailInput = it },
                        label = { Text(stringResource(R.string.voicemail_directory_number)) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (voicemailInput.isNotBlank()) {
                        OutlinedButton(
                            onClick = {
                                try {
                                    val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$voicemailInput"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    try {
                                        val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$voicemailInput"))
                                        context.startActivity(dialIntent)
                                    } catch (e2: Exception) {
                                        Toast.makeText(context, "Unable to place call", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Call, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Call")
                        }
                    }
                    Button(
                        onClick = {
                            viewModel.updateVoicemailNumber(voicemailInput)
                            Toast.makeText(context, context.getString(R.string.save_voicemail_number), Toast.LENGTH_SHORT).show()
                            showVoicemailDialog = false
                        }
                    ) {
                        Text(stringResource(R.string.btn_save))
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoicemailDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
private fun CallRecordingSection(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val isRecordingEnabled by viewModel.recordingEnabled

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        SettingsRowToggle(
            title = stringResource(R.string.header_call_recording),
            subtitle = stringResource(R.string.settings_call_recording_sub),
            checked = isRecordingEnabled,
            onCheckedChange = { viewModel.recordingEnabled.value = it },
            icon = Icons.Default.Mic,
            iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
            iconTint = MaterialTheme.colorScheme.onTertiaryContainer
        )

        // Saved Recordings List
        Spacer(modifier = Modifier.height(8.dp))
        CallRecordingsSettings(viewModel = viewModel, cardBgColor = cardBgColor)
    }
}
