package org.bhargav.pansariwala.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import org.bhargav.pansariwala.settings.CustomTheme

private val lightScheme = lightColorScheme(
    primary = primaryLight,
    onPrimary = onPrimaryLight,
    primaryContainer = primaryContainerLight,
    onPrimaryContainer = onPrimaryContainerLight,
    secondary = secondaryLight,
    onSecondary = onSecondaryLight,
    secondaryContainer = secondaryContainerLight,
    onSecondaryContainer = onSecondaryContainerLight,
    tertiary = tertiaryLight,
    onTertiary = onTertiaryLight,
    tertiaryContainer = tertiaryContainerLight,
    onTertiaryContainer = onTertiaryContainerLight,
    error = errorLight,
    onError = onErrorLight,
    errorContainer = errorContainerLight,
    onErrorContainer = onErrorContainerLight,
    background = backgroundLight,
    onBackground = onBackgroundLight,
    surface = surfaceLight,
    onSurface = onSurfaceLight,
    surfaceVariant = surfaceVariantLight,
    onSurfaceVariant = onSurfaceVariantLight,
    outline = outlineLight,
    outlineVariant = outlineVariantLight,
    scrim = scrimLight,
    inverseSurface = inverseSurfaceLight,
    inverseOnSurface = inverseOnSurfaceLight,
    inversePrimary = inversePrimaryLight,
    surfaceDim = surfaceDimLight,
    surfaceBright = surfaceBrightLight,
    surfaceContainerLowest = surfaceContainerLowestLight,
    surfaceContainerLow = surfaceContainerLowLight,
    surfaceContainer = surfaceContainerLight,
    surfaceContainerHigh = surfaceContainerHighLight,
    surfaceContainerHighest = surfaceContainerHighestLight,
)

private val darkScheme = darkColorScheme(
    primary = primaryDark,
    onPrimary = onPrimaryDark,
    primaryContainer = primaryContainerDark,
    onPrimaryContainer = onPrimaryContainerDark,
    secondary = secondaryDark,
    onSecondary = onSecondaryDark,
    secondaryContainer = secondaryContainerDark,
    onSecondaryContainer = onSecondaryContainerDark,
    tertiary = tertiaryDark,
    onTertiary = onTertiaryDark,
    tertiaryContainer = tertiaryContainerDark,
    onTertiaryContainer = onTertiaryContainerDark,
    error = errorDark,
    onError = onErrorDark,
    errorContainer = errorContainerDark,
    onErrorContainer = onErrorContainerDark,
    background = backgroundDark,
    onBackground = onBackgroundDark,
    surface = surfaceDark,
    onSurface = onSurfaceDark,
    surfaceVariant = surfaceVariantDark,
    onSurfaceVariant = onSurfaceVariantDark,
    outline = outlineDark,
    outlineVariant = outlineVariantDark,
    scrim = scrimDark,
    inverseSurface = inverseSurfaceDark,
    inverseOnSurface = inverseOnSurfaceDark,
    inversePrimary = inversePrimaryDark,
    surfaceDim = surfaceDimDark,
    surfaceBright = surfaceBrightDark,
    surfaceContainerLowest = surfaceContainerLowestDark,
    surfaceContainerLow = surfaceContainerLowDark,
    surfaceContainer = surfaceContainerDark,
    surfaceContainerHigh = surfaceContainerHighDark,
    surfaceContainerHighest = surfaceContainerHighestDark,
)

@Composable
fun PansariTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    customTheme: CustomTheme = CustomTheme.DEFAULT,
    content: @Composable () -> Unit,
) {
    val base = if (darkTheme) darkScheme else lightScheme
    MaterialTheme(
        colorScheme = base.withCustomTheme(customTheme, darkTheme),
        typography = AppTypography(),
        content = content,
    )
}

private fun ColorScheme.withCustomTheme(theme: CustomTheme, dark: Boolean): ColorScheme {
    if (theme == CustomTheme.DEFAULT) return this
    val accents = when (theme) {
        CustomTheme.DEFAULT -> return this
        CustomTheme.SAFFRON -> if (dark) {
            Accent(Color(0xFFFFB95C), Color(0xFF4A2800), Color(0xFF6B3B00), Color(0xFFFFDDB8))
        } else {
            Accent(Color(0xFF8B5000), Color(0xFFFFFFFF), Color(0xFFFFDDB8), Color(0xFF2D1600))
        }
        CustomTheme.TEAL -> if (dark) {
            Accent(Color(0xFF4DD0C4), Color(0xFF003732), Color(0xFF005049), Color(0xFF6FF7E9))
        } else {
            Accent(Color(0xFF006A63), Color(0xFFFFFFFF), Color(0xFF6FF7E9), Color(0xFF00201D))
        }
        CustomTheme.ROSE -> if (dark) {
            Accent(Color(0xFFFFB2BB), Color(0xFF5F001A), Color(0xFF8E0029), Color(0xFFFFD9DD))
        } else {
            Accent(Color(0xFFB01E45), Color(0xFFFFFFFF), Color(0xFFFFD9DD), Color(0xFF400012))
        }
    }
    return copy(
        primary = accents.primary,
        onPrimary = accents.onPrimary,
        primaryContainer = accents.primaryContainer,
        onPrimaryContainer = accents.onPrimaryContainer,
        inversePrimary = accents.primary,
    )
}

private data class Accent(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
)
