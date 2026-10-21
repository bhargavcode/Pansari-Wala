package org.bhargav.pansariwala.feature.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.ShopPartnerDto
import org.bhargav.pansariwala.api.ShopPartnerRequest
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.util.AppConstants
import pansariwala.shared.generated.resources.error_otp_invalid
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.notification.NotificationGateway
import org.bhargav.pansariwala.notification.OrderUpdate
import org.bhargav.pansariwala.notification.OrderUpdates
import org.bhargav.pansariwala.notification.PaymentUpdates
import org.bhargav.pansariwala.domain.model.PaymentIntent
import org.bhargav.pansariwala.domain.model.PaymentIntentStatus
import org.bhargav.pansariwala.util.asMoney
import pansariwala.shared.generated.resources.shop_pay_confirmed
import pansariwala.shared.generated.resources.shop_pay_refund_needed
import pansariwala.shared.generated.resources.shop_pay_rejected
import org.bhargav.pansariwala.settings.CancelOrderReason
import pansariwala.shared.generated.resources.Res
/** POS online-order filter tabs; [statuses] empty → every status. */
enum class OnlineOrderTab(val statuses: List<OrderStatus>) {
    /** Every open order: not yet delivered, completed, rejected or cancelled. */
    ACTIVE(
        listOf(
            OrderStatus.RECEIVED,
            OrderStatus.ACCEPTED,
            OrderStatus.PACKING,
            OrderStatus.LOOKING_FOR_PARTNER,
            OrderStatus.PARTNER_ACCEPTED,
            OrderStatus.ON_THE_WAY,
        ),
    ),
    NEW(listOf(OrderStatus.RECEIVED)),
    ACCEPTED(listOf(OrderStatus.ACCEPTED)),
    PACKING(listOf(OrderStatus.PACKING)),
    DELIVERY(listOf(OrderStatus.LOOKING_FOR_PARTNER, OrderStatus.PARTNER_ACCEPTED, OrderStatus.ON_THE_WAY)),
    DELIVERED(listOf(OrderStatus.DELIVERED, OrderStatus.COMPLETED)),
    CANCELLED(listOf(OrderStatus.CANCELLED, OrderStatus.REJECTED)),
    ALL(emptyList()),
    ;

    fun accepts(status: OrderStatus): Boolean = statuses.isEmpty() || status in statuses
}

data class OnlineOrdersUiState(
    val orders: List<Order> = emptyList(),
    val error: UiText? = null,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val tab: OnlineOrderTab = OnlineOrderTab.NEW,
    val query: String = "",
    val page: Int = 0,
    val hasMore: Boolean = false,
    /** Orders not yet accepted (RECEIVED) across all tabs — drives the badge. */
    val newCount: Int = 0,
    val busyId: String? = null,
    val cancelOrderId: String? = null,
    val cancelReason: CancelOrderReason = CancelOrderReason.CUSTOMER_REQUEST,
    val customCancelReason: String = "",
    /** orderId → productIds checked into the delivery packet while PACKING. */
    val packedChecks: Map<String, Set<String>> = emptyMap(),
    /** Order whose self-delivery is being completed; shows the OTP dialog. */
    val selfDeliverOrderId: String? = null,
    val selfDeliverOtp: String = "",
    val selfDeliverError: UiText? = null,
    /** Order being handed to one of the shop's own riders; shows the roster sheet. */
    val assignOrderId: String? = null,
    val shopPartners: List<ShopPartnerDto> = emptyList(),
    val partnersLoading: Boolean = false,
    val partnerForm: ShopPartnerForm? = null,
    val partnerError: UiText? = null,
    /** DIRECT_UPI payments the shop must confirm against its bank before the order exists. */
    val paymentIntents: List<PaymentIntent> = emptyList(),
    val paymentBusyId: String? = null,
    val paymentNotice: UiText? = null,
)

data class ShopPartnerForm(val name: String = "", val phone: String = "", val vehicleReg: String = "") {
    val valid: Boolean get() = name.trim().length >= 2 && phone.count { it.isDigit() } >= 10
}

/**
 * Live via FCM ([OrderUpdates]) — no polling. Loads on screen resume (covers missed pushes / app
 * returning to foreground); while visible, tray alerts for listed orders are cleared as consumed.
 */
