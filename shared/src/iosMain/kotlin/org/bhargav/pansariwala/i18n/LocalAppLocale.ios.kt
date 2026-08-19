package org.bhargav.pansariwala.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.InternalComposeUiApi
import org.bhargav.pansariwala.util.AppConstants
import platform.Foundation.NSLocale
import platform.Foundation.NSUserDefaults
import platform.Foundation.preferredLanguages

@OptIn(InternalComposeUiApi::class)
actual object LocalAppLocale {
    private val default =
        (NSLocale.preferredLanguages.firstOrNull() as? String) ?: AppConstants.DEFAULT_LANGUAGE
    private val localAppLocale = staticCompositionLocalOf { default }

    actual val current: String
        @Composable get() = localAppLocale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> {
        val new = value ?: default
        if (value == null) {
            NSUserDefaults.standardUserDefaults.removeObjectForKey(AppConstants.IOS_APPLE_LANGUAGES_KEY)
        } else {
            NSUserDefaults.standardUserDefaults.setObject(arrayListOf(new), AppConstants.IOS_APPLE_LANGUAGES_KEY)
        }
        return localAppLocale.provides(new)
    }
}
