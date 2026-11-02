package com.studyplanner.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.studyplanner.app.data.ThemeMode

fun parseHexColor(value: String, fallback: Color = Color(0xFF3B82F6)): Color {
    return runCatching {
        val hex = value.removePrefix("#")
        if (hex.length != 6) return@runCatching fallback
        Color((0xFF000000L or hex.toLong(16)).toInt())
    }.getOrDefault(fallback)
}

private fun onAccent(accent: Color): Color =
    if (accent.luminance() > 0.55f) Color(0xFF111827) else Color.White

private fun lightScheme(accent: Color) = lightColorScheme(
    primary = accent,
    onPrimary = onAccent(accent),
    primaryContainer = lerp(accent, Color.White, 0.82f),
    onPrimaryContainer = lerp(accent, Color.Black, 0.55f),
    inversePrimary = lerp(accent, Color.White, 0.35f),
    secondary = accent,
    onSecondary = onAccent(accent),
    secondaryContainer = lerp(accent, Color.White, 0.86f),
    onSecondaryContainer = lerp(accent, Color.Black, 0.6f),
    tertiary = Color(0xFF8B5CF6),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEDE9FE),
    onTertiaryContainer = Color(0xFF4C1D95),
    background = Color(0xFFF7F7F9),
    onBackground = Color(0xFF18181B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF18181B),
    surfaceVariant = Color(0xFFF4F4F5),
    onSurfaceVariant = Color(0xFF71717A),
    surfaceTint = accent,
    inverseSurface = Color(0xFF27272A),
    inverseOnSurface = Color(0xFFFAFAFA),
    error = Color(0xFFEF4444),
    onError = Color.White,
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D),
    outline = Color(0xFFE4E4E7),
    outlineVariant = Color(0xFFF0F0F0),
    scrim = Color(0x66000000),
)

private fun darkScheme(accent: Color) = darkColorScheme(
    primary = accent,
    onPrimary = onAccent(accent),
    primaryContainer = lerp(accent, Color(0xFF09090B), 0.68f),
    onPrimaryContainer = lerp(accent, Color.White, 0.78f),
    inversePrimary = lerp(accent, Color.Black, 0.25f),
    secondary = accent,
    onSecondary = onAccent(accent),
    secondaryContainer = lerp(accent, Color(0xFF09090B), 0.72f),
    onSecondaryContainer = lerp(accent, Color.White, 0.8f),
    tertiary = Color(0xFFA78BFA),
    onTertiary = Color(0xFF1E1B4B),
    tertiaryContainer = Color(0xFF3730A3),
    onTertiaryContainer = Color(0xFFE0E7FF),
    background = Color(0xFF09090B),
    onBackground = Color(0xFFFAFAFA),
    surface = Color(0xFF18181B),
    onSurface = Color(0xFFFAFAFA),
    surfaceVariant = Color(0xFF27272A),
    onSurfaceVariant = Color(0xFFA1A1AA),
    surfaceTint = accent,
    inverseSurface = Color(0xFFFAFAFA),
    inverseOnSurface = Color(0xFF18181B),
    error = Color(0xFFF87171),
    onError = Color(0xFF450A0A),
    errorContainer = Color(0xFF7F1D1D),
    onErrorContainer = Color(0xFFFEE2E2),
    outline = Color(0xFF3F3F46),
    outlineVariant = Color(0xFF27272A),
    scrim = Color(0x99000000),
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 36.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
    ),
)

@Composable
fun StudyPlannerTheme(
    themeMode: ThemeMode,
    accentHex: String,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val accent = remember(accentHex) { parseHexColor(accentHex) }
    val colorScheme = remember(accent, darkTheme) {
        if (darkTheme) darkScheme(accent) else lightScheme(accent)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity ?: return@SideEffect
            val window = activity.window
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}

val TimerFontFamily: FontFamily = FontFamily.Monospace

fun Color.asHex(): String = String.format("#%06X", 0xFFFFFF and toArgb())
