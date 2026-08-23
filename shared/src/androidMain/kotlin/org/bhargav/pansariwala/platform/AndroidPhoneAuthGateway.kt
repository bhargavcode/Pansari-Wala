package org.bhargav.pansariwala.platform

import org.bhargav.pansariwala.api.PansariApi

/**
 * In-app server OTP only. Firebase Phone Auth 24+ loads Play Integrity at runtime;
 * sideloaded Android Studio installs fail that check (or crash if the AAR is excluded).
 */
class AndroidPhoneAuthGateway(
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
