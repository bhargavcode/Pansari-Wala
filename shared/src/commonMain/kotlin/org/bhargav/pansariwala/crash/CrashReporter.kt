package org.bhargav.pansariwala.crash

/**
 * Crash reporting contract. Android actual wires Firebase Crashlytics when available;
 * otherwise logs with custom key/value params.
 */
interface CrashReporter {
    fun setCustomKeys(params: Map<String, String>)

    fun recordException(
        throwable: Throwable,
        params: Map<String, String> = emptyMap(),
    )

    fun log(message: String)
}

expect fun createCrashReporter(): CrashReporter
