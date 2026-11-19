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

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.PictureInPicture
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.AppShapes
import com.example.ui.theme.LocalAdaptiveLayout
import com.example.ui.theme.LocalAmoledMode
import com.example.ui.theme.LocalM3Expressive
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.CallScreenPermissionHelper
import com.example.util.RichHapticEngine

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CallSystemPermissionsCard(
    viewModel: DialerViewModel,
    modifier: Modifier = Modifier,
    onLearnMoreBattery: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val adaptive = LocalAdaptiveLayout.current
    val isDefaultDialer by viewModel.isDefaultDialer
    val isDynamicIslandEnabled by viewModel.isDynamicIslandEnabled
    val batteryWarningMode by viewModel.batteryWarningMode
    val batterySnoozeUntil by viewModel.batteryWarningSnoozeUntilMs

    var canFullScreen by remember { mutableStateOf(CallScreenPermissionHelper.canUseFullScreenIntent(context)) }
    var canOverlay by remember { mutableStateOf(CallScreenPermissionHelper.canDrawOverlays(context)) }
    var isBatteryOptimizationIgnored by remember {
        mutableStateOf(CallScreenPermissionHelper.isIgnoringBatteryOptimizations(context))
    }
    var checkNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

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
                isBatteryOptimizationIgnored = CallScreenPermissionHelper.isIgnoringBatteryOptimizations(context)
                checkNowMs = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val isBatteryWarningAllowed = remember(batteryWarningMode, batterySnoozeUntil, checkNowMs) {
        viewModel.shouldShowBatteryWarning(checkNowMs)
    }

    val needsDefaultDialer = !isDefaultDialer
    val needsBatteryExemption = !isBatteryOptimizationIgnored && isBatteryWarningAllowed
    val needsFullScreen = !canFullScreen
    val needsOverlay = isDynamicIslandEnabled && !canOverlay

    if (!needsDefaultDialer && !needsBatteryExemption && !needsFullScreen && !needsOverlay) {
        return
    }

    val isShowingBatteryBanner = !needsDefaultDialer && needsBatteryExemption
    val isAmoled = LocalAmoledMode.current
    val isExpressive = LocalM3Expressive.current
    val cardBgColor = when {
        isAmoled -> Color(0xFF000000)
        isExpressive -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
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
            isShowingBatteryBanner -> CallScreenPermissionHelper.requestIgnoreBatteryOptimizations(context)
            needsFullScreen -> CallScreenPermissionHelper.requestFullScreenIntentPermission(context)
            else -> CallScreenPermissionHelper.requestOverlayPermission(context)
        }
    }

    val openLearnMore = {
        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
        if (onLearnMoreBattery != null) {
            onLearnMoreBattery()
        } else {
            viewModel.openSettingsToTab(9)
        }
    }

    val dismissBatteryBanner = {
        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
        viewModel.setBatteryWarningEnabled(false)
        checkNowMs = System.currentTimeMillis()
        Toast.makeText(
            context,
            context.getString(R.string.toast_battery_warning_dismissed),
            Toast.LENGTH_SHORT
        ).show()
    }

    val fontScale = LocalDensity.current.fontScale

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = if (adaptive.isVeryNarrowWidth) 10.dp else 14.dp,
                vertical = 4.dp
            )
            .testTag("permissions_warning_card")
            .clickable { executeAction() },
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = cardBorder,
        shape = AppShapes.current.cardShape
    ) {
        val stackActionButton = isShowingBatteryBanner || adaptive.isVeryNarrowWidth || adaptive.maxWidth < 360.dp || fontScale >= 1.25f

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(if (stackActionButton) 8.dp else 0.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Surface(
                    shape = AppShapes.current.chipShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        Icon(
                            imageVector = when {
                                needsDefaultDialer -> Icons.Default.PhoneInTalk
                                isShowingBatteryBanner -> Icons.Default.BatteryChargingFull
                                needsFullScreen -> Icons.Default.LockOpen
                                else -> Icons.Default.PictureInPicture
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = when {
                        needsDefaultDialer -> stringResource(R.string.default_dialer_warning)
                        isShowingBatteryBanner -> stringResource(R.string.battery_warning_short_banner)
                        needsFullScreen -> stringResource(R.string.settings_full_screen_intent_warning)
                        else -> stringResource(R.string.settings_overlay_permission_warning)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                if (isShowingBatteryBanner) {
                    IconButton(
                        onClick = dismissBatteryBanner,
                        modifier = Modifier
                            .size(48.dp)
                            .testTag("battery_warning_dismiss_icon")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = stringResource(R.string.btn_dismiss),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (!stackActionButton) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { executeAction() },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("permissions_warning_action_button"),
                        shape = AppShapes.current.buttonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
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
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            if (isShowingBatteryBanner) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = dismissBatteryBanner,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("battery_warning_dismiss_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_dismiss),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }

                    TextButton(
                        onClick = openLearnMore,
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("battery_warning_learn_more_button"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_learn_more),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }

                    Button(
                        onClick = { executeAction() },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("permissions_warning_action_button"),
                        shape = AppShapes.current.buttonShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.btn_unrestricted),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else if (stackActionButton) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { executeAction() },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("permissions_warning_action_button"),
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
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
