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
import org.bhargav.pansariwala.platform.PhoneAuthGateway
import org.bhargav.pansariwala.platform.PhoneOtpSession
import org.bhargav.pansariwala.platform.digitsPhone
import org.bhargav.pansariwala.platform.fetchPlaceDetails
import org.bhargav.pansariwala.platform.mapPhoneAuthError
import org.bhargav.pansariwala.platform.searchPlaces
import org.bhargav.pansariwala.util.AppConstants
import pansariwala.shared.generated.resources.Res
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
                            hint = UiText.res(Res.string.otp_sent_customer),
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
    val error: String? = null,
)

class AddressViewModel(
    private val api: PansariApi,
) : ViewModel() {
    private val _state = MutableStateFlow(AddressUiState())
    val state: StateFlow<AddressUiState> = _state.asStateFlow()
    private var searchJob: Job? = null

    fun setName(value: String) { _state.update { it.copy(name = value) } }
    fun setAddress(value: String) { _state.update { it.copy(address = value) } }
    fun setLocality(value: String) { _state.update { it.copy(locality = value) } }

    fun setPlaceQuery(value: String) {
        _state.update { it.copy(placeQuery = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(AppConstants.PLACE_SEARCH_DEBOUNCE_MS)
            val results = searchPlaces(value)
            _state.update { it.copy(predictions = results) }
        }
    }

    fun selectPlace(placeId: String) {
        viewModelScope.launch {
            val details = fetchPlaceDetails(placeId) ?: return@launch
            _state.update {
                it.copy(
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

    fun save(requireName: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val s = _state.value
            if (requireName && s.name.isBlank()) {
                _state.update { it.copy(loading = false, error = AppConstants.Checkout.ERROR_PROFILE) }
                return@launch
            }
            if (s.address.isBlank() || s.locality.isBlank() || s.lat == null || s.lng == null) {
                _state.update { it.copy(loading = false, error = AppConstants.Checkout.ERROR_ADDRESS_REQUIRED) }
                return@launch
            }
            runCatching {
                if (requireName) {
                    api.updateProfile(s.name, s.address, s.locality, s.lat, s.lng)
                } else {
                    api.saveAddress(s.address, s.locality, s.lat, s.lng)
                }
            }.onSuccess { onDone() }
                .onFailure { _state.update { st -> st.copy(error = it.message) } }
            _state.update { it.copy(loading = false) }
        }
    }
}
