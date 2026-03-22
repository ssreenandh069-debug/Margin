package com.Margin.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeonTeal,
    onPrimary = Color(0xFF003D35),
    primaryContainer = NeonTealAlpha20,
    onPrimaryContainer = NeonTeal,
    secondary = Magenta,
    onSecondary = Color(0xFF3D0020),
    secondaryContainer = MagentaAlpha20,
    onSecondaryContainer = Magenta,
    tertiary = Yellow,
    onTertiary = Color(0xFF3D3000),
    tertiaryContainer = YellowAlpha20,
    onTertiaryContainer = Yellow,
    error = AttendRed,
    errorContainer = AttendRedAlpha20,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextSecondary,
    outline = StrokeColor,
    outlineVariant = TextMuted,
    scrim = Color(0xCC000000)
)

private val LightColorScheme = lightColorScheme(
    primary = NeonTealDim,
    onPrimary = Color(0xFF003D35),
    primaryContainer = NeonTealAlpha20,
    onPrimaryContainer = NeonTealDim,
    secondary = Magenta,
    onSecondary = Color(0xFF3D0020),
    secondaryContainer = MagentaAlpha20,
    onSecondaryContainer = Magenta,
    tertiary = Yellow,
    onTertiary = Color(0xFF3D3000),
    tertiaryContainer = YellowAlpha20,
    onTertiaryContainer = Yellow,
    error = AttendRed,
    errorContainer = AttendRedAlpha20,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightCard,
    onSurfaceVariant = LightTextSecondary,
    outline = LightStroke,
    outlineVariant = LightTextMuted,
    scrim = Color(0x80000000)
)

@Composable
fun MarginTheme(
    isDarkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}

