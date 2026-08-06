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

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallMissed
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import java.util.Calendar
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.CallRecord
import com.example.model.CallType
import com.example.model.Contact
import com.example.model.getAvatarShape
import com.example.ui.viewmodel.DialerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CallHistoryDetailsScreen(
    number: String,
    logs: List<CallRecord>,
    viewModel: DialerViewModel,
    onCallClick: (CallRecord) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    val primaryRecord = logs.firstOrNull() ?: return
    
    val blockedNumbersEntities by viewModel.blockedNumbersFlow.collectAsState()
    val isBlocked = remember(blockedNumbersEntities, number) {
        blockedNumbersEntities.any { it.number == number }
    }
    
    val isContact = primaryRecord.name != primaryRecord.number

    var showReminderDialog by remember { mutableStateOf(false) }
    var reminderNoteText by remember { mutableStateOf("") }
    var reminderDelayValue by remember { mutableStateOf("15") }
    var reminderDelayUnit by remember { mutableStateOf("min") } // "sec", "min", "hour", "day"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.call_details),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "More options"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_all_history)) },
                            onClick = {
                                showMenu = false
                                logs.forEach { log -> viewModel.deleteCallLog(log.id) }
                                onBack()
                            },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Delete, 
                                    contentDescription = null, 
                                    tint = MaterialTheme.colorScheme.error
                                ) 
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (isBlocked) stringResource(R.string.unblock_number) else stringResource(R.string.block_number)) },
                            onClick = {
                                showMenu = false
                                if (isBlocked) {
                                    viewModel.removeBlockedNumber(number)
                                } else {
                                    viewModel.addBlockedNumber(number)
                                }
                            },
                            leadingIcon = { 
                                Icon(
                                    imageVector = Icons.Default.Block, 
                                    contentDescription = null
                                ) 
                            }
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val avatarShape = getAvatarShape(viewModel.avatarShapeType.value)
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = avatarShape,
                        color = primaryRecord.avatarBg.copy(alpha = 0.85f),
                        tonalElevation = 2.dp
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (primaryRecord.photoUri.isNotEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(primaryRecord.photoUri)
                                        .size(256, 256)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Contact Photo",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val isSaved = primaryRecord.name != primaryRecord.number && primaryRecord.name != "Unknown" && primaryRecord.name.isNotBlank()
                                if (isSaved) {
                                    Text(
                                        text = primaryRecord.avatarText,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = primaryRecord.avatarTextColor,
                                        fontWeight = FontWeight.Bold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Unsaved Contact Icon",
                                        tint = primaryRecord.avatarTextColor,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = if (primaryRecord.name == "Unknown") stringResource(R.string.unknown) else primaryRecord.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = primaryRecord.number,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                    
                    if (primaryRecord.label.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(100.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            val labelRes = localizeContactLabel(primaryRecord.label)
                            Text(
                                text = labelRes.uppercase(java.util.Locale.ROOT),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
            
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DetailActionItem(
                        icon = Icons.Default.Call,
                        label = stringResource(R.string.action_call),
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onCallClick(primaryRecord)
                        }
                    )

                    DetailActionItem(
                        icon = Icons.Default.Message,
                        label = stringResource(R.string.action_message),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("smsto:$number")
                            }
                            try {
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "No SMS app found", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    DetailActionItem(
                        icon = Icons.Default.Block,
                        label = if (isBlocked) stringResource(R.string.unblock) else stringResource(R.string.block),
                        containerColor = if (isBlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        contentColor = if (isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        onClick = {
                            if (isBlocked) {
                                viewModel.removeBlockedNumber(number)
                            } else {
                                viewModel.addBlockedNumber(number)
                            }
                        }
                    )

                    DetailActionItem(
                        icon = if (isContact) Icons.Default.Person else Icons.Default.PersonAdd,
                        label = if (isContact) stringResource(R.string.edit) else stringResource(R.string.add),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = {
                            if (isContact) {
                                viewModel.oldContactToEdit.value = Contact(
                                    number = primaryRecord.number,
                                    name = primaryRecord.name,
                                    label = primaryRecord.label,
                                    favorite = false,
                                    avatarText = primaryRecord.avatarText,
                                    avatarBgValue = primaryRecord.avatarBgValue,
                                    avatarTextColorValue = primaryRecord.avatarTextColorValue,
                                    email = ""
                                )
                                viewModel.editContactName.value = primaryRecord.name
                                viewModel.editContactNumber.value = primaryRecord.number
                                viewModel.editContactLabel.value = primaryRecord.label
                                viewModel.isEditContactDialogVisible.value = true
                            } else {
                                viewModel.newContactName.value = ""
                                viewModel.newContactNumber.value = primaryRecord.number
                                viewModel.newContactLabel.value = "Mobile"
                                viewModel.isAddContactDialogVisible.value = true
                            }
                        }
                    )
                }
            }
            
            item {
                CallLogSummaryDashboard(callRecords = logs)
            }
            
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showReminderDialog = true }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.schedule_callback_reminder_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = stringResource(R.string.schedule_callback_reminder_sub),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (showReminderDialog) {
                    val finalDelaySeconds = remember(reminderDelayValue, reminderDelayUnit) {
                        val value = reminderDelayValue.toIntOrNull() ?: 0
                        when (reminderDelayUnit) {
                            "sec" -> value
                            "min" -> value * 60
                            "hour" -> value * 3600
                            "day" -> value * 86400
                            else -> value * 60
                        }
                    }

                    AlertDialog(
                        onDismissRequest = { showReminderDialog = false },
                        shape = RoundedCornerShape(24.dp),
                        icon = {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        title = { 
                            Text(
                                text = stringResource(R.string.callback_reminders_title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            ) 
                        },
                        text = {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = stringResource(R.string.remind_call_back_prompt, if (primaryRecord.name == "Unknown") primaryRecord.number else primaryRecord.name),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                
                                OutlinedTextField(
                                    value = reminderDelayValue,
                                    onValueChange = { newValue ->
                                        if (newValue.all { it.isDigit() } && newValue.length <= 4) {
                                            reminderDelayValue = newValue
                                        }
                                    },
                                    label = { Text(stringResource(R.string.duration_label)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("sec", "min", "hour", "day").forEach { unit ->
                                        FilterChip(
                                            selected = reminderDelayUnit == unit,
                                            onClick = { reminderDelayUnit = unit },
                                            label = { 
                                                Text(
                                                    text = unit, 
                                                    style = MaterialTheme.typography.bodySmall,
                                                    modifier = Modifier.fillMaxWidth(),
                                                    textAlign = TextAlign.Center
                                                ) 
                                            },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }

                                OutlinedTextField(
                                    value = reminderNoteText,
                                    onValueChange = { reminderNoteText = it },
                                    label = { Text(stringResource(R.string.optional_note_label)) },
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    val triggerTime = System.currentTimeMillis() + (finalDelaySeconds * 1000L)
                                    viewModel.addReminder(
                                        number = primaryRecord.number,
                                        name = if (primaryRecord.name == "Unknown") primaryRecord.number else primaryRecord.name,
                                        triggerTime = triggerTime,
                                        note = reminderNoteText
                                    )
                                    Toast.makeText(context, "Callback alarm scheduled!", Toast.LENGTH_SHORT).show()
                                    showReminderDialog = false
                                },
                                shape = RoundedCornerShape(100.dp)
                            ) {
                                Text(stringResource(R.string.btn_save))
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showReminderDialog = false }
                            ) {
                                Text(stringResource(R.string.btn_cancel))
                            }
                        }
                    )
                }
            }
            
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.history_timeline),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Text(
                        text = stringResource(R.string.calls_count, logs.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            items(
                items = logs,
                key = { it.id }
            ) { logRecord ->
                DetailHistoryItem(
                    record = logRecord,
                    onDeleteClick = {
                        viewModel.deleteCallLog(logRecord.id)
                    }
                )
            }
            
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        logs.forEach { log -> viewModel.deleteCallLog(log.id) }
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f),
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    ),
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp)
                        .height(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.clear_history_with_number),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

