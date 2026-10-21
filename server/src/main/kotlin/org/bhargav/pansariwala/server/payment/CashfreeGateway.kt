package org.bhargav.pansariwala.server.payment

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.server.ServerConfig
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Cashfree PG, API version [API_VERSION]: Orders + "Order Pay" UPI intent (returns a `upi://pay` link
 * the app opens directly), refunds and Easy Split. All calls throw on HTTP errors.
 */
class CashfreeGateway(private val config: ServerConfig) {
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; explicitNulls = false }
    private val http = HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = 8_000
            requestTimeoutMillis = 15_000
            socketTimeoutMillis = 15_000
        }
        install(ContentNegotiation) { json(json) }
    }

    val enabled: Boolean get() = config.cashfreeEnabled

    private val isProduction: Boolean
        get() = config.cashfreeEnv.equals("production", ignoreCase = true)

    private val baseUrl: String
        get() = if (isProduction) PROD_URL else SANDBOX_URL

    private fun HttpRequestBuilder.auth() {
        header("x-client-id", config.cashfreeAppId)
        header("x-client-secret", config.cashfreeSecretKey)
        header("x-api-version", API_VERSION)
    }

    /** Creates the gateway order and asks for a UPI intent; returns the `upi://pay` link. */
    suspend fun createUpiIntent(
        gatewayOrderId: String,
        amountInr: Double,
        customerId: String,
        customerName: String,
        customerPhone: String,
        note: String,
        expiresAtMs: Long,
    ): String {
        require(enabled) { "Cashfree is not configured on the server" }
        val base = config.publicApiBaseUrl.takeIf { it.startsWith("https://") }
        val order: CashfreeOrder = http.post("$baseUrl/pg/orders") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(
                CashfreeOrderRequest(
                    orderId = gatewayOrderId,
                    orderAmount = amountInr.money(),
                    customerDetails = CashfreeCustomer(
                        customerId = customerId.filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(50).ifBlank { "guest" },
                        customerPhone = customerPhone.filter(Char::isDigit).takeLast(10),
                        customerName = customerName.take(100).ifBlank { null },
                    ),
                    orderMeta = CashfreeOrderMeta(
                        notifyUrl = base?.let { "$it/payments/cashfree/webhook" },
                        returnUrl = base?.let { "$it/payments/cashfree/return?order_id={order_id}" },
                    ),
                    orderExpiryTime = ISO.format(Instant.ofEpochMilli(expiresAtMs.coerceAtLeast(System.currentTimeMillis() + MIN_EXPIRY_MS))),
                    orderNote = note.take(200),
                ),
            )
        }.decode("create order")
        val session = order.paymentSessionId ?: error("Cashfree did not return a payment session")
        val pay: CashfreeOrderPay = http.post("$baseUrl/pg/orders/sessions") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(CashfreeOrderPayRequest(session, CashfreePaymentMethod(CashfreeUpi("link"))))
        }.decode("start UPI payment")
        val link = pay.data?.payload?.get("default").orEmpty()
        // Sandbox answers with an https payment-simulator page instead of a real upi:// intent.
        val usable = link.startsWith("upi://") || (!isProduction && link.startsWith("https://"))
        return link.takeIf { usable }
            ?: error("Cashfree did not return a UPI link (enable UPI intent / seamless on the account)")
    }

    /** `ACTIVE`, `PAID`, `EXPIRED` or `TERMINATED`. */
    suspend fun orderStatus(gatewayOrderId: String): String =
        http.get("$baseUrl/pg/orders/$gatewayOrderId") { auth() }.decode<CashfreeOrder>("fetch order").orderStatus

    suspend fun refund(gatewayOrderId: String, amountInr: Double, refundId: String, note: String): String {
        val response = http.post("$baseUrl/pg/orders/$gatewayOrderId/refunds") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(CashfreeRefundRequest(amountInr.money(), refundId.take(40), note.take(100)))
        }
        return response.decode<CashfreeRefund>("refund").refundId ?: refundId
    }

    /** Easy Split after payment: sends each vendor its share of [gatewayOrderId]. */
    suspend fun split(gatewayOrderId: String, shares: Map<String, Double>) {
        require(shares.isNotEmpty()) { "Nothing to split" }
        http.post("$baseUrl/pg/easy-split/orders/$gatewayOrderId/split") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(CashfreeSplitRequest(shares.map { (vendor, amount) -> CashfreeSplit(vendor, amount.money()) }))
        }.decode<Map<String, kotlinx.serialization.json.JsonElement>>("split order")
    }

    /** `x-webhook-signature` = base64(HMAC-SHA256(timestamp + rawBody, secret)). */
    fun verifyWebhook(timestamp: String, rawBody: String, signature: String): Boolean {
        if (!enabled || timestamp.isBlank() || signature.isBlank()) return false
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(config.cashfreeSecretKey.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val expected = Base64.getEncoder().encodeToString(mac.doFinal((timestamp + rawBody).toByteArray(StandardCharsets.UTF_8)))
        return MessageDigest.isEqual(expected.toByteArray(), signature.toByteArray())
    }

    /** Pulls `order_id` out of a webhook body without trusting its status (we re-fetch it). */
    fun webhookOrderId(rawBody: String): String? =
        Regex("\"order_id\"\\s*:\\s*\"([^\"]+)\"").find(rawBody)?.groupValues?.get(1)

    private suspend inline fun <reified T> HttpResponse.decode(action: String): T {
        val text = bodyAsText()
        if (!status.isSuccess()) error("Cashfree could not $action (${status.value}): $text")
        return json.decodeFromString(text)
    }

    private fun Double.money(): Double = Math.round(this * 100) / 100.0

    companion object {
        private const val API_VERSION = "2023-08-01"
        private const val SANDBOX_URL = "https://sandbox.cashfree.com"
        private const val PROD_URL = "https://api.cashfree.com"
        /** Cashfree rejects order expiries closer than ~15 minutes; our own 5-minute timer still applies. */
        private const val MIN_EXPIRY_MS = 16 * 60_000L
        private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("Asia/Kolkata"))
        const val ORDER_PAID = "PAID"
    }
}

