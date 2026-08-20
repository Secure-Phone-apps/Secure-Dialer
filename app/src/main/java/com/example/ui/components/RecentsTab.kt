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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
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
import com.example.model.*
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.model.CallType
import com.example.ui.theme.LocalM3Expressive
import com.example.ui.theme.getMissedCallColor
import com.example.ui.theme.getDialedCallColor
import com.example.ui.theme.getReceivedCallColor
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Schedule
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class RecentsFilter {
    ALL, MISSED, DIALED, RECEIVED
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecentsTabContent(
    viewModel: com.example.ui.viewmodel.DialerViewModel,
    callRecords: List<CallRecord>,
    onCallClick: (CallRecord) -> Unit,
    onDeleteRecord: (Int) -> Unit,
    hasPermission: Boolean = true,
    isLoading: Boolean = false,
    onRequestPermission: () -> Unit = {}
) {
    var currentFilter by remember { mutableStateOf(RecentsFilter.ALL) }
    var selectedHistoryNumber by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(selectedHistoryNumber) {
        viewModel.isCallHistoryDetailsOpen.value = (selectedHistoryNumber != null)
    }

    if (selectedHistoryNumber != null) {
        val filteredLogs = remember(callRecords, selectedHistoryNumber) {
            callRecords.filter { it.number == selectedHistoryNumber }
        }
        CallHistoryDetailsScreen(
            number = selectedHistoryNumber!!,
            logs = filteredLogs,
            viewModel = viewModel,
            onCallClick = onCallClick,
            onBack = { selectedHistoryNumber = null }
        )
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp)
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

            if (isLoading) {
                RecentsSkeleton()
            } else {
                val isDashboardEnabled by viewModel.isCallLogDashboardEnabled
                val isFiltersEnabled by viewModel.isCallLogFiltersEnabled

                // 1. Call Log Summary Dashboard
                if (isDashboardEnabled) {
                    CallLogSummaryDashboard(
                        callRecords = callRecords,
                        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                    )
                }

                // 2. Borderless Color-Adapting Call Log Filter Chips (Squircle / Rounded Pill)
                if (isFiltersEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        data class RecentsFilterItem(
                            val label: String,
                            val filter: RecentsFilter,
                            val icon: androidx.compose.ui.graphics.vector.ImageVector,
                            val iconColor: Color
                        )

                        val filterOptions = listOf(
                            RecentsFilterItem("All", RecentsFilter.ALL, Icons.Default.History, MaterialTheme.colorScheme.primary),
                            RecentsFilterItem("Missed", RecentsFilter.MISSED, Icons.Default.CallMissed, Color(0xFFD32F2F)),
                            RecentsFilterItem("Dialed", RecentsFilter.DIALED, Icons.AutoMirrored.Filled.CallMade, Color(0xFF0288D1)),
                            RecentsFilterItem("Received", RecentsFilter.RECEIVED, Icons.AutoMirrored.Filled.CallReceived, Color(0xFF388E3C))
                        )

                        filterOptions.forEach { item ->
                            val isSelected = currentFilter == item.filter

                            val containerColor by androidx.compose.animation.animateColorAsState(
                                targetValue = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                                },
                                animationSpec = androidx.compose.animation.core.tween(150),
                                label = "filterChipBg"
                            )

                            val contentColor by androidx.compose.animation.animateColorAsState(
                                targetValue = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                                animationSpec = androidx.compose.animation.core.tween(150),
                                label = "filterChipText"
                            )

                            val iconTint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else item.iconColor

                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(containerColor)
                                    .clickable { currentFilter = item.filter }
                                    .padding(horizontal = 8.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.label,
                                    tint = iconTint,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = item.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = contentColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                if (callRecords.isEmpty()) {
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
                    // 2. Consolidated Call Logs list or search results state
                    val query by viewModel.searchQuery
                    val consolidatedRecords = remember(callRecords, currentFilter, query) {
                        groupCallRecords(callRecords, currentFilter, query)
                    }

                    if (consolidatedRecords.isEmpty() && query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            EmptyStateIllustration(
                                title = stringResource(R.string.no_results_title),
                                subtitle = stringResource(R.string.no_matching_calls_for, query)
                            )
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(vertical = 2.dp)
                        ) {
                            items(
                                items = consolidatedRecords,
                                key = { it.primary.id },
                                contentType = { "recent_call_group" }
                            ) { group ->
                                RecentCallRow(
                                    group = group,
                                    onCallClick = { onCallClick(group.primary) },
                                    onDeleteRecord = { id -> onDeleteRecord(id) },
                                    getHistory = { viewModel.getCallHistoryByNumber(it) },
                                    viewModel = viewModel,
                                    onHistoryClick = { selectedHistoryNumber = it }
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

data class CallGroup(
    val primary: CallRecord,
    val calls: List<CallRecord>
)

private fun normalizePhoneNumberForKey(number: String, name: String): String {
    val cleanName = name.trim()
    val digits = number.filter { it.isDigit() }
    return when {
        cleanName.isNotBlank() && cleanName != number && cleanName != "Unknown" -> cleanName.lowercase(Locale.ROOT)
        digits.length >= 7 -> digits.takeLast(10)
        digits.isNotEmpty() -> digits
        else -> number.trim().lowercase(Locale.ROOT)
    }
}

fun groupCallRecords(
    callRecords: List<CallRecord>,
    filter: RecentsFilter,
    query: String
): List<CallGroup> {
    if (callRecords.isEmpty()) return emptyList()

    val cleanQuery = query.trim()
    val hasQuery = cleanQuery.isNotEmpty()

    // Map maintaining order of insertion (latest call for each contact appears first)
    val groupedMap = LinkedHashMap<String, MutableList<CallRecord>>()

    for (i in callRecords.indices) {
        val record = callRecords[i]

        // 1. Filter by Call Type
        val matchesFilter = when (filter) {
            RecentsFilter.ALL -> true
            RecentsFilter.MISSED -> record.type == CallType.MISSED
            RecentsFilter.DIALED -> record.type == CallType.OUTGOING
            RecentsFilter.RECEIVED -> record.type == CallType.INCOMING
        }
        if (!matchesFilter) continue

        // 2. Filter by Search Query
        if (hasQuery) {
            val matchesQuery = record.name.contains(cleanQuery, ignoreCase = true) ||
                    record.number.contains(cleanQuery, ignoreCase = true)
            if (!matchesQuery) continue
        }

        // 3. Determine unique grouping key
        val key = normalizePhoneNumberForKey(record.number, record.name)

        var list = groupedMap[key]
        if (list == null) {
            list = ArrayList()
            groupedMap[key] = list
        }
        list.add(record)
    }

    if (groupedMap.isEmpty()) return emptyList()

    val result = ArrayList<CallGroup>(groupedMap.size)
    for (list in groupedMap.values) {
        result.add(CallGroup(list.first(), list))
    }

    return result
}
