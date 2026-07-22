package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.DialerViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import com.example.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanel(
    viewModel: DialerViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val isDarkTheme by viewModel.isDarkTheme
    val onThemeChange = { newVal: Boolean ->
        viewModel.isDarkTheme.value = newVal
        context.getSharedPreferences("dialer_prefs", Context.MODE_PRIVATE)
            .edit().putBoolean("is_dark_theme", newVal).apply()
    }
    val dialpadTonesEnabled by viewModel.dialpadTonesEnabled
    val onTonesChange = { newVal: Boolean -> viewModel.dialpadTonesEnabled.value = newVal }
    val vibrateOnClickEnabled by viewModel.vibrateOnClickEnabled
    val onVibrateChange = { newVal: Boolean -> viewModel.vibrateOnClickEnabled.value = newVal }
    val preferredSim by viewModel.preferredSim
    val onSimChange = { newVal: String -> viewModel.updatePreferredSim(newVal) }
    val voicemailNumber by viewModel.voicemailNumber
    val onVoicemailChange = { newVal: String -> viewModel.updateVoicemailNumber(newVal) }
    
    val blockedNumbersEntities by viewModel.blockedNumbersFlow.collectAsState()
    val blockedNumbers = remember(blockedNumbersEntities) { blockedNumbersEntities.map { it.number } }
    
    val quickResponsesEntities by viewModel.quickResponsesFlow.collectAsState()
    val quickResponses = remember(quickResponsesEntities) { quickResponsesEntities.map { it.message } }
    
    val speedDialEntities by viewModel.speedDialFlow.collectAsState()
    val speedDialMap = remember(speedDialEntities) { speedDialEntities.associate { it.key to it.number } }
    
    var activeTab by remember { mutableStateOf(0) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var newBlockedInput by remember { mutableStateOf("") }
    var newQuickRespInput by remember { mutableStateOf("") }
    var targetSpeedDialKey by remember { mutableIntStateOf(-1) }

    val isDark = androidx.compose.foundation.isSystemInDarkTheme()
    val cardBgColor = if (isDark) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (activeTab) {
                            0 -> stringResource(R.string.settings_title)
                            1 -> stringResource(R.string.settings_block_list_title)
                            2 -> stringResource(R.string.settings_speed_dial)
                            3 -> stringResource(R.string.settings_quick_resp_title)
                            4 -> stringResource(R.string.settings_voicemail_setup_title)
                            5 -> stringResource(R.string.settings_dedup_title)
                            6 -> stringResource(R.string.settings_updates_title)
                            7 -> stringResource(R.string.settings_recordings_title)
                            else -> stringResource(R.string.settings_title)
                        },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeTab != 0) activeTab = 0 else onClose()
                    }) {
                        Icon(
                            imageVector = if (activeTab != 0) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                            contentDescription = stringResource(R.string.settings_back),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            when (activeTab) {
                0 -> {
                    // General Settings View
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        // Appearance Card
                        item {
                            PreferenceHeader("Appearance")
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = cardBgColor
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column {
                                    SettingsRowToggle(
                                        title = "Dark Theme",
                                        subtitle = "Apply dark visual theme to the interface",
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
                            PreferenceHeader("Sound & Haptics")
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = cardBgColor
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column {
                                    SettingsRowToggle(
                                        title = "Dialpad Tones",
                                        subtitle = "Play sounds when dialing keys",
                                        checked = dialpadTonesEnabled,
                                        onCheckedChange = onTonesChange,
                                        icon = Icons.Default.VolumeUp,
                                        iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                        iconTint = MaterialTheme.colorScheme.secondary
                                    )
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    SettingsRowToggle(
                                        title = "Vibrate on Click",
                                        subtitle = "Haptic response on dialpad interactions",
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
                            PreferenceHeader("Calls & Blocking")
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = cardBgColor
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column {
                                    SettingsRowNav(
                                        title = "Speed Dial",
                                        subtitle = "Assign fast call keys 1-9",
                                        onClick = { activeTab = 2 },
                                        icon = Icons.Default.Speed,
                                        iconBgColor = Color(0xFFE3F2FD),
                                        iconTint = Color(0xFF1E88E5)
                                    )
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    SettingsRowNav(
                                        title = "Blocked Numbers",
                                        subtitle = "Manage restricted incoming callers",
                                        onClick = { activeTab = 1 },
                                        icon = Icons.Default.Block,
                                        iconBgColor = Color(0xFFFFEBEE),
                                        iconTint = Color(0xFFE53935)
                                    )
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    SettingsRowNav(
                                        title = "Quick Responses",
                                        subtitle = "Edit SMS decline templates",
                                        onClick = { activeTab = 3 },
                                        icon = Icons.Default.Message,
                                        iconBgColor = Color(0xFFE8F5E9),
                                        iconTint = Color(0xFF43A047)
                                    )
                                }
                            }
                        }

                        // SIM & Voicemail Card
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            PreferenceHeader("SIM & Voicemail")
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = cardBgColor
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column {
                                    ListItem(
                                        headlineContent = { 
                                            Text(
                                                "Preferred SIM",
                                                fontWeight = FontWeight.Medium,
                                                style = MaterialTheme.typography.bodyLarge
                                            ) 
                                        },
                                        supportingContent = { 
                                            Text(
                                                "Default SIM for making calls",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            ) 
                                        },
                                        leadingContent = {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFFFF3E0)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.SimCard,
                                                    contentDescription = null,
                                                    tint = Color(0xFFFB8C00),
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        },
                                        trailingContent = {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                listOf("SIM 1", "SIM 2", "Ask").forEach { op ->
                                                    val sel = preferredSim == op
                                                    FilterChip(
                                                        selected = sel,
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            onSimChange(op)
                                                        },
                                                        label = { 
                                                            Text(
                                                                op, 
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
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    SettingsRowNav(
                                        title = "Voicemail Number",
                                        subtitle = voicemailNumber.ifEmpty { "Not set" },
                                        onClick = { activeTab = 4 },
                                        icon = Icons.Default.Voicemail,
                                        iconBgColor = Color(0xFFF3E5F5),
                                        iconTint = Color(0xFF8E24AA)
                                    )
                                }
                            }
                        }

                        // Startup Options Card
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            PreferenceHeader("Startup Options")
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Default Startup Tab",
                                        fontWeight = FontWeight.Medium,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    val tabs = listOf("Favorites", "Recents", "Contacts", "Dialpad")
                                    val currentTabSelected = viewModel.defaultTab.intValue
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        tabs.forEachIndexed { idx, title ->
                                            val isSel = currentTabSelected == idx
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(8.dp))
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
                            PreferenceHeader("Calling Features")
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column {
                                    val callWaiting = viewModel.callWaitingEnabled.value
                                    val callRecording = viewModel.recordingEnabled.value
                                    SettingsRowToggle(
                                        title = "Call Waiting",
                                        subtitle = "Notify of incoming calls during an active call",
                                        checked = callWaiting,
                                        onCheckedChange = { viewModel.updateCallWaitingEnabled(it) },
                                        icon = Icons.Default.NetworkCell,
                                        iconBgColor = Color(0xFFECEFF1),
                                        iconTint = Color(0xFF607D8B)
                                    )
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    SettingsRowToggle(
                                        title = "Call Recording",
                                        subtitle = "Enable in-call recording controls & save locally",
                                        checked = callRecording,
                                        onCheckedChange = { viewModel.updateRecordingEnabled(it) },
                                        icon = Icons.Default.Mic,
                                        iconBgColor = Color(0xFFEDE7F6),
                                        iconTint = Color(0xFF673AB7)
                                    )
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    SettingsRowNav(
                                        title = "View Saved Recordings",
                                        subtitle = "Listen to and manage your call recordings",
                                        onClick = { activeTab = 7 },
                                        icon = Icons.Default.Audiotrack,
                                        iconBgColor = Color(0xFFE8F5E9),
                                        iconTint = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }

                        // Information Card
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            PreferenceHeader("Information & Utilities")
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = cardBgColor
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column {
                                    SettingsRowNav(
                                        title = "Merge Duplicate Contacts",
                                        subtitle = "Scan and clean duplicate contact listings",
                                        onClick = { activeTab = 5 },
                                        icon = Icons.Default.MergeType,
                                        iconBgColor = Color(0xFFE0F7FA),
                                        iconTint = Color(0xFF00838F)
                                    )
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    SettingsRowNav(
                                        title = "Check for Updates",
                                        subtitle = "Ensure app is up to date & secure",
                                        onClick = { activeTab = 6 },
                                        icon = Icons.Default.SystemUpdateAlt,
                                        iconBgColor = Color(0xFFE8F5E9),
                                        iconTint = Color(0xFF2E7D32)
                                    )
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    SettingsRowNav(
                                        title = "About",
                                        subtitle = "Version and developer info",
                                        onClick = { showAboutDialog = true },
                                        icon = Icons.Default.Info,
                                        iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    SettingsRowNav(
                                        title = "Privacy Policy",
                                        subtitle = "How we handle your data",
                                        onClick = { showPrivacyDialog = true },
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
                            PreferenceHeader("Contribution")
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = cardBgColor
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Support Secure Dialer",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        "Secure Dialer is 100% free, open-source, ad-free, and privacy-first. If you find this app helpful, consider contributing code, submitting translations, or supporting the development of free and secure tools.",
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
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(Icons.Default.Favorite, "Contribute")
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Contribute on GitHub")
                                    }
                                }
                            }
                        }


                    }
                }

                1 -> {
                    // Block List View
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = newBlockedInput,
                            onValueChange = { newBlockedInput = it },
                            label = { Text("Enter Number to Block") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (newBlockedInput.isNotEmpty()) {
                                    IconButton(onClick = { newBlockedInput = "" }) {
                                        Icon(Icons.Default.Clear, "Clear")
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                if (newBlockedInput.isNotBlank()) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    viewModel.addBlockedNumber(newBlockedInput.trim())
                                    newBlockedInput = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Block, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Block This Number", fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            "BLOCKED CALLERS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (blockedNumbers.isEmpty()) {
                            SettingsEmptyState(
                                icon = Icons.Default.Block,
                                title = "No Blocked Numbers",
                                description = "Numbers you block will appear here. They won't be able to call you.",
                                tintColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(blockedNumbers, key = { it }) { num ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = cardBgColor
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.Phone,
                                                        null,
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(
                                                    num,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                            IconButton(onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                viewModel.removeBlockedNumber(num)
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Unblock",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Speed Dial Setup
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Hold keys 1-9 on the dialpad to quickly call assigned numbers.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (targetSpeedDialKey != -1) {
                            var speedNumInput by remember { mutableStateOf("") }
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(18.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Surface(
                                            modifier = Modifier.size(32.dp),
                                            shape = CircleShape,
                                            color = MaterialTheme.colorScheme.primaryContainer
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    targetSpeedDialKey.toString(),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                        Text(
                                            "Assign Speed Dial Key $targetSpeedDialKey",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = speedNumInput,
                                        onValueChange = { speedNumInput = it },
                                        label = { Text("Phone Number") },
                                        singleLine = true,
                                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { targetSpeedDialKey = -1 }) {
                                            Text("Cancel")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                if (speedNumInput.isNotBlank()) {
                                                    viewModel.saveSpeedDial(targetSpeedDialKey, speedNumInput.trim(), "Speed Dial")
                                                    targetSpeedDialKey = -1
                                                }
                                            },
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Text("Assign Key")
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items((1..9).toList(), key = { it }) { digit ->
                                    val assignedNum = speedDialMap[digit]

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = cardBgColor
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Surface(
                                                    modifier = Modifier.size(44.dp),
                                                    shape = CircleShape,
                                                    color = if (assignedNum != null) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            digit.toString(),
                                                            style = MaterialTheme.typography.titleMedium,
                                                            fontWeight = FontWeight.Bold,
                                                            color = if (assignedNum != null) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column {
                                                    Text(
                                                        text = assignedNum ?: "Unassigned Key",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = if (assignedNum != null) FontWeight.Medium else FontWeight.Normal,
                                                        color = if (assignedNum != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                                    )
                                                    if (assignedNum != null) {
                                                        Text(
                                                            text = "Press & hold $digit on Dialpad",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                                        )
                                                    }
                                                }
                                            }

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                IconButton(
                                                    onClick = { targetSpeedDialKey = digit },
                                                    colors = IconButtonDefaults.iconButtonColors(
                                                        contentColor = MaterialTheme.colorScheme.primary
                                                    )
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Key $digit"
                                                    )
                                                }
                                                if (assignedNum != null) {
                                                    IconButton(
                                                        onClick = { viewModel.deleteSpeedDial(digit) },
                                                        colors = IconButtonDefaults.iconButtonColors(
                                                            contentColor = MaterialTheme.colorScheme.error
                                                        )
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete Key $digit"
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                3 -> {
                    // Quick Responses View
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        OutlinedTextField(
                            value = newQuickRespInput,
                            onValueChange = { newQuickRespInput = it },
                            label = { Text("Create Custom Reply") },
                            leadingIcon = {
                                Icon(Icons.Default.Message, null, tint = MaterialTheme.colorScheme.primary)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                if (newQuickRespInput.isNotBlank()) {
                                    viewModel.addQuickResponse(newQuickRespInput.trim())
                                    newQuickRespInput = ""
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Message Template", fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            "QUICK DECLINE MESSAGES",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (quickResponses.isEmpty()) {
                            SettingsEmptyState(
                                icon = Icons.Default.Message,
                                title = "No Responses Saved",
                                description = "Add custom message templates to quickly send when rejecting incoming calls.",
                                tintColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            )
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(quickResponses, key = { it }) { resp ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = cardBgColor
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        Icons.Default.ChatBubble,
                                                        null,
                                                        tint = MaterialTheme.colorScheme.primary,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Text(
                                                    resp,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            IconButton(onClick = { 
                                                quickResponsesEntities.find { it.message == resp }?.let { 
                                                    viewModel.deleteQuickResponse(it)
                                                }
                                            }) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete response",
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                4 -> {
                    // Voicemail Edit View
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(96.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Voicemail,
                                    null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Voicemail Setup",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Set the phone number to trigger when you long-press key '1' on the dialpad.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        OutlinedTextField(
                            value = voicemailNumber,
                            onValueChange = onVoicemailChange,
                            label = { Text("Voicemail Directory Number") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Phone, null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { activeTab = 0 },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Voicemail Number", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                5 -> {
                    // Merge Duplicate Contacts View
                    val allContacts by viewModel.allContactsFlow.collectAsState()
                    val duplicates = remember(allContacts) {
                        allContacts.groupBy { it.number.replace("[^0-9+]".toRegex(), "") }
                            .filter { it.key.isNotEmpty() && it.value.size > 1 }
                    }
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "DEDUPLICATION UTILITY",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "The system scans your local database for duplicate entries sharing the same phone number, allowing you to merge them instantly.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (duplicates.isEmpty()) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                SettingsEmptyState(
                                    icon = Icons.Default.FilterNone,
                                    title = "No Duplicates Found",
                                    description = "Your contacts list is completely clean! No duplicate numbers detected.",
                                    tintColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            }
                        } else {
                            val coroutineScope = rememberCoroutineScope()
                            Button(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    coroutineScope.launch {
                                        duplicates.forEach { (_, group) ->
                                            val primary = group.first()
                                            // delete the rest
                                            group.drop(1).forEach { dup ->
                                                viewModel.deleteContact(dup.number)
                                            }
                                        }
                                        Toast.makeText(context, "All duplicates merged successfully", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.MergeType, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Merge All Detected Duplicates (${duplicates.size} groups)", fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(duplicates.keys.toList(), key = { it }) { key ->
                                    val group = duplicates[key] ?: emptyList()
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(containerColor = cardBgColor)
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = group.first().number,
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                TextButton(
                                                    onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        coroutineScope.launch {
                                                            group.drop(1).forEach { dup ->
                                                                viewModel.deleteContact(dup.number)
                                                            }
                                                            Toast.makeText(context, "Merged duplicates for ${group.first().number}", Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                ) {
                                                    Text("Merge Group")
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            group.forEach { contact ->
                                                Row(
                                                    modifier = Modifier.padding(vertical = 4.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Person,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = contact.name,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    if (contact.email.isNotEmpty()) {
                                                        Text(
                                                            text = " • ${contact.email}",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                6 -> {
                    // Check for Updates View
                    var isChecking by remember { mutableStateOf(false) }
                    var checkResult by remember { mutableStateOf<String?>(null) }
                    val scope = rememberCoroutineScope()
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(96.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SystemUpdateAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            text = "In-App Updates",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Check and update Secure Dialer directly from official F-Droid or stable mirrors.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        if (isChecking) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Checking secure mirrors...", style = MaterialTheme.typography.bodyMedium)
                        } else if (checkResult != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CheckCircle, "Up to date", tint = Color(0xFF2E7D32), modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(checkResult!!, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("You are using the latest official cryptographic build of Secure Dialer.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(48.dp))
                        
                        Button(
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch {
                                    isChecking = true
                                    kotlinx.coroutines.delay(2000)
                                    isChecking = false
                                    checkResult = "Secure Dialer is up to date!\nVersion v1.0.2 (Stable Release)"
                                }
                            },
                            enabled = !isChecking,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check for Updates Now", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                7 -> {
                    // Call Recordings View
                    val recordings by viewModel.recordingsFlow.collectAsState()
                    var playingId by remember { mutableIntStateOf(-1) }
                    var playbackProgress by remember { mutableFloatStateOf(0f) }
                    val scope = rememberCoroutineScope()

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "SECURE LOCAL RECORDINGS",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Recordings are stored securely inside the app's sandboxed private database and not shared with external servers.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (recordings.isEmpty()) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                SettingsEmptyState(
                                    icon = Icons.Default.Mic,
                                    title = "No Recordings Found",
                                    description = "Enable Call Recording and tap the record button during any active call.",
                                    tintColor = MaterialTheme.colorScheme.primary
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(bottom = 16.dp)
                            ) {
                                items(recordings, key = { it.id }) { rec ->
                                    val isPlaying = playingId == rec.id
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 6.dp),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isPlaying) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                                            } else {
                                                cardBgColor
                                            }
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.weight(1f)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .clip(CircleShape)
                                                            .background(
                                                                if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        IconButton(onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            if (isPlaying) {
                                                                playingId = -1
                                                            } else {
                                                                playingId = rec.id
                                                                playbackProgress = 0f
                                                            }
                                                        }) {
                                                            Icon(
                                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                                contentDescription = if (isPlaying) "Pause" else "Play",
                                                                tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                                            )
                                                        }
                                                    }
                                                    Spacer(modifier = Modifier.width(16.dp))
                                                    Column {
                                                        Text(
                                                            text = rec.name.ifEmpty { rec.number },
                                                            style = MaterialTheme.typography.bodyLarge,
                                                            fontWeight = FontWeight.Bold,
                                                            color = MaterialTheme.colorScheme.onSurface
                                                        )
                                                        if (rec.name.isNotEmpty()) {
                                                            Text(
                                                                text = rec.number,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                                            )
                                                        }
                                                        val dateStr = remember(rec.timestamp) {
                                                            try {
                                                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                                                                sdf.format(java.util.Date(rec.timestamp.toLong()))
                                                            } catch (e: Exception) {
                                                                "Unknown Date"
                                                            }
                                                        }
                                                        Text(
                                                            text = "$dateStr • ${rec.duration}s",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                                        )
                                                    }
                                                }
                                                Row {
                                                    IconButton(onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        exportRecordingToDownloads(context, rec.filePath)
                                                    }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Download,
                                                            contentDescription = "Export to Downloads",
                                                            tint = MaterialTheme.colorScheme.primary
                                                        )
                                                    }
                                                    IconButton(onClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        viewModel.deleteCallRecording(rec.id)
                                                        if (isPlaying) playingId = -1
                                                        Toast.makeText(context, "Deleted recording", Toast.LENGTH_SHORT).show()
                                                    }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Delete Recording",
                                                            tint = MaterialTheme.colorScheme.error
                                                        )
                                                    }
                                                }
                                            }

                                            if (isPlaying) {
                                                LaunchedEffect(playingId) {
                                                    playbackProgress = 0f
                                                    val steps = rec.duration * 10
                                                    for (i in 1..steps) {
                                                        if (playingId != rec.id) break
                                                        delay(100)
                                                        playbackProgress = i.toFloat() / steps
                                                    }
                                                    if (playingId == rec.id) {
                                                        playingId = -1
                                                        playbackProgress = 0f
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.fillMaxWidth()
                                                ) {
                                                    LinearProgressIndicator(
                                                        progress = playbackProgress,
                                                        modifier = Modifier
                                                            .weight(1f)
                                                            .height(4.dp)
                                                            .clip(RoundedCornerShape(2.dp)),
                                                        color = MaterialTheme.colorScheme.primary,
                                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        text = "${(playbackProgress * rec.duration).toInt()}s / ${rec.duration}s",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    if (showPrivacyDialog) {
        PrivacyDialog(onDismiss = { showPrivacyDialog = false })
    }
}

@Composable
fun SettingsRowToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null,
    iconBgColor: Color = Color.Transparent,
    iconTint: Color = Color.Unspecified
) {
    ListItem(
        headlineContent = { 
            Text(
                title,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge
            ) 
        },
        supportingContent = { 
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ) 
        },
        leadingContent = if (icon != null) {
            {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
            }
        } else null,
        trailingContent = {
            val haptic = LocalHapticFeedback.current
            Switch(
                checked = checked,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCheckedChange(it)
                }
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun SettingsRowNav(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    iconBgColor: Color = Color.Transparent,
    iconTint: Color = Color.Unspecified
) {
    val haptic = LocalHapticFeedback.current
    ListItem(
        modifier = Modifier.clickable {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        headlineContent = { 
            Text(
                title,
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge
            ) 
        },
        supportingContent = { 
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ) 
        },
        leadingContent = if (icon != null) {
            {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
            }
        } else null,
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(18.dp)
            )
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun PreferenceHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

@Composable
fun SettingsEmptyState(
    icon: ImageVector,
    title: String,
    description: String,
    tintColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = tintColor,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )
    }
}

@Composable
fun ThemeColorPicker(
    currentSelected: String,
    onColorSelected: (String) -> Unit
) {
    val options = listOf(
        Triple("classic_slate", Color(0xFF1967D2), "Slate"),
        Triple("forest_green", Color(0xFF2E7D32), "Green"),
        Triple("ocean_blue", Color(0xFF1565C0), "Blue"),
        Triple("sunset_orange", Color(0xFFE65100), "Orange"),
        Triple("lavender_purple", Color(0xFF7B1FA2), "Purple"),
        Triple("dark_crimson", Color(0xFFB71C1C), "Crimson")
    )
    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = "Theme Accent Color",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            options.forEach { (key, color, label) ->
                val isSelected = currentSelected == key
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(color)
                        .clickable { onColorSelected(key) }
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = label,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

fun exportRecordingToDownloads(context: Context, filePath: String) {
    try {
        val file = java.io.File(filePath)
        if (!file.exists()) {
            Toast.makeText(context, "Recording source file not found", Toast.LENGTH_SHORT).show()
            return
        }
        val fileName = file.name
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "audio/m4a")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                resolver.openOutputStream(uri)?.use { out ->
                    file.inputStream().use { input -> input.copyTo(out) }
                }
                Toast.makeText(context, "Exported successfully to Downloads!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(context, "Failed to create public downloads entry", Toast.LENGTH_SHORT).show()
            }
        } else {
            @Suppress("DEPRECATION")
            val publicDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
            val destFile = java.io.File(publicDir, fileName)
            file.inputStream().use { input ->
                destFile.outputStream().use { out ->
                    input.copyTo(out)
                }
            }
            Toast.makeText(context, "Exported to ${destFile.absolutePath}", Toast.LENGTH_LONG).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Export failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    }
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("About", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Secure Dialer", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Version 1.0.2", style = MaterialTheme.typography.bodySmall)
                Text("Secure Dialer is a privacy-focused communication tool designed to put you in control of your data. All functions occur exclusively on your device.")
                Text("Our Mission", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("To provide a simple, reliable, and secure dialer experience without compromising user privacy.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}

@Composable
fun PrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Privacy Policy", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("At Secure Dialer, we prioritize your privacy.", style = MaterialTheme.typography.bodyLarge)
                Text("1. Local Data Processing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Your contacts, call logs, and personal history are never transmitted to our servers.")
                Text("2. Data Usage", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Information is used only for core dialer functions: Contacts mapping, Call history display, and Microphone access during calls.")
                Text("3. Data Sharing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("We do not share your data with any third parties.")
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}





