package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.History
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
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.model.CallType
import com.example.ui.theme.LocalM3Expressive

enum class RecentsFilter {
    ALL, MISSED, DIALED, RECEIVED
}

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
    var currentFilter by remember { mutableStateOf(RecentsFilter.ALL) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (!hasPermission && !isLoading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer
                    ),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.permissions_required),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.call_log_perm_desc),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = onRequestPermission,
                            modifier = Modifier.height(40.dp)
                        ) {
                            Text(stringResource(R.string.enable_call_log_perm))
                        }
                    }
                }
            }

            // Google Dialer style filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val filters = listOf(
                    Triple(RecentsFilter.ALL, "All", Icons.Default.History),
                    Triple(RecentsFilter.MISSED, "Missed", Icons.Default.CallMissed),
                    Triple(RecentsFilter.DIALED, "Dialed", Icons.AutoMirrored.Filled.CallMade),
                    Triple(RecentsFilter.RECEIVED, "Received", Icons.AutoMirrored.Filled.CallReceived)
                )

                filters.forEach { (filter, label, icon) ->
                    val selected = currentFilter == filter
                    val iconTint = when (filter) {
                        RecentsFilter.MISSED -> MaterialTheme.colorScheme.error
                        RecentsFilter.DIALED -> Color(0xFF2E7D32)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    FilterChip(
                        selected = selected,
                        onClick = { currentFilter = filter },
                        label = { Text(label) },
                        leadingIcon = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = iconTint,
                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedLeadingIconColor = iconTint
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

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
                        title = stringResource(R.string.no_call_log_title),
                        subtitle = stringResource(R.string.no_call_log_subtitle)
                    )
                }
            } else {
                val query by viewModel.searchQuery
                val consolidatedRecords = remember(callRecordsPaged.itemCount, callRecordsPaged.loadState.refresh, currentFilter, query) {
                    val baseFiltered = callRecordsPaged.itemSnapshotList.items
                        .filter { record ->
                            when (currentFilter) {
                                RecentsFilter.ALL -> true
                                RecentsFilter.MISSED -> record.type == CallType.MISSED
                                RecentsFilter.DIALED -> record.type == CallType.OUTGOING
                                RecentsFilter.RECEIVED -> record.type == CallType.INCOMING
                            }
                        }
                        .filter { record ->
                            if (query.isBlank()) {
                                true
                            } else {
                                record.name.contains(query, ignoreCase = true) ||
                                record.number.contains(query, ignoreCase = true)
                            }
                        }
                    
                    baseFiltered.groupBy { it.number }.map { (_, records) ->
                        CallGroup(records.first(), records)
                    }
                }

                if (consolidatedRecords.isEmpty() && query.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyStateIllustration(
                            title = "No results found",
                            subtitle = "No matching calls for \"$query\""
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(vertical = 4.dp)
                    ) {
                        items(
                            items = consolidatedRecords,
                            key = { it.primary.id }
                        ) { group ->
                            RecentCallRow(
                                group = group,
                                onCallClick = { onCallClick(group.primary) },
                                onDeleteRecord = { id -> onDeleteRecord(id) },
                                getHistory = { viewModel.getCallHistoryByNumber(it) },
                                viewModel = viewModel
                            )
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


    }
}

@Composable
fun RecentCallRow(
    group: CallGroup,
    onCallClick: () -> Unit,
    onDeleteRecord: (Int) -> Unit,
    getHistory: suspend (String) -> List<CallRecord>,
    viewModel: com.example.ui.viewmodel.DialerViewModel
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

    val isExpressive = LocalM3Expressive.current
    val searchBarColor = if (isExpressive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    }
    val containerColor = if (isExpanded) {
        searchBarColor.copy(alpha = minOf(1f, searchBarColor.alpha + 0.15f))
    } else {
        searchBarColor
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                isExpanded = !isExpanded
            },
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            ListItem(
                headlineContent = {
                    Column(
                        modifier = Modifier.offset(x = (-8).dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = record.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 18.sp,
                                color = if (record.type == CallType.MISSED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (group.calls.size > 1) {
                                Text(
                                    text = " (${group.calls.size})",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val (icon, iconColor) = when (record.type) {
                                CallType.MISSED -> Icons.Default.CallMissed to MaterialTheme.colorScheme.error
                                CallType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade to Color(0xFF2E7D32)
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
                    }
                },
                supportingContent = null,
                leadingContent = {
                    val avatarShape = if (LocalM3Expressive.current) MaterialTheme.shapes.medium else CircleShape
                    Surface(
                        modifier = Modifier
                            .offset(x = (-8).dp)
                            .size(40.dp),
                        shape = avatarShape,
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
                    
                    if (isLoadingHistory) {
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth().height(2.dp).padding(horizontal = 16.dp)
                        )
                    }

                    Column {
                        history.take(5).forEach { historyRecord ->
                            HistorySubItem(
                                record = historyRecord,
                                onDeleteClick = {
                                    onDeleteRecord(historyRecord.id)
                                    history = history.filter { it.id != historyRecord.id }
                                }
                            )
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

                    if (history.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = {
                                history.forEach { r -> onDeleteRecord(r.id) }
                                history = emptyList()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            ),
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .height(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Delete All Call History",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
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
            .clip(MaterialTheme.shapes.small)
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
fun HistorySubItem(record: CallRecord, onDeleteClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val (icon, color) = when (record.type) {
            CallType.MISSED -> Icons.Default.CallMissed to MaterialTheme.colorScheme.error
            CallType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade to Color(0xFF2E7D32)
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

        IconButton(onClick = onDeleteClick) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Delete call log entry",
                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                modifier = Modifier.size(20.dp)
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
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(12.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
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
