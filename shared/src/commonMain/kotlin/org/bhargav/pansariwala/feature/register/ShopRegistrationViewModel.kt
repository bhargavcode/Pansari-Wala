package org.bhargav.pansariwala.feature.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.ApiException
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.ShopHoursDayDto
import org.bhargav.pansariwala.api.ShopRegistrationDraftDto
import org.bhargav.pansariwala.api.ShopRegistrationSubmit
import org.bhargav.pansariwala.api.ShopTypeOptionDto
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.domain.pricing.DeliveryPricing
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.notification.PushSignals
import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.platform.LocationPermissionDeniedException
import org.bhargav.pansariwala.platform.LocationServicesDisabledException
import org.bhargav.pansariwala.platform.PhoneAuthGateway
import org.bhargav.pansariwala.platform.PhoneOtpSession
import org.bhargav.pansariwala.platform.PlacePrediction
import org.bhargav.pansariwala.platform.appBuildInfo
import org.bhargav.pansariwala.platform.digitsPhone
import org.bhargav.pansariwala.platform.fetchPlaceDetails
import org.bhargav.pansariwala.platform.mapPhoneAuthError
import org.bhargav.pansariwala.platform.otpCountdown
import org.bhargav.pansariwala.platform.otpRetryAfterSec
import org.bhargav.pansariwala.platform.pickLocationOnMap
import org.bhargav.pansariwala.platform.reverseGeocode
import org.bhargav.pansariwala.platform.sanitizeOtpInput
import org.bhargav.pansariwala.platform.searchPlaces
import org.bhargav.pansariwala.util.AppConstants
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.dev_otp_hint
import pansariwala.shared.generated.resources.error_phone_invalid
import pansariwala.shared.generated.resources.error_place_details_failed
import pansariwala.shared.generated.resources.location_unavailable
import pansariwala.shared.generated.resources.map_picker_unavailable
import pansariwala.shared.generated.resources.otp_sent_to
import pansariwala.shared.generated.resources.reg_error_address
import pansariwala.shared.generated.resources.reg_error_already_registered
import pansariwala.shared.generated.resources.reg_error_email
import pansariwala.shared.generated.resources.reg_error_gps_required
import pansariwala.shared.generated.resources.reg_error_hours
import pansariwala.shared.generated.resources.reg_error_location_far
import pansariwala.shared.generated.resources.reg_error_owner_name
import pansariwala.shared.generated.resources.reg_error_password_mismatch
import pansariwala.shared.generated.resources.reg_error_pin_required
import pansariwala.shared.generated.resources.reg_error_shop_name
import pansariwala.shared.generated.resources.reg_error_shop_types
import pansariwala.shared.generated.resources.reg_error_username
import pansariwala.shared.generated.resources.reg_error_username_taken
import pansariwala.shared.generated.resources.reg_error_weak_password
import pansariwala.shared.generated.resources.reg_error_zip

/** Order matters: it is the on-screen step order and the index saved as [ShopRegistrationDraftDto.step]. */
enum class RegistrationStep { PHONE, OWNER, SHOP, LOCATION, BUSINESS, CREDENTIALS, REVIEW }

enum class UsernameStatus { CHECKING, AVAILABLE, TAKEN }

