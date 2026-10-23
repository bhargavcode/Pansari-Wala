package org.bhargav.pansariwala.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.SnackbarDuration
import pansariwala.shared.generated.resources.printer_print_success
import org.jetbrains.compose.resources.getString
import org.bhargav.pansariwala.feature.printer.resolve
import org.bhargav.pansariwala.feature.printer.toPrintMessage
import org.bhargav.pansariwala.printing.PosPrintService
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.savedstate.serialization.SavedStateConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import org.bhargav.pansariwala.analytics.Analytics
import org.bhargav.pansariwala.analytics.AnalyticsEvent
import org.bhargav.pansariwala.feature.dashboard.DashboardScreen
import org.bhargav.pansariwala.feature.inventory.AddEditInventoryScreen
import org.bhargav.pansariwala.feature.inventory.InventoryListScreen
import org.bhargav.pansariwala.feature.inventory.MasterCatalogScreen
import org.bhargav.pansariwala.feature.inventory.InventoryProductDetailScreen
import org.bhargav.pansariwala.feature.login.LoginScreen
import org.bhargav.pansariwala.feature.order.OrderEditorScreen
import org.bhargav.pansariwala.feature.order.OrdersWorkspaceScreen
import org.bhargav.pansariwala.feature.printer.PrinterSettingsScreen
import org.bhargav.pansariwala.feature.settings.SettingsScreen
import org.bhargav.pansariwala.feature.splash.SplashScreen
import org.bhargav.pansariwala.api.SessionExpiredBus
import org.bhargav.pansariwala.notification.NotificationOpenRequests
import org.bhargav.pansariwala.notification.NotificationRouter
import androidx.compose.runtime.collectAsState
import org.bhargav.pansariwala.printer.PrinterManager
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_show
import pansariwala.shared.generated.resources.error_session_expired_login

private val navSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(AppRoute.Splash::class, AppRoute.Splash.serializer())
            subclass(AppRoute.Login::class, AppRoute.Login.serializer())
            subclass(AppRoute.Dashboard::class, AppRoute.Dashboard.serializer())
            subclass(AppRoute.Settings::class, AppRoute.Settings.serializer())
            subclass(AppRoute.AddEditInventory::class, AppRoute.AddEditInventory.serializer())
            subclass(AppRoute.InventoryList::class, AppRoute.InventoryList.serializer())
            subclass(AppRoute.ProductDetail::class, AppRoute.ProductDetail.serializer())
            subclass(AppRoute.MasterCatalog::class, AppRoute.MasterCatalog.serializer())
            subclass(AppRoute.OrderEditor::class, AppRoute.OrderEditor.serializer())
            subclass(AppRoute.OrdersWorkspace::class, AppRoute.OrdersWorkspace.serializer())
            subclass(AppRoute.OnlineOrders::class, AppRoute.OnlineOrders.serializer())
            subclass(AppRoute.CustomerRatings::class, AppRoute.CustomerRatings.serializer())
            subclass(AppRoute.PrinterSettings::class, AppRoute.PrinterSettings.serializer())
            subclass(AppRoute.Reports::class, AppRoute.Reports.serializer())
            subclass(AppRoute.CategorySales::class, AppRoute.CategorySales.serializer())
            subclass(AppRoute.ProductSales::class, AppRoute.ProductSales.serializer())
            subclass(AppRoute.ShopProfile::class, AppRoute.ShopProfile.serializer())
            subclass(AppRoute.PosSettings::class, AppRoute.PosSettings.serializer())
            subclass(AppRoute.Subscription::class, AppRoute.Subscription.serializer())
            subclass(AppRoute.CommissionDay::class, AppRoute.CommissionDay.serializer())
            subclass(AppRoute.CommissionOrder::class, AppRoute.CommissionOrder.serializer())
            subclass(AppRoute.Borrowers::class, AppRoute.Borrowers.serializer())
            subclass(AppRoute.ShopOrderDetail::class, AppRoute.ShopOrderDetail.serializer())
            subclass(AppRoute.BorrowerDetail::class, AppRoute.BorrowerDetail.serializer())
        }
    }
}

