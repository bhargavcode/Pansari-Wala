package org.bhargav.pansariwala.server.storage

import org.bhargav.pansariwala.server.AwsCredentialSource
import org.bhargav.pansariwala.server.ServerConfig
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/** S3 via SigV4 PUT; credentials from env keys or the EC2 instance role. */
class S3AssetStorage(private val config: ServerConfig) : AssetStorage {
    private val http = HttpClient.newBuilder().build()
    private val credentials = AwsCredentialSource(config)
    private val bucket = config.s3Bucket.trim()
    private val region = config.s3Region.trim()
    private val host = "$bucket.s3.$region.amazonaws.com"
    private val virtualHostedBase = "https://$host"

    init {
        require(bucket.isNotBlank()) { "S3_BUCKET is required when STORAGE_PROVIDER=s3" }
        requireNotNull(credentials.current()) {
            "S3 storage needs AWS_ACCESS_KEY_ID/AWS_SECRET_ACCESS_KEY or an EC2 instance role"
        }
    }

    override val provider = StorageProvider.S3

    override val description: String
        get() {
            val auth = if (credentials.current()?.sessionToken.isNullOrBlank()) "access-key" else "instance-role"
            return "s3 bucket=$bucket region=$region auth=$auth publicBase=${publicBase()}"
        }

    override fun publicUrl(key: String): String = "${publicBase()}/${key.trimStart('/')}"

    override fun publicBaseUrls(): List<String> = listOf(
        publicBase(),
        virtualHostedBase,
        "https://$bucket.s3.amazonaws.com",
        "https://s3.$region.amazonaws.com/$bucket",
    ).distinct()

    private fun publicBase(): String = config.assetPublicBaseUrl.trimEnd('/').ifBlank { virtualHostedBase }

    override fun put(key: String, bytes: ByteArray, contentType: String) {
        val session = credentials.current() ?: error("S3 credentials unavailable")
        val amzDate = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
            .withZone(ZoneOffset.UTC)
            .format(Instant.now())
        val dateStamp = amzDate.take(8)
        val payloadHash = sha256Hex(bytes)
        val cacheControl = "public, max-age=31536000, immutable"
        val canonicalUri = "/" + key.split('/').joinToString("/") { uriEncode(it) }
        val headers = sortedMapOf(
            "cache-control" to cacheControl,
            "content-type" to contentType,
            "host" to host,
            "x-amz-content-sha256" to payloadHash,
            "x-amz-date" to amzDate,
        )
        session.sessionToken?.takeIf { it.isNotBlank() }?.let { headers["x-amz-security-token"] = it }
        val canonicalHeaders = headers.entries.joinToString("") { (name, value) -> "$name:$value\n" }
        val signedHeaders = headers.keys.joinToString(";")
        val canonicalRequest = "PUT\n$canonicalUri\n\n$canonicalHeaders\n$signedHeaders\n$payloadHash"
        val credentialScope = "$dateStamp/$region/s3/aws4_request"
        val stringToSign = "AWS4-HMAC-SHA256\n$amzDate\n$credentialScope\n${sha256Hex(canonicalRequest.toByteArray())}"
        val signature = hmacHex(signingKey(dateStamp, session.secretAccessKey), stringToSign)
        val auth =
            "AWS4-HMAC-SHA256 Credential=${session.accessKeyId}/$credentialScope, SignedHeaders=$signedHeaders, Signature=$signature"
        val builder = HttpRequest.newBuilder()
            .uri(URI.create("$virtualHostedBase$canonicalUri"))
            .header("Cache-Control", cacheControl)
            .header("Content-Type", contentType)
            .header("x-amz-content-sha256", payloadHash)
            .header("x-amz-date", amzDate)
            .header("Authorization", auth)
        session.sessionToken?.takeIf { it.isNotBlank() }?.let { builder.header("x-amz-security-token", it) }
        val response = http.send(
            builder.PUT(HttpRequest.BodyPublishers.ofByteArray(bytes)).build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        if (response.statusCode() !in 200..299) {
            error("S3 upload failed (${response.statusCode()}): ${response.body().take(200)}")
        }
    }

    /** Encode one S3 path segment (SigV4); keep unreserved chars. */
    private fun uriEncode(value: String): String = buildString(value.length * 2) {
        for (ch in value) {
            when {
                ch.isLetterOrDigit() || ch in "-._~" -> append(ch)
                else -> append('%').append(ch.code.toString(16).uppercase(Locale.US).padStart(2, '0'))
            }
        }
    }

    private fun signingKey(dateStamp: String, secretAccessKey: String): ByteArray {
        val kDate = hmac(("AWS4" + secretAccessKey).toByteArray(), dateStamp)
        val kRegion = hmac(kDate, region)
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
