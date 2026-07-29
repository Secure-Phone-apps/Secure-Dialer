package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.CallRecord
import com.example.model.CallType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ContactCallSummaryDashboard(
    callRecords: List<CallRecord>,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var selectedRange by remember { mutableStateOf(SummaryTimeRange.TODAY) }
    
    val now = System.currentTimeMillis()
    val startOfWeek = remember(now) { now - 7L * 24 * 60 * 60 * 1000 }
    val startOfMonth = remember(now) { now - 30L * 24 * 60 * 60 * 1000 }
    val startOfYear = remember(now) { now - 365L * 24 * 60 * 60 * 1000 }

    val filteredRecords = remember(callRecords, selectedRange, startOfWeek, startOfMonth, startOfYear) {
        val todayPrefix = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())
        
        when (selectedRange) {
            SummaryTimeRange.TODAY -> {
                callRecords.filter { it.timestamp.startsWith(todayPrefix) }
            }
            SummaryTimeRange.WEEK -> {
                val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                val calCurrent = java.util.Calendar.getInstance()
                val currentYear = calCurrent.get(java.util.Calendar.YEAR)
                val currentMillis = calCurrent.timeInMillis
                callRecords.filter { record ->
                    try {
                        val parsed = sdf.parse(record.timestamp)
                        if (parsed != null) {
                            val calParsed = java.util.Calendar.getInstance().apply { 
                                time = parsed
                                set(java.util.Calendar.YEAR, currentYear)
                            }
                            if (calParsed.timeInMillis > currentMillis) {
                                calParsed.add(java.util.Calendar.YEAR, -1)
                            }
                            calParsed.timeInMillis >= startOfWeek
                        } else false
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            SummaryTimeRange.MONTH -> {
                val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                val calCurrent = java.util.Calendar.getInstance()
                val currentYear = calCurrent.get(java.util.Calendar.YEAR)
                val currentMillis = calCurrent.timeInMillis
                callRecords.filter { record ->
                    try {
                        val parsed = sdf.parse(record.timestamp)
                        if (parsed != null) {
                            val calParsed = java.util.Calendar.getInstance().apply { 
                                time = parsed
                                set(java.util.Calendar.YEAR, currentYear)
                            }
                            if (calParsed.timeInMillis > currentMillis) {
                                calParsed.add(java.util.Calendar.YEAR, -1)
                            }
                            calParsed.timeInMillis >= startOfMonth
                        } else false
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            SummaryTimeRange.YEAR -> {
                val sdf = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault())
                val calCurrent = java.util.Calendar.getInstance()
                val currentYear = calCurrent.get(java.util.Calendar.YEAR)
                val currentMillis = calCurrent.timeInMillis
                callRecords.filter { record ->
                    try {
                        val parsed = sdf.parse(record.timestamp)
                        if (parsed != null) {
                            val calParsed = java.util.Calendar.getInstance().apply { 
                                time = parsed
                                set(java.util.Calendar.YEAR, currentYear)
                            }
                            if (calParsed.timeInMillis > currentMillis) {
                                calParsed.add(java.util.Calendar.YEAR, -1)
                            }
                            calParsed.timeInMillis >= startOfYear
                        } else false
                    } catch (e: Exception) {
                        false
                    }
                }
            }
            SummaryTimeRange.ALL -> callRecords
        }
    }

    val incomingCount = remember(filteredRecords) { filteredRecords.count { it.type == CallType.INCOMING } }
    val outgoingCount = remember(filteredRecords) { filteredRecords.count { it.type == CallType.OUTGOING } }
    val missedCount = remember(filteredRecords) { filteredRecords.count { it.type == CallType.MISSED } }
    
    val totalDurationSecs = remember(filteredRecords) { filteredRecords.sumOf { it.duration.toLong() } }
    val formattedTotalDuration = remember(totalDurationSecs) {
        val hours = totalDurationSecs / 3600
        val mins = (totalDurationSecs % 3600) / 60
        val secs = totalDurationSecs % 60
        when {
            hours > 0 -> "${hours}h ${mins}m"
            mins > 0 -> "${mins}m ${secs}s"
            else -> "${secs}s"
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Call Summary",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                IconButton(
                    onClick = { isExpanded = !isExpanded },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse summary" else "Expand summary",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 10.dp)) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        SummaryTimeRange.entries.forEachIndexed { index, range ->
                            SegmentedButton(
                                selected = selectedRange == range,
                                onClick = { selectedRange = range },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = SummaryTimeRange.entries.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.primary,
                                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                                    inactiveContainerColor = Color.Transparent
                                )
                            ) {
                                Text(
                                    text = range.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (selectedRange == range) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CallReceived,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Incoming",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "$incomingCount",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFE8F5E9)
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.CallMade,
                                        contentDescription = null,
                                        tint = Color(0xFF2E7D32),
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Outgoing",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "$outgoingCount",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF2E7D32)
                                )
                            }
                        }

                        Surface(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                        ) {
                            Column(
                                modifier = Modifier.padding(6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CallMissed,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Missed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = "$missedCount",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Total Talk Time",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = formattedTotalDuration,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
