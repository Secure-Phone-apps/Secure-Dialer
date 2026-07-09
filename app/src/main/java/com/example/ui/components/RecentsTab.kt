package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
    primaryText: Color,
    secondaryText: Color,
    activePill: Color,
    brandBlue: Color,
    hasPermission: Boolean = true,
    isLoading: Boolean = false,
    onRequestPermission: () -> Unit = {}
) {
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

        if (callRecordsPaged.itemCount == 0 && callRecordsPaged.loadState.refresh is LoadState.NotLoading) {
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
                            primaryText = primaryText,
                            secondaryText = secondaryText,
                            activePill = activePill
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
    primaryText: Color,
    secondaryText: Color,
    activePill: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable { onCallClick() }
            .padding(12.dp),
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
                text = record.name,
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
                    CallType.MISSED -> "↙" to Color.Red
                    CallType.OUTGOING -> "↗" to Color(0xFF10B981) // Green
                    CallType.INCOMING -> "↔" to Color.Gray
                }
                Text(text = arrow, color = arrowColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    text = "${record.label} • ${record.timestamp}",
                    fontSize = 13.sp,
                    color = secondaryText
                )
            }
        }

        IconButton(onClick = onDeleteRecord) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(activePill)
                .clickable { onCallClick() },
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
