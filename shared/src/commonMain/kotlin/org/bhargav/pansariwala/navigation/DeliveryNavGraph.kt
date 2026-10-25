package org.bhargav.pansariwala.navigation

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.bhargav.pansariwala.api.SessionExpiredBus
import org.bhargav.pansariwala.feature.delivery.PartnerCapturePhotosScreen
import org.bhargav.pansariwala.feature.delivery.PartnerCustomerPaymentScreen
import org.bhargav.pansariwala.feature.delivery.PartnerDeliverToCustomerScreen
import org.bhargav.pansariwala.feature.delivery.PartnerDeliveryCompleteScreen
import org.bhargav.pansariwala.feature.delivery.PartnerEarningsScreen
import org.bhargav.pansariwala.feature.delivery.PartnerHomeScreen
import org.bhargav.pansariwala.feature.delivery.PartnerLoginOtpScreen
import org.bhargav.pansariwala.feature.delivery.PartnerLoginScreen
import org.bhargav.pansariwala.feature.delivery.PartnerLoginViewModel
import org.bhargav.pansariwala.feature.delivery.PartnerNavigateToStoreScreen
import org.bhargav.pansariwala.feature.delivery.PartnerPickupItemsScreen
import org.bhargav.pansariwala.feature.delivery.PartnerRegisterAddressScreen
import org.bhargav.pansariwala.feature.delivery.PartnerRegisterDetailsScreen
import org.bhargav.pansariwala.feature.delivery.PartnerRegisterOtpScreen
import org.bhargav.pansariwala.feature.delivery.PartnerRegisterVehicleScreen
import org.bhargav.pansariwala.feature.delivery.PartnerRegisterViewModel
import org.bhargav.pansariwala.feature.delivery.PartnerReturnToShopScreen
import org.koin.compose.viewmodel.koinViewModel
import org.bhargav.pansariwala.feature.splash.SplashScreen
import org.bhargav.pansariwala.feature.user.UserLanguageScreen
import org.bhargav.pansariwala.feature.user.UserNotificationPrefsScreen
import org.bhargav.pansariwala.feature.user.UserThemeScreen
import org.bhargav.pansariwala.notification.NotificationOpenRequests
import org.bhargav.pansariwala.notification.NotificationRouter
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_session_expired_login

private val deliveryNavConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(DeliveryRoute.Splash::class, DeliveryRoute.Splash.serializer())
            subclass(DeliveryRoute.Login::class, DeliveryRoute.Login.serializer())
            subclass(DeliveryRoute.LoginOtp::class, DeliveryRoute.LoginOtp.serializer())
            subclass(DeliveryRoute.Register::class, DeliveryRoute.Register.serializer())
            subclass(DeliveryRoute.RegisterAddress::class, DeliveryRoute.RegisterAddress.serializer())
            subclass(DeliveryRoute.RegisterVehicle::class, DeliveryRoute.RegisterVehicle.serializer())
            subclass(DeliveryRoute.RegisterOtp::class, DeliveryRoute.RegisterOtp.serializer())
            subclass(DeliveryRoute.Home::class, DeliveryRoute.Home.serializer())
            subclass(DeliveryRoute.NavigateToStore::class, DeliveryRoute.NavigateToStore.serializer())
            subclass(DeliveryRoute.PickupItems::class, DeliveryRoute.PickupItems.serializer())
            subclass(DeliveryRoute.CapturePhotos::class, DeliveryRoute.CapturePhotos.serializer())
            subclass(DeliveryRoute.DeliverToCustomer::class, DeliveryRoute.DeliverToCustomer.serializer())
            subclass(DeliveryRoute.CustomerPayment::class, DeliveryRoute.CustomerPayment.serializer())
            subclass(DeliveryRoute.DeliveryComplete::class, DeliveryRoute.DeliveryComplete.serializer())
            subclass(DeliveryRoute.ReturnToShop::class, DeliveryRoute.ReturnToShop.serializer())
            subclass(DeliveryRoute.Earnings::class, DeliveryRoute.Earnings.serializer())
            subclass(DeliveryRoute.LanguagePrefs::class, DeliveryRoute.LanguagePrefs.serializer())
            subclass(DeliveryRoute.ThemePrefs::class, DeliveryRoute.ThemePrefs.serializer())
            subclass(DeliveryRoute.NotificationPrefs::class, DeliveryRoute.NotificationPrefs.serializer())
        }
    }
}

