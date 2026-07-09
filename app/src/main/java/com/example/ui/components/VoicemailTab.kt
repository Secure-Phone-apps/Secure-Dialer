package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallRecord

@Composable
fun VoicemailTabContent(
    voicemailRecords: List<CallRecord>,
    onPlayClick: (CallRecord) -> Unit,
    primaryText: Color,
    secondaryText: Color,
    activePill: Color,
    navBg: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Voicemail",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = secondaryText,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )

        if (voicemailRecords.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📼", fontSize = 48.sp, modifier = Modifier.padding(bottom = 8.dp))
                    Text("No voicemails", fontWeight = FontWeight.Medium, color = secondaryText)
                    Text("You are all caught up", fontSize = 12.sp, color = secondaryText)
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(voicemailRecords, key = { it.id }) { record ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = navBg)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(record.avatarBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(record.avatarText, fontWeight = FontWeight.Bold, color = record.avatarTextColor)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(record.name, fontWeight = FontWeight.Medium, color = primaryText)
                                    Text("${record.timestamp} • 0:24 mins", fontSize = 12.sp, color = secondaryText)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(activePill)
                                        .clickable { onPlayClick(record) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("▶️", fontSize = 16.sp)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Transcript: \"Hey, just calling to catch up. Let me know when you're free this evening! Standard billing rates may apply.\"",
                                fontSize = 12.sp,
                                color = secondaryText,
                                lineHeight = 16.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}
