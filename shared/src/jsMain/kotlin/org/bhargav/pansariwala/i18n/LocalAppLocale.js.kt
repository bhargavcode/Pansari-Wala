package org.bhargav.pansariwala.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.intl.Locale
import kotlinx.browser.window

/** Compose resources read `navigator.languages`; this makes them return the in-app language when one is set. */
private val localeOverrideInstalled: Boolean = run {
    js(
        """
        (function () {
          var proto = Navigator.prototype;
          var langs = Object.getOwnPropertyDescriptor(proto, 'languages');
          var lang = Object.getOwnPropertyDescriptor(proto, 'language');
          Object.defineProperty(proto, 'languages', Object.assign({}, langs, {
            get: function () { return window.__pwLocale ? [window.__pwLocale] : langs.get.apply(this); }
          }));
          Object.defineProperty(proto, 'language', Object.assign({}, lang, {
            get: function () { return window.__pwLocale ? window.__pwLocale : lang.get.apply(this); }
          }));
        })()
        """,
    )
    true
}

actual object LocalAppLocale {
    private val localAppLocale = staticCompositionLocalOf { Locale.current.toString() }

    actual val current: String
        @Composable get() = localAppLocale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        check(localeOverrideInstalled)
        window.asDynamic().__pwLocale = value?.replace('_', '-')
        return localAppLocale.provides(Locale.current.toString())
    }
}