class OnlineOrdersViewModel(
    private val api: PansariApi,
    private val notifications: NotificationGateway,
) : ViewModel() {
    private val _uiState = MutableStateFlow(OnlineOrdersUiState())
    val uiState: StateFlow<OnlineOrdersUiState> = _uiState.asStateFlow()
    private var visible = false
    private var loadedOnce = false
    private var loadJob: Job? = null
    private var searchJob: Job? = null

    fun dismissError() { _uiState.update { it.copy(error = null) } }

    init {
        viewModelScope.launch {
            OrderUpdates.events.collect(::onOrderUpdate)
        }
        viewModelScope.launch {
            PaymentUpdates.events.collect {
                loadPaymentIntents()
                if (visible) notifications.clearOrder(it)
            }
        }
    }

    private suspend fun loadPaymentIntents() {
        runCatching { api.shopPaymentIntents() }
            .onSuccess { list -> _uiState.update { it.copy(paymentIntents = list) } }
            .onFailure { it.rethrowIfStructuredCancellation() }
    }

    fun confirmShopPayment(intentId: String) = runPaymentAction(intentId) {
        val result = api.confirmShopPayment(intentId)
        when (result.status) {
            PaymentIntentStatus.SUCCESS -> UiText.res(Res.string.shop_pay_confirmed, result.orderId)
            PaymentIntentStatus.REFUNDED -> UiText.res(Res.string.shop_pay_refund_needed, result.amountInr.asMoney())
            else -> null
        }
    }

    fun rejectShopPayment(intentId: String) = runPaymentAction(intentId) {
        api.rejectShopPayment(intentId)
        UiText.res(Res.string.shop_pay_rejected)
    }

    fun consumePaymentNotice() { _uiState.update { it.copy(paymentNotice = null) } }

    private fun runPaymentAction(intentId: String, block: suspend () -> UiText?) {
        if (_uiState.value.paymentBusyId != null) return
        viewModelScope.launch {
            _uiState.update { it.copy(paymentBusyId = intentId) }
            runCatching { block() }
                .onSuccess { notice -> _uiState.update { it.copy(paymentNotice = notice) } }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _uiState.update { it.copy(error = mapOnlineOrdersError(err)) }
                }
            _uiState.update { it.copy(paymentBusyId = null) }
            notifications.clearOrder(intentId)
            loadPaymentIntents()
            refresh(silent = true)
        }
    }

    fun onScreenVisible(isVisible: Boolean) {
        visible = isVisible
        if (isVisible) refresh(silent = loadedOnce)
    }

    fun selectTab(tab: OnlineOrderTab) {
        if (tab == _uiState.value.tab) return
        _uiState.update { it.copy(tab = tab, orders = emptyList(), page = 0, hasMore = false) }
        refresh()
    }

    fun setQuery(value: String) {
        _uiState.update { it.copy(query = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(AppConstants.ORDER_SEARCH_DEBOUNCE_MS)
            refresh()
        }
    }

    /** FCM payload → patch the row in place (no full reload), so paging position is kept. */
    private fun onOrderUpdate(update: OrderUpdate) {
        val status = update.status
        val known = _uiState.value.orders.any { it.id == update.orderId }
        viewModelScope.launch {
            // Partner assignment / new orders carry details the payload doesn't include.
            val current = _uiState.value.orders.firstOrNull { it.id == update.orderId }
            if (!known || current == null || status == null || status == OrderStatus.PARTNER_ACCEPTED) {
                reloadOrder(update.orderId)
            } else {
                upsertOrder(current.copy(status = status))
            }
            refreshNewCount()
            if (visible) notifications.clearOrder(update.orderId)
        }
    }

    private suspend fun reloadOrder(orderId: String) {
        val fresh = runCatching { api.order(orderId) }
            .onFailure { it.rethrowIfStructuredCancellation() }
            .getOrNull() ?: return
        upsertOrder(fresh)
    }

    /** Action responses omit the customer rating summary; keep the one loaded with the list. */
    private fun Order.keepCustomerRating(previous: Order): Order =
        if (customerRatingAvg != null) this
        else copy(customerRatingAvg = previous.customerRatingAvg, customerRatingCount = previous.customerRatingCount)

    /** Replaces, inserts (newest first) or removes [order] depending on whether it fits the current tab/search. */
    private fun upsertOrder(order: Order) {
        _uiState.update { state ->
            val exists = state.orders.any { it.id == order.id }
            val fits = state.tab.accepts(order.status) && order.matchesQuery(state.query)
            val orders = when {
                exists && fits -> state.orders.map { if (it.id == order.id) order.keepCustomerRating(it) else it }
                exists -> state.orders.filterNot { it.id == order.id }
                fits -> listOf(order) + state.orders
                else -> state.orders
            }
            state.copy(orders = orders)
        }
    }

    private fun Order.matchesQuery(query: String): Boolean {
        val q = query.trim()
        if (q.isEmpty()) return true
        return id.contains(q, ignoreCase = true) ||
            customerName.orEmpty().contains(q, ignoreCase = true) ||
            customerPhone.orEmpty().contains(q, ignoreCase = true)
    }

    private suspend fun refreshNewCount() {
        runCatching { api.shopOnlineOrdersPage(OnlineOrderTab.NEW.statuses, "", 0, 1) }
            .onSuccess { page -> _uiState.update { it.copy(newCount = page.newCount) } }
            .onFailure { it.rethrowIfStructuredCancellation() }
    }

    /** Reloads the first page for the current tab/search. [silent] keeps the list visible. */
    fun refresh(silent: Boolean = false, pullToRefresh: Boolean = false) {
        viewModelScope.launch { loadPaymentIntents() }
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    loading = !silent && !pullToRefresh,
                    refreshing = pullToRefresh,
                    loadingMore = false,
                )
            }
            val state = _uiState.value
            runCatching {
                api.shopOnlineOrdersPage(state.tab.statuses, state.query, 0, AppConstants.ORDER_PAGE_SIZE)
            }
                .onSuccess { page ->
                    loadedOnce = true
                    _uiState.update {
                        it.copy(
                            orders = page.items,
                            page = 0,
                            hasMore = page.hasMore,
                            newCount = page.newCount,
                            error = null,
                        )
                    }
                    if (visible) page.items.forEach { notifications.clearOrder(it.id) }
                }
                .onFailure { error ->
                    error.rethrowIfStructuredCancellation()
                    if (!silent) _uiState.update { it.copy(error = mapOnlineOrdersError(error)) }
                }
            _uiState.update { it.copy(loading = false, refreshing = false) }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (!state.hasMore || state.loadingMore || state.loading || state.refreshing) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(loadingMore = true) }
            val nextPage = state.page + 1
            runCatching {
                api.shopOnlineOrdersPage(state.tab.statuses, state.query, nextPage, AppConstants.ORDER_PAGE_SIZE)
            }
                .onSuccess { page ->
                    _uiState.update { current ->
                        val seen = current.orders.mapTo(HashSet()) { it.id }
                        current.copy(
                            orders = current.orders + page.items.filterNot { it.id in seen },
                            page = nextPage,
                            hasMore = page.hasMore,
                            newCount = page.newCount,
                        )
                    }
                }
                .onFailure { error ->
                    error.rethrowIfStructuredCancellation()
                    _uiState.update { it.copy(error = mapOnlineOrdersError(error)) }
                }
            _uiState.update { it.copy(loadingMore = false) }
        }
    }

    fun accept(id: String) = runOrderAction(id) { api.acceptOrder(id) }

    fun markPacking(id: String) = runOrderAction(id) {
        api.setOrderStatus(id, OrderStatus.PACKING.name)
    }

    /** Self-pickup orders end at the counter: the customer collected, no partner involved. */
    fun markCollected(id: String) = runOrderAction(id) {
        api.setOrderStatus(id, OrderStatus.COMPLETED.name)
    }

    fun togglePacked(orderId: String, productId: String) {
        _uiState.update { state ->
            val current = state.packedChecks[orderId].orEmpty()
            val next = if (productId in current) current - productId else current + productId
            state.copy(packedChecks = state.packedChecks + (orderId to next))
        }
    }

    fun setAllPacked(orderId: String, packed: Boolean) {
        val order = _uiState.value.orders.firstOrNull { it.id == orderId } ?: return
        val next = if (packed) order.items.map { it.productId }.toSet() else emptySet()
        _uiState.update { it.copy(packedChecks = it.packedChecks + (orderId to next)) }
    }

    fun requestDeliveryPartner(id: String) {
        val packed = _uiState.value.packedChecks[id].orEmpty()
        if (packed.isEmpty()) return
        runOrderAction(id) {
            api.requestDelivery(id, packed.toList())
            _uiState.update { it.copy(packedChecks = it.packedChecks - id) }
        }
    }

    fun startSelfDelivery(id: String) {
        val order = _uiState.value.orders.firstOrNull { it.id == id } ?: return
        // LOOKING_FOR_PARTNER: packet already confirmed server-side; checks were cleared on request.
        val packed = _uiState.value.packedChecks[id].orEmpty()
            .ifEmpty { order.packedProductIds }
            .ifEmpty { order.items.map { it.productId }.toSet() }
        runOrderAction(id) {
            api.startSelfDelivery(id, packed.toList())
            _uiState.update { it.copy(packedChecks = it.packedChecks - id) }
        }
    }

    fun openAssignPartner(id: String) {
        _uiState.update { it.copy(assignOrderId = id, partnerError = null, partnerForm = null) }
        loadShopPartners()
    }

    fun dismissAssignPartner() {
        _uiState.update { it.copy(assignOrderId = null, partnerForm = null, partnerError = null) }
    }

    private fun loadShopPartners() {
        viewModelScope.launch {
            _uiState.update { it.copy(partnersLoading = true) }
            runCatching { api.shopPartners() }
                .onSuccess { list ->
                    _uiState.update {
                        it.copy(shopPartners = list, partnerForm = if (list.isEmpty()) ShopPartnerForm() else it.partnerForm)
                    }
                }
                .onFailure { error ->
                    error.rethrowIfStructuredCancellation()
                    _uiState.update { it.copy(partnerError = mapOnlineOrdersError(error)) }
                }
            _uiState.update { it.copy(partnersLoading = false) }
        }
    }

    fun openPartnerForm() { _uiState.update { it.copy(partnerForm = ShopPartnerForm(), partnerError = null) } }

    fun closePartnerForm() { _uiState.update { it.copy(partnerForm = null, partnerError = null) } }

    fun updatePartnerForm(transform: (ShopPartnerForm) -> ShopPartnerForm) {
        _uiState.update { state -> state.copy(partnerForm = state.partnerForm?.let(transform), partnerError = null) }
    }

    fun savePartner() {
        val form = _uiState.value.partnerForm?.takeIf { it.valid } ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(partnersLoading = true) }
            runCatching {
                api.addShopPartner(ShopPartnerRequest(form.name.trim(), form.phone.trim(), form.vehicleReg.trim()))
            }
                .onSuccess { added ->
                    _uiState.update { it.copy(shopPartners = (it.shopPartners + added).sortedBy { p -> p.name }, partnerForm = null) }
                }
                .onFailure { error ->
                    error.rethrowIfStructuredCancellation()
                    _uiState.update { it.copy(partnerError = mapOnlineOrdersError(error)) }
                }
            _uiState.update { it.copy(partnersLoading = false) }
        }
    }

    fun removePartner(id: String) {
        viewModelScope.launch {
            runCatching { api.removeShopPartner(id) }
                .onSuccess { _uiState.update { it.copy(shopPartners = it.shopPartners.filterNot { p -> p.id == id }) } }
                .onFailure { error ->
                    error.rethrowIfStructuredCancellation()
                    _uiState.update { it.copy(partnerError = mapOnlineOrdersError(error)) }
                }
        }
    }

    fun assignPartner(shopPartnerId: String) {
        val state = _uiState.value
        val id = state.assignOrderId ?: return
        val order = state.orders.firstOrNull { it.id == id } ?: return
        val packed = state.packedChecks[id].orEmpty().ifEmpty { order.packedProductIds }
        _uiState.update { it.copy(assignOrderId = null, partnerForm = null) }
        runOrderAction(id) {
            api.assignShopPartner(id, shopPartnerId, packed.toList())
            _uiState.update { it.copy(packedChecks = it.packedChecks - id) }
        }
    }

    fun openSelfDeliverComplete(id: String) {
        _uiState.update { it.copy(selfDeliverOrderId = id, selfDeliverOtp = "", selfDeliverError = null) }
    }

    fun dismissSelfDeliverComplete() {
        _uiState.update { it.copy(selfDeliverOrderId = null, selfDeliverOtp = "", selfDeliverError = null) }
    }

    fun onSelfDeliverOtpChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(AppConstants.DELIVERY_OTP_LENGTH)
        _uiState.update { it.copy(selfDeliverOtp = digits, selfDeliverError = null) }
    }

    fun confirmSelfDeliverComplete() {
        val state = _uiState.value
        val id = state.selfDeliverOrderId ?: return
        if (state.selfDeliverOtp.length != AppConstants.DELIVERY_OTP_LENGTH) {
            _uiState.update { it.copy(selfDeliverError = UiText.res(Res.string.error_otp_invalid)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(busyId = id) }
            runCatching { api.completeSelfDelivery(id, state.selfDeliverOtp) }
                .onSuccess {
                    _uiState.update { it.copy(selfDeliverOrderId = null, selfDeliverOtp = "", selfDeliverError = null) }
                }
                .onFailure { error ->
                    error.rethrowIfStructuredCancellation()
                    _uiState.update { it.copy(selfDeliverError = mapOnlineOrdersError(error)) }
                }
            _uiState.update { it.copy(busyId = null) }
            reloadOrder(id)
            refreshNewCount()
        }
    }

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
            reloadOrder(id)
            refreshNewCount()
        }
    }

    private fun mapOnlineOrdersError(error: Throwable): UiText = error.toApiUiText()
}
