package org.bhargav.pansariwala.feature.reports

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.bhargav.pansariwala.domain.model.CategoryStock
import org.bhargav.pansariwala.i18n.localizedName
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.no_inventory_yet
import pansariwala.shared.generated.resources.report_chart_empty
import pansariwala.shared.generated.resources.report_chart_tap_hint
import pansariwala.shared.generated.resources.report_inventory_total
import pansariwala.shared.generated.resources.report_offline
import pansariwala.shared.generated.resources.report_online
import pansariwala.shared.generated.resources.report_point_orders
import pansariwala.shared.generated.resources.report_point_revenue
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.roundToInt

val OnlineColor = Color(0xFF1565C0)
val OfflineColor = Color(0xFFEF6C00)

private val pieColors = listOf(
    Color(0xFF2E7D32), Color(0xFF1565C0), Color(0xFFEF6C00), Color(0xFF6A1B9A),
    Color(0xFFC62828), Color(0xFF00838F), Color(0xFF9E9D24), Color(0xFF5D4037),
)

private const val NO_SELECTION = -1
private const val CHART_ANIM_MS = 600
private const val GRID_LINES = 4

@Composable
private fun rememberEntryProgress(key: Any?): Float {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(key) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(CHART_ANIM_MS))
    }
    return progress.value
}

// ---------- Inventory donut ----------

