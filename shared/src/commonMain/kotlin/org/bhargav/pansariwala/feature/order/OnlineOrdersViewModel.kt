package org.bhargav.pansariwala.feature.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.settings.CancelOrderReason
import org.bhargav.pansariwala.util.AppConstants
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_network
import pansariwala.shared.generated.resources.error_session_expired

data class OnlineOrdersUiState(
    val orders: List<Order> = emptyList(),
    val error: UiText? = null,
    val loading: Boolean = false,
    val busyId: String? = null,
    val cancelOrderId: String? = null,
    val cancelReason: CancelOrderReason = CancelOrderReason.CUSTOMER_REQUEST,
    val customCancelReason: String = "",
)

class OnlineOrdersViewModel(
    private val api: PansariApi,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnlineOrdersUiState())
    val uiState: StateFlow<OnlineOrdersUiState> = _uiState.asStateFlow()

    fun dismissError() { _uiState.update { it.copy(error = null) } }

    init {
        viewModelScope.launch {
            refresh()
            while (true) {
                delay(AppConstants.LIVE_ALERT_POLL_MS)
                refresh(silent = true)
            }
        }
    }

    fun refresh(silent: Boolean = false) {
        viewModelScope.launch {
            if (!silent) _uiState.update { it.copy(loading = true) }
            runCatching { api.shopOnlineOrders() }
                .onSuccess { orders ->
                    _uiState.update { it.copy(orders = orders, error = null) }
                }
                .onFailure { error ->
                    if (!silent) _uiState.update { it.copy(error = mapOnlineOrdersError(error)) }
                }
            _uiState.update { it.copy(loading = false) }
        }
    }

    fun accept(id: String) = runOrderAction(id) { api.acceptOrder(id) }

    fun markPacking(id: String) = runOrderAction(id) {
        api.setOrderStatus(id, OrderStatus.PACKING.name)
    }

    fun requestDeliveryPartner(id: String) = runOrderAction(id) { api.requestDelivery(id) }

    fun openCancel(id: String) {
        _uiState.update {
            it.copy(
                cancelOrderId = id,
                cancelReason = CancelOrderReason.CUSTOMER_REQUEST,
                customCancelReason = "",
            )
        }
    }

    fun dismissCancel() {
        _uiState.update { it.copy(cancelOrderId = null, customCancelReason = "") }
    }

    fun onCancelReasonChange(reason: CancelOrderReason) {
        _uiState.update { it.copy(cancelReason = reason) }
    }

    fun onCustomCancelReasonChange(value: String) {
        _uiState.update { it.copy(customCancelReason = value) }
    }

    fun confirmCancel() {
        val state = _uiState.value
        val id = state.cancelOrderId ?: return
        val reasonText = if (state.cancelReason == CancelOrderReason.OTHER) {
            state.customCancelReason.trim().ifBlank { state.cancelReason.name }
        } else {
            state.cancelReason.name
        }
        _uiState.update { it.copy(cancelOrderId = null, customCancelReason = "") }
        runOrderAction(id) { api.cancelShopOrder(id, reasonText) }
    }

    private fun runOrderAction(id: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(busyId = id) }
            runCatching { block() }
                .onFailure { error ->
                    _uiState.update { state -> state.copy(error = mapOnlineOrdersError(error)) }
                }
            _uiState.update { it.copy(busyId = null) }
            refresh(silent = true)
        }
    }

    private fun mapOnlineOrdersError(error: Throwable): UiText {
        val message = error.message.orEmpty()
        return if (message.contains("401") || message.contains("Unauthorized")) {
            UiText.res(Res.string.error_session_expired)
        } else {
            UiText.res(Res.string.error_network)
        }
    }
}
