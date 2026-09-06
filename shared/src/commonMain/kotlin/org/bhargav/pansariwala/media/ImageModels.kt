package org.bhargav.pansariwala.media

import org.bhargav.pansariwala.util.AppConstants

data class PickedImage(
    val displayName: String,
    val bytes: ByteArray,
    val mimeType: String,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PickedImage) return false
        return displayName == other.displayName &&
            mimeType == other.mimeType &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = displayName.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        return result
    }
}

data class CompressedImage(
    val displayName: String,
    val bytes: ByteArray,
    val mimeType: String,
    val originalByteCount: Int,
) {
    val reductionRatio: Double
        get() = if (originalByteCount <= 0) 0.0 else 1.0 - (bytes.size.toDouble() / originalByteCount.toDouble())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CompressedImage) return false
        return displayName == other.displayName &&
            mimeType == other.mimeType &&
            originalByteCount == other.originalByteCount &&
            bytes.contentEquals(other.bytes)
    }

    override fun hashCode(): Int {
        var result = displayName.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + originalByteCount
        return result
    }
}

data class UploadedImage(
    val url: String,
    val thumbnailUrl: String,
)

sealed class ImageSlotState {
    data object Empty : ImageSlotState()
    data class Compressing(val displayName: String) : ImageSlotState()
    data class Uploading(val displayName: String, val previewBytes: ByteArray) : ImageSlotState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Uploading) return false
            return displayName == other.displayName && previewBytes.contentEquals(other.previewBytes)
        }

        override fun hashCode(): Int = 31 * displayName.hashCode() + previewBytes.contentHashCode()
    }

    data class Ready(
        val url: String,
        val thumbnailUrl: String = url,
        val localPreviewBytes: ByteArray? = null,
    ) : ImageSlotState() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Ready) return false
            return url == other.url &&
                thumbnailUrl == other.thumbnailUrl &&
                (localPreviewBytes?.contentEquals(other.localPreviewBytes ?: ByteArray(0))
                    ?: other.localPreviewBytes == null)
        }

        override fun hashCode(): Int {
            var result = url.hashCode()
            result = 31 * result + thumbnailUrl.hashCode()
            result = 31 * result + (localPreviewBytes?.contentHashCode() ?: 0)
            return result
        }
    }

    data class Failed(
        val message: String,
        val compressed: CompressedImage,
        val prefix: String,
    ) : ImageSlotState()
}

fun normalizeImageMime(mimeType: String, fileName: String): String? {
    val mime = mimeType.lowercase().substringBefore(';').trim()
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when {
        mime in ACCEPTED_IMAGE_MIME || ext in ACCEPTED_IMAGE_EXT -> when {
            mime.contains("png") || ext == "png" -> "image/png"
            mime.contains("webp") || ext == "webp" -> "image/webp"
            else -> "image/jpeg"
        }
        else -> null
    }
}

fun isHttpImageUrl(value: String): Boolean {
    val v = value.trim()
    return v.startsWith("http://", ignoreCase = true) || v.startsWith("https://", ignoreCase = true)
}

private val ACCEPTED_IMAGE_MIME = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")
private val ACCEPTED_IMAGE_EXT = setOf("jpg", "jpeg", "png", "webp")

fun ensureUploadPrefix(prefix: String): String {
    val normalized = prefix.trim().trim('/') + "/"
    require(normalized in AppConstants.ALLOWED_UPLOAD_PREFIXES) {
        "Unsupported upload prefix: $prefix"
    }
    return normalized
}
