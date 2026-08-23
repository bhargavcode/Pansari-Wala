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
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.platform.LocationPermissionDeniedException
import org.bhargav.pansariwala.platform.LocationUnavailableException
import org.bhargav.pansariwala.util.AppConstants
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.location_unavailable

data class MarketUiState(
    val query: String = "",
    val radiusKm: Double = AppConstants.DEFAULT_SEARCH_RADIUS_KM,
    val shops: List<MarketplaceShop> = emptyList(),
    val loading: Boolean = true,
    val error: UiText? = null,
    val fetchingLocation: Boolean = false,
    val requestLocationPermission: Boolean = false,
    val showLocationDeniedDialog: Boolean = false,
    val locationPermissionGranted: Boolean = false,
)

class MarketViewModel(
    private val api: PansariApi,
    private val preferences: AppPreferences,
    private val location: DeviceLocation,
) : ViewModel() {
    private val _state = MutableStateFlow(MarketUiState())
    val state: StateFlow<MarketUiState> = _state.asStateFlow()

    fun dismissError() { _state.update { it.copy(error = null) } }

    init {
        viewModelScope.launch {
            val radius = preferences.getSearchRadiusKm()
            _state.update { it.copy(radiusKm = radius) }
            preferences.observeSearchRadiusKm().collect { km ->
                _state.update { it.copy(radiusKm = km) }
                if (_state.value.locationPermissionGranted) {
                    refreshShops()
                }
            }
        }
    }

    fun onHomeVisible() {
        if (_state.value.locationPermissionGranted) {
            refreshLocationAndShops()
        } else {
            _state.update { it.copy(requestLocationPermission = true) }
        }
    }

    fun consumeLocationPermissionRequest() {
        _state.update { it.copy(requestLocationPermission = false) }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        _state.update {
            it.copy(
                locationPermissionGranted = granted,
                showLocationDeniedDialog = !granted,
            )
        }
        if (granted) {
            refreshLocationAndShops()
        } else {
            _state.update { it.copy(loading = false) }
        }
    }

    fun retryLocationPermission() {
        _state.update { it.copy(showLocationDeniedDialog = false, requestLocationPermission = true) }
    }

    fun dismissLocationDeniedDialog() {
        _state.update { it.copy(showLocationDeniedDialog = false) }
    }

    fun setQuery(value: String) { _state.update { it.copy(query = value) } }

    fun setRadius(km: Double) {
        viewModelScope.launch {
            preferences.setSearchRadiusKm(km)
            _state.update { it.copy(radiusKm = km) }
            if (_state.value.locationPermissionGranted) {
                refreshShops()
            }
        }
    }

    fun search() {
        if (_state.value.locationPermissionGranted) {
            refreshShops()
        } else {
            _state.update { it.copy(requestLocationPermission = true) }
        }
    }

    private fun refreshLocationAndShops() {
        viewModelScope.launch {
            _state.update { it.copy(fetchingLocation = true, loading = true, error = null) }
            val geo = runCatching { location.currentOrDefault() }
                .getOrElse { err ->
                    handleLocationFailure(err)
                    return@launch
                }
            runCatching { api.updateCustomerLocation(geo.lat, geo.lng) }
            _state.update { it.copy(fetchingLocation = false) }
            loadShops(geo.lat, geo.lng)
        }
    }

    private fun refreshShops() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val geo = runCatching { location.currentOrDefault() }
                .getOrElse { err ->
                    handleLocationFailure(err)
                    return@launch
                }
            runCatching { api.updateCustomerLocation(geo.lat, geo.lng) }
            loadShops(geo.lat, geo.lng)
        }
    }

    private suspend fun loadShops(lat: Double, lng: Double) {
        runCatching {
            api.nearbyShops(lat, lng, _state.value.radiusKm, _state.value.query)
        }.onSuccess { shops ->
            _state.update { it.copy(loading = false, shops = shops) }
        }.onFailure { err ->
            _state.update { it.copy(loading = false, error = UiText.Plain(err.message.orEmpty())) }
        }
    }

    private fun handleLocationFailure(err: Throwable) {
        val denied = err is LocationPermissionDeniedException
        val unavailable = err is LocationUnavailableException
        _state.update {
            it.copy(
                fetchingLocation = false,
                loading = false,
                error = when {
                    denied -> null
                    unavailable -> UiText.res(Res.string.location_unavailable)
                    else -> UiText.Plain(err.message.orEmpty())
                },
                locationPermissionGranted = !denied,
                showLocationDeniedDialog = denied,
                requestLocationPermission = denied,
            )
        }
    }
}