@Composable
fun AppNavGraph(
    analytics: Analytics = koinInject(),
    printerManager: PrinterManager = koinInject(),
    printService: PosPrintService = koinInject(),
) {
    val backStack = rememberNavBackStack(navSavedStateConfiguration, AppRoute.Splash)
    var previousScreen by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val openAction = stringResource(Res.string.action_show)
    val sessionExpiredMessage = stringResource(Res.string.error_session_expired_login)

    fun replaceAll(route: AppRoute) {
        backStack.clear()
        backStack.add(route)
    }

    fun push(route: AppRoute) {
        backStack.add(route)
    }

    fun pop() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    LaunchedEffect(backStack.lastOrNull()) {
        val current = (backStack.lastOrNull() as? AppRoute)?.screenName() ?: return@LaunchedEffect
        if (current != previousScreen) {
            analytics.log(
                AnalyticsEvent.ScreenView(
                    fromScreen = previousScreen,
                    toScreen = current,
                ),
            )
            previousScreen = current
        }
    }

    LaunchedEffect(Unit) {
        SessionExpiredBus.events.collect {
            if (backStack.lastOrNull() is AppRoute.Login) return@collect
            replaceAll(AppRoute.Login)
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
                if (notification.type == AppConstants.Notification.TYPE_ONLINE_ORDER ||
                    notification.type == AppConstants.Notification.TYPE_PAYMENT_CONFIRM
                ) {
                    push(AppRoute.OnlineOrders)
                } else {
                    notification.orderId?.let { push(AppRoute.OrdersWorkspace(it)) }
                }
            }
        }
    }

    val pendingOpen by NotificationOpenRequests.pending.collectAsState()
    val currentRoute = backStack.lastOrNull()
    val canOpen = currentRoute != null && currentRoute !is AppRoute.Splash && currentRoute !is AppRoute.Login
    LaunchedEffect(pendingOpen, canOpen) {
        val request = pendingOpen ?: return@LaunchedEffect
        if (!canOpen) return@LaunchedEffect
        NotificationOpenRequests.consume()
        when {
            request.type == AppConstants.Notification.TYPE_ONLINE_ORDER ||
                request.type == AppConstants.Notification.TYPE_PAYMENT_CONFIRM -> {
                if (backStack.lastOrNull() !is AppRoute.OnlineOrders) push(AppRoute.OnlineOrders)
            }
            request.orderId != null -> push(AppRoute.OrdersWorkspace(request.orderId))
        }
    }

    LaunchedEffect(printService) {
        printService.results.collect { result ->
            val message = result.fold(
                onSuccess = { getString(Res.string.printer_print_success) },
                onFailure = { it.toPrintMessage().resolve() },
            )
            snackbarHostState.showSnackbar(message = message, duration = SnackbarDuration.Short)
        }
    }

    // Printer health monitoring / auto-reconnect only while signed in and the app is visible.
    val signedIn = backStack.none { it is AppRoute.Splash || it is AppRoute.Login }
    if (signedIn) {
        LifecycleStartEffect(printerManager) {
            printerManager.setAppInForeground(true)
            onStopOrDispose { printerManager.setAppInForeground(false) }
        }
    }

    Box(modifier = Modifier.fillMaxSize().safeDrawingPadding().animateContentSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = { pop() },
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = { key ->
                when (val route = key as AppRoute) {
                    AppRoute.Splash -> NavEntry(route) {
                        SplashScreen(
                            onNavigateToLogin = { replaceAll(AppRoute.Login) },
                            onNavigateToHome = { replaceAll(AppRoute.Dashboard) },
                        )
                    }

                    AppRoute.Login -> NavEntry(route) {
                        LoginScreen(
                            onLoginSuccess = { replaceAll(AppRoute.Dashboard) },
                        )
                    }

                    AppRoute.Dashboard -> NavEntry(route) {
                        DashboardScreen(
                            onCreateOrder = { push(AppRoute.OrderEditor()) },
                            onEditOrder = { orderId -> push(AppRoute.OrderEditor(orderId = orderId)) },
                            onAddOrUpdateInventory = { push(AppRoute.AddEditInventory()) },
                            onShowLowStockList = { push(AppRoute.InventoryList(lowStockOnly = true)) },
                            onShowFullInventory = { push(AppRoute.InventoryList(lowStockOnly = false)) },
                            onOpenSettings = { push(AppRoute.Settings) },
                            onOpenPrinter = { push(AppRoute.PrinterSettings) },
                            onOpenOrdersWorkspace = { push(AppRoute.OrdersWorkspace()) },
                            onOpenOnlineOrders = { push(AppRoute.OnlineOrders) },
                            onOpenReports = { push(AppRoute.Reports) },
                            onOpenProfile = { push(AppRoute.ShopProfile) },
                            onOpenSubscription = { push(AppRoute.Subscription) },
                            onOpenBorrowers = { push(AppRoute.Borrowers) },
                            onOpenMasterCatalog = { push(AppRoute.MasterCatalog) },
                            onOpenProduct = { productId -> push(AppRoute.ProductDetail(productId)) },
                            onOpenPosSettings = { push(AppRoute.PosSettings) },
                            onLogout = { replaceAll(AppRoute.Login) },
                        )
                    }

                    AppRoute.Settings -> NavEntry(route) {
                        SettingsScreen(onBack = { pop() })
                    }

                    AppRoute.PrinterSettings -> NavEntry(route) {
                        PrinterSettingsScreen(onBack = { pop() })
                    }

                    is AppRoute.AddEditInventory -> NavEntry(route) {
                        AddEditInventoryScreen(
                            productId = route.productId,
                            onBack = { pop() },
                            onSaved = { pop() },
                        )
                    }

                    is AppRoute.InventoryList -> NavEntry(route) {
                        InventoryListScreen(
                            lowStockOnly = route.lowStockOnly,
                            onBack = { pop() },
                            onOpenProduct = { productId -> push(AppRoute.ProductDetail(productId)) },
                            onAddCustomProduct = { push(AppRoute.AddEditInventory()) },
                            onAddFromMaster = { push(AppRoute.MasterCatalog) },
                        )
                    }

                    is AppRoute.ProductDetail -> NavEntry(route) {
                        InventoryProductDetailScreen(
                            productId = route.productId,
                            onBack = { pop() },
                            onEdit = { productId -> push(AppRoute.AddEditInventory(productId)) },
                        )
                    }

                    AppRoute.MasterCatalog -> NavEntry(route) {
                        MasterCatalogScreen(
                            onBack = { pop() },
                            onOpenProduct = { productId -> push(AppRoute.ProductDetail(productId)) },
                        )
                    }

                    is AppRoute.OrderEditor -> NavEntry(route) {
                        OrderEditorScreen(
                            orderId = route.orderId,
                            reorderFromId = route.reorderFromId,
                            onBack = { pop() },
                            onSaved = { pop() },
                            onOpenSubscription = { push(AppRoute.Subscription) },
                        )
                    }

                    is AppRoute.OrdersWorkspace -> NavEntry(route) {
                        OrdersWorkspaceScreen(
                            focusOrderId = route.orderId,
                            onBack = { pop() },
                            onEditOrder = { orderId -> push(AppRoute.OrderEditor(orderId = orderId)) },
                            onReorder = { orderId -> push(AppRoute.OrderEditor(reorderFromId = orderId)) },
                        )
                    }

                    AppRoute.OnlineOrders -> NavEntry(route) {
                        org.bhargav.pansariwala.feature.order.OnlineOrdersScreen(
                            onBack = { pop() },
                            onOpenCustomerRatings = { id, name -> push(AppRoute.CustomerRatings(id, name)) },
                        )
                    }

                    is AppRoute.CustomerRatings -> NavEntry(route) {
                        org.bhargav.pansariwala.feature.order.CustomerRatingsScreen(
                            customerId = route.customerId,
                            customerName = route.customerName,
                            onBack = { pop() },
                        )
                    }

                    AppRoute.Reports -> NavEntry(route) {
                        org.bhargav.pansariwala.feature.reports.ReportsScreen(
                            onBack = { pop() },
                            onOpenCategory = { category, window -> push(AppRoute.CategorySales(category, window)) },
                            onOpenOrder = { orderId, online -> push(AppRoute.ShopOrderDetail(orderId, online)) },
                        )
                    }

                    is AppRoute.ShopOrderDetail -> NavEntry(route) {
                        org.bhargav.pansariwala.feature.reports.ShopOrderDetailScreen(
                            orderId = route.orderId,
                            online = route.online,
                            onBack = { pop() },
                            onEditOrder = { orderId -> push(AppRoute.OrderEditor(orderId = orderId)) },
                            onReorder = { orderId -> push(AppRoute.OrderEditor(reorderFromId = orderId)) },
                        )
                    }

                    is AppRoute.CategorySales -> NavEntry(route) {
                        org.bhargav.pansariwala.feature.reports.CategorySalesScreen(
                            category = route.category,
                            window = route.window,
                            onBack = { pop() },
                            onOpenProduct = { id, name, window -> push(AppRoute.ProductSales(id, name, window)) },
                        )
                    }

                    is AppRoute.ProductSales -> NavEntry(route) {
                        org.bhargav.pansariwala.feature.reports.ProductSalesScreen(
                            productId = route.productId,
                            productName = route.productName,
                            window = route.window,
                            onBack = { pop() },
                        )
                    }

                    AppRoute.ShopProfile -> NavEntry(route) {
                        org.bhargav.pansariwala.feature.profile.ShopProfileScreen(
                            onBack = { pop() },
                            onOpenSubscription = { push(AppRoute.Subscription) },
                        )
                    }

                    AppRoute.PosSettings -> NavEntry(route) {
                        org.bhargav.pansariwala.feature.profile.ShopProfileScreen(
                            onBack = { pop() },
                            onOpenSubscription = { push(AppRoute.Subscription) },
                            settingsMode = true,
                        )
                    }

                    AppRoute.Subscription -> NavEntry(route) {
                        org.bhargav.pansariwala.feature.subscription.SubscriptionScreen(
                            onBack = { pop() },
                            onOpenDay = { day -> push(AppRoute.CommissionDay(day)) },
                        )
                    }

                    is AppRoute.CommissionDay -> NavEntry(route) {
                        org.bhargav.pansariwala.feature.subscription.CommissionDayScreen(
                            day = route.day,
                            onBack = { pop() },
                            onOpenOrder = { orderId -> push(AppRoute.CommissionOrder(route.day, orderId)) },
                        )
                    }

                    is AppRoute.CommissionOrder -> NavEntry(route) {
                        org.bhargav.pansariwala.feature.subscription.CommissionOrderScreen(
                            day = route.day,
                            orderId = route.orderId,
                            onBack = { pop() },
                        )
                    }

                    AppRoute.Borrowers -> NavEntry(route) {
                        org.bhargav.pansariwala.feature.udhaar.BorrowersReportScreen(
                            onBack = { pop() },
                            onOpenBorrower = { phone -> push(AppRoute.BorrowerDetail(phone)) },
                        )
                    }

                    is AppRoute.BorrowerDetail -> NavEntry(route) {
                        org.bhargav.pansariwala.feature.udhaar.BorrowerDetailScreen(
                            phone = route.phone,
                            onBack = { pop() },
                        )
                    }
                }
            },
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp),
        )
    }
}
