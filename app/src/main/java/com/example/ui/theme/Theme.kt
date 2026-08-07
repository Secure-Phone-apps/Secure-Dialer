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

package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val DarkColorScheme =
  darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = Color(0xFF000000),
    onBackground = OnBackgroundDark,
    surface = Color(0xFF000000),
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark
  )

private val LightColorScheme =
  lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = Color(0xFFFFFFFF),
    onBackground = OnBackgroundLight,
    surface = Color(0xFFFFFFFF),
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight
  )

@Composable
fun getMissedCallColor(): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) Color(0xFFFF6B6B) else Color(0xFFD32F2F)
}

@Composable
fun getCallGreenColor(): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) Color(0xFF34D399) else Color(0xFF059669)
}

@Composable
fun getOnCallGreenColor(): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) Color(0xFF003822) else Color(0xFFFFFFFF)
}

@Composable
fun getDeclineRedColor(): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) Color(0xFFF43F5E) else Color(0xFFE11D48)
}

@Composable
fun getOnDeclineRedColor(): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) Color(0xFF4C0519) else Color(0xFFFFFFFF)
}

@Composable
fun getDialedCallColor(): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) Color(0xFFA0AAB0) else Color(0xFF607D8B)
}

@Composable
fun getReceivedCallColor(): Color {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (isDark) Color(0xFF4CAF50) else Color(0xFF2E7D32)
}

fun parseHexColor(hex: String, fallback: Color = Color(0xFF68A500)): Color {
    return try {
        val clean = hex.removePrefix("#").trim()
        val colorInt = when (clean.length) {
            6 -> android.graphics.Color.parseColor("#FF$clean")
            8 -> android.graphics.Color.parseColor("#$clean")
            else -> android.graphics.Color.parseColor("#FF68A500")
        }
        Color(colorInt)
    } catch (e: Exception) {
        fallback
    }
}

fun getCustomSeedColorScheme(hex: String, darkTheme: Boolean): ColorScheme {
    val seed = parseHexColor(hex)
    return if (darkTheme) {
        darkColorScheme(
            primary = seed,
            onPrimary = Color.Black,
            primaryContainer = seed.copy(alpha = 0.35f),
            onPrimaryContainer = Color.White,
            secondary = seed.copy(alpha = 0.80f),
            onSecondary = Color.Black,
            secondaryContainer = seed.copy(alpha = 0.25f),
            onSecondaryContainer = Color.White,
            tertiary = seed.copy(alpha = 0.90f),
            onTertiary = Color.Black,
            tertiaryContainer = seed.copy(alpha = 0.30f),
            onTertiaryContainer = Color.White,
            background = Color(0xFF000000),
            surface = Color(0xFF0C0D0E),
            surfaceVariant = seed.copy(alpha = 0.18f),
            onSurface = Color(0xFFF1F5F9),
            outline = seed.copy(alpha = 0.60f)
        )
    } else {
        lightColorScheme(
            primary = seed,
            onPrimary = Color.White,
            primaryContainer = seed.copy(alpha = 0.20f),
            onPrimaryContainer = Color.Black,
            secondary = seed.copy(alpha = 0.85f),
            onSecondary = Color.White,
            secondaryContainer = seed.copy(alpha = 0.15f),
            onSecondaryContainer = Color.Black,
            tertiary = seed.copy(alpha = 0.75f),
            onTertiary = Color.White,
            tertiaryContainer = seed.copy(alpha = 0.18f),
            onTertiaryContainer = Color.Black,
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFFAFAFA),
            surfaceVariant = seed.copy(alpha = 0.12f),
            onSurface = Color(0xFF0F172A),
            outline = seed.copy(alpha = 0.50f)
        )
    }
}

