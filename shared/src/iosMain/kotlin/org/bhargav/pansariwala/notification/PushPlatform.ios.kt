package org.bhargav.pansariwala.notification

import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.util.AppConstants
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

actual val pushPlatform: String? = AppConstants.Push.PLATFORM_IOS

/** iOS tokens arrive via FirebaseMessaging's delegate in Swift ([IosPushBridge.onToken]). */
actual fun requestPushToken() = Unit

/** Called from `iOSApp.swift` (FirebaseMessaging delegate + UNUserNotificationCenter). */
object IosPushBridge : KoinComponent {
    private val scope = MainScope()
    private val handler: PushMessageHandler by inject()

    fun onToken(token: String) {
        if (token.isNotBlank()) PushSignals.token.value = token
    }

    /** Alert push while the app is open: caller suppresses the APNs banner, we show the localized one. */
    fun willPresent(userInfo: Map<Any?, *>) {
        val data = userInfo.toStringMap()
        scope.launch { handler.handle(data, showAlert = true) }
    }

    /**
     * `content-available` delivery (silent updates, and alert pushes in background where iOS already
     * showed the banner). Alert pushes while active are handled by [willPresent] instead.
     */
    fun didReceive(userInfo: Map<Any?, *>, active: Boolean) {
        val data = userInfo.toStringMap()
        if (active && data[AppConstants.Push.KEY_ALERT] != null) return
        scope.launch { handler.handle(data, showAlert = false) }
    }

    private fun Map<Any?, *>.toStringMap(): Map<String, String> =
        entries.mapNotNull { (k, v) -> (k as? String)?.let { key -> (v as? String)?.let { key to it } } }.toMap()
}
