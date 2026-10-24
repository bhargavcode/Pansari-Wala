package org.bhargav.pansariwala

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import kotlinx.browser.window
import kotlinx.coroutines.delay
import org.bhargav.pansariwala.api.SessionExpiredBus
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.master.MasterSession
import org.bhargav.pansariwala.util.AppClock
import org.bhargav.pansariwala.util.AppConstants
import org.koin.compose.koinInject
import org.w3c.dom.events.Event

/**
 * Browser POS hardening: signs out after [AppConstants.PosWeb.IDLE_TIMEOUT_MS] without input or once the JWT
 * expires, so an unattended counter tab does not stay logged in.
 */
@Composable
fun PosWebSessionGuard(content: @Composable () -> Unit) {
    val preferences: AppPreferences = koinInject()
    val activity = remember { LastActivity(AppClock.nowMillis()) }
    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = { activity.at = AppClock.nowMillis() }
        ACTIVITY_EVENTS.forEach { window.addEventListener(it, listener) }
        onDispose { ACTIVITY_EVENTS.forEach { window.removeEventListener(it, listener) } }
    }
    LaunchedEffect(Unit) {
        while (true) {
            delay(AppConstants.PosWeb.IDLE_CHECK_INTERVAL_MS)
            val token = preferences.getAccessToken()
            if (token.isNullOrBlank()) {
                activity.at = AppClock.nowMillis()
                continue
            }
            val now = AppClock.nowMillis()
            if (now - activity.at >= AppConstants.PosWeb.IDLE_TIMEOUT_MS || MasterSession.isExpired(token, now)) {
                SessionExpiredBus.onRefreshFailed(preferences)
            }
        }
    }
    content()
}

private class LastActivity(var at: Long)

private val ACTIVITY_EVENTS = listOf("pointerdown", "keydown", "wheel", "touchstart")

/** `/pos/register` opens registration directly; the URL is normalised so a reload lands on login. */
fun consumePosRegisterDeepLink(): Boolean {
    val path = jsPathname().lowercase().trimEnd('/')
    if (path != POS_REGISTER_PATH) return false
    window.history.replaceState(null, "", POS_PATH)
    return true
}

private const val POS_PATH = "/pos"
private const val POS_REGISTER_PATH = "/pos/register"
