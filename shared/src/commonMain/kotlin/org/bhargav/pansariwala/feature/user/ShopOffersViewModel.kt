package org.bhargav.pansariwala.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.domain.model.ShopOffer
import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.ui.AsyncUiState
import org.bhargav.pansariwala.ui.beginLoad

data class ShopOffersData(
    val shopName: String? = null,
    val offers: List<ShopOffer> = emptyList(),
)

class ShopOffersViewModel(
    private val api: PansariApi,
    private val cart: CartStore,
    private val location: DeviceLocation,
) : ViewModel() {
    private val _state = MutableStateFlow<AsyncUiState<ShopOffersData>>(AsyncUiState.Idle)
    val state: StateFlow<AsyncUiState<ShopOffersData>> = _state.asStateFlow()

    private var shopId: String? = null
    private var cartShopJob: Job? = null

    fun load(shopId: String, force: Boolean = false) {
        if (!force && this.shopId == shopId && _state.value is AsyncUiState.Success) return
        this.shopId = shopId
        viewModelScope.launch {
            _state.value = _state.value.beginLoad()
            runCatching {
                coroutineScope {
                    val offers = async { api.shopOffers(shopId) }
                    val shop = async {
                        runCatching {
                            val geo = location.currentOrDefault()
                            api.shopInfo(shopId, geo.lat, geo.lng)
                        }.getOrNull()
                    }
                    ShopOffersData(shopName = shop.await()?.name, offers = offers.await())
                }
            }.onSuccess { data ->
                _state.value = AsyncUiState.Success(data)
                data.shopName?.let { keepCartShopName(shopId, it) }
            }.onFailure { err ->
                err.rethrowIfStructuredCancellation()
                val message = err.toApiUiText()
                _state.value = when (val current = _state.value) {
                    is AsyncUiState.Success -> current.copy(isRefreshing = false, bannerError = message)
                    else -> AsyncUiState.Error(message)
                }
            }
        }
    }

    /** Adding an offer product switches the cart to this shop; label it so "Continue order" shows the name. */
    private fun keepCartShopName(shopId: String, name: String) {
        cartShopJob?.cancel()
        cartShopJob = viewModelScope.launch {
            cart.shopId.collect { if (it == shopId) cart.bindShop(shopId, name) }
        }
    }

    fun retry() {
        shopId?.let { load(it, force = true) }
    }

    fun dismissError() {
        _state.value = when (val current = _state.value) {
            is AsyncUiState.Error -> AsyncUiState.Idle
            is AsyncUiState.Success -> current.copy(bannerError = null)
            else -> current
        }
    }
}
