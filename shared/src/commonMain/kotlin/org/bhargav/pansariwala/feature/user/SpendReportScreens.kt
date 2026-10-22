package org.bhargav.pansariwala.feature.user

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.number
import org.bhargav.pansariwala.api.SpendOrderDto
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.StatTile
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.feature.reports.CustomRangeDialog
import org.bhargav.pansariwala.feature.reports.short
import org.bhargav.pansariwala.printing.formatPrintDateTime
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.partner_period_custom
import pansariwala.shared.generated.resources.partner_period_six_months
import pansariwala.shared.generated.resources.partner_period_three_months
import pansariwala.shared.generated.resources.report_month_short
import pansariwala.shared.generated.resources.report_period_month
import pansariwala.shared.generated.resources.report_period_week
import pansariwala.shared.generated.resources.report_period_year
import pansariwala.shared.generated.resources.spend_avg_order
import pansariwala.shared.generated.resources.spend_by_category
import pansariwala.shared.generated.resources.spend_by_shop
import pansariwala.shared.generated.resources.spend_chart_tap_hint
import pansariwala.shared.generated.resources.spend_details_items
import pansariwala.shared.generated.resources.spend_details_summary
import pansariwala.shared.generated.resources.spend_details_title
import pansariwala.shared.generated.resources.spend_empty
import pansariwala.shared.generated.resources.spend_orders
import pansariwala.shared.generated.resources.spend_period_last_30
import pansariwala.shared.generated.resources.spend_report_title
import pansariwala.shared.generated.resources.spend_shops
import pansariwala.shared.generated.resources.spend_total
import pansariwala.shared.generated.resources.spend_trend
import pansariwala.shared.generated.resources.spend_view_order

