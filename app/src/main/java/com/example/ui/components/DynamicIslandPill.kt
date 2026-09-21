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

import android.telecom.Call
import android.telecom.CallAudioState
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.CallManager
import com.example.util.CallAudioRecorder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun DynamicIslandPill(
    callerName: String,
    callerNumber: String,
    callState: Int,
    audioState: CallAudioState?,
    onExpandToFullScreen: () -> Unit,
    onHangUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Physics-based dragging state
    val animOffsetX = remember { Animatable(0f) }
    val animOffsetY = remember { Animatable(0f) }
    var isDragging by remember { mutableStateOf(false) }

    val activeStartTimestamp by CallManager.activeStartTimestamp.collectAsStateWithLifecycle()
    var tickTrigger by remember { mutableLongStateOf(0L) }

    val isRecording by CallAudioRecorder.isRecording.collectAsStateWithLifecycle()

    LaunchedEffect(callState) {
        if (callState == Call.STATE_ACTIVE) {
            while (true) {
                delay(1000)
                tickTrigger++
            }
        }
    }

    val callDurationSec = remember(activeStartTimestamp, tickTrigger, callState) {
        if (callState == Call.STATE_ACTIVE) {
            val start = if (activeStartTimestamp > 0L) activeStartTimestamp else System.currentTimeMillis()
            val durationMs = System.currentTimeMillis() - start
            (durationMs / 1000).coerceAtLeast(0L).toInt()
        } else {
            0
        }
    }

    val formattedDuration = remember(callDurationSec) {
        val mins = callDurationSec / 60
        val secs = callDurationSec % 60
        "%02d:%02d".format(mins, secs)
    }

    val isSpeakerOn = audioState?.route == CallAudioState.ROUTE_SPEAKER
    val isMuted = audioState?.isMuted == true

    val displayName = if (callerName.isNotBlank()) callerName else callerNumber.ifEmpty { "Call" }

    // Equalizer animation transitions
    val infiniteTransition = rememberInfiniteTransition(label = "DynamicIslandEqualizer")
    val bar1Height by infiniteTransition.animateFloat(
        initialValue = 4f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val bar2Height by infiniteTransition.animateFloat(
        initialValue = 14f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(350, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val bar3Height by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 6.dp, start = 12.dp, end = 12.dp, bottom = 4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(animOffsetX.value.roundToInt(), animOffsetY.value.roundToInt()) }
                .pointerInput(isExpanded) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                animOffsetX.snapTo(animOffsetX.value + dragAmount.x)
                                animOffsetY.snapTo((animOffsetY.value + dragAmount.y).coerceIn(-10f, 600f))
                            }
                        },
                        onDragEnd = {
                            isDragging = false
                            val currentX = animOffsetX.value
                            val targetX = when {
                                currentX < -80f -> -130f
                                currentX > 80f -> 130f
                                else -> 0f
                            }
                            coroutineScope.launch {
                                animOffsetX.animateTo(
                                    targetX,
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                            coroutineScope.launch {
                                animOffsetY.animateTo(
                                    0f,
                                    spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessMedium
                                    )
                                )
                            }
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragCancel = {
                            isDragging = false
                            coroutineScope.launch {
                                animOffsetX.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy))
                                animOffsetY.animateTo(0f, spring(dampingRatio = Spring.DampingRatioNoBouncy))
                            }
                        }
                    )
                }
        ) {
            AnimatedContent(
                targetState = isExpanded,
                transitionSpec = {
                    (fadeIn(animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)) +
                            scaleIn(initialScale = 0.85f, animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMedium)))
                        .togetherWith(
                            fadeOut(animationSpec = tween(150)) +
                                    scaleOut(targetScale = 0.9f, animationSpec = tween(150))
                        )
                },
                label = "DynamicIslandContent"
            ) { expanded ->
            if (!expanded) {
                // COMPACT DYNAMIC ISLAND PILL
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = Color(0xFF0F0F12),
                    contentColor = Color.White,
                    border = BorderStroke(1.dp, Color(0x33FFFFFF)),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .wrapContentWidth()
                        .widthIn(min = 200.dp, max = 340.dp)
                        .height(48.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .clickable {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            isExpanded = true
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Left: Icon Indicator (Speaker / Mic / Call)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        when {
                                            isSpeakerOn -> Color(0xFF00E5FF).copy(alpha = 0.25f)
                                            isMuted -> Color(0xFFFF5252).copy(alpha = 0.25f)
                                            else -> Color(0xFF00E676).copy(alpha = 0.25f)
                                        },
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = when {
                                        isSpeakerOn -> Icons.AutoMirrored.Filled.VolumeUp
                                        isMuted -> Icons.Default.MicOff
                                        else -> Icons.Default.Call
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        isSpeakerOn -> Color(0xFF00E5FF)
                                        isMuted -> Color(0xFFFF5252)
                                        else -> Color(0xFF00E676)
                                    },
                                    modifier = Modifier.size(15.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White,
                                modifier = Modifier.widthIn(max = 120.dp)
                            )
                        }

                        // Right: Live Equalizer Bars + Duration Timer
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (callState == Call.STATE_ACTIVE) {
                                // 3 Equalizer Bars
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    modifier = Modifier.height(18.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height(bar1Height.dp)
                                            .background(
                                                if (isSpeakerOn) Color(0xFF00E5FF) else Color(0xFF00E676),
                                                RoundedCornerShape(1.dp)
                                            )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height(bar2Height.dp)
                                            .background(
                                                if (isSpeakerOn) Color(0xFF00E5FF) else Color(0xFF00E676),
                                                RoundedCornerShape(1.dp)
                                            )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .width(2.5.dp)
                                            .height(bar3Height.dp)
                                            .background(
                                                if (isSpeakerOn) Color(0xFF00E5FF) else Color(0xFF00E676),
                                                RoundedCornerShape(1.dp)
                                            )
                                    )
                                }
                            }

                            Text(
                                text = if (callState == Call.STATE_ACTIVE) formattedDuration else "On Hold",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSpeakerOn) Color(0xFF00E5FF) else Color(0xFF00E676)
                            )
                        }
                    }
                }
            } else {
                // EXPANDED DYNAMIC ISLAND CARD
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFF141418),
                    contentColor = Color.White,
                    border = BorderStroke(1.dp, Color(0x44FFFFFF)),
                    shadowElevation = 14.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(24.dp))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Header: Caller details, state badges, close/expand buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .background(
                                            when {
                                                isSpeakerOn -> Color(0xFF00E5FF).copy(alpha = 0.2f)
                                                else -> Color(0xFF00E676).copy(alpha = 0.2f)
                                            },
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isSpeakerOn) Color(0xFF00E5FF) else Color(0xFF00E676),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = displayName,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = Color.White
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = if (callState == Call.STATE_ACTIVE) formattedDuration else "On Hold",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSpeakerOn) Color(0xFF00E5FF) else Color(0xFF00E676)
                                        )
                                        if (isSpeakerOn) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFF00E5FF).copy(alpha = 0.2f)
                                            ) {
                                                Text(
                                                    text = "SPEAKER",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    color = Color(0xFF00E5FF),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        if (isRecording) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = Color(0xFFFF5252).copy(alpha = 0.25f)
                                            ) {
                                                Text(
                                                    text = "REC",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFFF5252),
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Fullscreen & Collapse controls
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onExpandToFullScreen()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.OpenInFull,
                                        contentDescription = "Full in-call screen",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        isExpanded = false
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowUp,
                                        contentDescription = "Collapse",
                                        tint = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        // Quick In-Call Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 1. Mute Action Button
                            DynamicIslandActionButton(
                                icon = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                                label = if (isMuted) "Unmute" else "Mute",
                                isActive = isMuted,
                                activeColor = Color(0xFFFF5252),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    CallManager.setMuted(!isMuted)
                                }
                            )

                            // 2. Speaker Action Button
                            DynamicIslandActionButton(
                                icon = if (isSpeakerOn) Icons.AutoMirrored.Filled.VolumeUp else Icons.Default.VolumeDown,
                                label = "Speaker",
                                isActive = isSpeakerOn,
                                activeColor = Color(0xFF00E5FF),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    CallManager.setSpeaker(!isSpeakerOn)
                                }
                            )

                            // 3. Call Recording Button
                            DynamicIslandActionButton(
                                icon = Icons.Default.FiberManualRecord,
                                label = if (isRecording) "Stop REC" else "Record",
                                isActive = isRecording,
                                activeColor = Color(0xFFFF5252),
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    if (isRecording) {
                                        val result = CallAudioRecorder.stopRecording()
                                        val file = result.file
                                        if (file != null && file.exists() && file.length() > 0L) {
                                            val dur = result.durationSeconds.coerceAtLeast(1L)
                                            val loc = com.example.ui.components.getCurrentLocale(context)
                                            val sdf = java.text.SimpleDateFormat("MMM d, HH:mm", loc)
                                            val num = callerNumber.ifEmpty { "Unknown" }
                                            val rec = com.example.model.CallRecording(
                                                number = num,
                                                name = callerName,
                                                timestamp = sdf.format(java.util.Date()),
                                                duration = dur,
                                                filePath = file.absolutePath
                                            )
                                            // Saved to DB asynchronously
                                            coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                                                try {
                                                    val db = com.example.data.AppDatabase.getDatabase(context)
                                                    db.dialerDao().insertCallRecording(rec)
                                                } catch (e: Exception) {
                                                    e.printStackTrace()
                                                }
                                            }
                                        }
                                    } else {
                                        val num = callerNumber.ifEmpty { "Unknown" }
                                        CallAudioRecorder.startRecording(context, num)
                                    }
                                }
                            )

                            // 4. End Call Button
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFFF1744),
                                contentColor = Color.White,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onHangUp()
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CallEnd,
                                        contentDescription = "Hang Up",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun DynamicIslandActionButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = if (isActive) activeColor else Color(0xFF26262D),
            contentColor = if (isActive) Color.Black else Color.White,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable { onClick() }
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    modifier = Modifier.size(20.dp),
                    tint = if (isActive) Color.Black else Color.White
                )
            }
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = if (isActive) activeColor else Color.White.copy(alpha = 0.7f)
        )
    }
}
