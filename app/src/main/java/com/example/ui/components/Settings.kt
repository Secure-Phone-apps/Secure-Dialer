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
import com.example.ui.theme.LocalM3Expressive
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.draw.scale

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
        viewModel.updateDarkTheme(newVal)
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

    val isExpressive = LocalM3Expressive.current
    val cardBgColor = if (isExpressive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
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
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
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
                                        onClick = { activeTab = 2 },
                                        icon = Icons.Default.Speed,
                                        iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                                        iconTint = MaterialTheme.colorScheme.primary
                                    )
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    SettingsRowNav(
                                        title = stringResource(R.string.settings_blocked_numbers),
                                        subtitle = stringResource(R.string.settings_blocked_numbers_sub),
                                        onClick = { activeTab = 1 },
                                        icon = Icons.Default.Block,
                                        iconBgColor = MaterialTheme.colorScheme.errorContainer,
                                        iconTint = MaterialTheme.colorScheme.error
                                    )
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    SettingsRowNav(
                                        title = stringResource(R.string.settings_quick_responses),
                                        subtitle = stringResource(R.string.settings_quick_responses_sub),
                                        onClick = { activeTab = 3 },
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
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    SettingsRowNav(
                                        title = stringResource(R.string.settings_voicemail_num),
                                        subtitle = voicemailNumber.ifEmpty { stringResource(R.string.not_set) },
                                        onClick = { activeTab = 4 },
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
                                        onClick = { activeTab = 7 },
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
                                        title = stringResource(R.string.settings_merge_duplicate_contacts),
                                        subtitle = stringResource(R.string.settings_merge_duplicate_contacts_sub),
                                        onClick = { activeTab = 5 },
                                        icon = Icons.Default.MergeType,
                                        iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                                        iconTint = MaterialTheme.colorScheme.secondary
                                    )
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    SettingsRowNav(
                                        title = stringResource(R.string.settings_check_updates),
                                        subtitle = stringResource(R.string.settings_check_updates_sub),
                                        onClick = { activeTab = 6 },
                                        icon = Icons.Default.SystemUpdateAlt,
                                        iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                                        iconTint = MaterialTheme.colorScheme.primary
                                    )
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    SettingsRowNav(
                                        title = stringResource(R.string.settings_about),
                                        subtitle = stringResource(R.string.settings_about_sub),
                                        onClick = { showAboutDialog = true },
                                        icon = Icons.Default.Info,
                                        iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    SettingsRowNav(
                                        title = stringResource(R.string.settings_privacy),
                                        subtitle = stringResource(R.string.settings_privacy_sub),
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
                            label = { Text(stringResource(R.string.enter_number_to_block)) },
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
                            shape = MaterialTheme.shapes.small
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
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.Block, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.block_this_number), fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            stringResource(R.string.blocked_callers_header),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (blockedNumbers.isEmpty()) {
                            SettingsEmptyState(
                                icon = Icons.Default.Block,
                                title = stringResource(R.string.no_blocked_numbers_title),
                                description = stringResource(R.string.no_blocked_numbers_desc),
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
                                        shape = MaterialTheme.shapes.medium,
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
                                                        .clip(if (LocalM3Expressive.current) MaterialTheme.shapes.medium else CircleShape)
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
                                                    contentDescription = stringResource(R.string.unblock),
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
                            stringResource(R.string.speed_dial_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (targetSpeedDialKey != -1) {
                            var speedNumInput by remember { mutableStateOf("") }
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium,
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
                                            shape = if (LocalM3Expressive.current) MaterialTheme.shapes.medium else CircleShape,
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
                                            stringResource(R.string.assign_speed_dial_key, targetSpeedDialKey),
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutlinedTextField(
                                        value = speedNumInput,
                                        onValueChange = { speedNumInput = it },
                                        label = { Text(stringResource(R.string.enter_number_to_block)) },
                                        singleLine = true,
                                        leadingIcon = { Icon(Icons.Default.Phone, null) },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.small
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End
                                    ) {
                                        TextButton(onClick = { targetSpeedDialKey = -1 }) {
                                            Text(stringResource(R.string.btn_cancel))
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = {
                                                if (speedNumInput.isNotBlank()) {
                                                    viewModel.saveSpeedDial(targetSpeedDialKey, speedNumInput.trim(), "Speed Dial")
                                                    targetSpeedDialKey = -1
                                                }
                                            },
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(stringResource(R.string.assign_key))
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
                                        shape = MaterialTheme.shapes.medium,
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
                                                    shape = if (LocalM3Expressive.current) MaterialTheme.shapes.medium else CircleShape,
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
                                                        text = assignedNum ?: stringResource(R.string.unassigned_key),
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        fontWeight = if (assignedNum != null) FontWeight.Medium else FontWeight.Normal,
                                                        color = if (assignedNum != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
                                                    )
                                                    if (assignedNum != null) {
                                                        Text(
                                                            text = stringResource(R.string.press_and_hold_dialpad, digit),
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
                                                        contentDescription = stringResource(R.string.edit_key, digit)
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
                                                            contentDescription = stringResource(R.string.delete_key, digit)
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
                            label = { Text(stringResource(R.string.create_custom_reply)) },
                            leadingIcon = {
                                Icon(Icons.Default.Message, null, tint = MaterialTheme.colorScheme.primary)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small
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
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.add_message_template), fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            stringResource(R.string.quick_decline_messages),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        if (quickResponses.isEmpty()) {
                            SettingsEmptyState(
                                icon = Icons.Default.Message,
                                title = stringResource(R.string.no_responses_saved_title),
                                description = stringResource(R.string.no_responses_saved_desc),
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
                                        shape = MaterialTheme.shapes.medium,
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
                                                        .clip(if (LocalM3Expressive.current) MaterialTheme.shapes.medium else CircleShape)
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
                                                    contentDescription = stringResource(R.string.delete_response),
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
                            shape = if (LocalM3Expressive.current) MaterialTheme.shapes.large else CircleShape,
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
                            stringResource(R.string.settings_voicemail_setup_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            stringResource(R.string.voicemail_setup_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        OutlinedTextField(
                            value = voicemailNumber,
                            onValueChange = onVoicemailChange,
                            label = { Text(stringResource(R.string.voicemail_directory_number)) },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Default.Phone, null) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.small
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { activeTab = 0 },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.Check, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.save_voicemail_number), fontWeight = FontWeight.SemiBold)
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
                            text = stringResource(R.string.settings_dedup_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.dedup_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (duplicates.isEmpty()) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                SettingsEmptyState(
                                    icon = Icons.Default.FilterNone,
                                    title = stringResource(R.string.no_duplicates_title),
                                    description = stringResource(R.string.no_duplicates_desc),
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
                                shape = MaterialTheme.shapes.small
                            ) {
                                Icon(Icons.Default.MergeType, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.merge_all_duplicates, duplicates.size), fontWeight = FontWeight.SemiBold)
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
                                        shape = MaterialTheme.shapes.medium,
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
                                                    Text(stringResource(R.string.merge_group))
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
                    val upToDateMsg = stringResource(R.string.up_to_date_result)
                    
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(96.dp),
                            shape = if (LocalM3Expressive.current) MaterialTheme.shapes.large else CircleShape,
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
                            text = stringResource(R.string.settings_updates_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = stringResource(R.string.check_updates_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        if (isChecking) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(stringResource(R.string.checking_mirrors), style = MaterialTheme.typography.bodyMedium)
                        } else if (checkResult != null) {
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                shape = MaterialTheme.shapes.medium,
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(checkResult!!, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(stringResource(R.string.app_up_to_date_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
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
                                    checkResult = upToDateMsg
                                }
                            },
                            enabled = !isChecking,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = MaterialTheme.shapes.small
                        ) {
                            Icon(Icons.Default.Refresh, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.check_for_updates_now), fontWeight = FontWeight.SemiBold)
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
                            text = stringResource(R.string.secure_local_recordings),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.recordings_privacy_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (recordings.isEmpty()) {
                            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                SettingsEmptyState(
                                    icon = Icons.Default.Mic,
                                    title = stringResource(R.string.no_recordings_title),
                                    description = stringResource(R.string.no_recordings_desc),
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
                                        shape = MaterialTheme.shapes.medium,
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
                                                            .clip(if (LocalM3Expressive.current) MaterialTheme.shapes.medium else CircleShape)
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
                                                            contentDescription = stringResource(R.string.delete_key, rec.id),
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
                                                            .clip(MaterialTheme.shapes.extraSmall),
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
                        .clip(MaterialTheme.shapes.small)
                        .background(iconBgColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
            }
        } else null,
        trailingContent = {
            val haptic = LocalHapticFeedback.current
            val isExpressive = LocalM3Expressive.current
            Switch(
                checked = checked,
                onCheckedChange = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCheckedChange(it)
                },
                colors = if (isExpressive) {
                    SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                        uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                } else SwitchDefaults.colors()
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
                        .clip(MaterialTheme.shapes.small)
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
            shape = if (LocalM3Expressive.current) MaterialTheme.shapes.large else CircleShape,
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
        Triple("classic_slate", Color(0xFF2563EB), "Royal Blue"),
        Triple("forest_green", Color(0xFF059669), "Emerald"),
        Triple("ocean_blue", Color(0xFF0284C7), "Cyan"),
        Triple("sunset_orange", Color(0xFFD97706), "Amber"),
        Triple("lavender_purple", Color(0xFF7C3AED), "Purple"),
        Triple("dark_crimson", Color(0xFFE11D48), "Ruby"),
        Triple("natural_gray", Color(0xFF525252), "Titanium")
    )
    val isExpressive = LocalM3Expressive.current
    val pickerShape = if (isExpressive) RoundedCornerShape(12.dp) else CircleShape

    Column(modifier = Modifier.padding(16.dp)) {
        Text(
            text = stringResource(R.string.settings_accent_color),
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
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.15f else 1.0f,
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    ),
                    label = "color_picker_scale"
                )

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .scale(scale)
                        .clip(pickerShape)
                        .background(color)
                        .clickable { onColorSelected(key) }
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                            shape = pickerShape
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
    val dialogShape = if (LocalM3Expressive.current) RoundedCornerShape(32.dp) else MaterialTheme.shapes.extraLarge
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = dialogShape,
        title = { Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text("Version 1.0.2", style = MaterialTheme.typography.bodySmall)
                Text(stringResource(R.string.about_app_desc))
                Text(stringResource(R.string.about_mission_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.about_mission_desc))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}

@Composable
fun PrivacyDialog(onDismiss: () -> Unit) {
    val dialogShape = if (LocalM3Expressive.current) RoundedCornerShape(32.dp) else MaterialTheme.shapes.extraLarge
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = dialogShape,
        title = { Text(stringResource(R.string.privacy_policy_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(stringResource(R.string.privacy_intro), style = MaterialTheme.typography.bodyLarge)
                Text(stringResource(R.string.privacy_section_1_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.privacy_section_1_desc))
                Text(stringResource(R.string.privacy_section_2_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.privacy_section_2_desc))
                Text(stringResource(R.string.privacy_section_3_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.privacy_section_3_desc))
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
    )
}





