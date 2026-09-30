package org.bhargav.pansariwala.api

import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.LoggingConfig
import io.ktor.http.HttpHeaders
import org.bhargav.pansariwala.util.AppConstants

private const val HTTP_LOG_BODY_MAX_CHARS = 8_000

fun LoggingConfig.installPansariHttpLogging() {
    val level = ApiRuntime.resolvedHttpLogLevel()
    this.level = level.toKtor()
    logger = object : Logger {
        override fun log(message: String) {
            if (ApiRuntime.resolvedHttpLogLevel() == HttpLogLevel.NONE) return
            val line = sanitizeHttpLogMessage(message) ?: return
            println("${AppConstants.HTTP_LOG_TAG}: $line")
        }
    }
    sanitizeHeader { name -> name.equals(HttpHeaders.Authorization, ignoreCase = true) }
}

fun shouldInstallHttpLogging(): Boolean =
    ApiRuntime.resolvedHttpLogLevel() != HttpLogLevel.NONE

/** Drop multipart/image bytes; keep JSON response bodies. */
internal fun sanitizeHttpLogMessage(message: String): String? {
    val trimmed = message.trimStart()
    if (trimmed.isEmpty()) return message
    if (looksLikeBinaryOrMultipartBody(trimmed)) {
        return "[body omitted: binary/multipart, ${message.length} chars]"
    }
    if (message.length <= HTTP_LOG_BODY_MAX_CHARS) return message
    return message.take(HTTP_LOG_BODY_MAX_CHARS) +
        "… [truncated ${message.length - HTTP_LOG_BODY_MAX_CHARS} chars]"
}

private fun looksLikeBinaryOrMultipartBody(trimmed: String): Boolean {
    if (trimmed.startsWith("--") && trimmed.contains("Content-Disposition:", ignoreCase = true)) {
        return true
    }
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) return false
    if (trimmed.length < 256) return false
    var control = 0
    val sample = trimmed.take(512)
    for (ch in sample) {
        if (ch.code < 0x20 && ch != '\n' && ch != '\r' && ch != '\t') control++
    }
    return control > sample.length / 20
}
