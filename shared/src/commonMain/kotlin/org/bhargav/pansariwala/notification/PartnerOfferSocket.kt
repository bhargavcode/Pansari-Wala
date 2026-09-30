package org.bhargav.pansariwala.notification

import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.api.ApiRuntime
import org.bhargav.pansariwala.api.DeliveryOfferDto
import org.bhargav.pansariwala.api.createPlatformHttpClient
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.generateId
import org.jetbrains.compose.resources.getString
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.notify_delivery_offer_body
import pansariwala.shared.generated.resources.notify_delivery_offer_title
import kotlin.coroutines.coroutineContext

/**
 * Listens for partner offer push on `/ws/delivery` (no REST polling).
 * Connects only while the partner session is online.
 */
class PartnerOfferSocket(
    private val preferences: AppPreferences,
    private val gateway: NotificationGateway,
) {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    suspend fun run() {
        while (coroutineContext.isActive) {
            if (!preferences.hasSession() || !preferences.getPartnerOnlineDuty()) {
                delay(AppConstants.PARTNER_OFFER_SOCKET_IDLE_MS)
                continue
            }
            runCatching { listenOnce() }
            delay(AppConstants.PARTNER_OFFER_SOCKET_RETRY_MS)
        }
    }

    private suspend fun listenOnce() {
        val token = preferences.getAccessToken()
            ?.takeIf { it.startsWith(AppConstants.JWT_PREFIX) }
            ?: return
        val url = deliveryWsUrl(ApiRuntime.baseUrl)
        val client = createPlatformHttpClient().config {
            install(WebSockets)
        }
        try {
            client.webSocket(
                urlString = url,
                request = { header(HttpHeaders.Authorization, token) },
            ) {
                for (frame in incoming) {
                    if (!preferences.getPartnerOnlineDuty()) break
                    if (frame !is Frame.Text) continue
                    val text = frame.readText()
                    if (text == "pong" || text.isBlank()) continue
                    val offer = runCatching { json.decodeFromString<DeliveryOfferDto>(text) }.getOrNull()
                        ?: continue
                    gateway.show(
                        ShopNotification(
                            id = generateId("notif"),
                            title = getString(Res.string.notify_delivery_offer_title),
                            body = getString(
                                Res.string.notify_delivery_offer_body,
                                offer.shopName,
                                offer.payoutInr.toInt().toString(),
                            ),
                            orderId = offer.orderId,
                            offerId = offer.id,
                            type = AppConstants.Notification.TYPE_DELIVERY_OFFER,
                        ),
                    )
                }
            }
        } finally {
            client.close()
        }
    }

    private fun deliveryWsUrl(apiBase: String): String {
        val trimmed = apiBase.trimEnd('/')
        val wsBase = when {
            trimmed.startsWith("https://", ignoreCase = true) -> "wss://${trimmed.substringAfter("://")}"
            trimmed.startsWith("http://", ignoreCase = true) -> "ws://${trimmed.substringAfter("://")}"
            else -> trimmed
        }
        return "$wsBase/ws/delivery"
    }
}
