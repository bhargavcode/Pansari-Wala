package org.bhargav.pansariwala.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.animation.animateContentSize
import androidx.compose.material3.SnackbarDuration
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
import org.bhargav.pansariwala.feature.login.LoginScreen
import org.bhargav.pansariwala.feature.order.OrderEditorScreen
import org.bhargav.pansariwala.feature.order.OrdersWorkspaceScreen
import org.bhargav.pansariwala.feature.settings.SettingsScreen
import org.bhargav.pansariwala.feature.splash.SplashScreen
import org.bhargav.pansariwala.notification.NotificationRouter
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_show

private val navSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(AppRoute.Splash::class, AppRoute.Splash.serializer())
            subclass(AppRoute.Login::class, AppRoute.Login.serializer())
            subclass(AppRoute.Dashboard::class, AppRoute.Dashboard.serializer())
            subclass(AppRoute.Settings::class, AppRoute.Settings.serializer())
            subclass(AppRoute.AddEditInventory::class, AppRoute.AddEditInventory.serializer())
            subclass(AppRoute.InventoryList::class, AppRoute.InventoryList.serializer())
            subclass(AppRoute.OrderEditor::class, AppRoute.OrderEditor.serializer())
            subclass(AppRoute.OrdersWorkspace::class, AppRoute.OrdersWorkspace.serializer())
            subclass(AppRoute.OnlineOrders::class, AppRoute.OnlineOrders.serializer())
        }
    }
}

@Composable
fun AppNavGraph(
    analytics: Analytics = koinInject(),
) {
    val backStack = rememberNavBackStack(navSavedStateConfiguration, AppRoute.Splash)
    var previousScreen by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val openAction = stringResource(Res.string.action_show)

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
        NotificationRouter.events.collect { notification ->
            val result = snackbarHostState.showSnackbar(
                message = "${notification.title}: ${notification.body}",
                actionLabel = if (notification.orderId != null) openAction else null,
                duration = SnackbarDuration.Long,
            )
            if (result == SnackbarResult.ActionPerformed) {
                if (notification.type == AppConstants.Notification.TYPE_ONLINE_ORDER) {
                    push(AppRoute.OnlineOrders)
                } else {
                    notification.orderId?.let { push(AppRoute.OrdersWorkspace(it)) }
                }
            }
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
                            onEditOrder = { orderId -> push(AppRoute.OrdersWorkspace(orderId)) },
                            onAddOrUpdateInventory = { push(AppRoute.AddEditInventory()) },
                            onShowLowStockList = { push(AppRoute.InventoryList(lowStockOnly = true)) },
                            onShowFullInventory = { push(AppRoute.InventoryList(lowStockOnly = false)) },
                            onOpenSettings = { push(AppRoute.Settings) },
                            onOpenOrdersWorkspace = { push(AppRoute.OrdersWorkspace()) },
                            onOpenOnlineOrders = { push(AppRoute.OnlineOrders) },
                            onLogout = { replaceAll(AppRoute.Login) },
                        )
                    }

                    AppRoute.Settings -> NavEntry(route) {
                        SettingsScreen(onBack = { pop() })
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
                            onEditProduct = { productId -> push(AppRoute.AddEditInventory(productId)) },
                        )
                    }

                    is AppRoute.OrderEditor -> NavEntry(route) {
                        OrderEditorScreen(
                            orderId = route.orderId,
                            onBack = { pop() },
                            onSaved = { pop() },
                        )
                    }

                    is AppRoute.OrdersWorkspace -> NavEntry(route) {
                        OrdersWorkspaceScreen(
                            focusOrderId = route.orderId,
                            onBack = { pop() },
                        )
                    }

                    AppRoute.OnlineOrders -> NavEntry(route) {
                        org.bhargav.pansariwala.feature.order.OnlineOrdersScreen(onBack = { pop() })
                    }
                }
            },
        )

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp),
        )
    }
}
