package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallRecord
import com.example.model.CallType
import kotlinx.coroutines.delay

@Composable
fun RecentsTabContent(
    callRecords: List<CallRecord>,
    onCallClick: (CallRecord) -> Unit,
    onDeleteRecord: (Int) -> Unit,
    primaryText: Color,
    secondaryText: Color,
    activePill: Color,
    brandBlue: Color,
    hasPermission: Boolean = true,
    isLoading: Boolean = false,
    onRequestPermission: () -> Unit = {}
) {
    var expandedRecordId by remember { mutableStateOf<Int?>(null) }
    var isPlayingVoicemail by remember { mutableStateOf(false) }
    var voicemailProgress by remember { mutableStateOf(0.0f) }
    var currentPlayingId by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(isPlayingVoicemail, currentPlayingId) {
        if (isPlayingVoicemail && currentPlayingId != null) {
            while (voicemailProgress < 1.0f) {
                delay(100)
                voicemailProgress += 0.04f
            }
            isPlayingVoicemail = false
            voicemailProgress = 0.0f
        }
    }

    // Group call history records by number, preserving newest first order
    val groupedRecords = remember(callRecords) {
        val groups = mutableListOf<Pair<CallRecord, List<CallRecord>>>()
        val seenNumbers = mutableSetOf<String>()
        for (record in callRecords) {
            if (record.number !in seenNumbers) {
                seenNumbers.add(record.number)
                val sameNumberRecords = callRecords.filter { it.number == record.number }
                groups.add(record to sameNumberRecords)
            }
        }
        groups
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp, start = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recents",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = secondaryText,
                modifier = Modifier.testTag("recents_header")
            )
            Text(
                text = "Clear All",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = brandBlue,
                modifier = Modifier.clickable {
                    callRecords.forEach { onDeleteRecord(it.id) }
                }
            )
        }

        if (!hasPermission && !isLoading) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                colors = CardDefaults.cardColors(containerColor = activePill.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Permissions Required", fontWeight = FontWeight.Bold, color = brandBlue, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "To access, load, and call the real call history on your phone, please enable the Call Log Permission.",
                        fontSize = 11.sp,
                        color = secondaryText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = brandBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Enable Call Log", fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }

        if (groupedRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🕒", fontSize = 48.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Text("Your call log is empty", fontWeight = FontWeight.Medium, color = secondaryText)
                    Text("Recent calls will show up here", fontSize = 12.sp, color = secondaryText)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(groupedRecords, key = { it.first.id }) { (record, sameNumberRecords) ->
                    val isExpanded = expandedRecordId == record.id

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isExpanded) secondaryText.copy(alpha = 0.06f) else Color.Transparent)
                            .clickable {
                                if (isExpanded) {
                                    expandedRecordId = null
                                    isPlayingVoicemail = false
                                    voicemailProgress = 0.0f
                                    currentPlayingId = null
                                } else {
                                    expandedRecordId = record.id
                                    isPlayingVoicemail = false
                                    voicemailProgress = 0.0f
                                    currentPlayingId = record.id
                                }
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(record.avatarBg),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = record.avatarText,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = record.avatarTextColor
                                )
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (sameNumberRecords.size > 1) "${record.name} (${sameNumberRecords.size})" else record.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = primaryText
                                )
                                if (record.number.isNotEmpty()) {
                                    Text(
                                        text = record.number,
                                        fontSize = 14.sp,
                                        color = secondaryText
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    val (arrow, arrowColor) = when (record.type) {
                                        CallType.MISSED -> "↙" to MaterialTheme.colorScheme.onSurfaceVariant
                                        CallType.OUTGOING -> "↗" to MaterialTheme.colorScheme.primary
                                        CallType.INCOMING -> "↔" to MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                    Text(text = arrow, color = arrowColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text(
                                        text = "${record.label} • ${record.timestamp}",
                                        fontSize = 13.sp,
                                        color = secondaryText
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(activePill)
                                        .clickable { onCallClick(record) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Call,
                                        contentDescription = "Call",
                                        tint = if (activePill == Color(0xFF004A77)) Color.White else Color(0xFF041E49),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(
                            visible = isExpanded,
                            enter = expandVertically() + fadeIn(),
                            exit = shrinkVertically() + fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp, start = 64.dp, end = 8.dp)
                            ) {
                                HorizontalDivider(
                                    color = secondaryText.copy(alpha = 0.12f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                )

                                // Call History Header
                                Text(
                                    text = "Call History (${sameNumberRecords.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = secondaryText,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                sameNumberRecords.forEachIndexed { index, subRecord ->
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                val (arrow, arrowColor) = when (subRecord.type) {
                                                    CallType.MISSED -> "↙" to MaterialTheme.colorScheme.onSurfaceVariant
                                                    CallType.OUTGOING -> "↗" to MaterialTheme.colorScheme.primary
                                                    CallType.INCOMING -> "↔" to MaterialTheme.colorScheme.onSurfaceVariant
                                                }
                                                Text(text = arrow, color = arrowColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                                Text(
                                                    text = subRecord.type.name.lowercase().replaceFirstChar { it.uppercase() },
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = primaryText
                                                )
                                                Text(
                                                    text = "• ${subRecord.timestamp}",
                                                    fontSize = 13.sp,
                                                    color = secondaryText
                                                )
                                            }

                                            IconButton(
                                                onClick = { onDeleteRecord(subRecord.id) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = Color.Red.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = "Duration: ${if (subRecord.type == CallType.MISSED) "0s (Missed)" else "${subRecord.duration / 60} mins ${subRecord.duration % 60} secs"}",
                                            fontSize = 12.sp,
                                            color = secondaryText,
                                            modifier = Modifier.padding(start = 18.dp, bottom = 4.dp)
                                        )

                                        // Show voicemail player if this specific subRecord is a voicemail
                                        if (subRecord.hasVoicemail) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = secondaryText.copy(alpha = 0.08f)),
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(start = 18.dp, top = 4.dp, bottom = 4.dp)
                                            ) {
                                                Column(modifier = Modifier.padding(8.dp)) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        IconButton(
                                                            onClick = {
                                                                if (currentPlayingId == subRecord.id) {
                                                                    isPlayingVoicemail = !isPlayingVoicemail
                                                                } else {
                                                                    currentPlayingId = subRecord.id
                                                                    voicemailProgress = 0.0f
                                                                    isPlayingVoicemail = true
                                                                }
                                                            },
                                                            modifier = Modifier
                                                                .size(28.dp)
                                                                .clip(CircleShape)
                                                                .background(activePill)
                                                        ) {
                                                            Text(if (isPlayingVoicemail && currentPlayingId == subRecord.id) "⏸️" else "▶️", fontSize = 11.sp)
                                                        }

                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = "Voicemail Message",
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                color = primaryText
                                                            )
                                                            Spacer(modifier = Modifier.height(2.dp))
                                                            LinearProgressIndicator(
                                                                progress = { if (currentPlayingId == subRecord.id) voicemailProgress else 0.0f },
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .height(3.dp)
                                                                    .clip(RoundedCornerShape(1.5.dp)),
                                                                color = activePill,
                                                                trackColor = secondaryText.copy(alpha = 0.15f)
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = "Transcript:",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = secondaryText
                                                    )
                                                    Text(
                                                        text = when (subRecord.number) {
                                                            "+1 (555) 013-1122" -> "\"Hey there, just checking in. I wanted to see if you have the files for the marketing campaign ready? Give me a call back when you can. Bye!\""
                                                            "+1 (555) 011-9988" -> "\"Hello, this is a call from utility services regarding your bill update. Please press 1 or call back at your earliest convenience.\""
                                                            else -> "\"Hey! I missed you. Let me know when you're free to chat. Talk to you soon!\""
                                                        },
                                                        fontSize = 11.sp,
                                                        fontStyle = FontStyle.Italic,
                                                        color = primaryText,
                                                        modifier = Modifier.padding(top = 1.dp)
                                                    )
                                                }
                                            }
                                        }

                                        if (index < sameNumberRecords.lastIndex) {
                                            HorizontalDivider(
                                                color = secondaryText.copy(alpha = 0.06f),
                                                thickness = 0.5.dp,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                }

                                // Interactive Call Actions
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Button(
                                        onClick = { onCallClick(record) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = activePill,
                                            contentColor = if (activePill == Color(0xFF004A77)) Color.White else Color(0xFF041E49)
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Call Back", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = {
                                            sameNumberRecords.forEach { onDeleteRecord(it.id) }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFDC2626), // Solid bright red
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Text("Delete All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
