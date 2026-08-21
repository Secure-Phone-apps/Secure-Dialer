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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

@Composable
fun PrivacySecurityAboutSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color,
    onShowAbout: () -> Unit,
    onShowPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val isBiometricLockEnabled by viewModel.isBiometricLockEnabled
    val isPocketProtectionEnabled by viewModel.isPocketProtectionEnabled
    var showFakeCallDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // [Header] PRIVACY PROTECTION
        item {
            PreferenceHeader(stringResource(R.string.header_privacy_protection))
        }

        // Biometric Lock Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowToggle(
                    title = stringResource(R.string.settings_biometric_lock),
                    subtitle = stringResource(R.string.settings_biometric_lock_sub),
                    checked = isBiometricLockEnabled,
                    onCheckedChange = { viewModel.updateBiometricLockEnabled(it) },
                    icon = Icons.Default.Lock,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Pocket Protection Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowToggle(
                    title = stringResource(R.string.settings_pocket_protection),
                    subtitle = stringResource(R.string.settings_pocket_protection_sub),
                    checked = isPocketProtectionEnabled,
                    onCheckedChange = { viewModel.updatePocketProtectionEnabled(it) },
                    icon = Icons.Default.ScreenLockPortrait,
                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Fake Call Simulator Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.settings_fake_call_sim),
                    subtitle = stringResource(R.string.settings_fake_call_sim_sub),
                    onClick = { showFakeCallDialog = true },
                    icon = Icons.Default.PhoneCallback,
                    iconBgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // [Header] APP SPECIFICATIONS
        item {
            Spacer(modifier = Modifier.height(12.dp))
            PreferenceHeader(stringResource(R.string.header_app_specs))
        }

        // About Secure Dialer Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.settings_about),
                    subtitle = stringResource(R.string.settings_about_sub),
                    onClick = onShowAbout,
                    icon = Icons.Default.Info,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Privacy Policy Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.settings_privacy),
                    subtitle = stringResource(R.string.settings_privacy_sub),
                    onClick = onShowPrivacy,
                    icon = Icons.Default.PrivacyTip,
                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // [Header] COMMUNITY & SUPPORT
        item {
            Spacer(modifier = Modifier.height(12.dp))
            PreferenceHeader(stringResource(R.string.header_community))
        }

        // Support Open Source Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowNav(
                    title = stringResource(R.string.settings_contribute_github),
                    subtitle = stringResource(R.string.settings_support_desc),
                    onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com"))
                        try { context.startActivity(intent) } catch (e: Exception) {}
                    },
                    icon = Icons.Default.Code,
                    iconBgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }

    if (showFakeCallDialog) {
        AlertDialog(
            onDismissRequest = { showFakeCallDialog = false },
            title = { Text(stringResource(R.string.fake_call_sim_title)) },
            text = {
                FakeCallSettings(viewModel = viewModel, cardBgColor = cardBgColor)
            },
            confirmButton = {
                TextButton(onClick = { showFakeCallDialog = false }) {
                    Text(stringResource(R.string.close))
                }
            }
        )
    }
}
