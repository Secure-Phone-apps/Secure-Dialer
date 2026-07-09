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
                            onDeleteRecord = { onDeleteRecord(record.id) }
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
    onDeleteRecord: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCallClick()
            },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
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
            },
            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
        )
    }
}
