package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.model.CallRecord
import com.example.model.CallType

@Composable
fun RecentsTabContent(
    viewModel: com.example.ui.viewmodel.DialerViewModel,
    callRecordsPaged: LazyPagingItems<CallRecord>,
    onCallClick: (CallRecord) -> Unit,
    onDeleteRecord: (Int) -> Unit,
    hasPermission: Boolean = true,
    isLoading: Boolean = false,
    onRequestPermission: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recents",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("recents_header")
            )
        }

        if (!hasPermission && !isLoading) {
            // ... (keep permission block)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Permissions Required",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "To access, load, and call the real call history on your phone, please enable the Call Log Permission.",
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = onRequestPermission,
                        modifier = Modifier.height(40.dp)
                    ) {
                        Text("Enable Call Log")
                    }
                }
            }
        }

        if (callRecordsPaged.itemCount == 0 && callRecordsPaged.loadState.refresh is LoadState.NotLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                EmptyStateIllustration(
                    title = "Your call log is empty",
                    subtitle = "Recent calls will show up here"
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    count = callRecordsPaged.itemCount,
                    key = callRecordsPaged.itemKey { it.id },
                    contentType = callRecordsPaged.itemContentType { "call_record" }
                ) { index ->
                    val record = callRecordsPaged[index]
                    if (record != null) {
                        RecentCallRow(
                            record = record,
                            onCallClick = { onCallClick(record) },
                            onDeleteRecord = { onDeleteRecord(record.id) },
                            getHistory = { viewModel.getCallHistoryByNumber(it) }
                        )
                    }
                }

                if (callRecordsPaged.loadState.append is LoadState.Loading) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentCallRow(
    record: CallRecord,
    onCallClick: () -> Unit,
    onDeleteRecord: () -> Unit,
    getHistory: suspend (String) -> List<CallRecord>
) {
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }
    var history by remember { mutableStateOf<List<CallRecord>>(emptyList()) }
    var isLoadingHistory by remember { mutableStateOf(false) }

    LaunchedEffect(isExpanded) {
        if (isExpanded && history.isEmpty()) {
            isLoadingHistory = true
            history = getHistory(record.number)
            isLoadingHistory = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isExpanded = !isExpanded
            },
        colors = CardDefaults.cardColors(containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f) else Color.Transparent)
    ) {
        Column {
            ListItem(
                headlineContent = {
                    Text(
                        text = record.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = if (record.type == CallType.MISSED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                },
                supportingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val (arrow, arrowColor) = when (record.type) {
                            CallType.MISSED -> "↙" to MaterialTheme.colorScheme.error
                            CallType.OUTGOING -> "↗" to Color(0xFF4CAF50)
                            CallType.INCOMING -> "↔" to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(text = arrow, color = arrowColor, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                        val displayDetails = if (record.name != record.number && record.number.isNotBlank()) {
                            "${record.number} • ${record.label} • ${record.timestamp}"
                        } else {
                            "${record.label} • ${record.timestamp}"
                        }
                        Text(
                            text = displayDetails,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                },
                leadingContent = {
                    Surface(
                        modifier = Modifier.size(44.dp),
                        shape = CircleShape,
                        color = record.avatarBg
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = record.avatarText,
                                style = MaterialTheme.typography.titleMedium,
                                color = record.avatarTextColor
                            )
                        }
                    }
                },
                trailingContent = {
                    Row {
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCallClick()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onDeleteRecord()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            if (isExpanded) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                
                if (isLoadingHistory) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth().height(2.dp).padding(horizontal = 16.dp)
                    )
                }

                Column(modifier = Modifier.padding(bottom = 8.dp)) {
                    history.take(10).forEach { historyRecord ->
                        HistorySubItem(historyRecord)
                    }
                    if (history.size > 10) {
                        Text(
                            text = "showing last 10 calls",
                            modifier = Modifier.padding(start = 72.dp, top = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistorySubItem(record: CallRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(56.dp)) // Align with main item text
        val (arrow, arrowColor) = when (record.type) {
            CallType.MISSED -> "↙" to MaterialTheme.colorScheme.error
            CallType.OUTGOING -> "↗" to Color(0xFF4CAF50)
            CallType.INCOMING -> "↔" to MaterialTheme.colorScheme.onSurfaceVariant
        }
        Text(
            text = arrow,
            color = arrowColor,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(20.dp)
        )
        Column {
            Text(
                text = record.timestamp,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            val durationText = if (record.duration > 0) {
                val mins = record.duration / 60
                val secs = record.duration % 60
                if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
            } else {
                if (record.type == CallType.MISSED) "Missed" else "0s"
            }
            Text(
                text = "${record.type.name.lowercase().capitalize()} • $durationText",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
