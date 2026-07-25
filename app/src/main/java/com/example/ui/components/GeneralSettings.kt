package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
                    ThemeColorPicker(
                        currentSelected = viewModel.themeColor.value,
                        onColorSelected = { viewModel.updateThemeColor(it) }
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
                        icon = Icons.Default.VolumeUp,
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
                        icon = Icons.Default.Message,
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.settings_default_startup_tab),
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    val tabs = listOf(
                        stringResource(R.string.tab_recents),
                        stringResource(R.string.tab_contacts),
                        stringResource(R.string.tab_dialpad)
                    )
                    val currentTabSelected = viewModel.defaultTab.intValue
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        tabs.forEachIndexed { idx, title ->
                            val isSel = currentTabSelected == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(MaterialTheme.shapes.extraSmall)
                                    .background(
                                        if (isSel) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .clickable { viewModel.updateDefaultTab(idx) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSel) MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
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
                        title = "Backup & Service Health",
                        subtitle = "Encrypted local JSON backup, restore & watchdog status",
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
                        icon = Icons.Default.MergeType,
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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                colors = CardDefaults.cardColors(
                    containerColor = cardBgColor
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.settings_support_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        stringResource(R.string.settings_support_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Secure-Phone-apps/Secure-Dialer"))
                            try { context.startActivity(intent) } catch (e: Exception) {}
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Icon(Icons.Default.Favorite, "Contribute")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.settings_contribute_github))
                    }
                }
            }
        }
    }
}

@Composable
fun SettingsPreferredSimRow(
    preferredSim: String,
    onSimChange: (String) -> Unit,
    haptic: HapticFeedback
) {
    ListItem(
        headlineContent = { 
            Text(
                stringResource(R.string.settings_preferred_sim),
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge
            ) 
        },
        supportingContent = { 
            Text(
                stringResource(R.string.settings_preferred_sim_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ) 
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SimCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val askLabel = stringResource(R.string.sim_ask)
                listOf("SIM 1" to "SIM 1", "SIM 2" to "SIM 2", "Ask" to askLabel).forEach { (opKey, labelText) ->
                    val sel = preferredSim == opKey
                    FilterChip(
                        selected = sel,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSimChange(opKey)
                        },
                        label = { 
                            Text(
                                labelText, 
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                            ) 
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}
