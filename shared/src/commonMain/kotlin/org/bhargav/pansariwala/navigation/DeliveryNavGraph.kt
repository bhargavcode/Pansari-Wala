package org.bhargav.pansariwala.navigation

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.bhargav.pansariwala.feature.delivery.PartnerCapturePhotosScreen
import org.bhargav.pansariwala.feature.delivery.PartnerCustomerPaymentScreen
import org.bhargav.pansariwala.feature.delivery.PartnerDeliverToCustomerScreen
import org.bhargav.pansariwala.feature.delivery.PartnerDeliveryCompleteScreen
import org.bhargav.pansariwala.feature.delivery.PartnerEarningsScreen
import org.bhargav.pansariwala.feature.delivery.PartnerHomeScreen
import org.bhargav.pansariwala.feature.delivery.PartnerLocationAccessScreen
import org.bhargav.pansariwala.feature.delivery.PartnerLoginScreen
import org.bhargav.pansariwala.feature.delivery.PartnerNavigateToStoreScreen
import org.bhargav.pansariwala.feature.delivery.PartnerPickupItemsScreen
import org.bhargav.pansariwala.feature.delivery.PartnerRegisterScreen
import org.bhargav.pansariwala.feature.splash.SplashScreen
import org.bhargav.pansariwala.feature.user.UserLanguageScreen
import org.bhargav.pansariwala.feature.user.UserNotificationPrefsScreen
import org.bhargav.pansariwala.feature.user.UserThemeScreen
import org.bhargav.pansariwala.notification.NotificationRouter
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.util.AppConstants

private val deliveryNavConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(DeliveryRoute.Splash::class, DeliveryRoute.Splash.serializer())
            subclass(DeliveryRoute.Login::class, DeliveryRoute.Login.serializer())
            subclass(DeliveryRoute.Register::class, DeliveryRoute.Register.serializer())
            subclass(DeliveryRoute.LocationAccess::class, DeliveryRoute.LocationAccess.serializer())
            subclass(DeliveryRoute.Home::class, DeliveryRoute.Home.serializer())
            subclass(DeliveryRoute.NavigateToStore::class, DeliveryRoute.NavigateToStore.serializer())
            subclass(DeliveryRoute.PickupItems::class, DeliveryRoute.PickupItems.serializer())
            subclass(DeliveryRoute.CapturePhotos::class, DeliveryRoute.CapturePhotos.serializer())
            subclass(DeliveryRoute.DeliverToCustomer::class, DeliveryRoute.DeliverToCustomer.serializer())
            subclass(DeliveryRoute.CustomerPayment::class, DeliveryRoute.CustomerPayment.serializer())
            subclass(DeliveryRoute.DeliveryComplete::class, DeliveryRoute.DeliveryComplete.serializer())
            subclass(DeliveryRoute.Earnings::class, DeliveryRoute.Earnings.serializer())
            subclass(DeliveryRoute.LanguagePrefs::class, DeliveryRoute.LanguagePrefs.serializer())
            subclass(DeliveryRoute.ThemePrefs::class, DeliveryRoute.ThemePrefs.serializer())
            subclass(DeliveryRoute.NotificationPrefs::class, DeliveryRoute.NotificationPrefs.serializer())
        }
    }
}

private fun resumePartnerJob(order: Order): DeliveryRoute {
    val id = order.id
    return when (order.resumeProgress) {
        AppConstants.PartnerProgress.AT_STORE -> DeliveryRoute.PickupItems(id)
        AppConstants.PartnerProgress.CAPTURE -> DeliveryRoute.CapturePhotos(id)
        AppConstants.PartnerProgress.TO_CUSTOMER -> DeliveryRoute.DeliverToCustomer(id)
        AppConstants.PartnerProgress.AT_CUSTOMER -> DeliveryRoute.CustomerPayment(id)
        else -> DeliveryRoute.NavigateToStore(id)
    }
}

