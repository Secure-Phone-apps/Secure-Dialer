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

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.CallRecord
import com.example.model.CallRecording
import com.example.model.CallType
import com.example.model.Contact
import com.example.model.LabeledNumber
import com.example.model.getAvatarShape
import com.example.ui.theme.*
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.MultiSimManager
import com.example.util.RichHapticEngine


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RecentCallRow(
    group: CallGroup,
    onCallClick: () -> Unit,
    onDeleteRecord: (Int) -> Unit,
    getHistory: suspend (String) -> List<CallRecord>,
    viewModel: DialerViewModel,
    onHistoryClick: (String) -> Unit,
    onCallRecord: ((CallRecord) -> Unit)? = null
) {
    val record = group.primary
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    val isExpressive = LocalM3Expressive.current
    val isAmoled = LocalAmoledMode.current

    // SQUIRCLE ARCHITECTURE: Strictly enforce AppShapes.Avatar (percent = 32)
    val avatarShape = AppShapes.Avatar

    val searchBarColor = if (isAmoled) {
        Color(0xFF0C0C0C)
    } else if (isExpressive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    }

    val containerColor = if (isExpanded) {
        if (isAmoled) Color(0xFF141414) else searchBarColor.copy(alpha = minOf(1f, searchBarColor.alpha + 0.15f))
    } else {
        searchBarColor
    }

    val isRowSwipeEnabled by viewModel.isRowSwipeEnabled
    val allRecordings by viewModel.recordingsFlow.collectAsStateWithLifecycle()
    val allContacts by viewModel.allContactsFlow.collectAsStateWithLifecycle()

    val matchedContact = remember(allContacts, record.number, record.name) {
        val digits = record.number.filter { it.isDigit() }
        allContacts.firstOrNull { contact ->
            val cAllNums = contact.getAllNumbers()
            val numMatch = cAllNums.any { ln ->
                val cDigits = ln.number.filter { it.isDigit() }
                (digits.isNotEmpty() && cDigits.isNotEmpty() && (digits == cDigits || digits.endsWith(cDigits.takeLast(7)) || cDigits.endsWith(digits.takeLast(7)))) ||
                (ln.number == record.number)
            }
            numMatch || (record.name.isNotBlank() && record.name != "Unknown" && contact.name.equals(record.name, ignoreCase = true))
        }
    }

    val availableNumbers = remember(matchedContact, record.number, record.label) {
        val nums = matchedContact?.getAllNumbers()
        if (!nums.isNullOrEmpty()) {
            nums.distinctBy { it.number.filter { c -> c.isDigit() }.ifEmpty { it.number } }
        } else {
            listOf(LabeledNumber(number = record.number, label = record.label.ifBlank { "Mobile" }, isPrimary = true))
        }
    }

    var showCallerActionsDialog by remember { mutableStateOf(false) }

    val matchingRecordings = remember(allRecordings, record.number) {
        val digits = record.number.filter { it.isDigit() }
        allRecordings.filter { rec ->
            val recDigits = rec.number.filter { it.isDigit() }
            (digits.isNotEmpty() && recDigits.isNotEmpty() && (digits.endsWith(recDigits) || recDigits.endsWith(digits))) ||
            (rec.number == record.number) ||
            (record.name.isNotEmpty() && record.name != "Unknown" && rec.name.equals(record.name, ignoreCase = true))
        }
    }
    var showPlaybackDialog by remember { mutableStateOf(false) }

    val rowCardContent = @Composable {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        isExpanded = !isExpanded
                    },
                    onLongClick = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.HEAVY_CLICK)
                        showCallerActionsDialog = true
                    }
                ),
            colors = CardDefaults.cardColors(containerColor = containerColor),
            border = if (isAmoled) BorderStroke(1.dp, Color(0xFF1C1C1C)) else null,
            shape = AppShapes.Card
        ) {
            Column {
                ListItem(
                    headlineContent = {
                        Column(
                            modifier = Modifier.offset(x = (-8).dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (record.name == "Unknown" || record.name.isBlank() || record.name in listOf("-1", "-2", "-3")) {
                                        stringResource(R.string.unknown)
                                    } else {
                                        com.example.formatContactName(record.name, viewModel.contactNameFormat.value)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 18.sp,
                                    color = if (record.type == CallType.MISSED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f, fill = false),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (group.calls.size > 1) {
                                    Text(
                                        text = " (${group.calls.size})",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                val (icon, iconColor) = when (record.type) {
                                    CallType.MISSED -> Icons.Default.CallMissed to getMissedCallColor()
                                    CallType.OUTGOING -> Icons.AutoMirrored.Filled.CallMade to getDialedCallColor()
                                    CallType.INCOMING -> Icons.AutoMirrored.Filled.CallReceived to getReceivedCallColor()
                                }
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(13.dp)
                                )

                                val physicalSimCount = remember(context) { MultiSimManager.getPhysicalSimCount(context) }
                                if (physicalSimCount > 1) {
                                    val isSim1 = record.simSlot <= 1
                                    Surface(
                                        shape = AppShapes.Chip,
                                        color = if (isSim1) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                                        border = if (!isSim1) BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)) else null
                                    ) {
                                        Text(
                                            text = stringResource(if (isSim1) R.string.sim_1 else R.string.sim_2),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontSize = 8.5.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSim1) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 0.5.dp)
                                        )
                                    }
                                }

                                if (record.isVerified) {
                                    Surface(
                                        shape = AppShapes.Chip,
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.caller_verified),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                if (matchingRecordings.isNotEmpty()) {
                                    Surface(
                                        shape = AppShapes.Chip,
                                        color = MaterialTheme.colorScheme.tertiaryContainer,
                                        modifier = Modifier.clickable {
                                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                            showPlaybackDialog = true
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Mic,
                                                contentDescription = stringResource(R.string.recording),
                                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Text(
                                                text = "Rec",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontSize = 8.5.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onTertiaryContainer
                                            )
                                        }
                                    }
                                }

                                val extraInfo = when {
                                    record.isVerified && record.number.isNotBlank() -> "${record.number} • "
                                    record.label.isNotBlank() && !record.label.equals("Mobile", ignoreCase = true) -> "${localizeContactLabel(record.label)} • "
                                    else -> ""
                                }
                                Text(
                                    text = "$extraInfo${record.timestamp}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    supportingContent = null,
                    leadingContent = {
                        Box(
                            modifier = Modifier
                                .offset(x = (-8).dp)
                                .size(42.dp)
                                .clip(avatarShape)
                                .background(record.avatarBg.copy(alpha = 0.85f)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (record.photoUri.isNotEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(record.photoUri)
                                        .size(128, 128)
                                        .crossfade(false)
                                        .build(),
                                    contentDescription = record.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val isSaved = record.name != record.number && record.name != "Unknown" && record.name.isNotBlank()
                                if (isSaved) {
                                    Text(
                                        text = record.avatarText,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = record.avatarTextColor,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = record.avatarTextColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    },
                    trailingContent = {
                        IconButton(onClick = {
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                            onCallClick()
                        }) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = stringResource(R.string.call_status_ongoing),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )

                val blockedNumbersEntities by viewModel.blockedNumbersFlow.collectAsStateWithLifecycle()
                val isBlocked = remember(blockedNumbersEntities, record.number) {
                    blockedNumbersEntities.any { it.number == record.number }
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isContact = record.name != record.number

                            DetailActionItem(
                                icon = Icons.Default.Chat,
                                label = "WhatsApp",
                                containerColor = Color(0xFF25D366).copy(alpha = 0.20f),
                                contentColor = Color(0xFF1EBE5D),
                                onClick = {
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                                    com.example.util.WhatsAppHelper.openWhatsAppChat(context, record.number)
                                }
                            )

                            DetailActionItem(
                                icon = Icons.Default.Message,
                                label = stringResource(R.string.send_sms),
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("smsto:${record.number}")
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
                                containerColor = if (isBlocked) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                                contentColor = if (isBlocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error,
                                onClick = {
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                    if (isBlocked) {
                                        viewModel.removeBlockedNumber(record.number)
                                    } else {
                                        viewModel.addBlockedNumber(record.number)
                                    }
                                }
                            )

                            DetailActionItem(
                                icon = Icons.Default.History,
                                label = stringResource(R.string.history),
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                onClick = {
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                    onHistoryClick(record.number)
                                }
                            )

                            DetailActionItem(
                                icon = if (isContact) Icons.Default.Edit else Icons.Default.PersonAdd,
                                label = if (isContact) stringResource(R.string.btn_edit) else stringResource(R.string.action_add_contact),
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                onClick = {
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                    if (isContact) {
                                        viewModel.oldContactToEdit.value = Contact(
                                            number = record.number,
                                            name = record.name,
                                            label = record.label,
                                            favorite = false,
                                            avatarText = record.avatarText,
                                            avatarBgValue = record.avatarBgValue,
                                            avatarTextColorValue = record.avatarTextColorValue,
                                            email = ""
                                        )
                                        viewModel.editContactName.value = record.name
                                        viewModel.editContactNumber.value = record.number
                                        viewModel.editContactLabel.value = record.label
                                        viewModel.isEditContactDialogVisible.value = true
                                    } else {
                                        viewModel.newContactName.value = ""
                                        viewModel.newContactNumber.value = record.number
                                        viewModel.newContactLabel.value = "Mobile"
                                        viewModel.isAddContactDialogVisible.value = true
                                    }
                                }
                            )

                            if (matchingRecordings.isNotEmpty()) {
                                DetailActionItem(
                                    icon = Icons.Default.Mic,
                                    label = stringResource(R.string.recording),
                                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    onClick = {
                                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                        showPlaybackDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (isRowSwipeEnabled) {
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { dismissValue ->
                when (dismissValue) {
                    SwipeToDismissBoxValue.StartToEnd -> {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                        onCallClick()
                        false
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${record.number}")).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                            }
                            context.startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(context, context.getString(R.string.error_open_messages), Toast.LENGTH_SHORT).show()
                        }
                        false
                    }
                    else -> false
                }
            }
        )

        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {
                val direction = dismissState.dismissDirection
                val bgContainerColor = when (direction) {
                    SwipeToDismissBoxValue.StartToEnd -> getCallGreenColor()
                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.primaryContainer
                    else -> Color.Transparent
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.medium)
                        .background(bgContainerColor)
                        .padding(horizontal = 20.dp),
                    contentAlignment = if (direction == SwipeToDismissBoxValue.StartToEnd) Alignment.CenterStart else Alignment.CenterEnd
                ) {
                    if (direction == SwipeToDismissBoxValue.StartToEnd) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Call, contentDescription = stringResource(R.string.action_swipe_call), tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.action_swipe_call), color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    } else if (direction == SwipeToDismissBoxValue.EndToStart) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.action_swipe_message), color = MaterialTheme.colorScheme.onPrimaryContainer, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.Email, contentDescription = stringResource(R.string.action_swipe_message), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                    }
                }
            }
        ) {
            rowCardContent()
        }
    } else {
        rowCardContent()
    }

    if (showPlaybackDialog && matchingRecordings.isNotEmpty()) {
        DirectRecordingPlayerDialog(
            recordings = matchingRecordings,
            onDismiss = { showPlaybackDialog = false },
            viewModel = viewModel
        )
    }

    if (showCallerActionsDialog) {
        val isSavedContact = (matchedContact != null) || (record.name != record.number && record.name.isNotBlank() && record.name != "Unknown" && record.name !in listOf("-1", "-2", "-3"))
        val displayName = if (matchedContact != null && matchedContact.name.isNotBlank()) {
            matchedContact.name
        } else if (record.name.isNotBlank() && record.name != "Unknown" && record.name !in listOf("-1", "-2", "-3")) {
            com.example.formatContactName(record.name, viewModel.contactNameFormat.value)
        } else {
            record.number
        }

        if (availableNumbers.size > 1) {
            AlertDialog(
                onDismissRequest = { showCallerActionsDialog = false },
                shape = AppShapes.Dialog,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.dialog_call_contact, displayName),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.select_number_to_call),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        availableNumbers.forEach { labeledNum ->
                            Surface(
                                shape = AppShapes.Card,
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showCallerActionsDialog = false
                                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                                        val targetRecord = record.copy(number = labeledNum.number, label = labeledNum.label)
                                        if (onCallRecord != null) {
                                            onCallRecord(targetRecord)
                                        } else {
                                            onCallClick()
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Surface(
                                        shape = AppShapes.Keypad,
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.Call,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = localizeContactLabel(labeledNum.label),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = labeledNum.number,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            showCallerActionsDialog = false
                                            try {
                                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                    data = Uri.parse("smsto:${labeledNum.number}")
                                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {
                                                Toast.makeText(context, context.getString(R.string.error_open_messages), Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Message,
                                            contentDescription = stringResource(R.string.send_sms),
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            showCallerActionsDialog = false
                                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            val clip = ClipData.newPlainText("Phone Number", labeledNum.number)
                                            clipboard?.setPrimaryClip(clip)
                                            Toast.makeText(context, context.getString(R.string.toast_number_copied), Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = stringResource(R.string.action_copy_number),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    if (isSavedContact) {
                        TextButton(
                            onClick = {
                                showCallerActionsDialog = false
                                if (matchedContact != null) {
                                    viewModel.oldContactToEdit.value = matchedContact
                                    viewModel.editContactName.value = matchedContact.name
                                    viewModel.editContactNumber.value = matchedContact.number
                                    viewModel.editContactLabel.value = matchedContact.label
                                    viewModel.isEditContactDialogVisible.value = true
                                } else {
                                    viewModel.oldContactToEdit.value = Contact(
                                        number = record.number,
                                        name = record.name,
                                        label = record.label,
                                        favorite = false,
                                        avatarText = record.avatarText,
                                        avatarBgValue = record.avatarBgValue,
                                        avatarTextColorValue = record.avatarTextColorValue,
                                        email = ""
                                    )
                                    viewModel.editContactName.value = record.name
                                    viewModel.editContactNumber.value = record.number
                                    viewModel.editContactLabel.value = record.label
                                    viewModel.isEditContactDialogVisible.value = true
                                }
                            }
                        ) {
                            Text(stringResource(R.string.btn_edit))
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCallerActionsDialog = false }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        } else {
            AlertDialog(
                onDismissRequest = { showCallerActionsDialog = false },
                shape = AppShapes.Dialog,
                icon = {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (displayName != record.number) {
                            Text(
                                text = record.number,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Call Action
                        Surface(
                            shape = AppShapes.Card,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCallerActionsDialog = false
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                                    if (onCallRecord != null) {
                                        onCallRecord(record)
                                    } else {
                                        onCallClick()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = stringResource(R.string.action_call),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Send SMS Action
                        Surface(
                            shape = AppShapes.Card,
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCallerActionsDialog = false
                                    try {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("smsto:${record.number}")
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, context.getString(R.string.error_open_messages), Toast.LENGTH_SHORT).show()
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.Message, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                Text(
                                    text = stringResource(R.string.send_sms),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Copy Number Action
                        Surface(
                            shape = AppShapes.Card,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCallerActionsDialog = false
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    val clip = ClipData.newPlainText("Phone Number", record.number)
                                    clipboard?.setPrimaryClip(clip)
                                    Toast.makeText(context, context.getString(R.string.toast_number_copied), Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = stringResource(R.string.action_copy_number),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        // Edit / Add Contact Action
                        Surface(
                            shape = AppShapes.Card,
                            color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCallerActionsDialog = false
                                    if (isSavedContact) {
                                        if (matchedContact != null) {
                                            viewModel.oldContactToEdit.value = matchedContact
                                            viewModel.editContactName.value = matchedContact.name
                                            viewModel.editContactNumber.value = matchedContact.number
                                            viewModel.editContactLabel.value = matchedContact.label
                                            viewModel.isEditContactDialogVisible.value = true
                                        } else {
                                            viewModel.oldContactToEdit.value = Contact(
                                                number = record.number,
                                                name = record.name,
                                                label = record.label,
                                                favorite = false,
                                                avatarText = record.avatarText,
                                                avatarBgValue = record.avatarBgValue,
                                                avatarTextColorValue = record.avatarTextColorValue,
                                                email = ""
                                            )
                                            viewModel.editContactName.value = record.name
                                            viewModel.editContactNumber.value = record.number
                                            viewModel.editContactLabel.value = record.label
                                            viewModel.isEditContactDialogVisible.value = true
                                        }
                                    } else {
                                        viewModel.newContactName.value = ""
                                        viewModel.newContactNumber.value = record.number
                                        viewModel.newContactLabel.value = "Mobile"
                                        viewModel.isAddContactDialogVisible.value = true
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = if (isSavedContact) Icons.Default.Edit else Icons.Default.PersonAdd,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = if (isSavedContact) stringResource(R.string.btn_edit) else stringResource(R.string.action_add_contact),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showCallerActionsDialog = false }) {
                        Text(stringResource(R.string.btn_cancel))
                    }
                }
            )
        }
    }
}

@Composable
fun RecentActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(AppShapes.Small)
            .clickable(onClick = onClick)
            .padding(8.dp)
            .width(64.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}