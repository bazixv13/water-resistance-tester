package com.raywdude.waterresistance.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

/**
 * Semantic colors that Material's color scheme has no role for: a positive "seals intact" state
 * and a "reading is noisy" warning.
 */
@Immutable
data class SealAccents(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val cautionContainer: Color,
    val onCautionContainer: Color,
)

private val LightAccents = SealAccents(
    success = Color(0xFF166B45),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFFB7F3CF),
    onSuccessContainer = Color(0xFF00210F),
    cautionContainer = Color(0xFFFFE3A3),
    onCautionContainer = Color(0xFF3B2B00),
)

private val DarkAccents = SealAccents(
    success = Color(0xFF9AD7B2),
    onSuccess = Color(0xFF00391F),
    successContainer = Color(0xFF0B4F30),
    onSuccessContainer = Color(0xFFB7F3CF),
    cautionContainer = Color(0xFF574300),
    onCautionContainer = Color(0xFFFFE3A3),
)

val LocalSealAccents = staticCompositionLocalOf { LightAccents }

private val LightColors = lightColorScheme(
    primary = Color(0xFF00696E),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF9CF1F1),
    onPrimaryContainer = Color(0xFF002021),
    secondary = Color(0xFF4A6363),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCCE8E7),
    onSecondaryContainer = Color(0xFF051F1F),
    tertiary = Color(0xFF45617D),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFD0E4FF),
    onTertiaryContainer = Color(0xFF001D34),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Color(0xFFF4FAF9),
    onBackground = Color(0xFF161D1D),
    surface = Color(0xFFF4FAF9),
    onSurface = Color(0xFF161D1D),
    surfaceVariant = Color(0xFFDAE4E4),
    onSurfaceVariant = Color(0xFF3F4949),
    outline = Color(0xFF6F7979),
    outlineVariant = Color(0xFFBEC8C8),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEEF5F4),
    surfaceContainer = Color(0xFFE8EFEE),
    surfaceContainerHigh = Color(0xFFE3E9E9),
    surfaceContainerHighest = Color(0xFFDDE4E3),
    surfaceBright = Color(0xFFF4FAF9),
    surfaceDim = Color(0xFFD4DBDB),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF80D5D5),
    onPrimary = Color(0xFF003738),
    primaryContainer = Color(0xFF004F51),
    onPrimaryContainer = Color(0xFF9CF1F1),
    secondary = Color(0xFFB0CCCB),
    onSecondary = Color(0xFF1B3534),
    secondaryContainer = Color(0xFF324B4B),
    onSecondaryContainer = Color(0xFFCCE8E7),
    tertiary = Color(0xFFB4C8E5),
    onTertiary = Color(0xFF17324B),
    tertiaryContainer = Color(0xFF2E4963),
    onTertiaryContainer = Color(0xFFD0E4FF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF0B1413),
    onBackground = Color(0xFFDDE4E3),
    surface = Color(0xFF0B1413),
    onSurface = Color(0xFFDDE4E3),
    surfaceVariant = Color(0xFF3F4949),
    onSurfaceVariant = Color(0xFFBEC8C8),
    outline = Color(0xFF889392),
    outlineVariant = Color(0xFF3F4949),
    surfaceContainerLowest = Color(0xFF060E0E),
    surfaceContainerLow = Color(0xFF131C1B),
    surfaceContainer = Color(0xFF17201F),
    surfaceContainerHigh = Color(0xFF212A29),
    surfaceContainerHighest = Color(0xFF2C3534),
    surfaceBright = Color(0xFF323C3B),
    surfaceDim = Color(0xFF0B1413),
)

/**
 * Material 3 Expressive theme: expressive motion plus the expressive color roles. On Android 12+
 * the scheme follows the wallpaper (Material You); the hand-tuned teal scheme is the fallback.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SealTestTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColors
        else -> LightColors
    }

    CompositionLocalProvider(
        LocalSealAccents provides if (darkTheme) DarkAccents else LightAccents
    ) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            motionScheme = MotionScheme.expressive(),
            content = content,
        )
    }
}