@Serializable
private data class CashfreeOrderRequest(
    @SerialName("order_id") val orderId: String,
    @SerialName("order_amount") val orderAmount: Double,
    @SerialName("order_currency") val orderCurrency: String = "INR",
    @SerialName("customer_details") val customerDetails: CashfreeCustomer,
    @SerialName("order_meta") val orderMeta: CashfreeOrderMeta,
    @SerialName("order_expiry_time") val orderExpiryTime: String,
    @SerialName("order_note") val orderNote: String,
)

@Serializable
private data class CashfreeCustomer(
    @SerialName("customer_id") val customerId: String,
    @SerialName("customer_phone") val customerPhone: String,
    @SerialName("customer_name") val customerName: String? = null,
)

@Serializable
private data class CashfreeOrderMeta(
    @SerialName("notify_url") val notifyUrl: String? = null,
    /** Browser checkout (sandbox simulator / web UPI) lands here after success or failure. */
    @SerialName("return_url") val returnUrl: String? = null,
)

@Serializable
private data class CashfreeOrder(
    @SerialName("order_id") val orderId: String = "",
    @SerialName("order_status") val orderStatus: String = "",
    @SerialName("payment_session_id") val paymentSessionId: String? = null,
)

@Serializable
private data class CashfreeOrderPayRequest(
    @SerialName("payment_session_id") val paymentSessionId: String,
    @SerialName("payment_method") val paymentMethod: CashfreePaymentMethod,
)

@Serializable
private data class CashfreePaymentMethod(val upi: CashfreeUpi)

@Serializable
private data class CashfreeUpi(val channel: String)

@Serializable
private data class CashfreeOrderPay(val data: CashfreeOrderPayData? = null)

@Serializable
private data class CashfreeOrderPayData(val payload: Map<String, String>? = null)

@Serializable
private data class CashfreeRefundRequest(
    @SerialName("refund_amount") val refundAmount: Double,
    @SerialName("refund_id") val refundId: String,
    @SerialName("refund_note") val refundNote: String,
)

@Serializable
private data class CashfreeRefund(@SerialName("refund_id") val refundId: String? = null)

@Serializable
private data class CashfreeSplitRequest(
    val split: List<CashfreeSplit>,
    @SerialName("split_type") val splitType: String = "ORDER_AMOUNT",
)

@Serializable
private data class CashfreeSplit(
    @SerialName("vendor_id") val vendorId: String,
    val amount: Double,
)
