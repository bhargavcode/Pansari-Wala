package org.bhargav.pansariwala.api

import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.logging.LoggingConfig
import io.ktor.http.HttpHeaders
import org.bhargav.pansariwala.util.AppConstants

fun LoggingConfig.installPansariHttpLogging() {
    val level = ApiRuntime.resolvedHttpLogLevel()
    // BODY dumps multipart/image bytes into logcat and looks like "base64" noise.
    this.level = when (level) {
        HttpLogLevel.BODY -> LogLevel.HEADERS
        else -> level.toKtor()
    }
    logger = object : Logger {
        override fun log(message: String) {
            if (ApiRuntime.resolvedHttpLogLevel() == HttpLogLevel.NONE) return
            println("${AppConstants.HTTP_LOG_TAG}: $message")
        }
    }
    sanitizeHeader { name -> name.equals(HttpHeaders.Authorization, ignoreCase = true) }
}

fun shouldInstallHttpLogging(): Boolean =
    ApiRuntime.resolvedHttpLogLevel() != HttpLogLevel.NONE
