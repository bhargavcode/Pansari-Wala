package org.bhargav.pansariwala.platform

import org.bhargav.pansariwala.api.PansariApi

/**
 * In-app server OTP only. Personal Team builds cannot enable Push Notifications,
 * which Firebase Phone Auth needs for silent APNs verification.
 */
class IosPhoneAuthGateway(
    api: PansariApi,
) : PhoneAuthGateway {
    private val serverAuth = ServerPhoneAuthGateway(api)

    override suspend fun sendOtp(phone: String): Result<PhoneOtpSession> =
        serverAuth.sendOtp(phone)

    override suspend fun verifyOtp(
        phone: String,
        otp: String,
        session: PhoneOtpSession,
    ): Result<PhoneAuthResult> =
        serverAuth.verifyOtp(phone, otp, session)
}
