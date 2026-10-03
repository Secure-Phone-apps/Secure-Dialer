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
    avatarShapeType: String = "circular"
) {
    val context = LocalContext.current
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
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                        onAnswer()
                    },
                    interactionSource = answerInteractionSource,
                    color = getCallGreenColor(),
                    contentColor = getOnCallGreenColor(),
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
                            // FIXED: Localized accessibility description
                            contentDescription = stringResource(R.string.btn_answer),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                // FIXED: Lightweight Spacer maintains column alignment without dummy Box overhead
                Spacer(modifier = Modifier.weight(1f))

                HangUpActionCallButton(
                    onHangUp = onHangUp,
                    buttonShape = buttonShape,
                    labelResId = R.string.btn_decline,
                    modifier = Modifier.weight(1f)
                )
            } else {
                // Outgoing/Active: Symmetrical 3-slot layout with centered Hang Up
                Spacer(modifier = Modifier.weight(1f))

                HangUpActionCallButton(
                    onHangUp = onHangUp,
                    buttonShape = buttonShape,
                    labelResId = R.string.call_status_ended,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

// DRY FIX: Unified Hang Up / Decline action button eliminates duplicate styling & animation code
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
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        modifier = modifier
            .height(64.dp)
            .scale(hangUpScale)
            .testTag("hangup_button")
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Default.CallEnd,
                // FIXED: Localized accessibility description
                contentDescription = stringResource(labelResId),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}