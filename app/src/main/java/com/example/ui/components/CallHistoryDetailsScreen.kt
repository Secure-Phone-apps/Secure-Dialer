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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.CallRecord
import com.example.model.Contact
import com.example.model.getAvatarShape
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.RichHapticEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallHistoryDetailsScreen(
    number: String,
    logs: List<CallRecord>,
    viewModel: DialerViewModel,
    onCallClick: (CallRecord) -> Unit,
    onBack: () -> Unit
) {
    BackHandler(enabled = true, onBack = onBack)

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val primaryRecord = logs.firstOrNull() ?: return

    val blockedNumbersEntities by viewModel.blockedNumbersFlow.collectAsStateWithLifecycle()
    val isBlocked = remember(blockedNumbersEntities, number) {
        blockedNumbersEntities.any { it.number == number }
    }

    val isContact = primaryRecord.name != primaryRecord.number

    var showNoteDialog by remember { mutableStateOf(false) }
    var editingNoteText by remember { mutableStateOf("") }
    var showReminderDialog by remember { mutableStateOf(false) }

    fun deleteAllLogsWithNumber() {
        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.WARNING)
        coroutineScope.launch(Dispatchers.IO) {
            logs.forEach { log -> viewModel.deleteCallLog(log.id) }
        }
        onBack()
    }

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
                    IconButton(onClick = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        onBack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_cancel)
                        )
                    }
                },
                actions = {
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null
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
                                deleteAllLogsWithNumber()
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
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
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
                                    contentDescription = primaryRecord.name,
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
                                        contentDescription = null,
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
                            Text(
                                text = localizeContactLabel(primaryRecord.label).uppercase(Locale.ROOT),
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
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                            onCallClick(primaryRecord)
                        }
                    )

                    DetailActionItem(
                        icon = Icons.Default.Message,
                        label = stringResource(R.string.action_message),
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("smsto:$number")
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(context, context.getString(R.string.error_open_messages), Toast.LENGTH_SHORT).show()
                            }
                        }
                    )

                    DetailActionItem(
                        icon = Icons.Default.Block,
                        label = if (isBlocked) stringResource(R.string.unblock) else stringResource(R.string.block),
                        containerColor = if (isBlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                        contentColor = if (isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        onClick = {
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                            if (isBlocked) {
                                viewModel.removeBlockedNumber(number)
                            } else {
                                viewModel.addBlockedNumber(number)
                            }
                        }
                    )

                    DetailActionItem(
                        icon = if (isContact) Icons.Default.Person else Icons.Default.PersonAdd,
                        label = if (isContact) stringResource(R.string.btn_edit) else stringResource(R.string.action_add_contact),
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        onClick = {
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
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

            if (viewModel.isCallNotesEnabled.value) {
                item {
                    val numberNotesFlow = remember(number) { viewModel.getCallNotesForNumberFlow(number) }
                    val numberNotes by numberNotesFlow.collectAsStateWithLifecycle(initialValue = emptyList())
                    var showNotesListModal by remember { mutableStateOf(false) }
                    val noteSdf = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.25f),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        if (numberNotes.isNotEmpty()) {
                            val latestNote = numberNotes.first()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                        showNotesListModal = true
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = latestNote.note,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )
                                        if (numberNotes.size > 1) {
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.tertiaryContainer
                                            ) {
                                                Text(
                                                    text = stringResource(R.string.call_notes_count_badge, numberNotes.size),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = noteSdf.format(Date(latestNote.lastUpdated)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                        editingNoteText = ""
                                        showNoteDialog = true
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = stringResource(R.string.add_call_note_btn),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                        editingNoteText = ""
                                        showNoteDialog = true
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Description,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = stringResource(R.string.add_call_note_btn),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    if (showNoteDialog) {
                        InCallNoteDialog(
                            initialNote = editingNoteText,
                            onDismiss = { showNoteDialog = false },
                            onSaveNote = { newNote ->
                                viewModel.saveCallNote(number, newNote)
                            }
                        )
                    }

                    if (showNotesListModal && numberNotes.isNotEmpty()) {
                        AlertDialog(
                            onDismissRequest = { showNotesListModal = false },
                            title = { Text(stringResource(R.string.call_notes_count_title, numberNotes.size)) },
                            text = {
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.heightIn(max = 300.dp)
                                ) {
                                    items(numberNotes, key = { it.id }) { itemNote ->
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = noteSdf.format(Date(itemNote.lastUpdated)),
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    IconButton(
                                                        onClick = {
                                                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.WARNING)
                                                            viewModel.deleteCallNoteById(itemNote.id)
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Delete,
                                                            contentDescription = stringResource(R.string.btn_delete),
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = itemNote.note,
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showNotesListModal = false }) {
                                    Text(stringResource(R.string.close))
                                }
                            }
                        )
                    }
                }
            }

            if (viewModel.isCallbackRemindersEnabled.value) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                    showReminderDialog = true
                                }
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
                        CallbackReminderDialog(
                            context = context,
                            primaryRecord = primaryRecord,
                            viewModel = viewModel,
                            onDismiss = { showReminderDialog = false }
                        )
                    }
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
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.WARNING)
                        viewModel.deleteCallLog(logRecord.id)
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = { deleteAllLogsWithNumber() },
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