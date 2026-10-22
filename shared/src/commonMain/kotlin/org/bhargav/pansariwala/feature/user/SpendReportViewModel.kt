package org.bhargav.pansariwala.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.SpendItemDto
import org.bhargav.pansariwala.api.SpendOrderDto
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.feature.reports.ReportCalculator
import org.bhargav.pansariwala.feature.reports.ReportPeriod
import org.bhargav.pansariwala.feature.reports.ReportRange
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.util.AppClock
import org.bhargav.pansariwala.util.AppConstants
import kotlin.time.Instant

enum class SpendPeriod {
    LAST_30_DAYS, WEEK, MONTH, THREE_MONTHS, SIX_MONTHS, YEAR, CUSTOM;

    fun range(today: LocalDate, customFrom: LocalDate?, customTo: LocalDate?): ReportRange {
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        return when (this) {
            LAST_30_DAYS -> ReportRange(today.minus(AppConstants.SPEND_SUMMARY_DAYS - 1, DateTimeUnit.DAY), tomorrow)
            WEEK -> ReportCalculator.range(ReportPeriod.WEEK, today, null, null)
            MONTH -> ReportCalculator.range(ReportPeriod.MONTH, today, null, null)
            THREE_MONTHS -> ReportRange(today.minus(3, DateTimeUnit.MONTH), tomorrow)
            SIX_MONTHS -> ReportRange(today.minus(6, DateTimeUnit.MONTH), tomorrow)
            YEAR -> ReportCalculator.range(ReportPeriod.YEAR, today, null, null)
            CUSTOM -> ReportCalculator.range(ReportPeriod.CUSTOM, today, customFrom, customTo)
        }
    }
}

/** One pie slice / legend row; [key] is a category id or shop id. */
data class SpendSlice(val key: String, val label: String, val amount: Double)

/** Bar for [start, endExclusive); monthly bars when the range is longer than [MAX_DAILY_BARS] days. */
data class SpendBar(val start: LocalDate, val endExclusive: LocalDate, val monthly: Boolean, val amount: Double)

enum class SpendDrillKind { TIME, CATEGORY, SHOP }

data class SpendReportUiState(
    val period: SpendPeriod = SpendPeriod.LAST_30_DAYS,
    val range: ReportRange? = null,
    val loading: Boolean = true,
    val error: UiText? = null,
    val showDatePicker: Boolean = false,
    val orders: List<SpendOrderDto> = emptyList(),
    val bars: List<SpendBar> = emptyList(),
    val categories: List<SpendSlice> = emptyList(),
    val shops: List<SpendSlice> = emptyList(),
) {
    val total: Double get() = orders.sumOf { it.total }
    val average: Double get() = if (orders.isEmpty()) 0.0 else total / orders.size
}

class SpendReportViewModel(private val api: PansariApi) : ViewModel() {
    private val _state = MutableStateFlow(SpendReportUiState())
    val state: StateFlow<SpendReportUiState> = _state.asStateFlow()
    private var customFrom: LocalDate? = null
    private var customTo: LocalDate? = null
    private var loadJob: Job? = null

    init { refresh() }

    fun dismissError() { _state.update { it.copy(error = null) } }

    fun setPeriod(period: SpendPeriod) {
        if (period == SpendPeriod.CUSTOM) {
            _state.update { it.copy(showDatePicker = true) }
            return
        }
        _state.update { it.copy(period = period) }
        refresh()
    }

    fun dismissDatePicker() { _state.update { it.copy(showDatePicker = false) } }

    fun setCustomRange(startMs: Long, endMs: Long?) {
        customFrom = utcDate(startMs)
        customTo = endMs?.let(::utcDate) ?: customFrom
        _state.update { it.copy(period = SpendPeriod.CUSTOM, showDatePicker = false) }
        refresh()
    }

