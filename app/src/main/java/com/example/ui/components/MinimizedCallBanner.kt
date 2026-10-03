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
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.CallManager
import com.example.R
import com.example.ui.theme.getCallGreenColor
import com.example.ui.theme.getDeclineRedColor
import com.example.util.RichHapticEngine
import kotlinx.coroutines.delay

@Composable
fun MinimizedCallBanner(
    contactName: String,
    contactNumber: String,
    callState: Int,
    onExpand: () -> Unit,
    onHangUp: () -> Unit
) {
    val context = LocalContext.current
    val activeStartTimestamp by CallManager.activeStartTimestamp.collectAsStateWithLifecycle()
    var tickTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(callState) {
        if (callState == Call.STATE_ACTIVE) {
            while (true) {
                delay(1000)
                tickTrigger++
            }
        }
    }

    val callDuration = remember(activeStartTimestamp, tickTrigger, callState) {
        if (callState == Call.STATE_ACTIVE) {
            val start = if (activeStartTimestamp > 0L) activeStartTimestamp else System.currentTimeMillis()
            ((System.currentTimeMillis() - start) / 1000).coerceAtLeast(0L).toInt()
        } else {
            0
        }
    }

    val formattedTime = remember(callDuration) {
        val mins = callDuration / 60
        val secs = callDuration % 60
        "%02d:%02d".format(mins, secs)
    }

    // FIXED: Fully localized telephony status strings
    val statusText = when (callState) {
        Call.STATE_RINGING -> stringResource(R.string.call_status_ringing)
        Call.STATE_DIALING -> stringResource(R.string.call_status_dialing)
        Call.STATE_CONNECTING -> stringResource(R.string.call_status_connecting)
        Call.STATE_HOLDING -> stringResource(R.string.call_status_hold)
        else -> formattedTime
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable {
                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                onExpand()
            },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                BannerPulseIndicator(callState = callState)

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = contactName.ifEmpty { contactNumber },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        onExpand()
                    },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = stringResource(R.string.btn_answer),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.WARNING)
                        onHangUp()
                    },
                    modifier = Modifier.size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = stringResource(R.string.call_status_ended),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// PERF FIX: Isolates 60fps/120fps infinite pulse animation from triggering whole-banner recomposition
@Composable
private fun BannerPulseIndicator(callState: Int) {
    val indicatorColor = if (callState == Call.STATE_RINGING) getDeclineRedColor() else getCallGreenColor()

    val infiniteTransition = rememberInfiniteTransition(label = "banner_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "banner_scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "banner_alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale, alpha = alpha)
                .background(color = indicatorColor, shape = CircleShape)
        )
        Icon(
            imageVector = Icons.Default.Call,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = indicatorColor
        )
    }
}