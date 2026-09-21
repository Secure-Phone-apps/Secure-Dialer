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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.example.util.T9HighlightHelper
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.paging.compose.LazyPagingItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import kotlinx.coroutines.flow.MutableStateFlow

import com.example.ui.theme.LocalM3Expressive
import com.example.ui.theme.LocalAmoledMode
import com.example.util.RichHapticEngine

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
    val isAmoled = LocalAmoledMode.current
    val dialKeyColor = if (isAmoled) {
        Color(0xFF141414)
    } else if (isExpressive) {
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
                        itemsIndexed(
                            items = dialpadMatches,
                            key = { index, match -> if (match.number.isNotBlank()) "${match.number}_$index" else "match_$index" },
                            contentType = { _, _ -> "dialpad_match" }
                        ) { _, match ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCallClick(match.number) },
                                colors = CardDefaults.cardColors(containerColor = dialKeyColor),
                                shape = viewModel?.let { getAvatarShape(it.avatarShapeType.value) } ?: MaterialTheme.shapes.medium
                            ) {
                                ListItem(
                                    headlineContent = {
                                        val primaryColor = MaterialTheme.colorScheme.primary
                                        val onSurfaceColor = MaterialTheme.colorScheme.onSurface
                                        val highlightedName = remember(match.name, inputValue, primaryColor, onSurfaceColor) {
                                            T9HighlightHelper.highlightName(
                                                displayName = match.name,
                                                query = inputValue,
                                                highlightColor = primaryColor,
                                                defaultColor = onSurfaceColor,
                                                highlightFontWeight = FontWeight.SemiBold,
                                                defaultFontWeight = FontWeight.Medium
                                            )
                                        }
                                        Text(
                                            text = highlightedName,
                                            style = MaterialTheme.typography.bodyLarge
                                        )
                                    },
                                    supportingContent = {
                                        val primaryColor = MaterialTheme.colorScheme.primary
                                        val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
                                        val highlightedNumber = remember(match.number, inputValue, primaryColor, onSurfaceVariantColor) {
                                            T9HighlightHelper.highlightNumber(
                                                number = match.number,
                                                query = inputValue,
                                                highlightColor = primaryColor,
                                                defaultColor = onSurfaceVariantColor,
                                                highlightFontWeight = FontWeight.SemiBold,
                                                defaultFontWeight = FontWeight.Normal
                                            )
                                        }
                                        val labelPrefix = localizeContactLabel(match.label)
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "$labelPrefix • ",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = highlightedNumber,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
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
                                                if (viewModel?.vibrateOnClickEnabled?.value != false) {
                                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                                                }
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

        // Quick Action Chips for Unsaved Number (Material 3 Expressive)
        val allContacts by (viewModel?.allContactsFlow ?: MutableStateFlow(emptyList())).collectAsState()
        val isUnsavedNumber = remember(inputValue, allContacts) {
            if (inputValue.isBlank()) false
            else {
                val cleanInput = inputValue.filter { it.isDigit() || it == '+' }
                if (cleanInput.isEmpty()) false
                else {
                    !allContacts.any { contact ->
                        val cleanContactNum = contact.number.filter { it.isDigit() || it == '+' }
                        cleanContactNum == cleanInput || contact.getAllNumbers().any { 
                            it.number.filter { c -> c.isDigit() || c == '+' } == cleanInput 
                        }
                    }
                }
            }
        }

        // Elegant Display Screen with In-Line Cursor Editing & Selection
        val actionButtonShape = viewModel?.let { getAvatarShape(it.avatarShapeType.value) } ?: RoundedCornerShape(16.dp)
        var expandedClipboardMenu by remember { mutableStateOf(false) }
        val clipboardManager = remember { context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager }
        val currentTfv = viewModel?.dialpadTextFieldValue?.value ?: TextFieldValue(inputValue, TextRange(inputValue.length))
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        if (isUnsavedNumber && inputValue.isNotBlank()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // [+ Create Contact] Symmetrical Button
                Surface(
                    onClick = {
                        if (viewModel?.vibrateOnClickEnabled?.value != false) {
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                        }
                        viewModel?.openAddContactWithNumber(inputValue)
                    },
                    shape = actionButtonShape,
                    color = dialKeyColor,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("dialpad_create_contact_chip")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = stringResource(R.string.dialpad_create_contact),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }

                // [Add to Existing] Symmetrical Button
                Surface(
                    onClick = {
                        if (viewModel?.vibrateOnClickEnabled?.value != false) {
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                        }
                        viewModel?.addToExistingPendingNumber?.value = inputValue
                        viewModel?.isAddToExistingSheetVisible?.value = true
                    },
                    shape = actionButtonShape,
                    color = dialKeyColor,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp)
                        .testTag("dialpad_add_to_existing_chip")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.GroupAdd,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = stringResource(R.string.dialpad_add_to_existing),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 12.dp),
            shape = actionButtonShape,
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(
                width = if (isAmoled) 1.5.dp else 6.dp,
                color = if (isAmoled) Color(0xFF242424) else dialKeyColor
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(
                    LocalTextInputService provides null
                ) {
                    BasicTextField(
                        value = currentTfv,
                        onValueChange = { newTfv ->
                            if (viewModel != null) {
                                viewModel.onDialpadTextFieldValueChange(newTfv)
                            } else {
                                onValueChange(newTfv.text)
                            }
                        },
                        textStyle = if (currentTfv.text.isEmpty()) {
                            MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Normal,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                textAlign = TextAlign.Center
                            )
                        } else {
                            MaterialTheme.typography.headlineLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                        },
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    keyboardController?.hide()
                                }
                            }
                            .testTag("dialpad_number_field"),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentTfv.text.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.dialpad_enter_number),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        textAlign = TextAlign.Center
                                    )
                                }
                                innerTextField()
                            }
                        }
                    )
                }

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
                                    if (viewModel != null) {
                                        viewModel.insertDialpadDigit(filteredDigits)
                                    } else {
                                        onValueChange(filteredDigits)
                                    }
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
                                if (viewModel != null) {
                                    viewModel.clearDialpad()
                                } else {
                                    onValueChange("")
                                }
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
            val pasteInteractionSource = remember { MutableInteractionSource() }
            val isPastePressed by pasteInteractionSource.collectIsPressedAsState()
            val pasteScale by animateFloatAsState(
                targetValue = if (isPastePressed) 0.92f else 1.0f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessHigh,
                    dampingRatio = Spring.DampingRatioMediumBouncy
                ),
                label = "paste_button_scale"
            )

            Surface(
                onClick = {
                    if (viewModel?.vibrateOnClickEnabled?.value != false) {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                    }
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
                color = if (isAmoled) Color(0xFF161616) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                contentColor = if (hasClipboardText) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                interactionSource = pasteInteractionSource,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .scale(pasteScale)
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
            val callInteractionSource = remember { MutableInteractionSource() }
            val isCallPressed by callInteractionSource.collectIsPressedAsState()
            val callScale by animateFloatAsState(
                targetValue = if (isCallPressed) 0.92f else 1.0f,
                animationSpec = spring(
                    stiffness = Spring.StiffnessHigh,
                    dampingRatio = Spring.DampingRatioMediumBouncy
                ),
                label = "dialpad_tab_call_button_scale"
            )

            Surface(
                onClick = {
                    if (viewModel?.vibrateOnClickEnabled?.value != false) {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                    }
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
                interactionSource = callInteractionSource,
                modifier = Modifier
                    .weight(1f)
                    .height(60.dp)
                    .scale(callScale)
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
                        color = if (isAmoled) Color(0xFF161616) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                        contentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxSize()
                            .scale(backspaceScale)
                            .clip(actionButtonShape)
                            .combinedClickable(
                                interactionSource = backspaceInteractionSource,
                                indication = ripple(),
                                onClick = {
                                    if (viewModel?.vibrateOnClickEnabled?.value != false) {
                                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                    }
                                    if (viewModel != null) {
                                        viewModel.backspaceDialpad()
                                    } else {
                                        onValueChange(inputValue.dropLast(1))
                                    }
                                },
                                onLongClick = {
                                    if (viewModel?.vibrateOnClickEnabled?.value != false) {
                                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.WARNING)
                                    }
                                    if (viewModel != null) {
                                        viewModel.clearDialpad()
                                    } else {
                                        onValueChange("")
                                    }
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



