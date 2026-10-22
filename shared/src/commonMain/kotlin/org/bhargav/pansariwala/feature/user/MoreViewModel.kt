package org.bhargav.pansariwala.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.domain.model.CustomerProfile
import org.bhargav.pansariwala.domain.model.ShopFilter
import org.bhargav.pansariwala.feature.reports.ReportRange
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.media.ImageSource
import org.bhargav.pansariwala.media.ImageUploadFeature
import org.bhargav.pansariwala.media.prepareImageForUpload
import org.bhargav.pansariwala.platform.ImagePicker
import org.bhargav.pansariwala.platform.PhoneAuthGateway
import org.bhargav.pansariwala.platform.PhoneOtpSession
import org.bhargav.pansariwala.platform.digitsPhone
import org.bhargav.pansariwala.platform.mapPhoneAuthError
import org.bhargav.pansariwala.platform.otpCountdown
import org.bhargav.pansariwala.platform.otpRetryAfterSec
import org.bhargav.pansariwala.platform.pickImageCatching
import org.bhargav.pansariwala.platform.sanitizeOtpInput
import org.bhargav.pansariwala.util.AppClock
import org.bhargav.pansariwala.util.AppConstants
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.dev_otp_hint
import pansariwala.shared.generated.resources.error_image_upload_failed
import pansariwala.shared.generated.resources.error_phone_invalid
import pansariwala.shared.generated.resources.error_photo_pick_failed
import pansariwala.shared.generated.resources.error_profile_required
import pansariwala.shared.generated.resources.more_phone_same
import pansariwala.shared.generated.resources.otp_sent_to
import kotlin.time.Instant

data class SpendSummary(
    val range: ReportRange,
    val total: Double = 0.0,
    val orderCount: Int = 0,
    val loading: Boolean = true,
)

enum class PhoneChangeStep { ENTER_PHONE, ENTER_OTP }

data class PhoneChangeUi(
    val step: PhoneChangeStep = PhoneChangeStep.ENTER_PHONE,
    val phone: String = "",
    val otp: String = "",
    val loading: Boolean = false,
    val error: UiText? = null,
    val hint: UiText? = null,
    val resendInSec: Int = 0,
)

data class MoreUiState(
    val profile: CustomerProfile? = null,
    val loading: Boolean = true,
    val error: UiText? = null,
    val spend: SpendSummary,
    val filter: ShopFilter = ShopFilter(AppConstants.DEFAULT_SEARCH_RADIUS_KM),
    val minRadiusKm: Double = AppConstants.MIN_SEARCH_RADIUS_KM,
    val photoUploading: Boolean = false,
    val photoPreview: ByteArray? = null,
    val addressBusy: Boolean = false,
    val nameSaving: Boolean = false,
    /** Non-null while the change-number sheet is open. */
    val phoneChange: PhoneChangeUi? = null,
    val deletionBusy: Boolean = false,
    val showWelcomeBack: Boolean = false,
)

