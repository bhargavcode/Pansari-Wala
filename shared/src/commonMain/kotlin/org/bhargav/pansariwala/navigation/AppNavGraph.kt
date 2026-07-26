package org.bhargav.pansariwala.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import org.bhargav.pansariwala.feature.splash.SplashScreen
import org.koin.compose.koinInject

private val navSavedStateConfiguration = SavedStateConfiguration {
    serializersModule = SerializersModule {
        polymorphic(NavKey::class) {
            subclass(AppRoute.Splash::class, AppRoute.Splash.serializer())
            subclass(AppRoute.Login::class, AppRoute.Login.serializer())
            subclass(AppRoute.Dashboard::class, AppRoute.Dashboard.serializer())
            subclass(AppRoute.AddEditInventory::class, AppRoute.AddEditInventory.serializer())
            subclass(AppRoute.InventoryList::class, AppRoute.InventoryList.serializer())
            subclass(AppRoute.OrderEditor::class, AppRoute.OrderEditor.serializer())
        }
    }
}

@Composable
fun AppNavGraph(
    analytics: Analytics = koinInject(),
) {
    val backStack = rememberNavBackStack(navSavedStateConfiguration, AppRoute.Splash)
    var previousScreen by remember { mutableStateOf<String?>(null) }

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
                        onEditOrder = { orderId -> push(AppRoute.OrderEditor(orderId)) },
                        onAddOrUpdateInventory = { push(AppRoute.AddEditInventory()) },
                        onShowLowStockList = { push(AppRoute.InventoryList(lowStockOnly = true)) },
                        onShowFullInventory = { push(AppRoute.InventoryList(lowStockOnly = false)) },
                        onLogout = { replaceAll(AppRoute.Login) },
                    )
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
            }
        },
    )
}
