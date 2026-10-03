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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.viewmodel.DialerViewModel
import com.example.util.RichHapticEngine

@Composable
fun DefaultStartupTabCard(
    viewModel: DialerViewModel,
    cardBgColor: Color
) {
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth()) {
        // 1. Default Startup Tab Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_default_startup_tab),
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                val tabOptions = listOf(
                    "RECENTS" to stringResource(R.string.tab_recents),
                    "CONTACTS" to stringResource(R.string.tab_contacts),
                    "DIALPAD" to stringResource(R.string.tab_dialpad)
                )
                val currentStartupKey = viewModel.defaultStartupTabKey.value
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tabOptions.forEach { (key, title) ->
                        val isSel = currentStartupKey == key
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(if (isSel) MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable {
                                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                    viewModel.updateDefaultStartupTabKey(key)
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSel) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 2. Call Log Dashboard Card (Full-row clickable)
        val isCallLogDashboardEnabled by viewModel.isCallLogDashboardEnabled
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = MaterialTheme.shapes.medium,
            onClick = {
                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                viewModel.updateCallLogDashboardEnabled(!isCallLogDashboardEnabled)
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_call_log_dashboard),
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.settings_call_log_dashboard_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isCallLogDashboardEnabled,
                    onCheckedChange = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        viewModel.updateCallLogDashboardEnabled(it)
                    }
                )
            }
        }

        // 3. Call Log Filters Card (Full-row clickable)
        val isCallLogFiltersEnabled by viewModel.isCallLogFiltersEnabled
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = MaterialTheme.shapes.medium,
            onClick = {
                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                viewModel.updateCallLogFiltersEnabled(!isCallLogFiltersEnabled)
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_call_log_filters),
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.settings_call_log_filters_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isCallLogFiltersEnabled,
                    onCheckedChange = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        viewModel.updateCallLogFiltersEnabled(it)
                    }
                )
            }
        }

        // 4. Customizable Tab Position Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = MaterialTheme.shapes.medium
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.settings_tab_layout),
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = stringResource(R.string.settings_tab_layout_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val tabSlotLeft by viewModel.tabSlotLeft
                val tabSlotMiddle by viewModel.tabSlotMiddle
                val tabSlotRight by viewModel.tabSlotRight

                val availableScreens = listOf(
                    "RECENTS" to stringResource(R.string.tab_recents),
                    "CONTACTS" to stringResource(R.string.tab_contacts),
                    "DIALPAD" to stringResource(R.string.tab_dialpad)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TabSlotDropdown(
                        label = stringResource(R.string.tab_slot_left),
                        selectedKey = tabSlotLeft,
                        screens = availableScreens,
                        onSelect = { viewModel.updateTabSlotLeft(it) },
                        modifier = Modifier.weight(1f)
                    )

                    TabSlotDropdown(
                        label = stringResource(R.string.tab_slot_middle),
                        selectedKey = tabSlotMiddle,
                        screens = availableScreens,
                        onSelect = { viewModel.updateTabSlotMiddle(it) },
                        modifier = Modifier.weight(1f)
                    )

                    TabSlotDropdown(
                        label = stringResource(R.string.tab_slot_right),
                        selectedKey = tabSlotRight,
                        screens = availableScreens,
                        onSelect = { viewModel.updateTabSlotRight(it) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 5. Swipe Actions Toggle Card (Full-row clickable)
        val isRowSwipeEnabled by viewModel.isRowSwipeEnabled
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(containerColor = cardBgColor),
            shape = MaterialTheme.shapes.medium,
            onClick = {
                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                viewModel.updateRowSwipeEnabled(!isRowSwipeEnabled)
            }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                    Text(
                        text = stringResource(R.string.settings_swipe_actions),
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = stringResource(R.string.settings_swipe_actions_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = isRowSwipeEnabled,
                    onCheckedChange = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        viewModel.updateRowSwipeEnabled(it)
                    }
                )
            }
        }
    }
}

@Composable
fun TabSlotDropdown(
    label: String,
    selectedKey: String,
    screens: List<Pair<String, String>>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val displayTitle = screens.firstOrNull { it.first == selectedKey }?.second ?: selectedKey

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Box {
            Surface(
                onClick = {
                    RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                    expanded = true
                },
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                screens.forEach { (key, title) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (key == selectedKey) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                            expanded = false
                            onSelect(key)
                        }
                    )
                }
            }
        }
    }
}