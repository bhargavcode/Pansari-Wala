package org.bhargav.pansariwala.media

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Platform image compressor (WhatsApp-style): downscale longest edge, encode JPEG,
 * iteratively lower quality until under target size (~90% reduction on large camera shots).
 * Android/iOS also emit a small JPEG thumbnail so the server can store both without re-encoding.
 * Blocking/CPU-bound: call through [prepareImageForUpload] from UI code.
 */
expect fun compressImageForUpload(
    bytes: ByteArray,
    mimeType: String,
    displayName: String,
): CompressedImage

class UnsupportedImageException : IllegalArgumentException("Only PNG, JPEG, or WebP images are allowed")

/** Validates + compresses a picked photo on a background dispatcher so the UI never janks. */
suspend fun prepareImageForUpload(picked: PickedImage): CompressedImage =
    withContext(Dispatchers.Default) {
        require(picked.bytes.isNotEmpty()) { "Empty image" }
        val mime = normalizeImageMime(picked.mimeType, picked.displayName)
            ?: throw UnsupportedImageException()
        compressImageForUpload(picked.bytes, mime, picked.displayName)
    }
