package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

enum class AppThemeMode(val titleFa: String) {
    DARK("تاریک فیروزه‌ای"),
    LIGHT("روشن کاشی"),
    SYSTEM("مطابق سیستم")
}

@Immutable
data class HesabgarExtendedColors(
    val numberKeyBg: Color,
    val numberKeyText: Color,
    val functionKeyBg: Color,
    val functionKeyText: Color,
    val operatorKeyBg: Color,
    val operatorKeyText: Color,
    val equalsKeyBg: Color,
    val equalsKeyText: Color,
    val clearKeyBg: Color,
    val clearKeyText: Color,
    val displayCardBg: Color,
    val patternTint: Color
)

val LocalHesabgarColors = staticCompositionLocalOf {
    HesabgarExtendedColors(
        numberKeyBg = LapisContainer,
        numberKeyText = TextIvory,
        functionKeyBg = LapisElevated,
        functionKeyText = TextMist,
        operatorKeyBg = TurquoiseDeep,
        operatorKeyText = TurquoiseGlow,
        equalsKeyBg = SaffronGold,
        equalsKeyText = ObsidianDeep,
        clearKeyBg = CoralDeep,
        clearKeyText = PersianCoral,
        displayCardBg = LapisSurface,
        patternTint = TurquoiseGlow
    )
}

private val DarkColorScheme = darkColorScheme(
    primary = TurquoiseGlow,
    onPrimary = ObsidianDeep,
    primaryContainer = TurquoiseDeep,
    onPrimaryContainer = TurquoiseOnDeep,
    secondary = SaffronGold,
    onSecondary = ObsidianDeep,
    secondaryContainer = SaffronDeep,
    onSecondaryContainer = SaffronOnDeep,
    tertiary = TurquoiseOnDeep,
    background = ObsidianDeep,
    onBackground = TextIvory,
    surface = LapisSurface,
    onSurface = TextIvory,
    surfaceVariant = LapisContainer,
    onSurfaceVariant = TextMist,
    surfaceContainerLow = LapisSurface,
    surfaceContainer = LapisContainer,
    surfaceContainerHigh = LapisElevated,
    outline = OutlineDark,
    error = PersianCoral,
    onError = ObsidianDeep,
    errorContainer = CoralDeep,
    onErrorContainer = PersianCoral
)

private val LightColorScheme = lightColorScheme(
    primary = TurquoiseRich,
    onPrimary = WarmPaper,
    primaryContainer = TurquoiseTint,
    onPrimaryContainer = TurquoiseOnTint,
    secondary = SaffronRich,
    onSecondary = WarmPaper,
    secondaryContainer = SaffronTint,
    onSecondaryContainer = SaffronOnTint,
    tertiary = TurquoiseOnTint,
    background = AlabasterBg,
    onBackground = InkDark,
    surface = WarmPaper,
    onSurface = InkDark,
    surfaceVariant = SandstoneContainer,
    onSurfaceVariant = InkMuted,
    surfaceContainerLow = WarmPaper,
    surfaceContainer = SandstoneContainer,
    surfaceContainerHigh = SandstoneElevated,
    outline = OutlineLight,
    error = CoralRich,
    onError = WarmPaper,
    errorContainer = CoralTint,
    onErrorContainer = CoralRich
)

private val DarkExtendedColors = HesabgarExtendedColors(
    numberKeyBg = LapisContainer,
    numberKeyText = TextIvory,
    functionKeyBg = LapisElevated,
    functionKeyText = TurquoiseOnDeep,
    operatorKeyBg = TurquoiseDeep,
    operatorKeyText = TurquoiseGlow,
    equalsKeyBg = SaffronGold,
    equalsKeyText = ObsidianDeep,
    clearKeyBg = CoralDeep,
    clearKeyText = PersianCoral,
    displayCardBg = LapisSurface,
    patternTint = TurquoiseGlow
)

private val LightExtendedColors = HesabgarExtendedColors(
    numberKeyBg = WarmPaper,
    numberKeyText = InkDark,
    functionKeyBg = SandstoneElevated,
    functionKeyText = InkDark,
    operatorKeyBg = TurquoiseTint,
    operatorKeyText = TurquoiseRich,
    equalsKeyBg = SaffronRich,
    equalsKeyText = WarmPaper,
    clearKeyBg = CoralTint,
    clearKeyText = CoralRich,
    displayCardBg = WarmPaper,
    patternTint = TurquoiseRich
)

val HesabgarShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun HesabgarTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val colorScheme = if (useDark) DarkColorScheme else LightColorScheme
    val extendedColors = if (useDark) DarkExtendedColors else LightExtendedColors

    CompositionLocalProvider(LocalHesabgarColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = HesabgarShapes,
            content = content
        )
    }
}
