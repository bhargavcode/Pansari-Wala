package org.bhargav.pansariwala.feature.delivery

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.PartnerRegisterRequest
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.domain.model.DeliveryOffer
import org.bhargav.pansariwala.domain.model.DeliveryOfferStatus
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.PartnerDashboard
import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.platform.FormatPlateOcr
import org.bhargav.pansariwala.platform.ImagePicker
import org.bhargav.pansariwala.platform.PhoneAuthGateway
import org.bhargav.pansariwala.platform.PhoneOtpSession
import org.bhargav.pansariwala.platform.digitsPhone
import org.bhargav.pansariwala.platform.mapPhoneAuthError
import org.bhargav.pansariwala.platform.normalizeVehicleReg
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.util.AppConstants
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_generic
import pansariwala.shared.generated.resources.error_not_a_partner
import pansariwala.shared.generated.resources.error_photo_pick_failed
import pansariwala.shared.generated.resources.otp_sent_partner
import pansariwala.shared.generated.resources.otp_sent_partner_login

data class PartnerLoginUiState(
    val phone: String = "",
    val otp: String = "",
    val step: Int = 0,
    val loading: Boolean = false,
    val error: UiText? = null,
    val hint: UiText? = null,
)

class PartnerLoginViewModel(
    private val api: PansariApi,
    private val preferences: AppPreferences,
    private val phoneAuth: PhoneAuthGateway,
) : ViewModel() {
    private val _state = MutableStateFlow(PartnerLoginUiState())
    val state: StateFlow<PartnerLoginUiState> = _state.asStateFlow()
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
                            hint = UiText.res(Res.string.otp_sent_partner_login),
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = mapPhoneAuthError(error, verifying = false)) }
                }
        }
    }

    fun verify(onDone: () -> Unit) {
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
                if (token.role != AppConstants.Roles.PARTNER) {
                    _state.update { it.copy(loading = false, error = UiText.res(Res.string.error_not_a_partner)) }
                    return@launch
                }
                preferences.saveToken(token)
                onDone()
            }.onFailure { error ->
                _state.update { s -> s.copy(loading = false, error = mapPhoneAuthError(error, verifying = true)) }
            }
        }
    }
}

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val address: String = "",
    val phone: String = "",
    val vehicleReg: String = "",
    val platePhoto: String = "",
    val vehiclePhoto: String = "",
    val otp: String = "",
    val step: Int = 0,
    val loading: Boolean = false,
    val error: UiText? = null,
    val hint: UiText? = null,
)

class PartnerRegisterViewModel(
    private val api: PansariApi,
    private val preferences: AppPreferences,
    private val location: DeviceLocation,
    private val imagePicker: ImagePicker,
    private val phoneAuth: PhoneAuthGateway,
) : ViewModel() {
    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()
    private var session: PhoneOtpSession? = null
    private var registered = false
    private val ocr = FormatPlateOcr()

    fun setName(v: String) { _state.update { it.copy(name = v) } }
    fun setEmail(v: String) { _state.update { it.copy(email = v) } }
    fun setAddress(v: String) { _state.update { it.copy(address = v) } }
    fun setPhone(v: String) { _state.update { it.copy(phone = v) } }
    fun setVehicleReg(v: String) { _state.update { it.copy(vehicleReg = v) } }
    fun setOtp(v: String) { _state.update { it.copy(otp = v) } }
    fun clearPlate() { _state.update { it.copy(platePhoto = "", error = null) } }
    fun clearVehicle() { _state.update { it.copy(vehiclePhoto = "", error = null) } }

    fun attachPlate() {
        viewModelScope.launch {
            val picked = imagePicker.pickImage() ?: return@launch
            if (picked.base64.isBlank()) {
                _state.update { it.copy(error = UiText.res(Res.string.error_photo_pick_failed)) }
                return@launch
            }
            _state.update { it.copy(platePhoto = picked.base64, error = null) }
        }
    }

    fun attachVehicle() {
        viewModelScope.launch {
            val picked = imagePicker.pickImage() ?: return@launch
            if (picked.base64.isBlank()) {
                _state.update { it.copy(error = UiText.res(Res.string.error_photo_pick_failed)) }
                return@launch
            }
            _state.update { it.copy(vehiclePhoto = picked.base64, error = null) }
        }
    }

    fun save() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                ocr.readRegistration(_state.value.platePhoto.encodeToByteArray()).getOrThrow()
                val entered = normalizeVehicleReg(_state.value.vehicleReg)
                require(entered.length >= 8) { "Invalid vehicle registration" }
                if (!registered) {
                    val geo = location.currentOrDefault()
                    api.registerPartner(
                        PartnerRegisterRequest(
                            name = _state.value.name,
                            email = _state.value.email,
                            address = _state.value.address,
                            phone = digitsPhone(_state.value.phone),
                            vehicleReg = entered,
                            platePhotoBase64 = _state.value.platePhoto,
                            vehiclePhotoBase64 = _state.value.vehiclePhoto,
                            lat = geo.lat,
                            lng = geo.lng,
                        ),
                    )
                    registered = true
                }
                phoneAuth.sendOtp(_state.value.phone).getOrThrow()
            }.onSuccess { otpSession ->
                session = otpSession
                _state.update {
                    it.copy(
                        loading = false,
                        step = 1,
                        hint = UiText.res(Res.string.otp_sent_partner),
                    )
                }
            }.onFailure { error ->
                _state.update { s -> s.copy(loading = false, error = mapPhoneAuthError(error, verifying = false)) }
            }
        }
    }

    fun verify(onDone: () -> Unit) {
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
            }.onSuccess {
                preferences.saveToken(it)
                onDone()
            }.onFailure { error ->
                _state.update { s -> s.copy(loading = false, error = mapPhoneAuthError(error, verifying = true)) }
            }
            _state.update { it.copy(loading = false) }
        }
    }
}

