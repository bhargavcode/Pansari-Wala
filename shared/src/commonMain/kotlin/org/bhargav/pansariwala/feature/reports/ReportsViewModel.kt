package org.bhargav.pansariwala.feature.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.domain.model.CategoryStock
import org.bhargav.pansariwala.domain.model.OrderChannel
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.util.AppClock
import org.bhargav.pansariwala.util.AppConstants
import kotlin.time.Instant

data class ReportsUiState(
    val channel: ReportChannel = ReportChannel.ALL,
    val period: ReportPeriod = ReportPeriod.TODAY,
    val range: ReportRange? = null,
    val bucketUnit: BucketUnit = BucketUnit.HOUR,
    val summary: ReportSummary = ReportSummary(),
    val buckets: List<ReportBucket> = emptyList(),
    val inventory: List<CategoryStock> = emptyList(),
    val pageRows: List<ReportOrder> = emptyList(),
    val page: Int = 0,
    val pageCount: Int = 0,
    val totalRows: Int = 0,
    val loading: Boolean = true,
    val error: UiText? = null,
    val showDatePicker: Boolean = false,
)

class ReportsViewModel(
    private val repository: ShopRepository,
    private val preferences: AppPreferences,
    private val api: PansariApi,
) : ViewModel() {
    private val _state = MutableStateFlow(ReportsUiState())
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    private val tz = TimeZone.currentSystemDefault()
    private var shopId: String = SeedData.DEMO_SHOP_ID
    private var customFrom: LocalDate? = null
    private var customTo: LocalDate? = null

    /** Online orders are fetched once per screen session; period/channel changes reuse them. */
    private var onlineOrders: List<ReportOrder>? = null
    private var rangeOrders: List<ReportOrder> = emptyList()
    private var filtered: List<ReportOrder> = emptyList()
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            shopId = preferences.getShopId() ?: SeedData.DEMO_SHOP_ID
            launch { loadInventory() }
            reload()
        }
    }

    fun dismissError() { _state.update { it.copy(error = null) } }

    fun refresh() {
        onlineOrders = null
        viewModelScope.launch { loadInventory() }
        reload()
    }

    fun setChannel(channel: ReportChannel) {
        if (channel == _state.value.channel) return
        _state.update { it.copy(channel = channel) }
        viewModelScope.launch { recompute(resetPage = true) }
    }

    fun setPeriod(period: ReportPeriod) {
        if (period == ReportPeriod.CUSTOM) {
            _state.update { it.copy(showDatePicker = true) }
            return
        }
        if (period == _state.value.period) return
        _state.update { it.copy(period = period) }
        reload()
    }

    fun dismissDatePicker() { _state.update { it.copy(showDatePicker = false) } }

    /** [toUtcMs] null = a single specific date. Picker values are UTC-midnight millis. */
    fun setCustomRange(fromUtcMs: Long, toUtcMs: Long?) {
        customFrom = utcMillisToDate(fromUtcMs)
        customTo = toUtcMs?.let(::utcMillisToDate) ?: customFrom
        _state.update { it.copy(period = ReportPeriod.CUSTOM, showDatePicker = false) }
        reload()
    }

    fun goToPage(page: Int) {
        val state = _state.value
        val target = page.coerceIn(0, (state.pageCount - 1).coerceAtLeast(0))
        if (target == state.page) return
        _state.update { it.copy(page = target, pageRows = pageSlice(target)) }
    }

    private fun reload() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val today = Instant.fromEpochMilliseconds(AppClock.nowMillis()).toLocalDateTime(tz).date
            val range = ReportCalculator.range(_state.value.period, today, customFrom, customTo)
            val fromMs = range.fromMs(tz)
            val toMs = range.toMs(tz)
            val onlineDeferred = async { fetchOnlineOrders() }
            val offline = runCatching { repository.ordersBetween(shopId, fromMs, toMs) }
                .getOrElse { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { it.copy(error = err.toApiUiText()) }
                    emptyList()
                }
                .map {
                    ReportOrder(
                        id = it.id,
                        displayNumber = it.id.substringAfterLast('_').takeLast(AppConstants.PUBLIC_ORDER_CODE_LENGTH).uppercase(),
                        createdAtEpochMs = it.createdAtEpochMs,
                        channel = OrderChannel.POS,
                        customerName = it.customerName,
                        itemCount = it.itemCount,
                        total = it.totalValue,
                        status = it.status,
                    )
                }
            val online = onlineDeferred.await().filter { it.createdAtEpochMs in fromMs until toMs }
            rangeOrders = withContext(Dispatchers.Default) {
                (online + offline).sortedByDescending { it.createdAtEpochMs }
            }
            _state.update { it.copy(range = range, bucketUnit = ReportCalculator.bucketUnit(range)) }
            recompute(resetPage = true)
            _state.update { it.copy(loading = false) }
        }
    }

    private suspend fun fetchOnlineOrders(): List<ReportOrder> {
        onlineOrders?.let { return it }
        return runCatching { api.shopOnlineOrders() }
            .map { orders ->
                orders.map {
                    ReportOrder(
                        id = it.id,
                        displayNumber = it.displayNumber,
                        createdAtEpochMs = it.createdAtEpochMs,
                        channel = OrderChannel.ONLINE,
                        customerName = it.customerName,
                        itemCount = it.itemCount,
                        total = it.totalValue,
                        status = it.status,
                    )
                }
            }
            .onSuccess { onlineOrders = it }
            .getOrElse { err ->
                err.rethrowIfStructuredCancellation()
                _state.update { it.copy(error = err.toApiUiText()) }
                emptyList()
            }
    }

    private suspend fun recompute(resetPage: Boolean) {
        val state = _state.value
        val range = state.range ?: return
        val channel = state.channel
        val (list, summary, buckets) = withContext(Dispatchers.Default) {
            val list = ReportCalculator.filterChannel(rangeOrders, channel)
            Triple(
                list,
                ReportCalculator.summary(list),
                ReportCalculator.buckets(list, range, state.bucketUnit, tz),
            )
        }
        filtered = list
        val pageCount = (list.size + AppConstants.REPORT_PAGE_SIZE - 1) / AppConstants.REPORT_PAGE_SIZE
        val page = if (resetPage) 0 else state.page.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
        _state.update {
            it.copy(
                summary = summary,
                buckets = buckets,
                totalRows = list.size,
                pageCount = pageCount,
                page = page,
                pageRows = pageSlice(page),
            )
        }
    }

    private fun pageSlice(page: Int): List<ReportOrder> {
        val from = page * AppConstants.REPORT_PAGE_SIZE
        if (from >= filtered.size) return emptyList()
        return filtered.subList(from, minOf(from + AppConstants.REPORT_PAGE_SIZE, filtered.size))
    }

    private suspend fun loadInventory() {
        val products = runCatching { repository.observeProducts(shopId).first() }.getOrNull() ?: return
        val breakdown = withContext(Dispatchers.Default) {
            products.groupBy { it.category }
                .map { (category, list) ->
                    CategoryStock(category, list.size, list.sumOf { it.stockValue })
                }
                .sortedByDescending { it.stockValue }
        }
        _state.update { it.copy(inventory = breakdown) }
    }

    private fun utcMillisToDate(ms: Long): LocalDate =
        Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.UTC).date
}
