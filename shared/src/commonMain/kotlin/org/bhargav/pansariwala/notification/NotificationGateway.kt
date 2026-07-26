package org.bhargav.pansariwala.notification

interface NotificationGateway {
    fun ensureChannels()
    fun requestPermissionIfNeeded()
}

expect fun createNotificationGateway(): NotificationGateway
