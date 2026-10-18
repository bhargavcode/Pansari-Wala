package org.bhargav.pansariwala.feature.delivery

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.JwtAuthCache
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.PartnerRegisterRequest
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.domain.model.DeliveryOffer
import org.bhargav.pansariwala.domain.model.DeliveryOfferStatus
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.domain.model.PartnerEarnings
import org.bhargav.pansariwala.domain.model.PartnerProfile
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.domain.model.GeoPoint
import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.platform.PartnerLocationTracker
import org.bhargav.pansariwala.platform.LocationPermissionDeniedException
import org.bhargav.pansariwala.platform.LocationUnavailableException
import org.bhargav.pansariwala.platform.FormatPlateOcr
import org.bhargav.pansariwala.platform.ImagePicker
import org.bhargav.pansariwala.platform.CameraPermissionDeniedException
import org.bhargav.pansariwala.platform.pickImageCatching
import org.bhargav.pansariwala.media.ImageSource
import org.bhargav.pansariwala.media.ImageUploadFeature
import org.bhargav.pansariwala.media.PhotoUploadUi
import org.bhargav.pansariwala.media.prepareImageForUpload
import org.bhargav.pansariwala.platform.PhoneAuthGateway
import org.bhargav.pansariwala.platform.PhoneOtpSession
import org.bhargav.pansariwala.platform.digitsPhone
import org.bhargav.pansariwala.platform.fetchPlaceDetails
import org.bhargav.pansariwala.platform.mapPhoneAuthError
import org.bhargav.pansariwala.platform.otpCountdown
import org.bhargav.pansariwala.platform.otpRetryAfterSec
import org.bhargav.pansariwala.platform.sanitizeOtpInput
import org.bhargav.pansariwala.platform.toPhoneOtpSession
import org.bhargav.pansariwala.platform.normalizeVehicleReg
import org.bhargav.pansariwala.platform.searchPlaces
import org.bhargav.pansariwala.platform.geocodeAddress
import org.bhargav.pansariwala.platform.isLocationPermissionGranted
import org.bhargav.pansariwala.notification.NotificationRouter
import org.bhargav.pansariwala.notification.OrderUpdate
import org.bhargav.pansariwala.notification.OrderUpdates
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.AppClock
import org.bhargav.pansariwala.feature.reports.ReportRange
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_address_required
import pansariwala.shared.generated.resources.error_address_coordinates
import pansariwala.shared.generated.resources.error_email_invalid
import pansariwala.shared.generated.resources.error_otp_invalid
import pansariwala.shared.generated.resources.error_generic
import pansariwala.shared.generated.resources.error_place_details_failed
import pansariwala.shared.generated.resources.partner_job_load_failed
import pansariwala.shared.generated.resources.partner_job_unavailable
import pansariwala.shared.generated.resources.error_phone_invalid
import pansariwala.shared.generated.resources.error_vehicle_reg_invalid
import pansariwala.shared.generated.resources.error_name_required
import pansariwala.shared.generated.resources.error_not_a_partner
import pansariwala.shared.generated.resources.error_phone_required
import pansariwala.shared.generated.resources.error_photo_pick_failed
import pansariwala.shared.generated.resources.error_camera_permission_denied
import pansariwala.shared.generated.resources.error_photo_upload_in_progress
import pansariwala.shared.generated.resources.error_image_upload_failed
import pansariwala.shared.generated.resources.error_profile_photo_save_failed
import pansariwala.shared.generated.resources.error_plate_mismatch
import pansariwala.shared.generated.resources.location_unavailable
import pansariwala.shared.generated.resources.dev_otp_hint
import pansariwala.shared.generated.resources.otp_sent_partner
import pansariwala.shared.generated.resources.otp_sent_partner_login
import pansariwala.shared.generated.resources.otp_sent_to
import pansariwala.shared.generated.resources.partner_register_location_required

/** Keep in sync with server `Security.isValidVehicleReg` (state series + Bharat series). */
private val VEHICLE_REG_PATTERN = Regex("^([A-Z]{2}\\d{1,2}[A-Z]{0,3}\\d{1,4}|\\d{2}BH\\d{4}[A-Z]{1,2})$")
private val EMAIL_PATTERN = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

data class PartnerLoginUiState(
    val phone: String = "",
    val otp: String = "",
    val loading: Boolean = false,
    val error: UiText? = null,
    val hint: UiText? = null,
    val resendInSec: Int = 0,
)

class PartnerLoginViewModel(
    private val api: PansariApi,
    private val preferences: AppPreferences,
    private val phoneAuth: PhoneAuthGateway,
) : ViewModel() {
    private val _state = MutableStateFlow(PartnerLoginUiState())
    val state: StateFlow<PartnerLoginUiState> = _state.asStateFlow()

    fun dismissError() { _state.update { it.copy(error = null) } }
    private var session: PhoneOtpSession? = null
    private var sessionPhone: String? = null

    private var countdownJob: Job? = null

    fun setPhone(value: String) { _state.update { it.copy(phone = value, error = null) } }
    fun setOtp(value: String) { _state.update { it.copy(otp = sanitizeOtpInput(value), error = null) } }

    /** Shared across the phone and OTP screens, so clear it after sign-in / sign-out. */
    fun reset() {
        countdownJob?.cancel()
        session = null
        sessionPhone = null
        _state.value = PartnerLoginUiState()
    }

    /** [onSent] runs once a code is on its way (or one sent to this number is still valid). */
    fun sendOtp(onSent: () -> Unit = {}) {
        if (_state.value.loading) return
        if (_state.value.resendInSec > 0) {
            if (session != null && sessionPhone == _state.value.phone) onSent()
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            val phone = _state.value.phone
            phoneAuth.sendOtp(phone)
                .onSuccess { next ->
                    session = next
                    sessionPhone = phone
                    _state.update {
                        it.copy(
                            loading = false,
                            otp = "",
                            hint = when {
                                next.devOtp != null -> UiText.res(Res.string.dev_otp_hint)
                                next.maskedPhone.isNotBlank() -> UiText.res(Res.string.otp_sent_to, next.maskedPhone)
                                else -> UiText.res(Res.string.otp_sent_partner_login)
                            },
                        )
                    }
                    countdownJob = otpCountdown(countdownJob, next.resendAfterSec) { left ->
                        _state.update { it.copy(resendInSec = left) }
                    }
                    onSent()
                }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = mapPhoneAuthError(error, verifying = false)) }
                    error.otpRetryAfterSec()?.let { secs ->
                        countdownJob = otpCountdown(countdownJob, secs) { left -> _state.update { it.copy(resendInSec = left) } }
                    }
                }
        }
    }

    fun verify(onDone: () -> Unit) {
        val otpSession = session ?: return
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            phoneAuth.verifyOtp(_state.value.phone, _state.value.otp, otpSession).onSuccess { token ->
                if (token.role != AppConstants.Roles.PARTNER) {
                    _state.update { it.copy(loading = false, error = UiText.res(Res.string.error_not_a_partner)) }
                    return@launch
                }
                preferences.saveToken(token)
                JwtAuthCache.invalidate()
                onDone()
            }.onFailure { error ->
                _state.update { s -> s.copy(loading = false, error = mapPhoneAuthError(error, verifying = true)) }
            }
        }
    }
}

