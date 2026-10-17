package org.bhargav.pansariwala.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.background
import org.bhargav.pansariwala.domain.model.StockLevel
import org.bhargav.pansariwala.theme.stockCriticalColor
import org.bhargav.pansariwala.theme.stockHealthyColor
import org.bhargav.pansariwala.theme.stockLowColor
import org.bhargav.pansariwala.theme.stockOutColor
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.inv_stock_critical
import pansariwala.shared.generated.resources.inv_stock_healthy
import pansariwala.shared.generated.resources.inv_stock_low
import pansariwala.shared.generated.resources.inv_stock_out

/** Gauge is full at this multiple of the low-stock threshold. */
private const val GAUGE_FULL_AT_THRESHOLD_MULTIPLE = 3.0

fun StockLevel.color(): Color = when (this) {
    StockLevel.OUT_OF_STOCK -> stockOutColor
    StockLevel.CRITICAL -> stockCriticalColor
    StockLevel.LOW -> stockLowColor
    StockLevel.HEALTHY -> stockHealthyColor
}

val StockLevel.label: StringResource
    get() = when (this) {
        StockLevel.OUT_OF_STOCK -> Res.string.inv_stock_out
        StockLevel.CRITICAL -> Res.string.inv_stock_critical
        StockLevel.LOW -> Res.string.inv_stock_low
        StockLevel.HEALTHY -> Res.string.inv_stock_healthy
    }

/** 0..1 fill for a stock gauge; healthy stock above 3× threshold reads as full. */
fun stockFraction(stockQty: Double, lowStockThreshold: Double): Float {
    if (stockQty <= 0.0) return 0f
    val full = if (lowStockThreshold > 0.0) lowStockThreshold * GAUGE_FULL_AT_THRESHOLD_MULTIPLE else stockQty
    return (stockQty / full).coerceIn(0.0, 1.0).toFloat()
}

@Composable
fun StockBadge(level: StockLevel, modifier: Modifier = Modifier) {
    val color = level.color()
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = 0.14f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(color))
            Text(
                text = stringResource(level.label),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
            )
        }
    }
}

@Composable
fun StockGauge(stockQty: Double, lowStockThreshold: Double, modifier: Modifier = Modifier) {
    val level = StockLevel.of(stockQty, lowStockThreshold)
    LinearProgressIndicator(
        progress = { stockFraction(stockQty, lowStockThreshold) },
        modifier = modifier.clip(RoundedCornerShape(50)),
        color = level.color(),
        trackColor = level.color().copy(alpha = 0.18f),
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}
