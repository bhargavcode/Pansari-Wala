package org.bhargav.pansariwala.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.analytics.Analytics
import org.bhargav.pansariwala.analytics.AnalyticsEvent
import org.bhargav.pansariwala.designsystem.AdaptivePane
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.StatTile
import org.bhargav.pansariwala.designsystem.WindowWidthClass
import org.bhargav.pansariwala.domain.auth.AuthRepository
import org.bhargav.pansariwala.domain.model.OrderSummary
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DashboardScreen(
    onCreateOrder: () -> Unit,
    onEditOrder: (String) -> Unit,
    onAddOrUpdateInventory: () -> Unit,
    onShowLowStockList: () -> Unit,
    onShowFullInventory: () -> Unit,
    onLogout: () -> Unit,
    viewModel: DashboardViewModel = koinViewModel(),
    authRepository: AuthRepository = koinInject(),
    analytics: Analytics = koinInject(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    if (state.loading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    AdaptivePane(Modifier.fillMaxSize()) { widthClass ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Namaste, ${state.userName}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Here's your shop today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = {
                    analytics.log(AnalyticsEvent.ButtonClicked("logout", "dashboard"))
                    scope.launch {
                        authRepository.logout()
                        onLogout()
                    }
                }) { Text("Log out") }
            }

            SalesCard(state)

            if (widthClass == WindowWidthClass.Expanded) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    OrdersCard(
                        orders = state.recentOrders,
                        onCreateOrder = onCreateOrder,
                        onEditOrder = onEditOrder,
                        modifier = Modifier.weight(1f),
                    )
                    InventoryCard(
                        state = state,
                        onAddOrUpdateInventory = onAddOrUpdateInventory,
                        onShowLowStockList = onShowLowStockList,
                        onShowFullInventory = onShowFullInventory,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                OrdersCard(
                    orders = state.recentOrders,
                    onCreateOrder = onCreateOrder,
                    onEditOrder = onEditOrder,
                    modifier = Modifier.fillMaxWidth(),
                )
                InventoryCard(
                    state = state,
                    onAddOrUpdateInventory = onAddOrUpdateInventory,
                    onShowLowStockList = onShowLowStockList,
                    onShowFullInventory = onShowFullInventory,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun SalesCard(state: DashboardUiState) {
    SectionCard(
        title = "Today's sales",
        subtitle = "Completed orders since midnight",
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                label = "Orders",
                value = state.todaySales.orderCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "Revenue",
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
    onCreateOrder: () -> Unit,
    onEditOrder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = "Recent orders",
        subtitle = "Last ${orders.size} orders",
        modifier = modifier,
        action = { Button(onClick = onCreateOrder) { Text("Create order") } },
    ) {
        if (orders.isEmpty()) {
            Text(
                text = "No orders yet. Create your first order.",
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
                            text = order.customerName ?: "Walk-in",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                        )
                        Text(
                            text = "${order.itemCount} items · ${order.totalValue.asMoney()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { onEditOrder(order.id) }) { Text("Edit") }
                }
                if (index < orders.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun InventoryCard(
    state: DashboardUiState,
    onAddOrUpdateInventory: () -> Unit,
    onShowLowStockList: () -> Unit,
    onShowFullInventory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SectionCard(
        title = "Inventory",
        subtitle = "${state.totalProducts} products · ${state.totalInventoryValue.asMoney()} at cost",
        modifier = modifier,
        action = { OutlinedButton(onClick = onAddOrUpdateInventory) { Text("Add / Update") } },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(
                text = "Stock value by category",
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
                    text = "Low stock (${state.lowStockTotalCount})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = onShowFullInventory) { Text("All inventory") }
            }

            if (state.lowStockItems.isEmpty()) {
                Text(
                    text = "All items are well stocked.",
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
                            Text("Show full low-stock list")
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
                text = product.category.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = "${product.stockQty.asQuantity()} ${product.unit.label} left",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.error,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
