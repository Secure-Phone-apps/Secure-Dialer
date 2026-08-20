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
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.model.CallType
import com.example.model.Contact
import com.example.model.getAvatarShape
import com.example.ui.theme.LocalM3Expressive
import com.example.ui.theme.getMissedCallColor
import com.example.ui.theme.getDialedCallColor
import com.example.ui.theme.getReceivedCallColor
import com.example.ui.viewmodel.DialerViewModel

@Composable
fun RecentCallRow(
    group: CallGroup,
    onCallClick: () -> Unit,
    onDeleteRecord: (Int) -> Unit,
    getHistory: suspend (String) -> List<com.example.model.CallRecord>,
    viewModel: DialerViewModel,
    onHistoryClick: (String) -> Unit
) {
    val record = group.primary
    val haptic = LocalHapticFeedback.current
    var isExpanded by remember { mutableStateOf(false) }

    val isExpressive = LocalM3Expressive.current
    val searchBarColor = if (isExpressive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    }
    val containerColor = if (isExpanded) {
        searchBarColor.copy(alpha = minOf(1f, searchBarColor.alpha + 0.15f))
    } else {
        searchBarColor
    }

    val context = LocalContext.current
    val isRowSwipeEnabled by viewModel.isRowSwipeEnabled

    val rowCardContent = @Composable {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    isExpanded = !isExpanded
                },
            colors = CardDefaults.cardColors(
                containerColor = containerColor
            ),
            shape = MaterialTheme.shapes.medium
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
                                text = if (record.name == "Unknown" || record.name.isBlank() || record.name == "-1" || record.name == "-2" || record.name == "-3") stringResource(R.string.unknown) else record.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                                lineHeight = 18.sp,
                                color = if (record.type == CallType.MISSED) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false)
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

                            // SIM Slot Indicator Chip
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
                                        modifier = Modifier.padding(horizontal = 4.5.dp, vertical = 1.dp)
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

                            // Verified Chip (for CNAP / CNAM network-identified names)
                            if (record.isVerified) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
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
                    val avatarShape = getAvatarShape(viewModel.avatarShapeType.value)
                    Surface(
                        modifier = Modifier
                            .offset(x = (-8).dp)
                            .size(40.dp),
                        shape = avatarShape,
                        color = record.avatarBg.copy(alpha = 0.8f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (record.photoUri.isNotEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(record.photoUri)
                                        .size(256, 256)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Contact Photo",
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
                                        contentDescription = "Unsaved Contact Icon",
                                        tint = record.avatarTextColor,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                trailingContent = {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCallClick()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            val context = LocalContext.current
            val blockedNumbersEntities by viewModel.blockedNumbersFlow.collectAsState()
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

                        // 1. Send SMS
                        RecentActionItem(
                            icon = Icons.Default.Message,
                            label = stringResource(R.string.send_sms),
                            tint = MaterialTheme.colorScheme.primary,
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO).apply {
                                    data = Uri.parse("smsto:${record.number}")
                                }
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "No SMS app found", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )

                        // 2. Block/Spam
                        RecentActionItem(
                            icon = Icons.Default.Block,
                            label = if (isBlocked) stringResource(R.string.unblock) else stringResource(R.string.block),
                            tint = if (isBlocked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                            onClick = {
                                if (isBlocked) {
                                    viewModel.removeBlockedNumber(record.number)
                                } else {
                                    viewModel.addBlockedNumber(record.number)
                                }
                            }
                        )

                        // 3. History
                        RecentActionItem(
                            icon = Icons.Default.History,
                            label = stringResource(R.string.history),
                            tint = MaterialTheme.colorScheme.secondary,
                            onClick = {
                                onHistoryClick(record.number)
                            }
                        )

                        // 4. Add/Edit Contact
                        RecentActionItem(
                            icon = if (isContact) Icons.Default.Person else Icons.Default.PersonAdd,
                            label = if (isContact) stringResource(R.string.edit) else stringResource(R.string.add),
                            tint = MaterialTheme.colorScheme.tertiary,
                            onClick = {
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
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onCallClick()
                        false
                    }
                    SwipeToDismissBoxValue.EndToStart -> {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        try {
                            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${record.number}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Messaging app not available", Toast.LENGTH_SHORT).show()
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
                    SwipeToDismissBoxValue.StartToEnd -> com.example.ui.theme.getCallGreenColor()
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
            .clip(MaterialTheme.shapes.small)
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
            color = tint
        )
    }
}
