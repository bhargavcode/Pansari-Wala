package org.bhargav.pansariwala.server.routing

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import org.bhargav.pansariwala.server.dto.ApiErrorBody
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private const val MAX_IMAGE_BYTES = 12_000_000
private const val CACHE_CONTROL = "public, max-age=604800"

private val proxyClient: HttpClient = HttpClient.newBuilder()
    .connectTimeout(Duration.ofSeconds(10))
    .followRedirects(HttpClient.Redirect.NEVER)
    .build()

/**
 * Re-serves public http(s) images with CORS headers so browser clients can decode them
 * (S3 and third-party hosts do not send Access-Control-Allow-Origin).
 */
fun Route.imageProxyRoutes() {
    get("/media/image") {
        val raw = call.request.queryParameters["url"].orEmpty().trim()
        val uri = runCatching { URI(raw) }.getOrNull()
        if (uri == null || uri.scheme?.lowercase() !in setOf("http", "https") || uri.host.isNullOrBlank()) {
            call.respond(HttpStatusCode.BadRequest, ApiErrorBody("Invalid image URL"))
            return@get
        }
        val publicHost = runCatching { InetAddress.getAllByName(uri.host).none { it.isInternal() } }.getOrDefault(false)
        if (!publicHost) {
            call.respond(HttpStatusCode.BadRequest, ApiErrorBody("Image host not allowed"))
            return@get
        }
        val response = runCatching {
            proxyClient.send(
                HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(20))
                    .header("User-Agent", "PansariWala-image-proxy/1.0")
                    .header("Accept", "image/*")
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofInputStream(),
            )
        }.getOrNull()
        if (response == null || response.statusCode() !in 200..299) {
            response?.body()?.close()
            call.respond(HttpStatusCode.BadGateway, ApiErrorBody("Image unavailable"))
            return@get
        }
        val type = response.headers().firstValue("Content-Type").orElse("").substringBefore(';').trim().lowercase()
        if (!type.startsWith("image/")) {
            response.body().close()
            call.respond(HttpStatusCode.UnsupportedMediaType, ApiErrorBody("Not an image"))
            return@get
        }
        val bytes = response.body().use { input ->
            val out = ByteArrayOutputStream()
            val buffer = ByteArray(16 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                out.write(buffer, 0, n)
                if (out.size() > MAX_IMAGE_BYTES) return@use null
            }
            out.toByteArray()
        }
        if (bytes == null) {
            call.respond(HttpStatusCode.PayloadTooLarge, ApiErrorBody("Image too large"))
            return@get
        }
        call.response.header(HttpHeaders.CacheControl, CACHE_CONTROL)
        call.respondBytes(bytes, ContentType.parse(type))
    }
}

private fun InetAddress.isInternal(): Boolean =
    isAnyLocalAddress || isLoopbackAddress || isLinkLocalAddress || isSiteLocalAddress || isMulticastAddress ||
        (address.size == 16 && (address[0].toInt() and 0xFE) == 0xFC)
