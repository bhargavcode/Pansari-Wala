package org.bhargav.pansariwala.notification

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-app deep-link bus: when a notification is tapped (or shown on platforms
 * without system notifications), consumers can navigate.
 */
object NotificationRouter {
    private val _events = MutableSharedFlow<ShopNotification>(extraBufferCapacity = 8)
    val events: SharedFlow<ShopNotification> = _events.asSharedFlow()

    fun emit(notification: ShopNotification) {
        _events.tryEmit(notification)
    }
}
