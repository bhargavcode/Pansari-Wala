package org.bhargav.pansariwala.notification

import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.domain.model.PaymentIntentStatus
import pansariwala.shared.generated.resources.notify_payment_confirm_body
import pansariwala.shared.generated.resources.notify_payment_confirm_title
import pansariwala.shared.generated.resources.notify_payment_failed_body
import pansariwala.shared.generated.resources.notify_payment_failed_title
import pansariwala.shared.generated.resources.notify_payment_refunded_body
import pansariwala.shared.generated.resources.notify_payment_refunded_title
import pansariwala.shared.generated.resources.notify_payment_success_body
import pansariwala.shared.generated.resources.notify_payment_success_title
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.AppConstants.Push
import org.bhargav.pansariwala.util.generateId
import org.jetbrains.compose.resources.getString
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.notify_delivery_offer_body
import pansariwala.shared.generated.resources.notify_delivery_offer_title
import pansariwala.shared.generated.resources.notify_issue_resolved_body
import pansariwala.shared.generated.resources.notify_issue_resolved_body_version
import pansariwala.shared.generated.resources.notify_issue_resolved_title
import pansariwala.shared.generated.resources.notify_registration_reminder_body
import pansariwala.shared.generated.resources.notify_registration_reminder_title

/** Single entry for FCM data payloads on every platform: update open screens, then tray alert if any. */
class PushMessageHandler(
    private val gateway: NotificationGateway,
    private val preferences: AppPreferences,
) {
    /** @param showAlert false when the OS already displayed the alert (iOS background APNs). */
    suspend fun handle(data: Map<String, String>, showAlert: Boolean = true) {
        if (data[Push.KEY_EVENT] == Push.EVENT_REGISTRATION_REMINDER) {
            if (showAlert && !preferences.hasSession()) gateway.show(registrationReminderNotification())
            return
        }
        if (!preferences.hasSession()) return
        when (data[Push.KEY_EVENT]) {
            Push.EVENT_ORDER_UPDATE -> handleOrderUpdate(data, showAlert)
            Push.EVENT_DELIVERY_OFFER -> handleDeliveryOffer(data, showAlert)
            Push.EVENT_ISSUE_UPDATE -> if (showAlert) issueResolvedNotification(data)?.let(gateway::show)
            Push.EVENT_PAYMENT_UPDATE, Push.EVENT_PAYMENT_CONFIRM -> handlePayment(data, showAlert)
        }
    }

    private suspend fun handlePayment(data: Map<String, String>, showAlert: Boolean) {
        val intentId = data[Push.KEY_INTENT_ID]?.takeIf { it.isNotBlank() } ?: return
        PaymentUpdates.emit(intentId)
        if (!showAlert || data[Push.KEY_ALERT].isNullOrBlank()) return
        val amount = data[Push.KEY_AMOUNT].orEmpty()
        val shopName = data[Push.KEY_SHOP_NAME].orEmpty()
        val orderId = data[Push.KEY_ORDER_ID].orEmpty()
        val (title, body) = when {
            data[Push.KEY_EVENT] == Push.EVENT_PAYMENT_CONFIRM ->
                getString(Res.string.notify_payment_confirm_title) to
                    getString(Res.string.notify_payment_confirm_body, amount, data[Push.KEY_CUSTOMER_NAME].orEmpty(), intentId)
            data[Push.KEY_STATUS] == PaymentIntentStatus.SUCCESS.name ->
                getString(Res.string.notify_payment_success_title) to
                    getString(Res.string.notify_payment_success_body, amount, shopName, orderId)
            data[Push.KEY_STATUS] == PaymentIntentStatus.REFUNDED.name ->
                getString(Res.string.notify_payment_refunded_title) to
                    getString(Res.string.notify_payment_refunded_body, amount, shopName)
            data[Push.KEY_STATUS] == PaymentIntentStatus.FAILED.name ->
                getString(Res.string.notify_payment_failed_title) to
                    getString(Res.string.notify_payment_failed_body, amount, shopName)
            else -> return
        }
        gateway.show(
            ShopNotification(
                id = generateId("notif"),
                title = title,
                body = body,
                orderId = intentId,
                type = if (data[Push.KEY_EVENT] == Push.EVENT_PAYMENT_CONFIRM) {
                    AppConstants.Notification.TYPE_PAYMENT_CONFIRM
                } else {
                    AppConstants.Notification.TYPE_PAYMENT
                },
                key = "${data[Push.KEY_EVENT]}:$intentId",
            ),
        )
    }

    private suspend fun registrationReminderNotification() = ShopNotification(
        id = generateId("notif"),
        title = getString(Res.string.notify_registration_reminder_title),
        body = getString(Res.string.notify_registration_reminder_body),
        type = AppConstants.Notification.TYPE_REGISTRATION,
        key = Push.EVENT_REGISTRATION_REMINDER,
    )

    private suspend fun issueResolvedNotification(data: Map<String, String>): ShopNotification? {
        val issueId = data[Push.KEY_ISSUE_ID]?.takeIf { it.isNotBlank() } ?: return null
        val feature = data[Push.KEY_FEATURE].orEmpty()
        val fixedIn = data[Push.KEY_FIXED_IN].orEmpty()
        return ShopNotification(
            id = generateId("notif"),
            title = getString(Res.string.notify_issue_resolved_title),
            body = if (fixedIn.isBlank()) {
                getString(Res.string.notify_issue_resolved_body, feature)
            } else {
                getString(Res.string.notify_issue_resolved_body_version, feature, fixedIn)
            },
            type = AppConstants.Support.NOTIFICATION_TYPE,
            key = "${Push.ALERT_ISSUE_RESOLVED}:$issueId",
        )
    }

    private suspend fun handleOrderUpdate(data: Map<String, String>, showAlert: Boolean) {
        val orderId = data[Push.KEY_ORDER_ID]?.takeIf { it.isNotBlank() } ?: return
        val status = data[Push.KEY_STATUS]?.let { raw -> OrderStatus.entries.firstOrNull { it.name == raw } }
        OrderUpdates.emit(OrderUpdate(orderId, status))
        if (showAlert) OrderAlerts.fromPush(data)?.let(gateway::show)
    }

    private suspend fun handleDeliveryOffer(data: Map<String, String>, showAlert: Boolean) {
        if (!preferences.getPartnerOnlineDuty()) return
        val offerId = data[Push.KEY_OFFER_ID]?.takeIf { it.isNotBlank() } ?: return
        val notification = deliveryOfferNotification(
            offerId = offerId,
            orderId = data[Push.KEY_ORDER_ID],
            shopName = data[Push.KEY_SHOP_NAME].orEmpty(),
            payout = data[Push.KEY_PAYOUT].orEmpty(),
        )
        if (showAlert) gateway.show(notification) else NotificationRouter.emit(notification)
    }
}

/** Same key for socket and push so one offer produces one alert. */
internal suspend fun deliveryOfferNotification(
    offerId: String,
    orderId: String?,
    shopName: String,
    payout: String,
): ShopNotification = ShopNotification(
    id = generateId("notif"),
    title = getString(Res.string.notify_delivery_offer_title),
    body = getString(Res.string.notify_delivery_offer_body, shopName, payout),
    orderId = orderId,
    offerId = offerId,
    type = AppConstants.Notification.TYPE_DELIVERY_OFFER,
    key = "${Push.ALERT_DELIVERY_OFFER}:$offerId",
)
