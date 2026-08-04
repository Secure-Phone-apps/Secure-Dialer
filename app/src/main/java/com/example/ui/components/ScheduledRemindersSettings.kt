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

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.ui.viewmodel.DialerViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ScheduledRemindersSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val context = LocalContext.current
    val reminders by viewModel.remindersFlow.collectAsState()

    val activeReminders = remember(reminders) { reminders.filter { !it.isCompleted } }
    val completedReminders = remember(reminders) { reminders.filter { it.isCompleted } }

    val sdf = remember { SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Stats Overview Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.callback_reminders_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = stringResource(R.string.callback_reminders_stats, activeReminders.size, completedReminders.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // Active Reminders section
        item {
            Text(
                text = stringResource(R.string.active_reminders_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (activeReminders.isEmpty()) {
            item {
                SettingsEmptyState(
                    icon = Icons.Default.NotificationAdd,
                    title = stringResource(R.string.no_active_reminders_title),
                    description = stringResource(R.string.no_active_reminders_desc),
                    tintColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        } else {
            items(activeReminders, key = { it.id }) { reminder ->
                ListItem(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    headlineContent = { Text(reminder.name, fontWeight = FontWeight.SemiBold) },
                    supportingContent = {
                        Column {
                            Text(reminder.number)
                            Text(
                                text = stringResource(R.string.scheduled_prefix, sdf.format(Date(reminder.reminderTime))),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    },
                    leadingContent = {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = {
                            viewModel.deleteReminder(reminder)
                            Toast.makeText(context, "Cancelled reminder for ${reminder.name}", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.Cancel, stringResource(R.string.btn_cancel), tint = MaterialTheme.colorScheme.error)
                        }
                    }
                )
            }
        }

        // Completed Reminders section
        if (completedReminders.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.history_passed_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            items(completedReminders, key = { it.id }) { reminder ->
                ListItem(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    headlineContent = { Text(reminder.name, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    supportingContent = {
                        Column {
                            Text(reminder.number, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = stringResource(R.string.triggered_prefix, sdf.format(Date(reminder.reminderTime))),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    leadingContent = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.padding(8.dp)
                        )
                    },
                    trailingContent = {
                        IconButton(onClick = {
                            viewModel.deleteReminder(reminder)
                        }) {
                            Icon(Icons.Default.Delete, stringResource(R.string.btn_delete), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                        }
                    }
                )
            }
        }
    }
}
