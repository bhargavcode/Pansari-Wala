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

/** Platform push id sent to the server, or null where push is not supported (web). */
expect val pushPlatform: String?

/** Asks the platform for the current FCM token; the result lands in [PushSignals.token]. */
expect fun requestPushToken()
