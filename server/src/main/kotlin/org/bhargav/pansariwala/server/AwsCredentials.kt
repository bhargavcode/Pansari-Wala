package org.bhargav.pansariwala.server

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant

data class AwsSession(
    val accessKeyId: String,
    val secretAccessKey: String,
    val sessionToken: String?,
)

/** Static env keys, or the EC2 instance role via IMDSv2. */
class AwsCredentialSource(private val config: ServerConfig) {
    private val http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(1)).build()
    private val lock = Any()
    private var cached: AwsSession? = null
    private var expiresAt: Instant = Instant.EPOCH

    fun current(): AwsSession? {
        if (config.awsAccessKeyId.isNotBlank() && config.awsSecretAccessKey.isNotBlank()) {
            return AwsSession(config.awsAccessKeyId, config.awsSecretAccessKey, null)
        }
        synchronized(lock) {
            if (cached != null && Instant.now().isBefore(expiresAt.minusSeconds(300))) return cached
            val fresh = fetchInstanceRole() ?: return null
            cached = fresh.first
            expiresAt = fresh.second
            return cached
        }
    }

    private fun fetchInstanceRole(): Pair<AwsSession, Instant>? {
        return try {
            val token = imds(
                "http://169.254.169.254/latest/api/token",
                put = true,
                token = null,
            ) ?: return null
            val role = imds(
                "http://169.254.169.254/latest/meta-data/iam/security-credentials/",
                put = false,
                token = token,
            )?.lineSequence()?.firstOrNull { it.isNotBlank() } ?: return null
            val body = imds(
                "http://169.254.169.254/latest/meta-data/iam/security-credentials/$role",
                put = false,
                token = token,
            ) ?: return null
            val access = jsonField(body, "AccessKeyId") ?: return null
            val secret = jsonField(body, "SecretAccessKey") ?: return null
            val session = jsonField(body, "Token") ?: return null
            val expiration = jsonField(body, "Expiration")?.let { Instant.parse(it) }
                ?: Instant.now().plusSeconds(3600)
            AwsSession(access, secret, session) to expiration
        } catch (_: Exception) {
            null
        }
    }

    private fun imds(url: String, put: Boolean, token: String?): String? {
        val builder = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .timeout(Duration.ofSeconds(2))
        if (put) {
            builder.header("X-aws-ec2-metadata-token-ttl-seconds", "21600")
            builder.PUT(HttpRequest.BodyPublishers.noBody())
        } else {
            token?.let { builder.header("X-aws-ec2-metadata-token", it) }
            builder.GET()
        }
        val response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() != 200) return null
        return response.body().trim().ifBlank { null }
    }

    private fun jsonField(body: String, key: String): String? =
        Regex("\"$key\"\\s*:\\s*\"([^\"]*)\"").find(body)?.groupValues?.get(1)
}
