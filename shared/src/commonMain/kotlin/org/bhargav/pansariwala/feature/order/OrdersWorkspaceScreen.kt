package org.bhargav.pansariwala.feature.order

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.bhargav.pansariwala.designsystem.AppBottomSheet
import org.bhargav.pansariwala.designsystem.OutlinedTextField
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.domain.model.OrderSummary
import org.bhargav.pansariwala.domain.model.publicOrderNumber
import org.bhargav.pansariwala.feature.printer.PrintActionsRow
import org.bhargav.pansariwala.feature.printer.orderPrintActions
import org.bhargav.pansariwala.i18n.localizeCustomerName
import org.bhargav.pansariwala.printing.formatPrintDate
import org.bhargav.pansariwala.printing.formatPrintDateTime
import org.bhargav.pansariwala.settings.CancelOrderReason
import org.bhargav.pansariwala.util.AppClock
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_cancel_order
import pansariwala.shared.generated.resources.action_clear
import pansariwala.shared.generated.resources.action_no
import pansariwala.shared.generated.resources.action_process_order
import pansariwala.shared.generated.resources.action_yes
import pansariwala.shared.generated.resources.cancel_order_message
import pansariwala.shared.generated.resources.cancel_order_title
import pansariwala.shared.generated.resources.cancel_reason_custom_hint
import pansariwala.shared.generated.resources.cancel_reason_label
import pansariwala.shared.generated.resources.order_card_total
import pansariwala.shared.generated.resources.order_detail_discount
import pansariwala.shared.generated.resources.order_detail_items
import pansariwala.shared.generated.resources.order_detail_open_editor
import pansariwala.shared.generated.resources.order_detail_reorder
import pansariwala.shared.generated.resources.order_detail_subtotal
import pansariwala.shared.generated.resources.orders_empty_filtered
import pansariwala.shared.generated.resources.orders_filter_all
import pansariwala.shared.generated.resources.orders_filter_cancelled
import pansariwala.shared.generated.resources.orders_filter_completed
import pansariwala.shared.generated.resources.orders_filter_open
import pansariwala.shared.generated.resources.orders_search_hint
import pansariwala.shared.generated.resources.orders_stat_avg_bill
import pansariwala.shared.generated.resources.orders_stat_cancelled
import pansariwala.shared.generated.resources.orders_today
import pansariwala.shared.generated.resources.orders_workspace_title
import pansariwala.shared.generated.resources.orders_yesterday
import pansariwala.shared.generated.resources.no_orders_yet
import pansariwala.shared.generated.resources.sales_orders
import pansariwala.shared.generated.resources.sales_revenue
import pansariwala.shared.generated.resources.select_order_hint
import pansariwala.shared.generated.resources.status_accepted
import pansariwala.shared.generated.resources.status_cancelled
import pansariwala.shared.generated.resources.status_completed
import pansariwala.shared.generated.resources.status_delivered
import pansariwala.shared.generated.resources.status_draft
import pansariwala.shared.generated.resources.status_looking_partner
import pansariwala.shared.generated.resources.status_on_the_way
import pansariwala.shared.generated.resources.status_packing
import pansariwala.shared.generated.resources.status_partner_accepted
import pansariwala.shared.generated.resources.status_payment_pending
import pansariwala.shared.generated.resources.status_received
import pansariwala.shared.generated.resources.status_rejected
import pansariwala.shared.generated.resources.status_returned_to_shop
import pansariwala.shared.generated.resources.status_returning_to_shop

private val TWO_PANE_MIN_WIDTH = 720.dp
private val SUCCESS = Color(0xFF2E7D32)
private val WARNING = Color(0xFFEF6C00)
private val INFO = Color(0xFF1565C0)
private const val DAY_MS = 24L * 60 * 60 * 1000

