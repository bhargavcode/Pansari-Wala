package org.bhargav.pansariwala.notification

private class JsNotificationGateway : NotificationGateway {
    override fun ensureChannels() = Unit
    override fun requestPermissionIfNeeded() = Unit
    override fun show(notification: ShopNotification) {
        NotificationRouter.emit(notification)
    }
}

actual fun createNotificationGateway(): NotificationGateway = JsNotificationGateway()
