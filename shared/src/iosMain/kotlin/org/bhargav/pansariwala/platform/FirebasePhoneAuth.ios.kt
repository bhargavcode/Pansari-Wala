package org.bhargav.pansariwala.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

interface IosOtpSendCallback {
    fun onSuccess(verificationId: String)
    fun onFailure(message: String)
}

interface IosOtpVerifyCallback {
    fun onSuccess(idToken: String)
    fun onFailure(message: String)
}

interface IosPhoneOtpHost {
    fun sendOtp(e164Phone: String, callback: IosOtpSendCallback)
    fun verifyOtp(verificationId: String, code: String, callback: IosOtpVerifyCallback)
}

object IosFirebaseAuthBridge {
    var host: IosPhoneOtpHost? = null
}

class IosFirebasePhoneAuth : PhoneAuthGateway {
    override suspend fun sendOtp(phone: String): Result<PhoneOtpSession> {
        val host = IosFirebaseAuthBridge.host
            ?: return Result.failure(IllegalStateException("Firebase is not configured"))
        return suspendCancellableCoroutine { cont ->
            host.sendOtp(
                e164Phone(phone),
                object : IosOtpSendCallback {
                    override fun onSuccess(verificationId: String) {
                        cont.resume(Result.success(PhoneOtpSession(verificationId, usesFirebase = true)))
                    }

                    override fun onFailure(message: String) {
                        cont.resume(Result.failure(IllegalStateException(message)))
                    }
                },
            )
        }
    }

    override suspend fun verifyOtp(
        phone: String,
        otp: String,
        session: PhoneOtpSession,
    ): Result<PhoneAuthResult> {
        val host = IosFirebaseAuthBridge.host
            ?: return Result.failure(IllegalStateException("Firebase is not configured"))
        return suspendCancellableCoroutine { cont ->
            host.verifyOtp(
                session.sessionId,
                otp,
                object : IosOtpVerifyCallback {
                    override fun onSuccess(idToken: String) {
                        cont.resume(
                            Result.success(
                                PhoneAuthResult(firebaseIdToken = idToken, verifiedPhone = digitsPhone(phone)),
                            ),
                        )
                    }

                    override fun onFailure(message: String) {
                        cont.resume(Result.failure(IllegalStateException(message)))
                    }
                },
            )
        }
    }
}
