package org.bhargav.pansariwala.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.bhargav.pansariwala.util.AppConstants
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

actual fun compressImageForUpload(
    bytes: ByteArray,
    mimeType: String,
    displayName: String,
): CompressedImage {
    val original = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: error("Could not decode image")
    try {
        val scaled = scaleDown(original, AppConstants.PHOTO_MAX_EDGE_PX)
        val targetBytes = targetByteBudget(bytes.size)
        var quality = AppConstants.PHOTO_JPEG_QUALITY
        var out = encodeJpeg(scaled, quality)
        while (out.size > targetBytes && quality > AppConstants.PHOTO_JPEG_QUALITY_MIN) {
            quality -= 8
            out = encodeJpeg(scaled, quality)
        }
        if (out.size > targetBytes * 1.4) {
            val tighter = scaleDown(scaled, (AppConstants.PHOTO_MAX_EDGE_PX * 0.8).roundToInt())
            if (tighter !== scaled) {
                out = encodeJpeg(tighter, quality.coerceAtLeast(AppConstants.PHOTO_JPEG_QUALITY_MIN))
                if (tighter !== original) tighter.recycle()
            }
        }
        if (scaled !== original) scaled.recycle()
        val name = displayName.substringBeforeLast('.').ifBlank { "photo" } + ".jpg"
        return CompressedImage(
            displayName = name,
            bytes = out,
            mimeType = "image/jpeg",
            originalByteCount = bytes.size,
        )
    } finally {
        original.recycle()
    }
}

private fun targetByteBudget(originalSize: Int): Int {
    val byRatio = (originalSize * AppConstants.PHOTO_TARGET_RATIO).roundToInt()
    return max(AppConstants.PHOTO_TARGET_MAX_BYTES, byRatio).coerceAtMost(AppConstants.PHOTO_TARGET_MAX_BYTES * 2)
}

private fun encodeJpeg(bitmap: Bitmap, quality: Int): ByteArray {
    val bos = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, quality.coerceIn(1, 100), bos)
    return bos.toByteArray()
}

private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
    val longest = max(bitmap.width, bitmap.height)
    if (longest <= maxEdge) return bitmap
    val scale = maxEdge.toFloat() / longest.toFloat()
    return Bitmap.createScaledBitmap(
        bitmap,
        (bitmap.width * scale).toInt().coerceAtLeast(1),
        (bitmap.height * scale).toInt().coerceAtLeast(1),
        true,
    )
}
