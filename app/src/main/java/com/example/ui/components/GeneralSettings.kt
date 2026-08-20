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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale
import com.example.R
import com.example.ui.viewmodel.DialerViewModel

@Composable
fun GeneralSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color,
    onNavigateToTab: (Int) -> Unit,
    onShowAbout: () -> Unit,
    onShowPrivacy: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE) }
    var flashAlertsEnabled by remember { mutableStateOf(prefs.getBoolean("flash_alerts_enabled", false)) }
    val isDarkTheme by viewModel.isDarkTheme
    val onThemeChange = { newVal: Boolean ->
        viewModel.updateDarkTheme(newVal)
    }
    val isAmoledMode by viewModel.isAmoledMode
    val onAmoledChange = { newVal: Boolean ->
        viewModel.updateAmoledMode(newVal)
    }
    val useDynamicColor by viewModel.useDynamicColor
    val onDynamicColorChange = { newVal: Boolean ->
        viewModel.updateUseDynamicColor(newVal)
    }
    val isM3Expressive by viewModel.isM3Expressive
    val onExpressiveChange = { newVal: Boolean ->
        viewModel.updateM3Expressive(newVal)
    }
    val dialpadTonesEnabled by viewModel.dialpadTonesEnabled
    val onTonesChange = { newVal: Boolean -> viewModel.dialpadTonesEnabled.value = newVal }
    val vibrateOnClickEnabled by viewModel.vibrateOnClickEnabled
    val onVibrateChange = { newVal: Boolean -> viewModel.vibrateOnClickEnabled.value = newVal }
    val preferredSim by viewModel.preferredSim
    val onSimChange = { newVal: String -> viewModel.updatePreferredSim(newVal) }
    val voicemailNumber by viewModel.voicemailNumber
    val isBiometricLockEnabled by viewModel.isBiometricLockEnabled
    val isPocketProtectionEnabled by viewModel.isPocketProtectionEnabled

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // Appearance Card
        item {
            PreferenceHeader(stringResource(R.string.settings_appearance))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBgColor
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column {
                    SettingsRowToggle(
                        title = stringResource(R.string.settings_dark_theme),
                        subtitle = stringResource(R.string.settings_dark_theme_sub),
                        checked = isDarkTheme,
                        onCheckedChange = onThemeChange,
                        icon = Icons.Default.DarkMode,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                    
                    if (isDarkTheme) {
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        SettingsRowToggle(
                            title = stringResource(R.string.settings_pure_black),
                            subtitle = stringResource(R.string.settings_pure_black_sub),
                            checked = isAmoledMode,
                            onCheckedChange = onAmoledChange,
                            icon = Icons.Default.Contrast,
                            iconBgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                            iconTint = MaterialTheme.colorScheme.tertiary
                        )
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    val isDynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    SettingsRowToggle(
                        title = stringResource(R.string.settings_dynamic_color),
                        subtitle = if (isDynamicSupported) {
                            stringResource(R.string.settings_dynamic_color_sub)
                        } else {
                            stringResource(R.string.settings_dynamic_color_sub_fallback)
                        },
                        checked = useDynamicColor && isDynamicSupported,
                        onCheckedChange = { if (isDynamicSupported) onDynamicColorChange(it) },
                        icon = Icons.Default.Palette,
                        iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        iconTint = MaterialTheme.colorScheme.secondary,
                        enabled = isDynamicSupported
                    )

                    if (!useDynamicColor || !isDynamicSupported) {
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ThemeColorPicker(
                            currentSelected = viewModel.themeColor.value,
                            customColorHex = viewModel.customColorHex.value,
                            onColorSelected = { viewModel.updateThemeColor(it) },
                            onCustomColorChange = { viewModel.updateCustomColorHex(it) }
                        )
                    }

                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    SettingsRowToggle(
                        title = stringResource(R.string.settings_m3_expressive),
                        subtitle = stringResource(R.string.settings_m3_expressive_sub),
                        checked = isM3Expressive,
                        onCheckedChange = onExpressiveChange,
                        icon = Icons.Default.AutoAwesome,
                        iconBgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        // Sound & Haptics Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            PreferenceHeader(stringResource(R.string.settings_sound_haptics))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBgColor
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column {
                    SettingsRowToggle(
                        title = stringResource(R.string.settings_dialpad_tones),
                        subtitle = stringResource(R.string.settings_dialpad_tones_sub),
                        checked = dialpadTonesEnabled,
                        onCheckedChange = onTonesChange,
                        icon = Icons.AutoMirrored.Filled.VolumeUp,
                        iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRowToggle(
                        title = stringResource(R.string.settings_vibrate),
                        subtitle = stringResource(R.string.settings_vibrate_sub),
                        checked = vibrateOnClickEnabled,
                        onCheckedChange = onVibrateChange,
                        icon = Icons.Default.Vibration,
                        iconBgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        // Calls & Blocking Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            PreferenceHeader(stringResource(R.string.settings_calls_blocking))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBgColor
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column {
                    SettingsRowNav(
                        title = stringResource(R.string.settings_speed_dial),
                        subtitle = stringResource(R.string.settings_speed_dial_sub),
                        onClick = { onNavigateToTab(2) },
                        icon = Icons.Default.Speed,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRowNav(
                        title = stringResource(R.string.settings_blocked_numbers),
                        subtitle = stringResource(R.string.settings_blocked_numbers_sub),
                        onClick = { onNavigateToTab(1) },
                        icon = Icons.Default.Block,
                        iconBgColor = MaterialTheme.colorScheme.errorContainer,
                        iconTint = MaterialTheme.colorScheme.error
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRowNav(
                        title = stringResource(R.string.settings_quick_responses),
                        subtitle = stringResource(R.string.settings_quick_responses_sub),
                        onClick = { onNavigateToTab(3) },
                        icon = Icons.AutoMirrored.Filled.Message,
                        iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRowNav(
                        title = stringResource(R.string.settings_spam_protection),
                        subtitle = stringResource(R.string.settings_spam_protection_sub),
                        onClick = { onNavigateToTab(9) },
                        icon = Icons.Default.Shield,
                        iconBgColor = MaterialTheme.colorScheme.errorContainer,
                        iconTint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // SIM & Voicemail Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            PreferenceHeader(stringResource(R.string.settings_sim_voicemail))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBgColor
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column {
                    SettingsPreferredSimRow(
                        preferredSim = preferredSim,
                        onSimChange = onSimChange,
                        haptic = haptic
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRowNav(
                        title = stringResource(R.string.settings_voicemail_num),
                        subtitle = voicemailNumber.ifEmpty { stringResource(R.string.not_set) },
                        onClick = { onNavigateToTab(4) },
                        icon = Icons.Default.Voicemail,
                        iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        // Startup Options Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            PreferenceHeader(stringResource(R.string.settings_startup_options))
            DefaultStartupTabCard(viewModel = viewModel, cardBgColor = cardBgColor)
        }

        // Calling Features Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            PreferenceHeader(stringResource(R.string.settings_calling_features))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                Column {
                    val callWaiting = viewModel.callWaitingEnabled.value
                    val callRecording = viewModel.recordingEnabled.value
                    SettingsRowToggle(
                        title = stringResource(R.string.settings_call_waiting),
                        subtitle = stringResource(R.string.settings_call_waiting_sub),
                        checked = callWaiting,
                        onCheckedChange = { viewModel.updateCallWaitingEnabled(it) },
                        icon = Icons.Default.NetworkCell,
                        iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRowToggle(
                        title = stringResource(R.string.settings_call_recording),
                        subtitle = stringResource(R.string.settings_call_recording_sub),
                        checked = callRecording,
                        onCheckedChange = { viewModel.updateRecordingEnabled(it) },
                        icon = Icons.Default.Mic,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRowNav(
                        title = stringResource(R.string.settings_view_saved_recordings),
                        subtitle = stringResource(R.string.settings_view_saved_recordings_sub),
                        onClick = { onNavigateToTab(7) },
                        icon = Icons.Default.Audiotrack,
                        iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRowToggle(
                        title = stringResource(R.string.settings_flash_alerts),
                        subtitle = stringResource(R.string.settings_flash_alerts_sub),
                        checked = flashAlertsEnabled,
                        onCheckedChange = {
                            prefs.edit().putBoolean("flash_alerts_enabled", it).apply()
                            flashAlertsEnabled = it
                        },
                        icon = Icons.Default.FlashlightOn,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Security & Protection Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            PreferenceHeader(stringResource(R.string.settings_security_protection))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                Column {
                    SettingsRowToggle(
                        title = stringResource(R.string.settings_biometric_lock),
                        subtitle = stringResource(R.string.settings_biometric_lock_sub),
                        checked = isBiometricLockEnabled,
                        onCheckedChange = { viewModel.updateBiometricLockEnabled(it) },
                        icon = Icons.Default.Lock,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRowToggle(
                        title = stringResource(R.string.settings_pocket_protection),
                        subtitle = stringResource(R.string.settings_pocket_protection_sub),
                        checked = isPocketProtectionEnabled,
                        onCheckedChange = { viewModel.updatePocketProtectionEnabled(it) },
                        icon = Icons.Default.ScreenLockPortrait,
                        iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRowNav(
                        title = stringResource(R.string.settings_fake_call_sim),
                        subtitle = stringResource(R.string.settings_fake_call_sim_sub),
                        onClick = { onNavigateToTab(11) },
                        icon = Icons.Default.DirectionsRun,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Information Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            PreferenceHeader(stringResource(R.string.settings_info_utilities))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBgColor
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column {
                    SettingsRowNav(
                        title = stringResource(R.string.backup_service_health_title),
                        subtitle = stringResource(R.string.backup_service_health_sub),
                        onClick = { onNavigateToTab(8) },
                        icon = Icons.Default.Shield,
                        iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRowNav(
                        title = stringResource(R.string.settings_callback_reminders),
                        subtitle = stringResource(R.string.settings_callback_reminders_sub),
                        onClick = { onNavigateToTab(10) },
                        icon = Icons.Default.NotificationsActive,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRowNav(
                        title = stringResource(R.string.settings_all_call_notes),
                        subtitle = stringResource(R.string.settings_all_call_notes_sub),
                        onClick = { onNavigateToTab(12) },
                        icon = Icons.Default.Description,
                        iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRowNav(
                        title = stringResource(R.string.settings_check_updates),
                        subtitle = stringResource(R.string.settings_check_updates_sub),
                        onClick = { onNavigateToTab(6) },
                        icon = Icons.Default.SystemUpdateAlt,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRowNav(
                        title = stringResource(R.string.settings_about),
                        subtitle = stringResource(R.string.settings_about_sub),
                        onClick = onShowAbout,
                        icon = Icons.Default.Info,
                        iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    SettingsRowNav(
                        title = stringResource(R.string.settings_privacy),
                        subtitle = stringResource(R.string.settings_privacy_sub),
                        onClick = onShowPrivacy,
                        icon = Icons.Default.Security,
                        iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Contribution Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            PreferenceHeader(stringResource(R.string.settings_contribution))
            GeneralSettingsSupportCard(context)
        }
    }
}

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
