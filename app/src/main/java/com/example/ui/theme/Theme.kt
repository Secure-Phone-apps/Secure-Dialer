package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
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
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
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
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
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
                    primary = Color(0xFFA5D6A7),
                    onPrimary = Color(0xFF003300),
                    primaryContainer = Color(0xFF1B5E20),
                    onPrimaryContainer = Color(0xFFE8F5E9),
                    secondary = Color(0xFFC8E6C9),
                    onSecondary = Color(0xFF1B5E20),
                    secondaryContainer = Color(0xFF2E7D32),
                    onSecondaryContainer = Color(0xFFE8F5E9),
                    background = Color(0xFF121512),
                    surface = Color(0xFF121512),
                    surfaceVariant = Color(0xFF3E4A3E),
                    onSurface = Color(0xFFE2E6E2)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF2E7D32),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFC8E6C9),
                    onPrimaryContainer = Color(0xFF002200),
                    secondary = Color(0xFF4CAF50),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFE8F5E9),
                    onSecondaryContainer = Color(0xFF1B5E20),
                    background = Color(0xFFF9FBF9),
                    surface = Color(0xFFF9FBF9),
                    surfaceVariant = Color(0xFFE2EAE2),
                    onSurface = Color(0xFF121512)
                )
            }
        }
        "ocean_blue" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFF90CAF9),
                    onPrimary = Color(0xFF0D47A1),
                    primaryContainer = Color(0xFF1565C0),
                    onPrimaryContainer = Color(0xFFE3F2FD),
                    secondary = Color(0xFFBBDEFB),
                    onSecondary = Color(0xFF0D47A1),
                    secondaryContainer = Color(0xFF1976D2),
                    onSecondaryContainer = Color(0xFFE3F2FD),
                    background = Color(0xFF121417),
                    surface = Color(0xFF121417),
                    surfaceVariant = Color(0xFF3A424F),
                    onSurface = Color(0xFFE0E5ED)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF1565C0),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFBBDEFB),
                    onPrimaryContainer = Color(0xFF0D2140),
                    secondary = Color(0xFF2196F3),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFE3F2FD),
                    onSecondaryContainer = Color(0xFF0D47A1),
                    background = Color(0xFFFAFBFC),
                    surface = Color(0xFFFAFBFC),
                    surfaceVariant = Color(0xFFE5ECF6),
                    onSurface = Color(0xFF121417)
                )
            }
        }
        "sunset_orange" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFFFCC80),
                    onPrimary = Color(0xFFE65100),
                    primaryContainer = Color(0xFFD84315),
                    onPrimaryContainer = Color(0xFFFFF3E0),
                    secondary = Color(0xFFFFE0B2),
                    onSecondary = Color(0xFFE65100),
                    secondaryContainer = Color(0xFFF4511E),
                    onSecondaryContainer = Color(0xFFFFF3E0),
                    background = Color(0xFF171312),
                    surface = Color(0xFF171312),
                    surfaceVariant = Color(0xFF4C3E3A),
                    onSurface = Color(0xFFECE0DD)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFFE65100),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFFFE0B2),
                    onPrimaryContainer = Color(0xFF3E1200),
                    secondary = Color(0xFFFF9800),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFFFF3E0),
                    onSecondaryContainer = Color(0xFFE65100),
                    background = Color(0xFFFCFAF9),
                    surface = Color(0xFFFCFAF9),
                    surfaceVariant = Color(0xFFECE2DE),
                    onSurface = Color(0xFF171312)
                )
            }
        }
        "lavender_purple" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFCE93D8),
                    onPrimary = Color(0xFF4A148C),
                    primaryContainer = Color(0xFF7B1FA2),
                    onPrimaryContainer = Color(0xFFF3E5F5),
                    secondary = Color(0xFFE1BEE7),
                    onSecondary = Color(0xFF4A148C),
                    secondaryContainer = Color(0xFF8E24AA),
                    onSecondaryContainer = Color(0xFFF3E5F5),
                    background = Color(0xFF151217),
                    surface = Color(0xFF151217),
                    surfaceVariant = Color(0xFF453D4A),
                    onSurface = Color(0xFFE6E0E8)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFF7B1FA2),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFE1BEE7),
                    onPrimaryContainer = Color(0xFF2E004F),
                    secondary = Color(0xFF9C27B0),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFF3E5F5),
                    onSecondaryContainer = Color(0xFF4A148C),
                    background = Color(0xFFFAF9FC),
                    surface = Color(0xFFFAF9FC),
                    surfaceVariant = Color(0xFFE6E1EB),
                    onSurface = Color(0xFF151217)
                )
            }
        }
        "dark_crimson" -> {
            if (darkTheme) {
                darkColorScheme(
                    primary = Color(0xFFEF9A9A),
                    onPrimary = Color(0xFF7F0000),
                    primaryContainer = Color(0xFFB71C1C),
                    onPrimaryContainer = Color(0xFFFFEBEE),
                    secondary = Color(0xFFFFCDD2),
                    onSecondary = Color(0xFF7F0000),
                    secondaryContainer = Color(0xFFC62828),
                    onSecondaryContainer = Color(0xFFFFEBEE),
                    background = Color(0xFF171212),
                    surface = Color(0xFF171212),
                    surfaceVariant = Color(0xFF4C3D3D),
                    onSurface = Color(0xFFECE0E0)
                )
            } else {
                lightColorScheme(
                    primary = Color(0xFFB71C1C),
                    onPrimary = Color(0xFFFFFFFF),
                    primaryContainer = Color(0xFFFFCDD2),
                    onPrimaryContainer = Color(0xFF3E0000),
                    secondary = Color(0xFFD32F2F),
                    onSecondary = Color(0xFFFFFFFF),
                    secondaryContainer = Color(0xFFFFEBEE),
                    onSecondaryContainer = Color(0xFF7F0000),
                    background = Color(0xFFFCF9F9),
                    surface = Color(0xFFFCF9F9),
                    surfaceVariant = Color(0xFFECE1E1),
                    onSurface = Color(0xFF171212)
                )
            }
        }
        else -> {
            if (darkTheme) DarkColorScheme else LightColorScheme
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
  themeColor: String = "classic_slate",
  isM3Expressive: Boolean = true,
  content: @Composable () -> Unit,
) {
  var colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      else -> getColorSchemeForTheme(themeColor, darkTheme)
    }

  if (isM3Expressive) {
      colorScheme = getExpressiveColorScheme(colorScheme, darkTheme)
  }

  CompositionLocalProvider(LocalM3Expressive provides isM3Expressive) {
      MaterialTheme(
          colorScheme = colorScheme,
          shapes = if (isM3Expressive) ExpressiveShapes else StandardShapes,
          typography = if (isM3Expressive) ExpressiveTypography else Typography,
          content = content
      )
  }
}