enum class RegisterPhoto(val feature: ImageUploadFeature) {
    PROFILE(ImageUploadFeature.PARTNER_REGISTER_PROFILE),
    DL(ImageUploadFeature.PARTNER_REGISTER_DOCUMENT),
    ID(ImageUploadFeature.PARTNER_REGISTER_DOCUMENT),
    VEHICLE(ImageUploadFeature.PARTNER_REGISTER_VEHICLE),
}

data class RegisterUiState(
    val name: String = "",
    val email: String = "",
    val placeQuery: String = "",
    val predictions: List<org.bhargav.pansariwala.platform.PlacePrediction> = emptyList(),
    val address: String = "",
    val locality: String = "",
    val lat: Double? = null,
    val lng: Double? = null,
    val phone: String = "",
    val vehicleReg: String = "",
    val profilePhoto: String = "",
    val dlPhoto: String = "",
    val idPhoto: String = "",
    val vehiclePhoto: String = "",
    val photoUploads: Map<RegisterPhoto, PhotoUploadUi> = emptyMap(),
    val otp: String = "",
    val loading: Boolean = false,
    val requestLocationPermission: Boolean = false,
    val showLocationDeniedDialog: Boolean = false,
    val error: UiText? = null,
    val hint: UiText? = null,
    val resendInSec: Int = 0,
)

