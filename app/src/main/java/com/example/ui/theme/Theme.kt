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

fun getColorSchemeForTheme(themeColor: String, darkTheme: Boolean): androidx.compose.material3.ColorScheme {
    return when (themeColor) {
        "forest_green" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFF34D399),
                    onPrimary = Color(0xFF003300),
                    primaryContainer = Color(0xFF047857),
                    onPrimaryContainer = Color(0xFFE8F5E9),
                    secondary = Color(0xFFD1FAE5),
                    onSecondary = Color(0xFF047857),
                    secondaryContainer = Color(0xFF059669),
                    onSecondaryContainer = Color(0xFFE8F5E9),
                    background = Color(0xFF000000),
                    surface = Color(0xFF000000),
                    surfaceVariant = Color(0xFF3E4A3E),
                    onSurface = Color(0xFFE2E6E2)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF059669),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFD1FAE5),
                    onPrimaryContainer = Color(0xFF002200),
                    secondary = Color(0xFF10B981),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFE8F5E9),
                    onSecondaryContainer = Color(0xFF047857),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0xFFE2EAE2),
                    onSurface = Color(0xFF121512)
                )
            }
        }
        "ocean_blue" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFF38BDF8),
                    onPrimary = Color(0xFF0D47A1),
                    primaryContainer = Color(0xFF0284C7),
                    onPrimaryContainer = Color(0xFFE3F2FD),
                    secondary = Color(0xFFE0F2FE),
                    onSecondary = Color(0xFF0D47A1),
                    secondaryContainer = Color(0xFF1976D2),
                    onSecondaryContainer = Color(0xFFE3F2FD),
                    background = Color(0xFF000000),
                    surface = Color(0xFF000000),
                    surfaceVariant = Color(0xFF3A424F),
                    onSurface = Color(0xFFE0E5ED)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF0284C7),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFE0F2FE),
                    onPrimaryContainer = Color(0xFF0D2140),
                    secondary = Color(0xFF0EA5E9),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFE3F2FD),
                    onSecondaryContainer = Color(0xFF0D47A1),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0xFFE5ECF6),
                    onSurface = Color(0xFF121417)
                )
            }
        }
        "sunset_orange" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFFBBF24),
                    onPrimary = Color(0xFFD97706),
                    primaryContainer = Color(0xFFB45309),
                    onPrimaryContainer = Color(0xFFFFF3E0),
                    secondary = Color(0xFFFEF3C7),
                    onSecondary = Color(0xFFD97706),
                    secondaryContainer = Color(0xFFF4511E),
                    onSecondaryContainer = Color(0xFFFFF3E0),
                    background = Color(0xFF000000),
                    surface = Color(0xFF000000),
                    surfaceVariant = Color(0xFF4C3E3A),
                    onSurface = Color(0xFFECE0DD)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFFD97706),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFFEF3C7),
                    onPrimaryContainer = Color(0xFF3E1200),
                    secondary = Color(0xFFEA580C),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFFFF3E0),
                    onSecondaryContainer = Color(0xFFD97706),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0xFFECE2DE),
                    onSurface = Color(0xFF171312)
                )
            }
        }
        "lavender_purple" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFA78BFA),
                    onPrimary = Color(0xFF4A148C),
                    primaryContainer = Color(0xFF7C3AED),
                    onPrimaryContainer = Color(0xFFF3E5F5),
                    secondary = Color(0xFFEDE9FE),
                    onSecondary = Color(0xFF4A148C),
                    secondaryContainer = Color(0xFF8E24AA),
                    onSecondaryContainer = Color(0xFFF3E5F5),
                    background = Color(0xFF000000),
                    surface = Color(0xFF000000),
                    surfaceVariant = Color(0xFF453D4A),
                    onSurface = Color(0xFFE6E0E8)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF7C3AED),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFEDE9FE),
                    onPrimaryContainer = Color(0xFF2E004F),
                    secondary = Color(0xFF6D28D9),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFF3E5F5),
                    onSecondaryContainer = Color(0xFF4A148C),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0xFFE6E1EB),
                    onSurface = Color(0xFF151217)
                )
            }
        }
        "dark_crimson" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFFB7185),
                    onPrimary = Color(0xFF7F0000),
                    primaryContainer = Color(0xFFE11D48),
                    onPrimaryContainer = Color(0xFFFFEBEE),
                    secondary = Color(0xFFFFE4E6),
                    onSecondary = Color(0xFF7F0000),
                    secondaryContainer = Color(0xFFC62828),
                    onSecondaryContainer = Color(0xFFFFEBEE),
                    background = Color(0xFF000000),
                    surface = Color(0xFF000000),
                    surfaceVariant = Color(0xFF4C3D3D),
                    onSurface = Color(0xFFECE0E0)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFFE11D48),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFFFE4E6),
                    onPrimaryContainer = Color(0xFF3E0000),
                    secondary = Color(0xFFBE123C),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFFFEBEE),
                    onSecondaryContainer = Color(0xFF7F0000),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0xFFECE1E1),
                    onSurface = Color(0xFF171212)
                )
            }
        }
        "natural_gray" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFE5E5E5),
                    onPrimary = Color(0xFF212121),
                    primaryContainer = Color(0xFF525252),
                    onPrimaryContainer = Color(0xFFF5F5F5),
                    secondary = Color(0xFFBDBDBD),
                    onSecondary = Color(0xFF212121),
                    secondaryContainer = Color(0xFF303030),
                    onSecondaryContainer = Color(0xFFEEEEEE),
                    background = Color(0xFF000000),
                    surface = Color(0xFF000000),
                    surfaceVariant = Color(0xFF333333),
                    onSurface = Color(0xFFE5E5E5)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF525252),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFE5E5E5),
                    onPrimaryContainer = Color(0xFF1F1F1F),
                    secondary = Color(0xFF737373),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFF5F5F5),
                    onSecondaryContainer = Color(0xFF212121),
                    background = Color(0xFFFFFFFF),
                    surface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0xFFE5E5E5),
                    onSurface = Color(0xFF212121)
                )
            }
        }
        else -> {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
    }
}

