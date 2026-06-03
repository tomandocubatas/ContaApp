package com.contaapp.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF0D47A1),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8F1FF),
    onPrimaryContainer = Color(0xFF062B60),
    secondary = Color(0xFF0F766E),
    background = Color(0xFFF1F5F9),
    onBackground = Color(0xFF111827),
    surface = Color.White,
    onSurface = Color(0xFF111827),
    surfaceVariant = Color(0xFFE8EEF7),
    onSurfaceVariant = Color(0xFF475569),
    outline = Color(0xFFCBD5E1),
    error = Color(0xFFB91C1C)
)

private val DarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFF21D7A4),
    onPrimary = Color(0xFF04130F),
    primaryContainer = Color(0xFF073F39),
    onPrimaryContainer = Color(0xFFCFFAF0),
    secondary = Color(0xFF72B7FF),
    secondaryContainer = Color(0xFF102B48),
    onSecondaryContainer = Color(0xFFD8ECFF),
    background = Color(0xFF050B16),
    onBackground = Color(0xFFEAF2FF),
    surface = Color(0xFF0A1524),
    onSurface = Color(0xFFEAF2FF),
    surfaceVariant = Color(0xFF101E31),
    onSurfaceVariant = Color(0xFFB3C0D2),
    outline = Color(0xFF263B55),
    error = Color(0xFFFF7B7B)
)

private val AppTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = FontFamily.SansSerif),
        displayMedium = base.displayMedium.copy(fontFamily = FontFamily.SansSerif),
        displaySmall = base.displaySmall.copy(fontFamily = FontFamily.SansSerif),
        headlineLarge = base.headlineLarge.copy(fontFamily = FontFamily.SansSerif),
        headlineMedium = base.headlineMedium.copy(fontFamily = FontFamily.SansSerif),
        headlineSmall = base.headlineSmall.copy(fontFamily = FontFamily.SansSerif),
        titleLarge = base.titleLarge.copy(fontFamily = FontFamily.SansSerif),
        titleMedium = base.titleMedium.copy(fontFamily = FontFamily.SansSerif),
        titleSmall = base.titleSmall.copy(fontFamily = FontFamily.SansSerif),
        bodyLarge = base.bodyLarge.copy(fontFamily = FontFamily.SansSerif),
        bodyMedium = base.bodyMedium.copy(fontFamily = FontFamily.SansSerif),
        bodySmall = base.bodySmall.copy(fontFamily = FontFamily.SansSerif),
        labelLarge = base.labelLarge.copy(fontFamily = FontFamily.SansSerif),
        labelMedium = base.labelMedium.copy(fontFamily = FontFamily.SansSerif),
        labelSmall = base.labelSmall.copy(fontFamily = FontFamily.SansSerif)
    )
}

@Composable
fun ContaTheme(darkMode: Boolean? = null, content: @Composable () -> Unit) {
    val dark = darkMode ?: isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
