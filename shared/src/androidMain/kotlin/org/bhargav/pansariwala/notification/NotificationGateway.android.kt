package org.bhargav.pansariwala.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import org.bhargav.pansariwala.platform.AndroidActivityHolder
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private const val DEFAULT_CHANNEL_ID = "pansari_default"
private const val DEFAULT_CHANNEL_NAME = "Pansari Wala"
private const val EXTRA_ORDER_ID = "order_id"

private class AndroidNotificationGateway(
    private val context: Context,
) : NotificationGateway {
    override fun ensureChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            DEFAULT_CHANNEL_ID,
            DEFAULT_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Orders, sync, and shop alerts"
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    override fun requestPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val activity = AndroidActivityHolder.activity ?: return
        if (activity.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            return
        }
        activity.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
    }

    override fun show(notification: ShopNotification) {
        ensureChannels()
        NotificationRouter.emit(notification)

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        if (notification.orderId != null) {
            launchIntent?.putExtra(EXTRA_ORDER_ID, notification.orderId)
        }
        val pending = PendingIntent.getActivity(
            context,
            notification.id.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(context, DEFAULT_CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(context)
        }
        val built = builder
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(notification.title)
            .setContentText(notification.body)
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        try {
            manager.notify(notification.id.hashCode(), built)
        } catch (_: SecurityException) {
            // Missing POST_NOTIFICATIONS on API 33+ — in-app router still received the event.
        }
    }
}

private class AndroidNotificationGatewayFromKoin : NotificationGateway, KoinComponent {
    private val context: Context by inject()
    private val delegate by lazy { AndroidNotificationGateway(context) }
    override fun ensureChannels() = delegate.ensureChannels()
    override fun requestPermissionIfNeeded() = delegate.requestPermissionIfNeeded()
    override fun show(notification: ShopNotification) = delegate.show(notification)
}

actual fun createNotificationGateway(): NotificationGateway = AndroidNotificationGatewayFromKoin()