data class PartnerDashUiState(
    val dash: PartnerDashboard? = null,
    val loading: Boolean = true,
    val from: Long = 0,
    val to: Long = 0,
    val error: String? = null,
)

class PartnerDashboardViewModel(
    private val api: PansariApi,
) : ViewModel() {
    private val _state = MutableStateFlow(PartnerDashUiState())
    val state: StateFlow<PartnerDashUiState> = _state.asStateFlow()

    init { loadToday() }

    fun loadToday() {
        val start = org.bhargav.pansariwala.util.AppClock.startOfTodayMillis()
        load(start, start + org.bhargav.pansariwala.util.MILLIS_PER_DAY)
    }

    fun load(from: Long, to: Long) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, from = from, to = to) }
            runCatching { api.partnerDashboard(from, to) }
                .onSuccess { dash -> _state.update { it.copy(loading = false, dash = dash) } }
                .onFailure { _state.update { s -> s.copy(loading = false, error = it.message) } }
        }
    }
}

class OfferViewModel(
    private val api: PansariApi,
) : ViewModel() {
    private val _offer = MutableStateFlow<DeliveryOffer?>(null)
    val offer: StateFlow<DeliveryOffer?> = _offer.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private var pollJob: kotlinx.coroutines.Job? = null

    fun load(offerId: String?) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
                runCatching {
                    val incoming = api.incomingOffer()
                    _offer.value = incoming
                    when {
                        incoming != null -> _message.value = null
                        offerId != null -> _message.value = "taken"
                    }
                }
                delay(AppConstants.LIVE_ALERT_POLL_MS)
            }
        }
    }

    fun accept(onDone: () -> Unit) {
        val id = _offer.value?.id ?: return
        viewModelScope.launch {
            runCatching { api.acceptOffer(id) }
                .onSuccess {
                    if (it.status == DeliveryOfferStatus.TAKEN_BY_OTHER) _message.value = "taken"
                    else onDone()
                }
                .onFailure { _message.value = it.message }
        }
    }

    fun reject() {
        val id = _offer.value?.id ?: return
        viewModelScope.launch { runCatching { api.rejectOffer(id) } }
    }
}

class JobsViewModel(
    private val api: PansariApi,
) : ViewModel() {
    private val _jobs = MutableStateFlow<List<Order>>(emptyList())
    val jobs: StateFlow<List<Order>> = _jobs.asStateFlow()
    private val _error = MutableStateFlow<UiText?>(null)
    val error: StateFlow<UiText?> = _error.asStateFlow()

    fun loadAccepted() { viewModelScope.launch { _jobs.value = runCatching { api.acceptedJobs() }.getOrDefault(emptyList()) } }
    fun loadDelivered(from: Long, to: Long) {
        viewModelScope.launch { _jobs.value = runCatching { api.deliveredJobs(from, to) }.getOrDefault(emptyList()) }
    }
    fun cancel(orderId: String) { viewModelScope.launch { runCatching { api.cancelPickup(orderId) }; loadAccepted() } }
    fun pickup(orderId: String, one: String, two: String, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { api.submitPickup(orderId, one, two) }
                .onSuccess { onDone() }
                .onFailure { _error.value = UiText.res(Res.string.error_generic) }
        }
    }
    fun deliver(orderId: String, otp: String, onDone: () -> Unit) {
        viewModelScope.launch {
            runCatching { api.deliverOrder(orderId, otp) }
                .onSuccess { onDone(); loadAccepted() }
                .onFailure { _error.value = UiText.res(Res.string.error_generic) }
        }
    }
}
