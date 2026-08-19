package org.bhargav.pansariwala.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.domain.model.MoneyTxn
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.util.AppConstants

data class AccountUiState(
    val recent: List<Order> = emptyList(),
    val orders: List<Order> = emptyList(),
    val txns: List<MoneyTxn> = emptyList(),
    val loading: Boolean = true,
)

class AccountViewModel(
    private val api: PansariApi,
) : ViewModel() {
    private val _state = MutableStateFlow(AccountUiState())
    val state: StateFlow<AccountUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            val orders = runCatching { api.myOrders() }.getOrDefault(emptyList())
            val txns = runCatching { api.myTransactions() }.getOrDefault(emptyList())
            _state.update {
                it.copy(
                    loading = false,
                    orders = orders,
                    recent = orders.take(AppConstants.RECENT_ORDERS_CARD_LIMIT),
                    txns = txns,
                )
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
    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            preferences.clearSession()
            onDone()
        }
    }
}
