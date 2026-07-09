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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPanel(
    viewModel: DialerViewModel,
    onClose: () -> Unit
) {
    val context = LocalContext.current
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
    val onSimChange = { newVal: String -> viewModel.preferredSim.value = newVal }
    val voicemailNumber by viewModel.voicemailNumber
    val onVoicemailChange = { newVal: String -> viewModel.voicemailNumber.value = newVal }
    val blockedNumbers = viewModel.blockedNumbers
    val quickResponses = viewModel.quickResponses
    val speedDialMap = viewModel.speedDialMap
    var activeTab by remember { mutableStateOf(0) } // 0: Settings, 1: Block List, 2: Speed Dial, 3: Quick Responses
    var newBlockedInput by remember { mutableStateOf("") }
    var newQuickRespInput by remember { mutableStateOf("") }
    var targetSpeedDialKey by remember { mutableIntStateOf(-1) }

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
                            else -> "Settings"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (activeTab != 0) activeTab = 0 else onClose()
                    }) {
                        Icon(
                            imageVector = if (activeTab != 0) Icons.AutoMirrored.Filled.ArrowBack else Icons.Default.Close,
                            contentDescription = "Back"
                        )
                    }
                }
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
                    // General Settings Tab
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        item {
                            PreferenceHeader("Appearance")
                            SettingsRowToggle(
                                title = "Dark Theme",
                                subtitle = "Apply a dark visual theme to the app",
                                checked = isDarkTheme,
                                onCheckedChange = onThemeChange,
                                icon = Icons.Default.DarkMode
                            )
                        }

                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                            PreferenceHeader("Sound & Haptics")
                            SettingsRowToggle(
                                title = "Dialpad Tones",
                                subtitle = "Play sounds when using the dialpad",
                                checked = dialpadTonesEnabled,
                                onCheckedChange = onTonesChange,
                                icon = Icons.Default.VolumeUp
                            )
                            SettingsRowToggle(
                                title = "Vibration",
                                subtitle = "Vibrate on dialpad key presses",
                                checked = vibrateOnClickEnabled,
                                onCheckedChange = onVibrateChange,
                                icon = Icons.Default.Vibration
                            )
                        }

                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                            PreferenceHeader("Calls & Blocking")
                            SettingsRowNav(
                                title = "Speed Dial",
                                subtitle = "Configure speed dial keys",
                                onClick = { activeTab = 2 },
                                icon = Icons.Default.Speed
                            )
                            SettingsRowNav(
                                title = "Blocked Numbers",
                                subtitle = "Manage blocked phone numbers",
                                onClick = { activeTab = 1 },
                                icon = Icons.Default.Block
                            )
                            SettingsRowNav(
                                title = "Quick Responses",
                                subtitle = "Edit SMS decline templates",
                                onClick = { activeTab = 3 },
                                icon = Icons.Default.Message
                            )
                        }

                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)
                            PreferenceHeader("SIM & Voicemail")
                            ListItem(
                                headlineContent = { Text("Preferred SIM") },
                                supportingContent = { Text("Choose default SIM for calls") },
                                leadingContent = { Icon(Icons.Default.SimCard, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                                trailingContent = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        listOf("SIM 1", "SIM 2", "Ask").forEach { op ->
                                            val sel = preferredSim == op
                                            FilterChip(
                                                selected = sel,
                                                onClick = { onSimChange(op) },
                                                label = { Text(op) }
                                            )
                                        }
                                    }
                                }
                            )
                            SettingsRowNav(
                                title = "Voicemail Number",
                                subtitle = voicemailNumber.ifEmpty { "Not set" },
                                onClick = { activeTab = 4 },
                                icon = Icons.Default.Voicemail
                            )
                        }

                        item {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            PreferenceHeader("Information")
                            SettingsRowNav(
                                title = "About",
                                subtitle = "Version and developer info",
                                onClick = {
                                    context.startActivity(Intent(context, com.example.ui.AboutActivity::class.java))
                                }
                            )
                            SettingsRowNav(
                                title = "Privacy Policy",
                                subtitle = "How we handle your data",
                                onClick = {
                                    context.startActivity(Intent(context, com.example.ui.PrivacyPolicyActivity::class.java))
                                }
                            )
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newBlockedInput,
                                onValueChange = { newBlockedInput = it },
                                label = { Text("Block New Number") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (newBlockedInput.isNotBlank()) {
                                        blockedNumbers.add(newBlockedInput.trim())
                                        newBlockedInput = ""
                                    }
                                }
                            ) {
                                Text("Block")
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            "Blocked Numbers",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(blockedNumbers, key = { it }) { num ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            num,
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        IconButton(onClick = { blockedNumbers.remove(num) }) {
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

                2 -> {
                    // Speed Dial Setup
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Text(
                            "Assign keys 1-9 to instantly call specified contacts",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (targetSpeedDialKey != -1) {
                            var speedNumInput by remember { mutableStateOf("") }
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Assign Key $targetSpeedDialKey",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = speedNumInput,
                                        onValueChange = { speedNumInput = it },
                                        label = { Text("Phone Number") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
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
                                                    speedDialMap[targetSpeedDialKey] = speedNumInput.trim()
                                                    targetSpeedDialKey = -1
                                                }
                                            }
                                        ) {
                                            Text("Assign")
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items((1..9).toList(), key = { it }) { digit ->
                                    val assignedNum = speedDialMap[digit]

                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(
                                                    modifier = Modifier.size(40.dp),
                                                    shape = CircleShape,
                                                    color = MaterialTheme.colorScheme.primaryContainer
                                                ) {
                                                    Box(contentAlignment = Alignment.Center) {
                                                        Text(
                                                            digit.toString(),
                                                            style = MaterialTheme.typography.titleMedium,
                                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.width(16.dp))
                                                Column {
                                                    Text(
                                                        text = assignedNum ?: "Unassigned",
                                                        style = MaterialTheme.typography.bodyLarge,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                    if (assignedNum != null) {
                                                        Text(
                                                            text = "Long press $digit on Dialer to call",
                                                            style = MaterialTheme.typography.bodySmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                                        )
                                                    }
                                                }
                                            }

                                            Row {
                                                IconButton(onClick = { targetSpeedDialKey = digit }) {
                                                    Icon(
                                                        imageVector = Icons.Default.Settings,
                                                        contentDescription = "Edit",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                if (assignedNum != null) {
                                                    IconButton(onClick = { speedDialMap.remove(digit) }) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = "Clear",
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
                    }
                }

                3 -> {
                    // Quick Responses View
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newQuickRespInput,
                                onValueChange = { newQuickRespInput = it },
                                label = { Text("New Response") },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Button(
                                onClick = {
                                    if (newQuickRespInput.isNotBlank()) {
                                        quickResponses.add(newQuickRespInput.trim())
                                        newQuickRespInput = ""
                                    }
                                }
                            ) {
                                Text("Add")
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        Text(
                            "SMS Templates",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(quickResponses, key = { it }) { resp ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            resp,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(onClick = { quickResponses.remove(resp) }) {
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

                4 -> {
                    // Voicemail Edit View
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            modifier = Modifier.size(80.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.Voicemail,
                                    null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Voicemail Setup",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Enter the number to dial when you long-press '1' on the dialpad.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        OutlinedTextField(
                            value = voicemailNumber,
                            onValueChange = onVoicemailChange,
                            label = { Text("Voicemail Number") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Button(
                            onClick = { activeTab = 0 },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Save & Back")
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
    icon: ImageVector? = null
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = if (icon != null) {
            { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else null,
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
fun SettingsRowNav(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = if (icon != null) {
            { Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else null,
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
fun PreferenceHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
