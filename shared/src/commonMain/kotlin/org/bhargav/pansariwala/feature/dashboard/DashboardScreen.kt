package org.bhargav.pansariwala.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.analytics.Analytics
import org.bhargav.pansariwala.analytics.AnalyticsEvent
import org.bhargav.pansariwala.designsystem.AdaptivePane
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.StatTile
import org.bhargav.pansariwala.designsystem.WindowWidthClass
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.domain.auth.AuthRepository
import org.bhargav.pansariwala.domain.auth.ShopAccess
import org.bhargav.pansariwala.domain.auth.ShopPermission
import org.bhargav.pansariwala.domain.model.OrderSummary
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.feature.printer.PrinterStatusButton
import org.bhargav.pansariwala.i18n.localizeCustomerName
import org.bhargav.pansariwala.i18n.localizedLabel
import org.bhargav.pansariwala.i18n.localizedName
import org.bhargav.pansariwala.ui.AsyncUiState
import org.bhargav.pansariwala.ui.errorBannerOrNull
import org.bhargav.pansariwala.ui.isBlockingLoad
import org.bhargav.pansariwala.ui.isRefreshing
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.printing.formatPrintDateTime
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_edit
import pansariwala.shared.generated.resources.action_logout
import pansariwala.shared.generated.resources.action_settings
import pansariwala.shared.generated.resources.add_update_inventory
import pansariwala.shared.generated.resources.all_inventory
import pansariwala.shared.generated.resources.all_well_stocked
import pansariwala.shared.generated.resources.create_order
import pansariwala.shared.generated.resources.dashboard_greeting
import pansariwala.shared.generated.resources.dashboard_subtitle
import pansariwala.shared.generated.resources.inventory_subtitle
import pansariwala.shared.generated.resources.inventory_title
import pansariwala.shared.generated.resources.low_stock_count
import pansariwala.shared.generated.resources.no_orders_yet
import pansariwala.shared.generated.resources.order_items_summary
import pansariwala.shared.generated.resources.orders_workspace_title
import pansariwala.shared.generated.resources.profile_title
import pansariwala.shared.generated.resources.online_orders_title
import pansariwala.shared.generated.resources.recent_orders_subtitle
import pansariwala.shared.generated.resources.reports_title
import pansariwala.shared.generated.resources.recent_orders_title
import pansariwala.shared.generated.resources.sales_orders
import pansariwala.shared.generated.resources.sales_revenue
import pansariwala.shared.generated.resources.sales_subtitle
import pansariwala.shared.generated.resources.sales_title
import pansariwala.shared.generated.resources.shopkeeper_fallback
import pansariwala.shared.generated.resources.show_full_low_stock
import pansariwala.shared.generated.resources.stock_by_category
import pansariwala.shared.generated.resources.stock_left

