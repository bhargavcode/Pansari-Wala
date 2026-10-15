package org.bhargav.pansariwala.api

import io.ktor.client.plugins.logging.LogLevel
import org.bhargav.pansariwala.util.AppConstants

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
    var baseUrl: String = AppConstants.DEFAULT_API_BASE_URL
        set(value) {
            val trimmed = value.trim()
            field = if (trimmed.isEmpty() || AppConstants.LEGACY_API_HOSTS.any { trimmed.contains(it) }) {
                AppConstants.DEFAULT_API_BASE_URL
            } else {
                trimmed
            }
        }

    /** Set from app entry (Android BuildConfig.DEBUG / iOS debug scheme). */
    var isDebugBuild: Boolean = false

    /**
     * Explicit HTTP log override.
     * `null` → [HttpLogLevel.BODY] on debug builds, [HttpLogLevel.NONE] on release.
     * Binary/multipart bodies are redacted in [installPansariHttpLogging]; JSON bodies are kept.
     * Examples: `ApiRuntime.httpLogLevel = HttpLogLevel.HEADERS` or `NONE` to silence debug.
     */
    var httpLogLevel: HttpLogLevel? = null

    fun resolvedHttpLogLevel(): HttpLogLevel =
        httpLogLevel
            ?: if (isDebugBuild) {
                HttpLogLevel.BODY
            } else {
                HttpLogLevel.NONE
            }
}
