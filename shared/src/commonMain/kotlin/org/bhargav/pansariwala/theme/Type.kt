package org.bhargav.pansariwala.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import org.jetbrains.compose.resources.Font
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.montserrat_bold
import pansariwala.shared.generated.resources.montserrat_medium
import pansariwala.shared.generated.resources.montserrat_regular
import pansariwala.shared.generated.resources.montserrat_semibold

@Composable
fun montserratFontFamily(): FontFamily = FontFamily(
    Font(Res.font.montserrat_regular, weight = FontWeight.Normal),
    Font(Res.font.montserrat_medium, weight = FontWeight.Medium),
    Font(Res.font.montserrat_semibold, weight = FontWeight.SemiBold),
    Font(Res.font.montserrat_bold, weight = FontWeight.Bold),
)

@Composable
fun AppTypography(): Typography {
    val family = montserratFontFamily()
    val baseline = Typography()
    return Typography(
        displayLarge = baseline.displayLarge.copy(fontFamily = family),
        displayMedium = baseline.displayMedium.copy(fontFamily = family),
        displaySmall = baseline.displaySmall.copy(fontFamily = family),
        headlineLarge = baseline.headlineLarge.copy(fontFamily = family),
        headlineMedium = baseline.headlineMedium.copy(fontFamily = family),
        headlineSmall = baseline.headlineSmall.copy(fontFamily = family),
        titleLarge = baseline.titleLarge.copy(fontFamily = family),
        titleMedium = baseline.titleMedium.copy(fontFamily = family),
        titleSmall = baseline.titleSmall.copy(fontFamily = family),
        bodyLarge = baseline.bodyLarge.copy(fontFamily = family),
        bodyMedium = baseline.bodyMedium.copy(fontFamily = family),
        bodySmall = baseline.bodySmall.copy(fontFamily = family),
        labelLarge = baseline.labelLarge.copy(fontFamily = family),
        labelMedium = baseline.labelMedium.copy(fontFamily = family),
        labelSmall = baseline.labelSmall.copy(fontFamily = family),
    )
}
