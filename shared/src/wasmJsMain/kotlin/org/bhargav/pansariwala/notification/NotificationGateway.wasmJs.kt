package org.bhargav.pansariwala.notification

private class WasmNotificationGateway : NotificationGateway {
    override fun ensureChannels() = Unit
    override fun requestPermissionIfNeeded() = Unit
    override fun show(notification: ShopNotification) {
        NotificationRouter.emit(notification)
    }
}

actual fun createNotificationGateway(): NotificationGateway = WasmNotificationGateway()
