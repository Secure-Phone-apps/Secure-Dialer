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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AppShapes
import com.example.ui.theme.LocalAmoledMode
import com.example.ui.theme.LocalM3Expressive
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

    // Native Activity Result Launcher guarantees system role prompt opens & updates state immediately
    val defaultDialerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { _ ->
        val updatedDefault = CallScreenPermissionHelper.isDefaultDialer(context)
        viewModel.isDefaultDialer.value = updatedDefault
    }

    // Re-check permissions when returning from system settings
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.isDefaultDialer.value = CallScreenPermissionHelper.isDefaultDialer(context)
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

    val isAmoled = LocalAmoledMode.current
    val isExpressive = LocalM3Expressive.current
    val cardBgColor = when {
        isAmoled -> Color(0xFF000000)
        isExpressive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surfaceColorAtElevation(2.dp)
    }

    val cardBorder = if (isAmoled) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
    }

    val executeAction = {
        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
        when {
            needsDefaultDialer -> CallScreenPermissionHelper.requestDefaultDialer(context, defaultDialerLauncher)
            needsFullScreen -> CallScreenPermissionHelper.requestFullScreenIntentPermission(context)
            else -> CallScreenPermissionHelper.requestOverlayPermission(context)
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { executeAction() },
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = cardBorder,
        shape = AppShapes.current.cardShape
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Surface(
                shape = AppShapes.current.chipShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = when {
                            needsDefaultDialer -> Icons.Default.PhoneInTalk
                            needsFullScreen -> Icons.Default.LockOpen
                            else -> Icons.Default.PictureInPicture
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        needsDefaultDialer -> stringResource(R.string.default_dialer_warning)
                        needsFullScreen -> stringResource(R.string.settings_full_screen_intent_warning)
                        else -> stringResource(R.string.settings_overlay_permission_warning)
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = when {
                        needsDefaultDialer -> stringResource(R.string.btn_set_default)
                        needsFullScreen -> stringResource(R.string.btn_configure)
                        else -> stringResource(R.string.btn_grant)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Button(
                onClick = { executeAction() },
                shape = AppShapes.current.buttonShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
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
