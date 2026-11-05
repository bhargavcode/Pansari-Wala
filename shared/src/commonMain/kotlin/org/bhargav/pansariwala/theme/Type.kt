package org.bhargav.pansariwala.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.isSpecified
import org.jetbrains.compose.resources.Font
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.montserrat_bold
import pansariwala.shared.generated.resources.montserrat_medium
import pansariwala.shared.generated.resources.montserrat_regular
import pansariwala.shared.generated.resources.montserrat_semibold

/** Smallest window side (dp) → type scale, so text stays proportional from small phones to tablets / desktop. */
private const val SMALL_PHONE_MAX_DP = 360f
private const val PHONE_MAX_DP = 600f
private const val SMALL_TABLET_MAX_DP = 840f
private const val SMALL_PHONE_SCALE = 0.9f
private const val PHONE_SCALE = 1f
private const val SMALL_TABLET_SCALE = 1.06f
private const val LARGE_SCREEN_SCALE = 1.12f

/** OS accessibility font size is honoured only within this range; larger sizes break dense POS layouts. */
private const val MIN_SYSTEM_FONT_SCALE = 0.85f
private const val MAX_SYSTEM_FONT_SCALE = 1.15f

@Composable
fun montserratFontFamily(): FontFamily = FontFamily(
    Font(Res.font.montserrat_regular, weight = FontWeight.Normal),
    Font(Res.font.montserrat_medium, weight = FontWeight.Medium),
    Font(Res.font.montserrat_semibold, weight = FontWeight.SemiBold),
    Font(Res.font.montserrat_bold, weight = FontWeight.Bold),
)

/** Type scale for the current window size; [userScale] is the in-app font size setting. */
@Composable
fun screenTypeScale(userScale: Float): Float {
    val window = LocalWindowInfo.current.containerSize
    val density = LocalDensity.current.density
    val shortSideDp = minOf(window.width, window.height) / density
    val device = when {
        shortSideDp <= 0f -> PHONE_SCALE
        shortSideDp < SMALL_PHONE_MAX_DP -> SMALL_PHONE_SCALE
        shortSideDp < PHONE_MAX_DP -> PHONE_SCALE
        shortSideDp < SMALL_TABLET_MAX_DP -> SMALL_TABLET_SCALE
        else -> LARGE_SCREEN_SCALE
    }
    return device * userScale
}

/** [Density] with the OS font scale clamped to a range the layouts are designed for. */
@Composable
fun clampedFontDensity(): Density {
    val current = LocalDensity.current
    return remember(current.density, current.fontScale) {
        Density(current.density, current.fontScale.coerceIn(MIN_SYSTEM_FONT_SCALE, MAX_SYSTEM_FONT_SCALE))
    }
}

@Composable
fun AppTypography(scale: Float = 1f): Typography {
    val family = montserratFontFamily()
    return remember(family, scale) {
        val baseline = Typography()
        fun TextStyle.themed() = copy(
            fontFamily = family,
            fontSize = fontSize.scaled(scale),
            lineHeight = lineHeight.scaled(scale),
        )
        Typography(
            displayLarge = baseline.displayLarge.themed(),
            displayMedium = baseline.displayMedium.themed(),
            displaySmall = baseline.displaySmall.themed(),
            headlineLarge = baseline.headlineLarge.themed(),
            headlineMedium = baseline.headlineMedium.themed(),
            headlineSmall = baseline.headlineSmall.themed(),
            titleLarge = baseline.titleLarge.themed(),
            titleMedium = baseline.titleMedium.themed(),
            titleSmall = baseline.titleSmall.themed(),
            bodyLarge = baseline.bodyLarge.themed(),
            bodyMedium = baseline.bodyMedium.themed(),
            bodySmall = baseline.bodySmall.themed(),
            labelLarge = baseline.labelLarge.themed(),
            labelMedium = baseline.labelMedium.themed(),
            labelSmall = baseline.labelSmall.themed(),
        )
    }
}

private fun TextUnit.scaled(scale: Float): TextUnit = if (isSpecified && scale != 1f) this * scale else this