fun getColorSchemeForTheme(
    themeColor: String,
    darkTheme: Boolean,
    customColorHex: String = "#68A500"
): ColorScheme {
    if (themeColor == "custom") {
        return getCustomSeedColorScheme(customColorHex, darkTheme)
    }

    return when (themeColor) {
        "burgundy_plum" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFFDA4AF),
                    onPrimary = Color(0xFF5F0017),
                    primaryContainer = Color(0xFF800020),
                    onPrimaryContainer = Color(0xFFFFD9E2),
                    secondary = Color(0xFFE35D7A),
                    onSecondary = Color(0xFF490011),
                    secondaryContainer = Color(0xFF6E0D25),
                    onSecondaryContainer = Color(0xFFFFD9E2),
                    tertiary = Color(0xFFF89BB0),
                    onTertiary = Color(0xFF5F0017),
                    tertiaryContainer = Color(0xFF800020),
                    onTertiaryContainer = Color(0xFFFFD9E2),
                    background = Color(0xFF000000),
                    surface = Color(0xFF180509),
                    surfaceVariant = Color(0xFF2E0911),
                    onSurface = Color(0xFFFFF0F1),
                    outline = Color(0xFF800020)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF880D1E),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFFFF1F2),
                    onPrimaryContainer = Color(0xFF4C0519),
                    secondary = Color(0xFF9F1239),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFFFE4E6),
                    onSecondaryContainer = Color(0xFF4C0519),
                    tertiary = Color(0xFF4C0519),
                    onTertiary = Color(0xFFFFFFFF),
                    tertiaryContainer = Color(0xFFFFE4E6),
                    onTertiaryContainer = Color(0xFF4C0519),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFAFAFA),
                    surfaceVariant = Color(0xFFFFF1F2),
                    onSurface = Color(0xFF1C1917),
                    outline = Color(0xFFFB7185)
                )
            }
        }
        "oceanic_sapphire" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFF60A5FA),
                    onPrimary = Color(0xFF172554),
                    primaryContainer = Color(0xFF1E3A8A),
                    onPrimaryContainer = Color(0xFFDBEAFE),
                    secondary = Color(0xFF3B82F6),
                    onSecondary = Color(0xFF172554),
                    secondaryContainer = Color(0xFF1E40AF),
                    onSecondaryContainer = Color(0xFFDBEAFE),
                    tertiary = Color(0xFF93C5FD),
                    onTertiary = Color(0xFF172554),
                    tertiaryContainer = Color(0xFF1E3A8A),
                    onTertiaryContainer = Color(0xFFDBEAFE),
                    background = Color(0xFF000000),
                    surface = Color(0xFF030D1B),
                    surfaceVariant = Color(0xFF0B1931),
                    onSurface = Color(0xFFEFF6FF),
                    outline = Color(0xFF3B82F6)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF1E3A8A),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFEFF6FF),
                    onPrimaryContainer = Color(0xFF172554),
                    secondary = Color(0xFF3B82F6),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFDBEAFE),
                    onSecondaryContainer = Color(0xFF1E3A8A),
                    tertiary = Color(0xFF1D4ED8),
                    onTertiary = Color(0xFFFFFFFF),
                    tertiaryContainer = Color(0xFFDBEAFE),
                    onTertiaryContainer = Color(0xFF172554),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFAFAFA),
                    surfaceVariant = Color(0xFFEFF6FF),
                    onSurface = Color(0xFF0F172A),
                    outline = Color(0xFF93C5FD)
                )
            }
        }
        "burnt_terracotta" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFF97316),
                    onPrimary = Color(0xFF431407),
                    primaryContainer = Color(0xFF7C2D12),
                    onPrimaryContainer = Color(0xFFFFEDD5),
                    secondary = Color(0xFFEA580C),
                    onSecondary = Color(0xFF431407),
                    secondaryContainer = Color(0xFF9A3412),
                    onSecondaryContainer = Color(0xFFFFE4D6),
                    tertiary = Color(0xFFFDBA74),
                    onTertiary = Color(0xFF431407),
                    tertiaryContainer = Color(0xFF7C2D12),
                    onTertiaryContainer = Color(0xFFFFEDD5),
                    background = Color(0xFF000000),
                    surface = Color(0xFF150A05),
                    surfaceVariant = Color(0xFF2C140A),
                    onSurface = Color(0xFFFFF7ED),
                    outline = Color(0xFFEA580C)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFFC2410C),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFFFF7ED),
                    onPrimaryContainer = Color(0xFF431407),
                    secondary = Color(0xFFEA580C),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFFFEDD5),
                    onSecondaryContainer = Color(0xFF431407),
                    tertiary = Color(0xFF9A3412),
                    onTertiary = Color(0xFFFFFFFF),
                    tertiaryContainer = Color(0xFFFFEDD5),
                    onTertiaryContainer = Color(0xFF431407),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFAFAFA),
                    surfaceVariant = Color(0xFFFFF7ED),
                    onSurface = Color(0xFF1C1917),
                    outline = Color(0xFFFDBA74)
                )
            }
        }
        "sleek_slate" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFF94A3B8),
                    onPrimary = Color(0xFF0F172A),
                    primaryContainer = Color(0xFF1E293B),
                    onPrimaryContainer = Color(0xFFF1F5F9),
                    secondary = Color(0xFF64748B),
                    onSecondary = Color(0xFF0F172A),
                    secondaryContainer = Color(0xFF334155),
                    onSecondaryContainer = Color(0xFFE2E8F0),
                    tertiary = Color(0xFFCBD5E1),
                    onTertiary = Color(0xFF0F172A),
                    tertiaryContainer = Color(0xFF1E293B),
                    onTertiaryContainer = Color(0xFFF1F5F9),
                    background = Color(0xFF000000),
                    surface = Color(0xFF0A0F1D),
                    surfaceVariant = Color(0xFF192337),
                    onSurface = Color(0xFFF8FAFC),
                    outline = Color(0xFF475569)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF475569),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFF1F5F9),
                    onPrimaryContainer = Color(0xFF0F172A),
                    secondary = Color(0xFF64748B),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFE2E8F0),
                    onSecondaryContainer = Color(0xFF1E293B),
                    tertiary = Color(0xFF334155),
                    onTertiary = Color(0xFFFFFFFF),
                    tertiaryContainer = Color(0xFFE2E8F0),
                    onTertiaryContainer = Color(0xFF0F172A),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFAFAFA),
                    surfaceVariant = Color(0xFFF1F5F9),
                    onSurface = Color(0xFF0F172A),
                    outline = Color(0xFFCBD5E1)
                )
            }
        }
        "emerald_green" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFF34D399),
                    onPrimary = Color(0xFF003822),
                    primaryContainer = Color(0xFF065F46),
                    onPrimaryContainer = Color(0xFFD1FAE5),
                    secondary = Color(0xFF10B981),
                    onSecondary = Color(0xFF002214),
                    secondaryContainer = Color(0xFF047857),
                    onSecondaryContainer = Color(0xFFA7F3D0),
                    tertiary = Color(0xFF6EE7B7),
                    onTertiary = Color(0xFF003822),
                    tertiaryContainer = Color(0xFF065F46),
                    onTertiaryContainer = Color(0xFFD1FAE5),
                    background = Color(0xFF000000),
                    surface = Color(0xFF021B12),
                    surfaceVariant = Color(0xFF0B3324),
                    onSurface = Color(0xFFECFDF5),
                    outline = Color(0xFF059669)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF10B981),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFD1FAE5),
                    onPrimaryContainer = Color(0xFF003822),
                    secondary = Color(0xFF059669),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFECFDF5),
                    onSecondaryContainer = Color(0xFF065F46),
                    tertiary = Color(0xFF047857),
                    onTertiary = Color(0xFFFFFFFF),
                    tertiaryContainer = Color(0xFFD1FAE5),
                    onTertiaryContainer = Color(0xFF003822),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFAFAFA),
                    surfaceVariant = Color(0xFFECFDF5),
                    onSurface = Color(0xFF0F172A),
                    outline = Color(0xFF34D399)
                )
            }
        }
        "sky_cyan" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFF38BDF8),
                    onPrimary = Color(0xFF00365A),
                    primaryContainer = Color(0xFF0369A1),
                    onPrimaryContainer = Color(0xFFE0F2FE),
                    secondary = Color(0xFF0284C7),
                    onSecondary = Color(0xFF002540),
                    secondaryContainer = Color(0xFF075985),
                    onSecondaryContainer = Color(0xFFBAE6FD),
                    tertiary = Color(0xFF7DD3FC),
                    onTertiary = Color(0xFF00365A),
                    tertiaryContainer = Color(0xFF0369A1),
                    onTertiaryContainer = Color(0xFFE0F2FE),
                    background = Color(0xFF000000),
                    surface = Color(0xFF031A28),
                    surfaceVariant = Color(0xFF0A2E44),
                    onSurface = Color(0xFFF0F9FF),
                    outline = Color(0xFF0284C7)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF0284C7),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFE0F2FE),
                    onPrimaryContainer = Color(0xFF00365A),
                    secondary = Color(0xFF0369A1),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFF0F9FF),
                    onSecondaryContainer = Color(0xFF075985),
                    tertiary = Color(0xFF075985),
                    onTertiary = Color(0xFFFFFFFF),
                    tertiaryContainer = Color(0xFFE0F2FE),
                    onTertiaryContainer = Color(0xFF00365A),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFAFAFA),
                    surfaceVariant = Color(0xFFF0F9FF),
                    onSurface = Color(0xFF0F172A),
                    outline = Color(0xFF38BDF8)
                )
            }
        }
        "violet_bloom" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFA78BFA),
                    onPrimary = Color(0xFF2E1065),
                    primaryContainer = Color(0xFF5B21B6),
                    onPrimaryContainer = Color(0xFFEDE9FE),
                    secondary = Color(0xFF7C3AED),
                    onSecondary = Color(0xFF1E0059),
                    secondaryContainer = Color(0xFF4C1D95),
                    onSecondaryContainer = Color(0xFFDDD6FE),
                    tertiary = Color(0xFFC084FC),
                    onTertiary = Color(0xFF2E1065),
                    tertiaryContainer = Color(0xFF5B21B6),
                    onTertiaryContainer = Color(0xFFEDE9FE),
                    background = Color(0xFF000000),
                    surface = Color(0xFF130924),
                    surfaceVariant = Color(0xFF231240),
                    onSurface = Color(0xFFF5F3FF),
                    outline = Color(0xFF7C3AED)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF7C3AED),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFEDE9FE),
                    onPrimaryContainer = Color(0xFF2E1065),
                    secondary = Color(0xFF6D28D9),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFF5F3FF),
                    onSecondaryContainer = Color(0xFF4C1D95),
                    tertiary = Color(0xFF5B21B6),
                    onTertiary = Color(0xFFFFFFFF),
                    tertiaryContainer = Color(0xFFEDE9FE),
                    onTertiaryContainer = Color(0xFF2E1065),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFAFAFA),
                    surfaceVariant = Color(0xFFF5F3FF),
                    onSurface = Color(0xFF0F172A),
                    outline = Color(0xFFA78BFA)
                )
            }
        }
        "teal_breeze" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFF2DD4BF),
                    onPrimary = Color(0xFF003731),
                    primaryContainer = Color(0xFF115E59),
                    onPrimaryContainer = Color(0xFFCCFBF1),
                    secondary = Color(0xFF14B8A6),
                    onSecondary = Color(0xFF00251A),
                    secondaryContainer = Color(0xFF004D40),
                    onSecondaryContainer = Color(0xFF99F6E4),
                    tertiary = Color(0xFF5EEAD4),
                    onTertiary = Color(0xFF003731),
                    tertiaryContainer = Color(0xFF115E59),
                    onTertiaryContainer = Color(0xFFCCFBF1),
                    background = Color(0xFF000000),
                    surface = Color(0xFF041C19),
                    surfaceVariant = Color(0xFF0D332F),
                    onSurface = Color(0xFFE6FFFA),
                    outline = Color(0xFF0F766E)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF009688),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFCCFBF1),
                    onPrimaryContainer = Color(0xFF003731),
                    secondary = Color(0xFF14B8A6),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFE6FFFA),
                    onSecondaryContainer = Color(0xFF004D40),
                    tertiary = Color(0xFF0F766E),
                    onTertiary = Color(0xFFFFFFFF),
                    tertiaryContainer = Color(0xFFCCFBF1),
                    onTertiaryContainer = Color(0xFF003731),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFAFAFA),
                    surfaceVariant = Color(0xFFF0FDFA),
                    onSurface = Color(0xFF0F172A),
                    outline = Color(0xFF2DD4BF)
                )
            }
        }
        "electric_indigo" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFF818CF8),
                    onPrimary = Color(0xFF1A237E),
                    primaryContainer = Color(0xFF312E81),
                    onPrimaryContainer = Color(0xFFE0E7FF),
                    secondary = Color(0xFF6366F1),
                    onSecondary = Color(0xFF0D1344),
                    secondaryContainer = Color(0xFF1E1B4B),
                    onSecondaryContainer = Color(0xFFC7D2FE),
                    tertiary = Color(0xFFA5B4FC),
                    onTertiary = Color(0xFF1A237E),
                    tertiaryContainer = Color(0xFF312E81),
                    onTertiaryContainer = Color(0xFFE0E7FF),
                    background = Color(0xFF000000),
                    surface = Color(0xFF0B0D1B),
                    surfaceVariant = Color(0xFF1E2139),
                    onSurface = Color(0xFFEEF2FF),
                    outline = Color(0xFF4F46E5)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF3F51B5),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFE0E7FF),
                    onPrimaryContainer = Color(0xFF1A237E),
                    secondary = Color(0xFF6366F1),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFEEF2FF),
                    onSecondaryContainer = Color(0xFF312E81),
                    tertiary = Color(0xFF3730A3),
                    onTertiary = Color(0xFFFFFFFF),
                    tertiaryContainer = Color(0xFFE0E7FF),
                    onTertiaryContainer = Color(0xFF1A237E),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFAFAFA),
                    surfaceVariant = Color(0xFFEEF2FF),
                    onSurface = Color(0xFF0F172A),
                    outline = Color(0xFF818CF8)
                )
            }
        }
        "sunset_gold" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFFBBF24),
                    onPrimary = Color(0xFF451A03),
                    primaryContainer = Color(0xFF7C2D12),
                    onPrimaryContainer = Color(0xFFFFEDD5),
                    secondary = Color(0xFFF59E0B),
                    onSecondary = Color(0xFF3E1100),
                    secondaryContainer = Color(0xFFBF360C),
                    onSecondaryContainer = Color(0xFFFEF3C7),
                    tertiary = Color(0xFFFDBA74),
                    onTertiary = Color(0xFF431407),
                    tertiaryContainer = Color(0xFF7C2D12),
                    onTertiaryContainer = Color(0xFFFFEDD5),
                    background = Color(0xFF000000),
                    surface = Color(0xFF1A0A03),
                    surfaceVariant = Color(0xFF311508),
                    onSurface = Color(0xFFFFF7ED),
                    outline = Color(0xFFEA580C)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFFE65100),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFFFEDD5),
                    onPrimaryContainer = Color(0xFF431407),
                    secondary = Color(0xFFF59E0B),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFFEF3C7),
                    onSecondaryContainer = Color(0xFF7C2D12),
                    tertiary = Color(0xFF9A3412),
                    onTertiary = Color(0xFFFFFFFF),
                    tertiaryContainer = Color(0xFFFFEDD5),
                    onTertiaryContainer = Color(0xFF431407),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFAFAFA),
                    surfaceVariant = Color(0xFFFFF7ED),
                    onSurface = Color(0xFF1C1917),
                    outline = Color(0xFFF97316)
                )
            }
        }
        "rose_magenta" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFFB7185),
                    onPrimary = Color(0xFF4C0519),
                    primaryContainer = Color(0xFF881337),
                    onPrimaryContainer = Color(0xFFFFE4E6),
                    secondary = Color(0xFFF43F5E),
                    onSecondary = Color(0xFF3B001F),
                    secondaryContainer = Color(0xFF880E4F),
                    onSecondaryContainer = Color(0xFFFECDD3),
                    tertiary = Color(0xFFFDA4AF),
                    onTertiary = Color(0xFF4C0519),
                    tertiaryContainer = Color(0xFF881337),
                    onTertiaryContainer = Color(0xFFFFE4E6),
                    background = Color(0xFF000000),
                    surface = Color(0xFF1C030B),
                    surfaceVariant = Color(0xFF330716),
                    onSurface = Color(0xFFFFF1F2),
                    outline = Color(0xFFE11D48)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFFC2185B),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFFFE4E6),
                    onPrimaryContainer = Color(0xFF4C0519),
                    secondary = Color(0xFFE11D48),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFFFF1F2),
                    onSecondaryContainer = Color(0xFF881337),
                    tertiary = Color(0xFF9F1239),
                    onTertiary = Color(0xFFFFFFFF),
                    tertiaryContainer = Color(0xFFFFE4E6),
                    onTertiaryContainer = Color(0xFF4C0519),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFAFAFA),
                    surfaceVariant = Color(0xFFFFF1F2),
                    onSurface = Color(0xFF1C1917),
                    outline = Color(0xFFF43F5E)
                )
            }
        }
        "amoled_black" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFFAFAFA),
                    onPrimary = Color(0xFF000000),
                    primaryContainer = Color(0xFF27272A),
                    onPrimaryContainer = Color(0xFFFAFAFA),
                    secondary = Color(0xFFA1A1AA),
                    onSecondary = Color(0xFF000000),
                    secondaryContainer = Color(0xFF18181B),
                    onSecondaryContainer = Color(0xFFFAFAFA),
                    tertiary = Color(0xFFD4D4D8),
                    onTertiary = Color(0xFF000000),
                    tertiaryContainer = Color(0xFF27272A),
                    onTertiaryContainer = Color(0xFFFAFAFA),
                    background = Color(0xFF000000),
                    surface = Color(0xFF000000),
                    surfaceVariant = Color(0xFF18181B),
                    onSurface = Color(0xFFFAFAFA),
                    outline = Color(0xFF52525B)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF18181B),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFE4E4E7),
                    onPrimaryContainer = Color(0xFF09090B),
                    secondary = Color(0xFF3F3F46),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFF4F4F5),
                    onSecondaryContainer = Color(0xFF18181B),
                    tertiary = Color(0xFF27272A),
                    onTertiary = Color(0xFFFFFFFF),
                    tertiaryContainer = Color(0xFFE4E4E7),
                    onTertiaryContainer = Color(0xFF09090B),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFAFAFA),
                    surfaceVariant = Color(0xFFF4F4F5),
                    onSurface = Color(0xFF09090B),
                    outline = Color(0xFFA1A1AA)
                )
            }
        }
        else -> { // expressive_lime (#68A500)
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFA3E635),
                    onPrimary = Color(0xFF1A3300),
                    primaryContainer = Color(0xFF365314),
                    onPrimaryContainer = Color(0xFFECFCCB),
                    secondary = Color(0xFF84CC16),
                    onSecondary = Color(0xFF112300),
                    secondaryContainer = Color(0xFF2B5300),
                    onSecondaryContainer = Color(0xFFF7FEE7),
                    tertiary = Color(0xFFBEF264),
                    onTertiary = Color(0xFF1A3300),
                    tertiaryContainer = Color(0xFF365314),
                    onTertiaryContainer = Color(0xFFECFCCB),
                    background = Color(0xFF000000),
                    surface = Color(0xFF0D1204),
                    surfaceVariant = Color(0xFF1A2E05),
                    onSurface = Color(0xFFECFCCB),
                    outline = Color(0xFF4D7C0F)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF68A500),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFECFCCB),
                    onPrimaryContainer = Color(0xFF1A3300),
                    secondary = Color(0xFF84CC16),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFF7FEE7),
                    onSecondaryContainer = Color(0xFF365314),
                    tertiary = Color(0xFF4D7C0F),
                    onTertiary = Color(0xFFFFFFFF),
                    tertiaryContainer = Color(0xFFECFCCB),
                    onTertiaryContainer = Color(0xFF1A3300),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFAFAFA),
                    surfaceVariant = Color(0xFFF7FEE7),
                    onSurface = Color(0xFF1A2E05),
                    outline = Color(0xFFA3E635)
                )
            }
        }
    }
}

