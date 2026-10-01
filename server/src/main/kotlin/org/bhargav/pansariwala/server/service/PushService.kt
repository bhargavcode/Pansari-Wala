package org.bhargav.pansariwala.server.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.`in`
import com.mongodb.client.model.ReplaceOptions
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import org.bhargav.pansariwala.server.ServerConfig
import org.bhargav.pansariwala.server.db.DeviceTokenDoc
import org.bhargav.pansariwala.server.db.MongoApp
import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import java.util.Date

private const val FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging"
private const val DEFAULT_TOKEN_URI = "https://oauth2.googleapis.com/token"
private const val ACCESS_TOKEN_TTL_S = 3_600L
private const val ACCESS_TOKEN_REFRESH_MARGIN_MS = 120_000L
private const val PUSH_TTL = "3600s"

/** Payload contract shared with the app (`AppConstants.Push` in shared). */
private object PushContract {
    const val EVENT_ORDER_UPDATE = "ORDER_UPDATE"
    const val ALERT_ORDER_NEW = "ORDER_NEW"
    const val ALERT_PARTNER_TIMEOUT = "PARTNER_TIMEOUT"
    const val ALERT_ACCEPTED = "ACCEPTED"
    const val ALERT_ON_THE_WAY = "ON_THE_WAY"
    const val ALERT_DELIVERED = "DELIVERED"
    const val ALERT_CANCELLED = "CANCELLED"
    const val KEY_PREFIX_STATUS = "ORDER_STATUS"
}

/** Snapshot of an order right after a status write, published to its shop and customer. */
data class OrderUpdate(
    val orderId: String,
    val shopId: String,
    val customerId: String?,
    val previousStatus: String?,
    val status: String,
    val shopName: String,
    val customerName: String,
    val partnerTimeout: Boolean = false,
)

private class Alert(
    val kind: String,
    /** Same key replaces an earlier notification for this order/alert instead of stacking. */
    val key: String,
    /** APNs alert text (iOS shows it when the app is closed); Android builds localized text itself. */
    val title: String,
    val body: String,
)

private class PushMessage(
    val orderId: String,
    val data: Map<String, String>,
    /** Null → silent data push that only refreshes open screens. */
    val alert: Alert?,
)

/**
 * FCM HTTP v1 sender. All sends are fire-and-forget on an IO scope so request latency is unaffected;
 * tokens FCM reports as unregistered are pruned.
 */
