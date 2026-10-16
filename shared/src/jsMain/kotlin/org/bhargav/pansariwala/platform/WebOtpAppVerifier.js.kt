package org.bhargav.pansariwala.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import org.bhargav.pansariwala.api.OtpConfigDto
import org.jetbrains.compose.resources.getString
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_cancel_verification
import pansariwala.shared.generated.resources.otp_verify_human_title
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** reCAPTCHA overlay on the page; the site's domain must be in Firebase Auth authorized domains. */
class WebOtpAppVerifier : OtpAppVerifier {
    override suspend fun proof(e164Phone: String, config: OtpConfigDto, forceRecaptcha: Boolean): OtpAppProof {
        if (config.recaptchaSiteKey.isBlank()) throw OtpAppVerificationException("No reCAPTCHA key from server")
        val title = getString(Res.string.otp_verify_human_title)
        val cancel = getString(Res.string.action_cancel_verification)
        val token = suspendCancellableCoroutine { cont ->
            val onToken: (String) -> Unit = { if (cont.isActive) cont.resume(it) }
            val onCancel: () -> Unit = {
                if (cont.isActive) cont.resumeWithException(OtpVerificationCancelledException())
            }
            val onError: () -> Unit = {
                if (cont.isActive) cont.resumeWithException(OtpAppVerificationException("reCAPTCHA failed to load"))
            }
            showRecaptchaOverlay(config.recaptchaSiteKey, title, cancel, onToken, onCancel, onError)
        }
        return OtpAppProof(recaptchaToken = token)
    }
}

private val showRecaptchaOverlay: dynamic = js(
    """(function (siteKey, title, cancelLabel, onToken, onCancel, onError) {
    const overlay = document.createElement('div');
    overlay.style.cssText = 'position:fixed;inset:0;z-index:2147483647;background:rgba(0,0,0,.45);display:flex;align-items:center;justify-content:center;';
    const card = document.createElement('div');
    card.style.cssText = 'background:#fff;border-radius:16px;padding:24px;display:flex;flex-direction:column;align-items:center;font-family:sans-serif;';
    const heading = document.createElement('h3');
    heading.textContent = title;
    heading.style.margin = '0 0 16px';
    const box = document.createElement('div');
    const btn = document.createElement('button');
    btn.textContent = cancelLabel;
    btn.style.cssText = 'margin-top:16px;border:0;background:none;color:#1b6e3a;font-size:16px;cursor:pointer;';
    card.append(heading, box, btn);
    overlay.appendChild(card);
    document.body.appendChild(overlay);
    let done = false;
    const finish = (fn, value) => { if (done) return; done = true; overlay.remove(); fn(value); };
    btn.onclick = () => finish(onCancel);
    const render = () => window.grecaptcha.render(box, {
        sitekey: siteKey,
        callback: (t) => finish(onToken, t),
        'expired-callback': () => window.grecaptcha.reset(),
    });
    if (window.grecaptcha && window.grecaptcha.render) { render(); return; }
    window.__pansariRecaptchaOnLoad = render;
    if (!document.getElementById('pansari-recaptcha')) {
        const s = document.createElement('script');
        s.id = 'pansari-recaptcha';
        s.async = true;
        s.src = 'https://www.google.com/recaptcha/api.js?onload=__pansariRecaptchaOnLoad&render=explicit';
        s.onerror = () => { s.remove(); finish(onError); };
        document.head.appendChild(s);
    }
})""",
)
