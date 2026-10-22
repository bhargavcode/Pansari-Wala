package org.bhargav.pansariwala.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.bhargav.pansariwala.platform.SyncBrowserHistory
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.bhargav.pansariwala.feature.splash.SplashScreen
import org.bhargav.pansariwala.feature.user.AccountHomeScreen
import org.bhargav.pansariwala.feature.user.AddressScreen
import org.bhargav.pansariwala.feature.user.CartScreen
import org.bhargav.pansariwala.feature.user.CheckoutScreen
import org.bhargav.pansariwala.feature.user.HelpScreen
import org.bhargav.pansariwala.feature.user.MarketScreen
import org.bhargav.pansariwala.feature.user.OrderDetailsScreen
import org.bhargav.pansariwala.feature.user.OrdersListScreen
import org.bhargav.pansariwala.feature.user.PhoneAuthScreen
import org.bhargav.pansariwala.feature.user.PhoneAuthViewModel
import org.bhargav.pansariwala.feature.user.PhoneOtpScreen
import org.bhargav.pansariwala.feature.user.ProfileNameScreen
import org.bhargav.pansariwala.feature.user.ProfileSetupScreen
import org.koin.compose.viewmodel.koinViewModel
import org.bhargav.pansariwala.feature.user.ShopCatalogScreen
import org.bhargav.pansariwala.feature.user.ShopOffersScreen
import org.bhargav.pansariwala.feature.user.ThankYouScreen
import org.bhargav.pansariwala.feature.user.PaymentStatusScreen
import org.bhargav.pansariwala.feature.user.TransactionsScreen
import org.bhargav.pansariwala.feature.user.UserLanguageScreen
import org.bhargav.pansariwala.feature.user.UserNotificationPrefsScreen
import org.bhargav.pansariwala.feature.user.MoreScreen
import org.bhargav.pansariwala.feature.user.SpendDetailsScreen
import org.bhargav.pansariwala.feature.user.SpendDrillKind
import org.bhargav.pansariwala.feature.user.SpendReportScreen
import org.bhargav.pansariwala.feature.user.UserThemeScreen
import org.bhargav.pansariwala.api.SessionExpiredBus
import org.bhargav.pansariwala.notification.NotificationOpenRequests
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.notification.NotificationRouter
import androidx.compose.runtime.collectAsState
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_show
import pansariwala.shared.generated.resources.error_session_expired_login
import pansariwala.shared.generated.resources.tab_home
import pansariwala.shared.generated.resources.tab_orders
import pansariwala.shared.generated.resources.tab_more

private val userNavConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(UserRoute.Splash::class, UserRoute.Splash.serializer())
            subclass(UserRoute.PhoneAuth::class, UserRoute.PhoneAuth.serializer())
            subclass(UserRoute.PhoneOtp::class, UserRoute.PhoneOtp.serializer())
            subclass(UserRoute.ProfileSetup::class, UserRoute.ProfileSetup.serializer())
            subclass(UserRoute.ProfileAddress::class, UserRoute.ProfileAddress.serializer())
            subclass(UserRoute.Address::class, UserRoute.Address.serializer())
            subclass(UserRoute.Shell::class, UserRoute.Shell.serializer())
            subclass(UserRoute.ShopCatalog::class, UserRoute.ShopCatalog.serializer())
            subclass(UserRoute.ShopCategory::class, UserRoute.ShopCategory.serializer())
            subclass(UserRoute.ShopProduct::class, UserRoute.ShopProduct.serializer())
            subclass(UserRoute.ShopOffers::class, UserRoute.ShopOffers.serializer())
            subclass(UserRoute.Cart::class, UserRoute.Cart.serializer())
            subclass(UserRoute.Checkout::class, UserRoute.Checkout.serializer())
            subclass(UserRoute.ThankYou::class, UserRoute.ThankYou.serializer())
            subclass(UserRoute.PaymentStatus::class, UserRoute.PaymentStatus.serializer())
            subclass(UserRoute.OrderDetails::class, UserRoute.OrderDetails.serializer())
            subclass(UserRoute.OrdersList::class, UserRoute.OrdersList.serializer())
            subclass(UserRoute.Transactions::class, UserRoute.Transactions.serializer())
            subclass(UserRoute.Help::class, UserRoute.Help.serializer())
            subclass(UserRoute.NotificationPrefs::class, UserRoute.NotificationPrefs.serializer())
            subclass(UserRoute.LanguagePrefs::class, UserRoute.LanguagePrefs.serializer())
            subclass(UserRoute.ThemePrefs::class, UserRoute.ThemePrefs.serializer())
            subclass(UserRoute.SpendReport::class, UserRoute.SpendReport.serializer())
            subclass(UserRoute.SpendDetails::class, UserRoute.SpendDetails.serializer())
        }
    }
}

