package org.bhargav.pansariwala.platform

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Base64
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.bhargav.pansariwala.api.OtpConfigDto
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.getString
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_cancel_verification
import pansariwala.shared.generated.resources.otp_verify_human_title
import java.security.MessageDigest
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Play Integrity proof (no UI); falls back to an in-app reCAPTCHA dialog, never the external browser. */
class AndroidOtpAppVerifier(private val context: Context) : OtpAppVerifier {

    override suspend fun proof(e164Phone: String, config: OtpConfigDto, forceRecaptcha: Boolean): OtpAppProof {
        val identity = OtpAppProof(androidPackage = context.packageName, androidCertSha1 = signingCertSha1())
        if (!forceRecaptcha) {
            val token = runCatching { playIntegrityToken(e164Phone) }
                .onFailure { if (it is CancellationException) throw it }
                .getOrNull()
            if (token != null) return identity.copy(playIntegrityToken = token)
        }
        if (config.recaptchaSiteKey.isBlank()) throw OtpAppVerificationException("Play Integrity unavailable and no reCAPTCHA key")
        return identity.copy(recaptchaToken = recaptchaToken(config))
    }

    /** Firebase expects the nonce to be SHA-256 of the E.164 phone number. */
    private suspend fun playIntegrityToken(e164Phone: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(e164Phone.toByteArray(Charsets.UTF_8))
        val nonce = Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP)
        val request = IntegrityTokenRequest.builder()
            .setNonce(nonce)
            .setCloudProjectNumber(AppConstants.FIREBASE_PROJECT_NUMBER)
            .build()
        val manager = IntegrityManagerFactory.create(context.applicationContext)
        return suspendCancellableCoroutine { cont ->
            manager.requestIntegrityToken(request)
                .addOnSuccessListener { if (cont.isActive) cont.resume(it.token()) }
                .addOnFailureListener { if (cont.isActive) cont.resumeWithException(it) }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun recaptchaToken(config: OtpConfigDto): String {
        val title = getString(Res.string.otp_verify_human_title)
        val cancel = getString(Res.string.action_cancel_verification)
        val html = recaptchaPageHtml(config.recaptchaSiteKey, title, cancel)
        return withContext(Dispatchers.Main) {
            val activity = AndroidActivityHolder.activity
                ?: throw OtpAppVerificationException("No activity for reCAPTCHA")
            suspendCancellableCoroutine { cont ->
                val dialog = Dialog(activity, android.R.style.Theme_DeviceDefault_Light_NoActionBar)
                val webView = WebView(activity)
                webView.settings.javaScriptEnabled = true
                webView.settings.domStorageEnabled = true
                webView.addJavascriptInterface(
                    object {
                        @JavascriptInterface
                        fun postMessage(value: String) {
                            webView.post {
                                if (!cont.isActive) return@post
                                dialog.dismiss()
                                if (value == RECAPTCHA_CANCEL_MESSAGE) {
                                    cont.resumeWithException(OtpVerificationCancelledException())
                                } else {
                                    cont.resume(value)
                                }
                            }
                        }
                    },
                    RECAPTCHA_BRIDGE,
                )
                dialog.setContentView(
                    webView,
                    ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
                )
                dialog.setOnCancelListener {
                    if (cont.isActive) cont.resumeWithException(OtpVerificationCancelledException())
                }
                webView.loadDataWithBaseURL("https://${config.authDomain}", html, "text/html", "utf-8", null)
                dialog.show()
                cont.invokeOnCancellation {
                    webView.post {
                        dialog.dismiss()
                        webView.destroy()
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun signingCertSha1(): String? = runCatching {
        val pm = context.packageManager
        val certs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
                .signingInfo?.apkContentsSigners
        } else {
            pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES).signatures
        }
        val cert = certs?.firstOrNull() ?: return@runCatching null
        MessageDigest.getInstance("SHA-1").digest(cert.toByteArray()).joinToString("") { "%02X".format(it) }
    }.getOrNull()
}