data class ShopRegistrationUiState(
    val step: RegistrationStep = RegistrationStep.PHONE,
    /** Furthest step reached; earlier steps (and this one) can be reopened from the stepper. */
    val reachedStep: RegistrationStep = RegistrationStep.PHONE,
    /** Editing a step opened from the review; "Next" returns straight to the review. */
    val editingFromReview: Boolean = false,
    val phone: String = "",
    val otp: String = "",
    val otpSent: Boolean = false,
    val resendInSec: Int = 0,
    val hint: UiText? = null,
    val draft: ShopRegistrationDraftDto = ShopRegistrationDraftDto(),
    val shopTypeOptions: List<ShopTypeOptionDto> = emptyList(),
    val placeQuery: String = "",
    val predictions: List<PlacePrediction> = emptyList(),
    val searchingPlaces: Boolean = false,
    val locating: Boolean = false,
    val pickingOnMap: Boolean = false,
    val showLocationRationale: Boolean = false,
    val requestLocationPermission: Boolean = false,
    val showLocationDeniedDialog: Boolean = false,
    val usernameStatus: UsernameStatus? = null,
    val password: String = "",
    val confirmPassword: String = "",
    val loading: Boolean = false,
    val error: UiText? = null,
) {
    /** Distance between the GPS fix and the shop pin, when both are known. */
    val pinDistanceKm: Double?
        get() {
            val lat = draft.lat ?: return null
            val lng = draft.lng ?: return null
            val deviceLat = draft.deviceLat ?: return null
            val deviceLng = draft.deviceLng ?: return null
            return DeliveryPricing.haversineKm(lat, lng, deviceLat, deviceLng)
        }

    val locationVerified: Boolean
        get() = pinDistanceKm?.let { it <= AppConstants.ShopRegistration.LOCATION_TOLERANCE_KM } ?: false
}