class PartnerRegisterViewModel(
    private val api: PansariApi,
    private val preferences: AppPreferences,
    private val location: DeviceLocation,
    private val imagePicker: ImagePicker,
    private val phoneAuth: PhoneAuthGateway,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    private val _state = MutableStateFlow(restoreForm())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    init {
        // Camera / gallery can push the activity out of memory; keep the form across recreation.
        viewModelScope.launch {
            _state.collect { s ->
                savedState[KEY_NAME] = s.name
                savedState[KEY_EMAIL] = s.email
                savedState[KEY_PLACE_QUERY] = s.placeQuery
                savedState[KEY_ADDRESS] = s.address
                savedState[KEY_LOCALITY] = s.locality
                savedState[KEY_LAT] = s.lat
                savedState[KEY_LNG] = s.lng
                savedState[KEY_PHONE] = s.phone
                savedState[KEY_VEHICLE_REG] = s.vehicleReg
                savedState[KEY_PROFILE_PHOTO] = s.profilePhoto
                savedState[KEY_DL_PHOTO] = s.dlPhoto
                savedState[KEY_ID_PHOTO] = s.idPhoto
                savedState[KEY_VEHICLE_PHOTO] = s.vehiclePhoto
            }
        }
    }

    private fun restoreForm(): RegisterUiState = RegisterUiState(
        name = savedState[KEY_NAME] ?: "",
        email = savedState[KEY_EMAIL] ?: "",
        placeQuery = savedState[KEY_PLACE_QUERY] ?: "",
        address = savedState[KEY_ADDRESS] ?: "",
        locality = savedState[KEY_LOCALITY] ?: "",
        lat = savedState[KEY_LAT],
        lng = savedState[KEY_LNG],
        phone = savedState[KEY_PHONE] ?: "",
        vehicleReg = savedState[KEY_VEHICLE_REG] ?: "",
        profilePhoto = savedState[KEY_PROFILE_PHOTO] ?: "",
        dlPhoto = savedState[KEY_DL_PHOTO] ?: "",
        idPhoto = savedState[KEY_ID_PHOTO] ?: "",
        vehiclePhoto = savedState[KEY_VEHICLE_PHOTO] ?: "",
    )

    private companion object {
        const val KEY_NAME = "reg_name"
        const val KEY_EMAIL = "reg_email"
        const val KEY_PLACE_QUERY = "reg_place_query"
        const val KEY_ADDRESS = "reg_address"
        const val KEY_LOCALITY = "reg_locality"
        const val KEY_LAT = "reg_lat"
        const val KEY_LNG = "reg_lng"
        const val KEY_PHONE = "reg_phone"
        const val KEY_VEHICLE_REG = "reg_vehicle_reg"
        const val KEY_PROFILE_PHOTO = "reg_profile_photo"
        const val KEY_DL_PHOTO = "reg_dl_photo"
        const val KEY_ID_PHOTO = "reg_id_photo"
        const val KEY_VEHICLE_PHOTO = "reg_vehicle_photo"
    }

    fun dismissError() { _state.update { it.copy(error = null) } }
    private var session: PhoneOtpSession? = null
    private var sessionPhone: String? = null
    private var registered = false
    private var searchJob: Job? = null
    private val ocr = FormatPlateOcr()

    fun setName(v: String) { _state.update { it.copy(name = v, error = null) } }
    fun setEmail(v: String) { _state.update { it.copy(email = v, error = null) } }
    fun setAddress(v: String) {
        _state.update { it.copy(address = v, error = null, lat = null, lng = null) }
    }
    fun setLocality(v: String) {
        _state.update { it.copy(locality = v, error = null, lat = null, lng = null) }
    }
    fun setPhone(v: String) { _state.update { it.copy(phone = v, error = null) } }
    fun setVehicleReg(v: String) { _state.update { it.copy(vehicleReg = v, error = null) } }
    fun setOtp(v: String) { _state.update { it.copy(otp = sanitizeOtpInput(v), error = null) } }
    private var countdownJob: Job? = null

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

    fun requestLocationForAddress() {
        _state.update { it.copy(requestLocationPermission = true, error = null) }
    }

    fun consumeLocationPermissionRequest() {
        _state.update { it.copy(requestLocationPermission = false) }
    }

    fun onLocationPermissionResult(granted: Boolean) {
        if (!granted) {
            _state.update {
                it.copy(
                    showLocationDeniedDialog = true,
                    error = UiText.res(Res.string.partner_register_location_required),
                )
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { location.currentOrDefault() }
                .onSuccess { geo ->
                    _state.update { it.copy(lat = geo.lat, lng = geo.lng, loading = false) }
                }
                .onFailure { err ->
                    val message = when (err) {
                        is LocationPermissionDeniedException ->
                            UiText.res(Res.string.partner_register_location_required)
                        is LocationUnavailableException ->
                            UiText.res(Res.string.location_unavailable)
                        else -> UiText.res(Res.string.location_unavailable)
                    }
                    _state.update { it.copy(loading = false, error = message) }
                }
        }
    }

    fun retryLocationPermission() {
        _state.update {
            it.copy(showLocationDeniedDialog = false, requestLocationPermission = true, error = null)
        }
    }

    fun dismissLocationDeniedDialog() {
        _state.update { it.copy(showLocationDeniedDialog = false) }
    }

    private val photoJobs = mutableMapOf<RegisterPhoto, Job>()

    fun attachPhoto(photo: RegisterPhoto, source: ImageSource) {
        photoJobs[photo]?.cancel()
        photoJobs[photo] = viewModelScope.launch {
            val picked = imagePicker.pickImageCatching(source).getOrElse { err ->
                _state.update { it.copy(error = err.toPickErrorText()) }
                return@launch
            } ?: return@launch
            setPhotoUpload(photo, PhotoUploadUi(uploading = true))
            val compressed = runCatching { prepareImageForUpload(picked) }.getOrElse {
                setPhotoUpload(photo, PhotoUploadUi(failed = true))
                _state.update { it.copy(error = UiText.res(Res.string.error_photo_pick_failed)) }
                return@launch
            }
            setPhotoUpload(photo, PhotoUploadUi(previewBytes = compressed.previewBytes, uploading = true))
            if (photo == RegisterPhoto.VEHICLE) vehiclePhotoBytes = compressed.bytes
            runCatching {
                api.uploadImageGuest(
                    prefix = photo.feature.prefix,
                    fileName = compressed.displayName,
                    bytes = compressed.bytes,
                    contentType = compressed.mimeType,
                    thumbnailBytes = compressed.thumbnailBytes,
                ).url
            }.onSuccess { url ->
                _state.update { s ->
                    val withUrl = when (photo) {
                        RegisterPhoto.PROFILE -> s.copy(profilePhoto = url)
                        RegisterPhoto.DL -> s.copy(dlPhoto = url)
                        RegisterPhoto.ID -> s.copy(idPhoto = url)
                        RegisterPhoto.VEHICLE -> s.copy(vehiclePhoto = url)
                    }
                    withUrl.copy(
                        photoUploads = s.photoUploads + (photo to PhotoUploadUi(previewBytes = compressed.previewBytes)),
                    )
                }
            }.onFailure { err ->
                err.rethrowIfStructuredCancellation()
                setPhotoUpload(photo, PhotoUploadUi(previewBytes = compressed.previewBytes, failed = true))
                _state.update { it.copy(error = UiText.res(Res.string.error_image_upload_failed)) }
            }
        }
    }

    private fun setPhotoUpload(photo: RegisterPhoto, upload: PhotoUploadUi) {
        _state.update { it.copy(photoUploads = it.photoUploads + (photo to upload)) }
    }

    private var vehiclePhotoBytes: ByteArray = ByteArray(0)

    private fun detailsError(s: RegisterUiState): UiText? = when {
        s.photoUploads.values.any { it.uploading } -> UiText.res(Res.string.error_photo_upload_in_progress)
        s.name.trim().length < 2 -> UiText.res(Res.string.error_name_required)
        !EMAIL_PATTERN.matches(s.email.trim()) -> UiText.res(Res.string.error_email_invalid)
        s.phone.filter { it.isDigit() }.length < AppConstants.PHONE_LOCAL_DIGITS -> UiText.res(Res.string.error_phone_required)
        else -> null
    }

    private fun addressError(s: RegisterUiState): UiText? =
        if (s.address.isBlank()) UiText.res(Res.string.error_address_required) else null

    fun continueFromDetails(onValid: () -> Unit) {
        val error = detailsError(_state.value)
        if (error != null) _state.update { it.copy(error = error) } else onValid()
    }

    fun continueFromAddress(onValid: () -> Unit) {
        val error = addressError(_state.value)
        if (error != null) _state.update { it.copy(error = error) } else onValid()
    }

    /** Shared across the registration screens, so clear it once the partner is signed in. */
    fun reset() {
        countdownJob?.cancel()
        session = null
        sessionPhone = null
        registered = false
        vehiclePhotoBytes = ByteArray(0)
        _state.value = RegisterUiState()
    }

    /** Registers (first time) and sends the OTP; [onSent] runs once a code is on its way or still valid. */
    fun save(onSent: () -> Unit = {}) {
        if (_state.value.loading) return
        if (_state.value.resendInSec > 0) {
            if (session != null && sessionPhone == _state.value.phone) onSent()
            return
        }
        viewModelScope.launch {
            val s = _state.value
            val formError = detailsError(s) ?: addressError(s)
            if (formError != null) {
                _state.update { it.copy(error = formError) }
                return@launch
            }
            _state.update { it.copy(loading = true, error = null) }
            val phoneDigits = _state.value.phone.filter { it.isDigit() }
            if (phoneDigits.length != AppConstants.PHONE_LOCAL_DIGITS) {
                _state.update { it.copy(loading = false, error = UiText.res(Res.string.error_phone_invalid)) }
                return@launch
            }
            val entered = normalizeVehicleReg(_state.value.vehicleReg)
            if (!VEHICLE_REG_PATTERN.matches(entered)) {
                _state.update { it.copy(loading = false, error = UiText.res(Res.string.error_vehicle_reg_invalid)) }
                return@launch
            }
            val photoBytes = vehiclePhotoBytes.takeIf { it.isNotEmpty() } ?: ByteArray(0)
            val ocrResult = ocr.readRegistration(photoBytes)
            val plateFromPhoto = ocrResult.getOrNull()?.let { normalizeVehicleReg(it) }.orEmpty()
            if (plateFromPhoto.isNotEmpty() && plateFromPhoto != entered) {
                _state.update {
                    it.copy(
                        loading = false,
                        error = UiText.res(Res.string.error_plate_mismatch, plateFromPhoto, entered),
                    )
                }
                return@launch
            }
            val geo = resolveRegistrationGeo()
            if (geo == null) {
                _state.update { it.copy(loading = false, error = UiText.res(Res.string.error_address_coordinates)) }
                return@launch
            }
            runCatching {
                if (registered) {
                    phoneAuth.sendOtp(_state.value.phone).getOrThrow()
                } else {
                    api.registerPartner(
                        PartnerRegisterRequest(
                            name = _state.value.name,
                            email = _state.value.email,
                            address = listOf(_state.value.address, _state.value.locality)
                                .filter { it.isNotBlank() }
                                .joinToString(", "),
                            phone = digitsPhone(_state.value.phone),
                            vehicleReg = entered,
                            vehiclePhotoUrl = _state.value.vehiclePhoto,
                            profilePhotoUrl = _state.value.profilePhoto,
                            dlPhotoUrl = _state.value.dlPhoto,
                            idPhotoUrl = _state.value.idPhoto,
                            lat = geo.lat,
                            lng = geo.lng,
                        ),
                    ).also { registered = true }
                        .takeIf { it.sessionId.isNotBlank() }
                        ?.toPhoneOtpSession()
                        ?: phoneAuth.sendOtp(_state.value.phone).getOrThrow()
                }
            }.onSuccess { otpSession ->
                session = otpSession
                sessionPhone = _state.value.phone
                _state.update {
                    it.copy(
                        loading = false,
                        otp = "",
                        hint = when {
                            otpSession.devOtp != null -> UiText.res(Res.string.dev_otp_hint)
                            otpSession.maskedPhone.isNotBlank() -> UiText.res(Res.string.otp_sent_to, otpSession.maskedPhone)
                            else -> UiText.res(Res.string.otp_sent_partner)
                        },
                    )
                }
                countdownJob = otpCountdown(countdownJob, otpSession.resendAfterSec) { left ->
                    _state.update { it.copy(resendInSec = left) }
                }
                onSent()
            }.onFailure { error ->
                error.otpRetryAfterSec()?.let { secs ->
                    countdownJob = otpCountdown(countdownJob, secs) { left -> _state.update { it.copy(resendInSec = left) } }
                }
                val mapped = when (error) {
                    is LocationPermissionDeniedException ->
                        UiText.res(Res.string.partner_register_location_required)
                    is LocationUnavailableException ->
                        UiText.res(Res.string.location_unavailable)
                    else -> mapPhoneAuthError(error, verifying = false)
                }
                _state.update { s -> s.copy(loading = false, error = mapped) }
            }
        }
    }

    private suspend fun resolveRegistrationGeo(): GeoPoint? {
        val cachedLat = _state.value.lat
        val cachedLng = _state.value.lng
        if (cachedLat != null && cachedLng != null) {
            return GeoPoint(cachedLat, cachedLng)
        }
        // Location permission is asked only after registration, so fall back to geocoding the typed address.
        val query = listOf(_state.value.address, _state.value.locality, "India").filter { it.isNotBlank() }.joinToString(", ")
        geocodeAddress(query)?.let { return GeoPoint(it.lat, it.lng) }
        if (!isLocationPermissionGranted()) return null
        return runCatching { location.currentOrDefault() }.getOrNull()
    }

    fun verify(onDone: () -> Unit) {
        val otpSession = session ?: return
        if (_state.value.loading) return
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            phoneAuth.verifyOtp(_state.value.phone, _state.value.otp, otpSession).onSuccess {
                preferences.saveToken(it)
                JwtAuthCache.invalidate()
                onDone()
            }.onFailure { error ->
                _state.update { s -> s.copy(loading = false, error = mapPhoneAuthError(error, verifying = true)) }
            }
            _state.update { it.copy(loading = false) }
        }
    }
}

