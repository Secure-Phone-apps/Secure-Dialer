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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.scale
import android.widget.Toast
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.*
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.paging.compose.LazyPagingItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import com.example.ui.theme.LocalM3Expressive

val DIALPAD_KEYS = listOf(
    Triple("1", "", 1),
    Triple("2", "ABC", 2),
    Triple("3", "DEF", 3),
    Triple("4", "GHI", 4),
    Triple("5", "JKL", 5),
    Triple("6", "MNO", 6),
    Triple("7", "PQRS", 7),
    Triple("8", "TUV", 8),
    Triple("9", "WXYZ", 9),
    Triple("*", "", -1),
    Triple("0", "+", 0),
    Triple("#", "", -1)
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialpadTabContent(
    inputValue: String,
    onValueChange: (String) -> Unit,
    onCallClick: (String) -> Unit,
    onSpeedDialCall: (String) -> Unit,
    voicemailNumber: String,
    speedDialMap: Map<Int, String>,
    dialpadMatches: List<DialpadMatch>,
    onCollapseClick: () -> Unit,
    viewModel: com.example.ui.viewmodel.DialerViewModel? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val isExpressive = LocalM3Expressive.current
    val dialKeyColor = if (isExpressive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // T9 Results Preview (Vertical list occupying remaining top space)
        if (inputValue.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                if (dialpadMatches.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            stringResource(R.string.dialpad_no_matches),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(
                            items = dialpadMatches,
                            key = { it.number },
                            contentType = { "dialpad_match" }
                        ) { match ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCallClick(match.number) },
                                colors = CardDefaults.cardColors(containerColor = dialKeyColor),
                                shape = viewModel?.let { getAvatarShape(it.avatarShapeType.value) } ?: MaterialTheme.shapes.medium
                            ) {
                                ListItem(
                                    headlineContent = {
                                        Text(
                                            text = match.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                    },
                                    supportingContent = {
                                        Text(
                                            text = "${localizeContactLabel(match.label)} • ${match.number}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    leadingContent = {
                                        Surface(
                                            modifier = Modifier.size(40.dp),
                                            shape = viewModel?.let { getAvatarShape(it.avatarShapeType.value) } ?: CircleShape,
                                            color = match.avatarBg.copy(alpha = 0.8f)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                if (match.photoUri.isNotEmpty()) {
                                                    AsyncImage(
                                                        model = ImageRequest.Builder(LocalContext.current)
                                                            .data(match.photoUri)
                                                            .size(256, 256)
                                                            .crossfade(true)
                                                            .build(),
                                                        contentDescription = "Contact Photo",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Crop
                                                    )
                                                } else {
                                                    val isSaved = match.name.isNotBlank() && match.name != match.number && match.name != "Unknown"
                                                    if (isSaved) {
                                                        Text(
                                                            text = match.avatarText.ifEmpty { getInitials(match.name) },
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = match.avatarTextColor,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    } else {
                                                        Icon(
                                                            imageVector = Icons.Default.Person,
                                                            contentDescription = "Unsaved Contact Icon",
                                                            tint = match.avatarTextColor,
                                                            modifier = Modifier.size(22.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    },
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                onCallClick(match.number)
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Call,
                                                contentDescription = "Call",
                                                tint = com.example.ui.theme.getCallGreenColor(),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    },
                                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Push dialpad down when there is no input
            Spacer(modifier = Modifier.weight(1f))
        }

        // Elegant Display Screen
        val actionButtonShape = viewModel?.let { getAvatarShape(it.avatarShapeType.value) } ?: RoundedCornerShape(16.dp)
        var expandedClipboardMenu by remember { mutableStateOf(false) }
        val clipboardManager = remember { context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 12.dp),
            shape = actionButtonShape,
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(
                width = 6.dp,
                color = dialKeyColor
            ),
            onClick = {
                expandedClipboardMenu = true
            }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = inputValue.ifEmpty { stringResource(R.string.dialpad_enter_number) },
                    style = if (inputValue.isEmpty()) MaterialTheme.typography.titleMedium else MaterialTheme.typography.headlineLarge,
                    fontWeight = if (inputValue.isEmpty()) FontWeight.Normal else FontWeight.Bold,
                    color = if (inputValue.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                DropdownMenu(
                    expanded = expandedClipboardMenu,
                    onDismissRequest = { expandedClipboardMenu = false }
                ) {
                    val hasClipboardText = clipboardManager?.hasPrimaryClip() == true
                    if (hasClipboardText) {
                        val clipText = clipboardManager?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        val filteredDigits = clipText.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
                        if (filteredDigits.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("${stringResource(R.string.dialpad_paste)}: $filteredDigits") },
                                onClick = {
                                    onValueChange(filteredDigits)
                                    expandedClipboardMenu = false
                                }
                            )
                        }
                    }
                    if (inputValue.isNotEmpty()) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dialpad_copy)) },
                            onClick = {
                                try {
                                    val clip = android.content.ClipData.newPlainText("phone_number", inputValue)
                                    clipboardManager?.setPrimaryClip(clip)
                                    Toast.makeText(context, context.getString(R.string.number_copied), Toast.LENGTH_SHORT).show()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                                expandedClipboardMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.dialpad_clear)) },
                            onClick = {
                                onValueChange("")
                                expandedClipboardMenu = false
                            }
                        )
                    }
                }
            }
        }

        // Dialer Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            for (i in 0 until 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (j in 0 until 3) {
                        val index = i * 3 + j
                        val key = DIALPAD_KEYS[index]
                        DialButton(
                            key = key,
                            inputValue = inputValue,
                            onValueChange = onValueChange,
                            onSpeedDialCall = onSpeedDialCall,
                            speedDialMap = speedDialMap,
                            voicemailNumber = voicemailNumber,
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            viewModel = viewModel
                        )
                    }
                }
            }
        }

        // Action Row (Call & Backspace inline) - Made symmetrically and geometrically same to other dialpad buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Symmetrical Paste button on the left to keep call button perfectly centered
            val hasClipboardText = clipboardManager?.hasPrimaryClip() == true
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    try {
                        val clipText = clipboardManager?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                        val filteredDigits = clipText.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
                        if (filteredDigits.isNotEmpty()) {
                            onValueChange(filteredDigits)
                            Toast.makeText(context, "Pasted: $filteredDigits", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "No valid number in clipboard", Toast.LENGTH_SHORT).show()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
                shape = actionButtonShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                contentColor = if (hasClipboardText) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste number",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Central primary call button matching DialButton shape and size perfectly
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (inputValue.isEmpty()) {
                        val lastNumber = viewModel?.getLastOutgoingNumber() ?: ""
                        if (lastNumber.isNotBlank()) {
                            onValueChange(lastNumber)
                        } else {
                            Toast.makeText(context, "No recent dialed number", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        viewModel?.saveLastOutgoingNumber(inputValue)
                        onCallClick(inputValue)
                    }
                },
                shape = actionButtonShape,
                color = com.example.ui.theme.getCallGreenColor(),
                contentColor = com.example.ui.theme.getOnCallGreenColor(),
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .testTag("dialpad_call_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = "Place call",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Backspace button on the right matching shape and size perfectly
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp),
                contentAlignment = Alignment.Center
            ) {
                if (inputValue.isNotEmpty()) {
                    val backspaceInteractionSource = remember { MutableInteractionSource() }
                    val isBackspacePressed by backspaceInteractionSource.collectIsPressedAsState()
                    val backspaceScale by animateFloatAsState(
                        targetValue = if (isBackspacePressed) 0.92f else 1.0f,
                        animationSpec = spring(
                            stiffness = Spring.StiffnessHigh,
                            dampingRatio = Spring.DampingRatioMediumBouncy
                        ),
                        label = "backspace_button_scale"
                    )

                    Surface(
                        shape = actionButtonShape,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(backspaceScale)
                            .clip(actionButtonShape)
                            .combinedClickable(
                                interactionSource = backspaceInteractionSource,
                                indication = ripple(),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    onValueChange(inputValue.dropLast(1))
                                },
                                onLongClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onValueChange("")
                                }
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Backspace,
                                contentDescription = "Backspace",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}



