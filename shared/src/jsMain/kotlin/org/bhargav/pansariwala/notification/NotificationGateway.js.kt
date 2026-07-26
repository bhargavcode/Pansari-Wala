package org.bhargav.pansariwala.notification

private class JsNotificationGateway : NotificationGateway {
    override fun ensureChannels() = Unit
    override fun requestPermissionIfNeeded() = Unit
}

actual fun createNotificationGateway(): NotificationGateway = JsNotificationGateway()
