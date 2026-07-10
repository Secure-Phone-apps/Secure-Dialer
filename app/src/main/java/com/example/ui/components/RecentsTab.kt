package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
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

@OptIn(ExperimentalFoundationApi::class)
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
    var filterByMissed by remember { mutableStateOf(false) }

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

        // Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !filterByMissed,
                onClick = { filterByMissed = false },
                label = { Text("All") },
                leadingIcon = if (!filterByMissed) {
                    { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else null
            )
            FilterChip(
                selected = filterByMissed,
                onClick = { filterByMissed = true },
                label = { Text("Missed") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedLabelColor = MaterialTheme.colorScheme.error,
                    selectedLeadingIconColor = MaterialTheme.colorScheme.error
                ),
                leadingIcon = if (filterByMissed) {
                    { Icon(Icons.Default.Done, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
                } else null
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

        if (isLoading || callRecordsPaged.loadState.refresh is LoadState.Loading) {
            RecentsSkeleton()
        } else if (callRecordsPaged.itemCount == 0 && callRecordsPaged.loadState.refresh is LoadState.NotLoading) {
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
            val recordsSnapshot = callRecordsPaged.itemSnapshotList
            val filteredAndGrouped = remember(recordsSnapshot, filterByMissed) {
                val baseFiltered = recordsSnapshot.items
                    .filter { if (filterByMissed) it.type == CallType.MISSED else true }
                
                val dateGrouped = baseFiltered.groupBy { record ->
                    when {
                        record.timestamp.contains("Today", ignoreCase = true) -> "Today"
                        record.timestamp.contains("Yesterday", ignoreCase = true) -> "Yesterday"
                        else -> "Older"
                    }
                }

                // Further group consecutive calls from the same number within each date group
                dateGrouped.mapValues { (_, records) ->
                    val consolidated = mutableListOf<CallGroup>()
                    if (records.isNotEmpty()) {
                        var currentGroup = mutableListOf(records[0])
                        for (i in 1 until records.size) {
                            if (records[i].number == currentGroup.last().number) {
                                currentGroup.add(records[i])
                            } else {
                                consolidated.add(CallGroup(currentGroup[0], currentGroup.toList()))
                                currentGroup = mutableListOf(records[i])
                            }
                        }
                        consolidated.add(CallGroup(currentGroup[0], currentGroup.toList()))
                    }
                    consolidated
                }
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                filteredAndGrouped.forEach { (dateGroup, consolidatedGroups) ->
                    stickyHeader(key = "header_$dateGroup") {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                        ) {
                            Text(
                                text = dateGroup.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 8.dp),
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    items(
                        items = consolidatedGroups,
                        key = { it.primary.id }
                    ) { group ->
                        RecentCallRow(
                            group = group,
                            onCallClick = { onCallClick(group.primary) },
                            onDeleteRecord = { onDeleteRecord(group.primary.id) },
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
    group: CallGroup,
    onCallClick: () -> Unit,
    onDeleteRecord: () -> Unit,
    getHistory: suspend (String) -> List<CallRecord>
) {
    val record = group.primary
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
        colors = CardDefaults.cardColors(
            containerColor = if (isExpanded) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f) else Color.Transparent
        )
    ) {
        Column {
            ListItem(
                headlineContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = record.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = if (record.type == CallType.MISSED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                        if (group.calls.size > 1) {
                            Text(
                                text = " (${group.calls.size})",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                supportingContent = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val (icon, iconColor) = when (record.type) {
                            CallType.MISSED -> Icons.Default.CallMissed to MaterialTheme.colorScheme.error
                            CallType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade to Color(0xFF4CAF50)
                            CallType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived to MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(14.dp)
                        )
                        
                        val label = if (record.label.isNotBlank()) "${record.label} • " else ""
                        Text(
                            text = "$label${record.timestamp}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                leadingContent = {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = CircleShape,
                        color = record.avatarBg.copy(alpha = 0.8f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = record.avatarText,
                                style = MaterialTheme.typography.titleMedium,
                                color = record.avatarTextColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                trailingContent = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCallClick()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            if (isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    
                    // Primary Actions Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ActionItem(
                            icon = Icons.Default.Call,
                            label = "Call",
                            onClick = onCallClick,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        ActionItem(
                            icon = Icons.Default.Delete,
                            label = "Delete",
                            onClick = onDeleteRecord,
                            tint = MaterialTheme.colorScheme.error
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )

                    if (isLoadingHistory) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp).padding(horizontal = 16.dp)
                        )
                    }

                    Column {
                        history.take(5).forEach { historyRecord ->
                            HistorySubItem(historyRecord)
                        }
                        
                        if (history.size > 5) {
                            Text(
                                text = "Showing last 5 calls",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.padding(start = 72.dp, top = 8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
            .width(64.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint
        )
    }
}

@Composable
fun HistorySubItem(record: CallRecord) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, color) = when (record.type) {
            CallType.MISSED -> Icons.Default.CallMissed to MaterialTheme.colorScheme.error
            CallType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade to Color(0xFF4CAF50)
            CallType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived to MaterialTheme.colorScheme.onSurfaceVariant
        }
        
        Box(
            modifier = Modifier.width(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = color.copy(alpha = 0.8f)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = record.timestamp,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            val typeText = when (record.type) {
                CallType.MISSED -> "Missed call"
                CallType.OUTGOING -> "Outgoing call"
                CallType.INCOMING -> "Incoming call"
            }
            val durationText = if (record.duration > 0) {
                val mins = record.duration / 60
                val secs = record.duration % 60
                val timeStr = if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
                " • $timeStr"
            } else ""
            
            Text(
                text = "$typeText$durationText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun RecentsSkeleton() {
    Column(modifier = Modifier.fillMaxSize()) {
        repeat(5) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.4f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    )
                }
            }
        }
    }
}

data class CallGroup(
    val primary: CallRecord,
    val calls: List<CallRecord>
)
