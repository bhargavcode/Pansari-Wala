package org.bhargav.pansariwala.api

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bhargav.pansariwala.data.local.AppPreferences

/**
 * Fired when an authenticated API call gets 401 and the client cannot refresh tokens.
 * Nav graphs clear the back stack and return to login.
 */
object SessionExpiredBus {
    private val mutex = Mutex()
    private val _events = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val events: SharedFlow<Unit> = _events.asSharedFlow()

    suspend fun onRefreshFailed(preferences: AppPreferences) {
        mutex.withLock {
            if (preferences.getAccessToken().isNullOrBlank()) return
            preferences.clearSession()
            JwtAuthCache.invalidate()
            _events.tryEmit(Unit)
        }
    }
}