@Composable
fun InventoryPieChart(data: List<CategoryStock>, modifier: Modifier = Modifier) {
    if (data.isEmpty() || data.all { it.stockValue <= 0.0 }) {
        EmptyChart(stringResource(Res.string.no_inventory_yet))
        return
    }
    val total = remember(data) { data.sumOf { it.stockValue }.coerceAtLeast(1.0) }
    val sweeps = remember(data) { data.map { (it.stockValue / total * 360.0).toFloat() } }
    var selected by remember(data) { mutableIntStateOf(NO_SELECTION) }
    val progress = rememberEntryProgress(data)
    val surface = MaterialTheme.colorScheme.surface

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .pointerInput(sweeps) {
                        detectTapGestures { tap ->
                            val c = Offset(size.width / 2f, size.height / 2f)
                            val radius = minOf(size.width, size.height) / 2f
                            val dist = hypot(tap.x - c.x, tap.y - c.y)
                            if (dist > radius || dist < radius * 0.45f) {
                                selected = NO_SELECTION
                                return@detectTapGestures
                            }
                            var angle = (atan2(tap.y - c.y, tap.x - c.x) * 180f / PI.toFloat()) + 90f
                            if (angle < 0f) angle += 360f
                            var acc = 0f
                            val hit = sweeps.indexOfFirst { s -> acc += s; angle <= acc }
                            selected = if (hit == selected) NO_SELECTION else hit
                        }
                    },
            ) {
                val strokeWidth = size.minDimension * 0.22f
                val inset = strokeWidth / 2f + size.minDimension * 0.04f
                var start = -90f
                sweeps.forEachIndexed { i, sweep ->
                    val isSelected = i == selected
                    val grow = if (isSelected) size.minDimension * 0.04f else 0f
                    drawArc(
                        color = pieColors[i % pieColors.size].copy(
                            alpha = if (selected == NO_SELECTION || isSelected) 1f else 0.35f,
                        ),
                        startAngle = start,
                        sweepAngle = (sweep * progress - 1f).coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = Offset(inset - grow, inset - grow),
                        size = Size(size.width - 2 * (inset - grow), size.height - 2 * (inset - grow)),
                        style = Stroke(width = strokeWidth + if (isSelected) grow else 0f),
                    )
                    start += sweep * progress
                }
                drawCircle(surface, radius = size.minDimension / 2f - inset - strokeWidth / 2f)
            }
            val center = data.getOrNull(selected)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    center?.category?.localizedName() ?: stringResource(Res.string.report_inventory_total),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    (center?.stockValue ?: total).asMoney(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (center != null) {
                    Text(
                        "${((center.stockValue / total) * 100).roundToInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
        }
        data.forEachIndexed { i, item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (i == selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    )
                    .clickable { selected = if (selected == i) NO_SELECTION else i }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                LegendDot(pieColors[i % pieColors.size])
                Text(
                    item.category.localizedName(),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
                Text(
                    "${((item.stockValue / total) * 100).roundToInt()}% · ${item.stockValue.asMoney()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------- Orders line chart ----------

@Composable
fun OrdersLineChart(
    buckets: List<ReportBucket>,
    showOnline: Boolean,
    showOffline: Boolean,
    labelFor: (ReportBucket) -> String,
    modifier: Modifier = Modifier,
) {
    if (buckets.isEmpty() || buckets.all { it.onlineOrders + it.offlineOrders == 0 }) {
        EmptyChart(stringResource(Res.string.report_chart_empty))
        return
    }
    var selected by remember(buckets) { mutableIntStateOf(NO_SELECTION) }
    val progress = rememberEntryProgress(buckets)
    val maxY = remember(buckets, showOnline, showOffline) {
        buckets.maxOf {
            maxOf(if (showOnline) it.onlineOrders else 0, if (showOffline) it.offlineOrders else 0)
        }.coerceAtLeast(1)
    }
    val labels = remember(buckets, labelFor) { buckets.map(labelFor) }
    val measurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            if (showOnline) LegendItem(OnlineColor, stringResource(Res.string.report_online))
            if (showOffline) LegendItem(OfflineColor, stringResource(Res.string.report_offline))
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .chartSelection(buckets.size) { selected = it },
        ) {
            val plot = plotArea(this)
            drawGrid(plot, maxY.toDouble(), axisColor, measurer, labelStyle) { it.roundToInt().toString() }
            drawXLabels(plot, labels, measurer, labelStyle)
            if (selected in buckets.indices) {
                val x = plot.xAt(selected, buckets.size)
                drawLine(highlight, Offset(x, plot.top), Offset(x, plot.bottom), strokeWidth = 2.dp.toPx())
            }
            clipRect(right = plot.left + plot.width * progress) {
                if (showOffline) drawSeries(plot, buckets.map { it.offlineOrders.toDouble() }, maxY.toDouble(), OfflineColor, selected)
                if (showOnline) drawSeries(plot, buckets.map { it.onlineOrders.toDouble() }, maxY.toDouble(), OnlineColor, selected)
            }
        }
        val point = buckets.getOrNull(selected)
        SelectionCaption(
            if (point == null) {
                stringResource(Res.string.report_chart_tap_hint)
            } else {
                stringResource(Res.string.report_point_orders, labels[selected], point.onlineOrders, point.offlineOrders)
            },
        )
    }
}

// ---------- Revenue bar chart ----------

@Composable
fun RevenueBarChart(
    buckets: List<ReportBucket>,
    labelFor: (ReportBucket) -> String,
    modifier: Modifier = Modifier,
) {
    if (buckets.isEmpty() || buckets.all { it.onlineRevenue + it.offlineRevenue <= 0.0 }) {
        EmptyChart(stringResource(Res.string.report_chart_empty))
        return
    }
    var selected by remember(buckets) { mutableIntStateOf(NO_SELECTION) }
    val progress = rememberEntryProgress(buckets)
    val maxY = remember(buckets) {
        buckets.maxOf { it.onlineRevenue + it.offlineRevenue }.coerceAtLeast(1.0)
    }
    val labels = remember(buckets, labelFor) { buckets.map(labelFor) }
    val measurer = rememberTextMeasurer()
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    val labelStyle = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(OnlineColor, stringResource(Res.string.report_online))
            LegendItem(OfflineColor, stringResource(Res.string.report_offline))
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .chartSelection(buckets.size) { selected = it },
        ) {
            val plot = plotArea(this)
            drawGrid(plot, maxY, axisColor, measurer, labelStyle, ::compactAmount)
            drawXLabels(plot, labels, measurer, labelStyle)
            val slot = plot.width / buckets.size
            val barWidth = (slot * 0.65f).coerceAtMost(28.dp.toPx())
            buckets.forEachIndexed { i, b ->
                val alpha = if (selected == NO_SELECTION || selected == i) 1f else 0.35f
                val x = plot.left + slot * i + (slot - barWidth) / 2f
                val offlineH = (b.offlineRevenue / maxY * plot.height * progress).toFloat()
                val onlineH = (b.onlineRevenue / maxY * plot.height * progress).toFloat()
                if (offlineH > 0f) {
                    drawRect(OfflineColor.copy(alpha = alpha), Offset(x, plot.bottom - offlineH), Size(barWidth, offlineH))
                }
                if (onlineH > 0f) {
                    drawRoundRect(
                        OnlineColor.copy(alpha = alpha),
                        Offset(x, plot.bottom - offlineH - onlineH),
                        Size(barWidth, onlineH),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                    )
                }
            }
        }
        val point = buckets.getOrNull(selected)
        SelectionCaption(
            if (point == null) {
                stringResource(Res.string.report_chart_tap_hint)
            } else {
                stringResource(
                    Res.string.report_point_revenue,
                    labels[selected],
                    (point.onlineRevenue + point.offlineRevenue).asMoney(),
                    point.onlineRevenue.asMoney(),
                    point.offlineRevenue.asMoney(),
                )
            },
        )
    }
}

// ---------- shared drawing helpers ----------

private class PlotArea(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width get() = right - left
    val height get() = bottom - top

    /** Center of the i-th slot, so line points align with bar centers. */
    fun xAt(i: Int, count: Int): Float = left + width / count * (i + 0.5f)
}

private fun plotArea(scope: DrawScope): PlotArea = with(scope) {
    PlotArea(left = 40.dp.toPx(), top = 8.dp.toPx(), right = size.width - 4.dp.toPx(), bottom = size.height - 20.dp.toPx())
}

private fun Modifier.chartSelection(count: Int, onSelect: (Int) -> Unit): Modifier =
    pointerInput(count) {
        val left = 40.dp.toPx()
        val right = size.width - 4.dp.toPx()
        fun indexAt(x: Float): Int =
            (((x - left) / (right - left)) * count).toInt().coerceIn(0, count - 1)
        detectTapGestures { onSelect(indexAt(it.x)) }
    }.pointerInput(count) {
        val left = 40.dp.toPx()
        val right = size.width - 4.dp.toPx()
        detectHorizontalDragGestures { change, _ ->
            onSelect((((change.position.x - left) / (right - left)) * count).toInt().coerceIn(0, count - 1))
        }
    }

private fun DrawScope.drawGrid(
    plot: PlotArea,
    maxY: Double,
    color: Color,
    measurer: TextMeasurer,
    style: TextStyle,
    format: (Double) -> String,
) {
    val step = niceStep(maxY)
    for (g in 0..GRID_LINES) {
        val value = step * g
        if (value > maxY * 1.001 && g > 0) break
        val y = plot.bottom - (value / maxY * plot.height).toFloat()
        drawLine(color, Offset(plot.left, y), Offset(plot.right, y), strokeWidth = 1f)
        val text = measurer.measure(format(value), style)
        drawText(text, topLeft = Offset(plot.left - text.size.width - 4.dp.toPx(), y - text.size.height / 2f))
    }
}

private fun DrawScope.drawXLabels(plot: PlotArea, labels: List<String>, measurer: TextMeasurer, style: TextStyle) {
    if (labels.isEmpty()) return
    val maxLabels = (plot.width / 48.dp.toPx()).toInt().coerceAtLeast(2)
    val every = ceil(labels.size / maxLabels.toFloat()).toInt().coerceAtLeast(1)
    labels.forEachIndexed { i, label ->
        if (i % every != 0) return@forEachIndexed
        val text = measurer.measure(label, style)
        val x = (plot.xAt(i, labels.size) - text.size.width / 2f)
            .coerceIn(0f, size.width - text.size.width)
        drawText(text, topLeft = Offset(x, plot.bottom + 4.dp.toPx()))
    }
}

private fun DrawScope.drawSeries(plot: PlotArea, values: List<Double>, maxY: Double, color: Color, selected: Int) {
    val points = values.mapIndexed { i, v ->
        Offset(plot.xAt(i, values.size), plot.bottom - (v / maxY * plot.height).toFloat())
    }
    val path = Path().apply {
        points.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
    }
    drawPath(path, color, style = Stroke(width = 2.5.dp.toPx()))
    val showDots = points.size <= 31
    points.forEachIndexed { i, p ->
        if (i == selected) {
            drawCircle(color, radius = 5.dp.toPx(), center = p)
        } else if (showDots) {
            drawCircle(color, radius = 2.5.dp.toPx(), center = p)
        }
    }
}

private fun niceStep(maxY: Double): Double {
    val raw = maxY / GRID_LINES
    if (raw <= 1.0) return 1.0
    var magnitude = 1.0
    while (magnitude * 10 <= raw) magnitude *= 10
    val norm = raw / magnitude
    val nice = when {
        norm <= 1 -> 1.0
        norm <= 2 -> 2.0
        norm <= 5 -> 5.0
        else -> 10.0
    }
    return nice * magnitude
}

private fun compactAmount(value: Double): String = when {
    value >= 10_000_000 -> "${(value / 10_000_000).oneDecimal()}Cr"
    value >= 100_000 -> "${(value / 100_000).oneDecimal()}L"
    value >= 1_000 -> "${(value / 1_000).oneDecimal()}k"
    else -> value.roundToInt().toString()
}

private fun Double.oneDecimal(): String {
    val tenths = (this * 10).roundToInt()
    return if (tenths % 10 == 0) (tenths / 10).toString() else "${tenths / 10}.${tenths % 10}"
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        LegendDot(color)
        Text(label, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(start = 6.dp))
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(Modifier.size(10.dp).clip(CircleShape).background(color))
}

@Composable
private fun SelectionCaption(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun EmptyChart(text: String) {
    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
