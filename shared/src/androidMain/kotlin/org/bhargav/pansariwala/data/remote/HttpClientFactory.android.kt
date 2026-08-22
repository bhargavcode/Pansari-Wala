package org.bhargav.pansariwala.data.remote

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import org.bhargav.pansariwala.util.AppConstants
import java.util.concurrent.TimeUnit

actual fun createPlatformHttpClient(): HttpClient = HttpClient(OkHttp) {
    engine {
        config {
            connectTimeout(AppConstants.HTTP_CONNECT_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            readTimeout(AppConstants.HTTP_SOCKET_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            writeTimeout(AppConstants.HTTP_SOCKET_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            callTimeout(AppConstants.HTTP_REQUEST_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        }
    }
}
