package org.bhargav.pansariwala.platform

import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.util.AppConstants
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_otp_expired
import pansariwala.shared.generated.resources.error_otp_invalid
import pansariwala.shared.generated.resources.error_otp_quota
import pansariwala.shared.generated.resources.error_otp_send_failed
import pansariwala.shared.generated.resources.error_otp_verify_failed
import pansariwala.shared.generated.resources.error_phone_invalid

interface PhoneAuthGateway {
    suspend fun sendOtp(phone: String): Result<PhoneOtpSession>
    suspend fun verifyOtp(phone: String, otp: String, session: PhoneOtpSession): Result<PhoneAuthResult>
}

data class PhoneOtpSession(val sessionId: String, val usesFirebase: Boolean)
data class PhoneAuthResult(val firebaseIdToken: String?, val verifiedPhone: String)

class ServerPhoneAuthGateway(
    private val api: PansariApi,
) : PhoneAuthGateway {
    override suspend fun sendOtp(phone: String): Result<PhoneOtpSession> = runCatching {
        PhoneOtpSession(sessionId = api.requestOtp(digitsPhone(phone)), usesFirebase = false)
    }

    override suspend fun verifyOtp(
        phone: String,
        otp: String,
        session: PhoneOtpSession,
    ): Result<PhoneAuthResult> = Result.success(
        PhoneAuthResult(firebaseIdToken = null, verifiedPhone = digitsPhone(phone)),
    )
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

fun mapPhoneAuthError(error: Throwable, verifying: Boolean): UiText {
    val msg = error.message.orEmpty()
    return when {
        containsAny(msg, "invalid-phone", "ERROR_INVALID_PHONE_NUMBER", "Invalid format") ->
            UiText.res(Res.string.error_phone_invalid)
        containsAny(msg, "invalid-verification-code", "ERROR_INVALID_VERIFICATION_CODE", "Invalid OTP") ->
            UiText.res(Res.string.error_otp_invalid)
        containsAny(msg, "session-expired", "invalid-verification-id", "ERROR_SESSION_EXPIRED", "OTP expired") ->
            UiText.res(Res.string.error_otp_expired)
        containsAny(msg, "quota", "too-many-requests", "ERROR_TOO_MANY_REQUESTS") ->
            UiText.res(Res.string.error_otp_quota)
        verifying -> error.message?.let { UiText.Plain(it) } ?: UiText.res(Res.string.error_otp_verify_failed)
        else -> error.message?.let { UiText.Plain(it) } ?: UiText.res(Res.string.error_otp_send_failed)
    }
}

private fun containsAny(haystack: String, vararg needles: String): Boolean =
    needles.any { haystack.contains(it, ignoreCase = true) }
