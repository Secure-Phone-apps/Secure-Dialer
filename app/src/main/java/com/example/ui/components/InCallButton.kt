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

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalAmoledMode
import com.example.ui.theme.LocalM3Expressive
import com.example.util.RichHapticEngine

@Composable
fun InCallButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.medium,
    isPulsingRecording: Boolean = false
) {
    val context = LocalContext.current
    val isExpressive = LocalM3Expressive.current
    val isAmoled = LocalAmoledMode.current

    val btnColor = if (isActive) {
        if (isPulsingRecording) {
            Color(0xFFB71C1C).copy(alpha = 0.22f)
        } else {
            MaterialTheme.colorScheme.primaryContainer
        }
    } else {
        if (isAmoled) {
            Color(0xFF141414)
        } else if (isExpressive) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
        }
    }

    val contentColor = if (isActive) {
        if (isPulsingRecording) {
            Color(0xFFE53935)
        } else {
            MaterialTheme.colorScheme.onPrimaryContainer
        }
    } else {
        if (isAmoled) {
            Color(0xFFE0E0E0)
        } else if (isExpressive) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1.0f,
        animationSpec = spring(
            stiffness = Spring.StiffnessHigh,
            dampingRatio = Spring.DampingRatioMediumBouncy
        ),
        label = "incall_button_scale"
    )

    val finalModifier = if (modifier == Modifier) Modifier.size(64.dp) else modifier

    Surface(
        onClick = {
            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
            onClick()
        },
        interactionSource = interactionSource,
        modifier = finalModifier.scale(scale),
        shape = shape,
        color = btnColor,
        contentColor = contentColor,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(contentAlignment = Alignment.Center) {
                // PERF FIX: Only runs the 60fps/120fps infinite ticker when recording is actively pulsing
                if (isPulsingRecording) {
                    RecordingPulseHalo()
                }
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                if (isPulsingRecording) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE53935))
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                }
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun RecordingPulseHalo() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_recording_transition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_recording_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_recording_alpha"
    )

    Box(
        modifier = Modifier
            .size(24.dp)
            .scale(pulseScale)
            .clip(CircleShape)
            .background(Color(0xFFE53935).copy(alpha = pulseAlpha))
    )
}