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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
fun SpeedDialAndQuickResponsesSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color,
    highlightedTitle: String? = null
) {
    val scrollState = rememberScrollState()
    val speedDialEntities by viewModel.speedDialFlow.collectAsState()
    val quickResponsesEntities by viewModel.quickResponsesFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Expandable Speed Dial
        HighlightableCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            cardBgColor = cardBgColor,
            isHighlighted = isMatchTitle("Speed Dial Shortcuts (Keys 1–9)", highlightedTitle) || isMatchTitle("Speed Dial", highlightedTitle),
            shape = MaterialTheme.shapes.medium
        ) {
            ExpandableSettingsCard(
                title = stringResource(R.string.settings_speed_dial),
                subtitle = stringResource(R.string.settings_speed_dial_sub),
                icon = Icons.Default.TouchApp,
                iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                cardBgColor = Color.Transparent,
                badgeText = if (speedDialEntities.isNotEmpty()) "${speedDialEntities.size} configured" else null,
                initiallyExpanded = isMatchTitle("Speed Dial Shortcuts (Keys 1–9)", highlightedTitle)
            ) {
                SpeedDialSettings(viewModel = viewModel, cardBgColor = cardBgColor)
            }
        }

        // Expandable Quick Responses
        HighlightableCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            cardBgColor = cardBgColor,
            isHighlighted = isMatchTitle("Quick Decline Text Replies", highlightedTitle) || isMatchTitle("Quick Reply", highlightedTitle),
            shape = MaterialTheme.shapes.medium
        ) {
            ExpandableSettingsCard(
                title = stringResource(R.string.settings_quick_responses),
                subtitle = stringResource(R.string.quick_decline_messages),
                icon = Icons.Default.Quickreply,
                iconBgColor = MaterialTheme.colorScheme.secondaryContainer,
                iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
                cardBgColor = Color.Transparent,
                badgeText = if (quickResponsesEntities.isNotEmpty()) "${quickResponsesEntities.size} templates" else null,
                initiallyExpanded = isMatchTitle("Quick Decline Text Replies", highlightedTitle)
            ) {
                QuickResponsesSettings(viewModel = viewModel, cardBgColor = cardBgColor)
            }
        }
    }
}
