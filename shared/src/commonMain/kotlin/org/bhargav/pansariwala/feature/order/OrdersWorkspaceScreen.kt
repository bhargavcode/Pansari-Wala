package org.bhargav.pansariwala.feature.order

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.AdaptivePane
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.StatTile
import org.bhargav.pansariwala.designsystem.PansariTopBar
import org.bhargav.pansariwala.designsystem.WindowWidthClass
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.domain.model.OrderSummary
import org.bhargav.pansariwala.i18n.localizeCustomerName
import org.bhargav.pansariwala.settings.CancelOrderReason
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_back
import pansariwala.shared.generated.resources.action_cancel_order
import pansariwala.shared.generated.resources.action_no
import pansariwala.shared.generated.resources.action_process_order
import pansariwala.shared.generated.resources.action_yes
import pansariwala.shared.generated.resources.cancel_order_message
import pansariwala.shared.generated.resources.cancel_order_title
import pansariwala.shared.generated.resources.cancel_reason_custom_hint
import pansariwala.shared.generated.resources.cancel_reason_label
import pansariwala.shared.generated.resources.no_orders_yet
import pansariwala.shared.generated.resources.order_details_title
import pansariwala.shared.generated.resources.order_items_summary
import pansariwala.shared.generated.resources.order_status_label
import pansariwala.shared.generated.resources.orders_workspace_title
import pansariwala.shared.generated.resources.sales_orders
import pansariwala.shared.generated.resources.sales_revenue
import pansariwala.shared.generated.resources.select_order_hint
import pansariwala.shared.generated.resources.status_cancelled
import pansariwala.shared.generated.resources.status_completed
import pansariwala.shared.generated.resources.status_draft
import pansariwala.shared.generated.resources.status_received
import pansariwala.shared.generated.resources.status_accepted
import pansariwala.shared.generated.resources.status_packing
import pansariwala.shared.generated.resources.status_looking_partner
import pansariwala.shared.generated.resources.status_partner_accepted
import pansariwala.shared.generated.resources.status_on_the_way
import pansariwala.shared.generated.resources.status_delivered
import pansariwala.shared.generated.resources.status_rejected
import pansariwala.shared.generated.resources.tab_dashboard
import pansariwala.shared.generated.resources.tab_order_details
import pansariwala.shared.generated.resources.tab_orders
import pansariwala.shared.generated.resources.total_label

@Composable
fun OrdersWorkspaceScreen(
    focusOrderId: String?,
    onBack: () -> Unit,
    viewModel: OrdersWorkspaceViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(focusOrderId) { viewModel.focusOrder(focusOrderId) }

    AdaptivePane(modifier = Modifier.fillMaxSize()) { widthClass ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
        ) {
            PansariTopBar(
                title = stringResource(Res.string.orders_workspace_title),
                onBack = onBack,
            )

            if (widthClass == WindowWidthClass.Expanded) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    DashboardPane(state, Modifier.weight(1f))
                    OrdersListPane(
                        orders = state.orders,
                        selectedId = state.selectedOrder?.id,
                        onSelect = viewModel::selectOrder,
                        modifier = Modifier.weight(1f),
                    )
                    OrderDetailsPane(
                        state = state,
                        onProcess = viewModel::processOrder,
                        onCancel = viewModel::openCancelDialog,
                        modifier = Modifier.weight(1.2f),
                    )
                }
            } else {
                var tab by remember { mutableIntStateOf(if (focusOrderId != null) 2 else 1) }
                PrimaryScrollableTabRow(selectedTabIndex = tab) {
                    Tab(
                        selected = tab == 0,
                        onClick = { tab = 0 },
                        text = { Text(stringResource(Res.string.tab_dashboard)) },
                    )
                    Tab(
                        selected = tab == 1,
                        onClick = { tab = 1 },
                        text = { Text(stringResource(Res.string.tab_orders)) },
                    )
                    Tab(
                        selected = tab == 2,
                        onClick = { tab = 2 },
                        text = { Text(stringResource(Res.string.tab_order_details)) },
                    )
                }
                when (tab) {
                    0 -> DashboardPane(state, Modifier.fillMaxSize().padding(top = 12.dp))
                    1 -> OrdersListPane(
                        orders = state.orders,
                        selectedId = state.selectedOrder?.id,
                        onSelect = {
                            viewModel.selectOrder(it)
                            tab = 2
                        },
                        modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                    )
                    else -> OrderDetailsPane(
                        state = state,
                        onProcess = viewModel::processOrder,
                        onCancel = viewModel::openCancelDialog,
                        modifier = Modifier.fillMaxSize().padding(top = 12.dp),
                    )
                }
            }
        }
    }

        if (state.showCancelDialog) {
        CancelOrderBottomSheet(
            reason = state.cancelReason,
            customReason = state.customCancelReason,
            onReasonChange = viewModel::onCancelReasonChange,
            onCustomReasonChange = viewModel::onCustomCancelReasonChange,
            onConfirm = viewModel::confirmCancelOrder,
            onDismiss = viewModel::dismissCancelDialog,
                busy = state.cancellingOrder,
        )
    }
}

