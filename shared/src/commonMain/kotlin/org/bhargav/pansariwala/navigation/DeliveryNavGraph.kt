package org.bhargav.pansariwala.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.bhargav.pansariwala.feature.delivery.IncomingOfferScreen
import org.bhargav.pansariwala.feature.delivery.JobsListScreen
import org.bhargav.pansariwala.feature.delivery.PartnerDashboardScreen
import org.bhargav.pansariwala.feature.delivery.PartnerLoginScreen
import org.bhargav.pansariwala.feature.delivery.PartnerRegisterScreen
import org.bhargav.pansariwala.feature.delivery.PickupScreen
import org.bhargav.pansariwala.feature.splash.SplashScreen
import org.bhargav.pansariwala.notification.NotificationRouter
import org.bhargav.pansariwala.util.AppConstants

private val deliveryNavConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(DeliveryRoute.Splash::class, DeliveryRoute.Splash.serializer())
            subclass(DeliveryRoute.Login::class, DeliveryRoute.Login.serializer())
            subclass(DeliveryRoute.Register::class, DeliveryRoute.Register.serializer())
            subclass(DeliveryRoute.Dashboard::class, DeliveryRoute.Dashboard.serializer())
            subclass(DeliveryRoute.IncomingOffer::class, DeliveryRoute.IncomingOffer.serializer())
            subclass(DeliveryRoute.AcceptedJobs::class, DeliveryRoute.AcceptedJobs.serializer())
            subclass(DeliveryRoute.DeliveredJobs::class, DeliveryRoute.DeliveredJobs.serializer())
            subclass(DeliveryRoute.Earnings::class, DeliveryRoute.Earnings.serializer())
            subclass(DeliveryRoute.Pickup::class, DeliveryRoute.Pickup.serializer())
        }
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
            if (current is DeliveryRoute.IncomingOffer || current is DeliveryRoute.Pickup) return@collect
            push(DeliveryRoute.IncomingOffer(notification.offerId))
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
                            onNavigateToHome = { replaceAll(DeliveryRoute.Dashboard) },
                        )
                    }
                    DeliveryRoute.Login -> NavEntry(route) {
                        PartnerLoginScreen(
                            onVerified = { replaceAll(DeliveryRoute.Dashboard) },
                            onSignUp = { push(DeliveryRoute.Register) },
                        )
                    }
                    DeliveryRoute.Register -> NavEntry(route) {
                        PartnerRegisterScreen(
                            onVerified = { replaceAll(DeliveryRoute.Dashboard) },
                            onSignIn = { replaceAll(DeliveryRoute.Login) },
                        )
                    }
                    DeliveryRoute.Dashboard -> NavEntry(route) {
                        PartnerDashboardScreen(
                            onDelivered = { push(DeliveryRoute.DeliveredJobs) },
                            onAccepted = { push(DeliveryRoute.AcceptedJobs) },
                            onEarnings = { push(DeliveryRoute.Earnings) },
                            onIncoming = { push(DeliveryRoute.IncomingOffer()) },
                        )
                    }
                    is DeliveryRoute.IncomingOffer -> NavEntry(route) {
                        IncomingOfferScreen(
                            offerId = route.offerId,
                            onAccepted = { replaceAll(DeliveryRoute.AcceptedJobs) },
                            onBack = { pop() },
                        )
                    }
                    DeliveryRoute.AcceptedJobs -> NavEntry(route) {
                        JobsListScreen(
                            delivered = false,
                            onPickup = { push(DeliveryRoute.Pickup(it)) },
                            onBack = { pop() },
                        )
                    }
                    DeliveryRoute.DeliveredJobs, DeliveryRoute.Earnings -> NavEntry(route) {
                        JobsListScreen(delivered = true, onPickup = {}, onBack = { pop() })
                    }
                    is DeliveryRoute.Pickup -> NavEntry(route) {
                        PickupScreen(route.orderId, onDone = { pop() })
                    }
                }
            },
        )
    }
}
