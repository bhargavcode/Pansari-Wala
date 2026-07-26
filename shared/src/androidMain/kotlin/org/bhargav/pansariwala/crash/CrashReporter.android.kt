package org.bhargav.pansariwala.crash

import android.util.Log

/**
 * Phase 1 CrashReporter. Mirrors Firebase Crashlytics API surface
 * (setCustomKeys + recordException). Swap body for FirebaseCrashlytics
 * once google-services.json is added.
 */
private class AndroidCrashReporter : CrashReporter {
    private val customKeys = linkedMapOf<String, String>()

    override fun setCustomKeys(params: Map<String, String>) {
        customKeys.putAll(params)
        // FirebaseCrashlytics.getInstance().apply { params.forEach { (k, v) -> setCustomKey(k, v) } }
        Log.d("CrashReporter", "customKeys=$customKeys")
    }

    override fun recordException(throwable: Throwable, params: Map<String, String>) {
        if (params.isNotEmpty()) {
            setCustomKeys(params)
        }
        // FirebaseCrashlytics.getInstance().recordException(throwable)
        Log.e(
            "CrashReporter",
            "recordException keys=$customKeys message=${throwable.message}",
            throwable,
        )
    }

    override fun log(message: String) {
        // FirebaseCrashlytics.getInstance().log(message)
        Log.w("CrashReporter", message)
    }
}

actual fun createCrashReporter(): CrashReporter = AndroidCrashReporter()
