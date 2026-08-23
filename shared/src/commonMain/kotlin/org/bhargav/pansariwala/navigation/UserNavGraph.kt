package org.bhargav.pansariwala.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.animation.animateContentSize
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.bhargav.pansariwala.feature.splash.SplashScreen
import org.bhargav.pansariwala.feature.user.AccountHomeScreen
import org.bhargav.pansariwala.feature.user.AddressScreen
import org.bhargav.pansariwala.feature.user.CartFloatingPill
import org.bhargav.pansariwala.feature.user.CartScreen
import org.bhargav.pansariwala.feature.user.CartStore
import org.bhargav.pansariwala.feature.user.CheckoutScreen
import org.bhargav.pansariwala.feature.user.HelpScreen
import org.bhargav.pansariwala.feature.user.MarketScreen
import org.bhargav.pansariwala.feature.user.OrderDetailsScreen
import org.bhargav.pansariwala.feature.user.OrdersListScreen
import org.bhargav.pansariwala.feature.user.PhoneAuthScreen
import org.bhargav.pansariwala.feature.user.UserLocationAccessScreen
import org.bhargav.pansariwala.feature.user.ProfileSetupScreen
import org.bhargav.pansariwala.feature.user.ShopCatalogScreen
import org.bhargav.pansariwala.feature.user.ThankYouScreen
import org.bhargav.pansariwala.feature.user.TransactionsScreen
import org.bhargav.pansariwala.feature.user.UserLanguageScreen
import org.bhargav.pansariwala.feature.user.UserNotificationPrefsScreen
import org.bhargav.pansariwala.feature.user.UserSettingsHub
import org.bhargav.pansariwala.feature.user.UserShopTabScreen
import org.bhargav.pansariwala.feature.user.UserThemeScreen
import org.bhargav.pansariwala.notification.NotificationRouter
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_show
import pansariwala.shared.generated.resources.tab_home
import pansariwala.shared.generated.resources.tab_orders
import pansariwala.shared.generated.resources.tab_profile
import pansariwala.shared.generated.resources.tab_shop

private val userNavConfig = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(UserRoute.Splash::class, UserRoute.Splash.serializer())
            subclass(UserRoute.PhoneAuth::class, UserRoute.PhoneAuth.serializer())
            subclass(UserRoute.ProfileSetup::class, UserRoute.ProfileSetup.serializer())
            subclass(UserRoute.Address::class, UserRoute.Address.serializer())
            subclass(UserRoute.LocationAccess::class, UserRoute.LocationAccess.serializer())
            subclass(UserRoute.Shell::class, UserRoute.Shell.serializer())
            subclass(UserRoute.ShopCatalog::class, UserRoute.ShopCatalog.serializer())
            subclass(UserRoute.Cart::class, UserRoute.Cart.serializer())
            subclass(UserRoute.Checkout::class, UserRoute.Checkout.serializer())
            subclass(UserRoute.ThankYou::class, UserRoute.ThankYou.serializer())
            subclass(UserRoute.OrderDetails::class, UserRoute.OrderDetails.serializer())
            subclass(UserRoute.OrdersList::class, UserRoute.OrdersList.serializer())
            subclass(UserRoute.Transactions::class, UserRoute.Transactions.serializer())
            subclass(UserRoute.Help::class, UserRoute.Help.serializer())
            subclass(UserRoute.NotificationPrefs::class, UserRoute.NotificationPrefs.serializer())
            subclass(UserRoute.LanguagePrefs::class, UserRoute.LanguagePrefs.serializer())
            subclass(UserRoute.ThemePrefs::class, UserRoute.ThemePrefs.serializer())
        }
    }
}

