package org.bhargav.pansariwala.notification

import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.util.AppConstants.Push

/** Single entry for FCM data payloads on every platform: update open screens, then tray alert if any. */
class PushMessageHandler(
    private val gateway: NotificationGateway,
    private val preferences: AppPreferences,
) {
    /** @param showAlert false when the OS already displayed the alert (iOS background APNs). */
    suspend fun handle(data: Map<String, String>, showAlert: Boolean = true) {
        if (data[Push.KEY_EVENT] != Push.EVENT_ORDER_UPDATE) return
        if (!preferences.hasSession()) return
        val orderId = data[Push.KEY_ORDER_ID]?.takeIf { it.isNotBlank() } ?: return
        val status = data[Push.KEY_STATUS]?.let { raw -> OrderStatus.entries.firstOrNull { it.name == raw } }
        OrderUpdates.emit(OrderUpdate(orderId, status))
        if (showAlert) OrderAlerts.fromPush(data)?.let(gateway::show)
    }
}
