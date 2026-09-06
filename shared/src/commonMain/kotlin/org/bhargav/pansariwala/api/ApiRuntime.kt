package org.bhargav.pansariwala.api

import io.ktor.client.plugins.logging.LogLevel

/** Configurable HTTP client log verbosity. */
enum class HttpLogLevel {
    NONE,
    INFO,
    HEADERS,
    BODY,
    ;

    fun toKtor(): LogLevel = when (this) {
        NONE -> LogLevel.NONE
        INFO -> LogLevel.INFO
        HEADERS -> LogLevel.HEADERS
        BODY -> LogLevel.BODY
    }

    companion object {
        fun fromName(value: String?): HttpLogLevel? =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) }
    }
}

object ApiRuntime {
    var baseUrl: String = org.bhargav.pansariwala.util.AppConstants.DEFAULT_API_BASE_URL

    /** Set from app entry (Android BuildConfig.DEBUG / iOS debug scheme). */
    var isDebugBuild: Boolean = false

    /**
     * Explicit HTTP log override.
     * `null` → [HttpLogLevel.HEADERS] on debug builds, [HttpLogLevel.NONE] on release.
     * Note: [HttpLogLevel.BODY] is coerced to HEADERS to avoid dumping image bytes to logcat.
     * Examples: `ApiRuntime.httpLogLevel = HttpLogLevel.HEADERS` or `NONE` to silence debug.
     */
    var httpLogLevel: HttpLogLevel? = null

    fun resolvedHttpLogLevel(): HttpLogLevel =
        httpLogLevel
            ?: if (isDebugBuild) {
                HttpLogLevel.HEADERS
            } else {
                HttpLogLevel.NONE
            }
}