data class PartnerHomeUiState(
    val profile: PartnerProfile? = null,
    val online: Boolean = false,
    val incomingOffer: DeliveryOffer? = null,
    val availableOffers: List<DeliveryOffer> = emptyList(),
    val offerSecondsLeft: Int = 0,
    val acceptedJobs: List<Order> = emptyList(),
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val showOfferTakenSheet: Boolean = false,
    val error: UiText? = null,
    val lat: Double = AppConstants.DEFAULT_MAP_LAT,
    val lng: Double = AppConstants.DEFAULT_MAP_LNG,
    val fetchingLocation: Boolean = false,
    val requestLocationPermission: Boolean = false,
    val showLocationDeniedDialog: Boolean = false,
    val locationPermissionGranted: Boolean = false,
    val showLocationPrompt: Boolean = false,
    val earnings: PartnerEarnings? = null,
    val earningsPeriod: PartnerEarningsPeriod = PartnerEarningsPeriod.TODAY,
    val earningsRange: ReportRange? = null,
    val earningsLoading: Boolean = false,
    val showEarningsRangePicker: Boolean = false,
)

private data class HomeRefreshPayload(
    val accepted: List<Order>,
    val offers: List<DeliveryOffer>,
    val profile: PartnerProfile?,
    val incoming: DeliveryOffer?,
)

