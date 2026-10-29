package com.contaapp.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

private val LightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD5F3EE),
    onPrimaryContainer = Color(0xFF053B36),
    secondary = Color(0xFF0D47A1),
    secondaryContainer = Color(0xFFE8F1FF),
    onSecondaryContainer = Color(0xFF062B60),
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

/** Colores de marca y de estado que no existen en [ColorScheme]; cada tema aporta su variante. */
data class ContaPalette(
    val coral: Color,
    val teal: Color,
    val amber: Color,
    val danger: Color,
    val ink: Color,
    val muted: Color,
    val surface: Color,
    val blue: Color,
    val sky: Color,
    val mint: Color,
    val orange: Color,
    val background: Color,
    val income: Color,
    val expense: Color,
    val warning: Color,
    val debt: Color
)

private val LightPalette = ContaPalette(
    coral = Color(0xFFFF5A52),
    teal = Color(0xFF0F766E),
    amber = Color(0xFFF59E0B),
    danger = Color(0xFFDC2626),
    ink = Color(0xFF0F172A),
    muted = Color(0xFF64748B),
    surface = Color.White,
    blue = Color(0xFF0D47A1),
    sky = Color(0xFF1E88E5),
    mint = Color(0xFF22C55E),
    orange = Color(0xFFF59E0B),
    background = Color(0xFFF1F5F9),
    income = Color(0xFF0F766E),
    expense = Color(0xFFE0463F),
    warning = Color(0xFFB45309),
    debt = Color(0xFFC2410C)
)

private val DarkPalette = ContaPalette(
    coral = Color(0xFFFF8A80),
    teal = Color(0xFF2DD4BF),
    amber = Color(0xFFFBBF24),
    danger = Color(0xFFFF7B7B),
    ink = Color(0xFFEAF2FF),
    muted = Color(0xFF94A3B8),
    surface = Color(0xFF0A1524),
    blue = Color(0xFF72B7FF),
    sky = Color(0xFF64B5F6),
    mint = Color(0xFF4ADE80),
    orange = Color(0xFFFBBF24),
    background = Color(0xFF050B16),
    income = Color(0xFF2DD4BF),
    expense = Color(0xFFFF8A80),
    warning = Color(0xFFFBBF24),
    debt = Color(0xFFFB923C)
)

val LocalContaPalette = staticCompositionLocalOf { LightPalette }

@Composable
fun ContaTheme(darkMode: Boolean? = null, content: @Composable () -> Unit) {
    val dark = darkMode ?: isSystemInDarkTheme()
    CompositionLocalProvider(LocalContaPalette provides if (dark) DarkPalette else LightPalette) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            typography = AppTypography,
            content = content
        )
    }
}
