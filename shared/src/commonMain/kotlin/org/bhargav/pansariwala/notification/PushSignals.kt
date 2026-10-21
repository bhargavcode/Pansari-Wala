package org.bhargav.pansariwala.notification

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import org.bhargav.pansariwala.domain.model.OrderStatus

/** Process-wide push state shared by platform FCM/APNs hooks and [PushRegistrar]. */
object PushSignals {
    /** Current FCM registration token, set by platform code (Android service / iOS bridge). */
    val token = MutableStateFlow<String?>(null)
}

/** Order change delivered by push; [status] is null if the payload carried an unknown value. */
data class OrderUpdate(val orderId: String, val status: OrderStatus?)

/** Live order updates for open screens (replaces REST polling). Emitted only from push payloads. */
object OrderUpdates {
    private val _events = MutableSharedFlow<OrderUpdate>(extraBufferCapacity = 32)
    val events: SharedFlow<OrderUpdate> = _events.asSharedFlow()

    fun emit(update: OrderUpdate) {
        _events.tryEmit(update)
    }
}

/** Payment transaction ids changed by push (customer result or shop "confirm payment"); open screens reload. */
object PaymentUpdates {
    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val events: SharedFlow<String> = _events.asSharedFlow()

    fun emit(intentId: String) {
        _events.tryEmit(intentId)
    }
}

/** Defers the notification permission prompt until the signed-in home screen is shown. */
object NotificationPromptGate {
    val homeReached = MutableStateFlow(false)

    fun onHomeShown() {
        homeReached.value = true
    }
}

/** Platform push id sent to the server, or null where push is not supported (web). */
expect val pushPlatform: String?

/** Asks the platform for the current FCM token; the result lands in [PushSignals.token]. */
expect fun requestPushToken()
