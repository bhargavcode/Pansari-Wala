package org.bhargav.pansariwala.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.data.local.AppPreferences

object ApiConfig {
    /** Replace with real backend URL in a later phase. */
    const val BASE_URL = "https://api.example.com/"
    const val USE_DEMO_AUTH = true
}

expect fun createPlatformHttpClient(): HttpClient

fun createHttpClient(preferences: AppPreferences): HttpClient {
    val platformClient = createPlatformHttpClient()
    return platformClient.config {
        expectSuccess = false
        install(ContentNegotiation) {
            json(
                Json {
                    ignoreUnknownKeys = true
                    isLenient = true
                    prettyPrint = false
                },
            )
        }
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    println("Ktor: $message")
                }
            }
            level = LogLevel.INFO
        }
        install(Auth) {
            bearer {
                loadTokens {
                    val token = preferences.getAccessToken() ?: return@loadTokens null
                    BearerTokens(token, "")
                }
            }
        }
        defaultRequest {
            url(ApiConfig.BASE_URL)
            contentType(ContentType.Application.Json)
            header(HttpHeaders.Accept, ContentType.Application.Json.toString())
        }
    }
}
