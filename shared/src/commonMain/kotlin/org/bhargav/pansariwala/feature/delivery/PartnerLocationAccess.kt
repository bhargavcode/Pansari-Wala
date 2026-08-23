package org.bhargav.pansariwala.feature.delivery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.domain.model.GeoPoint
import org.bhargav.pansariwala.feature.user.UserLocationOnboardingHero
import org.bhargav.pansariwala.feature.user.UserLocationReasonRow
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.platform.LocationPermissionDeniedDialog
import org.bhargav.pansariwala.platform.LocationPermissionDeniedException
import org.bhargav.pansariwala.platform.LocationUnavailableException
import org.bhargav.pansariwala.platform.PartnerLocationTracker
import org.bhargav.pansariwala.platform.RequestLocationPermission
import org.bhargav.pansariwala.platform.openAppLocationSettings
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.location_unavailable
import pansariwala.shared.generated.resources.partner_location_access_action
import pansariwala.shared.generated.resources.partner_location_access_continue
import pansariwala.shared.generated.resources.partner_location_access_reason_eta
import pansariwala.shared.generated.resources.partner_location_access_reason_jobs
import pansariwala.shared.generated.resources.partner_location_access_reason_tracking
import pansariwala.shared.generated.resources.partner_location_access_subtitle
import pansariwala.shared.generated.resources.partner_location_access_title
import pansariwala.shared.generated.resources.partner_location_permission_denied_message
import pansariwala.shared.generated.resources.user_location_coords
import kotlin.math.roundToInt

data class PartnerLocationAccessUiState(
    val lat: Double? = null,
    val lng: Double? = null,
    val fetchingLocation: Boolean = false,
    val requestLocationPermission: Boolean = false,
    val showLocationDeniedDialog: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    val saving: Boolean = false,
    val error: UiText? = null,
)

class PartnerLocationAccessViewModel(
    private val api: PansariApi,
    private val location: DeviceLocation,
    private val locationTracker: PartnerLocationTracker,
) : ViewModel() {
    private val _state = MutableStateFlow(PartnerLocationAccessUiState())
    val state: StateFlow<PartnerLocationAccessUiState> = _state.asStateFlow()

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
                    _state.update { it.copy(lat = geo.lat, lng = geo.lng, fetchingLocation = false) }
                    locationTracker.ensureTracking()
                }
                .onFailure { e ->
                    val denied = e is LocationPermissionDeniedException
                    _state.update {
                        it.copy(
                            fetchingLocation = false,
                            error = when {
                                denied -> null
                                e is LocationUnavailableException -> UiText.res(Res.string.location_unavailable)
                                else -> UiText.Plain(e.message.orEmpty())
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
            runCatching { api.updatePartnerLocation(lat, lng) }
            locationTracker.ensureTracking()
            _state.update { it.copy(saving = false) }
            onDone()
        }
    }

    private suspend fun fetchAndPushLocation(): GeoPoint {
        val geo = location.currentOrDefault()
        runCatching { api.updatePartnerLocation(geo.lat, geo.lng) }
        return geo
    }
}

@Composable
fun PartnerLocationAccessScreen(
    onDone: () -> Unit,
    viewModel: PartnerLocationAccessViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val deniedMessage = stringResource(Res.string.partner_location_permission_denied_message)

    RequestLocationPermission(
        trigger = state.requestLocationPermission,
        onConsumed = viewModel::consumeLocationPermissionRequest,
        onResult = viewModel::onLocationPermissionResult,
    )
    LocationPermissionDeniedDialog(
        visible = state.showLocationDeniedDialog,
        onRetry = viewModel::retryLocationPermission,
        onOpenSettings = {
            openAppLocationSettings()
            viewModel.dismissLocationDeniedDialog()
        },
        onDismiss = viewModel::dismissLocationDeniedDialog,
        message = deniedMessage,
    )

    Column(
        Modifier.fillMaxSize().padding(24.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(16.dp))
        UserLocationOnboardingHero(fetching = state.fetchingLocation)
        Text(
            stringResource(Res.string.partner_location_access_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            stringResource(Res.string.partner_location_access_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        UserLocationReasonRow(stringResource(Res.string.partner_location_access_reason_jobs), "🛵")
        UserLocationReasonRow(stringResource(Res.string.partner_location_access_reason_tracking), "📍")
        UserLocationReasonRow(stringResource(Res.string.partner_location_access_reason_eta), "⏱️")
        if (state.lat != null && state.lng != null) {
            Text(
                stringResource(
                    Res.string.user_location_coords,
                    ((state.lat!! * 1000).roundToInt() / 1000.0).toString(),
                    ((state.lng!! * 1000).roundToInt() / 1000.0).toString(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium,
            )
        }
        if (!state.locationPermissionGranted || state.lat == null) {
            PartnerPrimaryButton(
                text = stringResource(Res.string.partner_location_access_action),
                onClick = viewModel::requestLocationAccess,
                enabled = !state.fetchingLocation,
            )
        }
        PartnerPrimaryButton(
            text = stringResource(Res.string.partner_location_access_continue),
            onClick = { viewModel.continueToHome(onDone) },
            enabled = !state.saving && state.lat != null && state.lng != null && state.locationPermissionGranted,
        )
        state.error?.let {
            Text(it.asString(), color = MaterialTheme.colorScheme.error)
        }
        if (state.fetchingLocation || state.saving) {
            CircularProgressIndicator()
        }
    }
}
