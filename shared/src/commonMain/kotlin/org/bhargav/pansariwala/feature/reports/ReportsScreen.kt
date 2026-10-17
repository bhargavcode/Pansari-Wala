package org.bhargav.pansariwala.feature.reports

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DateRangePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDateRangePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.StatTile
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.domain.model.OrderChannel
import org.bhargav.pansariwala.feature.user.userOrderStatusLabel
import org.bhargav.pansariwala.printing.formatPrintDateTime
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_apply
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.action_refresh
import pansariwala.shared.generated.resources.report_avg_order
import pansariwala.shared.generated.resources.report_channel_all
import pansariwala.shared.generated.resources.report_channel_offline
import pansariwala.shared.generated.resources.report_channel_online
import pansariwala.shared.generated.resources.report_inventory_title
import pansariwala.shared.generated.resources.report_list_empty
import pansariwala.shared.generated.resources.report_list_title
import pansariwala.shared.generated.resources.report_month_short
import pansariwala.shared.generated.resources.report_next_page
import pansariwala.shared.generated.resources.report_offline
import pansariwala.shared.generated.resources.report_online
import pansariwala.shared.generated.resources.report_orders_count
import pansariwala.shared.generated.resources.report_orders_trend
import pansariwala.shared.generated.resources.report_page_of
import pansariwala.shared.generated.resources.report_period_custom
import pansariwala.shared.generated.resources.report_period_month
import pansariwala.shared.generated.resources.report_period_today
import pansariwala.shared.generated.resources.report_period_week
import pansariwala.shared.generated.resources.report_period_year
import pansariwala.shared.generated.resources.report_period_yesterday
import pansariwala.shared.generated.resources.report_pick_dates
import pansariwala.shared.generated.resources.report_prev_page
import pansariwala.shared.generated.resources.report_revenue
import pansariwala.shared.generated.resources.report_revenue_trend
import pansariwala.shared.generated.resources.report_row_items
import pansariwala.shared.generated.resources.reports_title
import pansariwala.shared.generated.resources.report_print_sales
import org.bhargav.pansariwala.feature.printer.PrintAction
import org.bhargav.pansariwala.feature.printer.PrintMenuButton

