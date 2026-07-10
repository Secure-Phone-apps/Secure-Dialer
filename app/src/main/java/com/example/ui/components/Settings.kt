package com.example.ui.components

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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

import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

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
                            0 -> "Settings"
                            1 -> "Block List"
                            2 -> "Speed Dial"
                            3 -> "Quick Responses"
                            4 -> "Voicemail Setup"
                            else -> "Settings"
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
                            contentDescription = "Back",
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

                        // Information Card
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            PreferenceHeader("Information")
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
                                        title = "About",
                                        subtitle = "Version and developer info",
                                        onClick = {
                                            context.startActivity(Intent(context, com.example.ui.AboutActivity::class.java))
                                        },
                                        icon = Icons.Default.Info,
                                        iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                                    SettingsRowNav(
                                        title = "Privacy Policy",
                                        subtitle = "How we handle your data",
                                        onClick = {
                                            context.startActivity(Intent(context, com.example.ui.PrivacyPolicyActivity::class.java))
                                        },
                                        icon = Icons.Default.Security,
                                        iconBgColor = MaterialTheme.colorScheme.surfaceVariant,
                                        iconTint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
            }
        }
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