@Composable
fun UserNavGraph() {
    val backStack = rememberNavBackStack(userNavConfig, UserRoute.Splash)
    val snackbarHostState = remember { SnackbarHostState() }
    val openAction = stringResource(Res.string.action_show)
    fun replaceAll(route: UserRoute) { backStack.clear(); backStack.add(route) }
    fun push(route: UserRoute) { backStack.add(route) }
    fun pop() { if (backStack.size > 1) backStack.removeLastOrNull() }
    fun popOr(fallback: UserRoute) {
        if (backStack.size > 1) backStack.removeLastOrNull() else replaceAll(fallback)
    }

    LaunchedEffect(Unit) {
        NotificationRouter.events.collect { notification ->
            val result = snackbarHostState.showSnackbar(
                message = "${notification.title}: ${notification.body}",
                actionLabel = if (notification.orderId != null) openAction else null,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                notification.orderId?.let { push(UserRoute.OrderDetails(it)) }
            }
        }
    }

    Box(Modifier.fillMaxSize().safeDrawingPadding().animateContentSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = {
                when (backStack.lastOrNull()) {
                    is UserRoute.OrderDetails -> popOr(UserRoute.Shell)
                    else -> pop()
                }
            },
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
                        )
                    }
                    UserRoute.PhoneAuth -> NavEntry(route) {
                        PhoneAuthScreen(onVerified = { complete ->
                            if (complete) replaceAll(UserRoute.Shell) else replaceAll(UserRoute.ProfileSetup)
                        })
                    }
                    UserRoute.ProfileSetup -> NavEntry(route) {
                        ProfileSetupScreen(
                            onDone = {
                                if (backStack.size > 1) pop() else replaceAll(UserRoute.LocationAccess)
                            },
                        )
                    }
                    UserRoute.Address -> NavEntry(route) {
                        AddressScreen(onDone = { pop() }, onBack = { pop() })
                    }
                    UserRoute.LocationAccess -> NavEntry(route) {
                        UserLocationAccessScreen(onDone = { replaceAll(UserRoute.Shell) })
                    }
                    UserRoute.Shell -> NavEntry(route) {
                        UserShell(
                            onOpenShop = { push(UserRoute.ShopCatalog(it)) },
                            onOpenCart = { shopId -> push(UserRoute.Cart(shopId)) },
                            onOrder = { push(UserRoute.OrderDetails(it)) },
                            onAllOrders = { push(UserRoute.OrdersList) },
                            onTxns = { push(UserRoute.Transactions) },
                            onHelp = { push(UserRoute.Help) },
                            onLanguage = { push(UserRoute.LanguagePrefs) },
                            onNotifications = { push(UserRoute.NotificationPrefs) },
                            onTheme = { push(UserRoute.ThemePrefs) },
                            onLogout = { replaceAll(UserRoute.PhoneAuth) },
                        )
                    }
                    is UserRoute.ShopCatalog -> NavEntry(route) {
                        ShopCatalogScreen(
                            shopId = route.shopId,
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
                    is UserRoute.OrderDetails -> NavEntry(route) {
                        OrderDetailsScreen(
                            route.orderId,
                            onBack = { popOr(UserRoute.Shell) },
                        )
                    }
                    UserRoute.OrdersList -> NavEntry(route) {
                        OrdersListScreen(onOpen = { push(UserRoute.OrderDetails(it)) }, onBack = { pop() })
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
    onOpenCart: (String) -> Unit,
    onOrder: (String) -> Unit,
    onAllOrders: () -> Unit,
    onTxns: () -> Unit,
    onHelp: () -> Unit,
    onLanguage: () -> Unit,
    onNotifications: () -> Unit,
    onTheme: () -> Unit,
    onLogout: () -> Unit,
) {
    var tab by remember { mutableIntStateOf(0) }
    val cart: CartStore = koinInject()
    val cartLines by cart.lines.collectAsStateWithLifecycle(emptyList())
    val cartShopId by cart.shopId.collectAsStateWithLifecycle(null)
    val cartShopName by cart.shopName.collectAsStateWithLifecycle(null)
    val cartCount = cartLines.sumOf { it.quantity.toInt() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Column(Modifier.weight(1f).padding(bottom = 4.dp)) {
                when (tab) {
                    0 -> MarketScreen(onOpenShop = onOpenShop, onContinueOrder = onOpenCart)
                    1 -> UserShopTabScreen(
                        activeShopId = cartShopId,
                        activeShopName = cartShopName,
                        onOpenShop = onOpenShop,
                    )
                    2 -> AccountHomeScreen(
                        onRecent = onOrder,
                        onAllOrders = onAllOrders,
                        onTxns = onTxns,
                        onHelp = onHelp,
                    )
                    else -> UserSettingsHub(onLanguage, onNotifications, onTheme, onLogout)
                }
            }
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Text("H") },
                    label = { Text(stringResource(Res.string.tab_home)) },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Text("S") },
                    label = { Text(stringResource(Res.string.tab_shop)) },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Text("O") },
                    label = { Text(stringResource(Res.string.tab_orders)) },
                )
                NavigationBarItem(
                    selected = tab == 3,
                    onClick = { tab = 3 },
                    icon = { Text("P") },
                    label = { Text(stringResource(Res.string.tab_profile)) },
                )
            }
        }
        if (cartCount > 0 && cartShopId != null && tab in 0..1) {
            CartFloatingPill(
                itemCount = cartCount,
                onClick = { onOpenCart(cartShopId!!) },
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 80.dp),
            )
        }
    }
}
