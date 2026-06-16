package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldAccent,
    secondary = CyanAccent,
    tertiary = ArtisticLightPurple,
    background = ObsidianBg,
    surface = CardBackground,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = LightGrayText,
    onSurface = LightGrayText,
    surfaceVariant = ArtisticNavBg,
    onSurfaceVariant = CoolGrayMuted,
    error = ErrorColor
)

private val HighContrastColorScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),      // Brighter neon purple
    secondary = Color(0xFF80D0C7),
    tertiary = Color(0xFFE8DEF8),
    background = Color(0xFF000000),   // Pure black for high contrast dark accessibility
    surface = Color(0xFF121212),      // Extremely high-contrast dark gray surface
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,       // Pure white content for readability
    onSurface = Color.White,          // Pure white content for card surfaces
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color.White,
    error = Color(0xFFF2B8B5)
)

@Composable
fun MyApplicationTheme(
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (highContrast) HighContrastColorScheme else DarkColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
