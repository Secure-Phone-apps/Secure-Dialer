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

@Composable
fun GeneralSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color,
    onNavigateToTab: (Int) -> Unit
) {
    val context = LocalContext.current
    val isDefaultDialer = viewModel.isDefaultDialer.value

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Warning Card (if not default dialer)
        if (!isDefaultDialer) {
            item {
                DefaultDialerWarningCard(
                    onShowRestrictedSettings = {
                        val telecomManager = context.getSystemService(android.content.Context.TELECOM_SERVICE) as? android.telecom.TelecomManager
                        val intent = android.content.Intent(android.telecom.TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                            putExtra(android.telecom.TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                        }
                        try {
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // Fallback
                        }
                    }
                )
            }
        }

        // 1. Appearance & Color
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.cat_appearance_color),
                    subtitle = stringResource(R.string.cat_appearance_color_sub),
                    onClick = { onNavigateToTab(1) },
                    icon = Icons.Default.Palette,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 2. Sound, Haptics & Gestures
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.cat_sound_gestures),
                    subtitle = stringResource(R.string.cat_sound_gestures_sub),
                    onClick = { onNavigateToTab(2) },
                    icon = Icons.Default.VolumeUp,
                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // 3. SIM & Calling Accounts
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.cat_calling_accounts),
                    subtitle = stringResource(R.string.cat_calling_accounts_sub),
                    onClick = { onNavigateToTab(3) },
                    icon = Icons.Default.SimCard,
                    iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // 4. Speed Dial & Quick Responses
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.cat_speed_dial_quick_responses),
                    subtitle = stringResource(R.string.cat_speed_dial_quick_responses_sub),
                    onClick = { onNavigateToTab(4) },
                    icon = Icons.Default.TouchApp,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 5. Spam Defense & Blocking
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.cat_call_blocking),
                    subtitle = stringResource(R.string.cat_call_blocking_sub),
                    onClick = { onNavigateToTab(5) },
                    icon = Icons.Default.Shield,
                    iconBgColor = MaterialTheme.colorScheme.errorContainer,
                    iconTint = MaterialTheme.colorScheme.error
                )
            }
        }

        // 6. Contacts & Data Management
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.cat_contacts_data),
                    subtitle = stringResource(R.string.cat_contacts_data_sub),
                    onClick = { onNavigateToTab(6) },
                    icon = Icons.Default.Contacts,
                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // 7. Advanced Features
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.cat_advanced_features),
                    subtitle = stringResource(R.string.cat_advanced_features_sub),
                    onClick = { onNavigateToTab(7) },
                    icon = Icons.Default.AutoAwesome,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // 8. Privacy, Security & About
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.cat_privacy_security_about),
                    subtitle = stringResource(R.string.cat_privacy_security_about_sub),
                    onClick = { onNavigateToTab(8) },
                    icon = Icons.Default.Lock,
                    iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                    iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
