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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.LightBlue
import com.example.ui.viewmodel.DialerViewModel

@Composable
fun SettingsPanel(
    viewModel: DialerViewModel,
    onClose: () -> Unit,
    primaryText: Color,
    secondaryText: Color,
    navBg: Color,
    searchBg: Color,
    brandBlue: Color,
    activePill: Color
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(navBg)
                    .statusBarsPadding()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(activePill)
                        .clickable {
                            if (activeTab != 0) {
                                activeTab = 0
                            } else {
                                onClose()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text("←", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = if (isDarkTheme) Color.Black else Color.White)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = when (activeTab) {
                        0 -> "Settings"
                        1 -> "Block List Settings"
                        2 -> "Speed Dial Mapping"
                        3 -> "Quick SMS Responses"
                        else -> "Settings"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryText
                )
            }
        },
        containerColor = navBg
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            when (activeTab) {
                0 -> {
                    // General Settings Tab
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item {
                            Text("Theme & Display Options", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = brandBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            SettingsRowToggle(
                                title = "Dark Theme Mode",
                                subtitle = "Enable battery-saving dark interface style",
                                checked = isDarkTheme,
                                onCheckedChange = onThemeChange,
                                primaryText = primaryText,
                                secondaryText = secondaryText
                            )
                        }

                        item {
                            HorizontalDivider(color = secondaryText.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Sound & Haptics", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = brandBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            SettingsRowToggle(
                                title = "Dialpad Touch Tones",
                                subtitle = "Play DTMF synthesizer tones upon number press",
                                checked = dialpadTonesEnabled,
                                onCheckedChange = onTonesChange,
                                primaryText = primaryText,
                                secondaryText = secondaryText
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            SettingsRowToggle(
                                title = "Tactile Vibration Feedback",
                                subtitle = "Vibrate briefly upon clicking key buttons",
                                checked = vibrateOnClickEnabled,
                                onCheckedChange = onVibrateChange,
                                primaryText = primaryText,
                                secondaryText = secondaryText
                            )
                        }

                        item {
                            HorizontalDivider(color = secondaryText.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Dialer Capabilities", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = brandBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            SettingsRowNav(
                                title = "Speed Dial Setup",
                                subtitle = "Assign digit hotkeys 1-9 to specific numbers",
                                onClick = { activeTab = 2 },
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                activePill = activePill,
                                isDarkTheme = isDarkTheme
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            SettingsRowNav(
                                title = "Call Blocking",
                                subtitle = "Manage phone numbers to auto-reject",
                                onClick = { activeTab = 1 },
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                activePill = activePill,
                                isDarkTheme = isDarkTheme
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            SettingsRowNav(
                                title = "Quick SMS Templates",
                                subtitle = "Edit SMS responses for declined calls",
                                onClick = { activeTab = 3 },
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                activePill = activePill,
                                isDarkTheme = isDarkTheme
                            )
                        }

                        item {
                            HorizontalDivider(color = secondaryText.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("SIM & Telecom Config", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = brandBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Preferred Outbound SIM", fontWeight = FontWeight.Medium, color = primaryText)
                                    Text("SIM subscription for default calls", fontSize = 12.sp, color = secondaryText)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf("SIM 1", "SIM 2", "Ask").forEach { op ->
                                        val sel = preferredSim == op
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (sel) activePill else searchBg)
                                                .clickable { onSimChange(op) }
                                                .padding(horizontal = 10.dp, vertical = 6.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(op, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (sel) brandBlue else secondaryText)
                                        }
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = voicemailNumber,
                                onValueChange = onVoicemailChange,
                                label = { Text("Voicemail Retrieval Number") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        item {
                            HorizontalDivider(color = secondaryText.copy(alpha = 0.2f))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("About & Privacy", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = brandBlue)
                            Spacer(modifier = Modifier.height(8.dp))
                            SettingsRowNav(
                                title = "About Me",
                                subtitle = "Information about this app",
                                onClick = {
                                    val intent = Intent(context, com.example.ui.AboutActivity::class.java)
                                    context.startActivity(intent)
                                },
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                activePill = activePill,
                                isDarkTheme = isDarkTheme
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            SettingsRowNav(
                                title = "Privacy Policy",
                                subtitle = "View our data handling practices",
                                onClick = {
                                    val intent = Intent(context, com.example.ui.PrivacyPolicyActivity::class.java)
                                    context.startActivity(intent)
                                },
                                primaryText = primaryText,
                                secondaryText = secondaryText,
                                activePill = activePill,
                                isDarkTheme = isDarkTheme
                            )
                        }
                    }
                }

                1 -> {
                    // Block List View
                    Column(modifier = Modifier.fillMaxSize()) {
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
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = brandBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("+ Block", color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Currently Blocked Numbers:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = secondaryText)

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(blockedNumbers, key = { it }) { num ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(searchBg)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(num, color = primaryText, fontWeight = FontWeight.Medium)
                                    IconButton(onClick = { blockedNumbers.remove(num) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Unblock", tint = Color.Red.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Speed Dial Setup
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text("Assign keys 1-9 to instantly call specified contacts:", fontSize = 13.sp, color = secondaryText)
                        Spacer(modifier = Modifier.height(16.dp))

                        if (targetSpeedDialKey != -1) {
                            var speedNumInput by remember { mutableStateOf("") }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = searchBg),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Assign Number to Speed Dial Key $targetSpeedDialKey:",
                                        fontWeight = FontWeight.Bold,
                                        color = primaryText
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    OutlinedTextField(
                                        value = speedNumInput,
                                        onValueChange = { speedNumInput = it },
                                        label = { Text("Phone Number") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row {
                                        Button(
                                            onClick = {
                                                if (speedNumInput.isNotBlank()) {
                                                    speedDialMap[targetSpeedDialKey] = speedNumInput.trim()
                                                    targetSpeedDialKey = -1
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = brandBlue)
                                        ) {
                                            Text("Assign", color = Color.White)
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Button(
                                            onClick = { targetSpeedDialKey = -1 },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent)
                                        ) {
                                            Text("Cancel", color = brandBlue)
                                        }
                                    }
                                }
                            }
                        } else {
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                items((1..9).toList(), key = { it }) { digit ->
                                    val assignedNum = speedDialMap[digit]

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(searchBg)
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(activePill),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(digit.toString(), fontWeight = FontWeight.Bold, color = brandBlue)
                                            }
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(
                                                    text = assignedNum ?: "Unassigned",
                                                    color = primaryText,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = if (assignedNum != null) "Long press $digit on Dialer to call" else "Tap gear to assign",
                                                    fontSize = 12.sp,
                                                    color = secondaryText
                                                )
                                            }
                                        }

                                        Row {
                                            IconButton(onClick = { targetSpeedDialKey = digit }) {
                                                Text("⚙️", fontSize = 16.sp)
                                            }
                                            if (assignedNum != null) {
                                                IconButton(onClick = { speedDialMap.remove(digit) }) {
                                                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear", tint = Color.Red.copy(alpha = 0.7f))
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
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = newQuickRespInput,
                                onValueChange = { newQuickRespInput = it },
                                label = { Text("Add Quick SMS Template") },
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
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = brandBlue),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("+ Add", color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("Decline SMS Templates List:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = secondaryText)

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(quickResponses, key = { it }) { resp ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(searchBg)
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(resp, color = primaryText, fontSize = 14.sp, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { quickResponses.remove(resp) }) {
                                        Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete response", tint = Color.Red.copy(alpha = 0.8f))
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

@Composable
fun SettingsRowToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    primaryText: Color,
    secondaryText: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Medium, color = primaryText)
            Text(text = subtitle, fontSize = 12.sp, color = secondaryText)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = LightBlue
            )
        )
    }
}

@Composable
fun SettingsRowNav(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    primaryText: Color,
    secondaryText: Color,
    activePill: Color,
    isDarkTheme: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontWeight = FontWeight.Medium, color = primaryText)
            Text(text = subtitle, fontSize = 12.sp, color = secondaryText)
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(activePill),
            contentAlignment = Alignment.Center
        ) {
            Text("➔", fontSize = 12.sp, color = if (isDarkTheme) Color.Black else Color.White, fontWeight = FontWeight.Bold)
        }
    }
}
