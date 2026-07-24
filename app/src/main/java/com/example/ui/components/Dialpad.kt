package com.example.ui.components

import android.widget.Toast
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
import com.example.model.Contact
import com.example.model.DialpadMatch
import androidx.compose.ui.res.stringResource
import com.example.R
import androidx.paging.compose.LazyPagingItems
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

import com.example.ui.theme.LocalM3Expressive

private val DIALPAD_KEYS = listOf(
    Triple("1", "", 1),
    Triple("2", "", 2),
    Triple("3", "", 3),
    Triple("4", "", 4),
    Triple("5", "", 5),
    Triple("6", "", 6),
    Triple("7", "", 7),
    Triple("8", "", 8),
    Triple("9", "", 9),
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
    onCollapseClick: () -> Unit
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // T9 Results Preview (Vertical list occupying remaining top space)
        if (inputValue.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 4.dp)
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
                        items(dialpadMatches) { match ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onCallClick(match.number) },
                                colors = CardDefaults.cardColors(containerColor = Color.Transparent)
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
                                            text = "${match.label} • ${match.number}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    leadingContent = {
                                        Surface(
                                            modifier = Modifier.size(40.dp),
                                            shape = CircleShape,
                                            color = match.avatarBg.copy(alpha = 0.8f)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Text(
                                                    text = if (match.name.length >= 2) match.name.substring(0, 2).uppercase() else match.name.take(1).uppercase(),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = match.avatarTextColor,
                                                    fontWeight = FontWeight.SemiBold
                                                )
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
                                                tint = MaterialTheme.colorScheme.primary,
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
        var expandedClipboardMenu by remember { mutableStateOf(false) }
        val clipboardManager = remember { context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .padding(horizontal = 16.dp)
                .clickable {
                    expandedClipboardMenu = true
                },
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
                                Toast.makeText(context, "Number copied", Toast.LENGTH_SHORT).show()
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

        Spacer(modifier = Modifier.height(4.dp))

        // Dialer Grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            for (i in 0 until 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                                .height(70.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Action Row (Call & Backspace inline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Symmetrical space to keep call button perfectly centered
            Spacer(modifier = Modifier.size(78.dp))

            // Central green call button
            val isExpressive = LocalM3Expressive.current
            val callFabShape = if (isExpressive) MaterialTheme.shapes.large else CircleShape

            LargeFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCallClick(inputValue)
                },
                shape = callFabShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(78.dp)
                    .testTag("dialpad_call_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Place call",
                    modifier = Modifier.size(30.dp)
                )
            }

            // Backspace button on the right
            Box(
                modifier = Modifier.size(78.dp),
                contentAlignment = Alignment.Center
            ) {
                if (inputValue.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onValueChange(inputValue.dropLast(1))
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Backspace,
                            contentDescription = "Backspace",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
    }
}

@Composable
fun FloatingDialpadButton(
    onClick: () -> Unit
) {
    val shape = if (LocalM3Expressive.current) MaterialTheme.shapes.large else MaterialTheme.shapes.medium
    FloatingActionButton(
        onClick = onClick,
        shape = shape,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        modifier = Modifier.testTag("dialpad_fab")
    ) {
        Icon(
            imageVector = Icons.Default.Call,
            contentDescription = "Open Dialpad",
            modifier = Modifier.size(24.dp)
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialpadOverlay(
    inputValue: String,
    onValueChange: (String) -> Unit,
    onClose: () -> Unit,
    onCallClick: (String) -> Unit,
    onSpeedDialCall: (String) -> Unit,
    speedDialMap: Map<Int, String>,
    voicemailNumber: String
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                RoundedCornerShape(
                    topStart = if (LocalM3Expressive.current) 40.dp else 28.dp,
                    topEnd = if (LocalM3Expressive.current) 40.dp else 28.dp
                )
            ),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Swipe down handle
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.outlineVariant)
                    .padding(vertical = 12.dp)
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClose()
                    }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Input Display Screen
            var expandedOverlayClipboardMenu by remember { mutableStateOf(false) }
            val overlayClipboardManager = remember { context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as? android.content.ClipboardManager }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { expandedOverlayClipboardMenu = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = inputValue.ifEmpty { "Enter Number" },
                        style = MaterialTheme.typography.displaySmall,
                        color = if (inputValue.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )

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
                                    text = { Text("Paste: $filteredDigits") },
                                    onClick = {
                                        onValueChange(filteredDigits)
                                        expandedOverlayClipboardMenu = false
                                    }
                                )
                            }
                        }
                        if (inputValue.isNotEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Copy Number") },
                                onClick = {
                                    try {
                                        val clip = android.content.ClipData.newPlainText("phone_number", inputValue)
                                        overlayClipboardManager?.setPrimaryClip(clip)
                                        Toast.makeText(context, "Number copied", Toast.LENGTH_SHORT).show()
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    expandedOverlayClipboardMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Clear") },
                                onClick = {
                                    onValueChange("")
                                    expandedOverlayClipboardMenu = false
                                }
                            )
                        }
                    }
                }

                if (inputValue.isNotEmpty()) {
                    IconButton(onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onValueChange(inputValue.dropLast(1))
                    }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Backspace",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

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
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Call Action Button
            val isOverlayExpressive = LocalM3Expressive.current
            val overlayFabShape = if (isOverlayExpressive) MaterialTheme.shapes.large else CircleShape

            LargeFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCallClick(inputValue)
                },
                shape = overlayFabShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(78.dp)
                    .testTag("dialpad_call_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Place call",
                    modifier = Modifier.size(30.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DialButton(
    key: Triple<String, String, Int>,
    inputValue: String,
    onValueChange: (String) -> Unit,
    onSpeedDialCall: (String) -> Unit,
    speedDialMap: Map<Int, String>,
    voicemailNumber: String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val isExpressive = LocalM3Expressive.current
    val buttonShape = if (isExpressive) MaterialTheme.shapes.medium else CircleShape
    val buttonColor = if (isExpressive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f)
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    }

    Surface(
        modifier = modifier
            .size(if (modifier == Modifier) 64.dp else Dp.Unspecified)
            .heightIn(min = 52.dp)
            .clip(buttonShape)
            .combinedClickable(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onValueChange(inputValue + key.first)
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (key.first == "1") {
                        if (voicemailNumber.isNotBlank()) {
                            Toast.makeText(context, "📞 Calling Voicemail", Toast.LENGTH_SHORT).show()
                            onSpeedDialCall(voicemailNumber)
                        } else {
                            Toast.makeText(context, "Voicemail number not set. Configure in Settings!", Toast.LENGTH_SHORT).show()
                        }
                    } else if (key.first == "0") {
                        onValueChange(inputValue + "+")
                    } else if (key.third != -1) {
                        val speedNum = speedDialMap[key.third]
                        if (speedNum != null) {
                            Toast.makeText(context, "📞 Calling Speed Dial mapped to key ${key.first}!", Toast.LENGTH_SHORT).show()
                            onSpeedDialCall(speedNum)
                        } else {
                            Toast.makeText(context, "Speed dial not assigned for key ${key.first}. Assign in Settings!", Toast.LENGTH_SHORT).show()
                        }
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
