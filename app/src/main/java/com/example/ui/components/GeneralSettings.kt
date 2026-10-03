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

    val settingsIndex = remember {
        listOf(
            // 1. APPEARANCE
            SettingsSearchItem("Appearance Settings", "Dark mode, themes, colors & layout styles", "Appearance", 1, Icons.Default.Palette),
            SettingsSearchItem("Dark Mode & Theme Mode", "Switch between Dark, Light, or System default theme", "Appearance", 1, Icons.Default.DarkMode),
            SettingsSearchItem("True Black OLED Mode", "Pure pitch-black #000000 theme to maximize battery savings on AMOLED displays", "Appearance", 1, Icons.Default.Contrast),
            SettingsSearchItem("Expressive Material 3 Layout", "Enable fluid Material 3 Expressive shapes, rounded cards & container layouts", "Appearance", 1, Icons.Default.Style),
            SettingsSearchItem("Dynamic Colors (Monet)", "Use system wallpaper colors for app accents and container backgrounds", "Appearance", 1, Icons.Default.ColorLens),
            SettingsSearchItem("Accent Color Palette", "Select custom accent color palette for buttons, sliders & highlights", "Appearance", 1, Icons.Default.Brush),

            // 2. SOUND & GESTURES
            SettingsSearchItem("Sound & Gestures Settings", "Keypad tones, vibration & gestures", "Sound & Gestures", 2, Icons.Default.VolumeUp),
            SettingsSearchItem("Dialpad Keypad Tones", "Enable or disable DTMF keypad sounds when dialing numbers", "Sound & Gestures", 2, Icons.Default.Dialpad),
            SettingsSearchItem("Call Vibration & Haptics", "Configure call vibration, connection feedback & touch haptics", "Sound & Gestures", 2, Icons.Default.Vibration),
            SettingsSearchItem("Default Startup Tab & Home Layout", "Choose whether app opens to Recents, Contacts, or Dialpad layout", "Sound & Gestures", 2, Icons.Default.Home),
            SettingsSearchItem("Call Swipe Actions & Gestures", "Configure swipe to answer, swipe to reject, and in-call gestures", "Sound & Gestures", 2, Icons.Default.Gesture),
            SettingsSearchItem("Call Log Summary Dashboard & Analytics", "Analytics dashboard displaying total call duration, call counts, and stats", "Sound & Gestures", 2, Icons.Default.BarChart),

            // 3. SIM & CALLING
            SettingsSearchItem("SIM & Calling Accounts", "Dual SIM cards, call waiting, forwarding & voicemail", "SIM & Calling", 3, Icons.Default.SimCard),
            SettingsSearchItem("Preferred SIM Card", "Set default SIM for outgoing calls in dual SIM devices", "SIM & Calling", 3, Icons.Default.SimCard),
            SettingsSearchItem("Call Waiting", "Receive notifications for incoming calls while on an active call", "SIM & Calling", 3, Icons.Default.PhoneCallback),
            SettingsSearchItem("Call Forwarding", "Forward incoming calls to another phone number", "SIM & Calling", 3, Icons.Default.PhoneForwarded),
            SettingsSearchItem("Carrier Voicemail Setup", "Configure carrier voicemail number and quick dial action", "SIM & Calling", 3, Icons.Default.Voicemail),
            SettingsSearchItem("Hide Caller ID (CLIR)", "Suppress outgoing number so recipient sees Private or Unknown Caller", "SIM & Calling", 3, Icons.Default.VisibilityOff),
            SettingsSearchItem("Caller ID Prefix & Codes", "Select carrier CLIR prefix: #31#, *67, 141, 1831 or custom dial code", "SIM & Calling", 3, Icons.Default.Pin),
            SettingsSearchItem("Carrier SIM Hardware Caller ID", "Direct system shortcut to per-SIM network Caller ID settings", "SIM & Calling", 3, Icons.Default.SimCard),

            // 4. SPEED DIAL & QUICK REPLY
            SettingsSearchItem("Speed Dial & Quick Reply Settings", "Number key shortcuts and quick decline SMS responses", "Speed Dial & Quick Reply", 4, Icons.Default.TouchApp),
            SettingsSearchItem("Speed Dial Shortcuts (Keys 1–9)", "Assign favorite contacts to dialpad number keys for 1-tap quick calling", "Speed Dial & Quick Reply", 4, Icons.Default.Speed),
            SettingsSearchItem("Quick Decline Text Replies", "Manage predefined SMS messages to decline incoming calls with text", "Speed Dial & Quick Reply", 4, Icons.Default.Sms),

            // 5. SPAM & CALL BLOCK
            SettingsSearchItem("Spam & Call Block Settings", "Blocked numbers list, spam database & unknown number blocking", "Spam & Call Block", 5, Icons.Default.Shield),
            SettingsSearchItem("Blocked Numbers & Blacklist", "Add, manage, or remove phone numbers from your block list", "Spam & Call Block", 5, Icons.Default.Block),
            SettingsSearchItem("Offline Spam Database & Protection", "Identify spam calls locally without internet using built-in database", "Spam & Call Block", 5, Icons.Default.Security),
            SettingsSearchItem("Block Unknown & Private Calls", "Automatically decline calls from hidden, private, or unknown numbers", "Spam & Call Block", 5, Icons.Default.PhonelinkErase),

            // 6. CONTACTS & DATA
            SettingsSearchItem("Contacts & Data Settings", "Account filters, vCard import/export & database backup", "Contacts & Data", 6, Icons.Default.Contacts),
            SettingsSearchItem("Account Display Filters", "Choose which account contacts to display (Google, Phone, SIM)", "Contacts & Data", 6, Icons.Default.FilterList),
            SettingsSearchItem("Default Save Account", "Set default account location for saving newly created contacts", "Contacts & Data", 6, Icons.Default.AccountBox),
            SettingsSearchItem("Export Contacts (vCard / .vcf)", "Export all contacts to a standard .vcf backup file", "Contacts & Data", 6, Icons.Default.Upload),
            SettingsSearchItem("Import Contacts", "Restore contacts from a saved .vcf file", "Contacts & Data", 6, Icons.Default.Download),
            SettingsSearchItem("Database Backup & Restore", "Backup or restore local call logs, settings, and app data", "Contacts & Data", 6, Icons.Default.Backup),

            // 7. ADVANCED TOOLS
            SettingsSearchItem("Advanced Tools Settings", "Call recording, callback reminders, call notes & fake call simulator", "Advanced Tools", 7, Icons.Default.AutoAwesome),
            SettingsSearchItem("Call Recording & Local Audio Storage", "Enable in-call recording controls and view saved local call audio recordings", "Advanced Tools", 7, Icons.Default.Mic),
            SettingsSearchItem("Scheduled Callback Reminders Dashboard", "Dashboard for viewing and setting scheduled call alarms and reminders", "Advanced Tools", 7, Icons.Default.Schedule),
            SettingsSearchItem("Call Notes & Memos", "Create, view, and search notes linked to specific phone numbers", "Advanced Tools", 7, Icons.Default.NoteAlt),
            SettingsSearchItem("Fake Call Simulator", "Schedule simulated incoming phone calls with custom caller name & timer", "Advanced Tools", 7, Icons.Default.PhoneInTalk),

            // 8. PRIVACY & ABOUT
            SettingsSearchItem("Privacy & Security Settings", "Biometric app lock, pocket protection, app lock & app specs", "Privacy & About", 8, Icons.Default.Lock),
            SettingsSearchItem("Biometric App Lock", "Require fingerprint or PIN lock to open dialer application", "Privacy & About", 8, Icons.Default.Fingerprint),
            SettingsSearchItem("Pocket Protection Mode", "Prevent accidental pocket touches using proximity sensor", "Privacy & About", 8, Icons.Default.PhonelinkLock),
            SettingsSearchItem("App Version & Device Specifications", "View application version, build numbers, and system permissions", "Privacy & About", 8, Icons.Default.Info),
            SettingsSearchItem("Help & Customer Support", "Send feedback or contact developer support", "Privacy & About", 8, Icons.Default.Help)
        )
    }

    // FIXED: Search matches title, description, and category name to enable full discoverability
    val searchResults = remember(searchQuery) {
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
                            onNavigateToTab(resultItem.categoryTab, resultItem.title)
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
            if (!isDefaultDialer) {
                item {
                    DefaultDialerWarningCard(
                        onShowRestrictedSettings = {
                            try {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                                    val rm = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
                                    if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_DIALER)) {
                                        val roleIntent = rm.createRequestRoleIntent(RoleManager.ROLE_DIALER).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(roleIntent)
                                    }
                                } else {
                                    @Suppress("DEPRECATION")
                                    val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                                        putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    }
                                    context.startActivity(intent)
                                }
                            } catch (_: Exception) {}
                        }
                    )
                }
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