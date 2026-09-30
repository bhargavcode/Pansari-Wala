package org.bhargav.pansariwala.feature.delivery

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
import org.bhargav.pansariwala.media.ImageSource
import org.bhargav.pansariwala.media.ImageUploadFeature
import org.bhargav.pansariwala.media.PhotoUploadUi
import org.bhargav.pansariwala.media.prepareImageForUpload
import org.bhargav.pansariwala.platform.PhoneAuthGateway
import org.bhargav.pansariwala.platform.PhoneOtpSession
import org.bhargav.pansariwala.platform.digitsPhone
import org.bhargav.pansariwala.platform.fetchPlaceDetails
import org.bhargav.pansariwala.platform.mapPhoneAuthError
import org.bhargav.pansariwala.platform.normalizeVehicleReg
import org.bhargav.pansariwala.platform.searchPlaces
import org.bhargav.pansariwala.notification.NotificationRouter
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.AppClock
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_address_required
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
import pansariwala.shared.generated.resources.error_photo_upload_in_progress
import pansariwala.shared.generated.resources.error_image_upload_failed
import pansariwala.shared.generated.resources.error_photos_required
import pansariwala.shared.generated.resources.error_profile_photo_save_failed
import pansariwala.shared.generated.resources.error_plate_mismatch
import pansariwala.shared.generated.resources.location_unavailable
import pansariwala.shared.generated.resources.dev_otp_hint
import pansariwala.shared.generated.resources.otp_sent_partner
import pansariwala.shared.generated.resources.otp_sent_partner_login
import pansariwala.shared.generated.resources.partner_register_location_required

private val VEHICLE_REG_PATTERN = Regex("^[A-Z]{2}\\d{1,2}[A-Z]{0,3}\\d{4}$")

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
                                UiText.res(Res.string.otp_sent_partner_login)
                            },
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
    val step: Int = 0,
    val loading: Boolean = false,
    val requestLocationPermission: Boolean = false,
    val showLocationDeniedDialog: Boolean = false,
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

    fun dismissError() { _state.update { it.copy(error = null) } }
    private var session: PhoneOtpSession? = null
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
    fun setOtp(v: String) { _state.update { it.copy(otp = v, error = null) } }

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
            val picked = imagePicker.pickImage(source) ?: return@launch
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

    fun save() {
        viewModelScope.launch {
            val s = _state.value
            when {
                s.photoUploads.values.any { it.uploading } -> {
                    _state.update { it.copy(error = UiText.res(Res.string.error_photo_upload_in_progress)) }
                    return@launch
                }
                s.name.isBlank() -> {
                    _state.update { it.copy(error = UiText.res(Res.string.error_name_required)) }
                    return@launch
                }
                !s.email.contains("@") -> {
                    _state.update { it.copy(error = UiText.res(Res.string.error_email_invalid)) }
                    return@launch
                }
                s.address.isBlank() -> {
                    _state.update { it.copy(error = UiText.res(Res.string.error_address_required)) }
                    return@launch
                }
                s.phone.length < 10 -> {
                    _state.update { it.copy(error = UiText.res(Res.string.error_phone_required)) }
                    return@launch
                }
                s.dlPhoto.isBlank() || s.vehiclePhoto.isBlank() || s.idPhoto.isBlank() -> {
                    _state.update { it.copy(error = UiText.res(Res.string.error_photos_required)) }
                    return@launch
                }
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
                _state.update {
                    it.copy(
                        loading = false,
                        requestLocationPermission = true,
                        error = UiText.res(Res.string.partner_register_location_required),
                    )
                }
                return@launch
            }
            runCatching {
                if (!registered) {
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
                        hint = if (otpSession.devOtp != null) {
                            UiText.res(Res.string.dev_otp_hint)
                        } else {
                            UiText.res(Res.string.otp_sent_partner)
                        },
                    )
                }
            }.onFailure { error ->
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
        return runCatching { location.currentOrDefault() }
            .onFailure {
                if (it is LocationPermissionDeniedException) {
                    _state.update { s -> s.copy(requestLocationPermission = true) }
                }
            }
            .getOrNull()
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

    fun dismissError() { _state.update { it.copy(error = null) } }

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
    }

    fun requestLocationAccessOnLanding() {
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

    private suspend fun fetchAndPushLocation(): GeoPoint {
        val geo = location.currentOrDefault()
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
                acceptedJobs = accepted,
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
            val picked = imagePicker.pickImage(source) ?: return@launch
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
            val picked = imagePicker.pickImage(source) ?: return@launch
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
