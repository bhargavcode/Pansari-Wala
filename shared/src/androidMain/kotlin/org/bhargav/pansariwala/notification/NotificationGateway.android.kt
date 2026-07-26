package org.bhargav.pansariwala.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val DEFAULT_CHANNEL_ID = "pansari_default"
private const val DEFAULT_CHANNEL_NAME = "Pansari Wala"

private class AndroidNotificationGateway(
    private val context: Context,
) : NotificationGateway {
    override fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            DEFAULT_CHANNEL_ID,
            DEFAULT_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Orders, sync, and shop alerts"
        }
        manager.createNotificationChannel(channel)
    }

    override fun requestPermissionIfNeeded() {
        // POST_NOTIFICATIONS runtime prompt can be added with Activity result API later.
    }
}

private class AndroidNotificationGatewayFromKoin : NotificationGateway, KoinComponent {
    private val context: Context by inject()
    private val delegate by lazy { AndroidNotificationGateway(context) }
    override fun ensureChannels() = delegate.ensureChannels()
    override fun requestPermissionIfNeeded() = delegate.requestPermissionIfNeeded()
}

actual fun createNotificationGateway(): NotificationGateway = AndroidNotificationGatewayFromKoin()
