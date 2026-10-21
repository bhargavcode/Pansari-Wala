package org.bhargav.pansariwala.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.AppConstants.Notification
import org.bhargav.pansariwala.util.AppConstants.Push

data class NotificationOpenRequest(
    val type: String,
    val orderId: String? = null,
)

/**
 * Holds the latest system-tray tap until a signed-in nav graph consumes it, so a tap that
 * cold-starts the app still lands on the right screen after splash.
 */
object NotificationOpenRequests {
    private val _pending = MutableStateFlow<NotificationOpenRequest?>(null)
    val pending: StateFlow<NotificationOpenRequest?> = _pending.asStateFlow()

    fun open(request: NotificationOpenRequest) {
        _pending.value = request
    }

    fun consume() {
        _pending.value = null
    }

    /**
     * Tap target for an iOS alert: local notifications carry [Notification.USER_INFO_TYPE];
     * APNs alerts carry the raw FCM data payload.
     */
    fun fromUserInfo(data: Map<String, String>): NotificationOpenRequest? {
        data[Notification.USER_INFO_TYPE]?.takeIf { it.isNotBlank() }?.let { type ->
            return NotificationOpenRequest(type, data[Notification.USER_INFO_ORDER_ID])
        }
        val orderId = data[Push.KEY_ORDER_ID]?.takeIf { it.isNotBlank() }
        val intentId = data[Push.KEY_INTENT_ID]?.takeIf { it.isNotBlank() }
        return when (data[Push.KEY_EVENT]) {
            Push.EVENT_PAYMENT_UPDATE -> intentId?.let { NotificationOpenRequest(Notification.TYPE_PAYMENT, it) }
            Push.EVENT_PAYMENT_CONFIRM -> NotificationOpenRequest(Notification.TYPE_PAYMENT_CONFIRM, intentId)
            Push.EVENT_DELIVERY_OFFER -> NotificationOpenRequest(Notification.TYPE_DELIVERY_OFFER, orderId)
            Push.EVENT_ISSUE_UPDATE -> NotificationOpenRequest(AppConstants.Support.NOTIFICATION_TYPE)
            Push.EVENT_ORDER_UPDATE -> orderId?.let {
                val shopAlert = data[Push.KEY_ALERT] in setOf(Push.ALERT_ORDER_NEW, Push.ALERT_PARTNER_TIMEOUT)
                NotificationOpenRequest(if (shopAlert) Notification.TYPE_ONLINE_ORDER else Notification.TYPE_ORDER, it)
            }
            else -> null
        }
    }
}
