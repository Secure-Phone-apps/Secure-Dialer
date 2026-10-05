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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.getAvatarShape
import com.example.ui.theme.*
import com.example.util.RichHapticEngine

@Composable
fun InCallBottomBar(
    isIncoming: Boolean,
    onAnswer: () -> Unit,
    onHangUp: () -> Unit,
    onToggleQuickDeclineMenu: () -> Unit,
    avatarShapeType: String = "squircle"
) {
    val context = LocalContext.current
    val buttonShape = AppShapes.Keypad

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
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalArrangement = if (isIncoming) Arrangement.SpaceBetween else Arrangement.Center,
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
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                        onAnswer()
                    },
                    interactionSource = answerInteractionSource,
                    color = getCallGreenColor(),
                    contentColor = getOnCallGreenColor(),
                    shape = buttonShape,
                    modifier = Modifier
                        .size(width = 84.dp, height = 64.dp)
                        .scale(answerScale)
                        .testTag("answer_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = stringResource(R.string.btn_answer),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                HangUpActionCallButton(
                    onHangUp = onHangUp,
                    buttonShape = buttonShape,
                    labelResId = R.string.btn_decline,
                    modifier = Modifier.size(width = 84.dp, height = 64.dp)
                )
            } else {
                HangUpActionCallButton(
                    onHangUp = onHangUp,
                    buttonShape = buttonShape,
                    labelResId = R.string.call_status_ended,
                    modifier = Modifier.size(width = 84.dp, height = 64.dp)
                )
            }
        }
    }
}

@Composable
private fun HangUpActionCallButton(
    onHangUp: () -> Unit,
    buttonShape: Shape,
    labelResId: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
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
            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.WARNING)
            onHangUp()
        },
        interactionSource = hangUpInteractionSource,
        color = getDeclineRedColor(),
        contentColor = getOnDeclineRedColor(),
        shape = buttonShape,
        modifier = modifier
            .scale(hangUpScale)
            .testTag("hangup_button")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                contentDescription = stringResource(labelResId),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}