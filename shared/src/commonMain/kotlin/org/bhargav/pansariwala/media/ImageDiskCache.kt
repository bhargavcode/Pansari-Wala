package org.bhargav.pansariwala.media

/** Raw downloaded image bytes keyed by [imageCacheKey]; no-op on web, where the browser HTTP cache applies. */
internal expect suspend fun readImageDiskCache(key: String): ByteArray?

internal expect suspend fun writeImageDiskCache(key: String, bytes: ByteArray)

/** Stable file-safe key for a URL (FNV-1a 64-bit). */
internal fun imageCacheKey(url: String): String {
    var hash = -0x340d631b7bdddcdbL
    url.forEach { ch ->
        hash = hash xor ch.code.toLong()
        hash *= 0x100000001b3L
    }
    return hash.toULong().toString(16)
}
