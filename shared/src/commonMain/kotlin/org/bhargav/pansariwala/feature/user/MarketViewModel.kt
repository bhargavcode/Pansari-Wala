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
import org.bhargav.pansariwala.domain.model.MarketplaceShop
import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.util.AppConstants

data class MarketUiState(
    val query: String = "",
    val radiusKm: Double = AppConstants.DEFAULT_SEARCH_RADIUS_KM,
    val shops: List<MarketplaceShop> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
)

class MarketViewModel(
    private val api: PansariApi,
    private val preferences: AppPreferences,
    private val location: DeviceLocation,
) : ViewModel() {
    private val _state = MutableStateFlow(MarketUiState())
    val state: StateFlow<MarketUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val radius = preferences.getSearchRadiusKm()
            _state.update { it.copy(radiusKm = radius) }
            refresh()
            preferences.observeSearchRadiusKm().collect { km ->
                _state.update { it.copy(radiusKm = km) }
                refresh()
            }
        }
    }

    fun setQuery(value: String) { _state.update { it.copy(query = value) } }

    fun setRadius(km: Double) {
        viewModelScope.launch {
            preferences.setSearchRadiusKm(km)
            _state.update { it.copy(radiusKm = km) }
            refresh()
        }
    }

    fun search() { refresh() }

    private fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val geo = location.currentOrDefault()
                api.nearbyShops(geo.lat, geo.lng, _state.value.radiusKm, _state.value.query)
            }.onSuccess { shops ->
                _state.update { it.copy(loading = false, shops = shops) }
            }.onFailure { err ->
                _state.update { it.copy(loading = false, error = err.message) }
            }
        }
    }
}
