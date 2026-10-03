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

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.ui.theme.getDialedCallColor
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.RichHapticEngine
import java.util.Locale

enum class RecentsFilter {
    ALL, MISSED, DIALED, RECEIVED
}

@Composable
fun RecentsTabContent(
    viewModel: DialerViewModel,
    callRecords: List<CallRecord>,
    onCallClick: (CallRecord) -> Unit,
    onDeleteRecord: (Int) -> Unit,
    hasPermission: Boolean = true,
    isLoading: Boolean = false,
    onRequestPermission: () -> Unit = {}
) {
    val context = LocalContext.current
    var currentFilter by remember { mutableStateOf(RecentsFilter.ALL) }
    var selectedHistoryNumber by remember { mutableStateOf<String?>(null) }

    // FIXED: Intercept Android back gesture to exit call history details back to recents list
    BackHandler(enabled = selectedHistoryNumber != null) {
        selectedHistoryNumber = null
    }

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

                    if (isDashboardEnabled) {
                        CallLogSummaryDashboard(
                            callRecords = callRecords,
                            modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                        )
                    }

                    if (isFiltersEnabled) {
                        var showClearConfirmDialog by remember { mutableStateOf(false) }

                        if (showClearConfirmDialog) {
                            AlertDialog(
                                onDismissRequest = { showClearConfirmDialog = false },
                                title = { Text(stringResource(R.string.clear_call_log_confirm_title)) },
                                text = { Text(stringResource(R.string.clear_call_log_confirm_desc)) },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.WARNING)
                                            showClearConfirmDialog = false
                                            viewModel.clearAllCallLogs()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.error
                                        )
                                    ) {
                                        Text(stringResource(R.string.btn_clear_all))
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showClearConfirmDialog = false }) {
                                        Text(stringResource(R.string.btn_cancel))
                                    }
                                }
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val filterOptions = listOf(
                                    RecentsFilterItem(stringResource(R.string.filter_all), RecentsFilter.ALL, Icons.Default.History, MaterialTheme.colorScheme.primary),
                                    RecentsFilterItem(stringResource(R.string.filter_missed), RecentsFilter.MISSED, Icons.Default.CallMissed, Color(0xFFD32F2F)),
                                    RecentsFilterItem(stringResource(R.string.filter_dialed), RecentsFilter.DIALED, Icons.AutoMirrored.Filled.CallMade, getDialedCallColor()),
                                    RecentsFilterItem(stringResource(R.string.filter_received), RecentsFilter.RECEIVED, Icons.AutoMirrored.Filled.CallReceived, Color(0xFF388E3C))
                                )

                                filterOptions.forEach { item ->
                                    val isSelected = currentFilter == item.filter

                                    val containerColor by animateColorAsState(
                                        targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                                        animationSpec = tween(150),
                                        label = "filterChipBg"
                                    )

                                    val contentColor by animateColorAsState(
                                        targetValue = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                                        animationSpec = tween(150),
                                        label = "filterChipText"
                                    )

                                    val iconTint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else item.iconColor

                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(containerColor)
                                            .clickable {
                                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                                currentFilter = item.filter
                                            }
                                            .padding(horizontal = 4.dp, vertical = 7.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = item.icon,
                                            contentDescription = item.label,
                                            tint = iconTint,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = item.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = contentColor,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                    showClearConfirmDialog = true
                                },
                                modifier = Modifier.size(36.dp),
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.btn_clear_all),
                                    modifier = Modifier.size(18.dp)
                                )
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

private data class RecentsFilterItem(
    val label: String,
    val filter: RecentsFilter,
    val icon: ImageVector,
    val iconColor: Color
)

@Immutable
data class CallGroup(
    val primary: CallRecord,
    val calls: List<CallRecord>
)

// FIXED: Key ties strictly to normalized number to prevent different people with common names from merging
private fun normalizePhoneNumberForKey(number: String, name: String): String {
    val cleanName = name.trim().lowercase(Locale.ROOT)
    val digits = number.filter { it.isDigit() }
    val normalizedNumber = if (digits.length >= 7) digits.takeLast(10) else digits.ifEmpty { number.trim() }

    return if (cleanName.isNotBlank() && cleanName != "unknown") {
        "${normalizedNumber}_$cleanName"
    } else {
        normalizedNumber
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
    val groupedMap = LinkedHashMap<String, MutableList<CallRecord>>()

    for (i in callRecords.indices) {
        val record = callRecords[i]

        val matchesFilter = when (filter) {
            RecentsFilter.ALL -> true
            RecentsFilter.MISSED -> record.type == CallType.MISSED
            RecentsFilter.DIALED -> record.type == CallType.OUTGOING
            RecentsFilter.RECEIVED -> record.type == CallType.INCOMING
        }
        if (!matchesFilter) continue

        if (hasQuery) {
            val matchesQuery = record.name.contains(cleanQuery, ignoreCase = true) ||
                    record.number.contains(cleanQuery, ignoreCase = true)
            if (!matchesQuery) continue
        }

        val key = normalizePhoneNumberForKey(record.number, record.name)
        val list = groupedMap.getOrPut(key) { ArrayList() }
        list.add(record)
    }

    if (groupedMap.isEmpty()) return emptyList()

    val result = ArrayList<CallGroup>(groupedMap.size)
    for (list in groupedMap.values) {
        result.add(CallGroup(list.first(), list))
    }
    return result
}