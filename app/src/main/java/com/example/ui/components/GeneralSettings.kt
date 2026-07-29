package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
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
    val isDarkTheme by viewModel.isDarkTheme
    val onThemeChange = { newVal: Boolean ->
        viewModel.updateDarkTheme(newVal)
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
                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    val isDynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                    SettingsRowToggle(
                        title = stringResource(R.string.settings_dynamic_color),
                        subtitle = if (isDynamicSupported) {
                            stringResource(R.string.settings_dynamic_color_sub)
                        } else {
                            "System wallpaper color scheme (Requires Android 12+)"
                        },
                        checked = useDynamicColor && isDynamicSupported,
                        onCheckedChange = { if (isDynamicSupported) onDynamicColorChange(it) },
                        icon = Icons.Default.Palette,
                        iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                        iconTint = MaterialTheme.colorScheme.secondary,
                        enabled = isDynamicSupported
                    )
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

                    if (!useDynamicColor || !isDynamicSupported) {
                        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        ThemeColorPicker(
                            currentSelected = viewModel.themeColor.value,
                            onColorSelected = { viewModel.updateThemeColor(it) }
                        )
                    }
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
                        title = stringResource(R.string.settings_merge_duplicate_contacts),
                        subtitle = stringResource(R.string.settings_merge_duplicate_contacts_sub),
                        onClick = { onNavigateToTab(5) },
                        icon = Icons.AutoMirrored.Filled.MergeType,
                        iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconTint = MaterialTheme.colorScheme.secondary
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
