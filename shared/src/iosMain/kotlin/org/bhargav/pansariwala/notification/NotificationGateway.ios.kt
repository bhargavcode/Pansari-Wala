package org.bhargav.pansariwala.notification

import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

private class IosNotificationGateway : NotificationGateway {
    override fun ensureChannels() = Unit

    override fun requestPermissionIfNeeded() {
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
            UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge,
        ) { _, _ -> }
    }

    override fun show(notification: ShopNotification) {
        NotificationRouter.emit(notification)
        val content = UNMutableNotificationContent()
        content.setTitle(notification.title)
        content.setBody(notification.body)
        content.setSound(UNNotificationSound.defaultSound)
        notification.orderId?.let { content.setUserInfo(mapOf(ORDER_ID_KEY to it)) }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(0.15, repeats = false)
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = notification.key ?: notification.id,
            content = content,
            trigger = trigger,
        )
        UNUserNotificationCenter.currentNotificationCenter().addNotificationRequest(request, null)
    }

    /** Matches local alerts and APNs alerts alike: FCM puts `orderId` at the root of userInfo. */
    override fun clearOrder(orderId: String) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.getDeliveredNotificationsWithCompletionHandler { delivered ->
            val ids = delivered.orEmpty()
                .mapNotNull { it as? UNNotification }
                .filter { it.request.content.userInfo[ORDER_ID_KEY] == orderId }
                .map { it.request.identifier }
            if (ids.isNotEmpty()) center.removeDeliveredNotificationsWithIdentifiers(ids)
        }
    }
}

private const val ORDER_ID_KEY = "orderId"

actual fun createNotificationGateway(): NotificationGateway = IosNotificationGateway()
