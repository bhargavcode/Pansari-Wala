package org.bhargav.pansariwala.media

import org.bhargav.pansariwala.util.AppConstants
import kotlin.math.max
import kotlin.math.roundToInt

actual fun compressImageForUpload(
    bytes: ByteArray,
    mimeType: String,
    displayName: String,
): CompressedImage {
    val target = targetByteBudget(bytes.size)
    val name = displayName.substringBeforeLast('.').ifBlank { "photo" } +
        if (mimeType.contains("png")) ".png" else if (mimeType.contains("webp")) ".webp" else ".jpg"
    val out = if (bytes.size <= target * 2) bytes else bytes
    return CompressedImage(
        displayName = name,
        bytes = out,
        mimeType = normalizeImageMime(mimeType, name) ?: "image/jpeg",
        originalByteCount = bytes.size,
    )
}

private fun targetByteBudget(originalSize: Int): Int {
    val byRatio = (originalSize * AppConstants.PHOTO_TARGET_RATIO).roundToInt()
    return max(AppConstants.PHOTO_TARGET_MAX_BYTES, byRatio).coerceAtMost(AppConstants.PHOTO_TARGET_MAX_BYTES * 2)
}