class PartnerHomeViewModel(
    private val api: PansariApi,
    private val location: DeviceLocation,
    private val locationTracker: PartnerLocationTracker,
    private val preferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(PartnerHomeUiState())
    val state: StateFlow<PartnerHomeUiState> = _state.asStateFlow()
    private var timerJob: Job? = null
    /** Offers already shown in the accept flash — stay listable, do not re-popup. */
    private val flashedOfferIds = mutableSetOf<String>()
    /** Jobs finished on this device; a refresh that raced the completion must not bring them back. */
    private val finishedOrderIds = mutableSetOf<String>()
    private val tz = TimeZone.currentSystemDefault()
    private var earningsFrom: LocalDate? = null
    private var earningsTo: LocalDate? = null
    private var earningsJob: Job? = null

    fun dismissError() { _state.update { it.copy(error = null) } }

    fun selectEarningsPeriod(period: PartnerEarningsPeriod) {
        if (period == PartnerEarningsPeriod.CUSTOM) {
            _state.update { it.copy(showEarningsRangePicker = true) }
            return
        }
        _state.update { it.copy(earningsPeriod = period) }
        loadEarnings()
    }

    fun dismissEarningsRangePicker() { _state.update { it.copy(showEarningsRangePicker = false) } }

    /** Picker values are UTC-midnight millis; [toUtcMs] null means a single day. */
    fun setEarningsCustomRange(fromUtcMs: Long, toUtcMs: Long?) {
        earningsFrom = Instant.fromEpochMilliseconds(fromUtcMs).toLocalDateTime(TimeZone.UTC).date
        earningsTo = toUtcMs?.let { Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.UTC).date } ?: earningsFrom
        _state.update { it.copy(earningsPeriod = PartnerEarningsPeriod.CUSTOM, showEarningsRangePicker = false) }
        loadEarnings()
    }

    private fun loadEarnings() {
        earningsJob?.cancel()
        val today = Instant.fromEpochMilliseconds(AppClock.nowMillis()).toLocalDateTime(tz).date
        val range = _state.value.earningsPeriod.range(today, earningsFrom, earningsTo)
        _state.update { it.copy(earningsLoading = true, earningsRange = range) }
        earningsJob = viewModelScope.launch {
            runCatching { api.partnerEarnings(range.fromMs(tz), range.toMs(tz)) }
                .onSuccess { earnings -> _state.update { it.copy(earnings = earnings, earningsLoading = false) } }
                .onFailure { e ->
                    e.rethrowIfStructuredCancellation()
                    _state.update { it.copy(earningsLoading = false, error = e.toApiUiText()) }
                }
        }
    }

    init {
        viewModelScope.launch {
            val cached = preferences.getCachedPartnerProfile()
            if (cached != null) {
                _state.update {
                    it.copy(
                        profile = cached,
                        online = cached.online,
                        loading = false,
                    )
                }
                applyOnlineState(cached.online)
            }
        }
        refresh()
        requestLocationAccessOnLanding()
        viewModelScope.launch {
            NotificationRouter.events.collect { notification ->
                if (notification.type == AppConstants.Notification.TYPE_DELIVERY_OFFER) {
                    refreshForNewOffer()
                }
            }
        }
        viewModelScope.launch {
            OrderUpdates.events.collect { update ->
                val active = update.status == OrderStatus.PARTNER_ACCEPTED || update.status == OrderStatus.ON_THE_WAY
                if (!active && update.status != null) {
                    finishedOrderIds.add(update.orderId)
                    _state.update { s -> s.copy(acceptedJobs = s.acceptedJobs.filterNot { it.id == update.orderId }) }
                }
                refreshJobsAndOffers(presentIncoming = false)
            }
        }
    }

    /** Back on home (e.g. after finishing a job): resync jobs without a full-screen loader. */
    fun onResume() {
        syncCachedProfile()
        if (!_state.value.loading) refreshJobsAndOffers(presentIncoming = false)
    }

    /** OS permission already given: go straight to the GPS check; otherwise explain first. */
    fun requestLocationAccessOnLanding() {
        if (_state.value.locationPermissionGranted) {
            refreshCurrentLocation()
            return
        }
        if (isLocationPermissionGranted()) {
            _state.update { it.copy(requestLocationPermission = true) }
        } else {
            _state.update { it.copy(showLocationPrompt = true) }
        }
    }

    fun acceptLocationPrompt() {
        _state.update { it.copy(showLocationPrompt = false, requestLocationPermission = true) }
    }

    fun dismissLocationPrompt() {
        _state.update { it.copy(showLocationPrompt = false) }
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
            viewModelScope.launch { locationTracker.ensureTracking() }
        }
    }

    fun retryLocationPermission() {
        _state.update { it.copy(showLocationDeniedDialog = false, requestLocationPermission = true) }
    }

    fun dismissLocationDeniedDialog() {
        _state.update { it.copy(showLocationDeniedDialog = false) }
    }

    fun refresh() {
        loadEarnings()
        viewModelScope.launch {
            val keepContent = _state.value.profile != null ||
                _state.value.acceptedJobs.isNotEmpty() ||
                _state.value.availableOffers.isNotEmpty()
            _state.update {
                it.copy(
                    loading = !keepContent,
                    refreshing = keepContent,
                    error = null,
                )
            }
            runCatching {
                coroutineScope {
                    val accepted = async { runCatching { api.acceptedJobs() }.getOrDefault(emptyList()) }
                        .await()
                        .filter { it.isActiveDelivery }
                    val offers = async { runCatching { api.availableOffers() }.getOrDefault(emptyList()) }
                        .await()
                    val profile = async { runCatching { api.partnerProfile() }.getOrNull() }.await()
                    val incoming = async { runCatching { api.incomingOffer() }.getOrNull() }.await()
                    HomeRefreshPayload(accepted, offers, profile, incoming)
                }
            }.onSuccess { payload ->
                if (payload.profile != null) {
                    preferences.setCachedPartnerProfile(payload.profile)
                }
                val online = payload.profile?.online ?: preferences.getPartnerOnlineDuty()
                applyOnlineState(online)
                applyOfferLists(
                    payload.accepted,
                    payload.offers,
                    payload.incoming,
                    presentIncoming = true,
                )
                _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        online = online,
                        profile = payload.profile ?: it.profile,
                        error = null,
                    )
                }
            }.onFailure { e ->
                e.rethrowIfStructuredCancellation()
                val online = preferences.getPartnerOnlineDuty()
                applyOnlineState(online)
                _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        online = online,
                        error = e.toApiUiText(),
                    )
                }
            }
        }
    }

    fun pullRefresh() {
        if (_state.value.refreshing) return
        refresh()
    }

    fun syncCachedProfile() {
        viewModelScope.launch {
            preferences.getCachedPartnerProfile()?.let { cached ->
                _state.update {
                    it.copy(
                        profile = cached,
                        online = cached.online,
                    )
                }
                applyOnlineState(cached.online)
            }
        }
    }

    fun offerSecondsRemaining(offer: DeliveryOffer): Int =
        ((offer.expiresAtEpochMs - AppClock.nowMillis()) / 1000).toInt().coerceAtLeast(0)

    fun setOnline(online: Boolean) {
        viewModelScope.launch {
            runCatching { api.setPartnerOnline(online) }
                .onSuccess {
                    val updatedProfile = _state.value.profile?.copy(online = online)
                    if (updatedProfile != null) {
                        preferences.setCachedPartnerProfile(updatedProfile)
                    } else {
                        preferences.setPartnerOnlineDuty(online)
                    }
                    _state.update { it.copy(online = online, profile = updatedProfile ?: it.profile) }
                    syncLocationDuty(online)
                    if (online) {
                        if (_state.value.locationPermissionGranted) {
                            pushLocation()
                        } else {
                            requestLocationAccessOnLanding()
                        }
                        // One-shot refresh when going online; no periodic polling.
                        refreshJobsAndOffers(presentIncoming = true)
                    } else {
                        timerJob?.cancel()
                        flashedOfferIds.clear()
                        _state.update { it.copy(incomingOffer = null) }
                        refreshAvailableOffers()
                    }
                }
        }
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

    private fun syncLocationDuty(online: Boolean) {
        viewModelScope.launch { locationTracker.setOnlineDuty(online) }
    }

    /** Sync location duty only — offer discovery is push/notification driven, not polled. */
    private fun applyOnlineState(online: Boolean) {
        syncLocationDuty(online)
        if (!online) {
            timerJob?.cancel()
        }
    }

    private fun pushLocation() {
        if (!_state.value.locationPermissionGranted) return
        viewModelScope.launch {
            runCatching { fetchAndPushLocation() }
                .onSuccess { geo ->
                    _state.update { it.copy(lat = geo.lat, lng = geo.lng, error = null) }
                    locationTracker.ensureTracking()
                }
                .onFailure { e ->
                    when (e) {
                        is LocationPermissionDeniedException -> {
                            _state.update {
                                it.copy(
                                    locationPermissionGranted = false,
                                    showLocationDeniedDialog = true,
                                )
                            }
                        }
                        is LocationUnavailableException -> {
                            _state.update { it.copy(error = UiText.res(Res.string.location_unavailable)) }
                        }
                        else -> {
                            // Background location sync retries; don't spam the home screen with socket errors.
                            if (!e.isTransientNetworkFailure()) {
                                _state.update { it.copy(error = e.toApiUiText()) }
                            }
                        }
                    }
                }
        }
    }

    private fun Throwable.isTransientNetworkFailure(): Boolean {
        val message = message.orEmpty().lowercase()
        return message.contains("timeout") ||
            message.contains("connection") ||
            message.contains("unable to resolve") ||
            message.contains("failed to connect")
    }

    /** Shows the device fix immediately; a failed server upload must not leave the map on the default city. */
    private suspend fun fetchAndPushLocation(): GeoPoint {
        val geo = location.currentOrDefault()
        _state.update { it.copy(lat = geo.lat, lng = geo.lng) }
        api.updatePartnerLocation(geo.lat, geo.lng)
        return geo
    }

    /** New offer push/notification → refresh lists and present dialog together. */
    private fun refreshForNewOffer() {
        refreshJobsAndOffers(presentIncoming = true)
    }

    private fun refreshJobsAndOffers(presentIncoming: Boolean) {
        viewModelScope.launch {
            runCatching {
                coroutineScope {
                    val accepted = async { runCatching { api.acceptedJobs() }.getOrDefault(emptyList()) }
                    val offers = async { runCatching { api.availableOffers() }.getOrDefault(emptyList()) }
                    val incoming = async {
                        if (presentIncoming) runCatching { api.incomingOffer() }.getOrNull()
                        else null
                    }
                    Triple(
                        accepted.await().filter { it.isActiveDelivery },
                        offers.await(),
                        incoming.await(),
                    )
                }
            }.onSuccess { (accepted, offers, incoming) ->
                applyOfferLists(accepted, offers, incoming, presentIncoming = presentIncoming)
            }
        }
    }

    private fun applyOfferLists(
        accepted: List<Order>,
        offers: List<DeliveryOffer>,
        incoming: DeliveryOffer?,
        presentIncoming: Boolean,
    ) {
        var presented: DeliveryOffer? = null
        if (presentIncoming && incoming != null) {
            val currentId = _state.value.incomingOffer?.id
            when {
                incoming.id == currentId -> Unit
                incoming.id in flashedOfferIds -> Unit
                else -> {
                    flashedOfferIds.add(incoming.id)
                    presented = incoming
                    startOfferTimer(incoming)
                }
            }
        }
        val incomingId = presented?.id ?: _state.value.incomingOffer?.id
        _state.update {
            it.copy(
                acceptedJobs = accepted.filterNot { job -> job.id in finishedOrderIds },
                availableOffers = if (incomingId == null) {
                    offers
                } else {
                    offers.filterNot { offer -> offer.id == incomingId }
                },
                incomingOffer = presented ?: it.incomingOffer,
            )
        }
    }

    private fun startOfferTimer(offer: DeliveryOffer) {
        timerJob?.cancel()
        val acceptDeadlineMs = minOf(
            AppClock.nowMillis() + AppConstants.PARTNER_OFFER_ACCEPT_MS,
            offer.expiresAtEpochMs,
        )
        timerJob = viewModelScope.launch {
            while (true) {
                val left = ((acceptDeadlineMs - AppClock.nowMillis()) / 1000).toInt().coerceAtLeast(0)
                _state.update { it.copy(offerSecondsLeft = left) }
                if (left <= 0) {
                    _state.update { it.copy(incomingOffer = null) }
                    refreshAvailableOffers()
                    break
                }
                delay(1_000L)
            }
        }
    }

    fun acceptOffer(onAccepted: (String) -> Unit) {
        val offer = _state.value.incomingOffer ?: return
        acceptOfferById(offer.id, onAccepted)
    }

    fun acceptOfferById(offerId: String, onAccepted: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { api.acceptOffer(offerId) }
                .onSuccess { result ->
                    when (result.status) {
                        DeliveryOfferStatus.TAKEN_BY_OTHER -> {
                            _state.update { s ->
                                s.copy(
                                    incomingOffer = null,
                                    showOfferTakenSheet = true,
                                    availableOffers = s.availableOffers.filterNot { it.id == offerId },
                                )
                            }
                            timerJob?.cancel()
                        }
                        else -> {
                            _state.update { s ->
                                s.copy(
                                    incomingOffer = null,
                                    availableOffers = s.availableOffers.filterNot { it.id == offerId },
                                )
                            }
                            timerJob?.cancel()
                            refreshJobsAndOffers(presentIncoming = false)
                            onAccepted(result.orderId)
                        }
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(error = e.toApiUiText()) }
                }
        }
    }

    fun dismissOfferTakenSheet() {
        _state.update { it.copy(showOfferTakenSheet = false) }
        refreshAvailableOffers()
    }

    fun rejectOffer() {
        val offer = _state.value.incomingOffer ?: return
        rejectOfferById(offer.id)
    }

    fun rejectOfferById(offerId: String) {
        viewModelScope.launch {
            runCatching { api.rejectOffer(offerId) }
            _state.update {
                it.copy(
                    incomingOffer = null,
                    availableOffers = it.availableOffers.filterNot { offer -> offer.id == offerId },
                )
            }
            timerJob?.cancel()
            refreshAvailableOffers()
        }
    }

    private fun refreshAvailableOffers() {
        viewModelScope.launch {
            runCatching { api.availableOffers() }.onSuccess { offers ->
                val incomingId = _state.value.incomingOffer?.id
                _state.update {
                    it.copy(
                        availableOffers = if (incomingId == null) {
                            offers
                        } else {
                            offers.filterNot { offer -> offer.id == incomingId }
                        },
                    )
                }
            }
        }
    }

    fun dismissOffer() {
        _state.update { it.copy(incomingOffer = null) }
        timerJob?.cancel()
        refreshAvailableOffers()
    }
}