/** [onDrill] gets the time window, what was tapped and a title for the details screen. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpendReportScreen(
    onDrill: (fromMs: Long, toMs: Long, kind: SpendDrillKind, key: String, title: String) -> Unit,
    onBack: () -> Unit,
    viewModel: SpendReportViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val months = stringArrayResource(Res.array.report_month_short)
    val tz = TimeZone.currentSystemDefault()
    val range = state.range
    val multiYear = range != null && range.start.year != range.endInclusive.year
    val barLabels = state.bars.map { bar ->
        val month = months.getOrElse(bar.start.month.number - 1) { bar.start.month.number.toString() }
        when {
            !bar.monthly -> "${bar.start.day} $month"
            multiYear -> "$month ${(bar.start.year % 100).toString().padStart(2, '0')}"
            else -> month
        }
    }
    PansariScreen(
        title = stringResource(Res.string.spend_report_title),
        onBack = onBack,
        error = state.error.toErrorBanner(),
        onErrorAction = { handleErrorBannerAction(it, onRetry = viewModel::refresh, onDismiss = viewModel::dismissError) },
        isLoading = state.loading && state.orders.isEmpty(),
        isRefreshing = state.loading && state.orders.isNotEmpty(),
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "filters") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        SpendPeriod.entries.forEach { option ->
                            FilterChip(
                                selected = option == state.period,
                                onClick = { viewModel.setPeriod(option) },
                                label = { Text(spendPeriodLabel(option)) },
                            )
                        }
                    }
                    if (range != null) {
                        AssistChip(
                            onClick = { viewModel.setPeriod(SpendPeriod.CUSTOM) },
                            label = {
                                val from = range.start.short()
                                val to = range.endInclusive.short()
                                Text(if (from == to) from else "$from – $to")
                            },
                        )
                    }
                }
            }
            item(key = "kpis") {
                FlowRow(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val tile = Modifier.widthIn(min = 150.dp).weight(1f)
                    StatTile(stringResource(Res.string.spend_total), state.total.asMoney(), tile)
                    StatTile(
                        stringResource(Res.string.spend_orders),
                        state.orders.size.toString(),
                        tile,
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    StatTile(
                        stringResource(Res.string.spend_avg_order),
                        state.average.asMoney(),
                        tile,
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    StatTile(
                        stringResource(Res.string.spend_shops),
                        state.shops.size.toString(),
                        tile,
                        container = MaterialTheme.colorScheme.surfaceVariant,
                        onContainer = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item(key = "hint") {
                Text(
                    stringResource(Res.string.spend_chart_tap_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            item(key = "trend") {
                SectionCard(title = stringResource(Res.string.spend_trend)) {
                    SpendBarChart(
                        amounts = state.bars.map { it.amount },
                        labels = barLabels,
                        onBarClick = { i ->
                            val bar = state.bars[i]
                            onDrill(
                                bar.start.atStartMs(tz),
                                bar.endExclusive.atStartMs(tz),
                                SpendDrillKind.TIME,
                                "",
                                barLabels[i],
                            )
                        },
                    )
                }
            }
            if (range != null) {
                val fromMs = range.fromMs(tz)
                val toMs = range.toMs(tz)
                item(key = "categories") {
                    SectionCard(title = stringResource(Res.string.spend_by_category)) {
                        SpendDonutChart(state.categories, onSliceClick = {
                            onDrill(fromMs, toMs, SpendDrillKind.CATEGORY, it.key, it.label)
                        })
                    }
                }
                item(key = "shops") {
                    SectionCard(title = stringResource(Res.string.spend_by_shop)) {
                        SpendDonutChart(state.shops, onSliceClick = {
                            onDrill(fromMs, toMs, SpendDrillKind.SHOP, it.key, it.label)
                        })
                    }
                }
            }
        }
    }
    if (state.showDatePicker) {
        CustomRangeDialog(onDismiss = viewModel::dismissDatePicker, onConfirm = viewModel::setCustomRange)
    }
}

@Composable
fun SpendDetailsScreen(
    fromMs: Long,
    toMs: Long,
    kind: SpendDrillKind,
    key: String,
    title: String,
    onOpenOrder: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: SpendDetailsViewModel = koinViewModel(),
) {
    LaunchedEffect(fromMs, toMs, kind, key) { viewModel.load(fromMs, toMs, kind, key) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    PansariScreen(
        title = title.ifBlank { stringResource(Res.string.spend_details_title) },
        onBack = onBack,
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(
                it,
                onRetry = { viewModel.load(fromMs, toMs, kind, key, force = true) },
                onDismiss = viewModel::dismissError,
            )
        },
        isLoading = state.loading,
    ) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "summary") {
                Text(
                    stringResource(Res.string.spend_details_summary, state.total.asMoney(), state.orders.size),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
            }
            if (!state.loading && state.orders.isEmpty()) {
                item(key = "empty") {
                    Text(stringResource(Res.string.spend_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(state.orders, key = { it.orderId }) { order ->
                SpendOrderCard(order, onOpenOrder = { onOpenOrder(order.orderId) })
            }
        }
    }
}

@Composable
private fun SpendOrderCard(order: SpendOrderDto, onOpenOrder: () -> Unit) {
    var expanded by rememberSaveable(order.orderId) { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        order.shopName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${formatPrintDateTime(order.createdAtEpochMs)} · ${stringResource(Res.string.spend_details_items, order.items.size)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(order.total.asMoney(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                )
            }
            AnimatedVisibility(expanded) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HorizontalDivider()
                    order.items.forEach { item ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    listOfNotNull(item.productName, item.variantLabel).joinToString(" · "),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    "${formatQuantity(item.quantity)} ${item.unit} · ${item.categoryName}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(item.amount.asMoney(), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    TextButton(onClick = onOpenOrder, modifier = Modifier.align(Alignment.End)) {
                        Text(stringResource(Res.string.spend_view_order))
                    }
                }
            }
        }
    }
}

@Composable
private fun spendPeriodLabel(period: SpendPeriod): String = stringResource(
    when (period) {
        SpendPeriod.LAST_30_DAYS -> Res.string.spend_period_last_30
        SpendPeriod.WEEK -> Res.string.report_period_week
        SpendPeriod.MONTH -> Res.string.report_period_month
        SpendPeriod.THREE_MONTHS -> Res.string.partner_period_three_months
        SpendPeriod.SIX_MONTHS -> Res.string.partner_period_six_months
        SpendPeriod.YEAR -> Res.string.report_period_year
        SpendPeriod.CUSTOM -> Res.string.partner_period_custom
    },
)

private fun LocalDate.atStartMs(tz: TimeZone): Long = atStartOfDayIn(tz).toEpochMilliseconds()

private fun formatQuantity(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else ((value * 100).toLong() / 100.0).toString()
