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
fun CallBlockingSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color,
    highlightedTitle: String? = null
) {
    val scrollState = rememberScrollState()
    val blockedNumbersEntities by viewModel.blockedNumbersFlow.collectAsState()
    val spamList by viewModel.spamFlow.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Expandable Blocklist
        HighlightableCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            cardBgColor = cardBgColor,
            isHighlighted = isMatchTitle("Blocked Numbers & Blacklist", highlightedTitle) || isMatchTitle("Block Unknown & Private Calls", highlightedTitle) || isMatchTitle("Blocked Numbers", highlightedTitle),
            shape = MaterialTheme.shapes.medium
        ) {
            ExpandableSettingsCard(
                title = stringResource(R.string.settings_blocked_numbers),
                subtitle = stringResource(R.string.settings_blocked_numbers_sub),
                icon = Icons.Default.Block,
                iconBgColor = MaterialTheme.colorScheme.errorContainer,
                iconTint = MaterialTheme.colorScheme.onErrorContainer,
                cardBgColor = Color.Transparent,
                badgeText = if (blockedNumbersEntities.isNotEmpty()) "${blockedNumbersEntities.size} blocked" else null,
                initiallyExpanded = isMatchTitle("Blocked Numbers", highlightedTitle) || isMatchTitle("Block Unknown", highlightedTitle)
            ) {
                BlockListSettings(viewModel = viewModel, cardBgColor = cardBgColor)
            }
        }

        // Expandable Spam Defense Database
        HighlightableCard(
            modifier = Modifier.padding(horizontal = 16.dp),
            cardBgColor = cardBgColor,
            isHighlighted = isMatchTitle("Offline Spam Database & Protection", highlightedTitle) || isMatchTitle("Spam", highlightedTitle),
            shape = MaterialTheme.shapes.medium
        ) {
            ExpandableSettingsCard(
                title = stringResource(R.string.settings_spam_protection),
                subtitle = stringResource(R.string.local_offline_protection),
                icon = Icons.Default.Shield,
                iconBgColor = MaterialTheme.colorScheme.primaryContainer,
                iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                cardBgColor = Color.Transparent,
                badgeText = if (spamList.isNotEmpty()) "${spamList.size} rules" else null,
                initiallyExpanded = isMatchTitle("Offline Spam Database", highlightedTitle) || isMatchTitle("Spam", highlightedTitle)
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    SpamDatabaseSettings(viewModel = viewModel, cardBgColor = cardBgColor)
                }
            }
        }
    }
}
