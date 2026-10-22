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
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.domain.model.ProductCategory
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.util.AppClock
import kotlin.time.Instant

data class SalesDrillUiState(
    val period: ReportPeriod = ReportPeriod.MONTH,
    val channel: ReportChannel = ReportChannel.ALL,
    val range: ReportRange? = null,
    val bucketUnit: BucketUnit = BucketUnit.DAY,
    /** Category mode: products of the category, most ordered first. */
    val products: List<ProductSales> = emptyList(),
    val categoryTotal: CategorySales? = null,
    /** Product mode: totals and order count per bar. */
    val product: ProductSales? = null,
    val buckets: List<ReportBucket> = emptyList(),
    val loading: Boolean = true,
    val error: UiText? = null,
    val showDatePicker: Boolean = false,
)

/** Category → products and product → order-count bars, both over a selectable period. */
class SalesDrillViewModel(
    private val repository: ShopRepository,
    private val preferences: AppPreferences,
    private val api: PansariApi,
) : ViewModel() {
    private val _state = MutableStateFlow(SalesDrillUiState())
    val state: StateFlow<SalesDrillUiState> = _state.asStateFlow()

    private val tz = TimeZone.currentSystemDefault()
    private var started = false
    private var category: ProductCategory? = null
    private var productId: String? = null
    private var customFrom: LocalDate? = null
    private var customTo: LocalDate? = null
    private var loadJob: Job? = null

    fun start(category: String?, productId: String?, window: SalesWindow) {
        if (started) return
        started = true
        this.category = category?.let(ProductCategory::fromName)
        this.productId = productId
        customFrom = window.customFromUtc?.let(::utcDate)
        customTo = window.customToUtc?.let(::utcDate)
        val initialPeriod = ReportPeriod.entries.firstOrNull { it.name == window.period } ?: ReportPeriod.MONTH
        val initialChannel = ReportChannel.entries.firstOrNull { it.name == window.channel } ?: ReportChannel.ALL
        _state.update { it.copy(period = initialPeriod, channel = initialChannel) }
        reload()
    }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun refresh() = reload()

    fun setPeriod(period: ReportPeriod) {
        if (period == ReportPeriod.CUSTOM) {
            _state.update { it.copy(showDatePicker = true) }
            return
        }
        if (period == _state.value.period) return
        _state.update { it.copy(period = period) }
        reload()
    }

    fun setChannel(channel: ReportChannel) {
        if (channel == _state.value.channel) return
        _state.update { it.copy(channel = channel) }
        reload()
    }

    fun dismissDatePicker() = _state.update { it.copy(showDatePicker = false) }

    fun setCustomRange(fromUtcMs: Long, toUtcMs: Long?) {
        customFrom = utcDate(fromUtcMs)
        customTo = toUtcMs?.let(::utcDate) ?: customFrom
        _state.update { it.copy(period = ReportPeriod.CUSTOM, showDatePicker = false) }
        reload()
    }

    fun window(): SalesWindow = salesWindow(_state.value.period, _state.value.channel, customFrom, customTo)

    private fun reload() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val shopId = preferences.getShopId() ?: SeedData.DEMO_SHOP_ID
            val today = Instant.fromEpochMilliseconds(AppClock.nowMillis()).toLocalDateTime(tz).date
            val range = ReportCalculator.range(_state.value.period, today, customFrom, customTo)
            val fromMs = range.fromMs(tz)
            val toMs = range.toMs(tz)
            val online = async {
                runCatching { api.shopOnlineOrders(fromMs, toMs) }.getOrElse { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { it.copy(error = err.toApiUiText()) }
                    emptyList()
                }
            }
            val categories = async {
                runCatching { repository.observeProducts(shopId).first() }.getOrDefault(emptyList())
                    .associate { it.id to it.category }
            }
            val offline = runCatching { repository.itemSalesBetween(shopId, fromMs, toMs) }.getOrElse { err ->
                err.rethrowIfStructuredCancellation()
                _state.update { it.copy(error = err.toApiUiText()) }
                emptyList()
            }
            val onlineOrders = online.await().filter { it.createdAtEpochMs in fromMs until toMs }
            val categoryOf = categories.await()
            val unit = ReportCalculator.bucketUnit(range)
            val channel = _state.value.channel
            val wantedCategory = category
            val wantedProduct = productId
            val result = withContext(Dispatchers.Default) {
                val all = SalesBreakdown.filterChannel(offline + SalesBreakdown.onlineItemSales(onlineOrders), channel)
                val items = when {
                    wantedProduct != null -> all.filter { it.productId == wantedProduct }
                    wantedCategory != null -> all.filter {
                        (categoryOf[it.productId] ?: ProductCategory.GENERAL_GROCERY) == wantedCategory
                    }
                    else -> all
                }
                val products = SalesBreakdown.byProduct(items)
                val categoryTotal = wantedCategory?.let { c ->
                    SalesBreakdown.byCategory(items) { c }.firstOrNull()
                }
                val buckets = if (wantedProduct != null) {
                    ReportCalculator.buckets(SalesBreakdown.asOrders(items), range, unit, tz)
                } else {
                    emptyList()
                }
                Triple(products, categoryTotal, buckets)
            }
            _state.update {
                it.copy(
                    range = range,
                    bucketUnit = unit,
                    products = result.first,
                    categoryTotal = result.second,
                    product = if (wantedProduct != null) result.first.firstOrNull() else null,
                    buckets = result.third,
                    loading = false,
                )
            }
        }
    }

    private fun utcDate(ms: Long): LocalDate = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.UTC).date
}

internal fun salesWindow(
    period: ReportPeriod,
    channel: ReportChannel,
    customFrom: LocalDate?,
    customTo: LocalDate?,
): SalesWindow = SalesWindow(
    period = period.name,
    channel = channel.name,
    customFromUtc = customFrom?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
    customToUtc = customTo?.atStartOfDayIn(TimeZone.UTC)?.toEpochMilliseconds(),
)
