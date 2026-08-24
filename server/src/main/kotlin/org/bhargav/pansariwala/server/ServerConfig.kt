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
    val fcmServerKey: String,
    val devAuth: Boolean,
    /** Optional HTTP endpoint that accepts JSON `{ "phone": "10digits", "otp": "123456" }`. */
    val smsApiUrl: String,
    val smsApiToken: String,
    val passwordSalt: String,
    val uploadDir: String,
    val adminUsername: String,
    val adminPassword: String,
    val s3Bucket: String,
    val s3Region: String,
) {
    val paymentsEnabled: Boolean get() = razorpayKeyId.isNotBlank() && razorpayKeySecret.isNotBlank()
    val smsConfigured: Boolean get() = smsApiUrl.isNotBlank()
    val s3Configured: Boolean get() = s3Bucket.isNotBlank()

    companion object {
        const val DEFAULT_PASSWORD_SALT = "pansari-local-salt"
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
            fcmServerKey = env("FCM_SERVER_KEY", ""),
            devAuth = env("AUTH_DEV_MODE", "true").toBooleanStrict(),
            smsApiUrl = env("SMS_API_URL", ""),
            smsApiToken = env("SMS_API_TOKEN", ""),
            passwordSalt = env("PASSWORD_SALT", DEFAULT_PASSWORD_SALT),
            uploadDir = env("UPLOAD_DIR", "./data/uploads"),
            adminUsername = env("ADMIN_USERNAME", "bhargav"),
            adminPassword = env("ADMIN_PASSWORD", ""),
            s3Bucket = env("S3_BUCKET", "pansariwala-assets"),
            s3Region = env("AWS_REGION", "ap-south-1"),
        )

        private fun mongoUriFromEnv(): String {
            System.getenv("MONGODB_URI")?.takeIf { it.isNotBlank() }?.let { return it }
            val password = System.getenv("MONGODB_PASSWORD")?.takeIf { it.isNotBlank() }
                ?: error("Set MONGODB_URI or MONGODB_PASSWORD for Atlas cluster pansariwala")
            val encoded = URLEncoder.encode(password, StandardCharsets.UTF_8).replace("+", "%20")
            return "mongodb+srv://$ATLAS_USER:$encoded@$ATLAS_HOST/?appName=$ATLAS_APP"
        }

        private fun env(key: String, default: String): String =
            System.getenv(key)?.takeIf { it.isNotBlank() } ?: default
    }
}
