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

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallBlockingSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        Pair("Blocklist", Icons.Default.Block),
        Pair("Spam Database", Icons.Default.Shield),
        Pair("Quick Responses", Icons.Default.Quickreply)
    )

    Column(modifier = Modifier.fillMaxSize()) {
        PrimaryTabRow(
            selectedTabIndex = selectedSubTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth()
        ) {
            tabs.forEachIndexed { index, (title, icon) ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = { Text(title, maxLines = 1) },
                    icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp)) }
                )
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when (selectedSubTab) {
                0 -> BlockListSettings(viewModel = viewModel, cardBgColor = cardBgColor)
                1 -> SpamDatabaseSettings(viewModel = viewModel, cardBgColor = cardBgColor)
                2 -> QuickResponsesSettings(viewModel = viewModel, cardBgColor = cardBgColor)
            }
        }
    }
}
