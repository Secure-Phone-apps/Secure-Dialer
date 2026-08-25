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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.viewmodel.DialerViewModel

@Composable
fun SoundAndGesturesSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color,
    highlightedTitle: String? = null
) {
    val dialpadTonesEnabled by viewModel.dialpadTonesEnabled
    val vibrateOnClickEnabled by viewModel.vibrateOnClickEnabled
    val isRowSwipeEnabled by viewModel.isRowSwipeEnabled

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        // [Header] SOUND & HAPTICS
        item {
            PreferenceHeader(stringResource(R.string.header_sound_haptics))
        }

        // Dialpad Tones Card
        item {
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Dialpad Keypad Tones", highlightedTitle),
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
                isHighlighted = isMatchTitle("Call Vibration & Haptics", highlightedTitle),
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
