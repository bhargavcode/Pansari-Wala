package org.bhargav.pansariwala.platform

import android.os.Handler
import android.os.Looper
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume
import org.bhargav.pansariwala.util.AppConstants

class AndroidFirebasePhoneAuth : PhoneAuthGateway {
    @Volatile
    private var autoCredential: PhoneAuthCredential? = null

    override suspend fun sendOtp(phone: String): Result<PhoneOtpSession> {
        val activity = AndroidActivityHolder.activity
            ?: return Result.failure(IllegalStateException("Activity is not ready"))
        autoCredential = null
        return suspendCancellableCoroutine { cont ->
            val done = AtomicBoolean(false)
            fun resumeOnce(result: Result<PhoneOtpSession>) {
                if (done.compareAndSet(false, true)) cont.resume(result)
            }
            val handler = Handler(Looper.getMainLooper())
            val timeoutRunnable = Runnable {
                resumeOnce(
                    Result.failure(
                        IllegalStateException(
                            "Firebase phone verification timed out. Install a build signed with pansariwala.jks (SHA registered in Firebase).",
                        ),
                    ),
                )
            }
            handler.postDelayed(timeoutRunnable, AppConstants.OTP_TIMEOUT_SEC * 1_000L)
            cont.invokeOnCancellation { handler.removeCallbacks(timeoutRunnable) }
            fun clearTimeout() {
                handler.removeCallbacks(timeoutRunnable)
            }
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    clearTimeout()
                    autoCredential = credential
                    val sessionId = credential.smsCode ?: "auto"
                    resumeOnce(Result.success(PhoneOtpSession(sessionId = sessionId, usesFirebase = true)))
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    clearTimeout()
                    resumeOnce(Result.failure(e))
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    clearTimeout()
                    resumeOnce(Result.success(PhoneOtpSession(sessionId = verificationId, usesFirebase = true)))
                }
            }
            val options = PhoneAuthOptions.newBuilder(FirebaseAuth.getInstance())
                .setPhoneNumber(e164Phone(phone))
                .setTimeout(AppConstants.OTP_TIMEOUT_SEC, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    override suspend fun verifyOtp(
        phone: String,
        otp: String,
        session: PhoneOtpSession,
    ): Result<PhoneAuthResult> = runCatching {
        val credential = autoCredential
            ?: PhoneAuthProvider.getCredential(session.sessionId, otp)
        val authResult = awaitTask(FirebaseAuth.getInstance().signInWithCredential(credential))
        val token = awaitTask(authResult.user?.getIdToken(false) ?: error("Missing Firebase user"))
        PhoneAuthResult(
            firebaseIdToken = token.token ?: error("Missing Firebase ID token"),
            verifiedPhone = digitsPhone(phone),
        )
    }
}

private suspend fun <T> awaitTask(task: com.google.android.gms.tasks.Task<T>): T =
    suspendCancellableCoroutine { cont ->
        task.addOnCompleteListener { completed ->
            if (completed.isSuccessful) {
                cont.resume(completed.result)
            } else {
                cont.resumeWith(Result.failure(completed.exception ?: IllegalStateException("Firebase task failed")))
            }
        }
    }
