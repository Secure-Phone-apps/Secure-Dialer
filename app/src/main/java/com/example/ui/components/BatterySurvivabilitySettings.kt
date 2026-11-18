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

import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.LocalAmoledMode
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.CallScreenPermissionHelper
import com.example.util.RichHapticEngine
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BatterySurvivabilitySettings(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val context = LocalContext.current
    val isAmoled = LocalAmoledMode.current
    val oemFamily = remember { CallScreenPermissionHelper.detectOemFamily() }
    val oemName = remember(oemFamily) { CallScreenPermissionHelper.getOemDisplayName(oemFamily) }
    val androidRelease = remember { Build.VERSION.RELEASE ?: "Android" }
    val sdkInt = remember { Build.VERSION.SDK_INT }

    var isBatteryUnrestricted by remember {
        mutableStateOf(CallScreenPermissionHelper.isIgnoringBatteryOptimizations(context))
    }
    var isDefaultDialer by remember {
        mutableStateOf(CallScreenPermissionHelper.isDefaultDialer(context))
    }
    var checkNowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                isBatteryUnrestricted = CallScreenPermissionHelper.isIgnoringBatteryOptimizations(context)
                isDefaultDialer = CallScreenPermissionHelper.isDefaultDialer(context)
                viewModel.isDefaultDialer.value = isDefaultDialer
                checkNowMs = System.currentTimeMillis()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val warningMode by viewModel.batteryWarningMode
    val snoozeUntilMs by viewModel.batteryWarningSnoozeUntilMs

    val formattedSnoozeDate = remember(snoozeUntilMs) {
        if (snoozeUntilMs > 0L) {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(snoozeUntilMs))
        } else {
            ""
        }
    }

    val isWarningActive = when (warningMode) {
        DialerViewModel.BATTERY_WARNING_DISABLED -> false
        DialerViewModel.BATTERY_WARNING_SNOOZED -> checkNowMs >= snoozeUntilMs
        else -> true
    }

    val cardBorder = if (isAmoled) BorderStroke(1.dp, Color(0xFF1E1E1E)) else null

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("battery_survivability_screen"),
        contentPadding = PaddingValues(bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. LIVE DEVICE & PROTECTION STATUS
        item {
            PreferenceHeader(stringResource(R.string.battery_guide_status_header))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("battery_status_card"),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardBorder,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            modifier = Modifier.size(40.dp),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isBatteryUnrestricted) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.secondaryContainer
                            }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.BatteryChargingFull,
                                    contentDescription = null,
                                    tint = if (isBatteryUnrestricted) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSecondaryContainer
                                    },
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(
                                    R.string.battery_guide_device_row,
                                    oemName,
                                    androidRelease,
                                    sdkInt
                                ),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isBatteryUnrestricted) {
                                    stringResource(R.string.battery_guide_mode_unrestricted)
                                } else {
                                    stringResource(R.string.battery_guide_mode_optimized)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isBatteryUnrestricted) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                            Text(
                                text = if (isDefaultDialer) {
                                    stringResource(R.string.battery_guide_dialer_role_active)
                                } else {
                                    stringResource(R.string.battery_guide_dialer_role_inactive)
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = stringResource(R.string.battery_guide_android_lock_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 2. WARNING NOTIFICATION CONTROLS & SNOOZE SCHEDULE
        item {
            Spacer(modifier = Modifier.height(6.dp))
            PreferenceHeader(stringResource(R.string.battery_guide_controls_header))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("battery_warning_controls_card"),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardBorder,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsRowToggle(
                        title = stringResource(R.string.battery_warning_toggle_title),
                        subtitle = when {
                            warningMode == DialerViewModel.BATTERY_WARNING_DISABLED ->
                                stringResource(R.string.battery_warning_toggle_disabled)
                            warningMode == DialerViewModel.BATTERY_WARNING_SNOOZED && checkNowMs < snoozeUntilMs ->
                                stringResource(R.string.battery_warning_toggle_snoozed, formattedSnoozeDate)
                            else ->
                                stringResource(R.string.battery_warning_toggle_active)
                        },
                        checked = isWarningActive,
                        onCheckedChange = { enabled ->
                            viewModel.setBatteryWarningEnabled(enabled)
                            checkNowMs = System.currentTimeMillis()
                        },
                        icon = Icons.Default.NotificationsPaused,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                    )

                    FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("battery_warning_schedule_row"),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val isActiveMode = isWarningActive
                        val isSnoozedNow = warningMode == DialerViewModel.BATTERY_WARNING_SNOOZED && checkNowMs < snoozeUntilMs
                        val remainingMs = (snoozeUntilMs - checkNowMs).coerceAtLeast(0L)
                        val isSnoozed1Day = isSnoozedNow && remainingMs <= DialerViewModel.ONE_DAY_MS + 60_000L
                        val isSnoozed1Week = isSnoozedNow && remainingMs > DialerViewModel.ONE_DAY_MS + 60_000L
                        val isDisabled = warningMode == DialerViewModel.BATTERY_WARNING_DISABLED

                        FilterChip(
                            selected = isActiveMode,
                            onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                viewModel.setBatteryWarningEnabled(true)
                                checkNowMs = System.currentTimeMillis()
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.battery_schedule_show_now),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("battery_mode_enabled_chip")
                        )

                        FilterChip(
                            selected = isSnoozed1Day,
                            onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                val now = System.currentTimeMillis()
                                viewModel.snoozeBatteryWarning(DialerViewModel.ONE_DAY_MS, now)
                                checkNowMs = now
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.battery_schedule_1_day),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("battery_snooze_1d_chip")
                        )

                        FilterChip(
                            selected = isSnoozed1Week,
                            onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                val now = System.currentTimeMillis()
                                viewModel.snoozeBatteryWarning(DialerViewModel.ONE_WEEK_MS, now)
                                checkNowMs = now
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.battery_schedule_1_week),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("battery_snooze_7d_chip")
                        )

                        FilterChip(
                            selected = isDisabled,
                            onClick = {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                viewModel.setBatteryWarningEnabled(false)
                                checkNowMs = System.currentTimeMillis()
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.battery_schedule_disable),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .testTag("battery_disable_forever_chip")
                        )
                    }
                }
            }
        }

        // 3. THOROUGH EXPLANATION
        item {
            Spacer(modifier = Modifier.height(6.dp))
            PreferenceHeader(stringResource(R.string.battery_guide_why_header))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("battery_guide_explanation_card"),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardBorder,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    BatteryExplanationItem(
                        icon = Icons.Default.VerifiedUser,
                        title = stringResource(R.string.battery_why_aosp_title),
                        body = stringResource(R.string.battery_why_aosp_desc),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    BatteryExplanationItem(
                        icon = Icons.Default.PhoneAndroid,
                        title = stringResource(R.string.battery_why_oem_title),
                        body = stringResource(R.string.battery_why_oem_desc),
                        tint = MaterialTheme.colorScheme.secondary
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                    BatteryExplanationItem(
                        icon = Icons.Default.Mic,
                        title = stringResource(R.string.battery_why_recording_title),
                        body = stringResource(R.string.battery_why_recording_desc),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }

        // 4. SYSTEM & OEM SHORTCUTS
        item {
            Spacer(modifier = Modifier.height(6.dp))
            PreferenceHeader(stringResource(R.string.battery_guide_actions_header))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = cardBgColor),
                border = cardBorder,
                shape = MaterialTheme.shapes.medium
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.CLICK)
                            CallScreenPermissionHelper.openOemBackgroundSettings(context, oemFamily)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("open_oem_battery_settings_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_open_oem_battery_settings),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                            CallScreenPermissionHelper.openAppDetailsSettings(context)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .testTag("open_app_info_battery_btn"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.btn_open_app_details_settings),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BatteryExplanationItem(
    icon: ImageVector,
    title: String,
    body: String,
    tint: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier
                .padding(top = 2.dp)
                .size(20.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
