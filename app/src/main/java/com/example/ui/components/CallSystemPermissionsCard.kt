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

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.CallScreenPermissionHelper
import com.example.util.RichHapticEngine

@Composable
fun CallSystemPermissionsCard(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDefaultDialer by viewModel.isDefaultDialer
    val isDynamicIslandEnabled by viewModel.isDynamicIslandEnabled

    var canFullScreen by remember { mutableStateOf(CallScreenPermissionHelper.canUseFullScreenIntent(context)) }
    var canOverlay by remember { mutableStateOf(CallScreenPermissionHelper.canDrawOverlays(context)) }

    // Re-check whenever lifecycle resumes
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                canFullScreen = CallScreenPermissionHelper.canUseFullScreenIntent(context)
                canOverlay = CallScreenPermissionHelper.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val needsDefaultDialer = !isDefaultDialer
    val needsFullScreen = !canFullScreen
    val needsOverlay = isDynamicIslandEnabled && !canOverlay

    if (!needsDefaultDialer && !needsFullScreen && !needsOverlay) {
        return
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.85f),
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = when {
                    needsDefaultDialer -> Icons.Default.Warning
                    needsFullScreen -> Icons.Default.LockOpen
                    else -> Icons.Default.PictureInPicture
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(28.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        needsDefaultDialer -> stringResource(R.string.default_dialer_warning)
                        needsFullScreen -> stringResource(R.string.settings_full_screen_intent_warning)
                        else -> stringResource(R.string.settings_overlay_permission_warning)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            FilledTonalButton(
                onClick = {
                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                    when {
                        needsDefaultDialer -> CallScreenPermissionHelper.requestDefaultDialer(context)
                        needsFullScreen -> CallScreenPermissionHelper.requestFullScreenIntentPermission(context)
                        else -> CallScreenPermissionHelper.requestOverlayPermission(context)
                    }
                },
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = when {
                        needsDefaultDialer -> stringResource(R.string.btn_set_default)
                        needsFullScreen -> stringResource(R.string.btn_configure)
                        else -> stringResource(R.string.btn_grant)
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
