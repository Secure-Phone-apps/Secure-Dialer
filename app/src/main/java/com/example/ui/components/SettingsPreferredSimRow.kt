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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.util.MultiSimManager
import com.example.util.RichHapticEngine

@Composable
fun SettingsPreferredSimRow(
    preferredSim: String,
    onSimChange: (String) -> Unit,
    haptic: HapticFeedback? = null
) {
    val context = LocalContext.current
    val physicalSimCount = remember(context) { MultiSimManager.getPhysicalSimCount(context) }

    ListItem(
        headlineContent = {
            Text(
                text = stringResource(R.string.settings_preferred_sim),
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        supportingContent = {
            Text(
                text = if (physicalSimCount <= 1) {
                    stringResource(R.string.sim_1)
                } else {
                    stringResource(R.string.settings_preferred_sim_sub)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SimCard,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        },
        trailingContent = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val askLabel = stringResource(R.string.sim_ask)
                val sim1Label = stringResource(R.string.sim_1)
                val sim2Label = stringResource(R.string.sim_2)

                val options = if (physicalSimCount > 1) {
                    listOf("SIM 1" to sim1Label, "SIM 2" to sim2Label, "Ask" to askLabel)
                } else {
                    listOf("SIM 1" to sim1Label)
                }

                options.forEach { (opKey, labelText) ->
                    val isSelected = preferredSim == opKey || (physicalSimCount <= 1 && opKey == "SIM 1")
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                            onSimChange(opKey)
                        },
                        label = {
                            Text(
                                text = labelText,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        border = null,
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}