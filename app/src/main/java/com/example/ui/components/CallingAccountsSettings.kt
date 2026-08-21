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
    cardBgColor: Color
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
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
