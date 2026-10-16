package org.bhargav.pansariwala.platform

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.bhargav.pansariwala.api.OtpConfigDto
import org.jetbrains.compose.resources.getString
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_cancel_verification
import pansariwala.shared.generated.resources.otp_verify_human_title
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.UIKit.UIApplication
import platform.UIKit.UIModalPresentationFullScreen
import platform.UIKit.UIViewController
import platform.UIKit.UIWindow
import platform.WebKit.WKScriptMessage
import platform.WebKit.WKScriptMessageHandlerProtocol
import platform.WebKit.WKUserContentController
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.darwin.NSObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** In-app reCAPTCHA sheet (WKWebView) whose token the server forwards to Firebase. */
class IosOtpAppVerifier : OtpAppVerifier {

    @OptIn(ExperimentalForeignApi::class)
    override suspend fun proof(e164Phone: String, config: OtpConfigDto, forceRecaptcha: Boolean): OtpAppProof {
        if (config.recaptchaSiteKey.isBlank()) throw OtpAppVerificationException("No reCAPTCHA key from server")
        val html = recaptchaPageHtml(
            config.recaptchaSiteKey,
            getString(Res.string.otp_verify_human_title),
            getString(Res.string.action_cancel_verification),
        )
        val token = withContext(Dispatchers.Main) {
            val presenter = topViewController() ?: throw OtpAppVerificationException("No view controller for reCAPTCHA")
            suspendCancellableCoroutine { cont ->
                val controller = UIViewController(nibName = null, bundle = null)
                val contentController = WKUserContentController()
                val webConfig = WKWebViewConfiguration().apply { userContentController = contentController }
                val webView = WKWebView(frame = CGRectZero.readValue(), configuration = webConfig)
                fun finish(block: () -> Unit) {
                    contentController.removeScriptMessageHandlerForName(RECAPTCHA_BRIDGE)
                    controller.dismissViewControllerAnimated(true, completion = null)
                    block()
                }
                val handler = RecaptchaMessageHandler { value ->
                    if (!cont.isActive) return@RecaptchaMessageHandler
                    finish {
                        if (value == RECAPTCHA_CANCEL_MESSAGE) {
                            cont.resumeWithException(OtpVerificationCancelledException())
                        } else {
                            cont.resume(value)
                        }
                    }
                }
                contentController.addScriptMessageHandler(handler, RECAPTCHA_BRIDGE)
                controller.view = webView
                controller.modalPresentationStyle = UIModalPresentationFullScreen
                webView.loadHTMLString(html, baseURL = NSURL(string = "https://${config.authDomain}"))
                presenter.presentViewController(controller, animated = true, completion = null)
                cont.invokeOnCancellation {
                    platform.darwin.dispatch_async(platform.darwin.dispatch_get_main_queue()) {
                        contentController.removeScriptMessageHandlerForName(RECAPTCHA_BRIDGE)
                        controller.dismissViewControllerAnimated(true, completion = null)
                    }
                }
            }
        }
        return OtpAppProof(recaptchaToken = token)
    }
}

private class RecaptchaMessageHandler(private val onMessage: (String) -> Unit) : NSObject(), WKScriptMessageHandlerProtocol {
    override fun userContentController(
        userContentController: WKUserContentController,
        didReceiveScriptMessage: WKScriptMessage,
    ) {
        onMessage(didReceiveScriptMessage.body as? String ?: "")
    }
}

private fun topViewController(): UIViewController? {
    val window = UIApplication.sharedApplication.windows.mapNotNull { it as? UIWindow }.firstOrNull { it.isKeyWindow() }
    var top = window?.rootViewController
    while (top?.presentedViewController != null) top = top.presentedViewController
    return top
}
