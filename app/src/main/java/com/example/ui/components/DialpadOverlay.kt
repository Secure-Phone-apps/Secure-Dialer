/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ContactCache
import com.example.R
import com.example.ui.theme.*
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.RichHapticEngine

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialpadOverlay(
    inputValue: String,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit,
    onCallClick: (String) -> Unit,
    onSpeedDialCall: (String) -> Unit,
    speedDialMap: Map<Int, String>,
    voicemailNumber: String,
    viewModel: DialerViewModel? = null,
    onCallWithSim: ((String, String) -> Unit)? = null
) {
    val context = LocalContext.current
    val isExpressive = LocalM3Expressive.current
    val isAmoled = LocalAmoledMode.current

    val dialKeyColor = if (isAmoled) {
        Color(0xFF141414)
    } else if (isExpressive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(AppShapes.BottomSheet),
        color = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surface,
        border = if (isAmoled) BorderStroke(1.dp, Color(0xFF222222)) else null,
        tonalElevation = if (isAmoled) 0.dp else 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(AppShapes.Chip)
                    .background(MaterialTheme.colorScheme.outlineVariant)
                    .clickable {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        onClose()
                    }
            )

            Spacer(modifier = Modifier.height(8.dp))

            val isUnsavedNumber = remember(inputValue) {
                if (inputValue.isBlank()) false
                else viewModel?.isNumberUnsaved(inputValue) ?: (ContactCache.getContact(inputValue) == null)
            }

            if (isUnsavedNumber && inputValue.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        onClick = {
                            if (viewModel?.vibrateOnClickEnabled?.value != false) {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                            }
                            viewModel?.openAddContactWithNumber(inputValue)
                        },
                        shape = AppShapes.Chip,
                        color = dialKeyColor,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("overlay_create_contact_chip")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.dialpad_create_contact), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }

                    Surface(
                        onClick = {
                            if (viewModel?.vibrateOnClickEnabled?.value != false) {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                            }
                            viewModel?.addToExistingPendingNumber?.value = inputValue
                            viewModel?.isAddToExistingSheetVisible?.value = true
                        },
                        shape = AppShapes.Chip,
                        color = dialKeyColor,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("overlay_add_to_existing_chip")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.GroupAdd, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.secondary)
                            Text(stringResource(R.string.dialpad_add_to_existing), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            var expandedOverlayClipboardMenu by remember { mutableStateOf(false) }
            val overlayClipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager }
            val currentTfv = viewModel?.dialpadTextFieldValue?.value ?: TextFieldValue(inputValue, TextRange(inputValue.length))
            val keyboardController = LocalSoftwareKeyboardController.current

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    @Suppress("DEPRECATION")
                    CompositionLocalProvider(LocalTextInputService provides null) {
                        BasicTextField(
                            value = currentTfv,
                            onValueChange = { newTfv ->
                                if (viewModel != null) viewModel.onDialpadTextFieldValueChange(newTfv)
                                else onValueChange(newTfv.text)
                            },
                            textStyle = if (currentTfv.text.isEmpty()) {
                                MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.Normal,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                MaterialTheme.typography.displaySmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                            },
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .onFocusChanged { if (it.isFocused) keyboardController?.hide() }
                                .testTag("dialpad_overlay_number_field"),
                            decorationBox = { innerTextField ->
                                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    if (currentTfv.text.isEmpty()) {
                                        Text(stringResource(R.string.dialpad_enter_number), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), textAlign = TextAlign.Center)
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }

                    DropdownMenu(
                        expanded = expandedOverlayClipboardMenu,
                        onDismissRequest = { expandedOverlayClipboardMenu = false }
                    ) {
                        val hasClipboardText = overlayClipboardManager?.hasPrimaryClip() == true
                        if (hasClipboardText) {
                            val clipText = overlayClipboardManager?.primaryClip?.getItemAt(0)?.text?.toString() ?: ""
                            val filteredDigits = clipText.filter { it.isDigit() || it == '+' || it == '*' || it == '#' }
                            if (filteredDigits.isNotEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("${stringResource(R.string.dialpad_paste)}: $filteredDigits") },
                                    onClick = {
                                        if (viewModel != null) viewModel.insertDialpadDigit(filteredDigits)
                                        else onValueChange(filteredDigits)
                                        expandedOverlayClipboardMenu = false
                                    }
                                )
                            }
                        }
                        if (inputValue.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.dialpad_copy)) },
                                onClick = {
                                    try {
                                        val clip = ClipData.newPlainText("phone_number", inputValue)
                                        overlayClipboardManager?.setPrimaryClip(clip)
                                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                                            Toast.makeText(context, context.getString(R.string.number_copied), Toast.LENGTH_SHORT).show()
                                        }
                                    } catch (_: Exception) {}
                                    expandedOverlayClipboardMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.dialpad_clear)) },
                                onClick = {
                                    if (viewModel != null) viewModel.clearDialpad() else onValueChange("")
                                    expandedOverlayClipboardMenu = false
                                }
                            )
                        }
                    }
                }

                if (inputValue.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(AppShapes.Chip)
                            .combinedClickable(
                                onClick = {
                                    if (viewModel?.vibrateOnClickEnabled?.value != false) RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                    if (viewModel != null) viewModel.backspaceDialpad() else onValueChange(inputValue.dropLast(1))
                                },
                                onLongClick = {
                                    if (viewModel?.vibrateOnClickEnabled?.value != false) RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.WARNING)
                                    if (viewModel != null) viewModel.clearDialpad() else onValueChange("")
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = stringResource(R.string.dialpad_clear), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dialer Grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                for (i in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
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
                                modifier = Modifier.weight(1f),
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val activeSims = remember(context) { com.example.util.MultiSimManager.getActiveSimAccounts(context) }
            val preferredSim = viewModel?.preferredSim?.value ?: "Ask"
            val isDualSim = (preferredSim == "Ask" || activeSims.size > 1) && (activeSims.size >= 2 || com.example.util.MultiSimManager.getPhysicalSimCount(context) >= 2)

            val sim1 = activeSims.getOrNull(0)
            val sim2 = activeSims.getOrNull(1)
            val sim1Label = sim1?.displayName?.takeIf { it.isNotBlank() && !it.startsWith("SIM", ignoreCase = true) }
                ?: sim1?.carrierName?.takeIf { it.isNotBlank() && !it.equals("Carrier", ignoreCase = true) && !it.equals("Mobile Network", ignoreCase = true) }
                ?: "SIM 1"
            val sim2Label = sim2?.displayName?.takeIf { it.isNotBlank() && !it.startsWith("SIM", ignoreCase = true) }
                ?: sim2?.carrierName?.takeIf { it.isNotBlank() && !it.equals("Carrier", ignoreCase = true) && !it.equals("Mobile Network", ignoreCase = true) }
                ?: "SIM 2"

            if (isDualSim) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val call1InteractionSource = remember { MutableInteractionSource() }
                    val isCall1Pressed by call1InteractionSource.collectIsPressedAsState()
                    val call1Scale by animateFloatAsState(
                        targetValue = if (isCall1Pressed) 0.92f else 1.0f,
                        animationSpec = spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "overlay_call_sim1_scale"
                    )

                    Surface(
                        onClick = {
                            if (viewModel?.vibrateOnClickEnabled?.value != false) {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                            }
                            if (onCallWithSim != null) onCallWithSim(inputValue, "SIM 1") else onCallClick(inputValue)
                        },
                        shape = AppShapes.Keypad,
                        color = getCallGreenColor(),
                        contentColor = getOnCallGreenColor(),
                        interactionSource = call1InteractionSource,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .scale(call1Scale)
                            .testTag("dialpad_call_sim1_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sim1Label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    val call2InteractionSource = remember { MutableInteractionSource() }
                    val isCall2Pressed by call2InteractionSource.collectIsPressedAsState()
                    val call2Scale by animateFloatAsState(
                        targetValue = if (isCall2Pressed) 0.92f else 1.0f,
                        animationSpec = spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "overlay_call_sim2_scale"
                    )

                    Surface(
                        onClick = {
                            if (viewModel?.vibrateOnClickEnabled?.value != false) {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                            }
                            if (onCallWithSim != null) onCallWithSim(inputValue, "SIM 2") else onCallClick(inputValue)
                        },
                        shape = AppShapes.Keypad,
                        color = getCallGreenColor().copy(alpha = 0.88f),
                        contentColor = getOnCallGreenColor(),
                        interactionSource = call2InteractionSource,
                        modifier = Modifier
                            .weight(1f)
                            .height(64.dp)
                            .scale(call2Scale)
                            .testTag("dialpad_call_sim2_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = sim2Label,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                val callInteractionSource = remember { MutableInteractionSource() }
                val isCallPressed by callInteractionSource.collectIsPressedAsState()
                val callScale by animateFloatAsState(
                    targetValue = if (isCallPressed) 0.92f else 1.0f,
                    animationSpec = spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "overlay_call_button_scale"
                )

                Surface(
                    onClick = {
                        if (viewModel?.vibrateOnClickEnabled?.value != false) {
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                        }
                        onCallClick(inputValue)
                    },
                    shape = AppShapes.Keypad,
                    color = getCallGreenColor(),
                    contentColor = getOnCallGreenColor(),
                    interactionSource = callInteractionSource,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .scale(callScale)
                        .testTag("dialpad_call_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Call, contentDescription = stringResource(R.string.call_status_ongoing), modifier = Modifier.size(28.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialButton(
    key: Triple<String, String, Int>,
    inputValue: String,
    onValueChange: (String) -> Unit,
    onSpeedDialCall: (String) -> Unit,
    speedDialMap: Map<Int, String>,
    voicemailNumber: String,
    modifier: Modifier = Modifier,
    viewModel: DialerViewModel? = null
) {
    val context = LocalContext.current
    val isExpressive = LocalM3Expressive.current
    val isAmoled = LocalAmoledMode.current

    // FIXED: Keypad buttons are permanently locked to AppShapes.Keypad (RoundedCornerShape 16dp)
    val buttonShape = AppShapes.Keypad

    val buttonColor = if (isAmoled) {
        Color(0xFF141414)
    } else if (isExpressive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh, dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "dial_button_scale"
    )

    Surface(
        modifier = modifier
            .heightIn(min = 52.dp)
            .scale(scale)
            .clip(buttonShape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = ripple(),
                onClick = {
                    if (viewModel?.vibrateOnClickEnabled?.value != false) {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                    }
                    if (viewModel != null) {
                        viewModel.insertDialpadDigit(key.first)
                    } else {
                        onValueChange(inputValue + key.first)
                    }
                },
                onLongClick = {
                    if (viewModel?.vibrateOnClickEnabled?.value != false) {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.HEAVY_CLICK)
                    }
                    if (key.first == "1") {
                        if (voicemailNumber.isNotBlank()) onSpeedDialCall(voicemailNumber)
                        else Toast.makeText(context, context.getString(R.string.voicemail_settings), Toast.LENGTH_SHORT).show()
                    } else if (key.first == "0") {
                        if (viewModel != null) viewModel.insertDialpadDigit("+")
                        else onValueChange(inputValue + "+")
                    } else if (key.third != -1) {
                        val speedNum = speedDialMap[key.third]
                        if (speedNum != null) onSpeedDialCall(speedNum)
                        else Toast.makeText(context, context.getString(R.string.speed_dial), Toast.LENGTH_SHORT).show()
                    }
                }
            )
            .testTag("dialpad_key_${key.first}"),
        shape = buttonShape,
        color = buttonColor,
        tonalElevation = if (isExpressive) 4.dp else 2.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = key.first,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            if (key.second.isNotEmpty()) {
                Text(
                    text = key.second,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}