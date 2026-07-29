package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun SettingsPreferredSimRow(
    preferredSim: String,
    onSimChange: (String) -> Unit,
    haptic: HapticFeedback
) {
    ListItem(
        headlineContent = { 
            Text(
                stringResource(R.string.settings_preferred_sim),
                fontWeight = FontWeight.Medium,
                style = MaterialTheme.typography.bodyLarge
            ) 
        },
        supportingContent = { 
            Text(
                stringResource(R.string.settings_preferred_sim_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                listOf("SIM 1" to sim1Label, "SIM 2" to sim2Label, "Ask" to askLabel).forEach { (opKey, labelText) ->
                    val sel = preferredSim == opKey
                    FilterChip(
                        selected = sel,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSimChange(opKey)
                        },
                        label = { 
                            Text(
                                text = labelText, 
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
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
