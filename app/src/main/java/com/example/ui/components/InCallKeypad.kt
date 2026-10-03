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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.CallManager
import com.example.R
import com.example.model.getAvatarShape
import com.example.ui.theme.LocalAmoledMode
import com.example.ui.theme.LocalM3Expressive
import com.example.util.RichHapticEngine

private data class KeypadItem(val digit: String, val letters: String)

private val IN_CALL_KEYPAD_ITEMS = listOf(
    KeypadItem("1", ""),
    KeypadItem("2", "ABC"),
    KeypadItem("3", "DEF"),
    KeypadItem("4", "GHI"),
    KeypadItem("5", "JKL"),
    KeypadItem("6", "MNO"),
    KeypadItem("7", "PQRS"),
    KeypadItem("8", "TUV"),
    KeypadItem("9", "WXYZ"),
    KeypadItem("*", ""),
    KeypadItem("0", "+"),
    KeypadItem("#", "")
)

@Composable
fun InCallKeypad(
    onClose: () -> Unit,
    avatarShapeType: String = "circular"
) {
    var inCallDialpadInput by remember { mutableStateOf("") }
    val isAmoled = LocalAmoledMode.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = MaterialTheme.shapes.large,
        border = if (isAmoled) BorderStroke(1.dp, Color(0xFF222222)) else null,
        colors = CardDefaults.cardColors(
            containerColor = if (isAmoled) Color.Black else MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // Header with overflow protection and one-tap clear
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = inCallDialpadInput.ifEmpty { "In-Call Keypad" },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Start
                )

                if (inCallDialpadInput.isNotEmpty()) {
                    IconButton(
                        onClick = { inCallDialpadInput = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear Input",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (r in 0 until 4) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (c in 0 until 3) {
                            val item = IN_CALL_KEYPAD_ITEMS[r * 3 + c]
                            InCallKeypadButton(
                                key = item.digit,
                                letters = item.letters,
                                onClick = {
                                    inCallDialpadInput += item.digit
                                    CallManager.playDtmf(item.digit[0])
                                },
                                avatarShapeType = avatarShapeType,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(60.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onClose,
                modifier = Modifier.height(44.dp)
            ) {
                Text(
                    text = stringResource(R.string.close),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
fun InCallKeypadButton(
    key: String,
    letters: String = "",
    onClick: () -> Unit,
    avatarShapeType: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isExpressive = LocalM3Expressive.current
    val buttonShape = getAvatarShape(avatarShapeType)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessHigh,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "keypad_button_scale"
    )

    val isAmoled = LocalAmoledMode.current
    val buttonColor = if (isAmoled) {
        Color(0xFF141414)
    } else if (isExpressive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
    } else {
        MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
    }

    Surface(
        onClick = {
            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
            onClick()
        },
        interactionSource = interactionSource,
        modifier = modifier.scale(scale),
        shape = buttonShape,
        color = buttonColor,
        contentColor = MaterialTheme.colorScheme.primary,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Text(
                text = key,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 24.sp
            )
            // FIXED: T9 letter hints enable seamless alphanumeric automated phone tree navigation
            if (letters.isNotBlank()) {
                Text(
                    text = letters,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 9.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    letterSpacing = 1.sp
                )
            }
        }
    }
}