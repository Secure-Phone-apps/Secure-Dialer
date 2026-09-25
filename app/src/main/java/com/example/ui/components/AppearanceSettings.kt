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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
fun AppearanceSettings(
    viewModel: DialerViewModel,
    cardBgColor: Color,
    highlightedTitle: String? = null
) {
    val isDarkTheme by viewModel.isDarkTheme
    val isAmoledMode by viewModel.isAmoledMode
    val useDynamicColor by viewModel.useDynamicColor
    val isM3Expressive by viewModel.isM3Expressive

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
    ) {
        // [Header] THEME & STYLING
        item {
            PreferenceHeader(stringResource(R.string.header_theme_styling))
        }

        // Dark Theme Card
        item {
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Dark Mode & Theme Mode", highlightedTitle) || isMatchTitle("Appearance Settings", highlightedTitle) || isMatchTitle("Appearance & Theme", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowToggle(
                    title = stringResource(R.string.settings_dark_theme),
                    subtitle = stringResource(R.string.settings_dark_theme_sub),
                    checked = isDarkTheme,
                    onCheckedChange = { viewModel.updateDarkTheme(it) },
                    icon = Icons.Default.DarkMode,
                    iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
            }
        }

        // Pure Black AMOLED Card (Permanently visible for direct OLED configuration)
        item {
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Pure Black", highlightedTitle) || isMatchTitle("True Black", highlightedTitle) || isMatchTitle("OLED", highlightedTitle) || isMatchTitle("AMOLED", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowToggle(
                    title = stringResource(R.string.settings_pure_black),
                    subtitle = if (!isDarkTheme) {
                        stringResource(R.string.settings_pure_black_sub_disabled_dark)
                    } else {
                        stringResource(R.string.settings_pure_black_sub)
                    },
                    checked = isAmoledMode && isDarkTheme,
                    onCheckedChange = { checked ->
                        viewModel.updateAmoledMode(checked)
                    },
                    icon = Icons.Default.Contrast,
                    iconBgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // Dynamic Color Card
        val isDynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        item {
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Dynamic Colors (Monet)", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowToggle(
                    title = stringResource(R.string.settings_dynamic_color),
                    subtitle = if (isDynamicSupported) {
                        stringResource(R.string.settings_dynamic_color_sub)
                    } else {
                        stringResource(R.string.settings_dynamic_color_sub_fallback)
                    },
                    checked = useDynamicColor && isDynamicSupported,
                    onCheckedChange = { if (isDynamicSupported) viewModel.updateUseDynamicColor(it) },
                    icon = Icons.Default.Palette,
                    iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.secondary,
                    enabled = isDynamicSupported
                )
            }
        }

        // Theme Color Picker Card
        if (!useDynamicColor || !isDynamicSupported) {
            item {
                HighlightableCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    cardBgColor = cardBgColor,
                    isHighlighted = isMatchTitle("Accent Color Palette", highlightedTitle),
                    shape = MaterialTheme.shapes.medium
                ) {
                    ThemeColorPicker(
                        currentSelected = viewModel.themeColor.value,
                        customColorHex = viewModel.customColorHex.value,
                        onColorSelected = { viewModel.updateThemeColor(it) },
                        onCustomColorChange = { viewModel.updateCustomColorHex(it) }
                    )
                }
            }
        }

        // M3 Expressive Card
        item {
            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Expressive Material 3 Layout", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                SettingsRowToggle(
                    title = stringResource(R.string.settings_m3_expressive),
                    subtitle = stringResource(R.string.settings_m3_expressive_sub),
                    checked = isM3Expressive,
                    onCheckedChange = { viewModel.updateM3Expressive(it) },
                    icon = Icons.Default.AutoAwesome,
                    iconBgColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }
        }

        // [Header] DYNAMIC ISLAND & CALL CAPSULE
        item {
            PreferenceHeader(stringResource(R.string.settings_dynamic_island_title))
        }

        // Dynamic Island Master Card
        item {
            val isDynamicIslandEnabled by viewModel.isDynamicIslandEnabled
            val isDynamicIslandSpeakerOnly by viewModel.isDynamicIslandSpeakerOnly
            val context = androidx.compose.ui.platform.LocalContext.current

            HighlightableCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                cardBgColor = cardBgColor,
                isHighlighted = isMatchTitle("Dynamic Island", highlightedTitle) || isMatchTitle("Dynamic Island Call Capsule", highlightedTitle),
                shape = MaterialTheme.shapes.medium
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsRowToggle(
                        title = stringResource(R.string.settings_dynamic_island_title),
                        subtitle = stringResource(R.string.settings_dynamic_island_sub),
                        checked = isDynamicIslandEnabled,
                        onCheckedChange = { viewModel.updateDynamicIslandEnabled(it) },
                        icon = Icons.Default.CropPortrait,
                        iconBgColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                        iconTint = MaterialTheme.colorScheme.primary
                    )

                    if (isDynamicIslandEnabled) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsRowToggle(
                            title = stringResource(R.string.settings_dynamic_island_speaker_only),
                            subtitle = stringResource(R.string.settings_dynamic_island_speaker_only_sub),
                            checked = isDynamicIslandSpeakerOnly,
                            onCheckedChange = { viewModel.updateDynamicIslandSpeakerOnly(it) },
                            icon = Icons.Default.VolumeUp,
                            iconBgColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                            iconTint = MaterialTheme.colorScheme.secondary
                        )

                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            val canOverlay = android.provider.Settings.canDrawOverlays(context)
                            if (!canOverlay) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = stringResource(R.string.settings_dynamic_island_floating_permission),
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold
                                        )
                                        Text(
                                            text = stringResource(R.string.settings_dynamic_island_floating_permission_sub),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    FilledTonalButton(
                                        onClick = {
                                            try {
                                                val intent = android.content.Intent(
                                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    android.net.Uri.parse("package:${context.packageName}")
                                                )
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                        }
                                    ) {
                                        Text("Grant")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