val LocalM3Expressive = staticCompositionLocalOf { true }
val LocalImageToolboxStyle = staticCompositionLocalOf { false }

val ImageToolboxShapes = Shapes(
    extraSmall = RoundedCornerShape(14.dp),
    small = RoundedCornerShape(18.dp),
    medium = RoundedCornerShape(26.dp),
    large = RoundedCornerShape(34.dp),
    extraLarge = RoundedCornerShape(44.dp)
)

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

fun getImageToolboxColorScheme(palette: String, darkTheme: Boolean): ColorScheme {
    return when (palette) {
        "oled_obsidian" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFFFFFFF),
                    onPrimary = Color(0xFF000000),
                    primaryContainer = Color(0xFF1E2130),
                    onPrimaryContainer = Color(0xFFFFFFFF),
                    secondary = Color(0xFF9CA3AF),
                    onSecondary = Color(0xFF000000),
                    secondaryContainer = Color(0xFF11131E),
                    onSecondaryContainer = Color(0xFFE5E7EB),
                    tertiary = Color(0xFFD1D5DB),
                    onTertiary = Color(0xFF000000),
                    background = Color(0xFF000000),
                    surface = Color(0xFF0A0C10),
                    surfaceVariant = Color(0xFF151821),
                    onSurface = Color(0xFFF3F4F6),
                    outline = Color(0xFF2A2E3D)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF111827),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFE5E7EB),
                    onPrimaryContainer = Color(0xFF111827),
                    secondary = Color(0xFF4B5563),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFF3F4F6),
                    onSecondaryContainer = Color(0xFF111827),
                    tertiary = Color(0xFF374151),
                    onTertiary = Color(0xFFFFFFFF),
                    background = Color(0xFFF3F4F6),
                    surface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0xFFE5E7EB),
                    onSurface = Color(0xFF111827),
                    outline = Color(0xFF9CA3AF)
                )
            }
        }
        "nordic_forest" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFF86EFAC),
                    onPrimary = Color(0xFF022C22),
                    primaryContainer = Color(0xFF065F46),
                    onPrimaryContainer = Color(0xFFD1FAE5),
                    secondary = Color(0xFFA7F3D0),
                    onSecondary = Color(0xFF042F1A),
                    secondaryContainer = Color(0xFF0F3E26),
                    onSecondaryContainer = Color(0xFFE6FDF4),
                    tertiary = Color(0xFF6EE7B7),
                    onTertiary = Color(0xFF022C22),
                    background = Color(0xFF0D120F),
                    surface = Color(0xFF141A16),
                    surfaceVariant = Color(0xFF1D2821),
                    onSurface = Color(0xFFF0FDF4),
                    outline = Color(0xFF2F4538)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF065F46),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFD1FAE5),
                    onPrimaryContainer = Color(0xFF042F1A),
                    secondary = Color(0xFF0F5132),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFD2E7DF),
                    onSecondaryContainer = Color(0xFF082E1B),
                    tertiary = Color(0xFF1E5E3A),
                    onTertiary = Color(0xFFFFFFFF),
                    background = Color(0xFFF2F7F4),
                    surface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0xFFE1ECE6),
                    onSurface = Color(0xFF0F1A14),
                    outline = Color(0xFF829A8E)
                )
            }
        }
        "lavender_dusk" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFDDD6FE),
                    onPrimary = Color(0xFF2E1065),
                    primaryContainer = Color(0xFF5B21B6),
                    onPrimaryContainer = Color(0xFFF5F3FF),
                    secondary = Color(0xFFC7D2FE),
                    onSecondary = Color(0xFF1E1B4B),
                    secondaryContainer = Color(0xFF3730A3),
                    onSecondaryContainer = Color(0xFFEEF2FF),
                    tertiary = Color(0xFFC084FC),
                    onTertiary = Color(0xFF3B0764),
                    background = Color(0xFF0B0914),
                    surface = Color(0xFF110E21),
                    surfaceVariant = Color(0xFF1B1733),
                    onSurface = Color(0xFFF5F3FF),
                    outline = Color(0xFF322E4E)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF5B21B6),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFEDE9FE),
                    onPrimaryContainer = Color(0xFF1E1B4B),
                    secondary = Color(0xFF4338CA),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFE0E7FF),
                    onSecondaryContainer = Color(0xFF1E1B4B),
                    tertiary = Color(0xFF7C3AED),
                    onTertiary = Color(0xFFFFFFFF),
                    background = Color(0xFFF5F3F7),
                    surface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0xFFEDE9FE),
                    onSurface = Color(0xFF1E152A),
                    outline = Color(0xFF9086B1)
                )
            }
        }
        "terracotta_desert" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFFDBA74),
                    onPrimary = Color(0xFF451A03),
                    primaryContainer = Color(0xFF7C2D12),
                    onPrimaryContainer = Color(0xFFFFEDD5),
                    secondary = Color(0xFFFCD34D),
                    onSecondary = Color(0xFF451A03),
                    secondaryContainer = Color(0xFF78350F),
                    onSecondaryContainer = Color(0xFFFEF3C7),
                    tertiary = Color(0xFFFDA4AF),
                    onTertiary = Color(0xFF4C0519),
                    background = Color(0xFF130E0B),
                    surface = Color(0xFF1C1410),
                    surfaceVariant = Color(0xFF2D201A),
                    onSurface = Color(0xFFFFF7ED),
                    outline = Color(0xFF47342C)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF9E4E2A),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFFFEDD5),
                    onPrimaryContainer = Color(0xFF431407),
                    secondary = Color(0xFFB45309),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFFEF3C7),
                    onSecondaryContainer = Color(0xFF451A03),
                    tertiary = Color(0xFFBE123C),
                    onTertiary = Color(0xFFFFFFFF),
                    background = Color(0xFFFDF8F5),
                    surface = Color(0xFFFFFFFF),
                    surfaceVariant = Color(0xFFF5EBE6),
                    onSurface = Color(0xFF24140D),
                    outline = Color(0xFFA38C82)
                )
            }
        }
        else -> {
            if (darkTheme) DarkColorScheme else LightColorScheme
        }
    }
}

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
  themeColor: String = "classic_slate",
  isM3Expressive: Boolean = true,
  isImageToolboxStyle: Boolean = false,
  imageToolboxPalette: String = "oled_obsidian",
  content: @Composable () -> Unit,
) {
  var colorScheme =
    when {
      isImageToolboxStyle -> getImageToolboxColorScheme(imageToolboxPalette, darkTheme)
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      else -> getColorSchemeForTheme(themeColor, darkTheme)
    }

  if (isM3Expressive && !isImageToolboxStyle) {
      colorScheme = getExpressiveColorScheme(colorScheme, darkTheme)
  }

  CompositionLocalProvider(
      LocalM3Expressive provides isM3Expressive,
      LocalImageToolboxStyle provides isImageToolboxStyle
  ) {
      MaterialTheme(
          colorScheme = colorScheme,
          shapes = when {
              isImageToolboxStyle -> ImageToolboxShapes
              isM3Expressive -> ExpressiveShapes
              else -> StandardShapes
          },
          typography = if (isM3Expressive) ExpressiveTypography else Typography,
          content = content
      )
  }
}

@Composable
fun Modifier.imageToolboxBorder(
    shape: androidx.compose.ui.graphics.Shape? = null
): Modifier {
    val enabled = LocalImageToolboxStyle.current
    val targetShape = shape ?: MaterialTheme.shapes.medium
    return if (enabled) {
        this.border(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.22f),
            shape = targetShape
        )
    } else {
        this
    }
}
