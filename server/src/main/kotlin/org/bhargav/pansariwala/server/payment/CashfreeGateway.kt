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

/** Cashfree PG (Payment Links API, version [API_VERSION]). All calls throw on HTTP errors. */
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

    private val baseUrl: String
        get() = if (config.cashfreeEnv.equals("production", ignoreCase = true)) PROD_URL else SANDBOX_URL

    private fun HttpRequestBuilder.auth() {
        header("x-client-id", config.cashfreeAppId)
        header("x-client-secret", config.cashfreeSecretKey)
        header("x-api-version", API_VERSION)
    }

    suspend fun createLink(
        linkId: String,
        amountInr: Double,
        purpose: String,
        customerName: String,
        customerPhone: String,
        expiresAtMs: Long,
    ): CashfreeLink {
        require(enabled) { "Cashfree is not configured on the server" }
        val base = config.publicApiBaseUrl
        val body = CashfreeLinkRequest(
            linkId = linkId,
            linkAmount = Math.round(amountInr * 100) / 100.0,
            linkPurpose = purpose.take(500),
            customerDetails = CashfreeCustomer(
                customerPhone = customerPhone.filter(Char::isDigit).takeLast(10),
                customerName = customerName.take(100).ifBlank { null },
            ),
            linkExpiryTime = ISO.format(Instant.ofEpochMilli(expiresAtMs)),
            linkNotes = mapOf("order_id" to linkId),
            linkMeta = CashfreeLinkMeta(
                notifyUrl = base.takeIf { it.startsWith("https://") }?.let { "$it/payments/cashfree/webhook" },
                returnUrl = base.takeIf { it.startsWith("https://") }?.let { "$it/payments/cashfree/return?order_id=$linkId" },
            ),
        )
        val response = http.post("$baseUrl/pg/links") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        return response.decode("create payment link")
    }

    suspend fun getLink(linkId: String): CashfreeLink =
        http.get("$baseUrl/pg/links/$linkId") { auth() }.decode("fetch payment link")

    suspend fun cancelLink(linkId: String) {
        runCatching { http.post("$baseUrl/pg/links/$linkId/cancel") { auth() } }
    }

    /** Refunds the paid order behind [linkId]; returns Cashfree's refund id. */
    suspend fun refundLink(linkId: String, amountInr: Double, refundId: String, note: String): String {
        val orders: List<CashfreeLinkOrder> =
            http.get("$baseUrl/pg/links/$linkId/orders") { auth() }.decode("fetch link orders")
        val paid = orders.firstOrNull { it.orderStatus == "PAID" } ?: error("No paid Cashfree order to refund")
        val response = http.post("$baseUrl/pg/orders/${paid.orderId}/refunds") {
            auth()
            contentType(ContentType.Application.Json)
            setBody(CashfreeRefundRequest(Math.round(amountInr * 100) / 100.0, refundId, note.take(100)))
        }
        return response.decode<CashfreeRefund>("refund").refundId ?: refundId
    }

    /** `x-webhook-signature` = base64(HMAC-SHA256(timestamp + rawBody, secret)). */
    fun verifyWebhook(timestamp: String, rawBody: String, signature: String): Boolean {
        if (!enabled || timestamp.isBlank() || signature.isBlank()) return false
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(config.cashfreeSecretKey.toByteArray(StandardCharsets.UTF_8), "HmacSHA256"))
        val expected = Base64.getEncoder().encodeToString(mac.doFinal((timestamp + rawBody).toByteArray(StandardCharsets.UTF_8)))
        return MessageDigest.isEqual(expected.toByteArray(), signature.toByteArray())
    }

    /** Pulls `link_id` out of a webhook body without trusting its status (we re-fetch it). */
    fun webhookLinkId(rawBody: String): String? =
        runCatching { json.decodeFromString<CashfreeWebhook>(rawBody).data?.linkId }.getOrNull()
            ?: Regex("\"link_id\"\\s*:\\s*\"([^\"]+)\"").find(rawBody)?.groupValues?.get(1)

    private suspend inline fun <reified T> HttpResponse.decode(action: String): T {
        val text = bodyAsText()
        if (!status.isSuccess()) error("Cashfree could not $action (${status.value}): $text")
        return json.decodeFromString(text)
    }

    companion object {
        private const val API_VERSION = "2023-08-01"
        private const val SANDBOX_URL = "https://sandbox.cashfree.com"
        private const val PROD_URL = "https://api.cashfree.com"
        private val ISO: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("Asia/Kolkata"))
        const val LINK_PAID = "PAID"
        val LINK_DEAD = setOf("EXPIRED", "CANCELLED", "TERMINATED")
    }
}

@Serializable
data class CashfreeLink(
    @SerialName("link_id") val linkId: String,
    @SerialName("link_url") val linkUrl: String? = null,
    @SerialName("link_status") val linkStatus: String = "",
    @SerialName("link_amount_paid") val linkAmountPaid: Double = 0.0,
)

@Serializable
private data class CashfreeLinkRequest(
    @SerialName("link_id") val linkId: String,
    @SerialName("link_amount") val linkAmount: Double,
    @SerialName("link_currency") val linkCurrency: String = "INR",
    @SerialName("link_purpose") val linkPurpose: String,
    @SerialName("customer_details") val customerDetails: CashfreeCustomer,
    @SerialName("link_partial_payments") val linkPartialPayments: Boolean = false,
    @SerialName("link_expiry_time") val linkExpiryTime: String,
    @SerialName("link_notify") val linkNotify: CashfreeNotify = CashfreeNotify(),
    @SerialName("link_auto_reminders") val linkAutoReminders: Boolean = false,
    @SerialName("link_notes") val linkNotes: Map<String, String> = emptyMap(),
    @SerialName("link_meta") val linkMeta: CashfreeLinkMeta,
)

@Serializable
private data class CashfreeCustomer(
    @SerialName("customer_phone") val customerPhone: String,
    @SerialName("customer_name") val customerName: String? = null,
)

@Serializable
private data class CashfreeNotify(
    @SerialName("send_sms") val sendSms: Boolean = false,
    @SerialName("send_email") val sendEmail: Boolean = false,
)

@Serializable
private data class CashfreeLinkMeta(
    @SerialName("notify_url") val notifyUrl: String? = null,
    @SerialName("return_url") val returnUrl: String? = null,
    @SerialName("upi_intent") val upiIntent: Boolean = true,
)

@Serializable
private data class CashfreeLinkOrder(
    @SerialName("order_id") val orderId: String,
    @SerialName("order_status") val orderStatus: String = "",
)

@Serializable
private data class CashfreeRefundRequest(
    @SerialName("refund_amount") val refundAmount: Double,
    @SerialName("refund_id") val refundId: String,
    @SerialName("refund_note") val refundNote: String,
)

@Serializable
private data class CashfreeRefund(@SerialName("refund_id") val refundId: String? = null)

@Serializable
private data class CashfreeWebhook(val data: CashfreeWebhookData? = null)

@Serializable
private data class CashfreeWebhookData(@SerialName("link_id") val linkId: String? = null)