@Composable
fun UserNavGraph() {
    val backStack = rememberNavBackStack(userNavConfig, UserRoute.Splash)
    val snackbarHostState = remember { SnackbarHostState() }
    val openAction = stringResource(Res.string.action_show)
    val sessionExpiredMessage = stringResource(Res.string.error_session_expired_login)
    fun replaceAll(route: UserRoute) { backStack.clear(); backStack.add(route) }
    fun push(route: UserRoute) { backStack.add(route) }
    fun pop() { if (backStack.size > 1) backStack.removeLastOrNull() }
    fun popOr(fallback: UserRoute) {
        if (backStack.size > 1) backStack.removeLastOrNull() else replaceAll(fallback)
    }
    /** Back to whatever opened profile setup (e.g. checkout); straight to home after sign-up. */
    fun finishProfileSetup() {
        val start = backStack.indexOfFirst { it is UserRoute.ProfileSetup }
        if (start > 0) {
            while (backStack.size > start) backStack.removeLastOrNull()
        } else {
            replaceAll(UserRoute.Shell)
        }
    }
    val phoneAuthViewModel: PhoneAuthViewModel = koinViewModel()

    LaunchedEffect(Unit) {
        SessionExpiredBus.events.collect {
            val current = backStack.lastOrNull()
            if (current is UserRoute.PhoneAuth || current is UserRoute.PhoneOtp) return@collect
            replaceAll(UserRoute.PhoneAuth)
            snackbarHostState.showSnackbar(
                message = sessionExpiredMessage,
                duration = SnackbarDuration.Short,
            )
        }
    }

    LaunchedEffect(Unit) {
        NotificationRouter.events.collect { notification ->
            val result = snackbarHostState.showSnackbar(
                message = "${notification.title}: ${notification.body}",
                actionLabel = if (notification.orderId != null) openAction else null,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                notification.orderId?.let {
                    push(
                        if (notification.type == AppConstants.Notification.TYPE_PAYMENT) {
                            UserRoute.PaymentStatus(it)
                        } else {
                            UserRoute.OrderDetails(it)
                        },
                    )
                }
            }
        }
    }

    val pendingOpen by NotificationOpenRequests.pending.collectAsState()
    val currentRoute = backStack.lastOrNull()
    val signedIn = currentRoute != null && currentRoute !is UserRoute.Splash &&
        currentRoute !is UserRoute.PhoneAuth && currentRoute !is UserRoute.PhoneOtp &&
        currentRoute !is UserRoute.ProfileSetup && currentRoute !is UserRoute.ProfileAddress
    LaunchedEffect(pendingOpen, signedIn) {
        val request = pendingOpen ?: return@LaunchedEffect
        if (!signedIn) return@LaunchedEffect
        NotificationOpenRequests.consume()
        val orderId = request.orderId ?: return@LaunchedEffect
        if (request.type == AppConstants.Notification.TYPE_PAYMENT) {
            if ((backStack.lastOrNull() as? UserRoute.PaymentStatus)?.intentId != orderId) {
                push(UserRoute.PaymentStatus(orderId))
            }
        } else if ((backStack.lastOrNull() as? UserRoute.OrderDetails)?.orderId != orderId) {
            push(UserRoute.OrderDetails(orderId))
        }
    }

    fun onBack() {
        when (backStack.lastOrNull()) {
            is UserRoute.OrderDetails, is UserRoute.PaymentStatus -> popOr(UserRoute.Shell)
            else -> pop()
        }
    }

    SyncBrowserHistory(depth = backStack.size, onBrowserBack = ::onBack)

    Box(Modifier.fillMaxSize().safeDrawingPadding().animateContentSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = ::onBack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = { key ->
                when (val route = key as UserRoute) {
                    UserRoute.Splash -> NavEntry(route) {
                        SplashScreen(
                            onNavigateToLogin = { replaceAll(UserRoute.PhoneAuth) },
                            onNavigateToHome = { replaceAll(UserRoute.Shell) },
                            onNavigateToCompleteProfile = { replaceAll(UserRoute.ProfileSetup) },
                        )
                    }
                    UserRoute.PhoneAuth -> NavEntry(route) {
                        PhoneAuthScreen(onOtpSent = { push(UserRoute.PhoneOtp) }, viewModel = phoneAuthViewModel)
                    }
                    UserRoute.PhoneOtp -> NavEntry(route) {
                        PhoneOtpScreen(
                            onVerified = { complete ->
                                phoneAuthViewModel.reset()
                                if (complete) replaceAll(UserRoute.Shell) else replaceAll(UserRoute.ProfileSetup)
                            },
                            onBack = { pop() },
                            viewModel = phoneAuthViewModel,
                        )
                    }
                    UserRoute.ProfileSetup -> NavEntry(route) {
                        ProfileNameScreen(
                            onNext = { push(UserRoute.ProfileAddress(it)) },
                            onBack = if (backStack.size > 1) ::pop else null,
                        )
                    }
                    is UserRoute.ProfileAddress -> NavEntry(route) {
                        ProfileSetupScreen(
                            name = route.name,
                            onDone = ::finishProfileSetup,
                            onBack = { pop() },
                        )
                    }
                    UserRoute.Address -> NavEntry(route) {
                        AddressScreen(onDone = { pop() }, onBack = { pop() })
                    }
                    UserRoute.Shell -> NavEntry(route) {
                        UserShell(
                            onOpenShop = { push(UserRoute.ShopCatalog(it)) },
                            onShowShopOffers = { push(UserRoute.ShopOffers(it)) },
                            onOpenCart = { shopId -> push(UserRoute.Cart(shopId)) },
                            onOrder = { push(UserRoute.OrderDetails(it)) },
                            onPayment = { push(UserRoute.PaymentStatus(it)) },
                            onAllOrders = { push(UserRoute.OrdersList) },
                            onTxns = { push(UserRoute.Transactions) },
                            onHelp = { push(UserRoute.Help) },
                            onLanguage = { push(UserRoute.LanguagePrefs) },
                            onNotifications = { push(UserRoute.NotificationPrefs) },
                            onTheme = { push(UserRoute.ThemePrefs) },
                            onLogout = { replaceAll(UserRoute.PhoneAuth) },
                            onChangeLocation = { push(UserRoute.Address) },
                            onSpendReport = { push(UserRoute.SpendReport) },
                        )
                    }
                    UserRoute.SpendReport -> NavEntry(route) {
                        SpendReportScreen(
                            onDrill = { from, to, kind, key, title ->
                                push(UserRoute.SpendDetails(from, to, kind.name, key, title))
                            },
                            onBack = { pop() },
                        )
                    }
                    is UserRoute.SpendDetails -> NavEntry(route) {
                        SpendDetailsScreen(
                            fromMs = route.fromMs,
                            toMs = route.toMs,
                            kind = SpendDrillKind.valueOf(route.kind),
                            key = route.key,
                            title = route.title,
                            onOpenOrder = { push(UserRoute.OrderDetails(it)) },
                            onBack = { pop() },
                        )
                    }
                    is UserRoute.ShopCatalog -> NavEntry(route) {
                        ShopCatalogScreen(
                            shopId = route.shopId,
                            onOpenCart = { push(UserRoute.Cart(route.shopId)) },
                            onOpenCategory = { category, query ->
                                push(UserRoute.ShopCategory(route.shopId, category?.id, category?.name.orEmpty(), query))
                            },
                            onOpenProduct = { push(UserRoute.ShopProduct(route.shopId, it)) },
                            onBack = { pop() },
                        )
                    }
                    is UserRoute.ShopCategory -> NavEntry(route) {
                        org.bhargav.pansariwala.feature.user.ShopCategoryScreen(
                            shopId = route.shopId,
                            category = route.categoryId?.let {
                                org.bhargav.pansariwala.domain.model.CatalogCategory(it, route.categoryName)
                            },
                            initialQuery = route.query,
                            onOpenProduct = { push(UserRoute.ShopProduct(route.shopId, it)) },
                            onOpenCart = { push(UserRoute.Cart(route.shopId)) },
                            onBack = { pop() },
                        )
                    }
                    is UserRoute.ShopProduct -> NavEntry(route) {
                        org.bhargav.pansariwala.feature.user.ShopProductDetailScreen(
                            shopId = route.shopId,
                            productId = route.productId,
                            onOpenCart = { push(UserRoute.Cart(route.shopId)) },
                            onBack = { pop() },
                        )
                    }
                    is UserRoute.ShopOffers -> NavEntry(route) {
                        ShopOffersScreen(
                            shopId = route.shopId,
                            onOpenProduct = { push(UserRoute.ShopProduct(route.shopId, it)) },
                            onOpenShop = { push(UserRoute.ShopCatalog(route.shopId)) },
                            onOpenCart = { push(UserRoute.Cart(route.shopId)) },
                            onBack = { pop() },
                        )
                    }
                    is UserRoute.Cart -> NavEntry(route) {
                        CartScreen(
                            shopId = route.shopId,
                            onCheckout = { push(UserRoute.Checkout(route.shopId)) },
                            onBack = { pop() },
                        )
                    }
                    is UserRoute.Checkout -> NavEntry(route) {
                        CheckoutScreen(
                            shopId = route.shopId,
                            onPlaced = { replaceAll(UserRoute.ThankYou(it)) },
                            onPaymentStarted = { intentId ->
                                // Shell stays underneath so leaving the waiting screen lands on home.
                                replaceAll(UserRoute.Shell)
                                push(UserRoute.PaymentStatus(intentId, autoLaunch = true))
                            },
                            onCompleteProfile = { push(UserRoute.ProfileSetup) },
                            onAddAddress = { push(UserRoute.Address) },
                            onBack = { pop() },
                        )
                    }
                    is UserRoute.ThankYou -> NavEntry(route) {
                        ThankYouScreen(onContinue = {
                            // Keep Shell under OrderDetails so back always works after checkout.
                            replaceAll(UserRoute.Shell)
                            push(UserRoute.OrderDetails(route.orderId))
                        })
                    }
                    is UserRoute.PaymentStatus -> NavEntry(route) {
                        PaymentStatusScreen(
                            intentId = route.intentId,
                            autoLaunch = route.autoLaunch,
                            onViewOrder = { orderId ->
                                replaceAll(UserRoute.Shell)
                                push(UserRoute.OrderDetails(orderId))
                            },
                            onRetry = { shopId -> push(UserRoute.Cart(shopId)) },
                            onHome = { replaceAll(UserRoute.Shell) },
                        )
                    }
                    is UserRoute.OrderDetails -> NavEntry(route) {
                        OrderDetailsScreen(
                            route.orderId,
                            onBack = { popOr(UserRoute.Shell) },
                        )
                    }
                    UserRoute.OrdersList -> NavEntry(route) {
                        OrdersListScreen(
                            onOpen = { push(UserRoute.OrderDetails(it)) },
                            onOpenPayment = { push(UserRoute.PaymentStatus(it)) },
                            onRetryPayment = { shopId -> push(UserRoute.Cart(shopId)) },
                            onBack = { pop() },
                        )
                    }
                    UserRoute.Transactions -> NavEntry(route) {
                        TransactionsScreen(onBack = { pop() })
                    }
                    UserRoute.Help -> NavEntry(route) {
                        HelpScreen(onBack = { pop() })
                    }
                    UserRoute.NotificationPrefs -> NavEntry(route) {
                        UserNotificationPrefsScreen(onBack = { pop() })
                    }
                    UserRoute.LanguagePrefs -> NavEntry(route) {
                        UserLanguageScreen(onBack = { pop() })
                    }
                    UserRoute.ThemePrefs -> NavEntry(route) {
                        UserThemeScreen(onBack = { pop() })
                    }
                }
            },
        )
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
        )
    }
}

