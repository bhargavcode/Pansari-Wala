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
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.domain.model.OrderSummary
import org.bhargav.pansariwala.notification.ShopNotifier
import org.bhargav.pansariwala.settings.CancelOrderReason
import org.bhargav.pansariwala.util.AppClock

data class OrdersWorkspaceUiState(
    val loading: Boolean = true,
    val orders: List<OrderSummary> = emptyList(),
    val selectedOrder: Order? = null,
    val showCancelDialog: Boolean = false,
    val cancelReason: CancelOrderReason = CancelOrderReason.CUSTOMER_REQUEST,
    val customCancelReason: String = "",
    val processingOrder: Boolean = false,
    val cancellingOrder: Boolean = false,
    val todayOrders: Int = 0,
    val todayRevenue: Double = 0.0,
)

class OrdersWorkspaceViewModel(
    private val shopRepository: ShopRepository,
    private val preferences: AppPreferences,
    private val shopNotifier: ShopNotifier,
    private val api: PansariApi,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OrdersWorkspaceUiState())
    val uiState: StateFlow<OrdersWorkspaceUiState> = _uiState.asStateFlow()

    private var shopId: String = "shop_1"
    private var selectedOrderId: String? = null

    init {
        viewModelScope.launch {
            shopId = preferences.getShopId() ?: "shop_1"
            shopRepository.observeRecentOrders(shopId, 50).collect { orders ->
                val startOfDay = AppClock.startOfTodayMillis()
                val today = orders.filter {
                    it.createdAtEpochMs >= startOfDay && it.status == OrderStatus.COMPLETED
                }
                val selected = selectedOrderId?.let { id -> shopRepository.getOrder(id) }
                    ?: orders.firstOrNull()?.let { shopRepository.getOrder(it.id) }
                if (selected != null) selectedOrderId = selected.id
                _uiState.update {
                    it.copy(
                        loading = false,
                        orders = orders,
                        selectedOrder = selected,
                        todayOrders = today.size,
                        todayRevenue = today.sumOf { o -> o.totalValue },
                    )
                }
            }
        }
    }

    fun focusOrder(orderId: String?) {
        if (orderId == null) return
        selectedOrderId = orderId
        viewModelScope.launch {
            _uiState.update { it.copy(selectedOrder = shopRepository.getOrder(orderId)) }
        }
    }

    fun selectOrder(orderId: String) {
        focusOrder(orderId)
    }

    fun openCancelDialog() {
        _uiState.update { it.copy(showCancelDialog = true) }
    }

    fun dismissCancelDialog() {
        _uiState.update {
            it.copy(showCancelDialog = false, customCancelReason = "")
        }
    }

    fun onCancelReasonChange(reason: CancelOrderReason) {
        _uiState.update { it.copy(cancelReason = reason) }
    }

    fun onCustomCancelReasonChange(value: String) {
        _uiState.update { it.copy(customCancelReason = value) }
    }

    fun processOrder() {
        val order = _uiState.value.selectedOrder ?: return
        if (order.status != OrderStatus.RECEIVED && order.status != OrderStatus.DRAFT) return
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
            runCatching { api.cancelShopOrder(order.id, reasonText) }
                .onSuccess { serverUpdated ->
                    shopRepository.saveOrder(serverUpdated)
                    shopNotifier.onOrderCancelled(serverUpdated)
                    selectedOrderId = serverUpdated.id
                    _uiState.update {
                        it.copy(
                            selectedOrder = serverUpdated,
                            showCancelDialog = false,
                            customCancelReason = "",
                            cancellingOrder = false,
                        )
                    }
                }
                .onFailure {
                    // Fallback to local cancellation so the shop UI still updates
                    // even if the network/server is temporarily unavailable.
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
    }
}
