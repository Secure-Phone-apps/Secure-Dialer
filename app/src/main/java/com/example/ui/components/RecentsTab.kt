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

import android.provider.CallLog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Block
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.ui.theme.AppShapes
import com.example.ui.theme.LocalAdaptiveLayout
import com.example.ui.theme.getBlockedCallColor
import com.example.ui.theme.getDialedCallColor
import com.example.ui.theme.getMissedCallColor
import com.example.ui.theme.getReceivedCallColor
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.MultiSimManager
import com.example.util.RichHapticEngine
import java.util.Locale

const val RECORD_HISTORY_PREFIX = "__record_id_:"

enum class RecentsFilter {
    ALL, MISSED, DIALED, RECEIVED, BLOCKED
}

@Composable
fun RecentsTabContent(
    viewModel: DialerViewModel,
    callRecords: List<CallRecord>,
    onCallClick: (CallRecord) -> Unit,
    onDeleteRecord: (Int) -> Unit,
    hasPermission: Boolean = true,
    isLoading: Boolean = false,
    onRequestPermission: () -> Unit = {},
    isTabActive: Boolean = true
) {
    val context = LocalContext.current
    var currentFilter by remember { mutableStateOf(RecentsFilter.ALL) }
    var selectedHistoryNumber by remember { mutableStateOf<String?>(null) }

    // Intercept Android back gesture only when Recents tab is actively focused
    BackHandler(enabled = isTabActive && selectedHistoryNumber != null) {
        selectedHistoryNumber = null
        viewModel.isCallHistoryDetailsOpen.value = false
    }

    LaunchedEffect(selectedHistoryNumber) {
        viewModel.isCallHistoryDetailsOpen.value = (selectedHistoryNumber != null)
    }

    // Cleanly exit individual history view when user navigates away from Recents tab
    LaunchedEffect(isTabActive, viewModel.isCallHistoryDetailsOpen.value) {
        if ((!isTabActive || !viewModel.isCallHistoryDetailsOpen.value) && selectedHistoryNumber != null) {
            selectedHistoryNumber = null
            viewModel.isCallHistoryDetailsOpen.value = false
        }
    }

    val activeHistoryNumber = selectedHistoryNumber
    if (activeHistoryNumber != null) {
        val isSingleUnknownRecord = activeHistoryNumber.startsWith(RECORD_HISTORY_PREFIX)
        val targetRecordId = if (isSingleUnknownRecord) {
            activeHistoryNumber.removePrefix(RECORD_HISTORY_PREFIX).toIntOrNull()
        } else null

        val filteredLogs = remember(callRecords, activeHistoryNumber, targetRecordId) {
            if (targetRecordId != null) {
                callRecords.filter { it.id == targetRecordId }
            } else {
                callRecords.filter { it.number == activeHistoryNumber }
            }
        }
        CallHistoryDetailsScreen(
            number = if (isSingleUnknownRecord) "" else activeHistoryNumber,
            logs = filteredLogs,
            viewModel = viewModel,
            onCallClick = onCallClick,
            onBack = {
                selectedHistoryNumber = null
                viewModel.isCallHistoryDetailsOpen.value = false
            }
        )
    } else {
        val adaptive = LocalAdaptiveLayout.current
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = adaptive.contentMaxWidth)
                    .fillMaxSize()
                    .padding(horizontal = if (adaptive.isVeryNarrowWidth) 8.dp else 12.dp)
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
                        shape = AppShapes.Card
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

                        val filterFontScale = LocalDensity.current.fontScale
                        BoxWithConstraints(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            val useScrollChips = maxWidth < 460.dp || filterFontScale >= 1.15f
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .then(if (useScrollChips) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val filterOptions = listOf(
                                        RecentsFilterItem(stringResource(R.string.filter_all), RecentsFilter.ALL, Icons.Default.History, MaterialTheme.colorScheme.primary),
                                        RecentsFilterItem(stringResource(R.string.filter_missed), RecentsFilter.MISSED, Icons.Default.CallMissed, getMissedCallColor()),
                                        RecentsFilterItem(stringResource(R.string.filter_dialed), RecentsFilter.DIALED, Icons.AutoMirrored.Filled.CallMade, getDialedCallColor()),
                                        RecentsFilterItem(stringResource(R.string.filter_received), RecentsFilter.RECEIVED, Icons.AutoMirrored.Filled.CallReceived, getReceivedCallColor()),
                                        RecentsFilterItem(stringResource(R.string.filter_blocked), RecentsFilter.BLOCKED, Icons.Default.Block, getBlockedCallColor())
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
                                                .testTag("recents_filter_${item.filter.name.lowercase(Locale.ROOT)}")
                                                .then(if (useScrollChips) Modifier else Modifier.weight(1f))
                                                .heightIn(min = 48.dp)
                                                .clip(AppShapes.Chip)
                                                .background(containerColor)
                                                .clickable {
                                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                                    currentFilter = item.filter
                                                }
                                                .padding(horizontal = if (useScrollChips) 12.dp else 6.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = item.icon,
                                                contentDescription = item.label,
                                                tint = iconTint,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = item.label,
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = contentColor,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                        showClearConfirmDialog = true
                                    },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("clear_call_log_button"),
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
                    }

                    if (callRecords.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState()),
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
                        val listState = rememberLazyListState()
                        val latestCallId = consolidatedRecords.firstOrNull()?.primary?.id

                        LaunchedEffect(latestCallId) {
                            if (latestCallId != null) {
                                listState.animateScrollToItem(0)
                            }
                        }

                        if (consolidatedRecords.isEmpty() && query.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                                contentAlignment = Alignment.Center
                            ) {
                                EmptyStateIllustration(
                                    title = stringResource(R.string.no_results_title),
                                    subtitle = stringResource(R.string.no_matching_calls_for, query)
                                )
                            }
                        } else {
                            val blockedNumbersEntities by viewModel.blockedNumbersFlow.collectAsStateWithLifecycle()
                            val blockedNumbers = remember(blockedNumbersEntities) {
                                blockedNumbersEntities.mapTo(HashSet(blockedNumbersEntities.size)) { it.number }
                            }
                            val allRecordings by viewModel.recordingsFlow.collectAsStateWithLifecycle()
                            val allContacts by viewModel.allContactsFlow.collectAsStateWithLifecycle()
                            val physicalSimCount = remember(context) { MultiSimManager.getPhysicalSimCount(context) }

                            LazyColumn(
                                state = listState,
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
                                        onHistoryClick = { selectedHistoryNumber = it },
                                        onCallRecord = onCallClick,
                                        blockedNumbers = blockedNumbers,
                                        physicalSimCount = physicalSimCount,
                                        allRecordings = allRecordings,
                                        allContacts = allContacts
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

private val UNKNOWN_NUMBER_MARKERS = setOf(
    "-1", "-2", "-3", "unknown", "private", "restricted", "anonymous", "p"
)

private fun isUnknownOrPrivateRecord(record: CallRecord): Boolean {
    if (record.numberPresentation == CallLog.Calls.PRESENTATION_RESTRICTED ||
        record.numberPresentation == CallLog.Calls.PRESENTATION_UNKNOWN ||
        record.numberPresentation == CallLog.Calls.PRESENTATION_PAYPHONE
    ) {
        return true
    }
    val trimmed = record.number.trim()
    if (trimmed.isEmpty()) return true
    val lower = trimmed.lowercase(Locale.ROOT)
    if (lower in UNKNOWN_NUMBER_MARKERS) return true
    val digits = trimmed.filter { it.isDigit() }
    return digits == "0"
}

private fun normalizePhoneNumberKey(number: String): String {
    val trimmed = number.trim()
    val digits = trimmed.filter { it.isDigit() }
    return when {
        digits.length >= 7 -> digits.takeLast(10)
        digits.isNotEmpty() -> digits
        else -> trimmed
    }
}

private fun canGroupConsecutively(previous: CallRecord, current: CallRecord): Boolean {
    // Never collapse distinct Unknown / Private / Restricted / Payphone calls into one counter
    if (isUnknownOrPrivateRecord(previous) || isUnknownOrPrivateRecord(current)) {
        return false
    }
    if (previous.type != current.type) {
        return false
    }
    val prevNorm = normalizePhoneNumberKey(previous.number)
    val currNorm = normalizePhoneNumberKey(current.number)
    return prevNorm.isNotEmpty() && prevNorm == currNorm
}

/**
 * Matches stock Android Dialer grouping:
 * 1. Filters by category and search query.
 * 2. Preserves strict newest-first chronological ordering.
 * 3. Merges strictly consecutive calls that share both the same normalized phone number and the same CallType.
 * 4. Never merges hidden, private, or blank-number calls together.
 */
fun groupCallRecords(
    callRecords: List<CallRecord>,
    filter: RecentsFilter,
    query: String
): List<CallGroup> {
    if (callRecords.isEmpty()) return emptyList()

    val cleanQuery = query.trim()
    val hasQuery = cleanQuery.isNotEmpty()

    val filtered = ArrayList<CallRecord>(callRecords.size)
    for (i in callRecords.indices) {
        val record = callRecords[i]
        val matchesFilter = when (filter) {
            RecentsFilter.ALL -> true
            RecentsFilter.MISSED -> record.type == CallType.MISSED
            RecentsFilter.DIALED -> record.type == CallType.OUTGOING
            RecentsFilter.RECEIVED -> record.type == CallType.INCOMING
            RecentsFilter.BLOCKED -> record.type == CallType.REJECTED || record.type == CallType.BLOCKED
        }
        if (!matchesFilter) continue

        if (hasQuery) {
            val matchesQuery = record.name.contains(cleanQuery, ignoreCase = true) ||
                    record.number.contains(cleanQuery, ignoreCase = true) ||
                    record.callScreeningAppName.contains(cleanQuery, ignoreCase = true)
            if (!matchesQuery) continue
        }

        filtered.add(record)
    }

    if (filtered.isEmpty()) return emptyList()

    filtered.sortWith(compareByDescending<CallRecord> { it.timestampMs }.thenByDescending { it.id })

    val result = ArrayList<CallGroup>(filtered.size)
    var currentBucket = ArrayList<CallRecord>().apply { add(filtered[0]) }

    for (i in 1 until filtered.size) {
        val record = filtered[i]
        val prev = currentBucket.last()
        if (canGroupConsecutively(prev, record)) {
            currentBucket.add(record)
        } else {
            result.add(CallGroup(currentBucket.first(), currentBucket))
            currentBucket = ArrayList<CallRecord>().apply { add(record) }
        }
    }
    result.add(CallGroup(currentBucket.first(), currentBucket))

    return result
}