@Composable
fun OrdersWorkspaceScreen(
    focusOrderId: String?,
    onBack: () -> Unit,
    onEditOrder: (String) -> Unit = {},
    onReorder: (String) -> Unit = {},
    viewModel: OrdersWorkspaceViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var detailOpen by rememberSaveable { mutableStateOf(focusOrderId != null) }
    LaunchedEffect(focusOrderId) { viewModel.focusOrder(focusOrderId) }

    PansariScreen(
        title = stringResource(Res.string.orders_workspace_title),
        onBack = onBack,
        isLoading = state.loading,
    ) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val twoPane = maxWidth >= TWO_PANE_MIN_WIDTH
            val details = @Composable { modifier: Modifier ->
                OrderDetailsPane(
                    state = state,
                    onProcess = viewModel::processOrder,
                    onCancel = viewModel::openCancelDialog,
                    onEdit = onEditOrder,
                    onReorder = onReorder,
                    modifier = modifier,
                )
            }
            when {
                twoPane -> Row(Modifier.fillMaxSize()) {
                    OrdersListPane(
                        state = state,
                        viewModel = viewModel,
                        onSelect = viewModel::selectOrder,
                        modifier = Modifier.weight(0.42f).fillMaxHeight(),
                    )
                    Box(
                        Modifier
                            .weight(0.58f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest),
                    ) {
                        details(Modifier.fillMaxSize())
                    }
                }
                detailOpen && state.selectedOrder != null -> {
                    NavigationBackHandler(
                        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
                        isBackEnabled = true,
                        onBackCompleted = { detailOpen = false },
                    )
                    details(Modifier.fillMaxSize())
                }
                else -> OrdersListPane(
                    state = state,
                    viewModel = viewModel,
                    onSelect = {
                        viewModel.selectOrder(it)
                        detailOpen = true
                    },
                    highlightSelection = false,
                    modifier = Modifier.fillMaxSize(),
                )
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
}

// ---------- List ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OrdersListPane(
    state: OrdersWorkspaceUiState,
    viewModel: OrdersWorkspaceViewModel,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    highlightSelection: Boolean = true,
) {
    val todayStart = remember(state.orders) { AppClock.startOfTodayMillis() }
    val groups = remember(state.visibleOrders, todayStart) {
        state.visibleOrders.groupBy { dayBucket(it.createdAtEpochMs, todayStart) }
    }
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "stats") { TodayStats(state) }
        item(key = "search") {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                placeholder = { Text(stringResource(Res.string.orders_search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = if (state.query.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.action_clear))
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item(key = "filters") { FilterRow(state.filter, state.filterCounts, viewModel::onFilterChange) }
        if (state.visibleOrders.isEmpty()) {
            item(key = "empty") {
                EmptyOrders(if (state.orders.isEmpty()) Res.string.no_orders_yet else Res.string.orders_empty_filtered)
            }
        }
        groups.forEach { (bucket, orders) ->
            stickyHeader(key = "day_$bucket") { DayHeader(bucket, orders.first().createdAtEpochMs) }
            items(orders, key = { it.id }) { order ->
                OrderCard(
                    order = order,
                    number = publicOrderNumber(state.shopId, order.id),
                    selected = highlightSelection && order.id == state.selectedOrder?.id,
                    onClick = { onSelect(order.id) },
                )
            }
        }
    }
}

/** 0 = today, 1 = yesterday, larger = days ago. */
private fun dayBucket(epochMs: Long, todayStart: Long): Long =
    if (epochMs >= todayStart) 0 else (todayStart - epochMs - 1) / DAY_MS + 1

@Composable
private fun DayHeader(bucket: Long, sampleMs: Long) {
    val label = when (bucket) {
        0L -> stringResource(Res.string.orders_today)
        1L -> stringResource(Res.string.orders_yesterday)
        else -> formatPrintDate(sampleMs)
    }
    Surface(color = MaterialTheme.colorScheme.surface, modifier = Modifier.fillMaxWidth()) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
        )
    }
}

@Composable
private fun TodayStats(state: OrdersWorkspaceUiState) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatChip(stringResource(Res.string.sales_orders), state.todayOrders.toString(), INFO, Modifier.weight(1f))
        StatChip(stringResource(Res.string.sales_revenue), state.todayRevenue.asMoney(), SUCCESS, Modifier.weight(1f))
    }
    Spacer(Modifier.size(10.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StatChip(stringResource(Res.string.orders_stat_avg_bill), state.todayAverage.asMoney(), MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
        StatChip(stringResource(Res.string.orders_stat_cancelled), state.todayCancelled.toString(), MaterialTheme.colorScheme.error, Modifier.weight(1f))
    }
}

@Composable
private fun StatChip(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.16f), accent.copy(alpha = 0.04f))))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = accent, maxLines = 1)
    }
}

