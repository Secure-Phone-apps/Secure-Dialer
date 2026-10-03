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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.R
import com.example.model.Contact
import com.example.model.getAvatarShape
import com.example.model.getInitials
import com.example.ui.theme.LocalAmoledMode
import com.example.ui.theme.LocalM3Expressive
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.RichHapticEngine

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
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            ListItem(
                headlineContent = {
                    Column(
                        modifier = Modifier.offset(x = (-8).dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text(
                            text = contact.name,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            lineHeight = 18.sp
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${localizeContactLabel(contact.label)} • ${contact.number}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (allNumbers.size > 1) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = "+${allNumbers.size - 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
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
                    val avatarShape = getAvatarShape(viewModel.avatarShapeType.value)
                    Surface(
                        modifier = Modifier
                            .offset(x = (-8).dp)
                            .size(42.dp),
                        shape = avatarShape,
                        color = contact.avatarBg
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (contact.photoUri.isNotEmpty()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(contact.photoUri)
                                        .size(128, 128)
                                        .crossfade(true)
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

                    // Phone numbers section
                    allNumbers.forEach { labeledNum ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
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
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                    IconButton(
                                        onClick = {
                                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                                            onCallClick(contact.copy(number = labeledNum.number, label = labeledNum.label))
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Call,
                                            contentDescription = stringResource(R.string.call_status_ongoing),
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
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
                                            imageVector = Icons.Default.Email,
                                            contentDescription = stringResource(R.string.sms_sent),
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            try {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                                val clip = ClipData.newPlainText("Phone Number", labeledNum.number)
                                                clipboard?.setPrimaryClip(clip)
                                                // FIXED: Suppress redundant Toast on Android 13+ which has native system clipboard preview
                                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                                    Toast.makeText(context, context.getString(R.string.toast_number_copied), Toast.LENGTH_SHORT).show()
                                                }
                                            } catch (_: Exception) {}
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = stringResource(R.string.dialpad_copy),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Emails section
                    allEmails.forEach { emailItem ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Email,
                                        contentDescription = emailItem.email,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Physical Addresses section
                    allAddresses.forEach { addrItem ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = addrItem.address,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Action buttons (Edit & Delete)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                onEditContact(contact)
                            },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_edit))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(
                            onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.WARNING)
                                onDeleteContact(contact)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(stringResource(R.string.btn_delete))
                        }
                    }
                }
            }
        }
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