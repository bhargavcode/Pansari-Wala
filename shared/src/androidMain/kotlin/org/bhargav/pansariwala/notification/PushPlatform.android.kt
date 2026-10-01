package org.bhargav.pansariwala.notification

import com.google.firebase.messaging.FirebaseMessaging
import org.bhargav.pansariwala.util.AppConstants

actual val pushPlatform: String? = AppConstants.Push.PLATFORM_ANDROID

actual fun requestPushToken() {
    runCatching {
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            if (!token.isNullOrBlank()) PushSignals.token.value = token
        }
    }
}
