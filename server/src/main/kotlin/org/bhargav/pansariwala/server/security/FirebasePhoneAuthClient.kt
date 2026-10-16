package org.bhargav.pansariwala.server.security

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.bhargav.pansariwala.server.ServerConfig
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

/** Proof that the OTP request comes from a genuine app; Firebase refuses to send SMS without one. */
data class OtpAppProof(
    val playIntegrityToken: String? = null,
    val recaptchaToken: String? = null,
    val androidPackage: String? = null,
    val androidCertSha1: String? = null,
)

/** Firebase rejected the call; [reason] is the Identity Toolkit error code (e.g. `INVALID_CODE`). */
class FirebasePhoneAuthException(val reason: String, val httpStatus: Int) : RuntimeException(reason)

/** Server side of Firebase phone auth via the Identity Toolkit REST API (send + verify). */
class FirebasePhoneAuthClient(private val config: ServerConfig) {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(TIMEOUT_MS)).build()
    private val json = Json { ignoreUnknownKeys = true }
    private val siteKeyLock = Any()
    private var cachedSiteKey: Pair<String, Long>? = null

    val isConfigured: Boolean get() = config.firebaseApiKey.isNotBlank()

    /** @return Firebase `sessionInfo` to pass to [signIn]. */
    fun sendCode(e164Phone: String, proof: OtpAppProof): String {
        val body = buildJsonObject {
            put("phoneNumber", e164Phone)
            proof.playIntegrityToken?.takeIf { it.isNotBlank() }?.let { put("playIntegrityToken", it) }
            proof.recaptchaToken?.takeIf { it.isNotBlank() }?.let { put("recaptchaToken", it) }
        }
        val headers = buildMap {
            proof.androidPackage?.takeIf { it.isNotBlank() }?.let { put("X-Android-Package", it) }
            proof.androidCertSha1?.takeIf { it.isNotBlank() }?.let { put("X-Android-Cert", it.replace(":", "").uppercase()) }
        }
        val res = post("accounts:sendVerificationCode", body, headers)
        return res["sessionInfo"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            ?: throw FirebasePhoneAuthException("NO_SESSION_INFO", 502)
    }

    /** @return the verified phone number in E.164. */
    fun signIn(sessionInfo: String, code: String): String {
        val body = buildJsonObject {
            put("sessionInfo", sessionInfo)
            put("code", code)
        }
        val res = post("accounts:signInWithPhoneNumber", body, emptyMap())
        return res["phoneNumber"]?.jsonPrimitive?.content?.takeIf { it.isNotBlank() }
            ?: throw FirebasePhoneAuthException("NO_PHONE_NUMBER", 502)
    }

    /** Firebase's reCAPTCHA v2 site key for iOS/web app verification (cached). */
    fun recaptchaSiteKey(): String {
        synchronized(siteKeyLock) {
            cachedSiteKey?.takeIf { System.currentTimeMillis() - it.second < SITE_KEY_TTL_MS }?.let { return it.first }
        }
        val request = HttpRequest.newBuilder(URI.create("$BASE/recaptchaParams?key=${config.firebaseApiKey}"))
            .timeout(Duration.ofMillis(TIMEOUT_MS))
            .GET()
            .build()
        val res = parse(http.send(request, HttpResponse.BodyHandlers.ofString()))
        val key = res["recaptchaSiteKey"]?.jsonPrimitive?.content.orEmpty()
        if (key.isBlank()) throw FirebasePhoneAuthException("NO_RECAPTCHA_SITE_KEY", 502)
        synchronized(siteKeyLock) { cachedSiteKey = key to System.currentTimeMillis() }
        return key
    }

    private fun post(method: String, body: JsonObject, headers: Map<String, String>): JsonObject {
        val builder = HttpRequest.newBuilder(URI.create("$BASE/$method?key=${config.firebaseApiKey}"))
            .timeout(Duration.ofMillis(TIMEOUT_MS))
            .header("Content-Type", "application/json")
        headers.forEach { (k, v) -> builder.header(k, v) }
        return parse(http.send(builder.POST(HttpRequest.BodyPublishers.ofString(body.toString())).build(), HttpResponse.BodyHandlers.ofString()))
    }

    private fun parse(response: HttpResponse<String>): JsonObject {
        val obj = runCatching { json.parseToJsonElement(response.body()).jsonObject }.getOrNull() ?: JsonObject(emptyMap())
        if (response.statusCode() in 200..299) return obj
        val message = obj["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content.orEmpty()
        // Identity Toolkit sends e.g. "INVALID_PHONE_NUMBER : TOO_SHORT"; the code is the part before " : ".
        val reason = message.substringBefore(" ").substringBefore(":").trim().ifBlank { "HTTP_${response.statusCode()}" }
        throw FirebasePhoneAuthException(reason, response.statusCode())
    }

    companion object {
        private const val BASE = "https://identitytoolkit.googleapis.com/v1"
        private const val TIMEOUT_MS = 10_000L
        private const val SITE_KEY_TTL_MS = 6 * 3_600_000L
    }
}
