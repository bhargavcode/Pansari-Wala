package org.bhargav.pansariwala.server

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

data class ServerConfig(
    val port: Int,
    val jwtSecret: String,
    val jwtIssuer: String,
    val mongoUri: String,
    val mongoDbName: String,
    val razorpayKeyId: String,
    val razorpayKeySecret: String,
    val razorpayXAccountNumber: String,
    val defaultShopUpi: String,
    val firebaseProjectId: String,
    /** Firebase service-account JSON (inline or loaded from file) for FCM HTTP v1. Empty → push disabled. */
    val fcmServiceAccountJson: String,
    val devAuth: Boolean,
    /** Optional HTTP endpoint that accepts JSON `{ "phone": "10digits", "otp": "123456" }`. */
    val smsApiUrl: String,
    val smsApiToken: String,
    val passwordSalt: String,
    val adminUsername: String,
    val adminPassword: String,
    /** Asset backend id, see [org.bhargav.pansariwala.server.storage.StorageProvider]. */
    val storageProvider: String,
    /** Optional CDN / custom host for public object URLs (no trailing slash). Empty → backend default. */
    val assetPublicBaseUrl: String,
    /** Old public URL prefixes whose trailing path is an object key; inbound values are normalized to keys. */
    val legacyAssetBaseUrls: List<String>,
    val s3Bucket: String,
    val s3Region: String,
    val awsAccessKeyId: String,
    val awsSecretAccessKey: String,
    /** Firebase Web API key used for Identity Toolkit phone auth (send + verify OTP on the server). */
    val firebaseApiKey: String = "",
    val otpTtlSeconds: Int = 300,
    val otpResendCooldownSeconds: Int = 30,
    val otpMaxSendsPerHour: Int = 5,
    val otpMaxVerifyAttempts: Int = 5,
    val otpMaxRequestsPerIpPerHour: Int = 30,
    val cashfreeAppId: String = "",
    val cashfreeSecretKey: String = "",
    /** `sandbox` or `production`. */
    val cashfreeEnv: String = "sandbox",
    /** Public API origin (no trailing slash) Cashfree calls for webhooks / return page. */
    val publicApiBaseUrl: String = "",
) {
    val paymentsEnabled: Boolean get() = razorpayKeyId.isNotBlank() && razorpayKeySecret.isNotBlank()
    val cashfreeEnabled: Boolean get() = cashfreeAppId.isNotBlank() && cashfreeSecretKey.isNotBlank()

    companion object {
        const val DEFAULT_PASSWORD_SALT = "pansari-local-salt"
        /** Same public key as `google-services.json`; Firebase API keys identify the project, they are not secrets. */
        private const val DEFAULT_FIREBASE_API_KEY = "AIzaSyAuh8VVFsquRigomZodOTBb9G-e1TIUZSo"
        private const val ATLAS_USER = "pansariwala"
        private const val ATLAS_HOST = "pansariwala.nl9gm4j.mongodb.net"
        private const val ATLAS_APP = "pansariwala"

        fun fromEnv(): ServerConfig = ServerConfig(
            port = env("PORT", "8080").toInt(),
            jwtSecret = env("JWT_SECRET", "dev-only-change-me-use-64-random-bytes"),
            jwtIssuer = env("JWT_ISSUER", "pansariwala"),
            mongoUri = mongoUriFromEnv(),
            mongoDbName = env("MONGODB_DB", "pansariwala"),
            razorpayKeyId = env("RAZORPAY_KEY_ID", "rzp_test_TQsK8KEvuiZ9hC"),
            razorpayKeySecret = env("RAZORPAY_KEY_SECRET", "qyraiNcwk1EAI3NCpwpZasY5"),
            razorpayXAccountNumber = env("RAZORPAYX_ACCOUNT_NUMBER", ""),
            defaultShopUpi = env("RAZORPAY_TEST_UPI", "success@razorpay"),
            firebaseProjectId = env("FIREBASE_PROJECT_ID", "pansariwala-5f4b4"),
            fcmServiceAccountJson = fcmServiceAccountFromEnv(),
            devAuth = env("AUTH_DEV_MODE", "true").toBooleanStrict(),
            smsApiUrl = env("SMS_API_URL", ""),
            smsApiToken = env("SMS_API_TOKEN", ""),
            passwordSalt = env("PASSWORD_SALT", DEFAULT_PASSWORD_SALT),
            adminUsername = env("ADMIN_USERNAME", "bhargav"),
            adminPassword = env("ADMIN_PASSWORD", ""),
            storageProvider = env("STORAGE_PROVIDER", "s3"),
            assetPublicBaseUrl = env("ASSET_PUBLIC_BASE_URL", env("S3_PUBLIC_BASE_URL", "")),
            legacyAssetBaseUrls = env("LEGACY_ASSET_BASE_URLS", "https://api.pansariwala.shop/uploads")
                .split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() },
            s3Bucket = env("S3_BUCKET", "pansariwala-assets"),
            s3Region = env("AWS_REGION", "ap-south-1"),
            awsAccessKeyId = env("AWS_ACCESS_KEY_ID", ""),
            awsSecretAccessKey = env("AWS_SECRET_ACCESS_KEY", ""),
            firebaseApiKey = env("FIREBASE_API_KEY", DEFAULT_FIREBASE_API_KEY),
            otpTtlSeconds = env("OTP_TTL_SECONDS", "300").toInt(),
            otpResendCooldownSeconds = env("OTP_RESEND_COOLDOWN_SECONDS", "30").toInt(),
            otpMaxSendsPerHour = env("OTP_MAX_SENDS_PER_HOUR", "5").toInt(),
            otpMaxVerifyAttempts = env("OTP_MAX_VERIFY_ATTEMPTS", "5").toInt(),
            otpMaxRequestsPerIpPerHour = env("OTP_MAX_REQUESTS_PER_IP_PER_HOUR", "30").toInt(),
            cashfreeAppId = env("CASHFREE_APP_ID", ""),
            cashfreeSecretKey = env("CASHFREE_SECRET_KEY", ""),
            cashfreeEnv = env("CASHFREE_ENV", "sandbox"),
            publicApiBaseUrl = env("PUBLIC_API_BASE_URL", "").trimEnd('/'),
        )

        private fun mongoUriFromEnv(): String {
            System.getenv("MONGODB_URI")?.takeIf { it.isNotBlank() }?.let { return it }
            val password = System.getenv("MONGODB_PASSWORD")?.takeIf { it.isNotBlank() }
                ?: error("Set MONGODB_URI or MONGODB_PASSWORD for Atlas cluster pansariwala")
            val encoded = URLEncoder.encode(password, StandardCharsets.UTF_8).replace("+", "%20")
            return "mongodb+srv://$ATLAS_USER:$encoded@$ATLAS_HOST/?appName=$ATLAS_APP"
        }

        private fun fcmServiceAccountFromEnv(): String {
            env("FCM_SERVICE_ACCOUNT_JSON", "").takeIf { it.isNotBlank() }?.let { return it }
            val path = env("FCM_SERVICE_ACCOUNT_FILE", env("GOOGLE_APPLICATION_CREDENTIALS", ""))
            if (path.isBlank()) return ""
            val file = java.io.File(path)
            return if (file.isFile) file.readText() else ""
        }

        private fun env(key: String, default: String): String =
            System.getenv(key)?.takeIf { it.isNotBlank() } ?: default
    }
}
