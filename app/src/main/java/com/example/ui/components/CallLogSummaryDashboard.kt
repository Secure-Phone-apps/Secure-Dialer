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

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.ui.theme.LocalM3Expressive
import com.example.ui.theme.getDialedCallColor
import com.example.ui.theme.getMissedCallColor
import com.example.ui.theme.getReceivedCallColor
import com.example.util.RichHapticEngine
import java.text.SimpleDateFormat
import java.util.*

enum class SummaryTimeRange {
    TODAY, WEEK, MONTH, YEAR, ALL
}

@Composable
fun SummaryTimeRange.getLabel(): String {
    return when (this) {
        SummaryTimeRange.TODAY -> stringResource(R.string.summary_range_today)
        SummaryTimeRange.WEEK -> stringResource(R.string.summary_range_week_tab)
        SummaryTimeRange.MONTH -> stringResource(R.string.summary_range_month_tab)
        SummaryTimeRange.YEAR -> stringResource(R.string.summary_range_year_tab)
        SummaryTimeRange.ALL -> stringResource(R.string.summary_range_all_tab)
    }
}

@Composable
fun CallLogSummaryDashboard(
    callRecords: List<CallRecord>,
    dashboardMode: String = "FULL",
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(true) }
    var selectedRange by remember { mutableStateOf(SummaryTimeRange.TODAY) }

    val now = System.currentTimeMillis()
    val startOfWeek = remember(now) { now - 7L * 24 * 60 * 60 * 1000 }
    val startOfMonth = remember(now) { now - 30L * 24 * 60 * 60 * 1000 }
    val startOfYear = remember(now) { now - 365L * 24 * 60 * 60 * 1000 }

    val activeRange = if (dashboardMode == "FULL") selectedRange else SummaryTimeRange.ALL

    val filteredRecords = remember(callRecords, activeRange, startOfWeek, startOfMonth, startOfYear, context) {
        val currentLocale = getCurrentLocale(context)
        val todayPrefix = SimpleDateFormat("MMM d", currentLocale).format(Date())

        when (activeRange) {
            SummaryTimeRange.TODAY -> {
                val calToday = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val todayStartMs = calToday.timeInMillis
                callRecords.filter { record ->
                    if (record.timestampMs != 0L) record.timestampMs >= todayStartMs else record.timestamp.startsWith(todayPrefix)
                }
            }
            SummaryTimeRange.WEEK -> filterByThreshold(callRecords, startOfWeek, currentLocale)
            SummaryTimeRange.MONTH -> filterByThreshold(callRecords, startOfMonth, currentLocale)
            SummaryTimeRange.YEAR -> filterByThreshold(callRecords, startOfYear, currentLocale)
            SummaryTimeRange.ALL -> callRecords
        }
    }

    val totalCallsCount = filteredRecords.size
    val missedCallsCount = remember(filteredRecords) { filteredRecords.count { it.type == CallType.MISSED } }
    val outgoingCallsCount = remember(filteredRecords) { filteredRecords.count { it.type == CallType.OUTGOING } }
    val receivedCallsCount = remember(filteredRecords) { filteredRecords.count { it.type == CallType.INCOMING } }
    val totalDurationSeconds = remember(filteredRecords) { filteredRecords.sumOf { it.duration } }

    val formattedTotalDuration = remember(totalDurationSeconds) {
        val hrs = totalDurationSeconds / 3600
        val mins = (totalDurationSeconds % 3600) / 60
        val secs = totalDurationSeconds % 60
        when {
            hrs > 0 -> "${hrs}h ${mins}m"
            mins > 0 -> "${mins}m"
            else -> "${secs}s"
        }
    }

    val isExpressive = LocalM3Expressive.current
    val cardColor = if (isExpressive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        isExpanded = !isExpanded
                    },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val headerTitle = if (dashboardMode == "COMPACT_FILTERS" || activeRange == SummaryTimeRange.ALL) {
                        stringResource(R.string.today_call_summary)
                    } else if (activeRange == SummaryTimeRange.TODAY) {
                        stringResource(R.string.today_call_summary)
                    } else {
                        stringResource(R.string.call_summary_prefix, activeRange.getLabel())
                    }

                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!isExpanded) {
                        Text(
                            text = stringResource(R.string.summary_missed_and_time, missedCallsCount, formattedTotalDuration),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = stringResource(if (isExpanded) R.string.btn_cancel else R.string.btn_answer),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(4.dp))

                    if (dashboardMode == "FULL") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(2.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            // FIXED: Use .entries instead of .values() to eliminate array allocations
                            SummaryTimeRange.entries.forEach { range ->
                                val isSelected = selectedRange == range
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                        .clickable {
                                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                            selectedRange = range
                                        }
                                        .padding(vertical = 3.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = range.getLabel(),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SummaryBox(
                            value = totalCallsCount.toString(),
                            label = stringResource(R.string.filter_all),
                            icon = Icons.Default.Call,
                            iconColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )

                        SummaryBox(
                            value = missedCallsCount.toString(),
                            label = stringResource(R.string.filter_missed),
                            icon = Icons.Default.CallMissed,
                            iconColor = getMissedCallColor(),
                            modifier = Modifier.weight(1f)
                        )

                        SummaryBox(
                            value = outgoingCallsCount.toString(),
                            label = stringResource(R.string.filter_dialed),
                            icon = Icons.AutoMirrored.Filled.CallMade,
                            iconColor = getDialedCallColor(),
                            modifier = Modifier.weight(1f)
                        )

                        SummaryBox(
                            value = receivedCallsCount.toString(),
                            label = stringResource(R.string.filter_received),
                            icon = Icons.AutoMirrored.Filled.CallReceived,
                            iconColor = getReceivedCallColor(),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (dashboardMode == "FULL") {
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.total_talk_time),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = formattedTotalDuration,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// PERF FIX: Consolidated threshold filtering avoids repeated Date & Calendar allocations in loops
private fun filterByThreshold(records: List<CallRecord>, thresholdMs: Long, locale: Locale): List<CallRecord> {
    val sdf by lazy { SimpleDateFormat("MMM d, HH:mm", locale) }
    val currentYear by lazy { Calendar.getInstance().get(Calendar.YEAR) }

    return records.filter { record ->
        if (record.timestampMs != 0L) {
            record.timestampMs >= thresholdMs
        } else {
            try {
                val parsed = sdf.parse(record.timestamp)
                if (parsed != null) {
                    val cal = Calendar.getInstance().apply {
                        time = parsed
                        set(Calendar.YEAR, currentYear)
                    }
                    if (cal.timeInMillis > System.currentTimeMillis()) {
                        cal.add(Calendar.YEAR, -1)
                    }
                    cal.timeInMillis >= thresholdMs
                } else false
            } catch (_: Exception) {
                false
            }
        }
    }
}

@Composable
fun SummaryBox(
    value: String,
    label: String,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    val isExpressive = LocalM3Expressive.current
    val bgColor = if (isExpressive) {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    } else {
        MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    }

    Column(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(bgColor)
            .padding(vertical = 6.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 20.sp),
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(2.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
    }
}