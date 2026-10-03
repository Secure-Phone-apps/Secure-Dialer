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

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.parseHexColor
import com.example.util.RichHapticEngine

data class PaletteSeedItem(
    val key: String,
    val name: String,
    val colors: List<Color>
)

@Composable
fun ThemeColorPicker(
    currentSelected: String,
    customColorHex: String = "#68A500",
    onColorSelected: (String) -> Unit,
    onCustomColorChange: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var showCustomHexDialog by remember { mutableStateOf(false) }
    var tempHexInput by remember { mutableStateOf(customColorHex) }

    val parsedCustomColor = parseHexColor(customColorHex)

    val palettes = remember(customColorHex, parsedCustomColor) {
        listOf(
            PaletteSeedItem("expressive_lime", "Expressive Lime", listOf(Color(0xFF68A500), Color(0xFF84CC16), Color(0xFF365314), Color(0xFFECFCCB))),
            PaletteSeedItem("electric_indigo", "Electric Indigo", listOf(Color(0xFF3F51B5), Color(0xFF6366F1), Color(0xFF1E1B4B), Color(0xFFE0E7FF))),
            PaletteSeedItem("teal_breeze", "Teal Breeze", listOf(Color(0xFF009688), Color(0xFF14B8A6), Color(0xFF004D40), Color(0xFFCCFBF1))),
            PaletteSeedItem("sunset_gold", "Sunset Gold", listOf(Color(0xFFE65100), Color(0xFFF59E0B), Color(0xFF7C2D12), Color(0xFFFFEDD5))),
            PaletteSeedItem("burgundy_plum", "Burgundy Plum", listOf(Color(0xFF880D1E), Color(0xFF9F1239), Color(0xFF4C0519), Color(0xFFFFF1F2))),
            PaletteSeedItem("oceanic_sapphire", "Oceanic Sapphire", listOf(Color(0xFF1E3A8A), Color(0xFF3B82F6), Color(0xFF172554), Color(0xFFEFF6FF))),
            PaletteSeedItem("burnt_terracotta", "Burnt Terracotta", listOf(Color(0xFFC2410C), Color(0xFFEA580C), Color(0xFF431407), Color(0xFFFFF7ED))),
            PaletteSeedItem("sleek_slate", "Sleek Slate", listOf(Color(0xFF475569), Color(0xFF64748B), Color(0xFF1E293B), Color(0xFFF1F5F9))),
            PaletteSeedItem("rose_magenta", "Rose Magenta", listOf(Color(0xFFC2185B), Color(0xFFF43F5E), Color(0xFF881337), Color(0xFFFFE4E6))),
            PaletteSeedItem("sky_cyan", "Sky Cyan", listOf(Color(0xFF0284C7), Color(0xFF38BDF8), Color(0xFF0369A1), Color(0xFFE0F2FE))),
            PaletteSeedItem("violet_bloom", "Violet Bloom", listOf(Color(0xFF7C3AED), Color(0xFFA78BFA), Color(0xFF4C1D95), Color(0xFFEDE9FE))),
            PaletteSeedItem("custom", "Custom Seed ($customColorHex)", listOf(parsedCustomColor, parsedCustomColor.copy(alpha = 0.75f), parsedCustomColor.copy(alpha = 0.45f), Color.White))
        )
    }

    val selectedPalette = palettes.find { it.key == currentSelected } ?: palettes.first()

    Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = stringResource(R.string.settings_accent_color),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = selectedPalette.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            if (currentSelected == "custom") {
                TextButton(
                    onClick = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                        tempHexInput = customColorHex
                        showCustomHexDialog = true
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.btn_edit),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = stringResource(R.string.theme_hex_code_chip), fontSize = 12.sp)
                }
            }
        }

        // Grid of pure Squircle Swatches
        val chunkedPalettes = palettes.chunked(6)
        chunkedPalettes.forEachIndexed { rowIndex, rowItems ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = if (rowIndex < chunkedPalettes.size - 1) 6.dp else 0.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                rowItems.forEach { item ->
                    val isSelected = currentSelected == item.key
                    val cardScale by animateFloatAsState(
                        targetValue = if (isSelected) 1.08f else 1.0f,
                        animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing),
                        label = "squircle_scale"
                    )

                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .scale(cardScale)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else Color.Transparent
                            )
                            .border(
                                width = if (isSelected) 2.5.dp else 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(14.dp)
                            )
                            .clickable {
                                RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.KEY_TICK)
                                onColorSelected(item.key)
                                if (item.key == "custom") {
                                    tempHexInput = customColorHex
                                    showCustomHexDialog = true
                                }
                            }
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(RoundedCornerShape(10.dp))
                        ) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(item.colors.getOrElse(0) { Color.Gray }))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(item.colors.getOrElse(1) { Color.LightGray }))
                                }
                                Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(item.colors.getOrElse(2) { Color.DarkGray }))
                                    Box(modifier = Modifier.weight(1f).fillMaxHeight().background(item.colors.getOrElse(3) { Color.White }))
                                }
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCustomHexDialog) {
        val formattedCandidate = if (tempHexInput.startsWith("#")) tempHexInput else "#$tempHexInput"
        // FIXED: Safe color validation prevents runtime theme engine crashes on malformed hex strings
        val isValidHex = remember(formattedCandidate) {
            try {
                AndroidColor.parseColor(formattedCandidate)
                true
            } catch (_: Exception) {
                false
            }
        }

        AlertDialog(
            onDismissRequest = { showCustomHexDialog = false },
            icon = { Icon(Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text(stringResource(R.string.theme_custom_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.theme_custom_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = tempHexInput,
                        onValueChange = { tempHexInput = it.take(9) },
                        label = { Text(stringResource(R.string.theme_hex_code_label)) },
                        singleLine = true,
                        isError = !isValidHex && tempHexInput.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = {
                            val sampleColor = if (isValidHex) parseHexColor(formattedCandidate) else Color.Transparent
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(sampleColor)
                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                            )
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    enabled = isValidHex,
                    onClick = {
                        RichHapticEngine.performHaptic(context, RichHapticEngine.HapticStyle.SUCCESS)
                        onCustomColorChange(formattedCandidate)
                        onColorSelected("custom")
                        showCustomHexDialog = false
                    }
                ) {
                    Text(stringResource(R.string.theme_apply))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomHexDialog = false }) {
                    Text(stringResource(R.string.btn_cancel))
                }
            }
        )
    }
}