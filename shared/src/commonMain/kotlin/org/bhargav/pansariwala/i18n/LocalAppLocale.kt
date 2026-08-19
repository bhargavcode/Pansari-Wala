package org.bhargav.pansariwala.i18n

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * In-app locale override for Compose Multiplatform resources.
 * Platform actuals follow JetBrains resource-environment guidance.
 */
var customAppLocale by mutableStateOf<String?>(null)

expect object LocalAppLocale {
    val current: String
        @Composable get

    @Composable
    infix fun provides(value: String?): ProvidedValue<*>
}

@Composable
fun AppLocaleProvider(
    languageCode: String?,
    content: @Composable () -> Unit,
) {
    customAppLocale = languageCode
    CompositionLocalProvider(LocalAppLocale provides languageCode) {
        key(languageCode) {
            content()
        }
    }
}
