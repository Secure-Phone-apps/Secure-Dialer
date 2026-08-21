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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.viewmodel.DialerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoicemailAndToolsSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        Pair("Voicemail & Audio", Icons.Default.Voicemail),
        Pair("Reminders", Icons.Default.NotificationsActive),
        Pair("Call Notes", Icons.Default.Description)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, (title, icon) ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = { Text(title, maxLines = 1) },
                    icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedSubTab) {
                0 -> VoicemailAndRecordingSection(viewModel = viewModel, cardBgColor = cardBgColor)
                1 -> ScheduledRemindersSettings(viewModel = viewModel, cardBgColor = cardBgColor)
                2 -> CallNotesSettings(viewModel = viewModel, cardBgColor = cardBgColor)
            }
        }
    }
}

@Composable
private fun VoicemailAndRecordingSection(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val context = LocalContext.current
    var voicemailInput by remember { mutableStateOf(viewModel.voicemailNumber.value) }
    val isRecordingEnabled by viewModel.recordingEnabled

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // [Header] CARRIER VOICEMAIL
        item {
            PreferenceHeader(stringResource(R.string.header_carrier_voicemail))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Voicemail,
                                    null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.settings_voicemail_num),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(R.string.settings_voicemail_num_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    OutlinedTextField(
                        value = voicemailInput,
                        onValueChange = { voicemailInput = it },
                        label = { Text(stringResource(R.string.voicemail_directory_number)) },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.updateVoicemailNumber(voicemailInput)
                                Toast.makeText(context, context.getString(R.string.save_voicemail_number), Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.Save, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.save_voicemail_number))
                        }

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
                                },
                                shape = MaterialTheme.shapes.small
                            ) {
                                Icon(Icons.Default.Call, null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Call")
                            }
                        }
                    }
                }
            }
        }

        // [Header] CALL RECORDING
        item {
            Spacer(modifier = Modifier.height(16.dp))
            PreferenceHeader(stringResource(R.string.header_call_recording))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                Column {
                    SettingsRowToggle(
                        title = stringResource(R.string.settings_call_recording),
                        subtitle = stringResource(R.string.settings_call_recording_sub),
                        checked = isRecordingEnabled,
                        onCheckedChange = { viewModel.recordingEnabled.value = it },
                        icon = Icons.Default.Mic,
                        iconBgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        // Saved Recordings List
        item {
            Spacer(modifier = Modifier.height(8.dp))
            CallRecordingsSettings(viewModel = viewModel, cardBgColor = cardBgColor)
        }
    }
}
