package org.bhargav.pansariwala.platform

import org.bhargav.pansariwala.api.PansariApi

/**
 * Uses server OTP when the API reports [devAuth] (AUTH_DEV_MODE); otherwise Firebase phone auth.
 * Release builds signed with the production keystore need that SHA-1/SHA-256 in Firebase.
 */
class AndroidPhoneAuthGateway(
    private val api: PansariApi,
) : PhoneAuthGateway {
    private val firebaseAuth = AndroidFirebasePhoneAuth()
    private val serverAuth = ServerPhoneAuthGateway(api)

    @Volatile
    private var preferServerOtp: Boolean? = null

    private suspend fun useServerOtp(): Boolean {
        preferServerOtp?.let { return it }
        val devAuth = runCatching { api.publicConfig().devAuth }.getOrNull()
        // Fail closed to server OTP if config is unreachable (keeps local/dev usable).
        val useServer = devAuth ?: true
        preferServerOtp = useServer
        return useServer
    }

    override suspend fun sendOtp(phone: String): Result<PhoneOtpSession> =
        if (useServerOtp()) serverAuth.sendOtp(phone) else firebaseAuth.sendOtp(phone)

    override suspend fun verifyOtp(
        phone: String,
        otp: String,
        session: PhoneOtpSession,
    ): Result<PhoneAuthResult> =
        if (session.usesFirebase) {
            firebaseAuth.verifyOtp(phone, otp, session)
        } else {
            serverAuth.verifyOtp(phone, otp, session)
        }
}
