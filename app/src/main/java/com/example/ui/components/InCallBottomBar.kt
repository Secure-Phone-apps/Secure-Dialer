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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.getAvatarShape

@Composable
fun InCallBottomBar(
    isIncoming: Boolean,
    onAnswer: () -> Unit,
    onHangUp: () -> Unit,
    onToggleQuickDeclineMenu: () -> Unit,
    avatarShapeType: String = "circular"
) {
    val haptic = LocalHapticFeedback.current
    val buttonShape = getAvatarShape(avatarShapeType)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isIncoming) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(onClick = onToggleQuickDeclineMenu) {
                    Text(
                        text = stringResource(R.string.send_quick_response),
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isIncoming) {
                val answerInteractionSource = remember { MutableInteractionSource() }
                val isAnswerPressed by answerInteractionSource.collectIsPressedAsState()
                val answerScale by animateFloatAsState(
                    targetValue = if (isAnswerPressed) 0.92f else 1.0f,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessHigh,
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    ),
                    label = "answer_button_scale"
                )

                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onAnswer()
                    },
                    interactionSource = answerInteractionSource,
                    color = com.example.ui.theme.getCallGreenColor(),
                    contentColor = com.example.ui.theme.getOnCallGreenColor(),
                    shape = buttonShape,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .scale(answerScale)
                        .testTag("answer_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Answer",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // Center spacer matching DialButton dimensions for absolute symmetry
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Empty center space
                }

                val hangUpInteractionSource = remember { MutableInteractionSource() }
                val isHangUpPressed by hangUpInteractionSource.collectIsPressedAsState()
                val hangUpScale by animateFloatAsState(
                    targetValue = if (isHangUpPressed) 0.92f else 1.0f,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessHigh,
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    ),
                    label = "hangup_button_scale"
                )

                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onHangUp()
                    },
                    interactionSource = hangUpInteractionSource,
                    color = com.example.ui.theme.getDeclineRedColor(),
                    contentColor = com.example.ui.theme.getOnDeclineRedColor(),
                    shape = buttonShape,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .scale(hangUpScale)
                        .testTag("hangup_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Hang up",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            } else {
                // Outgoing/Active: Symmetrical 3-slot layout with centered Hang Up
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Empty Left Slot
                }

                val hangUpInteractionSource = remember { MutableInteractionSource() }
                val isHangUpPressed by hangUpInteractionSource.collectIsPressedAsState()
                val hangUpScale by animateFloatAsState(
                    targetValue = if (isHangUpPressed) 0.92f else 1.0f,
                    animationSpec = spring(
                        stiffness = Spring.StiffnessHigh,
                        dampingRatio = Spring.DampingRatioMediumBouncy
                    ),
                    label = "hangup_button_scale"
                )

                Surface(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onHangUp()
                    },
                    interactionSource = hangUpInteractionSource,
                    color = com.example.ui.theme.getDeclineRedColor(),
                    contentColor = com.example.ui.theme.getOnDeclineRedColor(),
                    shape = buttonShape,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp)
                        .scale(hangUpScale)
                        .testTag("hangup_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CallEnd,
                            contentDescription = "Hang up",
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Empty Right Slot
                }
            }
        }
    }
}