@Composable
fun ReportsScreen(
    onBack: () -> Unit,
    viewModel: ReportsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val canPrint by viewModel.canPrint.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val months = stringArrayResource(Res.array.report_month_short)
    val multiYear = state.range?.let { it.start.year != it.endInclusive.year } == true
    val labelFor: (ReportBucket) -> String = remember(months, multiYear) {
        { b -> bucketLabel(b, months, multiYear) }
    }
    val showOnline = state.channel != ReportChannel.OFFLINE
    val showOffline = state.channel != ReportChannel.ONLINE

    PansariScreen(
        title = stringResource(Res.string.reports_title),
        onBack = onBack,
        actions = {
            if (canPrint) {
                PrintMenuButton(
                    listOf(PrintAction(Res.string.report_print_sales) { printSalesReport(viewModel.buildSalesReport()) }),
                )
            }
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.action_refresh))
            }
        },
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = viewModel::refresh, onDismiss = viewModel::dismissError)
        },
        isLoading = state.loading && state.range == null,
        isRefreshing = state.loading && state.range != null,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val wide = maxWidth >= 840.dp
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "filters", contentType = "filters") {
                    ReportFilters(
                        channel = state.channel,
                        period = state.period,
                        range = state.range,
                        onChannel = viewModel::setChannel,
                        onPeriod = viewModel::setPeriod,
                    )
                }
                item(key = "kpis", contentType = "kpis") { KpiRow(state.summary, state.channel) }
                item(key = "charts", contentType = "charts") {
                    val orders: @Composable (Modifier) -> Unit = { m ->
                        SectionCard(title = stringResource(Res.string.report_orders_trend), modifier = m) {
                            OrdersLineChart(state.buckets, showOnline, showOffline, labelFor)
                        }
                    }
                    val revenue: @Composable (Modifier) -> Unit = { m ->
                        SectionCard(title = stringResource(Res.string.report_revenue_trend), modifier = m) {
                            RevenueBarChart(state.buckets, labelFor)
                        }
                    }
                    if (wide) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            orders(Modifier.weight(1f))
                            revenue(Modifier.weight(1f))
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            orders(Modifier.fillMaxWidth())
                            revenue(Modifier.fillMaxWidth())
                        }
                    }
                }
                item(key = "inventory", contentType = "inventory") {
                    SectionCard(title = stringResource(Res.string.report_inventory_title)) {
                        InventoryPieChart(state.inventory)
                    }
                }
                item(key = "list_header", contentType = "header") {
                    Text(
                        stringResource(Res.string.report_list_title, state.totalRows),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                if (state.pageRows.isEmpty() && !state.loading) {
                    item(key = "list_empty", contentType = "empty") {
                        Text(
                            stringResource(Res.string.report_list_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(state.pageRows, key = { "${it.channel}_${it.id}" }, contentType = { "row" }) { row ->
                    ReportOrderRow(row)
                }
                if (state.pageCount > 1) {
                    item(key = "pager", contentType = "pager") {
                        Pager(
                            page = state.page,
                            pageCount = state.pageCount,
                            onPage = { target ->
                                viewModel.goToPage(target)
                                scope.launch { listState.animateScrollToItem(LIST_HEADER_INDEX) }
                            },
                        )
                    }
                }
            }
        }
    }

    if (state.showDatePicker) {
        CustomRangeDialog(
            onDismiss = viewModel::dismissDatePicker,
            onConfirm = viewModel::setCustomRange,
        )
    }
}

/** filters, kpis, charts, inventory → header sits at index 4. */
private const val LIST_HEADER_INDEX = 4

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReportFilters(
    channel: ReportChannel,
    period: ReportPeriod,
    range: ReportRange?,
    onChannel: (ReportChannel) -> Unit,
    onPeriod: (ReportPeriod) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        PrimaryTabRow(selectedTabIndex = ReportChannel.entries.indexOf(channel)) {
            ReportChannel.entries.forEach { option ->
                Tab(
                    selected = option == channel,
                    onClick = { onChannel(option) },
                    text = { Text(channelLabel(option)) },
                )
            }
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ReportPeriod.entries.forEach { option ->
                FilterChip(
                    selected = option == period,
                    onClick = { onPeriod(option) },
                    label = { Text(periodLabel(option)) },
                )
            }
        }
        if (range != null) {
            AssistChip(
                onClick = { onPeriod(ReportPeriod.CUSTOM) },
                label = {
                    val from = range.start.short()
                    val to = range.endInclusive.short()
                    Text(if (from == to) from else "$from – $to")
                },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun KpiRow(summary: ReportSummary, channel: ReportChannel) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val tile = Modifier.widthIn(min = 150.dp).weight(1f)
        StatTile(stringResource(Res.string.report_revenue), summary.revenue.asMoney(), tile)
        StatTile(
            stringResource(Res.string.report_orders_count),
            summary.totalOrders.toString(),
            tile,
            container = MaterialTheme.colorScheme.secondaryContainer,
            onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        if (channel == ReportChannel.ALL) {
            StatTile(
                stringResource(Res.string.report_online),
                "${summary.onlineOrders} · ${summary.onlineRevenue.asMoney()}",
                tile,
                container = OnlineColor.copy(alpha = 0.15f),
                onContainer = OnlineColor,
            )
            StatTile(
                stringResource(Res.string.report_offline),
                "${summary.offlineOrders} · ${summary.offlineRevenue.asMoney()}",
                tile,
                container = OfflineColor.copy(alpha = 0.15f),
                onContainer = OfflineColor,
            )
        }
        StatTile(
            stringResource(Res.string.report_avg_order),
            summary.averageOrderValue.asMoney(),
            tile,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

@Composable
private fun ReportOrderRow(row: ReportOrder) {
    val accent = if (row.channel == OrderChannel.ONLINE) OnlineColor else OfflineColor
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = accent, shape = MaterialTheme.shapes.small, modifier = Modifier.height(36.dp).widthIn(4.dp, 4.dp)) {}
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    "#${row.displayNumber} · ${row.customerName.orEmpty().ifBlank { channelLabel(row.channel.toReportChannel()) }}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    "${formatPrintDateTime(row.createdAtEpochMs)} · ${stringResource(Res.string.report_row_items, row.itemCount)} · ${userOrderStatusLabel(row.status)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(row.total.asMoney(), fontWeight = FontWeight.SemiBold)
                Text(
                    channelLabel(row.channel.toReportChannel()),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                )
            }
        }
    }
}

@Composable
private fun Pager(page: Int, pageCount: Int, onPage: (Int) -> Unit) {
    Column {
        HorizontalDivider()
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            IconButton(onClick = { onPage(page - 1) }, enabled = page > 0) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(Res.string.report_prev_page),
                )
            }
            Row(
                Modifier.weight(1f).horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pageWindow(page, pageCount).forEach { p ->
                    if (p == null) {
                        Text("…", modifier = Modifier.padding(horizontal = 4.dp))
                    } else {
                        FilterChip(
                            selected = p == page,
                            onClick = { onPage(p) },
                            label = { Text((p + 1).toString()) },
                        )
                    }
                }
            }
            IconButton(onClick = { onPage(page + 1) }, enabled = page < pageCount - 1) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(Res.string.report_next_page),
                )
            }
        }
        Text(
            stringResource(Res.string.report_page_of, page + 1, pageCount),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

/** First, last, and up to two neighbours of the current page; null marks a gap. */
private fun pageWindow(page: Int, pageCount: Int): List<Int?> {
    val shown = (listOf(0, pageCount - 1) + (page - 1..page + 1))
        .filter { it in 0 until pageCount }
        .distinct()
        .sorted()
    val out = mutableListOf<Int?>()
    shown.forEachIndexed { i, p ->
        if (i > 0 && p - shown[i - 1] > 1) out += null
        out += p
    }
    return out
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CustomRangeDialog(onDismiss: () -> Unit, onConfirm: (Long, Long?) -> Unit) {
    val pickerState = rememberDateRangePickerState()
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    val start = pickerState.selectedStartDateMillis ?: return@TextButton
                    onConfirm(start, pickerState.selectedEndDateMillis)
                },
                enabled = pickerState.selectedStartDateMillis != null,
            ) { Text(stringResource(Res.string.action_apply)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) }
        },
    ) {
        DateRangePicker(
            state = pickerState,
            title = {
                Text(
                    stringResource(Res.string.report_pick_dates),
                    modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 16.dp),
                )
            },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun channelLabel(channel: ReportChannel): String = stringResource(
    when (channel) {
        ReportChannel.ALL -> Res.string.report_channel_all
        ReportChannel.ONLINE -> Res.string.report_channel_online
        ReportChannel.OFFLINE -> Res.string.report_channel_offline
    },
)

@Composable
private fun periodLabel(period: ReportPeriod): String = stringResource(
    when (period) {
        ReportPeriod.TODAY -> Res.string.report_period_today
        ReportPeriod.YESTERDAY -> Res.string.report_period_yesterday
        ReportPeriod.WEEK -> Res.string.report_period_week
        ReportPeriod.MONTH -> Res.string.report_period_month
        ReportPeriod.YEAR -> Res.string.report_period_year
        ReportPeriod.CUSTOM -> Res.string.report_period_custom
    },
)

private fun OrderChannel.toReportChannel(): ReportChannel =
    if (this == OrderChannel.ONLINE) ReportChannel.ONLINE else ReportChannel.OFFLINE

private fun Int.two(): String = toString().padStart(2, '0')

internal fun LocalDate.short(): String = "${day.two()}/${month.number.two()}/$year"

private fun bucketLabel(b: ReportBucket, months: List<String>, multiYear: Boolean): String {
    val monthName = months.getOrElse(b.month - 1) { b.month.toString() }
    return when (b.unit) {
        BucketUnit.HOUR -> b.hour.two()
        BucketUnit.DAY -> "${b.day} $monthName"
        BucketUnit.MONTH -> if (multiYear) "$monthName ${(b.year % 100).two()}" else monthName
    }
}