@Composable
fun DeliveryNavGraph() {
    val backStack = rememberNavBackStack(deliveryNavConfig, DeliveryRoute.Splash)
    fun replaceAll(route: DeliveryRoute) { backStack.clear(); backStack.add(route) }
    fun push(route: DeliveryRoute) { backStack.add(route) }
    fun pop() { if (backStack.size > 1) backStack.removeLastOrNull() }

    LaunchedEffect(Unit) {
        NotificationRouter.events.collect { notification ->
            if (notification.type != AppConstants.Notification.TYPE_DELIVERY_OFFER) return@collect
            val current = backStack.lastOrNull()
            if (current !is DeliveryRoute.Home) {
                replaceAll(DeliveryRoute.Home)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .animateContentSize(),
    ) {
        NavDisplay(
            backStack = backStack,
            onBack = { pop() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = { key ->
                when (val route = key as DeliveryRoute) {
                    DeliveryRoute.Splash -> NavEntry(route) {
                        SplashScreen(
                            onNavigateToLogin = { replaceAll(DeliveryRoute.Login) },
                            onNavigateToHome = { replaceAll(DeliveryRoute.Home) },
                        )
                    }
                    DeliveryRoute.Login -> NavEntry(route) {
                        PartnerLoginScreen(
                            onVerified = { replaceAll(DeliveryRoute.LocationAccess) },
                            onSignUp = { push(DeliveryRoute.Register) },
                        )
                    }
                    DeliveryRoute.Register -> NavEntry(route) {
                        PartnerRegisterScreen(
                            onVerified = { replaceAll(DeliveryRoute.LocationAccess) },
                            onSignIn = { replaceAll(DeliveryRoute.Login) },
                            onBack = { replaceAll(DeliveryRoute.Login) },
                        )
                    }
                    DeliveryRoute.LocationAccess -> NavEntry(route) {
                        PartnerLocationAccessScreen(onDone = { replaceAll(DeliveryRoute.Home) })
                    }
                    DeliveryRoute.Home -> NavEntry(route) {
                        PartnerHomeScreen(
                            onNavigateToStore = { push(DeliveryRoute.NavigateToStore(it)) },
                            onResumeJob = { order ->
                                push(resumePartnerJob(order))
                            },
                            onEarnings = { push(DeliveryRoute.Earnings) },
                        )
                    }
                    is DeliveryRoute.NavigateToStore -> NavEntry(route) {
                        PartnerNavigateToStoreScreen(
                            orderId = route.orderId,
                            onArrived = { push(DeliveryRoute.PickupItems(route.orderId)) },
                            onBack = { pop() },
                        )
                    }
                    is DeliveryRoute.PickupItems -> NavEntry(route) {
                        PartnerPickupItemsScreen(
                            orderId = route.orderId,
                            onVerifyBags = { push(DeliveryRoute.CapturePhotos(route.orderId)) },
                            onBack = { pop() },
                        )
                    }
                    is DeliveryRoute.CapturePhotos -> NavEntry(route) {
                        PartnerCapturePhotosScreen(
                            orderId = route.orderId,
                            onPhotosReady = { push(DeliveryRoute.DeliverToCustomer(route.orderId)) },
                            onBack = { pop() },
                        )
                    }
                    is DeliveryRoute.DeliverToCustomer -> NavEntry(route) {
                        PartnerDeliverToCustomerScreen(
                            orderId = route.orderId,
                            onArrived = { push(DeliveryRoute.CustomerPayment(route.orderId)) },
                            onBack = { pop() },
                        )
                    }
                    is DeliveryRoute.CustomerPayment -> NavEntry(route) {
                        PartnerCustomerPaymentScreen(
                            orderId = route.orderId,
                            onComplete = { push(DeliveryRoute.DeliveryComplete(route.orderId)) },
                            onBack = { pop() },
                        )
                    }
                    is DeliveryRoute.DeliveryComplete -> NavEntry(route) {
                        PartnerDeliveryCompleteScreen(
                            orderId = route.orderId,
                            onDone = { replaceAll(DeliveryRoute.Home) },
                            onBack = { replaceAll(DeliveryRoute.Home) },
                        )
                    }
                    DeliveryRoute.Earnings -> NavEntry(route) {
                        PartnerEarningsScreen(
                            onBack = { pop() },
                            onLanguage = { push(DeliveryRoute.LanguagePrefs) },
                            onTheme = { push(DeliveryRoute.ThemePrefs) },
                            onNotifications = { push(DeliveryRoute.NotificationPrefs) },
                        )
                    }
                    DeliveryRoute.LanguagePrefs -> NavEntry(route) {
                        UserLanguageScreen(onBack = { pop() })
                    }
                    DeliveryRoute.ThemePrefs -> NavEntry(route) {
                        UserThemeScreen(onBack = { pop() })
                    }
                    DeliveryRoute.NotificationPrefs -> NavEntry(route) {
                        UserNotificationPrefsScreen(onBack = { pop() })
                    }
                }
            },
        )
    }
}
