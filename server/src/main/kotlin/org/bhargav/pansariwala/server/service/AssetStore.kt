package org.bhargav.pansariwala.server.service

import org.bhargav.pansariwala.server.ServerConfig
import org.bhargav.pansariwala.server.dto.UploadResultDto
import java.awt.Image
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.imageio.ImageIO

class AssetStore(private val config: ServerConfig) {
    private val http = HttpClient.newBuilder().build()

    fun save(prefix: String, originalName: String, bytes: ByteArray, contentType: String): UploadResultDto {
        val safePrefix = prefix.trim('/').ifBlank { "master/product-images" } + "/"
        val ext = extension(originalName, contentType)
        val id = UUID.randomUUID().toString().replace("-", "")
        val key = "$safePrefix$id$ext"
        val thumbKey = "$safePrefix$id-thumb.jpg"
        val thumbBytes = thumbnailJpeg(bytes)

        return if (config.s3Configured) {
            putS3(key, bytes, contentType)
            putS3(thumbKey, thumbBytes, "image/jpeg")
            val base = "https://${config.s3Bucket}.s3.${config.s3Region}.amazonaws.com/"
            UploadResultDto(url = base + key, thumbnailUrl = base + thumbKey)
        } else {
            writeLocal(key, bytes)
            writeLocal(thumbKey, thumbBytes)
            val base = config.publicBaseUrl.trimEnd('/') + "/uploads/"
            UploadResultDto(url = base + key, thumbnailUrl = base + thumbKey)
        }
    }

    private fun writeLocal(key: String, bytes: ByteArray) {
        val file = File(config.uploadDir, key)
        file.parentFile?.mkdirs()
        file.writeBytes(bytes)
    }

    private fun extension(name: String, contentType: String): String {
        val fromName = name.substringAfterLast('.', "").lowercase(Locale.US)
        if (fromName in setOf("jpg", "jpeg", "png", "webp", "gif")) return ".$fromName"
        return when {
            contentType.contains("png") -> ".png"
            contentType.contains("webp") -> ".webp"
            contentType.contains("gif") -> ".gif"
            else -> ".jpg"
        }
    }

    private fun thumbnailJpeg(bytes: ByteArray): ByteArray {
        return try {
            val src = ImageIO.read(ByteArrayInputStream(bytes)) ?: return bytes
            val max = 256
            val scale = minOf(1.0, max.toDouble() / maxOf(src.width, src.height))
            val w = (src.width * scale).toInt().coerceAtLeast(1)
            val h = (src.height * scale).toInt().coerceAtLeast(1)
            val scaled = src.getScaledInstance(w, h, Image.SCALE_SMOOTH)
            val out = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
            val g = out.createGraphics()
            g.drawImage(scaled, 0, 0, null)
            g.dispose()
            ByteArrayOutputStream().use { bos ->
                ImageIO.write(out, "jpg", bos)
                bos.toByteArray()
            }
        } catch (_: Exception) {
            bytes
        }
    }

    private fun putS3(key: String, bytes: ByteArray, contentType: String) {
        val host = "${config.s3Bucket}.s3.${config.s3Region}.amazonaws.com"
        val amzDate = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())
        val dateStamp = amzDate.take(8)
        val payloadHash = sha256Hex(bytes)
        val canonicalHeaders =
            "content-type:$contentType\nhost:$host\nx-amz-content-sha256:$payloadHash\nx-amz-date:$amzDate\n"
        val signedHeaders = "content-type;host;x-amz-content-sha256;x-amz-date"
        val canonicalRequest = "PUT\n/${key}\n\n$canonicalHeaders\n$signedHeaders\n$payloadHash"
        val credentialScope = "$dateStamp/${config.s3Region}/s3/aws4_request"
        val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n${sha256Hex(canonicalRequest.toByteArray())}"
        val signature = hmacHex(signingKey(dateStamp), stringToSign)
        val auth =
            "AWS4-HMAC-SHA256 Credential=${config.awsAccessKeyId}/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"
        val request = HttpRequest.newBuilder()
            .uri(URI.create("https://$host/$key"))
            .header("Content-Type", contentType)
            .header("x-amz-content-sha256", payloadHash)
            .header("x-amz-date", amzDate)
            .header("Authorization", auth)
            .PUT(HttpRequest.BodyPublishers.ofByteArray(bytes))
            .build()
        val response = http.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            error("S3 upload failed (${response.statusCode()}): ${response.body().take(200)}")
        }
    }

    private fun signingKey(dateStamp: String): ByteArray {
        val kDate = hmac(("AWS4" + config.awsSecretAccessKey).toByteArray(), dateStamp)
        val kRegion = hmac(kDate, config.s3Region)
        val kService = hmac(kRegion, "s3")
        return hmac(kService, "aws4_request")
    }

    private fun hmac(key: ByteArray, data: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data.toByteArray(Charsets.UTF_8))
    }

    private fun hmacHex(key: ByteArray, data: String): String =
        hmac(key, data).joinToString("") { "%02x".format(it) }

    private fun sha256Hex(data: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256").digest(data)
        return digest.joinToString("") { "%02x".format(it) }
    }
}
