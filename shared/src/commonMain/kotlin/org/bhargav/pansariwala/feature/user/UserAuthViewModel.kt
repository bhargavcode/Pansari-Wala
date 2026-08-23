package org.bhargav.pansariwala.feature.user

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
import org.bhargav.pansariwala.api.TokenResponse
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.platform.PhoneAuthGateway
import org.bhargav.pansariwala.platform.PhoneOtpSession
import org.bhargav.pansariwala.platform.digitsPhone
import org.bhargav.pansariwala.platform.fetchPlaceDetails
import org.bhargav.pansariwala.platform.geocodeAddress
import org.bhargav.pansariwala.platform.mapPhoneAuthError
import org.bhargav.pansariwala.platform.searchPlaces
import org.bhargav.pansariwala.util.AppConstants
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_address_coordinates
import pansariwala.shared.generated.resources.error_address_required
import pansariwala.shared.generated.resources.error_place_details_failed
import pansariwala.shared.generated.resources.error_profile_required
import pansariwala.shared.generated.resources.location_unavailable
import pansariwala.shared.generated.resources.dev_otp_hint
import pansariwala.shared.generated.resources.otp_sent_customer

data class PhoneAuthUiState(
    val phone: String = "",
    val otp: String = "",
    val step: Int = 0,
    val loading: Boolean = false,
    val error: UiText? = null,
    val hint: UiText? = null,
    val profileComplete: Boolean = true,
)

class PhoneAuthViewModel(
    private val api: PansariApi,
    private val preferences: AppPreferences,
    private val phoneAuth: PhoneAuthGateway,
) : ViewModel() {
    private val _state = MutableStateFlow(PhoneAuthUiState())
    val state: StateFlow<PhoneAuthUiState> = _state.asStateFlow()

    fun dismissError() { _state.update { it.copy(error = null) } }
    private var session: PhoneOtpSession? = null

    fun setPhone(value: String) { _state.update { it.copy(phone = value, error = null) } }
    fun setOtp(value: String) { _state.update { it.copy(otp = value, error = null) } }

    fun sendOtp() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            phoneAuth.sendOtp(_state.value.phone)
                .onSuccess { next ->
                    session = next
                    _state.update {
                        it.copy(
                            loading = false,
                            step = 1,
                            hint = if (next.devOtp != null) {
                                UiText.res(Res.string.dev_otp_hint)
                            } else {
                                UiText.res(Res.string.otp_sent_customer)
                            },
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = mapPhoneAuthError(error, verifying = false)) }
                }
        }
    }

    fun verify(onDone: (TokenResponse) -> Unit) {
        val otpSession = session ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val auth = phoneAuth.verifyOtp(_state.value.phone, _state.value.otp, otpSession).getOrThrow()
                val phone = digitsPhone(_state.value.phone)
                if (otpSession.usesFirebase) {
                    api.loginWithFirebase(requireNotNull(auth.firebaseIdToken))
                } else {
                    api.verifyOtp(phone, _state.value.otp, otpSession.sessionId)
                }
            }.onSuccess { token ->
                preferences.saveToken(token)
                _state.update { s -> s.copy(loading = false, profileComplete = token.profileComplete) }
                onDone(token)
            }.onFailure { error ->
                _state.update { s -> s.copy(loading = false, error = mapPhoneAuthError(error, verifying = true)) }
            }
        }
    }
}

data class AddressUiState(
    val name: String = "",
    val placeQuery: String = "",
    val predictions: List<org.bhargav.pansariwala.platform.PlacePrediction> = emptyList(),
    val address: String = "",
    val locality: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val loading: Boolean = false,
    val error: UiText? = null,
)

class AddressViewModel(
    private val api: PansariApi,
    private val location: DeviceLocation,
) : ViewModel() {
    private val _state = MutableStateFlow(AddressUiState())
    val state: StateFlow<AddressUiState> = _state.asStateFlow()
    private var searchJob: Job? = null

    fun dismissError() { _state.update { it.copy(error = null) } }

    fun setName(value: String) { _state.update { it.copy(name = value, error = null) } }
    fun setAddress(value: String) {
        _state.update { it.copy(address = value, error = null, lat = null, lng = null) }
    }
    fun setLocality(value: String) {
        _state.update { it.copy(locality = value, error = null, lat = null, lng = null) }
    }

    fun setPlaceQuery(value: String) {
        _state.update { it.copy(placeQuery = value, error = null) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(AppConstants.PLACE_SEARCH_DEBOUNCE_MS)
            val results = searchPlaces(value)
            _state.update { it.copy(predictions = results) }
        }
    }

    fun selectPlace(placeId: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val details = fetchPlaceDetails(placeId)
            if (details == null) {
                _state.update {
                    it.copy(loading = false, error = UiText.res(Res.string.error_place_details_failed))
                }
                return@launch
            }
            _state.update {
                it.copy(
                    loading = false,
                    placeQuery = details.formattedAddress,
                    predictions = emptyList(),
                    address = details.formattedAddress,
                    locality = details.locality.ifBlank { it.locality },
                    lat = details.lat,
                    lng = details.lng,
                )
            }
        }
    }

    fun useCurrentLocation() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { location.currentOrDefault() }
                .onSuccess { geo ->
                    _state.update { it.copy(lat = geo.lat, lng = geo.lng, loading = false) }
                }
                .onFailure {
                    _state.update {
                        it.copy(loading = false, error = UiText.res(Res.string.location_unavailable))
                    }
                }
        }
    }

    fun save(requireName: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val s = _state.value
            if (requireName && s.name.isBlank()) {
                _state.update { it.copy(loading = false, error = UiText.res(Res.string.error_profile_required)) }
                return@launch
            }
            if (s.address.isBlank() || s.locality.isBlank()) {
                _state.update { it.copy(loading = false, error = UiText.res(Res.string.error_address_required)) }
                return@launch
            }
            val coords = resolveCoordinates(s)
            if (coords == null) {
                _state.update {
                    it.copy(loading = false, error = UiText.res(Res.string.error_address_coordinates))
                }
                return@launch
            }
            val (lat, lng) = coords
            runCatching {
                if (requireName) {
                    api.updateProfile(s.name, s.address, s.locality, lat, lng)
                } else {
                    api.saveAddress(s.address, s.locality, lat, lng)
                }
            }.onSuccess { onDone() }
                .onFailure { err ->
                    _state.update { st -> st.copy(error = UiText.Plain(err.message.orEmpty())) }
                }
            _state.update { it.copy(loading = false, lat = lat, lng = lng) }
        }
    }

    private suspend fun resolveCoordinates(state: AddressUiState): Pair<Double, Double>? {
        state.lat?.let { lat -> state.lng?.let { lng -> return lat to lng } }
        val query = listOf(state.address, state.locality, "India").filter { it.isNotBlank() }.joinToString(", ")
        geocodeAddress(query)?.let { return it.lat to it.lng }
        return runCatching { location.currentOrDefault() }.getOrNull()?.let { it.lat to it.lng }
    }
}
