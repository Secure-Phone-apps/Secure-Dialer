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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.ui.theme.getMissedCallColor
import com.example.ui.theme.getDialedCallColor
import com.example.ui.theme.getReceivedCallColor

@Composable
fun DetailHistoryItem(record: CallRecord, onDeleteClick: () -> Unit) {
    val context = LocalContext.current
    val (icon, color) = when (record.type) {
        CallType.MISSED -> Icons.Default.CallMissed to getMissedCallColor()
        CallType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade to getDialedCallColor()
        CallType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived to getReceivedCallColor()
    }
    
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(color)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Surface(
                modifier = Modifier.size(32.dp),
                shape = RoundedCornerShape(16.dp),
                color = color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = color
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 10.dp)
            ) {
                Text(
                    text = record.timestamp,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                val typeStr = when (record.type) {
                    CallType.MISSED -> stringResource(R.string.call_type_missed)
                    CallType.OUTGOING -> stringResource(R.string.call_type_outgoing)
                    CallType.INCOMING -> stringResource(R.string.call_type_incoming)
                }
                
                val durationText = if (record.duration > 0) {
                    val mins = record.duration / 60
                    val secs = record.duration % 60
                    if (mins > 0) "${mins}m ${secs}s" else "${secs}s"
                } else null
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    // SIM Slot Indicator Chip
                    val physicalSimCount = remember(context) { com.example.util.MultiSimManager.getPhysicalSimCount(context) }
                    if (physicalSimCount > 1) {
                        val isSim1 = record.simSlot <= 1
                        if (isSim1) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.primary,
                                shadowElevation = 0.5.dp
                            ) {
                                Text(
                                    text = stringResource(R.string.sim_1),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 0.5.dp)
                                )
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.sim_2),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 0.5.dp)
                                )
                            }
                        }
                    }

                    if (record.isVerified) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 0.5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(9.dp)
                                )
                                Text(
                                    text = stringResource(R.string.caller_verified),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        val isUnsavedWithoutCnap = record.name == record.number || record.name == "Unknown" || record.name.isBlank() || record.name == "-1" || record.name == "-2" || record.name == "-3"
                        if (isUnsavedWithoutCnap) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                border = BorderStroke(
                                    0.5.dp,
                                    MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.unknown),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 0.5.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = typeStr,
                        style = MaterialTheme.typography.bodySmall,
                        color = color,
                        fontWeight = FontWeight.Medium
                    )
                    
                    if (durationText != null) {
                        Text(
                            text = "• $durationText",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (record.type != CallType.MISSED) {
                        Text(
                            text = "• " + stringResource(R.string.no_answer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete call entry",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
