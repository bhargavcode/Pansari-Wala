package org.bhargav.pansariwala.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.JwtAuthCache
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.domain.model.MoneyTxn
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.CustomerProfile
import org.bhargav.pansariwala.domain.model.PaymentIntent
import org.bhargav.pansariwala.domain.model.PaymentIntentStatus
import org.bhargav.pansariwala.notification.OrderUpdates
import org.bhargav.pansariwala.notification.PaymentUpdates
import org.bhargav.pansariwala.ui.AsyncUiState
import org.bhargav.pansariwala.ui.beginLoad
import org.bhargav.pansariwala.util.AppConstants

data class AccountData(
    val orders: List<Order> = emptyList(),
    /** Checkouts not yet an order: pending, failed or refunded payments. */
    val payments: List<PaymentIntent> = emptyList(),
    val paymentBusyId: String? = null,
    val txns: List<MoneyTxn> = emptyList(),
    val profile: CustomerProfile? = null,
    val ordersPage: Int = 0,
    val ordersHasMore: Boolean = false,
    val ordersTotal: Int = 0,
    val loadingMoreOrders: Boolean = false,
) {
    val recent: List<Order> get() = orders.take(AppConstants.RECENT_ORDERS_CARD_LIMIT)
}

typealias AccountUiState = AsyncUiState<AccountData>

class AccountViewModel(
    private val api: PansariApi,
    private val preferences: AppPreferences,
    private val cart: CartStore,
) : ViewModel() {
    private val _state = MutableStateFlow<AccountUiState>(AsyncUiState.Idle)
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.getCachedCustomerProfile()?.let { cached ->
                _state.value = AsyncUiState.Success(AccountData(profile = cached))
            }
        }
        refresh()
        viewModelScope.launch {
            PaymentUpdates.events.collect { refresh() }
        }
        viewModelScope.launch {
            OrderUpdates.events.collect { update ->
                val current = _state.value as? AsyncUiState.Success ?: return@collect
                val status = update.status
                if (status == null || current.data.orders.none { it.id == update.orderId }) {
                    refresh()
                    return@collect
                }
                _state.value = current.copy(
                    data = current.data.copy(
                        orders = current.data.orders.map { if (it.id == update.orderId) it.copy(status = status) else it },
                    ),
                )
            }
        }
    }

    fun dismissError() {
        _state.value = when (val current = _state.value) {
            is AsyncUiState.Error -> AsyncUiState.Idle
            is AsyncUiState.Success -> current.copy(bannerError = null)
            else -> current
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.beginLoad()
            runCatching {
                coroutineScope {
                    val orders = async { api.myOrdersPage(0, AppConstants.ORDER_PAGE_SIZE) }
                    val txns = async { api.myTransactions() }
                    val profile = async { api.me() }
                    val payments = async {
                        runCatching { api.myPaymentIntents() }
                            .onFailure { it.rethrowIfStructuredCancellation() }
                            .getOrDefault(emptyList())
                    }
                    val page = orders.await()
                    AccountData(
                        orders = page.items,
                        payments = payments.await(),
                        txns = txns.await(),
                        profile = profile.await(),
                        ordersPage = 0,
                        ordersHasMore = page.hasMore,
                        ordersTotal = page.totalCount,
                    )
                }
            }.onSuccess { data ->
                data.profile?.let { preferences.setCachedCustomerProfile(it) }
                _state.value = AsyncUiState.Success(data)
            }.onFailure { error ->
                error.rethrowIfStructuredCancellation()
                val message = error.toApiUiText()
                _state.value = when (val current = _state.value) {
                    is AsyncUiState.Success -> current.copy(isRefreshing = false, bannerError = message)
                    else -> AsyncUiState.Error(message)
                }
            }
        }
    }

    /** Asks the server for the latest payment state; an overdue payment comes back FAILED so it can be retried. */
    fun checkPayment(intentId: String) {
        runPaymentAction(intentId) {
            val fresh = api.refreshPaymentIntent(intentId)
            if (fresh.status == PaymentIntentStatus.SUCCESS) refresh() else replacePayment(fresh)
        }
    }

    /** Re-checks first (the payment may have arrived), then refills the cart for a new attempt. */
    fun retryPayment(intentId: String, onCartReady: (String) -> Unit) {
        runPaymentAction(intentId) {
            val current = (_state.value as? AsyncUiState.Success)?.data?.payments?.firstOrNull { it.id == intentId }
                ?: return@runPaymentAction
            val fresh = if (current.status == PaymentIntentStatus.PENDING) api.refreshPaymentIntent(intentId) else current
            when (fresh.status) {
                PaymentIntentStatus.SUCCESS -> refresh()
                PaymentIntentStatus.PENDING -> replacePayment(fresh)
                else -> {
                    restoreCartForRetry(api, cart, fresh)
                    onCartReady(fresh.shopId)
                }
            }
        }
    }

    private fun runPaymentAction(intentId: String, block: suspend () -> Unit) {
        val current = _state.value as? AsyncUiState.Success ?: return
        if (current.data.paymentBusyId != null) return
        _state.value = current.copy(data = current.data.copy(paymentBusyId = intentId))
        viewModelScope.launch {
            runCatching { block() }.onFailure { error ->
                error.rethrowIfStructuredCancellation()
                val latest = _state.value as? AsyncUiState.Success ?: return@onFailure
                _state.value = latest.copy(bannerError = error.toApiUiText())
            }
            val latest = _state.value as? AsyncUiState.Success ?: return@launch
            _state.value = latest.copy(data = latest.data.copy(paymentBusyId = null))
        }
    }

    private fun replacePayment(intent: PaymentIntent) {
        val current = _state.value as? AsyncUiState.Success ?: return
        _state.value = current.copy(
            data = current.data.copy(payments = current.data.payments.map { if (it.id == intent.id) intent else it }),
        )
    }

    fun loadMoreOrders() {
        val current = _state.value as? AsyncUiState.Success ?: return
        val data = current.data
        if (!data.ordersHasMore || data.loadingMoreOrders) return
        _state.value = current.copy(data = data.copy(loadingMoreOrders = true))
        viewModelScope.launch {
            val nextPage = data.ordersPage + 1
            runCatching { api.myOrdersPage(nextPage, AppConstants.ORDER_PAGE_SIZE) }
                .onSuccess { page ->
                    val latest = _state.value as? AsyncUiState.Success ?: return@onSuccess
                    val seen = latest.data.orders.mapTo(HashSet()) { it.id }
                    _state.value = latest.copy(
                        data = latest.data.copy(
                            orders = latest.data.orders + page.items.filterNot { it.id in seen },
                            ordersPage = nextPage,
                            ordersHasMore = page.hasMore,
                            ordersTotal = page.totalCount,
                            loadingMoreOrders = false,
                        ),
                    )
                }
                .onFailure { error ->
                    error.rethrowIfStructuredCancellation()
                    val latest = _state.value as? AsyncUiState.Success ?: return@onFailure
                    _state.value = latest.copy(
                        bannerError = error.toApiUiText(),
                        data = latest.data.copy(loadingMoreOrders = false),
                    )
                }
        }
    }
}

