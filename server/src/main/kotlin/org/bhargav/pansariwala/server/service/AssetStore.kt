package org.bhargav.pansariwala.server.service

import org.bhargav.pansariwala.server.dto.UploadResultDto
import org.bhargav.pansariwala.server.storage.AssetRefs
import org.bhargav.pansariwala.server.storage.AssetStorage
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.Locale
import java.util.UUID
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.ImageWriter
import kotlin.math.max
import kotlin.math.roundToInt

class AssetStore(private val storage: AssetStorage) {
    private val allowedMime = setOf("image/jpeg", "image/jpg", "image/png", "image/webp")

    fun save(
        prefix: String,
        originalName: String,
        bytes: ByteArray,
        contentType: String,
        clientThumbnail: ByteArray? = null,
    ): UploadResultDto {
        require(bytes.isNotEmpty()) { "Empty file" }
        require(bytes.size <= 12_000_000) { "File too large" }
        val mime = contentType.lowercase(Locale.US).substringBefore(';').trim()
        require(mime in allowedMime) { "Only PNG, JPEG, or WebP images are allowed" }
        val safePrefix = normalizePrefix(prefix)
        val id = UUID.randomUUID().toString().replace("-", "")
        val key = "${safePrefix}$id.jpg"
        val thumbKey = "${safePrefix}$id-thumb.jpg"
        val preCompressed = clientThumbnail != null &&
            bytes.size <= CLIENT_MAX_BYTES &&
            clientThumbnail.size <= CLIENT_THUMB_MAX_BYTES &&
            isJpeg(bytes) && isJpeg(clientThumbnail)
        val compressed = if (preCompressed) bytes else compressForUpload(bytes)
        val thumbBytes = if (preCompressed) clientThumbnail!! else thumbnailJpeg(compressed)

        storage.put(key, compressed, "image/jpeg")
        storage.put(thumbKey, thumbBytes, "image/jpeg")
        return UploadResultDto(
            url = storage.publicUrl(key),
            thumbnailUrl = storage.publicUrl(thumbKey),
            key = key,
            thumbnailKey = thumbKey,
        )
    }

    private fun isJpeg(data: ByteArray): Boolean =
        data.size > 3 && data[0] == 0xFF.toByte() && data[1] == 0xD8.toByte() && data[2] == 0xFF.toByte()

    private fun normalizePrefix(prefix: String): String {
        val normalized = prefix.trim().trim('/') + "/"
        require(normalized in AssetRefs.ALLOWED_PREFIXES) { "Unsupported upload prefix: $prefix" }
        return normalized
    }

    /** WhatsApp-style: max edge 1600, JPEG ~82 down to 55, target ~100KB / ~90% reduction. */
    private fun compressForUpload(bytes: ByteArray): ByteArray {
        return try {
            val src = ImageIO.read(ByteArrayInputStream(bytes)) ?: return bytes
            val maxEdge = 1600
            val scale = minOf(1.0, maxEdge.toDouble() / max(src.width, src.height))
            var w = (src.width * scale).toInt().coerceAtLeast(1)
            var h = (src.height * scale).toInt().coerceAtLeast(1)
            var quality = 0.82f
            val target = max(100_000, (bytes.size * 0.10).roundToInt()).coerceAtMost(200_000)
            var out = encodeJpeg(scaleRgb(src, w, h), quality)
            while (out.size > target && quality > 0.55f) {
                quality -= 0.08f
                out = encodeJpeg(scaleRgb(src, w, h), quality)
            }
            if (out.size > target * 1.4) {
                w = (w * 0.8).roundToInt().coerceAtLeast(1)
                h = (h * 0.8).roundToInt().coerceAtLeast(1)
                out = encodeJpeg(scaleRgb(src, w, h), quality.coerceAtLeast(0.55f))
            }
            out
        } catch (_: Exception) {
            bytes
        }
    }

    private fun scaleRgb(src: BufferedImage, w: Int, h: Int): BufferedImage {
        val scaled = src.getScaledInstance(w, h, Image.SCALE_SMOOTH)
        val out = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        g.drawImage(scaled, 0, 0, null)
        g.dispose()
        return out
    }

    private fun encodeJpeg(image: BufferedImage, quality: Float): ByteArray {
        val writers = ImageIO.getImageWritersByFormatName("jpg")
        if (!writers.hasNext()) {
            return ByteArrayOutputStream().use { bos ->
                ImageIO.write(image, "jpg", bos)
                bos.toByteArray()
            }
        }
        val writer: ImageWriter = writers.next()
        val param = writer.defaultWriteParam
        if (param.canWriteCompressed()) {
            param.compressionMode = ImageWriteParam.MODE_EXPLICIT
            param.compressionQuality = quality.coerceIn(0.1f, 1f)
        }
        return ByteArrayOutputStream().use { bos ->
            val ios = ImageIO.createImageOutputStream(bos)
            writer.output = ios
            writer.write(null, IIOImage(image, null, null), param)
            ios.close()
            writer.dispose()
            bos.toByteArray()
        }
    }

    private companion object {
        /** Client-compressed uploads above these sizes are re-encoded server-side. */
        const val CLIENT_MAX_BYTES = 1_500_000
        const val CLIENT_THUMB_MAX_BYTES = 150_000
    }

    private fun thumbnailJpeg(bytes: ByteArray): ByteArray {
        return try {
            val src = ImageIO.read(ByteArrayInputStream(bytes)) ?: return bytes
            val max = 256
            val scale = minOf(1.0, max.toDouble() / max(src.width, src.height))
            val w = (src.width * scale).toInt().coerceAtLeast(1)
            val h = (src.height * scale).toInt().coerceAtLeast(1)
            encodeJpeg(scaleRgb(src, w, h), 0.75f)
        } catch (_: Exception) {
            bytes
        }
    }
}
