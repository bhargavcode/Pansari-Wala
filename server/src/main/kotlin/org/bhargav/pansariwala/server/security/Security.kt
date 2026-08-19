package org.bhargav.pansariwala.server.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.basicAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.server.ServerConfig
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.util.Date
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

class Security(
    private val config: ServerConfig,
) {
    private val algorithm = Algorithm.HMAC256(config.jwtSecret)
    private val random = SecureRandom()
    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(json)
        }
    }

    fun hashPassword(raw: String): String = sha256(config.passwordSalt + raw)

    fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun issueJwt(
        subject: String,
        role: String,
        shopId: String? = null,
        displayName: String? = null,
        ttlSeconds: Long = 60L * 60L * 24L * 7L,
    ): String = JWT.create()
        .withIssuer(config.jwtIssuer)
        .withSubject(subject)
        .withClaim("role", role)
        .withClaim("shopId", shopId)
        .withClaim("name", displayName)
        .withExpiresAt(Date.from(Instant.now().plusSeconds(ttlSeconds)))
        .sign(algorithm)

    fun verifier() = JWT.require(algorithm).withIssuer(config.jwtIssuer).build()

    fun randomOtp(): String = if (config.devAuth) "123456" else (100000 + random.nextInt(900000)).toString()

    fun deliveryOtp(): String = (1000 + random.nextInt(9000)).toString()

    fun randomId(prefix: String): String = prefix + "_" + sha256(System.nanoTime().toString() + random.nextLong()).take(12)

    fun hmacSha256Hex(secret: String, payload: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(secret.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        return mac.doFinal(payload.toByteArray(StandardCharsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    fun signaturesEqual(expected: String, actual: String): Boolean {
        val a = expected.lowercase().toByteArray(StandardCharsets.UTF_8)
        val b = actual.lowercase().toByteArray(StandardCharsets.UTF_8)
        return MessageDigest.isEqual(a, b)
    }

    fun verifyRazorpaySignature(orderId: String, paymentId: String, signature: String): Boolean {
        if (!config.paymentsEnabled) {
            return config.devAuth && (signature == "dev" || signature.isNotBlank())
        }
        val expected = hmacSha256Hex(config.razorpayKeySecret, "$orderId|$paymentId")
        return signaturesEqual(expected, signature)
    }

    suspend fun createRazorpayOrder(
        amountPaise: Long,
        receipt: String,
        notes: Map<String, String> = emptyMap(),
    ): String {
        if (!config.paymentsEnabled) {
            return "order_dev_$receipt"
        }
        val response = http.post("https://api.razorpay.com/v1/orders") {
            basicAuth(config.razorpayKeyId, config.razorpayKeySecret)
            contentType(ContentType.Application.Json)
            setBody(
                RazorpayCreateBody(
                    amount = amountPaise,
                    currency = "INR",
                    receipt = receipt,
                    payment_capture = 1,
                    notes = notes.takeIf { it.isNotEmpty() },
                ),
            )
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            error("Razorpay could not create order (${response.status.value}): $text")
        }
        return json.decodeFromString<RazorpayCreateResult>(text).id
    }

    suspend fun transferToShopUpi(
        shopName: String,
        upiId: String,
        amountPaise: Long,
        paymentId: String,
    ): String? {
        if (!config.paymentsEnabled || upiId.isBlank() || amountPaise <= 0) return null
        val accountNumber = config.razorpayXAccountNumber
        if (accountNumber.isBlank()) return null
        val response = http.post("https://api.razorpay.com/v1/payouts") {
            basicAuth(config.razorpayKeyId, config.razorpayKeySecret)
            contentType(ContentType.Application.Json)
            setBody(
                RazorpayPayoutBody(
                    account_number = accountNumber,
                    amount = amountPaise,
                    fund_account = RazorpayPayoutFundAccount(
                        vpa = RazorpayVpa(upiId),
                        contact = RazorpayPayoutContact(name = shopName.take(50)),
                    ),
                    notes = mapOf(
                        "payment_id" to paymentId,
                        "shop_upi" to upiId,
                    ),
                ),
            )
        }
        if (!response.status.isSuccess()) return null
        return runCatching { response.body<RazorpayPayoutResult>().id }.getOrNull()
    }

    suspend fun refundRazorpayPayment(
        paymentId: String,
        amountPaise: Long,
        notes: Map<String, String> = emptyMap(),
    ): String? {
        if (amountPaise <= 0 || paymentId.isBlank()) return null
        if (!config.paymentsEnabled || paymentId.startsWith("pay_dev")) return "rfnd_dev"
        val response = http.post("https://api.razorpay.com/v1/payments/$paymentId/refund") {
            basicAuth(config.razorpayKeyId, config.razorpayKeySecret)
            contentType(ContentType.Application.Json)
            setBody(
                RazorpayRefundBody(
                    amount = amountPaise,
                    notes = notes.takeIf { it.isNotEmpty() },
                ),
            )
        }
        val text = response.bodyAsText()
        if (!response.status.isSuccess()) {
            error("Razorpay refund failed (${response.status.value}): $text")
        }
        return json.decodeFromString<RazorpayRefundResult>(text).id
    }

    fun verifyFirebaseOrDev(idToken: String): String {
        if (idToken.startsWith("dev:")) {
            require(config.devAuth) { "Dev tokens disabled" }
            return Security.normalizePhone(idToken.removePrefix("dev:"))
        }
        if (config.firebaseProjectId.isBlank()) {
            error("FIREBASE_PROJECT_ID is not configured")
        }
        val decoded = JWT.decode(idToken)
        val aud = decoded.audience?.firstOrNull()
        val iss = decoded.issuer
        require(aud == config.firebaseProjectId) { "Invalid Firebase audience" }
        require(iss == "https://securetoken.google.com/${config.firebaseProjectId}") { "Invalid Firebase issuer" }
        val phone = decoded.getClaim("phone_number").asString()
            ?: decoded.getClaim("phone").asString()
            ?: error("Firebase token missing phone_number")
        return Security.normalizePhone(phone)
    }

    companion object {
        val vehicleRegRegex = Regex("^[A-Z]{2}[0-9]{1,2}[A-Z]{1,3}[0-9]{4}$")
        fun normalizeReg(value: String): String = value.uppercase().replace(" ", "").replace("-", "")
        fun normalizePhone(phone: String): String {
            val digits = phone.filter { it.isDigit() }
            return if (digits.length >= 10) digits.takeLast(10) else digits
        }
    }
}

@Serializable
private data class RazorpayCreateBody(
    val amount: Long,
    val currency: String,
    val receipt: String,
    val payment_capture: Int,
    val notes: Map<String, String>? = null,
)

@Serializable
private data class RazorpayCreateResult(val id: String)

@Serializable
private data class RazorpayPayoutBody(
    val account_number: String,
    val amount: Long,
    val currency: String = "INR",
    val mode: String = "UPI",
    val purpose: String = "payout",
    val fund_account: RazorpayPayoutFundAccount,
    val notes: Map<String, String>? = null,
    val queue_if_low_balance: Boolean = true,
)

@Serializable
private data class RazorpayPayoutFundAccount(
    val account_type: String = "vpa",
    val vpa: RazorpayVpa,
    val contact: RazorpayPayoutContact,
)

@Serializable
private data class RazorpayVpa(val address: String)

@Serializable
private data class RazorpayPayoutContact(
    val name: String,
    val type: String = "vendor",
)

@Serializable
private data class RazorpayPayoutResult(val id: String? = null)

@Serializable
private data class RazorpayRefundBody(
    val amount: Long,
    val notes: Map<String, String>? = null,
)

@Serializable
private data class RazorpayRefundResult(val id: String? = null)
