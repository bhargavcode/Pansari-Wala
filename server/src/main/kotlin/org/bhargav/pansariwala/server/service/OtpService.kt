package org.bhargav.pansariwala.server.service

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Updates.inc
import com.mongodb.kotlin.client.MongoCollection
import org.bhargav.pansariwala.server.ServerConfig
import org.bhargav.pansariwala.server.db.OtpDoc
import org.bhargav.pansariwala.server.dto.OtpConfigResponse
import org.bhargav.pansariwala.server.dto.OtpSessionResponse
import org.bhargav.pansariwala.server.security.FirebasePhoneAuthClient
import org.bhargav.pansariwala.server.security.FirebasePhoneAuthException
import org.bhargav.pansariwala.server.security.OtpAppProof
import org.bhargav.pansariwala.server.security.Security
import java.util.concurrent.ConcurrentHashMap

/** OTP failure with an HTTP status and a stable [code] clients map to localized text. */
class OtpException(
    val status: Int,
    val code: String,
    message: String,
    val retryAfterSec: Int? = null,
) : RuntimeException(message)

object OtpErrorCode {
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

/**
 * Phone OTP owned by the server. Production sends and checks the code through Firebase Phone Auth
 * (Identity Toolkit); `AUTH_DEV_MODE=true` uses a fixed code with no SMS.
 */
class OtpService(
    private val config: ServerConfig,
    private val security: Security,
    private val col: MongoCollection<OtpDoc>,
) {
    private val ipHits = ConcurrentHashMap<String, ArrayDeque<Long>>()
    private val firebase = FirebasePhoneAuthClient(config)

    init {
        val mode = when {
            config.devAuth -> "dev (fixed $DEV_OTP, no SMS)"
            firebase.isConfigured -> "firebase (project ${config.firebaseProjectId})"
            else -> "UNAVAILABLE — set FIREBASE_API_KEY or AUTH_DEV_MODE=true"
        }
        println("OTP mode: $mode")
    }

    fun config(): OtpConfigResponse = when {
        config.devAuth -> OtpConfigResponse(mode = MODE_DEV)
        else -> OtpConfigResponse(
            mode = MODE_FIREBASE,
            recaptchaSiteKey = runCatching { firebase.recaptchaSiteKey() }
                .onFailure { println("WARN: Firebase reCAPTCHA site key fetch failed: ${it.message}") }
                .getOrDefault(""),
            authDomain = "${config.firebaseProjectId}.firebaseapp.com",
        )
    }

    fun request(rawPhone: String, clientIp: String?, proof: OtpAppProof = OtpAppProof()): OtpSessionResponse {
        val phone = validPhone(rawPhone)
        checkIpLimit(clientIp)
        val now = System.currentTimeMillis()
        val existing = col.find(eq("phone", phone)).toList().maxByOrNull { it.createdAt }
        if (existing != null) {
            val waitMs = existing.createdAt + config.otpResendCooldownSeconds * 1_000L - now
            if (waitMs > 0) {
                throw OtpException(429, OtpErrorCode.RESEND_COOLDOWN, "Please wait before requesting another OTP", secs(waitMs))
            }
        }
        val inWindow = existing != null && now - existing.windowStart < HOUR_MS
        val sendCount = if (inWindow) existing.sendCount + 1 else 1
        val windowStart = if (inWindow) existing.windowStart else now
        if (sendCount > config.otpMaxSendsPerHour) {
            throw OtpException(
                429,
                OtpErrorCode.RATE_LIMITED,
                "Too many OTP requests. Try again later",
                secs(windowStart + HOUR_MS - now),
            )
        }
        val sessionId = security.randomId("otp")
        val firebaseSession = if (config.devAuth) "" else sendViaFirebase(phone, proof)
        val doc = OtpDoc(
            sessionId = sessionId,
            phone = phone,
            codeHash = if (config.devAuth) hash(sessionId, DEV_OTP) else "",
            purpose = "phone",
            expiresAt = now + config.otpTtlSeconds * 1_000L,
            createdAt = now,
            sendCount = sendCount,
            windowStart = windowStart,
            provider = if (config.devAuth) MODE_DEV else MODE_FIREBASE,
            firebaseSession = firebaseSession,
        )
        col.deleteMany(eq("phone", phone))
        col.insertOne(doc)
        return OtpSessionResponse(
            sessionId = sessionId,
            devOtp = DEV_OTP.takeIf { config.devAuth },
            expiresInSec = config.otpTtlSeconds,
            resendAfterSec = config.otpResendCooldownSeconds,
            maskedPhone = mask(phone),
        )
    }

    /** Consumes a valid OTP and returns the normalized phone; throws [OtpException] otherwise. */
    fun verify(rawPhone: String, otp: String, sessionId: String?): String {
        val phone = Security.normalizePhone(rawPhone)
        val filter = if (sessionId.isNullOrBlank()) eq("phone", phone) else and(eq("phone", phone), eq("_id", sessionId))
        val row = col.find(filter).firstOrNull()
            ?: throw OtpException(400, OtpErrorCode.NOT_FOUND, "No active OTP. Please request a new one")
        val now = System.currentTimeMillis()
        if (row.expiresAt < now) {
            col.deleteOne(eq("_id", row.sessionId))
            throw OtpException(400, OtpErrorCode.EXPIRED, "OTP expired. Please request a new one")
        }
        if (row.attempts >= config.otpMaxVerifyAttempts) {
            col.deleteOne(eq("_id", row.sessionId))
            throw OtpException(429, OtpErrorCode.TOO_MANY_ATTEMPTS, "Too many wrong attempts. Please request a new OTP")
        }
        val code = otp.filter { it.isDigit() }
        if (code.length != OTP_LENGTH || !codeMatches(row, code)) {
            col.updateOne(eq("_id", row.sessionId), inc("attempts", 1))
            val left = config.otpMaxVerifyAttempts - row.attempts - 1
            if (left <= 0) {
                col.deleteOne(eq("_id", row.sessionId))
                throw OtpException(429, OtpErrorCode.TOO_MANY_ATTEMPTS, "Too many wrong attempts. Please request a new OTP")
            }
            throw OtpException(400, OtpErrorCode.INVALID, "Invalid OTP. $left attempt(s) left")
        }
        col.deleteOne(eq("_id", row.sessionId))
        return phone
    }

    private fun sendViaFirebase(phone: String, proof: OtpAppProof): String {
        if (!firebase.isConfigured) {
            throw OtpException(503, OtpErrorCode.SMS_UNAVAILABLE, "OTP service is temporarily unavailable")
        }
        if (proof.playIntegrityToken.isNullOrBlank() && proof.recaptchaToken.isNullOrBlank()) {
            throw OtpException(400, OtpErrorCode.APP_VERIFICATION_FAILED, "App verification is required")
        }
        return try {
            firebase.sendCode("+91$phone", proof)
        } catch (e: FirebasePhoneAuthException) {
            println("WARN: Firebase sendVerificationCode failed for ${mask(phone)}: HTTP ${e.httpStatus} ${e.detail}")
            throw e.toOtpException(sending = true)
        } catch (e: java.io.IOException) {
            println("WARN: Firebase unreachable for ${mask(phone)}: ${e.message}")
            throw OtpException(503, OtpErrorCode.DELIVERY_FAILED, "Could not send OTP. Please try again")
        }
    }

    /** Firebase result for real sessions; a wrong code is reported as `false` so attempts are counted. */
    private fun codeMatches(row: OtpDoc, code: String): Boolean {
        if (row.firebaseSession.isBlank()) return security.signaturesEqual(row.codeHash, hash(row.sessionId, code))
        val verified = try {
            firebase.signIn(row.firebaseSession, code)
        } catch (e: FirebasePhoneAuthException) {
            if (e.reason == "INVALID_CODE") return false
            println("WARN: Firebase signInWithPhoneNumber failed for ${mask(row.phone)}: HTTP ${e.httpStatus} ${e.detail}")
            if (e.reason == "SESSION_EXPIRED" || e.reason == "INVALID_SESSION_INFO") col.deleteOne(eq("_id", row.sessionId))
            throw e.toOtpException(sending = false)
        } catch (e: java.io.IOException) {
            throw OtpException(503, OtpErrorCode.DELIVERY_FAILED, "Could not verify OTP. Please try again")
        }
        return Security.normalizePhone(verified) == row.phone
    }

    private fun FirebasePhoneAuthException.toOtpException(sending: Boolean): OtpException = when (reason) {
        "INVALID_PHONE_NUMBER", "MISSING_PHONE_NUMBER" ->
            OtpException(400, OtpErrorCode.INVALID_PHONE, "Enter a valid 10-digit mobile number")
        "TOO_MANY_ATTEMPTS_TRY_LATER", "QUOTA_EXCEEDED" ->
            OtpException(429, OtpErrorCode.RATE_LIMITED, "Too many OTP requests. Try again later", HOUR_SECS)
        "SESSION_EXPIRED", "INVALID_SESSION_INFO", "MISSING_SESSION_INFO" ->
            OtpException(400, OtpErrorCode.EXPIRED, "OTP expired. Please request a new one")
        "MISSING_CLIENT_IDENTIFIER", "INVALID_APP_CREDENTIAL", "MISSING_APP_CREDENTIAL",
        "INVALID_PLAY_INTEGRITY_TOKEN", "MISSING_PLAY_INTEGRITY_TOKEN", "INVALID_RECAPTCHA_TOKEN",
        "MISSING_RECAPTCHA_TOKEN", "CAPTCHA_CHECK_FAILED", "INVALID_CERT_HASH", "MISSING_CERT_HASH",
        "APP_NOT_AUTHORIZED", "MISSING_ANDROID_PACKAGE_NAME" ->
            OtpException(400, OtpErrorCode.APP_VERIFICATION_FAILED, "App verification failed. Please try again")
        "OPERATION_NOT_ALLOWED", "BILLING_NOT_ENABLED", "PROJECT_NOT_FOUND", "API_KEY_INVALID" ->
            OtpException(503, OtpErrorCode.SMS_UNAVAILABLE, "OTP service is temporarily unavailable")
        else -> if (httpStatus == 403) {
            OtpException(503, OtpErrorCode.SMS_UNAVAILABLE, "OTP service is temporarily unavailable")
        } else if (sending) {
            OtpException(503, OtpErrorCode.DELIVERY_FAILED, "Could not send OTP. Please try again")
        } else {
            OtpException(503, OtpErrorCode.DELIVERY_FAILED, "Could not verify OTP. Please try again")
        }
    }

    private fun validPhone(raw: String): String {
        val phone = Security.normalizePhone(raw)
        if (!INDIAN_MOBILE.matches(phone)) {
            throw OtpException(400, OtpErrorCode.INVALID_PHONE, "Enter a valid 10-digit mobile number")
        }
        return phone
    }

    private fun checkIpLimit(ip: String?) {
        val key = ip?.trim()?.takeIf { it.isNotEmpty() } ?: return
        val now = System.currentTimeMillis()
        val hits = ipHits.computeIfAbsent(key) { ArrayDeque() }
        synchronized(hits) {
            while (hits.isNotEmpty() && now - hits.first() > HOUR_MS) hits.removeFirst()
            if (hits.size >= config.otpMaxRequestsPerIpPerHour) {
                throw OtpException(429, OtpErrorCode.RATE_LIMITED, "Too many OTP requests. Try again later", secs(hits.first() + HOUR_MS - now))
            }
            hits.addLast(now)
        }
        if (ipHits.size > MAX_TRACKED_IPS) ipHits.entries.removeIf { (_, q) -> synchronized(q) { q.isEmpty() || now - q.last() > HOUR_MS } }
    }

    private fun hash(sessionId: String, code: String) = security.sha256("${config.jwtSecret}:$sessionId:$code")

    private fun secs(ms: Long) = ((ms + 999) / 1_000).toInt().coerceAtLeast(1)

    private fun mask(phone: String) = if (phone.length >= 4) "******" + phone.takeLast(4) else phone

    companion object {
        const val DEV_OTP = "123456"
        const val MODE_DEV = "dev"
        const val MODE_FIREBASE = "firebase"
        private const val HOUR_SECS = 3_600
        private const val OTP_LENGTH = 6
        private const val HOUR_MS = 3_600_000L
        private const val MAX_TRACKED_IPS = 10_000
        private val INDIAN_MOBILE = Regex("^[6-9]\\d{9}$")
    }
}
