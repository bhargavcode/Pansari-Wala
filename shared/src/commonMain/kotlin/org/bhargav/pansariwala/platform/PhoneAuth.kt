package org.bhargav.pansariwala.platform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.ApiException
import org.bhargav.pansariwala.api.OtpConfigDto
import org.bhargav.pansariwala.api.OtpRequest
import org.bhargav.pansariwala.api.OtpSessionResponse
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.TokenResponse
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.api.userFacingServerMessage
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.util.AppConstants
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_otp_app_verification
import pansariwala.shared.generated.resources.error_otp_cooldown
import pansariwala.shared.generated.resources.error_otp_delivery_failed
import pansariwala.shared.generated.resources.error_otp_expired
import pansariwala.shared.generated.resources.error_otp_invalid
import pansariwala.shared.generated.resources.error_otp_not_found
import pansariwala.shared.generated.resources.error_otp_quota
import pansariwala.shared.generated.resources.error_otp_send_failed
import pansariwala.shared.generated.resources.error_otp_too_many_attempts
import pansariwala.shared.generated.resources.error_otp_unavailable
import pansariwala.shared.generated.resources.error_otp_verify_failed
import pansariwala.shared.generated.resources.error_phone_invalid

/** Server-side OTP for every flavour: the API generates, sends (SMS failover) and verifies the code. */
interface PhoneAuthGateway {
    suspend fun sendOtp(phone: String): Result<PhoneOtpSession>
    suspend fun verifyOtp(phone: String, otp: String, session: PhoneOtpSession): Result<TokenResponse>
}

data class PhoneOtpSession(
    val sessionId: String,
    val devOtp: String? = null,
    val resendAfterSec: Int = AppConstants.OTP_DEFAULT_RESEND_SEC,
    val maskedPhone: String = "",
    val expiresInSec: Int = AppConstants.OTP_DEFAULT_TTL_SEC,
)

fun OtpSessionResponse.toPhoneOtpSession() = PhoneOtpSession(
    sessionId = sessionId,
    devOtp = devOtp,
    expiresInSec = expiresInSec,
    resendAfterSec = resendAfterSec,
    maskedPhone = maskedPhone,
)

/** App proof Firebase needs before the server may send an SMS (Play Integrity on Android, reCAPTCHA elsewhere). */
data class OtpAppProof(
    val playIntegrityToken: String? = null,
    val recaptchaToken: String? = null,
    val androidPackage: String? = null,
    val androidCertSha1: String? = null,
)

interface OtpAppVerifier {
    /** [forceRecaptcha] skips Play Integrity, used after Firebase rejected an integrity token. */
    suspend fun proof(e164Phone: String, config: OtpConfigDto, forceRecaptcha: Boolean = false): OtpAppProof
}

/** User closed the verification challenge; the OTP request is dropped without an error banner. */
class OtpVerificationCancelledException : Exception("App verification cancelled")

class OtpAppVerificationException(message: String, cause: Throwable? = null) : Exception(message, cause)

class ServerPhoneAuthGateway(
    private val api: PansariApi,
    private val verifier: OtpAppVerifier,
) : PhoneAuthGateway {
    /** Set once Firebase rejects this install's Play Integrity token; later sends go straight to reCAPTCHA. */
    private var integrityRejected = false

    override suspend fun sendOtp(phone: String): Result<PhoneOtpSession> = runCatching {
        val digits = digitsPhone(phone)
        val masked = maskPhoneForStatus(digits)
        OtpStatusCenter.post(OtpStatus.Sending(masked))
        val config = api.otpConfig()
        if (config.mode != AppConstants.OTP_MODE_FIREBASE) return@runCatching request(digits, OtpAppProof())
        OtpStatusCenter.post(OtpStatus.VerifyingHuman(masked))
        val proof = verifier.proof(e164Phone(digits), config, forceRecaptcha = integrityRejected)
        OtpStatusCenter.post(OtpStatus.Sending(masked))
        try {
            request(digits, proof)
        } catch (e: ApiException) {
            if (proof.playIntegrityToken == null || e.code !in INTEGRITY_RETRY_CODES) throw e
            integrityRejected = true
            OtpStatusCenter.post(OtpStatus.VerifyingHuman(masked))
            val retryProof = verifier.proof(e164Phone(digits), config, forceRecaptcha = true)
            OtpStatusCenter.post(OtpStatus.Sending(masked))
            request(digits, retryProof)
        }
    }.onSuccess { session ->
        val shown = session.maskedPhone.ifBlank { maskPhoneForStatus(digitsPhone(phone)) }
        OtpStatusCenter.post(OtpStatus.Sent(shown, ((session.expiresInSec + SECONDS_PER_MIN - 1) / SECONDS_PER_MIN).coerceAtLeast(1)))
    }.onFailure {
        it.rethrowIfStructuredCancellation()
        OtpStatusCenter.post(
            if (it is OtpVerificationCancelledException) OtpStatus.Cancelled
            else OtpStatus.SendFailed(mapPhoneAuthError(it, verifying = false)),
        )
    }

    private companion object {
        /** Older servers report Firebase's HTTP 500 for a bad integrity token as a delivery failure. */
        val INTEGRITY_RETRY_CODES = setOf(OtpErrorCodes.APP_VERIFICATION_FAILED, OtpErrorCodes.DELIVERY_FAILED)
        const val SECONDS_PER_MIN = 60
    }

    private suspend fun request(digits: String, proof: OtpAppProof): PhoneOtpSession =
        api.requestOtp(
            OtpRequest(
                phone = digits,
                playIntegrityToken = proof.playIntegrityToken,
                recaptchaToken = proof.recaptchaToken,
                androidPackage = proof.androidPackage,
                androidCertSha1 = proof.androidCertSha1,
            ),
        ).toPhoneOtpSession()

    override suspend fun verifyOtp(
        phone: String,
        otp: String,
        session: PhoneOtpSession,
    ): Result<TokenResponse> = runCatching {
        OtpStatusCenter.post(OtpStatus.Checking)
        api.verifyOtp(digitsPhone(phone), otp.filter { it.isDigit() }, session.sessionId)
    }.onSuccess { OtpStatusCenter.post(OtpStatus.Verified) }
        .onFailure {
            it.rethrowIfStructuredCancellation()
            OtpStatusCenter.post(OtpStatus.VerifyFailed(mapPhoneAuthError(it, verifying = true)))
        }
}