    fun refresh() {
        val tz = TimeZone.currentSystemDefault()
        val range = _state.value.period.range(today(tz), customFrom, customTo)
        _state.update { it.copy(range = range, loading = true, error = null) }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            runCatching { api.mySpend(range.fromMs(tz), range.toMs(tz)) }
                .onSuccess { report ->
                    _state.update {
                        it.copy(
                            loading = false,
                            orders = report.orders,
                            bars = bars(range, report.orders, tz),
                            categories = slices(report.orders.flatMap { o -> o.items }, { it.categoryId }, { it.categoryName }, { it.amount }),
                            shops = slices(report.orders, { it.shopId }, { it.shopName }, { it.total }),
                        )
                    }
                }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { it.copy(loading = false, error = err.toApiUiText()) }
                }
        }
    }

    private fun bars(range: ReportRange, orders: List<SpendOrderDto>, tz: TimeZone): List<SpendBar> {
        val monthly = range.start.daysUntil(range.endExclusive) > MAX_DAILY_BARS
        val out = mutableListOf<SpendBar>()
        var cursor = if (monthly) LocalDate(range.start.year, range.start.month, 1) else range.start
        while (cursor < range.endExclusive) {
            val next = cursor.plus(1, if (monthly) DateTimeUnit.MONTH else DateTimeUnit.DAY)
            val from = maxOf(cursor, range.start).atStartOfDayIn(tz).toEpochMilliseconds()
            val to = minOf(next, range.endExclusive).atStartOfDayIn(tz).toEpochMilliseconds()
            val amount = orders.filter { it.createdAtEpochMs in from until to }.sumOf { it.total }
            out += SpendBar(maxOf(cursor, range.start), minOf(next, range.endExclusive), monthly, amount)
            cursor = next
        }
        return out
    }

    private fun today(tz: TimeZone): LocalDate =
        Instant.fromEpochMilliseconds(AppClock.nowMillis()).toLocalDateTime(tz).date

    /** Date pickers hand back UTC midnight. */
    private fun utcDate(ms: Long): LocalDate = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.UTC).date

    private companion object {
        const val MAX_DAILY_BARS = 31
    }
}

private inline fun <T> slices(
    rows: List<T>,
    key: (T) -> String,
    label: (T) -> String,
    amount: (T) -> Double,
): List<SpendSlice> =
    rows.groupBy(key)
        .map { (k, group) -> SpendSlice(k, label(group.first()), group.sumOf(amount)) }
        .filter { it.amount > 0.0 }
        .sortedByDescending { it.amount }

/** Bills behind one bar or slice; category drill-downs keep only the matching lines of each bill. */
data class SpendDetailsUiState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val orders: List<SpendOrderDto> = emptyList(),
    val total: Double = 0.0,
)

class SpendDetailsViewModel(private val api: PansariApi) : ViewModel() {
    private val _state = MutableStateFlow(SpendDetailsUiState())
    val state: StateFlow<SpendDetailsUiState> = _state.asStateFlow()
    private var loadedKey: String? = null

    fun load(fromMs: Long, toMs: Long, kind: SpendDrillKind, key: String, force: Boolean = false) {
        val requestKey = "$fromMs|$toMs|$kind|$key"
        if (!force && requestKey == loadedKey) return
        loadedKey = requestKey
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { api.mySpend(fromMs, toMs) }
                .onSuccess { report ->
                    val orders = when (kind) {
                        SpendDrillKind.TIME -> report.orders
                        SpendDrillKind.SHOP -> report.orders.filter { it.shopId == key }
                        SpendDrillKind.CATEGORY -> report.orders.mapNotNull { order ->
                            val lines: List<SpendItemDto> = order.items.filter { it.categoryId == key }
                            if (lines.isEmpty()) null else order.copy(items = lines, total = lines.sumOf { it.amount })
                        }
                    }
                    _state.update { SpendDetailsUiState(loading = false, orders = orders, total = orders.sumOf { it.total }) }
                }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { it.copy(loading = false, error = err.toApiUiText()) }
                }
        }
    }

    fun dismissError() { _state.update { it.copy(error = null) } }
}