@Composable
private fun FilterRow(selected: OrdersFilter, counts: Map<OrdersFilter, Int>, onSelect: (OrdersFilter) -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OrdersFilter.entries.forEach { f ->
            val accent = f.accent()
            FilterChip(
                selected = selected == f,
                onClick = { onSelect(f) },
                label = { Text("${stringResource(f.labelRes())}  ${counts[f] ?: 0}") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accent.copy(alpha = 0.16f),
                    selectedLabelColor = accent,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == f,
                    selectedBorderColor = accent.copy(alpha = 0.5f),
                    selectedBorderWidth = 1.dp,
                ),
                shape = RoundedCornerShape(50),
            )
        }
    }
}

private fun OrdersFilter.labelRes() = when (this) {
    OrdersFilter.ALL -> Res.string.orders_filter_all
    OrdersFilter.OPEN -> Res.string.orders_filter_open
    OrdersFilter.COMPLETED -> Res.string.orders_filter_completed
    OrdersFilter.CANCELLED -> Res.string.orders_filter_cancelled
}

@Composable
private fun OrdersFilter.accent(): Color = when (this) {
    OrdersFilter.ALL -> MaterialTheme.colorScheme.primary
    OrdersFilter.OPEN -> WARNING
    OrdersFilter.COMPLETED -> SUCCESS
    OrdersFilter.CANCELLED -> MaterialTheme.colorScheme.error
}

@Composable
private fun OrderCard(order: OrderSummary, number: String, selected: Boolean, onClick: () -> Unit) {
    val accent = statusAccent(order.status)
    val name = localizeCustomerName(order.customerName)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    name.firstOrNull()?.uppercase() ?: "#",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    "#$number · ${formatPrintDateTime(order.createdAtEpochMs).substringAfter(' ')}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
                Text(
                    stringResource(Res.string.order_detail_items, order.itemCount),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(order.totalValue.asMoney(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                StatusPill(order.status)
            }
        }
    }
}