@Composable
private fun DashboardPane(state: OrdersWorkspaceUiState, modifier: Modifier = Modifier) {
    SectionCard(
        title = stringResource(Res.string.tab_dashboard),
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                label = stringResource(Res.string.sales_orders),
                value = state.todayOrders.toString(),
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = stringResource(Res.string.sales_revenue),
                value = state.todayRevenue.asMoney(),
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun OrdersListPane(
    orders: List<OrderSummary>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            stringResource(Res.string.tab_orders),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        if (orders.isEmpty()) {
            Text(stringResource(Res.string.no_orders_yet))
        } else {
            LazyColumn {
                items(orders, key = { it.id }) { order ->
                    val selected = order.id == selectedId
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(order.id) }
                            .padding(vertical = 10.dp),
                    ) {
                    Text(
                        text = order.id,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                        Text(
                            text = localizeCustomerName(order.customerName),
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
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
                            text = stringResource(
                                Res.string.order_status_label,
                                orderStatusLabel(order.status),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun OrderDetailsPane(
    state: OrdersWorkspaceUiState,
    onProcess: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val order = state.selectedOrder
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        Text(
            stringResource(Res.string.order_details_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (order == null) {
            Text(
                stringResource(Res.string.select_order_hint),
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return
        }
        val itemsTotal = order.items.sumOf { it.lineTotal }

        Text(
            text = order.id,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            localizeCustomerName(order.customerName),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(top = 8.dp),
        )
        Text(
            stringResource(Res.string.order_status_label, orderStatusLabel(order.status)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            stringResource(Res.string.total_label, itemsTotal.asMoney()),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(vertical = 8.dp),
        )
        order.items.forEach { item ->
            Text(
                "${item.productName} × ${item.quantity} · ${item.lineTotal.asMoney()}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        if (order.cancelReason != null) {
            Text(
                "${stringResource(Res.string.cancel_reason_label)}: ${order.cancelReason}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        val canAct = order.status == OrderStatus.RECEIVED || order.status == OrderStatus.DRAFT
        if (canAct) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onProcess,
                    enabled = !state.processingOrder && !state.cancellingOrder,
                    modifier = Modifier.weight(1f),
                ) {
                    if (state.processingOrder) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(Res.string.action_process_order))
                    }
                }
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !state.processingOrder && !state.cancellingOrder,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(Res.string.action_cancel_order))
                }
            }
        } else if (order.status == OrderStatus.COMPLETED) {
            OutlinedButton(
                onClick = onCancel,
                enabled = !state.processingOrder && !state.cancellingOrder,
                modifier = Modifier.padding(top = 16.dp),
            ) {
                Text(stringResource(Res.string.action_cancel_order))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CancelOrderBottomSheet(
    reason: CancelOrderReason,
    customReason: String,
    onReasonChange: (CancelOrderReason) -> Unit,
    onCustomReasonChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    busy: Boolean,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(Res.string.cancel_order_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(stringResource(Res.string.cancel_order_message))
            Text(
                stringResource(Res.string.cancel_reason_label),
                style = MaterialTheme.typography.titleSmall,
            )
            CancelOrderReason.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = reason == option,
                            onClick = { onReasonChange(option) },
                            enabled = !busy,
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = reason == option,
                        onClick = { onReasonChange(option) },
                    )
                    Text(
                        stringResource(option.labelRes),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }

            if (reason == CancelOrderReason.OTHER) {
                OutlinedTextField(
                    value = customReason,
                    onValueChange = onCustomReasonChange,
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.cancel_reason_custom_hint)) },
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(Res.string.action_no))
                    }
                }
                Button(
                    onClick = onConfirm,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) {
                    if (busy) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(Res.string.action_yes))
                    }
                }
            }
        }
    }
}

@Composable
private fun orderStatusLabel(status: OrderStatus): String = stringResource(
    when (status) {
        OrderStatus.RECEIVED -> Res.string.status_received
        OrderStatus.COMPLETED -> Res.string.status_completed
        OrderStatus.CANCELLED -> Res.string.status_cancelled
        OrderStatus.DRAFT -> Res.string.status_draft
        OrderStatus.ACCEPTED -> Res.string.status_accepted
        OrderStatus.PACKING -> Res.string.status_packing
        OrderStatus.LOOKING_FOR_PARTNER -> Res.string.status_looking_partner
        OrderStatus.PARTNER_ACCEPTED -> Res.string.status_partner_accepted
        OrderStatus.ON_THE_WAY -> Res.string.status_on_the_way
        OrderStatus.DELIVERED -> Res.string.status_delivered
        OrderStatus.REJECTED -> Res.string.status_rejected
    },
)