private fun resumePartnerJob(order: Order): DeliveryRoute {
    val id = order.id
    if (order.status == OrderStatus.RETURNING_TO_SHOP) return DeliveryRoute.ReturnToShop(id)
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
    val backStack = retainedAcrossRemount("delivery", rememberNavBackStack(deliveryNavConfig, DeliveryRoute.Splash))
    val snackbarHostState = remember { SnackbarHostState() }
    val sessionExpiredMessage = stringResource(Res.string.error_session_expired_login)
    val loginViewModel: PartnerLoginViewModel = koinViewModel()
    val registerViewModel: PartnerRegisterViewModel = koinViewModel()
    fun replaceAll(route: DeliveryRoute) { backStack.clear(); backStack.add(route) }
    fun push(route: DeliveryRoute) { backStack.add(route) }
    fun pop() { if (backStack.size > 1) backStack.removeLastOrNull() }

    LaunchedEffect(Unit) {
        SessionExpiredBus.events.collect {
            val current = backStack.lastOrNull()
            if (current is DeliveryRoute.Login || current is DeliveryRoute.LoginOtp) return@collect
            replaceAll(DeliveryRoute.Login)
            snackbarHostState.showSnackbar(
                message = sessionExpiredMessage,
                duration = SnackbarDuration.Short,
            )
        }
    }

    LaunchedEffect(Unit) {
        NotificationRouter.events.collect { notification ->
            if (notification.type != AppConstants.Notification.TYPE_DELIVERY_OFFER) return@collect
            val current = backStack.lastOrNull()
            if (current !is DeliveryRoute.Home) {
                replaceAll(DeliveryRoute.Home)
            }
        }
    }

    val pendingOpen by NotificationOpenRequests.pending.collectAsState()
    val currentRoute = backStack.lastOrNull()
    val signedIn = currentRoute != null && currentRoute !is DeliveryRoute.Splash &&
        currentRoute !is DeliveryRoute.Login && currentRoute !is DeliveryRoute.LoginOtp &&
        currentRoute !is DeliveryRoute.Register && currentRoute !is DeliveryRoute.RegisterAddress &&
        currentRoute !is DeliveryRoute.RegisterVehicle && currentRoute !is DeliveryRoute.RegisterOtp
    LaunchedEffect(pendingOpen, signedIn) {
        val request = pendingOpen ?: return@LaunchedEffect
        if (!signedIn) return@LaunchedEffect
        NotificationOpenRequests.consume()
        val onActiveJob = backStack.any {
            it is DeliveryRoute.NavigateToStore || it is DeliveryRoute.PickupItems ||
                it is DeliveryRoute.CapturePhotos || it is DeliveryRoute.DeliverToCustomer ||
                it is DeliveryRoute.CustomerPayment
        }
        if (request.type == AppConstants.Notification.TYPE_DELIVERY_OFFER || !onActiveJob) {
            if (backStack.lastOrNull() !is DeliveryRoute.Home) replaceAll(DeliveryRoute.Home)
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
                rememberStableViewModelStoreNavEntryDecorator(),
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
                            onOtpSent = { push(DeliveryRoute.LoginOtp) },
                            onSignUp = { push(DeliveryRoute.Register) },
                            viewModel = loginViewModel,
                        )
                    }
                    DeliveryRoute.LoginOtp -> NavEntry(route) {
                        PartnerLoginOtpScreen(
                            onVerified = {
                                loginViewModel.reset()
                                replaceAll(DeliveryRoute.Home)
                            },
                            onBack = { pop() },
                            viewModel = loginViewModel,
                        )
                    }
                    DeliveryRoute.Register -> NavEntry(route) {
                        PartnerRegisterDetailsScreen(
                            onNext = { push(DeliveryRoute.RegisterAddress) },
                            onSignIn = { replaceAll(DeliveryRoute.Login) },
                            onBack = { if (backStack.size > 1) pop() else replaceAll(DeliveryRoute.Login) },
                            viewModel = registerViewModel,
                        )
                    }
                    DeliveryRoute.RegisterAddress -> NavEntry(route) {
                        PartnerRegisterAddressScreen(
                            onNext = { push(DeliveryRoute.RegisterVehicle) },
                            onBack = { pop() },
                            viewModel = registerViewModel,
                        )
                    }
                    DeliveryRoute.RegisterVehicle -> NavEntry(route) {
                        PartnerRegisterVehicleScreen(
                            onOtpSent = { push(DeliveryRoute.RegisterOtp) },
                            onBack = { pop() },
                            viewModel = registerViewModel,
                        )
                    }
                    DeliveryRoute.RegisterOtp -> NavEntry(route) {
                        PartnerRegisterOtpScreen(
                            onVerified = {
                                registerViewModel.reset()
                                replaceAll(DeliveryRoute.Home)
                            },
                            onBack = { pop() },
                            viewModel = registerViewModel,
                        )
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
                            onReturning = { replaceAll(DeliveryRoute.Home); push(DeliveryRoute.ReturnToShop(route.orderId)) },
                            onBack = { pop() },
                        )
                    }
                    is DeliveryRoute.ReturnToShop -> NavEntry(route) {
                        PartnerReturnToShopScreen(
                            orderId = route.orderId,
                            onDone = { replaceAll(DeliveryRoute.Home) },
                            onBack = { replaceAll(DeliveryRoute.Home) },
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
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
        )
    }
}
