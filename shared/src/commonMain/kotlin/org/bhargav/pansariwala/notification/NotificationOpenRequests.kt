package org.bhargav.pansariwala.notification

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

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
}
