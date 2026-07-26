package org.bhargav.pansariwala.notification

private class IosNotificationGateway : NotificationGateway {
    override fun ensureChannels() = Unit
    override fun requestPermissionIfNeeded() = Unit
}

actual fun createNotificationGateway(): NotificationGateway = IosNotificationGateway()
