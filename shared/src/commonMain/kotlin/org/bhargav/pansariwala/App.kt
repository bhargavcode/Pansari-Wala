package org.bhargav.pansariwala

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.i18n.AppLocaleProvider
import org.bhargav.pansariwala.navigation.AppNavGraph
import org.bhargav.pansariwala.navigation.DeliveryNavGraph
import org.bhargav.pansariwala.navigation.UserNavGraph
import org.bhargav.pansariwala.notification.LiveAlerts
import org.bhargav.pansariwala.notification.NotificationGateway
import org.bhargav.pansariwala.platform.PartnerLocationTracker
import org.bhargav.pansariwala.product.AppProduct
import org.bhargav.pansariwala.product.currentAppProduct
import org.bhargav.pansariwala.settings.AppUserSettings
import org.bhargav.pansariwala.settings.ThemeMode
import org.bhargav.pansariwala.theme.PansariTheme
import org.koin.compose.koinInject
import androidx.compose.runtime.LaunchedEffect

@Composable
fun App(
    preferences: AppPreferences = koinInject(),
    alerts: LiveAlerts = koinInject(),
    notifications: NotificationGateway = koinInject(),
    locationTracker: PartnerLocationTracker = koinInject(),
) {
    val settings by preferences.userSettings.collectAsStateWithLifecycle(
        initialValue = AppUserSettings(),
    )
    val product = currentAppProduct()
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    LaunchedEffect(product) {
        notifications.ensureChannels()
        notifications.requestPermissionIfNeeded()
        if (product == AppProduct.DELIVERY) {
            locationTracker.restore()
        }
        alerts.run(product)
    }

    AppLocaleProvider(languageCode = settings.language.code) {
        PansariTheme(
            darkTheme = darkTheme,
            customTheme = settings.customTheme,
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                when (product) {
                    AppProduct.POS -> AppNavGraph()
                    AppProduct.USER -> UserNavGraph()
                    AppProduct.DELIVERY -> DeliveryNavGraph()
                }
            }
        }
    }
}
