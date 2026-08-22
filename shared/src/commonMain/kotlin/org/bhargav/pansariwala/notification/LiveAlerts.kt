package org.bhargav.pansariwala.notification

import kotlinx.coroutines.delay
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.product.AppProduct
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.generateId
import org.jetbrains.compose.resources.getString
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.notify_delivered_body
import pansariwala.shared.generated.resources.notify_delivered_title
import pansariwala.shared.generated.resources.notify_delivery_offer_body
import pansariwala.shared.generated.resources.notify_delivery_offer_title
import pansariwala.shared.generated.resources.notify_new_order_body
import pansariwala.shared.generated.resources.notify_new_order_title
import pansariwala.shared.generated.resources.notify_on_the_way_body
import pansariwala.shared.generated.resources.notify_on_the_way_title
import pansariwala.shared.generated.resources.notify_order_accepted_body
import pansariwala.shared.generated.resources.notify_order_accepted_title

class LiveAlerts(
    private val api: PansariApi,
    private val gateway: NotificationGateway,
    private val preferences: AppPreferences,
) {
    suspend fun run(product: AppProduct) {
        val seenOrders = mutableSetOf<String>()
        val lastStatus = mutableMapOf<String, OrderStatus>()
        var lastOfferId: String? = null
        var primed = false
        while (true) {
            if (!preferences.hasSession()) {
                primed = false
                seenOrders.clear()
                lastStatus.clear()
                lastOfferId = null
            } else {
                runCatching {
                    when (product) {
                        AppProduct.POS -> pollShop(seenOrders, primed)
                        AppProduct.DELIVERY -> lastOfferId = pollPartner(lastOfferId, primed)
                        AppProduct.USER -> pollCustomer(lastStatus, primed)
                    }
                }
                primed = true
            }
            delay(AppConstants.LIVE_ALERT_POLL_MS)
        }
    }

    private suspend fun pollShop(seen: MutableSet<String>, primed: Boolean) {
        val orders = api.shopOnlineOrders()
        if (primed) {
            orders.filter { it.status == OrderStatus.RECEIVED && it.id !in seen }.forEach { order ->
                gateway.show(
                    ShopNotification(
                        id = generateId("notif"),
                        title = getString(Res.string.notify_new_order_title),
                        body = getString(
                            Res.string.notify_new_order_body,
                            order.id.takeLast(6),
                            order.customerName.orEmpty().ifBlank { order.id },
                        ),
                        orderId = order.id,
                        type = AppConstants.Notification.TYPE_ONLINE_ORDER,
                    ),
                )
            }
        }
        seen.addAll(orders.map { it.id })
    }

    private suspend fun pollPartner(lastOfferId: String?, primed: Boolean): String? {
        val offer = api.incomingOffer()
        if (primed && offer != null && offer.id != lastOfferId) {
            gateway.show(
                ShopNotification(
                    id = generateId("notif"),
                    title = getString(Res.string.notify_delivery_offer_title),
                    body = getString(
                        Res.string.notify_delivery_offer_body,
                        offer.shop.name,
                        offer.payoutInr.toInt().toString(),
                    ),
                    orderId = offer.orderId,
                    offerId = offer.id,
                    type = AppConstants.Notification.TYPE_DELIVERY_OFFER,
                ),
            )
        }
        return offer?.id
    }

    private suspend fun pollCustomer(lastStatus: MutableMap<String, OrderStatus>, primed: Boolean) {
        val orders = api.myOrders()
        if (primed) {
            orders.forEach { order ->
                val previous = lastStatus[order.id]
                if (previous != null && previous != order.status) {
                    customerStatusAlert(order.shopName ?: order.shopId, order.id, order.status)?.let { gateway.show(it) }
                }
            }
        }
        lastStatus.clear()
        lastStatus.putAll(orders.associate { it.id to it.status })
    }

    private suspend fun customerStatusAlert(shopName: String, orderId: String, status: OrderStatus): ShopNotification? {
        val (title, body) = when (status) {
            OrderStatus.ACCEPTED, OrderStatus.PACKING, OrderStatus.LOOKING_FOR_PARTNER, OrderStatus.PARTNER_ACCEPTED ->
                getString(Res.string.notify_order_accepted_title) to getString(Res.string.notify_order_accepted_body, shopName)
            OrderStatus.ON_THE_WAY ->
                getString(Res.string.notify_on_the_way_title) to getString(Res.string.notify_on_the_way_body, shopName)
            OrderStatus.DELIVERED, OrderStatus.COMPLETED ->
                getString(Res.string.notify_delivered_title) to getString(Res.string.notify_delivered_body, shopName)
            else -> return null
        }
        return ShopNotification(
            id = generateId("notif"),
            title = title,
            body = body,
            orderId = orderId,
            type = AppConstants.Notification.TYPE_ORDER,
        )
    }
}
