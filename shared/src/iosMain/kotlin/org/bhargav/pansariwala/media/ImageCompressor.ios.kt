package org.bhargav.pansariwala.media

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.useContents
import kotlinx.cinterop.usePinned
import org.bhargav.pansariwala.util.AppConstants
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSData
import platform.Foundation.dataWithBytes
import platform.UIKit.UIGraphicsImageRenderer
import platform.UIKit.UIGraphicsImageRendererFormat
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.posix.memcpy
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalForeignApi::class)
actual fun compressImageForUpload(
    bytes: ByteArray,
    mimeType: String,
    displayName: String,
): CompressedImage {
    val decoded = UIImage(data = bytes.toNSData()) ?: error("Could not decode image")
    val image = resizeImage(decoded, AppConstants.PHOTO_MAX_EDGE_PX)
    val targetBytes = targetByteBudget(bytes.size)
    var quality = AppConstants.PHOTO_JPEG_QUALITY / 100.0
    var out = jpegBytes(image, quality) ?: error("Could not compress image")
    while (out.size > targetBytes && quality > AppConstants.PHOTO_JPEG_QUALITY_MIN / 100.0) {
        quality -= 0.08
        out = jpegBytes(image, quality) ?: out
    }
    val thumb = resizeImage(image, AppConstants.PHOTO_THUMB_EDGE_PX)
    val thumbBytes = jpegBytes(thumb, AppConstants.PHOTO_THUMB_JPEG_QUALITY / 100.0)
    val name = displayName.substringBeforeLast('.').ifBlank { "photo" } + ".jpg"
    return CompressedImage(
        displayName = name,
        bytes = out,
        mimeType = "image/jpeg",
        originalByteCount = bytes.size,
        thumbnailBytes = thumbBytes,
    )
}

/** Downscales to [maxEdge] and bakes in EXIF orientation (renderer is thread-safe). */
@OptIn(ExperimentalForeignApi::class)
internal fun resizeImage(image: UIImage, maxEdge: Int): UIImage {
    val (w, h) = image.size.useContents { width to height }
    val longest = max(w, h)
    val scale = if (longest > maxEdge) maxEdge / longest else 1.0
    val tw = (w * scale).coerceAtLeast(1.0)
    val th = (h * scale).coerceAtLeast(1.0)
    val format = UIGraphicsImageRendererFormat().apply { this.scale = 1.0 }
    val renderer = UIGraphicsImageRenderer(size = CGSizeMake(tw, th), format = format)
    return renderer.imageWithActions { _ -> image.drawInRect(CGRectMake(0.0, 0.0, tw, th)) }
}

private fun targetByteBudget(originalSize: Int): Int {
    val byRatio = (originalSize * AppConstants.PHOTO_TARGET_RATIO).roundToInt()
    return max(AppConstants.PHOTO_TARGET_MAX_BYTES, byRatio).coerceAtMost(AppConstants.PHOTO_TARGET_MAX_BYTES * 2)
}

@OptIn(ExperimentalForeignApi::class)
internal fun jpegBytes(image: UIImage, quality: Double): ByteArray? {
    val data = UIImageJPEGRepresentation(image, quality.coerceIn(0.1, 1.0)) ?: return null
    return data.toByteArray()
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData =
    usePinned { pinned ->
        NSData.dataWithBytes(pinned.addressOf(0), length = size.toULong())
    }

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val count = length.toInt()
    if (count == 0) return ByteArray(0)
    val out = ByteArray(count)
    out.usePinned { pinned ->
        memcpy(pinned.addressOf(0), bytes, count.toULong())
    }
    return out
}
