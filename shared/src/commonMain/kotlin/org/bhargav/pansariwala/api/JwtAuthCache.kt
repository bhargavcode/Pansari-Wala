package org.bhargav.pansariwala.api

import kotlin.concurrent.Volatile

/**
 * Clears Ktor bearer token cache after login/logout so the next request
 * reloads the JWT from DataStore (avoids stuck null/ stale tokens).
 */
object JwtAuthCache {
    @Volatile
    var onInvalidate: (() -> Unit)? = null

    fun invalidate() {
        onInvalidate?.invoke()
    }
}
