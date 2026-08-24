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
import android.provider.Settings
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun CallingAccountsSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color,
    highlightedTitle: String? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val callWaitingEnabled by viewModel.callWaitingEnabled
    val flashAlertsEnabled by viewModel.flashAlertsEnabled

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // [Header] SIM & CARRIER CONFIGURATION
        item {
            PreferenceHeader(stringResource(R.string.header_sim_carrier))
        }

        // Preferred SIM
        item {
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Preferred SIM Card", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsPreferredSimRow(
                    preferredSim = viewModel.preferredSim.value,
                    onSimChange = { viewModel.updatePreferredSim(it) },
                    haptic = haptic
                )
            }
        }

        // Carrier Call Settings
        item {
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Call Forwarding", highlightedTitle) || isMatchTitle("Carrier", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.settings_carrier_call_settings),
                    subtitle = stringResource(R.string.settings_carrier_call_settings_sub),
                    onClick = {
                        val telecomIntent = Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS)
                        val wirelessIntent = Intent(Settings.ACTION_WIRELESS_SETTINGS)
                        val settingsIntent = Intent(Settings.ACTION_SETTINGS)
                        try {
                            if (telecomIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(telecomIntent)
                            } else if (wirelessIntent.resolveActivity(context.packageManager) != null) {
                                context.startActivity(wirelessIntent)
                            } else {
                                context.startActivity(settingsIntent)
                            }
                        } catch (e: Exception) {
                            try {
                                context.startActivity(settingsIntent)
                            } catch (e2: Exception) {
                                Toast.makeText(context, "Could not open phone account settings", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    icon = Icons.Default.SettingsPhone,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Call Waiting
        item {
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Call Waiting", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowToggle(
                    title = stringResource(R.string.settings_call_waiting),
                    subtitle = stringResource(R.string.settings_call_waiting_sub),
                    checked = callWaitingEnabled,
                    onCheckedChange = { viewModel.callWaitingEnabled.value = it },
                    icon = Icons.Default.PhonePaused,
                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // [Header] CARRIER VOICEMAIL
        item {
            Spacer(modifier = Modifier.height(16.dp))
            PreferenceHeader(stringResource(R.string.header_carrier_voicemail))
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Carrier Voicemail Setup", highlightedTitle) || isMatchTitle("Voicemail", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                VoicemailCallingAccountsRow(viewModel = viewModel)
            }
        }

        // [Header] INCOMING CALL ALERTS
        item {
            Spacer(modifier = Modifier.height(16.dp))
            PreferenceHeader(stringResource(R.string.header_incoming_alerts))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                Column {
                    SettingsRowToggle(
                        title = stringResource(R.string.settings_flash_alerts),
                        subtitle = stringResource(R.string.settings_flash_alerts_sub),
                        checked = flashAlertsEnabled,
                        onCheckedChange = { viewModel.flashAlertsEnabled.value = it },
                        icon = Icons.Default.FlashlightOn,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

    }
}

@Composable
private fun VoicemailCallingAccountsRow(
    viewModel: DialerViewModel
) {
    val context = LocalContext.current
    val voicemailNum by viewModel.voicemailNumber
    var voicemailInput by remember(voicemailNum) { mutableStateOf(voicemailNum) }
    var showVoicemailDialog by remember { mutableStateOf(false) }

    SettingsRowNav(
        title = stringResource(R.string.settings_voicemail_num),
        subtitle = if (voicemailNum.isNotBlank()) voicemailNum else stringResource(R.string.settings_voicemail_num_sub),
        onClick = { showVoicemailDialog = true },
        icon = Icons.Default.Voicemail,
        iconBgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
        iconTint = MaterialTheme.colorScheme.tertiary
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
