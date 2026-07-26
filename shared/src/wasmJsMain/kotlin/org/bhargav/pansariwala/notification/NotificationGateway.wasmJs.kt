package org.bhargav.pansariwala.notification

private class WasmNotificationGateway : NotificationGateway {
    override fun ensureChannels() = Unit
    override fun requestPermissionIfNeeded() = Unit
}

actual fun createNotificationGateway(): NotificationGateway = WasmNotificationGateway()