data class PartnerJobUiState(
    val order: Order? = null,
    val photoOne: String = "",
    val photoTwo: String = "",
    val photoOneUpload: PhotoUploadUi = PhotoUploadUi(),
    val photoTwoUpload: PhotoUploadUi = PhotoUploadUi(),
    val captureStep: Int = 0,
    val loading: Boolean = true,
    val submitting: Boolean = false,
    val error: UiText? = null,
)

class PartnerJobViewModel(
    private val api: PansariApi,
    private val imagePicker: ImagePicker,
) : ViewModel() {
    private val _state = MutableStateFlow(PartnerJobUiState())
    val state: StateFlow<PartnerJobUiState> = _state.asStateFlow()

    fun dismissError() { _state.update { it.copy(error = null) } }

    fun load(orderId: String) {
        if (orderId.isBlank()) {
            _state.update {
                it.copy(loading = false, order = null, error = UiText.res(Res.string.partner_job_unavailable))
            }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null, order = null, submitting = false) }
            runCatching { api.partnerJob(orderId) }
                .onSuccess { order ->
                    val storedOne = order.visiblePickupPhotos.getOrNull(0).orEmpty()
                    val storedTwo = order.visiblePickupPhotos.getOrNull(1).orEmpty()
                    _state.update {
                        it.copy(
                            loading = false,
                            order = order,
                            error = null,
                            photoOne = storedOne.ifBlank { it.photoOne },
                            photoTwo = storedTwo.ifBlank { it.photoTwo },
                            captureStep = if (storedOne.isNotBlank() && storedTwo.isNotBlank() &&
                                order.resumeProgress == AppConstants.PartnerProgress.CAPTURE
                            ) 1 else it.captureStep,
                        )
                    }
                }
                .onFailure { e ->
                    val message = when {
                        e.message?.contains("not found", ignoreCase = true) == true ->
                            UiText.res(Res.string.partner_job_unavailable)
                        e.message?.contains("Forbidden", ignoreCase = true) == true ->
                            UiText.res(Res.string.partner_job_unavailable)
                        else -> e.toApiUiText()
                    }
                    _state.update { s -> s.copy(loading = false, order = null, error = message) }
                }
        }
    }

    private val photoJobs = mutableMapOf<Int, Job>()

    fun attachPhoto(slot: Int, source: ImageSource) {
        photoJobs[slot]?.cancel()
        photoJobs[slot] = viewModelScope.launch {
            val picked = imagePicker.pickImageCatching(source).getOrElse { err ->
                _state.update { it.copy(error = err.toPickErrorText()) }
                return@launch
            } ?: return@launch
            setSlotUpload(slot, PhotoUploadUi(uploading = true))
            val compressed = runCatching { prepareImageForUpload(picked) }.getOrElse {
                setSlotUpload(slot, PhotoUploadUi(failed = true))
                _state.update { it.copy(error = UiText.res(Res.string.error_photo_pick_failed)) }
                return@launch
            }
            setSlotUpload(slot, PhotoUploadUi(previewBytes = compressed.previewBytes, uploading = true))
            runCatching {
                api.uploadImage(
                    prefix = ImageUploadFeature.DELIVERY_PACKET.prefix,
                    fileName = compressed.displayName,
                    bytes = compressed.bytes,
                    contentType = compressed.mimeType,
                    thumbnailBytes = compressed.thumbnailBytes,
                ).url
            }.onSuccess { url ->
                val done = PhotoUploadUi(previewBytes = compressed.previewBytes)
                _state.update {
                    when (slot) {
                        1 -> it.copy(photoOne = url, photoOneUpload = done, error = null)
                        else -> it.copy(photoTwo = url, photoTwoUpload = done, error = null)
                    }
                }
            }.onFailure { err ->
                err.rethrowIfStructuredCancellation()
                setSlotUpload(slot, PhotoUploadUi(previewBytes = compressed.previewBytes, failed = true))
                _state.update { it.copy(error = UiText.res(Res.string.error_image_upload_failed)) }
            }
        }
    }

    private fun setSlotUpload(slot: Int, upload: PhotoUploadUi) {
        _state.update {
            if (slot == 1) it.copy(photoOneUpload = upload) else it.copy(photoTwoUpload = upload)
        }
    }

    fun setCaptureStep(step: Int) { _state.update { it.copy(captureStep = step) } }

    fun arrivedAtStore(onDone: () -> Unit) {
        val orderId = _state.value.order?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            runCatching { api.arrivedAtStore(orderId) }
                .onSuccess {
                    _state.update { it.copy(submitting = false) }
                    onDone()
                }
                .onFailure {
                    _state.update { s -> s.copy(submitting = false, error = UiText.res(Res.string.error_generic)) }
                }
        }
    }

    fun verifyBags(onDone: () -> Unit) {
        val orderId = _state.value.order?.id ?: return
        val one = _state.value.photoOne
        val two = _state.value.photoTwo
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            runCatching { api.verifyBags(orderId, one, two) }
                .onSuccess { order ->
                    val storedOne = order.visiblePickupPhotos.getOrNull(0).orEmpty()
                    val storedTwo = order.visiblePickupPhotos.getOrNull(1).orEmpty()
                    _state.update {
                        it.copy(
                            submitting = false,
                            order = order,
                            photoOne = storedOne.ifBlank { it.photoOne },
                            photoTwo = storedTwo.ifBlank { it.photoTwo },
                            captureStep = if (storedOne.isNotBlank() && storedTwo.isNotBlank()) 1 else it.captureStep,
                        )
                    }
                    onDone()
                }
                .onFailure {
                    _state.update { s -> s.copy(submitting = false, error = UiText.res(Res.string.error_generic)) }
                }
        }
    }

    fun submitPickup(onDone: () -> Unit) {
        val orderId = _state.value.order?.id ?: return
        val one = _state.value.photoOne
        val two = _state.value.photoTwo
        if (one.isBlank() || two.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            runCatching { api.submitPickup(orderId, one, two) }
                .onSuccess { order ->
                    _state.update { it.copy(submitting = false, order = order) }
                    onDone()
                }
                .onFailure {
                    _state.update { s -> s.copy(submitting = false, error = UiText.res(Res.string.error_generic)) }
                }
        }
    }

    fun rateCustomer(stars: Int, comment: String, onDone: () -> Unit) {
        val orderId = _state.value.order?.id ?: return
        if (stars !in 1..5) return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            runCatching { api.rateCustomer(orderId, stars, comment.trim().ifBlank { null }) }
                .onSuccess { order ->
                    _state.update { it.copy(submitting = false, order = order) }
                    onDone()
                }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { s -> s.copy(submitting = false, error = err.toApiUiText()) }
                }
        }
    }

    fun arrivedAtCustomer(onDone: () -> Unit) {
        val orderId = _state.value.order?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            runCatching { api.arrivedAtCustomer(orderId) }
                .onSuccess {
                    _state.update { it.copy(submitting = false) }
                    onDone()
                }
                .onFailure {
                    _state.update { s -> s.copy(submitting = false, error = UiText.res(Res.string.error_generic)) }
                }
        }
    }

    fun completeDelivery(otp: String, onDone: () -> Unit) {
        val orderId = _state.value.order?.id ?: return
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, error = null) }
            runCatching { api.deliverOrder(orderId, otp) }
                .onSuccess { order ->
                    _state.update { it.copy(submitting = false, order = order) }
                    OrderUpdates.emit(OrderUpdate(orderId, OrderStatus.DELIVERED))
                    onDone()
                }
                .onFailure { e ->
                    val otpFailed = e.message?.contains("OTP", ignoreCase = true) == true ||
                        e.message?.contains("400") == true
                    _state.update { s ->
                        s.copy(
                            submitting = false,
                            error = UiText.res(if (otpFailed) Res.string.error_otp_invalid else Res.string.error_generic),
                        )
                    }
                }
        }
    }
}