/** Digits only, capped at [AppConstants.OTP_LENGTH]. */
fun sanitizeOtpInput(value: String): String = value.filter { it.isDigit() }.take(AppConstants.OTP_LENGTH)

/** Seconds the server asked the client to wait (cooldown / rate limit), if any. */
fun Throwable.otpRetryAfterSec(): Int? = (this as? ApiException)?.retryAfterSec

/** Ticks [onTick] from [seconds] down to 0 once per second, replacing [previous]. */
fun ViewModel.otpCountdown(previous: Job?, seconds: Int, onTick: (Int) -> Unit): Job {
    previous?.cancel()
    return viewModelScope.launch {
        for (left in seconds downTo 0) {
            onTick(left)
            if (left > 0) delay(AppConstants.OTP_COUNTDOWN_TICK_MS)
        }
    }
}

fun digitsPhone(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return if (digits.length >= AppConstants.PHONE_LOCAL_DIGITS) {
        digits.takeLast(AppConstants.PHONE_LOCAL_DIGITS)
    } else {
        digits
    }
}

fun e164Phone(phone: String): String {
    val trimmed = phone.trim()
    if (trimmed.startsWith("+")) {
        return "+" + trimmed.drop(1).filter { it.isDigit() }
    }
    return AppConstants.DEFAULT_PHONE_COUNTRY_CODE + digitsPhone(trimmed)
}

/** Null when the user cancelled the verification challenge (nothing to report). */
fun mapPhoneAuthError(error: Throwable, verifying: Boolean): UiText? {
    if (error is OtpVerificationCancelledException) return null
    if (error is OtpAppVerificationException) return UiText.res(Res.string.error_otp_app_verification)
    val api = error as? ApiException
    when (api?.code) {
        OtpErrorCodes.APP_VERIFICATION_FAILED -> return UiText.res(Res.string.error_otp_app_verification)
        OtpErrorCodes.INVALID_PHONE -> return UiText.res(Res.string.error_phone_invalid)
        OtpErrorCodes.RESEND_COOLDOWN ->
            return UiText.res(Res.string.error_otp_cooldown, api.retryAfterSec ?: AppConstants.OTP_DEFAULT_RESEND_SEC)
        OtpErrorCodes.RATE_LIMITED -> return UiText.res(Res.string.error_otp_quota)
        OtpErrorCodes.SMS_UNAVAILABLE -> return UiText.res(Res.string.error_otp_unavailable)
        OtpErrorCodes.DELIVERY_FAILED -> return UiText.res(Res.string.error_otp_delivery_failed)
        OtpErrorCodes.NOT_FOUND -> return UiText.res(Res.string.error_otp_not_found)
        OtpErrorCodes.EXPIRED -> return UiText.res(Res.string.error_otp_expired)
        OtpErrorCodes.TOO_MANY_ATTEMPTS -> return UiText.res(Res.string.error_otp_too_many_attempts)
        OtpErrorCodes.INVALID -> return UiText.res(Res.string.error_otp_invalid)
    }
    error.userFacingServerMessage()?.let { return UiText.Plain(it) }
    if (api == null) return error.toApiUiText()
    return if (verifying) UiText.res(Res.string.error_otp_verify_failed) else UiText.res(Res.string.error_otp_send_failed)
}

/** Mirrors the server's `OtpErrorCode` values sent in `ApiErrorBody.code`. */
object OtpErrorCodes {
    const val INVALID_PHONE = "OTP_INVALID_PHONE"
    const val RESEND_COOLDOWN = "OTP_RESEND_COOLDOWN"
    const val RATE_LIMITED = "OTP_RATE_LIMITED"
    const val SMS_UNAVAILABLE = "OTP_SMS_UNAVAILABLE"
    const val DELIVERY_FAILED = "OTP_DELIVERY_FAILED"
    const val NOT_FOUND = "OTP_NOT_FOUND"
    const val EXPIRED = "OTP_EXPIRED"
    const val INVALID = "OTP_INVALID"
    const val TOO_MANY_ATTEMPTS = "OTP_TOO_MANY_ATTEMPTS"
    const val APP_VERIFICATION_FAILED = "OTP_APP_VERIFICATION_FAILED"
}
