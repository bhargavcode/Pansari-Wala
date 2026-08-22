package org.bhargav.pansariwala.platform

import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.product.AppProduct
import org.bhargav.pansariwala.product.AppProductHolder

class IosPhoneAuthGateway(
    private val api: PansariApi,
) : PhoneAuthGateway {
    private val firebaseAuth = IosFirebasePhoneAuth()
    private val serverAuth = ServerPhoneAuthGateway(api)

    @Volatile
    private var preferServerOtp: Boolean? = null

    private suspend fun useServerOtp(): Boolean {
        preferServerOtp?.let { return it }
        if (AppProductHolder.current == AppProduct.USER ||
            AppProductHolder.current == AppProduct.DELIVERY
        ) {
            preferServerOtp = true
            return true
        }
        val devAuth = runCatching { api.publicConfig().devAuth }.getOrNull()
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