class PushService(
    config: ServerConfig,
    mongo: MongoApp,
) {
    private val json = Json { ignoreUnknownKeys = true }
    private val tokenCol = mongo.db.getCollection<DeviceTokenDoc>("device_tokens")
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val account = parseServiceAccount(config.fcmServiceAccountJson)
    private val http = HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = 8_000
            requestTimeoutMillis = 12_000
            socketTimeoutMillis = 12_000
        }
    }
    private val tokenMutex = Mutex()
    @Volatile private var accessToken: String? = null
    @Volatile private var accessTokenExpiresAt: Long = 0

    val enabled: Boolean get() = account != null

    fun register(token: String, principalId: String, role: String, shopId: String?, platform: String) {
        require(token.isNotBlank() && token.length <= 4_096) { "Invalid push token" }
        tokenCol.replaceOne(
            eq("_id", token),
            DeviceTokenDoc(
                token = token,
                principalId = principalId,
                role = role,
                shopId = shopId,
                platform = platform.take(16),
                updatedAt = System.currentTimeMillis(),
            ),
            ReplaceOptions().upsert(true),
        )
    }

    fun unregister(token: String) {
        if (token.isBlank()) return
        tokenCol.deleteOne(eq("_id", token))
    }

    /**
     * Every order status change is pushed to the shop and the customer so open screens update without
     * polling. Only meaningful transitions carry a tray alert; the rest are silent data pushes.
     */
    fun orderUpdated(update: OrderUpdate) {
        if (!enabled) return
        val data = mapOf(
            "event" to PushContract.EVENT_ORDER_UPDATE,
            "orderId" to update.orderId,
            "status" to update.status,
            "shopName" to update.shopName,
            "customerName" to update.customerName,
        )
        send(
            and(eq("role", "SHOP"), eq("shopId", update.shopId)),
            PushMessage(update.orderId, data, shopAlert(update)),
        )
        if (!update.customerId.isNullOrBlank() && update.previousStatus != null) {
            send(
                and(eq("role", "USER"), eq("principalId", update.customerId)),
                PushMessage(update.orderId, data, customerAlert(update)),
            )
        }
    }

    private fun shopAlert(u: OrderUpdate): Alert? {
        val short = u.orderId.takeLast(6)
        return when {
            u.partnerTimeout -> Alert(
                kind = PushContract.ALERT_PARTNER_TIMEOUT,
                key = "${PushContract.ALERT_PARTNER_TIMEOUT}:${u.orderId}",
                title = "No delivery partner found",
                body = "No partner accepted order $short in 5 minutes. Request a delivery partner again.",
            )
            u.previousStatus == null && u.status == "RECEIVED" -> Alert(
                kind = PushContract.ALERT_ORDER_NEW,
                key = "${PushContract.ALERT_ORDER_NEW}:${u.orderId}",
                title = "New online order",
                body = "Order $short from ${u.customerName.ifBlank { u.orderId }}",
            )
            else -> null
        }
    }

    private fun customerAlert(u: OrderUpdate): Alert? {
        if (u.previousStatus == u.status) return null
        val shop = u.shopName
        val (kind, text) = when (u.status) {
            "ACCEPTED" -> if (u.previousStatus == "RECEIVED") {
                PushContract.ALERT_ACCEPTED to ("Order accepted" to "$shop is preparing your order.")
            } else {
                return null
            }
            "ON_THE_WAY" -> PushContract.ALERT_ON_THE_WAY to ("On the way" to "Your order from $shop is on the way.")
            "DELIVERED" -> PushContract.ALERT_DELIVERED to ("Delivered" to "Order from $shop was delivered.")
            "CANCELLED", "REJECTED" ->
                PushContract.ALERT_CANCELLED to ("Order cancelled" to "Your order from $shop was cancelled.")
            else -> return null
        }
        return Alert(kind, "${PushContract.KEY_PREFIX_STATUS}:${u.orderId}:$kind", text.first, text.second)
    }

    private fun send(filter: org.bson.conversions.Bson, message: PushMessage) {
        if (!enabled) return
        scope.launch {
            val tokens = runCatching { tokenCol.find(filter).toList().map { it.token } }.getOrDefault(emptyList())
            if (tokens.isEmpty()) return@launch
            val bearer = runCatching { oauthToken() }.getOrElse {
                println("FCM auth failed: ${it.message}")
                return@launch
            }
            val dead = tokens.filter { token -> sendOne(bearer, token, message) == SendResult.DEAD_TOKEN }
            if (dead.isNotEmpty()) runCatching { tokenCol.deleteMany(`in`("_id", dead)) }
        }
    }

    private enum class SendResult { OK, DEAD_TOKEN, FAILED }

    private suspend fun sendOne(bearer: String, token: String, message: PushMessage): SendResult {
        val sa = account ?: return SendResult.FAILED
        val alert = message.alert
        val collapse = (alert?.key ?: "${PushContract.EVENT_ORDER_UPDATE}:${message.orderId}").take(64)
        val payload = buildJsonObject {
            putJsonObject("message") {
                put("token", token)
                putJsonObject("data") {
                    message.data.forEach { (k, v) -> put(k, v) }
                    if (alert != null) {
                        put("alert", alert.kind)
                        put("key", alert.key)
                    }
                }
                // Android is data-only: the app shows localized alerts and applies the update itself.
                putJsonObject("android") {
                    put("priority", if (alert != null) "HIGH" else "NORMAL")
                    put("ttl", PUSH_TTL)
                    put("collapse_key", collapse)
                }
                putJsonObject("apns") {
                    putJsonObject("headers") {
                        if (alert != null) {
                            put("apns-push-type", "alert")
                            put("apns-priority", "10")
                            put("apns-collapse-id", collapse)
                        } else {
                            put("apns-push-type", "background")
                            put("apns-priority", "5")
                        }
                    }
                    putJsonObject("payload") {
                        putJsonObject("aps") {
                            if (alert != null) {
                                putJsonObject("alert") {
                                    put("title", alert.title)
                                    put("body", alert.body)
                                }
                                put("sound", "default")
                            }
                            put("content-available", 1)
                        }
                    }
                }
            }
        }
        return runCatching {
            val response = http.post("https://fcm.googleapis.com/v1/projects/${sa.projectId}/messages:send") {
                bearerAuth(bearer)
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
            when {
                response.status.isSuccess() -> SendResult.OK
                else -> {
                    val text = response.bodyAsText()
                    val dead = response.status.value == 404 ||
                        text.contains("UNREGISTERED") ||
                        (response.status.value == 400 && text.contains("registration token", ignoreCase = true))
                    if (!dead) println("FCM send failed (${response.status.value}): ${text.take(300)}")
                    if (dead) SendResult.DEAD_TOKEN else SendResult.FAILED
                }
            }
        }.getOrElse {
            println("FCM send error: ${it.message}")
            SendResult.FAILED
        }
    }

    private suspend fun oauthToken(): String {
        val now = System.currentTimeMillis()
        accessToken?.takeIf { now < accessTokenExpiresAt - ACCESS_TOKEN_REFRESH_MARGIN_MS }?.let { return it }
        return tokenMutex.withLock {
            accessToken?.takeIf { System.currentTimeMillis() < accessTokenExpiresAt - ACCESS_TOKEN_REFRESH_MARGIN_MS }
                ?.let { return@withLock it }
            val sa = account ?: error("FCM not configured")
            val issued = System.currentTimeMillis()
            val assertion = JWT.create()
                .withIssuer(sa.clientEmail)
                .withAudience(sa.tokenUri)
                .withClaim("scope", FCM_SCOPE)
                .withIssuedAt(Date(issued))
                .withExpiresAt(Date(issued + ACCESS_TOKEN_TTL_S * 1_000))
                .sign(Algorithm.RSA256(null, sa.privateKey))
            val response = http.submitForm(
                url = sa.tokenUri,
                formParameters = parameters {
                    append("grant_type", "urn:ietf:params:oauth:grant-type:jwt-bearer")
                    append("assertion", assertion)
                },
            )
            val text = response.bodyAsText()
            check(response.status.isSuccess()) { "token endpoint ${response.status.value}: ${text.take(200)}" }
            val obj = json.parseToJsonElement(text).jsonObject
            val token = obj["access_token"]?.jsonPrimitive?.content ?: error("No access_token")
            val expiresIn = obj["expires_in"]?.jsonPrimitive?.long ?: ACCESS_TOKEN_TTL_S
            accessToken = token
            accessTokenExpiresAt = issued + expiresIn * 1_000
            token
        }
    }

    private class ServiceAccount(
        val projectId: String,
        val clientEmail: String,
        val tokenUri: String,
        val privateKey: RSAPrivateKey,
    )

    private fun parseServiceAccount(raw: String): ServiceAccount? {
        if (raw.isBlank()) return null
        return runCatching {
            val obj = json.parseToJsonElement(raw).jsonObject
            val pem = obj.getValue("private_key").jsonPrimitive.content
            val der = Base64.getDecoder().decode(
                pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("\\n", "")
                    .replace(Regex("\\s"), ""),
            )
            val key = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(der)) as RSAPrivateKey
            ServiceAccount(
                projectId = obj.getValue("project_id").jsonPrimitive.content,
                clientEmail = obj.getValue("client_email").jsonPrimitive.content,
                tokenUri = obj["token_uri"]?.jsonPrimitive?.content ?: DEFAULT_TOKEN_URI,
                privateKey = key,
            )
        }.onFailure { println("FCM service account invalid: ${it.message}") }.getOrNull()
    }
}
