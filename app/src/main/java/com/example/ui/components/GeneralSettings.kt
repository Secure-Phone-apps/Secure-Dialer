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

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telecom.TelecomManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalM3Expressive
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.RichHapticEngine
import java.util.Locale

data class SettingsSearchItem(
    val highlightKey: String,
    val title: String,
    val description: String,
    val categoryName: String,
    val categoryTab: Int,
    val icon: ImageVector
)

@Composable
fun SettingsSearchBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit
) {
    val searchShape = RoundedCornerShape(16.dp)
    val isExpressive = LocalM3Expressive.current
    val containerColor = if (isExpressive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 4.dp),
        shape = searchShape,
        color = containerColor,
        tonalElevation = if (isExpressive) 6.dp else 3.dp
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .height(44.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = stringResource(R.string.search_settings_placeholder),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.weight(1.0f)) {
                if (searchQuery.isEmpty()) {
                    Text(
                        text = stringResource(R.string.search_settings_placeholder),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (searchQuery.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.search_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun GeneralSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color,
    onNavigateToTab: (Int, String?) -> Unit
) {
    val context = LocalContext.current
    val isDefaultDialer = viewModel.isDefaultDialer.value
    var searchQuery by remember { mutableStateOf("") }

    val configuration = LocalConfiguration.current
    val catAppearance = context.getString(R.string.search_cat_appearance)
    val catSoundGestures = context.getString(R.string.search_cat_sound_gestures)
    val catSimCalling = context.getString(R.string.search_cat_sim_calling)
    val catSpeedDialReply = context.getString(R.string.search_cat_speed_dial_reply)
    val catSpamBlock = context.getString(R.string.search_cat_spam_block)
    val catContactsData = context.getString(R.string.search_cat_contacts_data)
    val catAdvancedTools = context.getString(R.string.search_cat_advanced_tools)
    val catPrivacyAbout = context.getString(R.string.search_cat_privacy_about)

    val settingsIndex = remember(configuration, context) {
        listOf(
            // 1. APPEARANCE
            SettingsSearchItem("Appearance Settings", context.getString(R.string.search_item_appearance), context.getString(R.string.search_desc_appearance), catAppearance, 1, Icons.Default.Palette),
            SettingsSearchItem("Dark Mode & Theme Mode", context.getString(R.string.search_item_dark_mode), context.getString(R.string.search_desc_dark_mode), catAppearance, 1, Icons.Default.DarkMode),
            SettingsSearchItem("True Black OLED Mode", context.getString(R.string.search_item_true_black_oled), context.getString(R.string.search_desc_true_black_oled), catAppearance, 1, Icons.Default.Contrast),
            SettingsSearchItem("Expressive Material 3 Layout", context.getString(R.string.search_item_expressive_m3), context.getString(R.string.search_desc_expressive_m3), catAppearance, 1, Icons.Default.Style),
            SettingsSearchItem("Dynamic Colors (Monet)", context.getString(R.string.search_item_dynamic_colors), context.getString(R.string.search_desc_dynamic_colors), catAppearance, 1, Icons.Default.ColorLens),
            SettingsSearchItem("Accent Color Palette", context.getString(R.string.search_item_accent_palette), context.getString(R.string.search_desc_accent_palette), catAppearance, 1, Icons.Default.Brush),
            SettingsSearchItem("App Language", context.getString(R.string.settings_app_language), context.getString(R.string.settings_app_language_sub), catAppearance, 1, Icons.Default.Language),

            // 2. SOUND & GESTURES
            SettingsSearchItem("Sound & Gestures Settings", context.getString(R.string.search_item_sound_gestures), context.getString(R.string.search_desc_sound_gestures), catSoundGestures, 2, Icons.Default.VolumeUp),
            SettingsSearchItem("Incoming Call Display (Screen Active)", context.getString(R.string.search_item_incoming_screen_on), context.getString(R.string.search_desc_incoming_screen_on), catSoundGestures, 2, Icons.Default.PhoneInTalk),
            SettingsSearchItem("Incoming Call Display (Screen Off / Locked)", context.getString(R.string.search_item_incoming_screen_off), context.getString(R.string.search_desc_incoming_screen_off), catSoundGestures, 2, Icons.Default.Fullscreen),
            SettingsSearchItem("Dialpad Keypad Tones", context.getString(R.string.search_item_keypad_tones), context.getString(R.string.search_desc_keypad_tones), catSoundGestures, 2, Icons.Default.Dialpad),
            SettingsSearchItem("Call Vibration & Haptics", context.getString(R.string.search_item_call_vibration), context.getString(R.string.search_desc_call_vibration), catSoundGestures, 2, Icons.Default.Vibration),
            SettingsSearchItem("Default Startup Tab & Home Layout", context.getString(R.string.search_item_startup_tab), context.getString(R.string.search_desc_startup_tab), catSoundGestures, 2, Icons.Default.Home),
            SettingsSearchItem("Call Swipe Actions & Gestures", context.getString(R.string.search_item_swipe_actions), context.getString(R.string.search_desc_swipe_actions), catSoundGestures, 2, Icons.Default.Gesture),
            SettingsSearchItem("Call Log Summary Dashboard & Analytics", context.getString(R.string.search_item_call_log_summary), context.getString(R.string.search_desc_call_log_summary), catSoundGestures, 2, Icons.Default.BarChart),

            // 3. SIM & CALLING
            SettingsSearchItem("SIM & Calling Accounts", context.getString(R.string.search_item_sim_calling), context.getString(R.string.search_desc_sim_calling), catSimCalling, 3, Icons.Default.SimCard),
            SettingsSearchItem("Preferred SIM Card", context.getString(R.string.search_item_preferred_sim), context.getString(R.string.search_desc_preferred_sim), catSimCalling, 3, Icons.Default.SimCard),
            SettingsSearchItem("Call Waiting", context.getString(R.string.search_item_call_waiting), context.getString(R.string.search_desc_call_waiting), catSimCalling, 3, Icons.Default.PhoneCallback),
            SettingsSearchItem("Call Forwarding", context.getString(R.string.search_item_call_forwarding), context.getString(R.string.search_desc_call_forwarding), catSimCalling, 3, Icons.Default.PhoneForwarded),
            SettingsSearchItem("Carrier Voicemail Setup", context.getString(R.string.search_item_voicemail_setup), context.getString(R.string.search_desc_voicemail_setup), catSimCalling, 3, Icons.Default.Voicemail),
            SettingsSearchItem("Hide Caller ID (CLIR)", context.getString(R.string.search_item_hide_caller_id), context.getString(R.string.search_desc_hide_caller_id), catSimCalling, 3, Icons.Default.VisibilityOff),
            SettingsSearchItem("Caller ID Prefix & Codes", context.getString(R.string.search_item_clir_prefix), context.getString(R.string.search_desc_clir_prefix), catSimCalling, 3, Icons.Default.Pin),
            SettingsSearchItem("Carrier SIM Hardware Caller ID", context.getString(R.string.search_item_carrier_hardware_clir), context.getString(R.string.search_desc_carrier_hardware_clir), catSimCalling, 3, Icons.Default.SimCard),
            SettingsSearchItem("Background & Battery Survivability", context.getString(R.string.search_item_battery_survivability), context.getString(R.string.search_desc_battery_survivability), catSimCalling, 9, Icons.Default.BatteryChargingFull),
            SettingsSearchItem("Disable Battery Optimization Warning", context.getString(R.string.search_item_disable_battery_warning), context.getString(R.string.search_desc_disable_battery_warning), catSimCalling, 9, Icons.Default.NotificationsPaused),

            // 4. SPEED DIAL & QUICK REPLY
            SettingsSearchItem("Speed Dial & Quick Reply Settings", context.getString(R.string.search_item_speed_dial_quick_reply), context.getString(R.string.search_desc_speed_dial_quick_reply), catSpeedDialReply, 4, Icons.Default.TouchApp),
            SettingsSearchItem("Speed Dial Shortcuts (Keys 1–9)", context.getString(R.string.search_item_speed_dial_shortcuts), context.getString(R.string.search_desc_speed_dial_shortcuts), catSpeedDialReply, 4, Icons.Default.Speed),
            SettingsSearchItem("Quick Decline Text Replies", context.getString(R.string.search_item_quick_decline_replies), context.getString(R.string.search_desc_quick_decline_replies), catSpeedDialReply, 4, Icons.Default.Sms),

            // 5. SPAM & CALL BLOCK
            SettingsSearchItem("Spam & Call Block Settings", context.getString(R.string.search_item_spam_call_block), context.getString(R.string.search_desc_spam_call_block), catSpamBlock, 5, Icons.Default.Shield),
            SettingsSearchItem("Blocked Numbers & Blacklist", context.getString(R.string.search_item_blocked_numbers), context.getString(R.string.search_desc_blocked_numbers), catSpamBlock, 5, Icons.Default.Block),
            SettingsSearchItem("Offline Spam Database & Protection", context.getString(R.string.search_item_offline_spam_db), context.getString(R.string.search_desc_offline_spam_db), catSpamBlock, 5, Icons.Default.Security),
            SettingsSearchItem("Block Unknown & Private Calls", context.getString(R.string.search_item_block_unknown_private), context.getString(R.string.search_desc_block_unknown_private), catSpamBlock, 5, Icons.Default.PhonelinkErase),

            // 6. CONTACTS & DATA
            SettingsSearchItem("Contacts & Data Settings", context.getString(R.string.search_item_contacts_data), context.getString(R.string.search_desc_contacts_data), catContactsData, 6, Icons.Default.Contacts),
            SettingsSearchItem("Name format & sort order", context.getString(R.string.search_item_name_format_sort), context.getString(R.string.search_desc_name_format_sort), catContactsData, 6, Icons.Default.SortByAlpha),
            SettingsSearchItem("Account Display Filters", context.getString(R.string.search_item_account_filters), context.getString(R.string.search_desc_account_filters), catContactsData, 6, Icons.Default.FilterList),
            SettingsSearchItem("Default Save Account", context.getString(R.string.search_item_default_save_account), context.getString(R.string.search_desc_default_save_account), catContactsData, 6, Icons.Default.AccountBox),
            SettingsSearchItem("Export Contacts (vCard / .vcf)", context.getString(R.string.search_item_export_vcard), context.getString(R.string.search_desc_export_vcard), catContactsData, 6, Icons.Default.Upload),
            SettingsSearchItem("Import Contacts", context.getString(R.string.search_item_import_contacts), context.getString(R.string.search_desc_import_contacts), catContactsData, 6, Icons.Default.Download),
            SettingsSearchItem("Database Backup & Restore", context.getString(R.string.search_item_db_backup_restore), context.getString(R.string.search_desc_db_backup_restore), catContactsData, 6, Icons.Default.Backup),

            // 7. ADVANCED TOOLS
            SettingsSearchItem("Advanced Tools Settings", context.getString(R.string.search_item_advanced_tools), context.getString(R.string.search_desc_advanced_tools), catAdvancedTools, 7, Icons.Default.AutoAwesome),
            SettingsSearchItem("Call Recording & Local Audio Storage", context.getString(R.string.search_item_call_recording_storage), context.getString(R.string.search_desc_call_recording_storage), catAdvancedTools, 7, Icons.Default.Mic),
            SettingsSearchItem("Scheduled Callback Reminders Dashboard", context.getString(R.string.search_item_callback_reminders), context.getString(R.string.search_desc_callback_reminders), catAdvancedTools, 7, Icons.Default.Schedule),
            SettingsSearchItem("Call Notes & Memos", context.getString(R.string.search_item_call_notes_memos), context.getString(R.string.search_desc_call_notes_memos), catAdvancedTools, 7, Icons.Default.NoteAlt),
            SettingsSearchItem("Fake Call Simulator", context.getString(R.string.search_item_fake_call_simulator), context.getString(R.string.search_desc_fake_call_simulator), catAdvancedTools, 7, Icons.Default.PhoneInTalk),
            SettingsSearchItem("Post-Call Quick Actions", context.getString(R.string.search_item_post_call_actions), context.getString(R.string.search_desc_post_call_actions), catAdvancedTools, 7, Icons.Default.CallEnd),
            SettingsSearchItem("Auto-Dismiss Countdown", context.getString(R.string.search_item_auto_dismiss_countdown), context.getString(R.string.search_desc_auto_dismiss_countdown), catAdvancedTools, 7, Icons.Default.Timer),
            SettingsSearchItem("Default WhatsApp Account", context.getString(R.string.search_item_default_whatsapp), context.getString(R.string.search_desc_default_whatsapp), catAdvancedTools, 7, Icons.Default.Chat),

            // 8. PRIVACY & ABOUT
            SettingsSearchItem("Privacy & Security Settings", context.getString(R.string.search_item_privacy_security), context.getString(R.string.search_desc_privacy_security), catPrivacyAbout, 8, Icons.Default.Lock),
            SettingsSearchItem("Biometric App Lock", context.getString(R.string.search_item_biometric_app_lock), context.getString(R.string.search_desc_biometric_app_lock), catPrivacyAbout, 8, Icons.Default.Fingerprint),
            SettingsSearchItem("Require Unlock to Answer Calls", context.getString(R.string.search_item_require_unlock_answer), context.getString(R.string.search_desc_require_unlock_answer), catPrivacyAbout, 8, Icons.Default.LockPerson),
            SettingsSearchItem("Pocket Protection Mode", context.getString(R.string.search_item_pocket_protection), context.getString(R.string.search_desc_pocket_protection), catPrivacyAbout, 8, Icons.Default.PhonelinkLock),
            SettingsSearchItem("App Version & Device Specifications", context.getString(R.string.search_item_app_version_specs), context.getString(R.string.search_desc_app_version_specs), catPrivacyAbout, 8, Icons.Default.Info),
            SettingsSearchItem("Help & Customer Support", context.getString(R.string.search_item_help_support), context.getString(R.string.search_desc_help_support), catPrivacyAbout, 8, Icons.Default.Help)
        )
    }

    // FIXED: Search matches title, description, and category name to enable full discoverability
    val searchResults = remember(searchQuery, settingsIndex) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            val queryWords = searchQuery.trim().lowercase(Locale.ROOT).split("\\s+".toRegex()).filter { it.isNotBlank() }
            settingsIndex.filter { item ->
                val fullSearchBlob = "${item.title} ${item.description} ${item.categoryName}".lowercase(Locale.ROOT)
                queryWords.all { word -> fullSearchBlob.contains(word) }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SettingsSearchBar(
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it }
            )
        }

        if (searchQuery.isNotBlank()) {
            item {
                Text(
                    text = stringResource(R.string.search_results_count, searchResults.size).uppercase(Locale.ROOT),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                )
            }

            if (searchResults.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        SettingsEmptyState(
                            icon = Icons.Default.SearchOff,
                            title = stringResource(R.string.no_matching_settings),
                            description = stringResource(R.string.no_matching_settings_desc),
                            tintColor = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            } else {
                items(searchResults) { resultItem ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBgColor),
                        shape = MaterialTheme.shapes.medium,
                        onClick = {
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                            onNavigateToTab(resultItem.categoryTab, resultItem.highlightKey)
                            searchQuery = ""
                        }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = resultItem.icon,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = resultItem.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${resultItem.categoryName} • ${resultItem.description}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        } else {
            // Standard category cards
            // System & Calling permissions
            item {
                CallSystemPermissionsCard(
                    viewModel = viewModel,
                    onLearnMoreBattery = { onNavigateToTab(9, null) }
                )
            }

            // 1. Appearance
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
                        onClick = { onNavigateToTab(1, null) },
                        icon = Icons.Default.Palette,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 2. Sound & Gestures
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
                        onClick = { onNavigateToTab(2, null) },
                        icon = Icons.Default.VolumeUp,
                        iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // 3. SIM & Calling
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
                        onClick = { onNavigateToTab(3, null) },
                        icon = Icons.Default.SimCard,
                        iconBgColor = MaterialTheme.colorScheme.tertiaryContainer,
                        iconTint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            // 4. Speed Dial & Quick Reply
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
                        onClick = { onNavigateToTab(4, null) },
                        icon = Icons.Default.TouchApp,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 5. Spam & Call Block
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
                        onClick = { onNavigateToTab(5, null) },
                        icon = Icons.Default.Shield,
                        iconBgColor = MaterialTheme.colorScheme.errorContainer,
                        iconTint = MaterialTheme.colorScheme.error
                    )
                }
            }

            // 6. Contacts & Data
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
                        onClick = { onNavigateToTab(6, null) },
                        icon = Icons.Default.Contacts,
                        iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f),
                        iconTint = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            // 7. Advanced Tools
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
                        onClick = { onNavigateToTab(7, null) },
                        icon = Icons.Default.AutoAwesome,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // 8. Privacy & About
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
                        onClick = { onNavigateToTab(8, null) },
                        icon = Icons.Default.Lock,
                        iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}