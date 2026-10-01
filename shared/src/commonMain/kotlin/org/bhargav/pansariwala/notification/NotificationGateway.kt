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
    /**
     * Stable event key (e.g. `ORDER_NEW:<orderId>`). Push and in-app poll build the same key, so the
     * same event shows once and replaces the earlier system notification.
     */
    val key: String? = null,
)

interface NotificationGateway {
    fun ensureChannels()
    fun requestPermissionIfNeeded()

    /** True when the OS would still show a notification permission prompt for this app. */
    suspend fun needsPermissionPrompt(): Boolean
    fun show(notification: ShopNotification)

    /** Removes this order's notifications from the tray once its update is on screen (consumed). */
    fun clearOrder(orderId: String)
}

expect fun createNotificationGateway(): NotificationGateway
