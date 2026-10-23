package org.bhargav.pansariwala.notification

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.product.AppProduct
import org.bhargav.pansariwala.util.AppConstants

/**
 * Keeps the server's device-token mapping in sync with the session: registers on login / token
 * rotation / account switch / language change, unregisters on logout or session expiry (all paths clear the JWT).
 */
class PushRegistrar(
    private val api: PansariApi,
    private val preferences: AppPreferences,
) {
    suspend fun run(product: AppProduct) {
        val platform = pushPlatform ?: return
        requestPushToken()
        val session = preferences.accessToken
            .map { jwt -> jwt?.takeIf { it.startsWith(AppConstants.JWT_PREFIX) } }
            .distinctUntilChanged()
        // Re-registers on language change: the server sends iOS tray alert text in the registered language.
        val language = preferences.userSettings.map { it.language }.distinctUntilChanged()
        combine(session, PushSignals.token, language) { jwt, token, _ -> jwt to token }
            .collectLatest { (jwt, token) ->
                when {
                    jwt == null -> unregister()
                    token != null -> register(token, platform)
                }
            }
    }

    /**
     * Always upserts (once per app start / session / token change): the server row can be lost while
     * the local record still says "registered" (cancelled logout, pruned token), which would silence
     * pushes for this device indefinitely.
     */
    private suspend fun register(token: String, platform: String) {
        val userId = preferences.getUserId()
        val previous = preferences.getRegisteredPushToken()
        if (previous != null && previous != token) {
            runCatching { api.unregisterPushToken(previous) }
        }
        val deviceId = preferences.getOrCreateDeviceId()
        while (true) {
            if (runCatching { api.registerPushToken(token, platform, deviceId) }.isSuccess) {
                preferences.setRegisteredPushToken(token, userId)
                return
            }
            delay(AppConstants.PUSH_REGISTER_RETRY_MS)
        }
    }

    private suspend fun unregister() {
        val previous = preferences.getRegisteredPushToken() ?: return
        if (runCatching { api.unregisterPushToken(previous) }.isSuccess) {
            preferences.setRegisteredPushToken(null)
        }
    }
}
