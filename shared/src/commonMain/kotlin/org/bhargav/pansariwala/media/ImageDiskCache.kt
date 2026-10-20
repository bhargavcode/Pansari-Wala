package org.bhargav.pansariwala.media

import io.ktor.http.encodeURLParameter
import org.bhargav.pansariwala.api.ApiRuntime
import org.bhargav.pansariwala.util.AppConstants

/** Raw downloaded image bytes keyed by [imageCacheKey]; no-op on web, where the browser HTTP cache applies. */
internal expect suspend fun readImageDiskCache(key: String): ByteArray?

internal expect suspend fun writeImageDiskCache(key: String, bytes: ByteArray)

/** Browsers block cross-origin image bytes without CORS headers, so web fetches go through the API proxy. */
internal expect val imageFetchViaProxy: Boolean

/** URL to download [url] from on this platform. */
fun imageFetchUrl(url: String): String {
    if (!imageFetchViaProxy) return url
    val base = ApiRuntime.baseUrl.trimEnd('/')
    if (url.startsWith(base)) return url
    return base + AppConstants.IMAGE_PROXY_PATH + "?url=" + url.encodeURLParameter()
}

/** Stable file-safe key for a URL (FNV-1a 64-bit). */
internal fun imageCacheKey(url: String): String {
    var hash = -0x340d631b7bdddcdbL
    url.forEach { ch ->
        hash = hash xor ch.code.toLong()
        hash *= 0x100000001b3L
    }
    return hash.toULong().toString(16)
}
