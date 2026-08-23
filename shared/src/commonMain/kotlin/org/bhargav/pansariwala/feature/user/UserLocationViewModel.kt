package org.bhargav.pansariwala.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.domain.model.GeoPoint
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.platform.LocationPermissionDeniedException
import org.bhargav.pansariwala.platform.LocationUnavailableException
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.location_unavailable

data class UserLocationAccessUiState(
    val lat: Double? = null,
    val lng: Double? = null,
    val fetchingLocation: Boolean = false,
    val requestLocationPermission: Boolean = false,
    val showLocationDeniedDialog: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    val saving: Boolean = false,
    val error: UiText? = null,
)

class UserLocationAccessViewModel(
    private val api: PansariApi,
    private val location: DeviceLocation,
) : ViewModel() {
    private val _state = MutableStateFlow(UserLocationAccessUiState())
    val state: StateFlow<UserLocationAccessUiState> = _state.asStateFlow()

    fun dismissError() { _state.update { it.copy(error = null) } }

    init {
        requestLocationAccess()
    }

    fun requestLocationAccess() {
        if (_state.value.locationPermissionGranted) {
            refreshCurrentLocation()
            return
        }
        _state.update { it.copy(requestLocationPermission = true) }
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
            refreshCurrentLocation()
        }
    }

    fun retryLocationPermission() {
        _state.update { it.copy(showLocationDeniedDialog = false, requestLocationPermission = true) }
    }

    fun dismissLocationDeniedDialog() {
        _state.update { it.copy(showLocationDeniedDialog = false) }
    }

    fun refreshCurrentLocation() {
        if (!_state.value.locationPermissionGranted) {
            _state.update { it.copy(requestLocationPermission = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(fetchingLocation = true, error = null) }
            runCatching { fetchAndPushLocation() }
                .onSuccess { geo ->
                    _state.update {
                        it.copy(lat = geo.lat, lng = geo.lng, fetchingLocation = false)
                    }
                }
                .onFailure { e ->
                    val denied = e is LocationPermissionDeniedException
                    val unavailable = e is LocationUnavailableException
                    _state.update {
                        it.copy(
                            fetchingLocation = false,
                            error = when {
                                denied -> null
                                unavailable -> UiText.res(Res.string.location_unavailable)
                                else -> e.toApiUiText()
                            },
                            locationPermissionGranted = !denied,
                            showLocationDeniedDialog = denied,
                            requestLocationPermission = denied,
                        )
                    }
                }
        }
    }

    fun continueToHome(onDone: () -> Unit) {
        val lat = _state.value.lat ?: return
        val lng = _state.value.lng ?: return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, error = null) }
            runCatching { api.updateCustomerLocation(lat, lng) }
            _state.update { it.copy(saving = false) }
            onDone()
        }
    }

    private suspend fun fetchAndPushLocation(): GeoPoint {
        val geo = location.currentOrDefault()
        runCatching { api.updateCustomerLocation(geo.lat, geo.lng) }
        return geo
    }
}
