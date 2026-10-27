package org.bhargav.pansariwala

import androidx.compose.runtime.CompositionLocalProvider
import org.bhargav.pansariwala.designsystem.FloatingKeyboardHost
import org.bhargav.pansariwala.designsystem.LocalFloatingKeyboardEligible
import org.bhargav.pansariwala.designsystem.hasSoftKeyboard
import org.bhargav.pansariwala.navigation.RetainAcrossRemount
import org.bhargav.pansariwala.platform.BrowserBackBridge
import org.bhargav.pansariwala.platform.WebExitGuard
import org.bhargav.pansariwala.platform.OtpStatusHost
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.coroutineScope
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.i18n.AppLocaleProvider
import org.bhargav.pansariwala.navigation.AppNavGraph
import org.bhargav.pansariwala.navigation.DeliveryNavGraph
import org.bhargav.pansariwala.navigation.UserNavGraph
import org.bhargav.pansariwala.notification.NotificationGateway
import org.bhargav.pansariwala.notification.NotificationPromptGate
import org.bhargav.pansariwala.notification.PartnerOfferSocket
import org.bhargav.pansariwala.notification.PushRegistrar
import org.bhargav.pansariwala.notification.AppInstallReporter
import org.bhargav.pansariwala.platform.PartnerLocationTracker
import org.bhargav.pansariwala.product.AppProduct
import org.bhargav.pansariwala.product.currentAppProduct
import org.bhargav.pansariwala.settings.AppUserSettings
import org.bhargav.pansariwala.settings.ThemeMode
import org.bhargav.pansariwala.theme.PansariTheme
import org.koin.compose.koinInject
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.data.sync.OfflineOrderSync
import org.bhargav.pansariwala.designsystem.PermissionRationaleSheet
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.notification_rationale_allow
import pansariwala.shared.generated.resources.notification_rationale_footer
import pansariwala.shared.generated.resources.notification_rationale_message
import pansariwala.shared.generated.resources.notification_rationale_point_alerts
import pansariwala.shared.generated.resources.notification_rationale_point_delivery
import pansariwala.shared.generated.resources.notification_rationale_point_orders
import pansariwala.shared.generated.resources.notification_rationale_title

@Composable
fun App(
    preferences: AppPreferences = koinInject(),
    offerSocket: PartnerOfferSocket = koinInject(),
    notifications: NotificationGateway = koinInject(),
    locationTracker: PartnerLocationTracker = koinInject(),
    pushRegistrar: PushRegistrar = koinInject(),
    orderSync: OfflineOrderSync = koinInject(),
    installReporter: AppInstallReporter = koinInject(),
) {
    // Null until prefs load: composing with defaults first would flip AppLocaleProvider's key and wipe all
    // restored saveable state (nav back stack, scroll, inputs) after every activity recreation.
    val loadedSettings by preferences.userSettings.collectAsStateWithLifecycle<AppUserSettings?>(
        initialValue = null,
    )
    val settings = loadedSettings ?: AppUserSettings()
    val product = currentAppProduct()
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    var showNotificationRationale by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(product) { installReporter.run(product) }

    LaunchedEffect(product) {
        notifications.ensureChannels()
        if (product == AppProduct.POS) orderSync.start()
        when (product) {
            AppProduct.DELIVERY -> coroutineScope {
                locationTracker.restore()
                launch { pushRegistrar.run(product) }
                offerSocket.run()
            }
            AppProduct.POS, AppProduct.USER -> pushRegistrar.run(product)
        }
    }

    val homeReached by NotificationPromptGate.homeReached.collectAsStateWithLifecycle()
    LaunchedEffect(product, homeReached) {
        // POS has no phone/OTP onboarding; consumer + partner apps ask only once on their home screen.
        if (product != AppProduct.POS && !homeReached) return@LaunchedEffect
        showNotificationRationale = !preferences.isNotificationPromptShown() &&
            notifications.needsPermissionPrompt()
    }

    BrowserBackBridge()
    if (loadedSettings == null) return
    RetainAcrossRemount { AppLocaleProvider(languageCode = settings.language.code) {
        PansariTheme(
            darkTheme = darkTheme,
            customTheme = settings.customTheme,
        ) {
            val keyboardEligible = hasSoftKeyboard
            val floatingKeyboardOn by preferences.floatingKeyboard.collectAsStateWithLifecycle(initialValue = true)
            Surface(modifier = Modifier.fillMaxSize()) {
                CompositionLocalProvider(LocalFloatingKeyboardEligible provides keyboardEligible) {
                    WebExitGuard()
                    FloatingKeyboardHost(enabled = keyboardEligible && floatingKeyboardOn, preferences = preferences) {
                        when (product) {
                            AppProduct.POS -> AppNavGraph()
                            AppProduct.USER -> UserNavGraph()
                            AppProduct.DELIVERY -> DeliveryNavGraph()
                        }
                    }
                }
                OtpStatusHost()
                if (showNotificationRationale) {
                    fun close(allow: Boolean) {
                        showNotificationRationale = false
                        scope.launch { preferences.setNotificationPromptShown() }
                        if (allow) notifications.requestPermissionIfNeeded()
                    }
                    PermissionRationaleSheet(
                        icon = Icons.Default.Notifications,
                        title = stringResource(Res.string.notification_rationale_title),
                        message = stringResource(Res.string.notification_rationale_message),
                        points = listOf(
                            stringResource(Res.string.notification_rationale_point_orders),
                            stringResource(Res.string.notification_rationale_point_delivery),
                            stringResource(Res.string.notification_rationale_point_alerts),
                        ),
                        footer = stringResource(Res.string.notification_rationale_footer),
                        confirmLabel = stringResource(Res.string.notification_rationale_allow),
                        onConfirm = { close(allow = true) },
                        onDismiss = { close(allow = false) },
                    )
                }
            }
        }
    } }
}
