package org.bhargav.pansariwala.feature.user

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.spend_empty
import pansariwala.shared.generated.resources.spend_total
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.hypot
import kotlin.math.roundToInt

private val spendColors = listOf(
    Color(0xFF2E7D32), Color(0xFF1565C0), Color(0xFFEF6C00), Color(0xFF6A1B9A),
    Color(0xFFC62828), Color(0xFF00838F), Color(0xFF9E9D24), Color(0xFF5D4037),
    Color(0xFFAD1457), Color(0xFF283593),
)

private const val SPEND_CHART_ANIM_MS = 700

fun spendColor(index: Int): Color = spendColors[index % spendColors.size]

@Composable
private fun rememberGrow(key: Any?): Float {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(key) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(SPEND_CHART_ANIM_MS))
    }
    return progress.value
}

/** Tapping a bar opens the bills of that day / month. */
@Composable
fun SpendBarChart(
    amounts: List<Double>,
    labels: List<String>,
    onBarClick: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (amounts.none { it > 0.0 }) {
        SpendEmpty()
        return
    }
    val progress = rememberGrow(amounts)
    val maxY = remember(amounts) { amounts.max().coerceAtLeast(1.0) }
    val measurer = rememberTextMeasurer()
    val labelStyle = TextStyle(fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    val top = MaterialTheme.colorScheme.primary
    val bottom = MaterialTheme.colorScheme.tertiary
    val track = MaterialTheme.colorScheme.surfaceVariant
    Canvas(
        modifier
            .fillMaxWidth()
            .height(200.dp)
            .pointerInput(amounts.size) {
                detectTapGestures { tap ->
                    val i = (tap.x / (size.width / amounts.size)).toInt().coerceIn(0, amounts.size - 1)
                    if (amounts[i] > 0.0) onBarClick(i)
                }
            },
    ) {
        val labelSpace = 18.dp.toPx()
        val plotHeight = size.height - labelSpace
        val slot = size.width / amounts.size
        val barWidth = (slot * 0.62f).coerceIn(2.dp.toPx(), 26.dp.toPx())
        val radius = CornerRadius(minOf(barWidth / 2f, 6.dp.toPx()))
        amounts.forEachIndexed { i, amount ->
            val x = slot * i + (slot - barWidth) / 2f
            drawRoundRect(track, Offset(x, 0f), Size(barWidth, plotHeight), radius)
            val h = (amount / maxY * plotHeight * progress).toFloat()
            if (h > 0f) {
                drawRoundRect(
                    Brush.verticalGradient(listOf(top, bottom), startY = plotHeight - h, endY = plotHeight),
                    Offset(x, plotHeight - h),
                    Size(barWidth, h),
                    radius,
                )
            }
        }
        val maxLabels = (size.width / 44.dp.toPx()).toInt().coerceAtLeast(2)
        val every = ceil(labels.size / maxLabels.toFloat()).toInt().coerceAtLeast(1)
        labels.forEachIndexed { i, label ->
            if (i % every != 0) return@forEachIndexed
            val text = measurer.measure(label, labelStyle)
            val x = (slot * i + slot / 2f - text.size.width / 2f).coerceIn(0f, size.width - text.size.width)
            drawText(text, topLeft = Offset(x, plotHeight + 4.dp.toPx()))
        }
    }
}

/** Donut with a legend; tapping a slice or a legend row opens that slice's bills. */
@Composable
fun SpendDonutChart(
    slices: List<SpendSlice>,
    onSliceClick: (SpendSlice) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (slices.isEmpty()) {
        SpendEmpty()
        return
    }
    val total = remember(slices) { slices.sumOf { it.amount }.coerceAtLeast(1.0) }
    val sweeps = remember(slices) { slices.map { (it.amount / total * 360.0).toFloat() } }
    val progress = rememberGrow(slices)
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .widthIn(max = 220.dp)
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .pointerInput(sweeps) {
                        detectTapGestures { tap ->
                            val c = Offset(size.width / 2f, size.height / 2f)
                            val r = minOf(size.width, size.height) / 2f
                            val dist = hypot(tap.x - c.x, tap.y - c.y)
                            if (dist > r || dist < r * 0.45f) return@detectTapGestures
                            var angle = (atan2(tap.y - c.y, tap.x - c.x) * 180f / PI.toFloat()) + 90f
                            if (angle < 0f) angle += 360f
                            var acc = 0f
                            val hit = sweeps.indexOfFirst { s -> acc += s; angle <= acc }
                            slices.getOrNull(hit)?.let(onSliceClick)
                        }
                    },
            ) {
                val stroke = size.minDimension * 0.2f
                val inset = stroke / 2f + size.minDimension * 0.03f
                var start = -90f
                sweeps.forEachIndexed { i, sweep ->
                    drawArc(
                        color = spendColor(i),
                        startAngle = start,
                        sweepAngle = (sweep * progress - 1f).coerceAtLeast(0.5f),
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - 2 * inset, size.height - 2 * inset),
                        style = Stroke(width = stroke),
                    )
                    start += sweep * progress
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    stringResource(Res.string.spend_total),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(total.asMoney(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        }
        slices.forEachIndexed { i, slice ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSliceClick(slice) }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(spendColor(i)))
                Text(
                    slice.label,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
                Text(
                    "${((slice.amount / total) * 100).roundToInt()}% · ${slice.amount.asMoney()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SpendEmpty() {
    Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
        Text(
            stringResource(Res.string.spend_empty),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
