package org.bhargav.pansariwala.feature.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.domain.model.OrderSummary
import org.bhargav.pansariwala.domain.model.publicOrderNumber
import org.bhargav.pansariwala.notification.ShopNotifier
import org.bhargav.pansariwala.settings.CancelOrderReason
import org.bhargav.pansariwala.util.AppClock

private const val ORDERS_WINDOW = 200

enum class OrdersFilter { ALL, OPEN, COMPLETED, CANCELLED }

private val OPEN_STATUSES = setOf(OrderStatus.RECEIVED, OrderStatus.DRAFT)

fun OrderStatus.isOpenPos(): Boolean = this in OPEN_STATUSES

data class OrdersWorkspaceUiState(
    val loading: Boolean = true,
    val shopId: String = SeedData.DEMO_SHOP_ID,
    val orders: List<OrderSummary> = emptyList(),
    val visibleOrders: List<OrderSummary> = emptyList(),
    val query: String = "",
    val filter: OrdersFilter = OrdersFilter.ALL,
    val filterCounts: Map<OrdersFilter, Int> = emptyMap(),
    val selectedOrder: Order? = null,
    val showCancelDialog: Boolean = false,
    val cancelReason: CancelOrderReason = CancelOrderReason.CUSTOMER_REQUEST,
    val customCancelReason: String = "",
    val processingOrder: Boolean = false,
    val cancellingOrder: Boolean = false,
    val todayOrders: Int = 0,
    val todayRevenue: Double = 0.0,
    val todayAverage: Double = 0.0,
    val todayCancelled: Int = 0,
)

class OrdersWorkspaceViewModel(
    private val shopRepository: ShopRepository,
    private val preferences: AppPreferences,
    private val shopNotifier: ShopNotifier,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OrdersWorkspaceUiState())
    val uiState: StateFlow<OrdersWorkspaceUiState> = _uiState.asStateFlow()

    private var selectedOrderId: String? = null

    init {
        viewModelScope.launch {
            val shopId = preferences.getShopId() ?: SeedData.DEMO_SHOP_ID
            _uiState.update { it.copy(shopId = shopId) }
            shopRepository.observeRecentOrders(shopId, ORDERS_WINDOW).collect { orders ->
                val startOfDay = AppClock.startOfTodayMillis()
                val today = orders.filter { it.createdAtEpochMs >= startOfDay }
                val todayDone = today.filter { it.status == OrderStatus.COMPLETED }
                val revenue = todayDone.sumOf { it.totalValue }
                val selected = (selectedOrderId ?: orders.firstOrNull()?.id)?.let { shopRepository.getOrder(it) }
                if (selected != null) selectedOrderId = selected.id
                _uiState.update {
                    it.copy(
                        loading = false,
                        orders = orders,
                        selectedOrder = selected,
                        todayOrders = todayDone.size,
                        todayRevenue = revenue,
                        todayAverage = if (todayDone.isEmpty()) 0.0 else revenue / todayDone.size,
                        todayCancelled = today.count { o -> o.status == OrderStatus.CANCELLED },
                        filterCounts = OrdersFilter.entries.associateWith { f -> orders.count { o -> f.matches(o) } },
                    ).withVisible()
                }
            }
        }
    }

    fun onQueryChange(value: String) = _uiState.update { it.copy(query = value).withVisible() }

    fun onFilterChange(filter: OrdersFilter) = _uiState.update { it.copy(filter = filter).withVisible() }

    fun focusOrder(orderId: String?) {
        if (orderId == null) return
        selectedOrderId = orderId
        viewModelScope.launch {
            _uiState.update { it.copy(selectedOrder = shopRepository.getOrder(orderId)) }
        }
    }

    fun selectOrder(orderId: String) = focusOrder(orderId)

    fun openCancelDialog() {
        _uiState.update { it.copy(showCancelDialog = true) }
    }

    fun dismissCancelDialog() {
        _uiState.update { it.copy(showCancelDialog = false, customCancelReason = "") }
    }

    fun onCancelReasonChange(reason: CancelOrderReason) {
        _uiState.update { it.copy(cancelReason = reason) }
    }

    fun onCustomCancelReasonChange(value: String) {
        _uiState.update { it.copy(customCancelReason = value) }
    }

    fun processOrder() {
        val order = _uiState.value.selectedOrder ?: return
        if (!order.status.isOpenPos()) return
        viewModelScope.launch {
            _uiState.update { it.copy(processingOrder = true) }
            val updated = order.copy(status = OrderStatus.COMPLETED)
            shopRepository.saveOrder(updated)
            selectedOrderId = updated.id
            _uiState.update { it.copy(selectedOrder = updated, processingOrder = false) }
        }
    }

    fun confirmCancelOrder() {
        val order = _uiState.value.selectedOrder ?: return
        if (order.status == OrderStatus.CANCELLED) return
        val state = _uiState.value
        val reasonText = if (state.cancelReason == CancelOrderReason.OTHER) {
            state.customCancelReason.trim().ifBlank { state.cancelReason.name }
        } else {
            state.cancelReason.name
        }
        viewModelScope.launch {
            _uiState.update { it.copy(cancellingOrder = true) }
            val cancelled = order.copy(status = OrderStatus.CANCELLED, cancelReason = reasonText)
            shopRepository.saveOrder(cancelled)
            shopNotifier.onOrderCancelled(cancelled)
            selectedOrderId = cancelled.id
            _uiState.update {
                it.copy(
                    selectedOrder = cancelled,
                    showCancelDialog = false,
                    customCancelReason = "",
                    cancellingOrder = false,
                )
            }
        }
    }

    private fun OrdersWorkspaceUiState.withVisible(): OrdersWorkspaceUiState {
        val q = query.trim()
        val visible = orders.filter { o ->
            filter.matches(o) && (
                q.isEmpty() ||
                    o.customerName.orEmpty().contains(q, ignoreCase = true) ||
                    publicOrderNumber(shopId, o.id).contains(q, ignoreCase = true)
                )
        }
        return copy(visibleOrders = visible)
    }
}

private fun OrdersFilter.matches(order: OrderSummary): Boolean = when (this) {
    OrdersFilter.ALL -> true
    OrdersFilter.OPEN -> order.status.isOpenPos()
    OrdersFilter.COMPLETED -> order.status == OrderStatus.COMPLETED
    OrdersFilter.CANCELLED -> order.status == OrderStatus.CANCELLED
}