data class PartnerEarningsUiState(
    val profile: PartnerProfile? = null,
    val earnings: PartnerEarnings? = null,
    val loading: Boolean = true,
    val uploadingPhoto: Boolean = false,
    val profilePhotoPreviewBytes: ByteArray? = null,
    val error: UiText? = null,
)

class PartnerEarningsViewModel(
    private val api: PansariApi,
    private val preferences: AppPreferences,
    private val imagePicker: ImagePicker,
) : ViewModel() {
    private val _state = MutableStateFlow(PartnerEarningsUiState())
    val state: StateFlow<PartnerEarningsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            preferences.getCachedPartnerProfile()?.let { cached ->
                _state.update { it.copy(profile = cached) }
            }
        }
        load()
    }

    fun dismissError() { _state.update { it.copy(error = null) } }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                coroutineScope {
                    val profile = async { api.partnerProfile() }
                    val earnings = async { api.partnerEarnings() }
                    profile.await() to earnings.await()
                }
            }.onSuccess { (profile, earnings) ->
                preferences.setCachedPartnerProfile(profile)
                _state.update { it.copy(loading = false, profile = profile, earnings = earnings) }
            }.onFailure { error ->
                error.rethrowIfStructuredCancellation()
                _state.update { s -> s.copy(loading = false, error = error.toApiUiText()) }
            }
        }
    }

    fun changeProfilePhoto(source: ImageSource) {
        if (_state.value.uploadingPhoto) return
        viewModelScope.launch {
            val picked = imagePicker.pickImageCatching(source).getOrElse { err ->
                _state.update { it.copy(error = err.toPickErrorText()) }
                return@launch
            } ?: return@launch
            _state.update { it.copy(uploadingPhoto = true, error = null) }
            val compressed = runCatching { prepareImageForUpload(picked) }.getOrElse {
                _state.update {
                    it.copy(uploadingPhoto = false, error = UiText.res(Res.string.error_photo_pick_failed))
                }
                return@launch
            }
            _state.update { it.copy(profilePhotoPreviewBytes = compressed.previewBytes) }
            val uploadedUrl = runCatching {
                api.uploadImage(
                    prefix = ImageUploadFeature.PARTNER_PROFILE_PHOTO.prefix,
                    fileName = compressed.displayName,
                    bytes = compressed.bytes,
                    contentType = compressed.mimeType,
                    thumbnailBytes = compressed.thumbnailBytes,
                ).url
            }.getOrElse {
                it.rethrowIfStructuredCancellation()
                _state.update {
                    it.copy(
                        uploadingPhoto = false,
                        profilePhotoPreviewBytes = null,
                        error = UiText.res(Res.string.error_image_upload_failed),
                    )
                }
                return@launch
            }
            // Show URL immediately in UI + prefs; then persist on server.
            val updated = (_state.value.profile ?: preferences.getCachedPartnerProfile())
                ?.copy(profilePhoto = uploadedUrl)
            if (updated != null) {
                preferences.setCachedPartnerProfile(updated)
                _state.update { it.copy(profile = updated) }
            }
            runCatching { api.updatePartnerProfilePhoto(uploadedUrl) }
                .onSuccess {
                    _state.update { it.copy(uploadingPhoto = false, error = null) }
                }
                .onFailure {
                    _state.update {
                        it.copy(
                            uploadingPhoto = false,
                            error = UiText.res(Res.string.error_profile_photo_save_failed),
                        )
                    }
                }
        }
    }
}

private fun Throwable.toPickErrorText(): UiText = UiText.res(
    if (this is CameraPermissionDeniedException) {
        Res.string.error_camera_permission_denied
    } else {
        Res.string.error_photo_pick_failed
    },
)