class UserSettingsViewModel(
    private val preferences: AppPreferences,
    private val api: PansariApi,
) : ViewModel() {
    val settings = preferences.userSettings
    val radius = preferences.observeSearchRadiusKm()
    private val _minRadius = MutableStateFlow(AppConstants.MIN_SEARCH_RADIUS_KM)
    /** Lower bound of the radius slider; configured by the platform admin. */
    val minRadius: StateFlow<Double> = _minRadius.asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { api.publicConfig().minUserSearchRadiusKm }
                .onSuccess { km ->
                    _minRadius.value = km.coerceIn(AppConstants.SEARCH_RADIUS_STEP_KM, AppConstants.MAX_SEARCH_RADIUS_KM - 1)
                }
                .onFailure { it.rethrowIfStructuredCancellation() }
        }
    }

    fun setRadius(km: Double) { viewModelScope.launch { preferences.setSearchRadiusKm(km) } }
    fun setNotifyOffers(value: Boolean) { viewModelScope.launch { preferences.setNotifyOffers(value) } }
    fun setNotifyDelivery(value: Boolean) { viewModelScope.launch { preferences.setNotifyDelivery(value) } }
    fun setNotifyOrderEvents(value: Boolean) { viewModelScope.launch { preferences.setNotifyOrderEvents(value) } }
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            preferences.clearSession()
            JwtAuthCache.invalidate()
            onDone()
        }
    }
}