val LocalM3Expressive = staticCompositionLocalOf { true }

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(12.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(24.dp),
    large = RoundedCornerShape(32.dp),
    extraLarge = RoundedCornerShape(40.dp)
)

val StandardShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

fun getExpressiveColorScheme(colorScheme: ColorScheme, darkTheme: Boolean): ColorScheme {
    return if (darkTheme) {
        colorScheme.copy(
            primaryContainer = colorScheme.primary.copy(alpha = 0.35f),
            onPrimaryContainer = Color.White,
            secondaryContainer = colorScheme.secondary.copy(alpha = 0.30f),
            onSecondaryContainer = Color.White,
            surfaceVariant = colorScheme.primary.copy(alpha = 0.18f),
            tertiaryContainer = colorScheme.tertiary.copy(alpha = 0.30f)
        )
    } else {
        colorScheme.copy(
            primaryContainer = colorScheme.primary.copy(alpha = 0.22f),
            secondaryContainer = colorScheme.secondary.copy(alpha = 0.22f),
            surfaceVariant = colorScheme.primary.copy(alpha = 0.12f),
            tertiaryContainer = colorScheme.tertiary.copy(alpha = 0.20f)
        )
    }
}

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  themeColor: String = "expressive_lime",
  customColorHex: String = "#68A500",
  isAmoledMode: Boolean = false,
  isM3Expressive: Boolean = true,
  content: @Composable () -> Unit,
) {
  var colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      else -> getColorSchemeForTheme(themeColor, darkTheme, customColorHex)
    }

  if (darkTheme && isAmoledMode) {
      colorScheme = colorScheme.copy(
          background = Color(0xFF000000),
          surface = Color(0xFF000000)
      )
  }

  if (isM3Expressive) {
      colorScheme = getExpressiveColorScheme(colorScheme, darkTheme)
  }

  CompositionLocalProvider(
      LocalM3Expressive provides isM3Expressive
  ) {
      MaterialTheme(
          colorScheme = colorScheme,
          shapes = if (isM3Expressive) ExpressiveShapes else StandardShapes,
          typography = if (isM3Expressive) ExpressiveTypography else Typography,
          content = content
      )
  }
}