@Composable
private fun UserShell(
    onOpenShop: (String) -> Unit,
    onShowShopOffers: (String) -> Unit,
    onOpenCart: (String) -> Unit,
    onOrder: (String) -> Unit,
    onPayment: (String) -> Unit,
    onAllOrders: () -> Unit,
    onTxns: () -> Unit,
    onHelp: () -> Unit,
    onLanguage: () -> Unit,
    onNotifications: () -> Unit,
    onTheme: () -> Unit,
    onLogout: () -> Unit,
    onChangeLocation: () -> Unit,
    onSpendReport: () -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = tab != 0,
        onBackCompleted = { tab = 0 },
    )
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).padding(bottom = 4.dp)) {
                when (tab) {
                    0 -> MarketScreen(
                        onOpenShop = onOpenShop,
                        onContinueOrder = onOpenCart,
                        onChangeLocation = onChangeLocation,
                        onShowShopOffers = onShowShopOffers,
                    )
                    1 -> AccountHomeScreen(
                        onRecent = onOrder,
                        onOpenPayment = onPayment,
                        onRetryPayment = onOpenCart,
                        onAllOrders = onAllOrders,
                        onTxns = onTxns,
                        onHelp = onHelp,
                    )
                    else -> MoreScreen(
                        onAddAddress = onChangeLocation,
                        onOpenSpendReport = onSpendReport,
                        onLanguage = onLanguage,
                        onNotifications = onNotifications,
                        onTheme = onTheme,
                        onLogout = onLogout,
                    )
                }
            }
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = stringResource(Res.string.tab_home)) },
                    label = { Text(stringResource(Res.string.tab_home)) },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = stringResource(Res.string.tab_orders)) },
                    label = { Text(stringResource(Res.string.tab_orders)) },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Icon(Icons.Default.Menu, contentDescription = stringResource(Res.string.tab_more)) },
                    label = { Text(stringResource(Res.string.tab_more)) },
                )
            }
        }
    }
}
