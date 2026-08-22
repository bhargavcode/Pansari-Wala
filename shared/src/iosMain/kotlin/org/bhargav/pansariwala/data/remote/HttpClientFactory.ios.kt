package org.bhargav.pansariwala.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import org.bhargav.pansariwala.util.AppConstants

actual fun createPlatformHttpClient(): HttpClient = HttpClient(Darwin) {
    engine {
        configureRequest {
            setTimeoutInterval(AppConstants.HTTP_REQUEST_TIMEOUT_MS / 1_000.0)
        }
    }
}
