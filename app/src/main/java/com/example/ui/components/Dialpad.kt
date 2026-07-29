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
                        items(
                            items = dialpadMatches,
                            key = { it.number },
                            contentType = { "dialpad_match" }
                        ) { match ->
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

        Spacer(modifier = Modifier.height(4.dp))



        // Action Row (Call & Backspace inline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Symmetrical Paste button on the left to keep call button perfectly centered
            Box(
                modifier = Modifier.size(78.dp),
                contentAlignment = Alignment.Center
            ) {
                val hasClipboardText = clipboardManager?.hasPrimaryClip() == true
                IconButton(
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
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentPaste,
                        contentDescription = "Paste number",
                        tint = if (hasClipboardText) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // Central green call button
            val callFabShape = RoundedCornerShape(16.dp)

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



