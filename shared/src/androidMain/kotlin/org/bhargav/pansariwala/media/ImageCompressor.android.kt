package org.bhargav.pansariwala.media

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import org.bhargav.pansariwala.util.AppConstants
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

actual fun compressImageForUpload(
    bytes: ByteArray,
    mimeType: String,
    displayName: String,
): CompressedImage {
    val decoded = decodeSampled(bytes, AppConstants.PHOTO_MAX_EDGE_PX)
    val oriented = applyExifOrientation(decoded, bytes)
    val scaled = scaleDown(oriented, AppConstants.PHOTO_MAX_EDGE_PX)
    try {
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
                tighter.recycle()
            }
        }
        val thumb = scaleDown(scaled, AppConstants.PHOTO_THUMB_EDGE_PX)
        val thumbBytes = encodeJpeg(thumb, AppConstants.PHOTO_THUMB_JPEG_QUALITY)
        if (thumb !== scaled) thumb.recycle()
        val name = displayName.substringBeforeLast('.').ifBlank { "photo" } + ".jpg"
        return CompressedImage(
            displayName = name,
            bytes = out,
            mimeType = "image/jpeg",
            originalByteCount = bytes.size,
            thumbnailBytes = thumbBytes,
        )
    } finally {
        if (scaled !== oriented) scaled.recycle()
        if (oriented !== decoded) oriented.recycle()
        decoded.recycle()
    }
}

/** Decodes at a power-of-two sample size so a 12MP camera shot never lands fully in memory. */
private fun decodeSampled(bytes: ByteArray, maxEdge: Int): Bitmap {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Could not decode image" }
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
    val opts = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
        ?: error("Could not decode image")
}

private fun applyExifOrientation(bitmap: Bitmap, bytes: ByteArray): Bitmap {
    val orientation = runCatching {
        ExifInterface(ByteArrayInputStream(bytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.preScale(-1f, 1f) }
        ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.preScale(-1f, 1f) }
        else -> return bitmap
    }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
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
