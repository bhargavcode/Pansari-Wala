package org.bhargav.pansariwala.platform

/** Message the reCAPTCHA page posts when the user taps its cancel button. */
const val RECAPTCHA_CANCEL_MESSAGE = "__cancel__"

/** JS bridge name (Android `addJavascriptInterface` / iOS `messageHandlers`). */
const val RECAPTCHA_BRIDGE = "pansariOtp"

/**
 * Firebase reCAPTCHA v2 checkbox for in-app WebViews. Load it with the Firebase auth domain as base URL
 * so the token's hostname is one Firebase accepts.
 */
fun recaptchaPageHtml(siteKey: String, title: String, cancelLabel: String): String = """
<!DOCTYPE html>
<html><head>
<meta name="viewport" content="width=device-width, initial-scale=1, maximum-scale=1">
<style>
  body { margin:0; font-family:-apple-system,Roboto,sans-serif; background:#fff; color:#222;
         display:flex; flex-direction:column; align-items:center; justify-content:center; min-height:100vh; }
  h3 { font-weight:600; margin:0 0 20px; }
  button { margin-top:24px; border:0; background:none; color:#1b6e3a; font-size:16px; padding:12px 24px; }
</style>
<script>
  function post(v) {
    if (window.$RECAPTCHA_BRIDGE && window.$RECAPTCHA_BRIDGE.postMessage) { window.$RECAPTCHA_BRIDGE.postMessage(v); return; }
    if (window.webkit && webkit.messageHandlers && webkit.messageHandlers.$RECAPTCHA_BRIDGE) {
      webkit.messageHandlers.$RECAPTCHA_BRIDGE.postMessage(v);
    }
  }
  function onLoad() {
    var reset = function () { grecaptcha.reset(); };
    grecaptcha.render('box', { sitekey: '$siteKey', size: 'normal', callback: post, 'expired-callback': reset, 'error-callback': reset });
  }
</script>
<script src="https://www.google.com/recaptcha/api.js?onload=onLoad&render=explicit" async defer></script>
</head><body>
  <h3>$title</h3>
  <div id="box"></div>
  <button onclick="post('$RECAPTCHA_CANCEL_MESSAGE')">$cancelLabel</button>
</body></html>
""".trimIndent()
