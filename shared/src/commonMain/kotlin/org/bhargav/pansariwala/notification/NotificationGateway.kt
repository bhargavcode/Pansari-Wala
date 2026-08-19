package org.bhargav.pansariwala.notification

import org.bhargav.pansariwala.util.AppConstants

/**
 * Payload carried with shop notifications. Tapping an order notification
 * should open the orders workspace focused on [orderId].
 */
data class ShopNotification(
    val id: String,
    val title: String,
    val body: String,
    val orderId: String? = null,
    val offerId: String? = null,
    val type: String = AppConstants.Notification.TYPE_ORDER,
)

interface NotificationGateway {
    fun ensureChannels()
    fun requestPermissionIfNeeded()
    fun show(notification: ShopNotification)
}

expect fun createNotificationGateway(): NotificationGateway
