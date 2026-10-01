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
 * rotation / account switch, unregisters on logout or session expiry (all paths clear the JWT).
 */
class PushRegistrar(
    private val api: PansariApi,
    private val preferences: AppPreferences,
) {
    suspend fun run(product: AppProduct) {
        val platform = pushPlatform ?: return
        // Partners get jobs over /ws/delivery; server push covers shop + customer alerts only.
        if (product == AppProduct.DELIVERY) return
        requestPushToken()
        val session = preferences.accessToken
            .map { jwt -> jwt?.takeIf { it.startsWith(AppConstants.JWT_PREFIX) } }
            .distinctUntilChanged()
        combine(session, PushSignals.token) { jwt, token -> jwt to token }
            .collectLatest { (jwt, token) ->
                when {
                    jwt == null -> unregister()
                    token != null -> register(token, platform)
                }
            }
    }

    private suspend fun register(token: String, platform: String) {
        val previous = preferences.getRegisteredPushToken()
        if (previous != null && previous != token) {
            runCatching { api.unregisterPushToken(previous) }
        }
        while (true) {
            if (runCatching { api.registerPushToken(token, platform) }.isSuccess) {
                preferences.setRegisteredPushToken(token)
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
