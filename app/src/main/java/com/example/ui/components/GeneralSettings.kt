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
import kotlinx.coroutines.launch
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalM3Expressive
import com.example.ui.viewmodel.DialerViewModel

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
    var selectedItemTitle by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val settingsIndex = remember {
        listOf(
            // ================= 1. APPEARANCE =================
            SettingsSearchItem(
                title = "Appearance Settings",
                description = "Dark mode, themes, colors & layout styles",
                categoryName = "Appearance",
                categoryTab = 1,
                icon = Icons.Default.Palette
            ),
            SettingsSearchItem(
                title = "Dark Mode & Theme Mode",
                description = "Switch between Dark, Light, or System default theme",
                categoryName = "Appearance",
                categoryTab = 1,
                icon = Icons.Default.DarkMode
            ),
            SettingsSearchItem(
                title = "Expressive Material 3 Layout",
                description = "Enable fluid Material 3 Expressive shapes, rounded cards & container layouts",
                categoryName = "Appearance",
                categoryTab = 1,
                icon = Icons.Default.Style
            ),
            SettingsSearchItem(
                title = "Dynamic Colors (Monet)",
                description = "Use system wallpaper colors for app accents and container backgrounds",
                categoryName = "Appearance",
                categoryTab = 1,
                icon = Icons.Default.ColorLens
            ),
            SettingsSearchItem(
                title = "Accent Color Palette",
                description = "Select custom accent color palette for buttons, sliders & highlights",
                categoryName = "Appearance",
                categoryTab = 1,
                icon = Icons.Default.Brush
            ),
            // ================= 2. SOUND & GESTURES =================
            SettingsSearchItem(
                title = "Sound & Gestures Settings",
                description = "Keypad tones, vibration & gestures",
                categoryName = "Sound & Gestures",
                categoryTab = 2,
                icon = Icons.Default.VolumeUp
            ),
            SettingsSearchItem(
                title = "Dialpad Keypad Tones",
                description = "Enable or disable DTMF keypad sounds when dialing numbers",
                categoryName = "Sound & Gestures",
                categoryTab = 2,
                icon = Icons.Default.Dialpad
            ),
            SettingsSearchItem(
                title = "Call Vibration & Haptics",
                description = "Configure call vibration, connection feedback & touch haptics",
                categoryName = "Sound & Gestures",
                categoryTab = 2,
                icon = Icons.Default.Vibration
            ),
            SettingsSearchItem(
                title = "Default Startup Tab & Home Layout",
                description = "Choose whether app opens to Recents, Contacts, or Dialpad layout",
                categoryName = "Sound & Gestures",
                categoryTab = 2,
                icon = Icons.Default.Home
            ),
            SettingsSearchItem(
                title = "Call Swipe Actions & Gestures",
                description = "Configure swipe to answer, swipe to reject, and in-call gestures",
                categoryName = "Sound & Gestures",
                categoryTab = 2,
                icon = Icons.Default.Gesture
            ),
            SettingsSearchItem(
                title = "Call Log Summary Dashboard & Analytics",
                description = "Analytics dashboard displaying total call duration, call counts, and stats in Navigation & Layout",
                categoryName = "Sound & Gestures",
                categoryTab = 2,
                icon = Icons.Default.BarChart
            ),

            // ================= 3. SIM & CALLING =================
            SettingsSearchItem(
                title = "SIM & Calling Accounts",
                description = "Dual SIM cards, call waiting, forwarding & voicemail",
                categoryName = "SIM & Calling",
                categoryTab = 3,
                icon = Icons.Default.SimCard
            ),
            SettingsSearchItem(
                title = "Preferred SIM Card",
                description = "Set default SIM for outgoing calls in dual SIM devices",
                categoryName = "SIM & Calling",
                categoryTab = 3,
                icon = Icons.Default.SimCard
            ),
            SettingsSearchItem(
                title = "Call Waiting",
                description = "Receive notifications for incoming calls while on an active call",
                categoryName = "SIM & Calling",
                categoryTab = 3,
                icon = Icons.Default.PhoneCallback
            ),
            SettingsSearchItem(
                title = "Call Forwarding",
                description = "Forward incoming calls to another phone number",
                categoryName = "SIM & Calling",
                categoryTab = 3,
                icon = Icons.Default.PhoneForwarded
            ),
            SettingsSearchItem(
                title = "Carrier Voicemail Setup",
                description = "Configure carrier voicemail number and quick dial action",
                categoryName = "SIM & Calling",
                categoryTab = 3,
                icon = Icons.Default.Voicemail
            ),

            // ================= 4. SPEED DIAL & QUICK REPLY =================
            SettingsSearchItem(
                title = "Speed Dial & Quick Reply Settings",
                description = "Number key shortcuts and quick decline SMS responses",
                categoryName = "Speed Dial & Quick Reply",
                categoryTab = 4,
                icon = Icons.Default.TouchApp
            ),
            SettingsSearchItem(
                title = "Speed Dial Shortcuts (Keys 1–9)",
                description = "Assign favorite contacts to dialpad number keys for 1-tap quick calling",
                categoryName = "Speed Dial & Quick Reply",
                categoryTab = 4,
                icon = Icons.Default.Speed
            ),
            SettingsSearchItem(
                title = "Quick Decline Text Replies",
                description = "Manage predefined SMS messages to decline incoming calls with text",
                categoryName = "Speed Dial & Quick Reply",
                categoryTab = 4,
                icon = Icons.Default.Sms
            ),

            // ================= 5. SPAM & CALL BLOCK =================
            SettingsSearchItem(
                title = "Spam & Call Block Settings",
                description = "Blocked numbers list, spam database & unknown number blocking",
                categoryName = "Spam & Call Block",
                categoryTab = 5,
                icon = Icons.Default.Shield
            ),
            SettingsSearchItem(
                title = "Blocked Numbers & Blacklist",
                description = "Add, manage, or remove phone numbers from your block list",
                categoryName = "Spam & Call Block",
                categoryTab = 5,
                icon = Icons.Default.Block
            ),
            SettingsSearchItem(
                title = "Offline Spam Database & Protection",
                description = "Identify spam calls locally without internet using built-in database",
                categoryName = "Spam & Call Block",
                categoryTab = 5,
                icon = Icons.Default.Security
            ),
            SettingsSearchItem(
                title = "Block Unknown & Private Calls",
                description = "Automatically decline calls from hidden, private, or unknown numbers",
                categoryName = "Spam & Call Block",
                categoryTab = 5,
                icon = Icons.Default.PhonelinkErase
            ),

            // ================= 6. CONTACTS & DATA =================
            SettingsSearchItem(
                title = "Contacts & Data Settings",
                description = "Account filters, vCard import/export & database backup",
                categoryName = "Contacts & Data",
                categoryTab = 6,
                icon = Icons.Default.Contacts
            ),
            SettingsSearchItem(
                title = "Account Display Filters",
                description = "Choose which account contacts to display (Google, Phone, SIM)",
                categoryName = "Contacts & Data",
                categoryTab = 6,
                icon = Icons.Default.FilterList
            ),
            SettingsSearchItem(
                title = "Default Save Account",
                description = "Set default account location for saving newly created contacts",
                categoryName = "Contacts & Data",
                categoryTab = 6,
                icon = Icons.Default.AccountBox
            ),
            SettingsSearchItem(
                title = "Export Contacts (vCard / .vcf)",
                description = "Export all contacts to a standard .vcf backup file",
                categoryName = "Contacts & Data",
                categoryTab = 6,
                icon = Icons.Default.Upload
            ),
            SettingsSearchItem(
                title = "Import Contacts",
                description = "Restore contacts from a saved .vcf file",
                categoryName = "Contacts & Data",
                categoryTab = 6,
                icon = Icons.Default.Download
            ),
            SettingsSearchItem(
                title = "Database Backup & Restore",
                description = "Backup or restore local call logs, settings, and app data",
                categoryName = "Contacts & Data",
                categoryTab = 6,
                icon = Icons.Default.Backup
            ),

            // ================= 7. ADVANCED TOOLS =================
            SettingsSearchItem(
                title = "Advanced Tools Settings",
                description = "Call recording, callback reminders, call notes & fake call simulator",
                categoryName = "Advanced Tools",
                categoryTab = 7,
                icon = Icons.Default.AutoAwesome
            ),
            SettingsSearchItem(
                title = "Call Recording & Local Audio Storage",
                description = "Enable in-call recording controls and view saved local call audio recordings",
                categoryName = "Advanced Tools",
                categoryTab = 7,
                icon = Icons.Default.Mic
            ),
            SettingsSearchItem(
                title = "Scheduled Callback Reminders Dashboard",
                description = "Dashboard for viewing and setting scheduled call alarms and reminders",
                categoryName = "Advanced Tools",
                categoryTab = 7,
                icon = Icons.Default.Schedule
            ),
            SettingsSearchItem(
                title = "Call Notes & Memos",
                description = "Create, view, and search notes linked to specific phone numbers",
                categoryName = "Advanced Tools",
                categoryTab = 7,
                icon = Icons.Default.NoteAlt
            ),
            SettingsSearchItem(
                title = "Fake Call Simulator",
                description = "Schedule simulated incoming phone calls with custom caller name & timer",
                categoryName = "Advanced Tools",
                categoryTab = 7,
                icon = Icons.Default.PhoneInTalk
            ),

            // ================= 8. PRIVACY & ABOUT =================
            SettingsSearchItem(
                title = "Privacy & Security Settings",
                description = "Biometric app lock, pocket protection, app lock & app specs",
                categoryName = "Privacy & About",
                categoryTab = 8,
                icon = Icons.Default.Lock
            ),
            SettingsSearchItem(
                title = "Biometric App Lock",
                description = "Require fingerprint or PIN lock to open dialer application",
                categoryName = "Privacy & About",
                categoryTab = 8,
                icon = Icons.Default.Fingerprint
            ),
            SettingsSearchItem(
                title = "Pocket Protection Mode",
                description = "Prevent accidental pocket touches using proximity sensor",
                categoryName = "Privacy & About",
                categoryTab = 8,
                icon = Icons.Default.PhonelinkLock
            ),
            SettingsSearchItem(
                title = "App Version & Device Specifications",
                description = "View application version, build numbers, and system permissions",
                categoryName = "Privacy & About",
                categoryTab = 8,
                icon = Icons.Default.Info
            ),
            SettingsSearchItem(
                title = "Help & Customer Support",
                description = "Send feedback or contact developer support",
                categoryName = "Privacy & About",
                categoryTab = 8,
                icon = Icons.Default.Help
            )
        )
    }

    val searchResults = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            emptyList()
        } else {
            val queryWords = searchQuery.trim().lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
            settingsIndex.filter { item ->
                val titleLower = item.title.lowercase()
                queryWords.all { word ->
                    titleLower.contains(word)
                }
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Search Bar at the top of settings
        item {
            SettingsSearchBar(
                searchQuery = searchQuery,
                onQueryChange = { searchQuery = it }
            )
        }

        if (searchQuery.isNotBlank()) {
            item {
                Text(
                    text = "SEARCH RESULTS (${searchResults.size})",
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
                            title = "No Matching Settings",
                            description = "Try searching for terms like 'Dark Mode', 'Ringtone', 'Layout', 'Dashboard', 'SIM', 'Block', or 'Record'.",
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
            // Standard category cards when not searching
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
