package org.bhargav.pansariwala.server.service

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.server.ServerConfig

/**
 * Transactional SMS through a generic HTTP gateway: `POST SMS_API_URL` with a bearer `SMS_API_TOKEN`
 * and a JSON body `{ "to": "+91XXXXXXXXXX", "message": "..." }`. Disabled when the URL is not set.
 */
class SmsGateway(private val config: ServerConfig) {
    private val json = Json { encodeDefaults = true }
    private val http = HttpClient(CIO) {
        install(HttpTimeout) {
            connectTimeoutMillis = 8_000
            requestTimeoutMillis = 12_000
            socketTimeoutMillis = 12_000
        }
    }

    val enabled: Boolean get() = config.smsApiUrl.isNotBlank()

    /** Returns false when SMS is not configured or the gateway rejected the message. */
    suspend fun send(phone10: String, message: String): Boolean {
        if (!enabled) {
            println("SMS (not configured) to ******${phone10.takeLast(4)}: $message")
            return false
        }
        return runCatching {
            http.post(config.smsApiUrl) {
                if (config.smsApiToken.isNotBlank()) bearerAuth(config.smsApiToken)
                contentType(ContentType.Application.Json)
                setBody(json.encodeToString(SmsBody(to = "+91$phone10", message = message)))
            }.status.isSuccess()
        }.onFailure { println("WARN: SMS send failed: ${it.message}") }.getOrDefault(false)
    }

    @Serializable
    private data class SmsBody(val to: String, val message: String)
}
