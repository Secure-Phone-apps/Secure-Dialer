/*
 * Copyright (C) 2026 MovStore
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.CallManager
import com.example.R
import com.example.model.PostCallData
import com.example.model.getAvatarShape
import com.example.model.getInitials
import com.example.ui.theme.AppShapes
import com.example.ui.theme.LocalAmoledMode
import com.example.ui.theme.LocalM3Expressive
import com.example.ui.theme.getCallGreenColor
import com.example.ui.theme.getOnCallGreenColor
import com.example.util.PhoneFormatHelper
import com.example.util.RichHapticEngine
import com.example.util.WhatsAppHelper
import kotlinx.coroutines.delay

@Composable
fun PostCallSheet(
    data: PostCallData,
    dismissDurationSeconds: Int = 5,
    onDismiss: () -> Unit = { CallManager.dismissPostCallData() },
    onCallBack: (String, String) -> Unit,
    onSaveContact: (String) -> Unit
) {
    val context = LocalContext.current
    val isExpressive = LocalM3Expressive.current
    val isAmoled = LocalAmoledMode.current

    val isAutoDismissEnabled = dismissDurationSeconds > 0
    val dismissMillis = (dismissDurationSeconds * 1000L).coerceAtLeast(0L)

    // Auto-dismiss countdown timer (configurable seconds, or persistent if 0)
    var progressTarget by remember(dismissDurationSeconds, data) { mutableFloatStateOf(1f) }
    LaunchedEffect(data, dismissDurationSeconds) {
        if (isAutoDismissEnabled) {
            progressTarget = 0f
            delay(dismissMillis)
            onDismiss()
        }
    }

    val animatedProgress by animateFloatAsState(
        targetValue = if (isAutoDismissEnabled) progressTarget else 1f,
        animationSpec = tween(
            durationMillis = if (isAutoDismissEnabled) dismissMillis.toInt() else 0,
            easing = LinearEasing
        ),
        label = "post_call_progress"
    )

    val cardColor = if (isAmoled) {
        Color(0xFF141414)
    } else if (isExpressive) {
        MaterialTheme.colorScheme.surfaceColorAtElevation(6.dp)
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }

    val countryIso = remember(context) { PhoneFormatHelper.getCountryIso(context) }
    val formattedNumber = remember(data.number, countryIso) {
        PhoneFormatHelper.formatDisplayNumber(data.number, countryIso)
    }

    val durationText = remember(data.durationSeconds) {
        if (data.durationSeconds <= 0L) "Call ended"
        else {
            val mins = data.durationSeconds / 60
            val secs = data.durationSeconds % 60
            if (mins > 0) "Call ended • ${mins}m ${secs}s" else "Call ended • ${secs}s"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .testTag("post_call_card"),
        shape = AppShapes.Card,
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 10.dp, start = 14.dp, end = 14.dp)
        ) {
            // Header with duration, timer progress, and dismiss button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(AppShapes.Avatar)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Text(
                        text = durationText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        onDismiss()
                    },
                    modifier = Modifier.size(24.dp).testTag("post_call_close")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.close),
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Caller Identity Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Squircle Avatar
                Surface(
                    modifier = Modifier.size(46.dp),
                    shape = AppShapes.Avatar,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    tonalElevation = 2.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (data.photoUri.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(data.photoUri)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = data.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = getInitials(data.name.ifBlank { data.number }),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }

                // Name and Formatted Number
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = data.name.ifBlank { formattedNumber },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formattedNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action Chips: WhatsApp | Call Back | Message | Save
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. WhatsApp Action
                PostCallActionChip(
                    icon = Icons.Default.Chat,
                    label = "WhatsApp",
                    containerColor = Color(0xFF25D366).copy(alpha = 0.20f),
                    contentColor = Color(0xFF1EBE5D),
                    modifier = Modifier.weight(1f),
                    testTag = "post_call_whatsapp"
                ) {
                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                    WhatsAppHelper.openWhatsAppChat(context, data.number)
                    onDismiss()
                }

                // 2. Call Back Action
                PostCallActionChip(
                    icon = Icons.Default.Call,
                    label = "Call Back",
                    containerColor = getCallGreenColor().copy(alpha = 0.22f),
                    contentColor = getCallGreenColor(),
                    modifier = Modifier.weight(1f),
                    testTag = "post_call_callback"
                ) {
                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                    onDismiss()
                    onCallBack(data.number, data.name)
                }

                // 3. Message / SMS Action
                PostCallActionChip(
                    icon = Icons.Default.Message,
                    label = "SMS",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f),
                    testTag = "post_call_sms"
                ) {
                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                    try {
                        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${data.number}")).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        context.startActivity(intent)
                    } catch (_: Exception) {
                        Toast.makeText(context, context.getString(R.string.error_open_messages), Toast.LENGTH_SHORT).show()
                    }
                    onDismiss()
                }

                // 4. Save Contact Action (if unsaved)
                if (data.isUnsaved) {
                    PostCallActionChip(
                        icon = Icons.Default.PersonAdd,
                        label = "Save",
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.weight(1f),
                        testTag = "post_call_save"
                    ) {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        onDismiss()
                        onSaveContact(data.number)
                    }
                }
            }

            if (isAutoDismissEnabled) {
                Spacer(modifier = Modifier.height(8.dp))

                // Subdued Linear Progress Indicator showing auto-dismiss time
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.dp)
                        .clip(AppShapes.Avatar),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                )
            }
        }
    }
}

@Composable
private fun PostCallActionChip(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String = "",
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = AppShapes.Chip,
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
            .height(38.dp)
            .testTag(testTag)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
