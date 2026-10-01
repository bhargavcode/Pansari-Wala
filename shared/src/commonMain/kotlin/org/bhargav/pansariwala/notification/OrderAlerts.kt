package org.bhargav.pansariwala.notification

import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.AppConstants.Push
import org.bhargav.pansariwala.util.generateId
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.notify_delivered_body
import pansariwala.shared.generated.resources.notify_delivered_title
import pansariwala.shared.generated.resources.notify_new_order_body
import pansariwala.shared.generated.resources.notify_new_order_title
import pansariwala.shared.generated.resources.notify_on_the_way_body
import pansariwala.shared.generated.resources.notify_on_the_way_title
import pansariwala.shared.generated.resources.notify_order_accepted_body
import pansariwala.shared.generated.resources.notify_order_accepted_title
import pansariwala.shared.generated.resources.notify_order_cancelled_body
import pansariwala.shared.generated.resources.notify_order_cancelled_title
import pansariwala.shared.generated.resources.notify_partner_timeout_body
import pansariwala.shared.generated.resources.notify_partner_timeout_title

/** Builds the localized tray notification for an FCM order update that carries an `alert`. */
object OrderAlerts {
    suspend fun fromPush(data: Map<String, String>): ShopNotification? {
        val orderId = data[Push.KEY_ORDER_ID]?.takeIf { it.isNotBlank() } ?: return null
        val short = orderId.takeLast(6)
        val shopName = data[Push.KEY_SHOP_NAME].orEmpty()
        return when (val alert = data[Push.KEY_ALERT]) {
            Push.ALERT_ORDER_NEW -> ShopNotification(
                id = generateId("notif"),
                title = getString(Res.string.notify_new_order_title),
                body = getString(
                    Res.string.notify_new_order_body,
                    short,
                    data[Push.KEY_CUSTOMER_NAME].orEmpty().ifBlank { orderId },
                ),
                orderId = orderId,
                type = AppConstants.Notification.TYPE_ONLINE_ORDER,
                key = "${Push.ALERT_ORDER_NEW}:$orderId",
            )
            Push.ALERT_PARTNER_TIMEOUT -> ShopNotification(
                id = generateId("notif"),
                title = getString(Res.string.notify_partner_timeout_title),
                body = getString(Res.string.notify_partner_timeout_body, short),
                orderId = orderId,
                type = AppConstants.Notification.TYPE_ONLINE_ORDER,
                key = "${Push.ALERT_PARTNER_TIMEOUT}:$orderId",
            )
            Push.ALERT_ACCEPTED ->
                customer(orderId, alert, Res.string.notify_order_accepted_title, Res.string.notify_order_accepted_body, shopName)
            Push.ALERT_ON_THE_WAY ->
                customer(orderId, alert, Res.string.notify_on_the_way_title, Res.string.notify_on_the_way_body, shopName)
            Push.ALERT_DELIVERED ->
                customer(orderId, alert, Res.string.notify_delivered_title, Res.string.notify_delivered_body, shopName)
            Push.ALERT_CANCELLED ->
                customer(orderId, alert, Res.string.notify_order_cancelled_title, Res.string.notify_order_cancelled_body, shopName)
            else -> null
        }
    }

    private suspend fun customer(
        orderId: String,
        alert: String,
        title: StringResource,
        body: StringResource,
        shopName: String,
    ) = ShopNotification(
        id = generateId("notif"),
        title = getString(title),
        body = getString(body, shopName),
        orderId = orderId,
        type = AppConstants.Notification.TYPE_ORDER,
        key = "${Push.KEY_PREFIX_STATUS}:$orderId:$alert",
    )
}