@Composable
private fun EmptyOrders(message: org.jetbrains.compose.resources.StringResource) {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(44.dp))
        Text(stringResource(message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun StatusPill(status: OrderStatus, onDark: Boolean = false) {
    val accent = if (onDark) Color.White else statusAccent(status)
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(if (onDark) Color.White.copy(alpha = 0.2f) else accent.copy(alpha = 0.14f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(accent))
        Text(orderStatusLabel(status), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = accent)
    }
}

@Composable
private fun statusAccent(status: OrderStatus): Color = when {
    status == OrderStatus.COMPLETED || status == OrderStatus.DELIVERED -> SUCCESS
    status == OrderStatus.CANCELLED || status == OrderStatus.REJECTED -> MaterialTheme.colorScheme.error
    status.isOpenPos() -> WARNING
    else -> INFO
}

// ---------- Details ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderDetailsPane(
    state: OrdersWorkspaceUiState,
    onProcess: () -> Unit,
    onCancel: () -> Unit,
    onEdit: (String) -> Unit,
    onReorder: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val order = state.selectedOrder
    if (order == null) {
        Box(modifier.padding(24.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(48.dp))
                Text(stringResource(Res.string.select_order_hint), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        return
    }
    val busy = state.processingOrder || state.cancellingOrder
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        DetailHeader(order)
        ItemsCard(order)
        order.cancelReason?.let { reason ->
            Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Block, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Text(
                        "${stringResource(Res.string.cancel_reason_label)}: ${cancelReasonText(reason)}",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (order.status.isOpenPos()) {
                Button(
                    onClick = onProcess,
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = SUCCESS, contentColor = Color.White),
                ) {
                    if (state.processingOrder) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        ActionLabel(Icons.Default.CheckCircle, stringResource(Res.string.action_process_order))
                    }
                }
            }
            if (order.status != OrderStatus.CANCELLED) {
                FilledTonalButton(onClick = { onEdit(order.id) }, enabled = !busy) {
                    ActionLabel(Icons.Default.Edit, stringResource(Res.string.order_detail_open_editor))
                }
            }
            OutlinedButton(onClick = { onReorder(order.id) }, enabled = !busy) {
                ActionLabel(Icons.Default.Replay, stringResource(Res.string.order_detail_reorder))
            }
            if (order.status != OrderStatus.CANCELLED) {
                OutlinedButton(
                    onClick = onCancel,
                    enabled = !busy,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                ) {
                    ActionLabel(Icons.Default.Block, stringResource(Res.string.action_cancel_order))
                }
            }
        }
        PrintActionsRow(
            actions = remember(order) { orderPrintActions(order) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ActionLabel(icon: ImageVector, text: String) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
    Spacer(Modifier.width(8.dp))
    Text(text)
}

@Composable
private fun DetailHeader(order: Order) {
    val scheme = MaterialTheme.colorScheme
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(scheme.primary, scheme.tertiary)))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#${order.displayNumber}",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = scheme.onPrimary.copy(alpha = 0.9f),
                modifier = Modifier.weight(1f),
            )
            StatusPill(order.status, onDark = true)
        }
        Text(
            localizeCustomerName(order.customerName),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = scheme.onPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            formatPrintDateTime(order.createdAtEpochMs),
            style = MaterialTheme.typography.bodySmall,
            color = scheme.onPrimary.copy(alpha = 0.85f),
        )
        Row(verticalAlignment = Alignment.Bottom, modifier = Modifier.padding(top = 6.dp)) {
            Text(
                order.totalValue.asMoney(),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = scheme.onPrimary,
                modifier = Modifier.weight(1f),
            )
            if (order.paymentMethod.isNotBlank()) {
                Text(
                    orderPaymentLabel(order),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onPrimary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(Color.White.copy(alpha = 0.2f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun ItemsCard(order: Order) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                stringResource(Res.string.order_detail_items, order.itemCount),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            order.items.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            buildString {
                                append(item.productName)
                                item.variantLabel?.takeIf(String::isNotBlank)?.let { append(" · ").append(it) }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            "${item.quantity.asQuantity()} × ${item.unitPrice.asMoney()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(item.lineTotal.asMoney(), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            TotalRow(stringResource(Res.string.order_detail_subtotal), order.subtotal.asMoney())
            if (order.discount > 0) {
                TotalRow(stringResource(Res.string.order_detail_discount), "−${order.discount.asMoney()}", SUCCESS)
            }
            TotalRow(stringResource(Res.string.order_card_total), order.totalValue.asMoney(), emphasize = true)
        }
    }
}

@Composable
private fun TotalRow(label: String, value: String, color: Color = Color.Unspecified, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            label,
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else null,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
            color = color,
        )
    }
}

@Composable
private fun cancelReasonText(raw: String): String =
    CancelOrderReason.entries.firstOrNull { it.name == raw }?.let { stringResource(it.labelRes) } ?: raw

// ---------- Cancel sheet ----------

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
    AppBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.cancel_order_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(Res.string.cancel_order_message), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(stringResource(Res.string.cancel_reason_label), style = MaterialTheme.typography.titleSmall)
            CancelOrderReason.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .selectable(
                            selected = reason == option,
                            onClick = { onReasonChange(option) },
                            enabled = !busy,
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = reason == option, onClick = null)
                    Text(stringResource(option.labelRes), modifier = Modifier.padding(start = 8.dp))
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.action_no))
                }
                Button(
                    onClick = onConfirm,
                    enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
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
        OrderStatus.RETURNING_TO_SHOP -> Res.string.status_returning_to_shop
        OrderStatus.RETURNED_TO_SHOP -> Res.string.status_returned_to_shop
        OrderStatus.DRAFT -> Res.string.status_draft
        OrderStatus.PAYMENT_PENDING -> Res.string.status_payment_pending
        OrderStatus.ACCEPTED -> Res.string.status_accepted
        OrderStatus.PACKING -> Res.string.status_packing
        OrderStatus.LOOKING_FOR_PARTNER -> Res.string.status_looking_partner
        OrderStatus.PARTNER_ACCEPTED -> Res.string.status_partner_accepted
        OrderStatus.ON_THE_WAY -> Res.string.status_on_the_way
        OrderStatus.DELIVERED -> Res.string.status_delivered
        OrderStatus.REJECTED -> Res.string.status_rejected
    },
)