@Composable
fun DashboardScreen(
    onCreateOrder: () -> Unit,
    onEditOrder: (String) -> Unit,
    onAddOrUpdateInventory: () -> Unit,
    onShowLowStockList: () -> Unit,
    onShowFullInventory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPrinter: () -> Unit,
    onOpenOrdersWorkspace: () -> Unit,
    onOpenOnlineOrders: () -> Unit,
    onOpenReports: () -> Unit,
    onOpenProfile: () -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
    authRepository: AuthRepository = koinInject(),
    analytics: Analytics = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val access by viewModel.access.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val success = state as? AsyncUiState.Success

    PansariScreen(
        error = state.errorBannerOrNull(),
        isLoading = state.isBlockingLoad(),
        isRefreshing = state.isRefreshing(),
        onErrorAction = { handleErrorBannerAction(it, onRetry = {}, onDismiss = {}) },
    ) {
        val data = success?.data ?: return@PansariScreen
        AdaptivePane(Modifier.fillMaxSize()) { widthClass ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                stickyHeader {
                    Column(Modifier.background(MaterialTheme.colorScheme.surface)) {
                        DashboardHeader(data, analytics, onOpenSettings = {
                            analytics.log(AnalyticsEvent.ButtonClicked("settings", "dashboard"))
                            onOpenSettings.invoke()
                        }, scope, authRepository, onLogout = {
                            analytics.log(AnalyticsEvent.ButtonClicked("logout", "dashboard"))
                            onLogout.invoke()
                        }, onOpenPrinter = {
                            analytics.log(AnalyticsEvent.ButtonClicked("printer", "dashboard"))
                            onOpenPrinter.invoke()
                        }, onOpenProfile = {
                            analytics.log(AnalyticsEvent.ButtonClicked("profile", "dashboard"))
                            onOpenProfile.invoke()
                        }, access = access, compact = widthClass == WindowWidthClass.Compact)
                        if (widthClass != WindowWidthClass.Compact && access.can(ShopPermission.VIEW_REPORTS)) {
                            SalesCard(data)
                        }
                    }
                }

                if (widthClass == WindowWidthClass.Compact && access.can(ShopPermission.VIEW_REPORTS)) {
                    item { SalesCard(data) }
                }

                val showOrders = access.can(ShopPermission.BILLING) ||
                    access.can(ShopPermission.ONLINE_ORDERS) ||
                    access.can(ShopPermission.VIEW_REPORTS)
                val showInventory = access.can(ShopPermission.INVENTORY)
                if (widthClass == WindowWidthClass.Expanded) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            if (showOrders) {
                                OrdersCard(
                                    orders = data.recentOrders,
                                    access = access,
                                    onCreateOrder = onCreateOrder,
                                    onEditOrder = onEditOrder,
                                    onOpenOrdersWorkspace = onOpenOrdersWorkspace,
                                    onOpenOnlineOrders = onOpenOnlineOrders,
                                    onOpenReports = onOpenReports,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (showInventory) {
                                InventoryCard(
                                    state = data,
                                    onAddOrUpdateInventory = onAddOrUpdateInventory,
                                    onShowLowStockList = onShowLowStockList,
                                    onShowFullInventory = onShowFullInventory,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                } else {
                    if (showOrders) {
                        item {
                            OrdersCard(
                                orders = data.recentOrders,
                                access = access,
                                onCreateOrder = onCreateOrder,
                                onEditOrder = onEditOrder,
                                onOpenOrdersWorkspace = onOpenOrdersWorkspace,
                                onOpenOnlineOrders = onOpenOnlineOrders,
                                onOpenReports = onOpenReports,
                                compact = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                    if (showInventory) {
                        item {
                            InventoryCard(
                                state = data,
                                onAddOrUpdateInventory = onAddOrUpdateInventory,
                                onShowLowStockList = onShowLowStockList,
                                onShowFullInventory = onShowFullInventory,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DashboardHeader(
    state: DashboardData,
    analytics: Analytics,
    onOpenSettings: () -> Unit,
    scope: CoroutineScope,
    authRepository: AuthRepository,
    onLogout: () -> Unit,
    onOpenPrinter: () -> Unit,
    onOpenProfile: () -> Unit,
    access: ShopAccess,
    compact: Boolean = false,
) {
    val showPrinter = access.can(ShopPermission.BILLING) || access.can(ShopPermission.PRINT_REPORTS) ||
        access.can(ShopPermission.SETTINGS)
    val actions = @Composable {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onOpenProfile) {
                Text(stringResource(Res.string.profile_title), style = MaterialTheme.typography.titleMedium)
            }
            if (access.can(ShopPermission.SETTINGS)) {
                TextButton(onClick = {
                    onOpenSettings.invoke()
                }) {
                    Text("⚙ ${stringResource(Res.string.action_settings)}",
                        style = MaterialTheme.typography.titleMedium,)
                }
            }
            TextButton(onClick = {
                analytics.log(AnalyticsEvent.ButtonClicked("logout", "dashboard"))
                scope.launch {
                    authRepository.logout()
                    onLogout.invoke()
                }
            }) { Text(stringResource(Res.string.action_logout),
                style = MaterialTheme.typography.titleMedium) }
            if (!compact && showPrinter) PrinterStatusButton(onClick = onOpenPrinter)
        }
    }
    val greeting = @Composable { modifier: Modifier ->
        Column(modifier = modifier) {
            val userName = state.userName.ifBlank {
                stringResource(Res.string.shopkeeper_fallback)
            }
            Text(
                text = stringResource(Res.string.dashboard_greeting, userName),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(Res.string.dashboard_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    if (compact) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                greeting(Modifier.weight(1f))
                if (showPrinter) PrinterStatusButton(onClick = onOpenPrinter)
            }
            actions()
        }
    } else {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            greeting(Modifier.weight(1f))
            actions()
        }
    }
}
@Composable
private fun SalesCard(state: DashboardData) {
    SectionCard(
        title = stringResource(Res.string.sales_title),
        subtitle = stringResource(Res.string.sales_subtitle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                label = stringResource(Res.string.sales_orders),
                value = state.todaySales.orderCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(Res.string.sales_revenue),
                value = state.todaySales.totalValue.asMoney(),
                modifier = Modifier.weight(1f),
                container = MaterialTheme.colorScheme.tertiaryContainer,
                onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
}

@Composable
private fun OrdersCard(
    orders: List<OrderSummary>,
    access: ShopAccess,
    onCreateOrder: () -> Unit,
    onEditOrder: (String) -> Unit,
    onOpenOrdersWorkspace: () -> Unit,
    onOpenOnlineOrders: () -> Unit,
    onOpenReports: () -> Unit,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val canBill = access.can(ShopPermission.BILLING)
    val buttons = @Composable { buttonModifier: Modifier ->
        if (canBill) {
            OutlinedButton(onClick = onOpenOrdersWorkspace, modifier = buttonModifier) {
                Text(stringResource(Res.string.orders_workspace_title))
            }
        }
        if (access.can(ShopPermission.ONLINE_ORDERS)) {
            val newOnline = org.bhargav.pansariwala.feature.order.rememberNewOnlineOrderCount()
            OutlinedButton(onClick = onOpenOnlineOrders, modifier = buttonModifier) {
                Text(stringResource(Res.string.online_orders_title))
                org.bhargav.pansariwala.feature.order.NewOrdersBadge(newOnline)
            }
        }
        if (access.can(ShopPermission.VIEW_REPORTS)) {
            OutlinedButton(onClick = onOpenReports, modifier = buttonModifier) {
                Text(stringResource(Res.string.reports_title))
            }
        }
        if (canBill) {
            Button(onClick = onCreateOrder, modifier = buttonModifier) {
                Text(stringResource(Res.string.create_order))
            }
        }
    }
    SectionCard(
        title = stringResource(Res.string.recent_orders_title),
        subtitle = stringResource(Res.string.recent_orders_subtitle, orders.size),
        modifier = modifier,
        action = {
            if (compact) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) { buttons(Modifier.fillMaxWidth()) }
            } else {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) { buttons(Modifier) }
            }
        },
    ) {
        if (orders.isEmpty()) {
            Text(
                text = stringResource(Res.string.no_orders_yet),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SectionCard
        }
        Column {
            orders.forEachIndexed { index, order ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = localizeCustomerName(order.customerName),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = stringResource(
                                Res.string.order_items_summary,
                                order.itemCount.toString(),
                                order.totalValue.asMoney(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = formatPrintDateTime(order.createdAtEpochMs),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (canBill) {
                        TextButton(onClick = { onEditOrder(order.id) }) {
                            Text(stringResource(Res.string.action_edit))
                        }
                    }
                }
                if (index < orders.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun InventoryCard(
    state: DashboardData,
    onAddOrUpdateInventory: () -> Unit,
    onShowLowStockList: () -> Unit,
    onShowFullInventory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = stringResource(Res.string.inventory_title),
        subtitle = stringResource(
            Res.string.inventory_subtitle,
            state.totalProducts,
            state.totalInventoryValue.asMoney(),
        ),
        modifier = modifier,
        action = {
            OutlinedButton(onClick = onAddOrUpdateInventory) {
                Text(stringResource(Res.string.add_update_inventory))
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = stringResource(Res.string.stock_by_category),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
            CategoryBarChart(data = state.categoryBreakdown, modifier = Modifier.fillMaxWidth())

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.low_stock_count, state.lowStockTotalCount),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = onShowFullInventory) {
                    Text(stringResource(Res.string.all_inventory))
                }
            }

            if (state.lowStockItems.isEmpty()) {
                Text(
                    text = stringResource(Res.string.all_well_stocked),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column {
                    state.lowStockItems.forEach { product ->
                        LowStockRow(product)
                    }
                    Box(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                        TextButton(
                            onClick = onShowLowStockList,
                            modifier = Modifier.widthIn(min = 0.dp).align(Alignment.Center),
                        ) {
                            Text(stringResource(Res.string.show_full_low_stock))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LowStockRow(product: Product) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = product.category.localizedName(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = stringResource(
                Res.string.stock_left,
                product.stockQty.asQuantity(),
                product.unit.localizedLabel(),
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
