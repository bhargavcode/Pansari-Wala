package org.bhargav.pansariwala.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.domain.model.MoneyTxn
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.ui.AsyncUiState
import org.bhargav.pansariwala.ui.beginLoad
import org.bhargav.pansariwala.util.AppConstants

data class AccountData(
    val orders: List<Order> = emptyList(),
    val txns: List<MoneyTxn> = emptyList(),
) {
    val recent: List<Order> get() = orders.take(AppConstants.RECENT_ORDERS_CARD_LIMIT)
}

typealias AccountUiState = AsyncUiState<AccountData>

class AccountViewModel(
    private val api: PansariApi,
) : ViewModel() {
    private val _state = MutableStateFlow<AccountUiState>(AsyncUiState.Idle)
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init { refresh() }

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
                    val orders = async { api.myOrders() }
                    val txns = async { api.myTransactions() }
                    AccountData(orders = orders.await(), txns = txns.await())
                }
            }.onSuccess { data ->
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
}

class UserSettingsViewModel(
    private val preferences: AppPreferences,
) : ViewModel() {
    val settings = preferences.userSettings
    val radius = preferences.observeSearchRadiusKm()

    fun setRadius(km: Double) { viewModelScope.launch { preferences.setSearchRadiusKm(km) } }
    fun setNotifyOffers(value: Boolean) { viewModelScope.launch { preferences.setNotifyOffers(value) } }
    fun setNotifyDelivery(value: Boolean) { viewModelScope.launch { preferences.setNotifyDelivery(value) } }
    fun setNotifyOrderEvents(value: Boolean) { viewModelScope.launch { preferences.setNotifyOrderEvents(value) } }
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            preferences.clearSession()
            onDone()
        }
    }
}
