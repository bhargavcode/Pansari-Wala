package org.bhargav.pansariwala.notification

import org.bhargav.pansariwala.util.AppClock
import kotlin.concurrent.atomics.AtomicReference
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val DEDUPE_WINDOW_MS = 3 * 60_000L

/**
 * Drops a keyed notification already shown within [DEDUPE_WINDOW_MS], so FCM redelivery or the
 * iOS foreground + background callbacks for one push produce a single alert/snackbar.
 */
@OptIn(ExperimentalAtomicApi::class)
class DedupingNotificationGateway(
    private val delegate: NotificationGateway,
) : NotificationGateway {
    private val recent = AtomicReference<Map<String, Long>>(emptyMap())

    override fun ensureChannels() = delegate.ensureChannels()

    override fun requestPermissionIfNeeded() = delegate.requestPermissionIfNeeded()

    override suspend fun needsPermissionPrompt(): Boolean = delegate.needsPermissionPrompt()

    override fun show(notification: ShopNotification) {
        val key = notification.key
        if (key == null || markIfNew(key)) delegate.show(notification)
    }

    override fun clearOrder(orderId: String) = delegate.clearOrder(orderId)

    private fun markIfNew(key: String): Boolean {
        while (true) {
            val now = AppClock.nowMillis()
            val current = recent.load()
            val shownAt = current[key]
            if (shownAt != null && now - shownAt < DEDUPE_WINDOW_MS) return false
            val next = current.filterValues { now - it < DEDUPE_WINDOW_MS } + (key to now)
            if (recent.compareAndSet(current, next)) return true
        }
    }
}