class ShopRegistrationViewModel(
    private val api: PansariApi,
    private val phoneAuth: PhoneAuthGateway,
    private val location: DeviceLocation,
) : ViewModel() {
    private val _state = MutableStateFlow(ShopRegistrationUiState())
    val state: StateFlow<ShopRegistrationUiState> = _state.asStateFlow()

    private var otpSession: PhoneOtpSession? = null
    private var token: String? = null
    private var countdownJob: Job? = null
    private var searchJob: Job? = null
    private var usernameJob: Job? = null

    fun dismissError() { _state.update { it.copy(error = null) } }

    // region Phone + OTP

    fun setPhone(value: String) {
        _state.update {
            it.copy(phone = value.filter(Char::isDigit).take(AppConstants.PHONE_LOCAL_DIGITS), otpSent = false, otp = "", error = null)
        }
    }

    fun setOtp(value: String) { _state.update { it.copy(otp = sanitizeOtpInput(value), error = null) } }

    fun sendOtp() {
        val s = _state.value
        if (s.loading || s.resendInSec > 0) return
        if (digitsPhone(s.phone).length != AppConstants.PHONE_LOCAL_DIGITS) {
            _state.update { it.copy(error = UiText.res(Res.string.error_phone_invalid)) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            phoneAuth.sendOtp(s.phone)
                .onSuccess { session ->
                    otpSession = session
                    _state.update {
                        it.copy(
                            loading = false,
                            otpSent = true,
                            otp = "",
                            hint = if (session.devOtp != null) {
                                UiText.res(Res.string.dev_otp_hint)
                            } else {
                                UiText.res(Res.string.otp_sent_to, session.maskedPhone.ifBlank { s.phone })
                            },
                        )
                    }
                    startCountdown(session.resendAfterSec)
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = mapPhoneAuthError(error, verifying = false)) }
                    error.otpRetryAfterSec()?.let(::startCountdown)
                }
        }
    }

    private fun startCountdown(seconds: Int) {
        countdownJob = otpCountdown(countdownJob, seconds) { left -> _state.update { it.copy(resendInSec = left) } }
    }

    /** Verifies the OTP and resumes any saved draft for this number. */
    fun verifyOtp() {
        val session = otpSession ?: return
        val s = _state.value
        if (s.loading || s.otp.length != AppConstants.OTP_LENGTH) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { api.verifyShopRegistration(digitsPhone(s.phone), s.otp, session.sessionId) }
                .onSuccess { result ->
                    token = result.token
                    countdownJob?.cancel()
                    val resumed = RegistrationStep.entries
                        .getOrElse(result.draft.step) { RegistrationStep.OWNER }
                        .coerceAtLeast(RegistrationStep.OWNER)
                    _state.update {
                        it.copy(
                            loading = false,
                            draft = result.draft.withDefaults(),
                            step = resumed,
                            reachedStep = resumed,
                            placeQuery = result.draft.address,
                            hint = null,
                        )
                    }
                    loadShopTypes()
                }
                .onFailure { error ->
                    error.rethrowIfStructuredCancellation()
                    _state.update { it.copy(loading = false, error = mapRegistrationError(error, verifying = true)) }
                }
        }
    }

    // endregion

    private fun loadShopTypes() {
        val auth = token ?: return
        viewModelScope.launch {
            runCatching { api.registrationShopTypes(auth) }
                .onSuccess { types -> _state.update { it.copy(shopTypeOptions = types.filter { t -> t.active }) } }
                .onFailure { it.rethrowIfStructuredCancellation() }
        }
    }

    fun updateDraft(transform: (ShopRegistrationDraftDto) -> ShopRegistrationDraftDto) {
        _state.update { it.copy(draft = transform(it.draft), error = null) }
    }

    fun toggleShopType(id: String) = updateDraft { d ->
        d.copy(shopTypes = if (id in d.shopTypes) d.shopTypes - id else d.shopTypes + id)
    }

    fun updateHours(day: String, transform: (ShopHoursDayDto) -> ShopHoursDayDto) = updateDraft { d ->
        d.copy(operatingHours = d.operatingHours.map { if (it.day == day) transform(it) else it })
    }

    // region Location

    fun setPlaceQuery(value: String) {
        searchJob?.cancel()
        _state.update { it.copy(placeQuery = value, searchingPlaces = value.isNotBlank(), predictions = emptyList()) }
        if (value.isBlank()) return
        searchJob = viewModelScope.launch {
            delay(AppConstants.PLACE_SEARCH_DEBOUNCE_MS)
            val results = searchPlaces(value)
            _state.update { it.copy(predictions = results, searchingPlaces = false) }
        }
    }

    fun selectPlace(placeId: String) {
        searchJob?.cancel()
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, searchingPlaces = false) }
            val details = fetchPlaceDetails(placeId)
            if (details == null) {
                _state.update { it.copy(loading = false, error = UiText.res(Res.string.error_place_details_failed)) }
                return@launch
            }
            _state.update {
                it.copy(
                    loading = false,
                    placeQuery = details.formattedAddress,
                    predictions = emptyList(),
                    draft = it.draft.copy(
                        address = details.formattedAddress,
                        city = details.locality.ifBlank { it.draft.city },
                        lat = details.lat,
                        lng = details.lng,
                    ),
                )
            }
        }
    }

    /** Takes a GPS fix; it verifies the pin and, when no pin is set yet, becomes the pin. */
    fun useCurrentLocation() {
        if (_state.value.locating) return
        searchJob?.cancel()
        viewModelScope.launch {
            _state.update { it.copy(locating = true, error = null, searchingPlaces = false) }
            val geo = try {
                location.currentOrDefault()
            } catch (_: LocationPermissionDeniedException) {
                _state.update { it.copy(locating = false, showLocationRationale = true) }
                return@launch
            } catch (_: LocationServicesDisabledException) {
                _state.update { it.copy(locating = false, requestLocationPermission = true) }
                return@launch
            } catch (e: Throwable) {
                e.rethrowIfStructuredCancellation()
                _state.update { it.copy(locating = false, error = UiText.res(Res.string.location_unavailable)) }
                return@launch
            }
            val pinMissing = _state.value.draft.lat == null || _state.value.draft.lng == null
            val details = if (pinMissing) reverseGeocode(geo.lat, geo.lng) else null
            _state.update {
                val d = it.draft.copy(deviceLat = geo.lat, deviceLng = geo.lng)
                it.copy(
                    locating = false,
                    placeQuery = details?.formattedAddress ?: it.placeQuery,
                    predictions = emptyList(),
                    draft = if (pinMissing) {
                        d.copy(
                            lat = geo.lat,
                            lng = geo.lng,
                            address = details?.formattedAddress?.ifBlank { null } ?: d.address,
                            city = details?.locality?.ifBlank { null } ?: d.city,
                        )
                    } else {
                        d
                    },
                )
            }
        }
    }

    /** Pins the shop on a map (search or pan); fills address parts the geocoder returns. GPS verification still applies. */
    fun pickOnMap() {
        if (_state.value.pickingOnMap) return
        searchJob?.cancel()
        viewModelScope.launch {
            _state.update { it.copy(pickingOnMap = true, error = null, searchingPlaces = false, predictions = emptyList()) }
            val d = _state.value.draft
            val startLat = d.lat ?: d.deviceLat
            val startLng = d.lng ?: d.deviceLng
            val picked = try {
                if (startLat != null && startLng != null) {
                    pickLocationOnMap(startLat, startLng, AppConstants.MAP_PICKER_PIN_ZOOM)
                } else {
                    pickLocationOnMap(AppConstants.DEFAULT_MAP_LAT, AppConstants.DEFAULT_MAP_LNG, AppConstants.MAP_PICKER_CITY_ZOOM)
                }
            } catch (e: Throwable) {
                e.rethrowIfStructuredCancellation()
                _state.update { it.copy(pickingOnMap = false, error = UiText.res(Res.string.map_picker_unavailable)) }
                return@launch
            }
            if (picked == null) {
                _state.update { it.copy(pickingOnMap = false) }
                return@launch
            }
            val fallback = if (picked.address.isBlank()) reverseGeocode(picked.lat, picked.lng) else null
            val address = picked.address.ifBlank { fallback?.formattedAddress.orEmpty() }
            val city = picked.city.ifBlank { fallback?.locality.orEmpty() }
            val zip = picked.zip.filter(Char::isDigit).takeIf { it.length == AppConstants.ShopRegistration.ZIP_LENGTH }
            _state.update {
                it.copy(
                    pickingOnMap = false,
                    placeQuery = address.ifBlank { it.placeQuery },
                    draft = it.draft.copy(
                        lat = picked.lat,
                        lng = picked.lng,
                        address = address.ifBlank { it.draft.address },
                        city = city.ifBlank { it.draft.city },
                        state = picked.state.ifBlank { it.draft.state },
                        zip = zip ?: it.draft.zip,
                    ),
                )
            }
        }
    }

    fun acceptLocationRationale() { _state.update { it.copy(showLocationRationale = false, requestLocationPermission = true) } }
    fun dismissLocationRationale() { _state.update { it.copy(showLocationRationale = false) } }
    fun onLocationPermissionRequestConsumed() { _state.update { it.copy(requestLocationPermission = false) } }
    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) useCurrentLocation() else _state.update { it.copy(showLocationDeniedDialog = true) }
    }
    fun retryLocationPermission() { _state.update { it.copy(showLocationDeniedDialog = false, requestLocationPermission = true) } }
    fun dismissLocationDeniedDialog() { _state.update { it.copy(showLocationDeniedDialog = false) } }

    // endregion

    // region Credentials

    fun setUsername(value: String) {
        val username = value.trim().lowercase()
        updateDraft { it.copy(loginUsername = username) }
        usernameJob?.cancel()
        val auth = token
        if (auth == null || username.length < MIN_USERNAME) {
            _state.update { it.copy(usernameStatus = null) }
            return
        }
        _state.update { it.copy(usernameStatus = UsernameStatus.CHECKING) }
        usernameJob = viewModelScope.launch {
            delay(AppConstants.ShopRegistration.USERNAME_CHECK_DEBOUNCE_MS)
            runCatching { api.registrationUsernameAvailable(auth, username) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(usernameStatus = if (result.available) UsernameStatus.AVAILABLE else UsernameStatus.TAKEN)
                    }
                }
                .onFailure { error ->
                    error.rethrowIfStructuredCancellation()
                    _state.update { it.copy(usernameStatus = null) }
                }
        }
    }

    fun setPassword(value: String) { _state.update { it.copy(password = value, error = null) } }
    fun setConfirmPassword(value: String) { _state.update { it.copy(confirmPassword = value, error = null) } }

    // endregion

    // region Navigation

    fun openStep(step: RegistrationStep) {
        val s = _state.value
        if (step == RegistrationStep.PHONE || step > s.reachedStep || s.loading) return
        _state.update {
            it.copy(step = step, editingFromReview = s.step == RegistrationStep.REVIEW && step != RegistrationStep.REVIEW, error = null)
        }
    }

    /** Returns false when the flow should close (back from the phone step). */
    fun back(): Boolean {
        val s = _state.value
        if (s.loading) return true
        if (s.step == RegistrationStep.PHONE) {
            if (!s.otpSent) return false
            _state.update { it.copy(otpSent = false, otp = "", error = null) }
            return true
        }
        if (s.step == RegistrationStep.OWNER) return false
        val previous = if (s.editingFromReview) RegistrationStep.REVIEW else RegistrationStep.entries[s.step.ordinal - 1]
        _state.update { it.copy(step = previous, editingFromReview = false, error = null) }
        return true
    }

    /** Validates the current step, saves the draft and moves forward. */
    fun next() {
        val s = _state.value
        if (s.loading) return
        validate(s)?.let { error ->
            _state.update { it.copy(error = error) }
            return
        }
        val target = if (s.editingFromReview) {
            RegistrationStep.REVIEW
        } else {
            RegistrationStep.entries[(s.step.ordinal + 1).coerceAtMost(RegistrationStep.REVIEW.ordinal)]
        }
        val reached = maxOf(s.reachedStep, target)
        val auth = token ?: return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val outgoing = s.draft.copy(
                step = reached.ordinal,
                platform = appBuildInfo().platform,
                pushToken = PushSignals.token.value,
            )
            runCatching { api.saveRegistrationDraft(auth, outgoing) }
                .onSuccess { saved ->
                    if (s.step == RegistrationStep.LOCATION && !saved.locationVerified) {
                        _state.update { it.copy(loading = false, draft = saved.withDefaults(), error = UiText.res(Res.string.reg_error_location_far, AppConstants.ShopRegistration.LOCATION_TOLERANCE_KM)) }
                        return@onSuccess
                    }
                    _state.update {
                        it.copy(
                            loading = false,
                            draft = saved.withDefaults(),
                            step = target,
                            reachedStep = reached,
                            editingFromReview = false,
                        )
                    }
                }
                .onFailure { error ->
                    error.rethrowIfStructuredCancellation()
                    _state.update { it.copy(loading = false, error = mapRegistrationError(error, verifying = false)) }
                }
        }
    }

    fun submit(onRegistered: (shopName: String, loginUsername: String) -> Unit) {
        val s = _state.value
        val auth = token ?: return
        if (s.loading) return
        RegistrationStep.entries.filter { it != RegistrationStep.PHONE && it != RegistrationStep.REVIEW }.forEach { step ->
            validate(s.copy(step = step))?.let { error ->
                _state.update { it.copy(step = step, editingFromReview = true, error = error) }
                return
            }
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                api.submitShopRegistration(
                    auth,
                    ShopRegistrationSubmit(
                        draft = s.draft.copy(platform = appBuildInfo().platform, pushToken = PushSignals.token.value),
                        password = s.password,
                    ),
                )
            }
                .onSuccess { result ->
                    token = null
                    _state.update { it.copy(loading = false, password = "", confirmPassword = "") }
                    onRegistered(result.shopName, result.loginUsername)
                }
                .onFailure { error ->
                    error.rethrowIfStructuredCancellation()
                    _state.update { it.copy(loading = false, error = mapRegistrationError(error, verifying = false)) }
                }
        }
    }

    // endregion

    private fun validate(s: ShopRegistrationUiState): UiText? {
        val d = s.draft
        return when (s.step) {
            RegistrationStep.PHONE, RegistrationStep.REVIEW -> null
            RegistrationStep.OWNER -> when {
                d.ownerName.trim().length < MIN_NAME -> UiText.res(Res.string.reg_error_owner_name)
                d.ownerEmail.isNotBlank() && !EMAIL_REGEX.matches(d.ownerEmail.trim()) -> UiText.res(Res.string.reg_error_email)
                else -> null
            }
            RegistrationStep.SHOP -> when {
                d.shopName.trim().length < MIN_NAME -> UiText.res(Res.string.reg_error_shop_name)
                s.shopTypeOptions.isNotEmpty() && d.shopTypes.isEmpty() -> UiText.res(Res.string.reg_error_shop_types)
                else -> null
            }
            RegistrationStep.LOCATION -> when {
                d.address.isBlank() || d.city.isBlank() || d.state.isBlank() -> UiText.res(Res.string.reg_error_address)
                d.zip.length != AppConstants.ShopRegistration.ZIP_LENGTH || !d.zip.all(Char::isDigit) ->
                    UiText.res(Res.string.reg_error_zip)
                d.lat == null || d.lng == null -> UiText.res(Res.string.reg_error_pin_required)
                d.deviceLat == null || d.deviceLng == null -> UiText.res(Res.string.reg_error_gps_required)
                !s.locationVerified ->
                    UiText.res(Res.string.reg_error_location_far, AppConstants.ShopRegistration.LOCATION_TOLERANCE_KM)
                else -> null
            }
            RegistrationStep.BUSINESS -> when {
                d.operatingHours.all { it.closed } -> UiText.res(Res.string.reg_error_hours)
                else -> null
            }
            RegistrationStep.CREDENTIALS -> when {
                d.loginUsername.length < MIN_USERNAME -> UiText.res(Res.string.reg_error_username)
                s.usernameStatus == UsernameStatus.TAKEN -> UiText.res(Res.string.reg_error_username_taken)
                !isStrongPassword(s.password) ->
                    UiText.res(Res.string.reg_error_weak_password, AppConstants.ShopRegistration.MIN_PASSWORD)
                s.password != s.confirmPassword -> UiText.res(Res.string.reg_error_password_mismatch)
                else -> null
            }
        }
    }

    private fun mapRegistrationError(error: Throwable, verifying: Boolean): UiText? =
        when ((error as? ApiException)?.code) {
            AppConstants.ShopRegistration.CODE_ALREADY_REGISTERED -> UiText.res(Res.string.reg_error_already_registered)
            AppConstants.ShopRegistration.CODE_LOCATION_UNVERIFIED ->
                UiText.res(Res.string.reg_error_location_far, AppConstants.ShopRegistration.LOCATION_TOLERANCE_KM)
            AppConstants.ShopRegistration.CODE_WEAK_PASSWORD ->
                UiText.res(Res.string.reg_error_weak_password, AppConstants.ShopRegistration.MIN_PASSWORD)
            else -> if (verifying) mapPhoneAuthError(error, verifying = true) else error.toApiUiText()
        }

    private companion object {
        const val MIN_NAME = 2
        const val MIN_USERNAME = 4
        val EMAIL_REGEX = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")
    }
}

fun isStrongPassword(password: String): Boolean =
    password.length >= AppConstants.ShopRegistration.MIN_PASSWORD &&
        password.any(Char::isLetter) &&
        password.any(Char::isDigit)

/** Fills defaults the server leaves empty for a fresh draft (week hours, country). */
private fun ShopRegistrationDraftDto.withDefaults(): ShopRegistrationDraftDto = copy(
    country = country.ifBlank { AppConstants.ShopRegistration.DEFAULT_COUNTRY },
    operatingHours = operatingHours.ifEmpty {
        AppConstants.ShopRegistration.WEEK_DAYS.map {
            ShopHoursDayDto(
                day = it,
                start = AppConstants.ShopRegistration.DEFAULT_OPEN,
                end = AppConstants.ShopRegistration.DEFAULT_CLOSE,
            )
        }
    },
)