class MoreViewModel(
    private val api: PansariApi,
    private val preferences: AppPreferences,
    private val imagePicker: ImagePicker,
    private val phoneAuth: PhoneAuthGateway,
) : ViewModel() {
    private val _state = MutableStateFlow(MoreUiState(spend = SpendSummary(lastDaysRange())))
    val state: StateFlow<MoreUiState> = _state.asStateFlow()

    private var otpSession: PhoneOtpSession? = null
    private var countdownJob: Job? = null
    private var photoJob: Job? = null

    init {
        viewModelScope.launch {
            preferences.getCachedCustomerProfile()?.let { cached -> _state.update { it.copy(profile = cached) } }
        }
        viewModelScope.launch { preferences.observeShopFilter().collect { f -> _state.update { it.copy(filter = f) } } }
        viewModelScope.launch {
            val min = api.minSearchRadiusKm()
            _state.update { it.copy(minRadiusKm = min) }
        }
    }

    fun dismissError() { _state.update { it.copy(error = null) } }

    /** Called each time the tab is shown so the card and the pending-deletion state stay fresh. */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            runCatching { api.me() }
                .onSuccess { profile ->
                    preferences.setCachedCustomerProfile(profile)
                    _state.update { it.copy(profile = profile, loading = false) }
                }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { it.copy(loading = false, error = err.toApiUiText()) }
                }
        }
        loadSpend()
    }

    private fun loadSpend() {
        val range = lastDaysRange()
        _state.update { it.copy(spend = it.spend.copy(range = range, loading = true)) }
        viewModelScope.launch {
            val tz = TimeZone.currentSystemDefault()
            runCatching { api.mySpend(range.fromMs(tz), range.toMs(tz)) }
                .onSuccess { report ->
                    _state.update {
                        it.copy(spend = SpendSummary(range, report.orders.sumOf { o -> o.total }, report.orders.size, false))
                    }
                }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { it.copy(spend = it.spend.copy(loading = false)) }
                }
        }
    }

    fun changePhoto(source: ImageSource) {
        photoJob?.cancel()
        photoJob = viewModelScope.launch {
            val picked = imagePicker.pickImageCatching(source).getOrElse {
                _state.update { it.copy(error = UiText.res(Res.string.error_photo_pick_failed)) }
                return@launch
            } ?: return@launch
            val compressed = runCatching { prepareImageForUpload(picked) }.getOrElse {
                _state.update { it.copy(error = UiText.res(Res.string.error_photo_pick_failed)) }
                return@launch
            }
            _state.update { it.copy(photoUploading = true, photoPreview = compressed.previewBytes) }
            runCatching {
                val url = api.uploadImage(
                    prefix = ImageUploadFeature.USER_PROFILE_PHOTO.prefix,
                    fileName = compressed.displayName,
                    bytes = compressed.bytes,
                    contentType = compressed.mimeType,
                    thumbnailBytes = compressed.thumbnailBytes,
                ).url
                api.updateProfilePhoto(url)
            }.onSuccess { profile ->
                onProfileUpdated(profile)
                _state.update { it.copy(photoUploading = false) }
            }.onFailure { err ->
                err.rethrowIfStructuredCancellation()
                _state.update {
                    it.copy(photoUploading = false, photoPreview = null, error = UiText.res(Res.string.error_image_upload_failed))
                }
            }
        }
    }

    fun saveName(name: String, onDone: () -> Unit) {
        val profile = _state.value.profile ?: return
        val trimmed = name.trim()
        if (trimmed.length < 2) {
            _state.update { it.copy(error = UiText.res(Res.string.error_profile_required)) }
            return
        }
        if (_state.value.nameSaving) return
        _state.update { it.copy(nameSaving = true) }
        viewModelScope.launch {
            runCatching {
                api.updateProfile(
                    name = trimmed,
                    address = profile.address,
                    locality = profile.locality,
                    lat = profile.location?.lat,
                    lng = profile.location?.lng,
                )
            }.onSuccess {
                onProfileUpdated(it)
                _state.update { s -> s.copy(nameSaving = false) }
                onDone()
            }.onFailure { err ->
                err.rethrowIfStructuredCancellation()
                _state.update { it.copy(nameSaving = false, error = err.toApiUiText()) }
            }
        }
    }

    /** The chosen address becomes the primary one for checkout and shop search. */
    fun selectAddress(addressId: String) {
        val current = _state.value.profile ?: return
        if (current.addresses.firstOrNull { it.isDefault }?.id == addressId || _state.value.addressBusy) return
        _state.update { it.copy(addressBusy = true) }
        viewModelScope.launch {
            runCatching { api.selectAddress(addressId) }
                .onSuccess {
                    onProfileUpdated(it)
                    _state.update { s -> s.copy(addressBusy = false) }
                }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { it.copy(addressBusy = false, error = err.toApiUiText()) }
                }
        }
    }

    fun applyFilter(filter: ShopFilter) {
        viewModelScope.launch { preferences.setShopFilter(filter) }
    }

    // ---------- change phone ----------

    fun openPhoneChange() { _state.update { it.copy(phoneChange = PhoneChangeUi()) } }

    fun closePhoneChange() {
        countdownJob?.cancel()
        otpSession = null
        _state.update { it.copy(phoneChange = null) }
    }

    fun setNewPhone(value: String) = updatePhone { it.copy(phone = value.filter(Char::isDigit).take(AppConstants.PHONE_LOCAL_DIGITS), error = null) }

    fun setPhoneOtp(value: String) = updatePhone { it.copy(otp = sanitizeOtpInput(value), error = null) }

    fun backToPhoneEntry() = updatePhone { it.copy(step = PhoneChangeStep.ENTER_PHONE, otp = "", error = null) }

    fun sendPhoneOtp() {
        val ui = _state.value.phoneChange ?: return
        if (ui.loading || (ui.resendInSec > 0 && otpSession != null)) return
        val digits = digitsPhone(ui.phone)
        if (digits.length != AppConstants.PHONE_LOCAL_DIGITS) {
            updatePhone { it.copy(error = UiText.res(Res.string.error_phone_invalid)) }
            return
        }
        if (digits == _state.value.profile?.phone?.let(::digitsPhone)) {
            updatePhone { it.copy(error = UiText.res(Res.string.more_phone_same)) }
            return
        }
        updatePhone { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            phoneAuth.sendOtp(digits)
                .onSuccess { session ->
                    otpSession = session
                    updatePhone {
                        it.copy(
                            step = PhoneChangeStep.ENTER_OTP,
                            loading = false,
                            otp = "",
                            hint = if (session.devOtp != null) {
                                UiText.res(Res.string.dev_otp_hint)
                            } else {
                                UiText.res(Res.string.otp_sent_to, session.maskedPhone.ifBlank { digits })
                            },
                        )
                    }
                    startCountdown(session.resendAfterSec)
                }
                .onFailure { err ->
                    updatePhone { it.copy(loading = false, error = mapPhoneAuthError(err, verifying = false)) }
                    err.otpRetryAfterSec()?.let(::startCountdown)
                }
        }
    }

    fun verifyPhoneOtp() {
        val ui = _state.value.phoneChange ?: return
        val session = otpSession ?: return
        if (ui.loading || ui.otp.length != AppConstants.OTP_LENGTH) return
        updatePhone { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { api.changePhone(digitsPhone(ui.phone), ui.otp, session.sessionId) }
                .onSuccess { profile ->
                    onProfileUpdated(profile)
                    closePhoneChange()
                }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    updatePhone { it.copy(loading = false, error = mapPhoneAuthError(err, verifying = true) ?: err.toApiUiText()) }
                }
        }
    }

    private fun startCountdown(seconds: Int) {
        countdownJob = otpCountdown(countdownJob, seconds) { left -> updatePhone { it.copy(resendInSec = left) } }
    }

    private inline fun updatePhone(crossinline block: (PhoneChangeUi) -> PhoneChangeUi) {
        _state.update { s -> s.phoneChange?.let { s.copy(phoneChange = block(it)) } ?: s }
    }

    // ---------- account deletion ----------

    fun requestDeletion(onDone: () -> Unit) = runDeletion(onDone) { api.requestAccountDeletion() }

    fun cancelDeletion() = runDeletion(onDone = {}) {
        api.cancelAccountDeletion().also { _state.update { s -> s.copy(showWelcomeBack = true) } }
    }

    fun dismissWelcomeBack() { _state.update { it.copy(showWelcomeBack = false) } }

    private fun runDeletion(onDone: () -> Unit, call: suspend () -> CustomerProfile) {
        if (_state.value.deletionBusy) return
        _state.update { it.copy(deletionBusy = true) }
        viewModelScope.launch {
            runCatching { call() }
                .onSuccess { profile ->
                    onProfileUpdated(profile)
                    _state.update { it.copy(deletionBusy = false) }
                    onDone()
                }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { it.copy(deletionBusy = false, error = err.toApiUiText()) }
                }
        }
    }

    private suspend fun onProfileUpdated(profile: CustomerProfile) {
        preferences.setCachedCustomerProfile(profile)
        _state.update { it.copy(profile = profile) }
    }

    private fun lastDaysRange(): ReportRange {
        val today = Instant.fromEpochMilliseconds(AppClock.nowMillis()).toLocalDateTime(TimeZone.currentSystemDefault()).date
        return ReportRange(
            start = today.minus(AppConstants.SPEND_SUMMARY_DAYS - 1, DateTimeUnit.DAY),
            endExclusive = today.plus(1, DateTimeUnit.DAY),
        )
    }
}
