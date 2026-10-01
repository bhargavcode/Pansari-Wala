package org.bhargav.pansariwala.notification

private class JsNotificationGateway : NotificationGateway {
    override fun ensureChannels() = Unit
    override fun requestPermissionIfNeeded() = Unit
    override suspend fun needsPermissionPrompt(): Boolean = false
    override fun show(notification: ShopNotification) {
        NotificationRouter.emit(notification)
    }
    override fun clearOrder(orderId: String) = Unit
}

actual fun createNotificationGateway(): NotificationGateway = JsNotificationGateway()
