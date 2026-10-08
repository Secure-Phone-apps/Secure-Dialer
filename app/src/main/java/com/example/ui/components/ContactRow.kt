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
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.Contact
import com.example.model.getAvatarShape
import com.example.model.getInitials
import com.example.ui.theme.AppShapes
import com.example.ui.theme.LocalAmoledMode
import com.example.ui.theme.LocalM3Expressive
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.RichHapticEngine

private enum class NumberPickerAction { CALL, MESSAGE }

@Composable
fun ContactRow(
    contact: Contact,
    onCallClick: (Contact) -> Unit,
    onToggleFavorite: (Contact) -> Unit,
    onEditContact: (Contact) -> Unit,
    onDeleteContact: (Contact) -> Unit,
    viewModel: DialerViewModel
) {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }
    var numberPickerAction by remember { mutableStateOf<NumberPickerAction?>(null) }

    val isExpressive = LocalM3Expressive.current
    val isAmoled = LocalAmoledMode.current
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

    val allNumbers = remember(contact) { contact.getAllNumbers() }
    val allEmails = remember(contact) { contact.getAllEmails() }
    val allAddresses = remember(contact) { contact.getAllAddresses() }

    val accountBadgeText = remember(contact.accountName, contact.accountType) {
        when {
            contact.accountType.equals("com.google", ignoreCase = true) -> {
                if (contact.accountName.isNotBlank()) "Google • ${contact.accountName}" else "Google"
            }
            contact.accountType.contains("sim", ignoreCase = true) -> "SIM"
            contact.accountName.isNotBlank() && contact.accountName != "Phone" -> contact.accountName
            contact.accountType.isNotBlank() && !contact.accountType.contains("local") -> contact.accountType.substringAfterLast('.')
            else -> ""
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                isExpanded = !isExpanded
            },
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (isAmoled) BorderStroke(1.dp, Color(0xFF1C1C1C)) else null,
        shape = AppShapes.Card
    ) {
        Column {
            ListItem(
                modifier = Modifier.padding(vertical = 0.dp),
                headlineContent = {
                    Column(
                        modifier = Modifier.offset(x = (-8).dp),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "${localizeContactLabel(contact.label)} • ${contact.number}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (allNumbers.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .clip(AppShapes.Chip)
                                        .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                                        .padding(horizontal = 6.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        text = "+${allNumbers.size - 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                            }
                        }

                        if (accountBadgeText.isNotBlank()) {
                            Text(
                                text = accountBadgeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                fontWeight = FontWeight.Medium
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
                            .clip(AppShapes.Avatar)
                            .background(contact.avatarBg),
                        contentAlignment = Alignment.Center
                    ) {
                        if (contact.photoUri.isNotEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(contact.photoUri)
                                    .size(128, 128)
                                    .crossfade(false)
                                    .build(),
                                contentDescription = contact.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            val isSaved = contact.name.isNotBlank() && contact.name != contact.number && contact.name != "Unknown"
                            if (isSaved) {
                                Text(
                                    text = contact.avatarText.ifEmpty { getInitials(contact.name) },
                                    style = MaterialTheme.typography.titleMedium,
                                    color = contact.avatarTextColor,
                                    fontWeight = FontWeight.SemiBold
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = contact.avatarTextColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                },
                trailingContent = {
                    IconButton(onClick = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        onToggleFavorite(contact)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = stringResource(R.string.tab_favorites),
                            tint = if (contact.favorite) Color(0xFFEAB308) else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    }
                },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent)
            )

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    val primaryNumber = allNumbers.firstOrNull()?.number ?: contact.number
                    val primaryLabel = allNumbers.firstOrNull()?.label ?: contact.label

                    // 1. Phone numbers section placed FIRST (Above action buttons)
                    allNumbers.forEach { labeledNum ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppShapes.Chip)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                                        onCallClick(contact.copy(number = labeledNum.number, label = labeledNum.label))
                                    }
                            ) {
                                Text(
                                    text = localizeContactLabel(labeledNum.label),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = labeledNum.number,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            IconButton(
                                onClick = {
                                    try {
                                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                        val clip = ClipData.newPlainText("Phone Number", labeledNum.number)
                                        clipboard?.setPrimaryClip(clip)
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                            Toast.makeText(context, context.getString(R.string.toast_number_copied), Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = stringResource(R.string.dialpad_copy),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                    }

                    // 2. Emails section (if any)
                    allEmails.forEach { emailItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppShapes.Chip)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${emailItem.label} Email",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = emailItem.email,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            IconButton(
                                onClick = {
                                    try {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("mailto:${emailItem.email}")
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, context.getString(R.string.error_open_messages), Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = emailItem.email,
                                    tint = MaterialTheme.colorScheme.tertiary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // 3. Physical Addresses section (if any)
                    allAddresses.forEach { addrItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(AppShapes.Chip)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${addrItem.label} Address",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = addrItem.address,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Normal
                                )
                            }

                            IconButton(
                                onClick = {
                                    try {
                                        val uri = Uri.parse("geo:0,0?q=${Uri.encode(addrItem.address)}")
                                        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {}
                                },
                                modifier = Modifier.size(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = addrItem.address,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    // 4. Hero Quick-Action Row (Call, Message, Edit, Delete placed LOWER than numbers)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 2.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DetailActionItem(
                            icon = Icons.Default.Call,
                            label = stringResource(R.string.action_call),
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            onClick = {
                                if (allNumbers.size > 1) {
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                    numberPickerAction = NumberPickerAction.CALL
                                } else {
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                                    onCallClick(contact.copy(number = primaryNumber, label = primaryLabel))
                                }
                            }
                        )

                        DetailActionItem(
                            icon = Icons.Default.Message,
                            label = stringResource(R.string.action_message),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            onClick = {
                                if (allNumbers.size > 1) {
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                    numberPickerAction = NumberPickerAction.MESSAGE
                                } else {
                                    try {
                                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                                            data = Uri.parse("smsto:$primaryNumber")
                                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                        }
                                        context.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(context, context.getString(R.string.error_open_messages), Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        )

                        DetailActionItem(
                            icon = Icons.Default.Edit,
                            label = stringResource(R.string.btn_edit),
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            onClick = {
                                onEditContact(contact)
                            }
                        )

                        DetailActionItem(
                            icon = Icons.Default.Delete,
                            label = stringResource(R.string.btn_delete),
                            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f),
                            contentColor = MaterialTheme.colorScheme.error,
                            onClick = {
                                onDeleteContact(contact)
                            }
                        )
                    }
                }
            }
        }
    }

    if (numberPickerAction != null) {
        val currentAction = numberPickerAction
        AlertDialog(
            onDismissRequest = { numberPickerAction = null },
            shape = AppShapes.Dialog,
            icon = {
                Icon(
                    imageVector = if (currentAction == NumberPickerAction.CALL) Icons.Default.Call else Icons.Default.Message,
                    contentDescription = null,
                    tint = if (currentAction == NumberPickerAction.CALL) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )
            },
            title = {
                Text(
                    text = stringResource(
                        if (currentAction == NumberPickerAction.CALL) R.string.dialog_call_contact else R.string.dialog_message_contact,
                        contact.name
                    ),
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
                        text = stringResource(
                            if (currentAction == NumberPickerAction.CALL) R.string.select_number_to_call else R.string.select_number_to_message
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    allNumbers.forEach { labeledNum ->
                        Surface(
                            shape = AppShapes.Card,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val isCall = currentAction == NumberPickerAction.CALL
                                    numberPickerAction = null
                                    if (isCall) {
                                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                                        onCallClick(contact.copy(number = labeledNum.number, label = labeledNum.label))
                                    } else {
                                        try {
                                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                                data = Uri.parse("smsto:${labeledNum.number}")
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Exception) {
                                            Toast.makeText(context, context.getString(R.string.error_open_messages), Toast.LENGTH_SHORT).show()
                                        }
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
                                Surface(
                                    shape = AppShapes.Keypad,
                                    color = if (currentAction == NumberPickerAction.CALL) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (currentAction == NumberPickerAction.CALL) Icons.Default.Call else Icons.Default.Message,
                                            contentDescription = null,
                                            tint = if (currentAction == NumberPickerAction.CALL) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
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
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { numberPickerAction = null }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}

@Composable
fun ContactActionItem(
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