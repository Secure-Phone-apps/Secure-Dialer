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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.Contact
import androidx.paging.compose.LazyPagingItems

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DialpadTabContent(
    inputValue: String,
    onValueChange: (String) -> Unit,
    onCallClick: (String) -> Unit,
    onSpeedDialCall: (String) -> Unit,
    voicemailNumber: String,
    speedDialMap: Map<Int, String>,
    contactsPaged: LazyPagingItems<Contact>
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // T9 Results Preview
        if (inputValue.isNotEmpty()) {
            Text(
                text = "T9 Search",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Start).padding(start = 8.dp, top = 8.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (contactsPaged.itemCount == 0) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "No matches",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    for (i in 0 until minOf(contactsPaged.itemCount, 3)) {
                        val contact = contactsPaged[i]
                        if (contact != null) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onCallClick(contact.number) }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(contact.avatarBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        contact.avatarText,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = contact.avatarTextColor
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = contact.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Text(
                text = "Dial Pad",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(bottom = 4.dp, start = 4.dp, top = 8.dp)
            )
        }

        // Elegant Display Screen
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = inputValue.ifEmpty { "Enter Number" },
                style = MaterialTheme.typography.displayMedium,
                color = if (inputValue.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Dialer Grid
        val keys = listOf(
            Triple("1", "Voicemail", 1),
            Triple("2", "A B C", 2),
            Triple("3", "D E F", 3),
            Triple("4", "G H I", 4),
            Triple("5", "J K L", 5),
            Triple("6", "M N O", 6),
            Triple("7", "P Q R S", 7),
            Triple("8", "T U V", 8),
            Triple("9", "W X Y Z", 9),
            Triple("*", "", -1),
            Triple("0", "+", 0),
            Triple("#", "", -1)
        )

        Column(
            modifier = Modifier.width(280.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (i in 0 until 4) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (j in 0 until 3) {
                        val index = i * 3 + j
                        val key = keys[index]
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                .combinedClickable(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onValueChange(inputValue + key.first)
                                    },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (key.third != -1) {
                                            val speedNum = speedDialMap[key.third]
                                            if (speedNum != null) {
                                                Toast
                                                    .makeText(
                                                        context,
                                                        "📞 Calling Speed Dial mapped to key ${key.first}!",
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                                onSpeedDialCall(speedNum)
                                            } else {
                                                Toast
                                                    .makeText(
                                                        context,
                                                        "Speed dial not assigned for key ${key.first}. Assign in Settings!",
                                                        Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                            }
                                        }
                                    }
                                )
                                .testTag("dialpad_key_${key.first}"),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = key.first,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (key.second.isNotEmpty()) {
                                    Text(
                                        text = key.second,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Action Row (Call & Backspace inline)
        Row(
            modifier = Modifier
                .width(280.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Empty box for symmetry on the left
            Box(modifier = Modifier.size(60.dp))

            // Central green call button
            LargeFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCallClick(inputValue)
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(80.dp)
                    .testTag("dialpad_call_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Place call",
                    modifier = Modifier.size(32.dp)
                )
            }

            // Backspace button on the right
            Box(
                modifier = Modifier.size(60.dp),
                contentAlignment = Alignment.Center
            ) {
                if (inputValue.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onValueChange(inputValue.dropLast(1))
                        }
                    ) {
                        Text(
                            text = "⌫",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun FloatingDialpadButton(
    onClick: () -> Unit
) {
    FloatingActionButton(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
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
    speedDialMap: Map<Int, String>
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
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
                    .clickable { onClose() }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Input Display Screen
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = inputValue.ifEmpty { "Enter Number" },
                    style = MaterialTheme.typography.displaySmall,
                    color = if (inputValue.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )

                if (inputValue.isNotEmpty()) {
                    IconButton(onClick = { onValueChange(inputValue.dropLast(1)) }) {
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
            val keys = listOf(
                Triple("1", "Voicemail", 1),
                Triple("2", "A B C", 2),
                Triple("3", "D E F", 3),
                Triple("4", "G H I", 4),
                Triple("5", "J K L", 5),
                Triple("6", "M N O", 6),
                Triple("7", "P Q R S", 7),
                Triple("8", "T U V", 8),
                Triple("9", "W X Y Z", 9),
                Triple("*", "", -1),
                Triple("0", "+", 0),
                Triple("#", "", -1)
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (i in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        for (j in 0 until 3) {
                            val index = i * 3 + j
                            val key = keys[index]
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(64.dp)
                                    .clip(RoundedCornerShape(32.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                                    .combinedClickable(
                                        onClick = { onValueChange(inputValue + key.first) },
                                        onLongClick = {
                                            if (key.first == "1") {
                                                Toast
                                                    .makeText(context, "📞 Calling Voicemail", Toast.LENGTH_SHORT)
                                                    .show()
                                            } else if (key.third != -1) {
                                                val speedNum = speedDialMap[key.third]
                                                if (speedNum != null) {
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            "📞 Calling Speed Dial mapped to key ${key.first}!",
                                                            Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                                    onSpeedDialCall(speedNum)
                                                } else {
                                                    Toast
                                                        .makeText(
                                                            context,
                                                            "Speed dial not assigned for key ${key.first}. Assign in Settings!",
                                                            Toast.LENGTH_SHORT
                                                        )
                                                        .show()
                                                }
                                            }
                                        }
                                    )
                                    .testTag("dialpad_key_${key.first}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = key.first,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    if (key.second.isNotEmpty()) {
                                        Text(
                                            text = key.second,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Call Action Button
            LargeFloatingActionButton(
                onClick = { onCallClick(inputValue) },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .size(72.dp)
                    .testTag("dialpad_call_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Call,
                    contentDescription = "Place call",
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
