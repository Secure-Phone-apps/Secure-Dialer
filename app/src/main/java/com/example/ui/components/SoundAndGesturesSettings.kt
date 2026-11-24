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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Gesture
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhoneInTalk
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.CallScreenPermissionHelper
import com.example.util.IncomingCallDisplayMode
import com.example.util.RichHapticEngine

@Composable
fun SoundAndGesturesSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color,
    highlightedTitle: String? = null
) {
    val context = LocalContext.current
    val dialpadTonesEnabled by viewModel.dialpadTonesEnabled
    val vibrateOnClickEnabled by viewModel.vibrateOnClickEnabled
    val flipToSilenceEnabled by viewModel.flipToSilenceEnabled
    val isRowSwipeEnabled by viewModel.isRowSwipeEnabled
    val incomingScreenOnModeKey by viewModel.incomingCallScreenOnMode
    val incomingScreenOffModeKey by viewModel.incomingCallScreenOffMode

    val screenOnMode = IncomingCallDisplayMode.fromScreenOnKey(incomingScreenOnModeKey)
    val screenOffMode = IncomingCallDisplayMode.fromScreenOffKey(incomingScreenOffModeKey)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag("sound_gestures_list"),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        // [Header] INCOMING CALL DISPLAY
        item {
            PreferenceHeader(stringResource(R.string.header_incoming_call_display))
        }

        // Incoming Call Display: When Screen is Active
        item {
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Incoming Call Display (Screen Active)", highlightedTitle) ||
                        isMatchTitle("Incoming Call Display", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                IncomingDisplayModeSelectorCard(
                    title = stringResource(R.string.settings_incoming_screen_on_title),
                    subtitle = stringResource(R.string.settings_incoming_screen_on_sub),
                    icon = Icons.Default.PhoneInTalk,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    iconTint = MaterialTheme.colorScheme.primary,
                    options = listOf(
                        IncomingCallDisplayMode.NOTIFICATION to Triple(
                            stringResource(R.string.incoming_display_mode_notification),
                            Icons.Default.Notifications,
                            "incoming_screen_on_notification"
                        ),
                        IncomingCallDisplayMode.FULL_SCREEN to Triple(
                            stringResource(R.string.incoming_display_mode_full_screen),
                            Icons.Default.Fullscreen,
                            "incoming_screen_on_fullscreen"
                        )
                    ),
                    selectedMode = screenOnMode,
                    onSelect = { mode ->
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        viewModel.updateIncomingCallScreenOnMode(mode)
                    }
                )
            }
        }

        // Incoming Call Display: When Screen is Off or Locked
        item {
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Incoming Call Display (Screen Off / Locked)", highlightedTitle) ||
                        isMatchTitle("Full Screen Calls on Lock Screen", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                IncomingDisplayModeSelectorCard(
                    title = stringResource(R.string.settings_incoming_screen_off_title),
                    subtitle = stringResource(R.string.settings_incoming_screen_off_sub),
                    icon = Icons.Default.Lock,
                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    iconTint = MaterialTheme.colorScheme.secondary,
                    options = listOf(
                        IncomingCallDisplayMode.FULL_SCREEN to Triple(
                            stringResource(R.string.incoming_display_mode_full_screen),
                            Icons.Default.Fullscreen,
                            "incoming_screen_off_fullscreen"
                        ),
                        IncomingCallDisplayMode.NOTIFICATION to Triple(
                            stringResource(R.string.incoming_display_mode_notification),
                            Icons.Default.Notifications,
                            "incoming_screen_off_notification"
                        )
                    ),
                    selectedMode = screenOffMode,
                    onSelect = { mode ->
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        viewModel.updateIncomingCallScreenOffMode(mode)
                    }
                )
            }
        }

        // Full Screen Lock Screen Calls OS Permission Helper
        item {
            val canFullScreen = remember { CallScreenPermissionHelper.canUseFullScreenIntent(context) }
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.settings_lockscreen_calling_title),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.settings_lockscreen_calling_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    FilledTonalButton(
                        onClick = {
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                            CallScreenPermissionHelper.requestFullScreenIntentPermission(context)
                        }
                    ) {
                        Text(if (canFullScreen) stringResource(R.string.btn_configure) else stringResource(R.string.btn_grant))
                    }
                }
            }
        }

        // [Header] SOUND & HAPTICS
        item {
            Spacer(modifier = Modifier.height(12.dp))
            PreferenceHeader(stringResource(R.string.header_sound_haptics))
        }

        // Dialpad Tones Card
        item {
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Dialpad Keypad Tones", highlightedTitle) ||
                        isMatchTitle("Sound & Gestures Settings", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowToggle(
                    title = stringResource(R.string.settings_dialpad_tones),
                    subtitle = stringResource(R.string.settings_dialpad_tones_sub),
                    checked = dialpadTonesEnabled,
                    onCheckedChange = { viewModel.updateDialpadTonesEnabled(it) },
                    icon = Icons.AutoMirrored.Filled.VolumeUp,
                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // Vibrate on Click Card
        item {
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Call Vibration & Haptics", highlightedTitle) ||
                        isMatchTitle("Vibrate", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowToggle(
                    title = stringResource(R.string.settings_vibrate),
                    subtitle = stringResource(R.string.settings_vibrate_sub),
                    checked = vibrateOnClickEnabled,
                    onCheckedChange = { viewModel.updateVibrateOnClickEnabled(it) },
                    icon = Icons.Default.Vibration,
                    iconBgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // Flip to Silence Card
        item {
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Flip to Silence", highlightedTitle) ||
                        isMatchTitle("Flip", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowToggle(
                    title = stringResource(R.string.settings_flip_to_silence),
                    subtitle = stringResource(R.string.settings_flip_to_silence_sub),
                    checked = flipToSilenceEnabled,
                    onCheckedChange = { viewModel.updateFlipToSilenceEnabled(it) },
                    icon = Icons.Default.ScreenRotation,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Row Swipe Actions Card (RESTORED: Fixed dormant preference bug)
        item {
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Call Swipe Actions", highlightedTitle) ||
                        isMatchTitle("Swipe", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowToggle(
                    title = stringResource(R.string.action_swipe_call),
                    subtitle = stringResource(R.string.action_swipe_message),
                    checked = isRowSwipeEnabled,
                    onCheckedChange = { viewModel.updateRowSwipeEnabled(it) },
                    icon = Icons.Default.Gesture,
                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        // [Header] NAVIGATION & LAYOUT
        item {
            Spacer(modifier = Modifier.height(12.dp))
            PreferenceHeader(stringResource(R.string.header_navigation_layout))
        }

        item {
            DefaultStartupTabCard(viewModel = viewModel, cardBgColor = cardBgColor)
        }
    }
}

@Composable
private fun IncomingDisplayModeSelectorCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconBgColor: Color,
    iconTint: Color,
    options: List<Pair<IncomingCallDisplayMode, Triple<String, ImageVector, String>>>,
    selectedMode: IncomingCallDisplayMode,
    onSelect: (IncomingCallDisplayMode) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            options.forEach { (mode, meta) ->
                val (label, optionIcon, tag) = meta
                val isSelected = selectedMode == mode
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                        .testTag(tag)
                        .semantics { selected = isSelected }
                        .clickable(role = Role.RadioButton) { onSelect(mode) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = optionIcon,
                        contentDescription = null,
                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}