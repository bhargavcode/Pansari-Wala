package org.bhargav.pansariwala.feature.order

import org.bhargav.pansariwala.api.ShopPartnerDto
import org.bhargav.pansariwala.feature.profile.RiderEnrollSheet

import org.bhargav.pansariwala.designsystem.AppAlertDialog
import org.bhargav.pansariwala.designsystem.AppBottomSheet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import pansariwala.shared.generated.resources.action_assign_own_partner
import pansariwala.shared.generated.resources.action_remove
import pansariwala.shared.generated.resources.action_save
import pansariwala.shared.generated.resources.own_partner_add
import pansariwala.shared.generated.resources.own_partner_assign
import pansariwala.shared.generated.resources.own_partner_empty
import pansariwala.shared.generated.resources.own_partner_not_on_app
import pansariwala.shared.generated.resources.own_partner_on_app
import pansariwala.shared.generated.resources.own_partner_sheet_title
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.notification.OrderUpdates
import org.koin.compose.koinInject
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import org.bhargav.pansariwala.util.AppConstants
import pansariwala.shared.generated.resources.action_clear
import pansariwala.shared.generated.resources.action_refresh
import pansariwala.shared.generated.resources.online_orders_search_hint
import pansariwala.shared.generated.resources.online_orders_tab_accepted
import pansariwala.shared.generated.resources.online_orders_tab_active
import pansariwala.shared.generated.resources.online_orders_tab_all
import pansariwala.shared.generated.resources.online_orders_tab_cancelled
import pansariwala.shared.generated.resources.online_orders_tab_delivered
import pansariwala.shared.generated.resources.online_orders_tab_delivery
import pansariwala.shared.generated.resources.online_orders_tab_new
import pansariwala.shared.generated.resources.online_orders_tab_packing
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.ui.text.input.KeyboardType
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.action_mark_delivered
import pansariwala.shared.generated.resources.action_self_delivery
import pansariwala.shared.generated.resources.online_order_self_delivering
import pansariwala.shared.generated.resources.self_delivery_complete_hint
import pansariwala.shared.generated.resources.self_delivery_complete_title
import pansariwala.shared.generated.resources.self_delivery_otp_label
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import org.bhargav.pansariwala.designsystem.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import org.bhargav.pansariwala.feature.delivery.ReturnToShopDialog
import org.bhargav.pansariwala.feature.delivery.returnReasonLabel
import org.bhargav.pansariwala.feature.printer.PrintMenuButton
import org.bhargav.pansariwala.feature.printer.orderPrintActions
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Surface
import pansariwala.shared.generated.resources.order_card_total
import pansariwala.shared.generated.resources.order_card_items_count
import pansariwala.shared.generated.resources.walk_in
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import pansariwala.shared.generated.resources.customer_rating_badge_cd
import pansariwala.shared.generated.resources.order_action_mark_collected
import pansariwala.shared.generated.resources.order_pickup_badge
import pansariwala.shared.generated.resources.action_confirm_payment
import pansariwala.shared.generated.resources.shop_pay_hint
import pansariwala.shared.generated.resources.shop_pay_late
import pansariwala.shared.generated.resources.shop_pay_not_received
import pansariwala.shared.generated.resources.shop_pay_row
import pansariwala.shared.generated.resources.shop_pay_time_left
import pansariwala.shared.generated.resources.shop_pay_title
import pansariwala.shared.generated.resources.shop_pay_txn
import org.bhargav.pansariwala.domain.model.PaymentIntent
import org.bhargav.pansariwala.domain.model.PaymentIntentStatus
import androidx.compose.runtime.produceState
import kotlinx.coroutines.delay
import kotlin.time.Clock
import pansariwala.shared.generated.resources.online_order_payment_utr
import pansariwala.shared.generated.resources.status_payment_pending
import kotlin.math.roundToInt
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.settings.CancelOrderReason
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.printing.formatPrintDateTime
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.online_order_returning
import pansariwala.shared.generated.resources.online_order_returned
import pansariwala.shared.generated.resources.return_shop_verify_title
import pansariwala.shared.generated.resources.return_shop_verify_body
import pansariwala.shared.generated.resources.return_shop_verify_confirm
import pansariwala.shared.generated.resources.return_shop_own_action
import pansariwala.shared.generated.resources.return_shop_verify_action
import pansariwala.shared.generated.resources.return_shop_collected_action
import pansariwala.shared.generated.resources.action_accept_order
import pansariwala.shared.generated.resources.action_back
import pansariwala.shared.generated.resources.action_cancel_order
import pansariwala.shared.generated.resources.action_no
import pansariwala.shared.generated.resources.action_packing
import pansariwala.shared.generated.resources.action_request_delivery
import pansariwala.shared.generated.resources.action_request_delivery_partial
import pansariwala.shared.generated.resources.action_retry
import pansariwala.shared.generated.resources.action_yes
import pansariwala.shared.generated.resources.cancel_order_message
import pansariwala.shared.generated.resources.cancel_order_title
import pansariwala.shared.generated.resources.cancel_reason_custom_hint
import pansariwala.shared.generated.resources.cancel_reason_label
import pansariwala.shared.generated.resources.delivery_partner_name
import pansariwala.shared.generated.resources.delivery_partner_phone
import pansariwala.shared.generated.resources.delivery_partner_title
import pansariwala.shared.generated.resources.delivery_partner_vehicle
import pansariwala.shared.generated.resources.online_order_looking_partner
import pansariwala.shared.generated.resources.online_order_waiting_pickup
import pansariwala.shared.generated.resources.online_orders_empty
import pansariwala.shared.generated.resources.online_orders_title
import pansariwala.shared.generated.resources.status_accepted
import pansariwala.shared.generated.resources.status_delivered
import pansariwala.shared.generated.resources.status_looking_partner
import pansariwala.shared.generated.resources.status_on_the_way
import pansariwala.shared.generated.resources.status_packing
import pansariwala.shared.generated.resources.status_partner_accepted
import pansariwala.shared.generated.resources.status_received
import pansariwala.shared.generated.resources.status_rejected
import pansariwala.shared.generated.resources.status_cancelled
import pansariwala.shared.generated.resources.status_returned_to_shop
import pansariwala.shared.generated.resources.status_returning_to_shop

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineOrdersScreen(
    onBack: () -> Unit,
    onOpenCustomerRatings: (customerId: String, customerName: String) -> Unit = { _, _ -> },
    viewModel: OnlineOrdersViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.onScreenVisible(true)
        onPauseOrDispose { viewModel.onScreenVisible(false) }
    }
    val listState = rememberLazyListState()
    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 1 - AppConstants.LIST_PREFETCH_DISTANCE
        }
    }
    LaunchedEffect(nearEnd, state.hasMore, state.orders.size) {
        if (nearEnd && state.hasMore) viewModel.loadMore()
    }
    PansariScreen(
        title = stringResource(Res.string.online_orders_title),
        onBack = onBack,
        actions = {
            IconButton(onClick = { viewModel.refresh(pullToRefresh = true) }, enabled = !state.refreshing) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.action_refresh))
            }
        },
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = { viewModel.refresh() }, onDismiss = viewModel::dismissError)
        },
        isLoading = state.loading && state.orders.isEmpty(),
        isRefreshing = state.loading && state.orders.isNotEmpty(),
    ) {
        Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = viewModel::setQuery,
            placeholder = { Text(stringResource(Res.string.online_orders_search_hint)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = if (state.query.isNotEmpty()) {
                {
                    IconButton(onClick = { viewModel.setQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.action_clear))
                    }
                }
            } else {
                null
            },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        )
        OnlineOrderTabs(selected = state.tab, newCount = state.newCount, onSelect = viewModel::selectTab)
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { viewModel.refresh(pullToRefresh = true) },
            modifier = Modifier.fillMaxSize(),
        ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.paymentNotice?.let { notice ->
                item(key = "paymentNotice") {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.fillMaxWidth().padding(start = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(notice.asString(), modifier = Modifier.weight(1f))
                            IconButton(onClick = viewModel::consumePaymentNotice) {
                                Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.action_clear))
                            }
                        }
                    }
                }
            }
            if (state.paymentIntents.isNotEmpty()) {
                item(key = "paymentsToConfirm") {
                    PaymentsToConfirmCard(
                        intents = state.paymentIntents,
                        busyId = state.paymentBusyId,
                        onConfirm = viewModel::confirmShopPayment,
                        onReject = viewModel::rejectShopPayment,
                    )
                }
            }
            if (state.orders.isEmpty() && !state.loading && state.error == null) {
                item(key = "empty") { Text(stringResource(Res.string.online_orders_empty)) }
            }
            items(state.orders, key = { it.id }, contentType = { "order" }) { order ->
                val busy = state.busyId == order.id
                val packedChecks = state.packedChecks[order.id].orEmpty()
                val lineColor = onlineOrderStatusColor(order.status)
                Column(
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .drawWithContent {
                            drawContent()
                            val stroke = 4.dp.toPx()
                            drawRect(
                                color = lineColor,
                                topLeft = Offset(0f, size.height - stroke),
                                size = Size(size.width, stroke),
                            )
                        },
                ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    tonalElevation = 1.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                "#${order.displayNumber}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                formatPrintDateTime(order.createdAtEpochMs),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Surface(color = lineColor.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
                            Text(
                                onlineOrderStatusLabel(order.status),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = lineColor,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                        PrintMenuButton(actions = remember(order) { orderPrintActions(order) })
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            order.customerName.orEmpty().ifBlank { stringResource(Res.string.walk_in) },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        val customerId = order.customerId
                        val avg = order.customerRatingAvg
                        if (customerId != null && avg != null) {
                            CustomerRatingBadge(
                                average = avg,
                                onClick = { onOpenCustomerRatings(customerId, order.customerName.orEmpty()) },
                            )
                        }
                        if (order.selfPickup) {
                            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
                                Text(
                                    stringResource(Res.string.order_pickup_badge),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                    }
                    HorizontalDivider()
                    when {
                        order.status == OrderStatus.PACKING -> PackingChecklist(
                            order = order,
                            checked = packedChecks,
                            enabled = !busy,
                            onToggle = { viewModel.togglePacked(order.id, it) },
                            onSetAll = { viewModel.setAllPacked(order.id, it) },
                        )
                        order.isPackedForDelivery -> DeliveringItemsList(order)
                        else -> OrderItemsTable(order.items, collapsedCount = ONLINE_CARD_VISIBLE_ITEMS)
                    }
                    HorizontalDivider()
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${stringResource(Res.string.order_card_items_count, order.itemCount)} · " +
                                stringResource(paymentMethodLabelRes(order.paymentMethod)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${stringResource(Res.string.order_card_total)} ${order.totalValue.asMoney()}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    OnlineOrderStatusNote(order)
                    val isReturn = order.status == OrderStatus.RETURNING_TO_SHOP || order.status == OrderStatus.RETURNED_TO_SHOP
                    (if (isReturn) returnReasonLabel(order.cancelReason) else order.cancelReason)?.let { reason ->
                        Text(
                            text = "${stringResource(Res.string.cancel_reason_label)}: $reason",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    DeliveryPartnerDetails(order)
                    OnlineOrderActions(
                        order = order,
                        busy = busy,
                        packedCount = packedChecks.size,
                        onAccept = { viewModel.accept(order.id) },
                        onPacking = { viewModel.markPacking(order.id) },
                        onRequestPartner = { viewModel.requestDeliveryPartner(order.id) },
                        onSelfDeliver = { viewModel.startSelfDelivery(order.id) },
                        onAssignOwnPartner = { viewModel.openAssignPartner(order.id) },
                        onCompleteSelfDelivery = { viewModel.openSelfDeliverComplete(order.id) },
                        onCancel = { viewModel.openCancel(order.id) },
                        onMarkCollected = { viewModel.markCollected(order.id) },
                        onAcceptReturn = { viewModel.openAcceptReturn(order.id) },
                    )
                }
                }
                }
            }
            if (state.loadingMore) {
                item(key = "loadingMore") {
                    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
        }
        }
        if (state.assignOrderId != null) {
            AssignOwnPartnerSheet(
                state = state,
                onAssign = viewModel::assignPartner,
                onRemove = viewModel::removePartner,
                onOpenForm = viewModel::openPartnerForm,
                onCloseForm = viewModel::closePartnerForm,
                onEnrolled = viewModel::onPartnerEnrolled,
                onDismiss = viewModel::dismissAssignPartner,
            )
        }
        if (state.cancelOrderId != null) {
            CancelOnlineOrderSheet(
                reason = state.cancelReason,
                customReason = state.customCancelReason,
                onReasonChange = viewModel::onCancelReasonChange,
                onCustomReasonChange = viewModel::onCustomCancelReasonChange,
                onConfirm = viewModel::confirmCancel,
                onDismiss = viewModel::dismissCancel,
                busy = state.busyId == state.cancelOrderId,
            )
        }
        state.returnOrderId?.let { orderId ->
            val target = state.orders.firstOrNull { it.id == orderId }
            if (target?.status == OrderStatus.RETURNING_TO_SHOP) {
                AppAlertDialog(
                    onDismissRequest = viewModel::dismissAcceptReturn,
                    title = { Text(stringResource(Res.string.return_shop_verify_title)) },
                    text = { Text(stringResource(Res.string.return_shop_verify_body)) },
                    confirmButton = {
                        Button(onClick = { viewModel.confirmAcceptReturn() }) {
                            Text(stringResource(Res.string.return_shop_verify_confirm))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = viewModel::dismissAcceptReturn) { Text(stringResource(Res.string.action_cancel)) }
                    },
                )
            } else {
                ReturnToShopDialog(
                    busy = state.busyId == orderId,
                    onConfirm = viewModel::confirmAcceptReturn,
                    onDismiss = viewModel::dismissAcceptReturn,
                )
            }
        }
        state.selfDeliverOrderId?.let { orderId ->
            AppAlertDialog(
                onDismissRequest = viewModel::dismissSelfDeliverComplete,
                title = { Text(stringResource(Res.string.self_delivery_complete_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Res.string.self_delivery_complete_hint))
                        OutlinedTextField(
                            value = state.selfDeliverOtp,
                            onValueChange = viewModel::onSelfDeliverOtpChange,
                            label = { Text(stringResource(Res.string.self_delivery_otp_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            isError = state.selfDeliverError != null,
                        )
                        state.selfDeliverError?.let {
                            Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::confirmSelfDeliverComplete,
                        enabled = state.busyId != orderId,
                    ) {
                        if (state.busyId == orderId) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(Res.string.action_mark_delivered))
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissSelfDeliverComplete) {
                        Text(stringResource(Res.string.action_cancel))
                    }
                },
            )
        }
    }
}

@Composable
private fun CustomerRatingBadge(average: Double, onClick: () -> Unit) {
    val label = formatRating(average)
    val description = stringResource(Res.string.customer_rating_badge_cd, label)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Text(
            "★$label",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        )
    }
}

private const val ONLINE_CARD_VISIBLE_ITEMS = 4

internal fun formatRating(value: Double): String = ((value * 10).roundToInt() / 10.0).toString()

@Composable
private fun OnlineOrderStatusNote(order: Order) {
    val note = when (order.status) {
        OrderStatus.LOOKING_FOR_PARTNER -> stringResource(Res.string.online_order_looking_partner)
        OrderStatus.PARTNER_ACCEPTED -> stringResource(Res.string.online_order_waiting_pickup)
        OrderStatus.ON_THE_WAY -> if (order.isSelfDelivery) stringResource(Res.string.online_order_self_delivering) else null
        OrderStatus.RETURNING_TO_SHOP -> stringResource(Res.string.online_order_returning)
        OrderStatus.RETURNED_TO_SHOP -> stringResource(Res.string.online_order_returned)
        else -> null
    }
    note?.let { Text(it) }
}

/** DIRECT_UPI money goes straight to the shop's UPI ID, so the shop confirms it before the order is created. */
@Composable
private fun PaymentsToConfirmCard(
    intents: List<PaymentIntent>,
    busyId: String?,
    onConfirm: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    val now by produceState(Clock.System.now().toEpochMilliseconds()) {
        while (true) {
            value = Clock.System.now().toEpochMilliseconds()
            delay(AppConstants.Payment.COUNTDOWN_TICK_MS)
        }
    }
    SectionCard(title = stringResource(Res.string.shop_pay_title), subtitle = stringResource(Res.string.shop_pay_hint)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            intents.forEach { intent ->
                val busy = busyId == intent.id
                val secondsLeft = ((intent.expiresAtEpochMs - now) / 1_000L).coerceAtLeast(0L)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(Res.string.shop_pay_row, intent.amountInr.asMoney(), intent.customerName),
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(stringResource(Res.string.shop_pay_txn, intent.id), style = MaterialTheme.typography.bodySmall)
                    intent.utr?.let {
                        Text(stringResource(Res.string.online_order_payment_utr, it), fontWeight = FontWeight.SemiBold)
                    }
                    Text(
                        if (intent.status == PaymentIntentStatus.PENDING && secondsLeft > 0) {
                            stringResource(Res.string.shop_pay_time_left, "${secondsLeft / 60}:${(secondsLeft % 60).toString().padStart(2, '0')}")
                        } else {
                            stringResource(Res.string.shop_pay_late)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { onConfirm(intent.id) }, enabled = busyId == null) {
                            if (busy) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text(stringResource(Res.string.action_confirm_payment))
                            }
                        }
                        if (intent.status == PaymentIntentStatus.PENDING) {
                            OutlinedButton(onClick = { onReject(intent.id) }, enabled = busyId == null) {
                                Text(stringResource(Res.string.shop_pay_not_received))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryPartnerDetails(order: Order) {
    if (!order.hasAssignedPartner) return
    Text(stringResource(Res.string.delivery_partner_title), fontWeight = FontWeight.SemiBold)
    order.partnerName?.takeIf { it.isNotBlank() }?.let {
        Text(stringResource(Res.string.delivery_partner_name, it))
    }
    order.partnerPhone?.takeIf { it.isNotBlank() }?.let {
        Text(stringResource(Res.string.delivery_partner_phone, it))
    }
    order.partnerVehicleReg?.takeIf { it.isNotBlank() }?.let {
        Text(stringResource(Res.string.delivery_partner_vehicle, it))
    }
}

@Composable
private fun OnlineOrderActions(
    order: Order,
    busy: Boolean,
    packedCount: Int,
    onAccept: () -> Unit,
    onPacking: () -> Unit,
    onRequestPartner: () -> Unit,
    onSelfDeliver: () -> Unit,
    onAssignOwnPartner: () -> Unit,
    onCompleteSelfDelivery: () -> Unit,
    onCancel: () -> Unit,
    onMarkCollected: () -> Unit,
    onAcceptReturn: () -> Unit,
) {
    val canSelfDeliver = !order.selfPickup && !order.hasAssignedPartner && (
        (order.status == OrderStatus.PACKING && packedCount > 0) ||
            order.status == OrderStatus.LOOKING_FOR_PARTNER
        )
    val selfOnTheWay = order.isSelfDelivery && order.status == OrderStatus.ON_THE_WAY
    if (canSelfDeliver || selfOnTheWay) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (canSelfDeliver) {
                OutlinedButton(onClick = onSelfDeliver, enabled = !busy) {
                    Text(stringResource(Res.string.action_self_delivery))
                }
                OutlinedButton(onClick = onAssignOwnPartner, enabled = !busy) {
                    Text(stringResource(Res.string.action_assign_own_partner))
                }
            }
            if (selfOnTheWay) {
                Button(onClick = onCompleteSelfDelivery, enabled = !busy) {
                    Text(stringResource(Res.string.action_mark_delivered))
                }
                TextButton(onClick = onAcceptReturn, enabled = !busy) {
                    Text(stringResource(Res.string.return_shop_own_action), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
    val nextLabel = when (order.status) {
        OrderStatus.RECEIVED -> stringResource(Res.string.action_accept_order)
        OrderStatus.ACCEPTED -> stringResource(Res.string.action_packing)
        OrderStatus.RETURNING_TO_SHOP -> stringResource(Res.string.return_shop_verify_action)
        OrderStatus.RETURNED_TO_SHOP -> stringResource(Res.string.return_shop_collected_action)
        OrderStatus.PACKING -> if (order.selfPickup) {
            stringResource(Res.string.order_action_mark_collected)
        } else if (packedCount in 1 until order.items.size) {
            stringResource(Res.string.action_request_delivery_partial, packedCount, order.items.size)
        } else {
            stringResource(Res.string.action_request_delivery)
        }
        else -> null
    }
    val nextEnabled = order.status != OrderStatus.PACKING || packedCount > 0
    val nextAction = when (order.status) {
        OrderStatus.RECEIVED -> onAccept
        OrderStatus.ACCEPTED -> onPacking
        OrderStatus.PACKING -> if (order.selfPickup) onMarkCollected else onRequestPartner
        OrderStatus.RETURNING_TO_SHOP -> onAcceptReturn
        OrderStatus.RETURNED_TO_SHOP -> onCompleteSelfDelivery
        else -> null
    }
    if (nextAction == null && !order.canCancel) return
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (nextLabel != null && nextAction != null) {
            Button(onClick = nextAction, enabled = !busy && nextEnabled) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(nextLabel)
                }
            }
        }
        if (order.canCancel) {
            TextButton(onClick = onCancel, enabled = !busy) {
                if (busy) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.action_cancel_order))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CancelOnlineOrderSheet(
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
                            role = Role.RadioButton,
                        )
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = reason == option, onClick = { onReasonChange(option) })
                    Text(stringResource(option.labelRes), modifier = Modifier.padding(start = 8.dp))
                }
            }
            if (reason == CancelOrderReason.OTHER) {
                OutlinedTextField(
                    value = customReason,
                    onValueChange = onCustomReasonChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(Res.string.cancel_reason_custom_hint)) },
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) {
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

/** Not-yet-accepted online orders; refreshed on resume and on every order push. */
@Composable
fun rememberNewOnlineOrderCount(api: PansariApi = koinInject()): Int {
    var count by remember { mutableIntStateOf(0) }
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }
    suspend fun fetch() {
        runCatching { api.shopOnlineOrdersPage(OnlineOrderTab.NEW.statuses, "", 0, 1) }
            .onSuccess { count = it.newCount }
            .onFailure { it.rethrowIfStructuredCancellation() }
    }
    LaunchedEffect(tick) { fetch() }
    LaunchedEffect(Unit) { OrderUpdates.events.collect { fetch() } }
    return count
}

/** Small count badge used next to "Online orders" entry points. */
@Composable
fun NewOrdersBadge(count: Int) {
    if (count <= 0) return
    Spacer(Modifier.width(6.dp))
    Badge { Text(if (count > 99) "99+" else count.toString()) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignOwnPartnerSheet(
    state: OnlineOrdersUiState,
    onAssign: (String) -> Unit,
    onRemove: (String) -> Unit,
    onOpenForm: () -> Unit,
    onCloseForm: () -> Unit,
    onEnrolled: (ShopPartnerDto) -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(Res.string.own_partner_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            state.partnerError?.let {
                Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (state.partnersLoading && state.shopPartners.isEmpty()) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (state.shopPartners.isEmpty()) {
                Text(stringResource(Res.string.own_partner_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.shopPartners.forEach { partner ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(partner.name, fontWeight = FontWeight.SemiBold)
                        Text(
                            listOf(partner.phone, partner.vehicleReg).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            stringResource(if (partner.onPartnerApp) Res.string.own_partner_on_app else Res.string.own_partner_not_on_app),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (partner.onPartnerApp) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { onRemove(partner.id) }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(Res.string.action_remove))
                    }
                    Button(onClick = { onAssign(partner.id) }) {
                        Text(stringResource(Res.string.own_partner_assign))
                    }
                }
            }
            OutlinedButton(onClick = onOpenForm, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text(stringResource(Res.string.own_partner_add))
            }
        }
    }
    if (state.enrollingPartner) RiderEnrollSheet(onAdded = onEnrolled, onDismiss = onCloseForm)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OnlineOrderTabs(
    selected: OnlineOrderTab,
    newCount: Int,
    onSelect: (OnlineOrderTab) -> Unit,
) {
    val tabs = OnlineOrderTab.entries
    PrimaryScrollableTabRow(selectedTabIndex = tabs.indexOf(selected), edgePadding = 12.dp) {
        tabs.forEach { tab ->
            Tab(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(onlineOrderStatusColor(tab.statuses.firstOrNull())),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(onlineOrderTabLabel(tab))
                        if (tab == OnlineOrderTab.NEW) NewOrdersBadge(newCount)
                    }
                },
            )
        }
    }
}

@Composable
private fun onlineOrderTabLabel(tab: OnlineOrderTab): String = stringResource(
    when (tab) {
        OnlineOrderTab.ACTIVE -> Res.string.online_orders_tab_active
        OnlineOrderTab.NEW -> Res.string.online_orders_tab_new
        OnlineOrderTab.ACCEPTED -> Res.string.online_orders_tab_accepted
        OnlineOrderTab.PACKING -> Res.string.online_orders_tab_packing
        OnlineOrderTab.DELIVERY -> Res.string.online_orders_tab_delivery
        OnlineOrderTab.DELIVERED -> Res.string.online_orders_tab_delivered
        OnlineOrderTab.CANCELLED -> Res.string.online_orders_tab_cancelled
        OnlineOrderTab.ALL -> Res.string.online_orders_tab_all
    },
)

/** Status accent used for the tab dot and the line under each order card; null → neutral. */
@Composable
private fun onlineOrderStatusColor(status: OrderStatus?): Color = when (status) {
    null -> MaterialTheme.colorScheme.outline
    OrderStatus.RECEIVED, OrderStatus.DRAFT -> Color(0xFFF57C00)
    OrderStatus.PAYMENT_PENDING -> Color(0xFFFBC02D)
    OrderStatus.ACCEPTED -> Color(0xFF1E88E5)
    OrderStatus.PACKING -> Color(0xFF8E24AA)
    OrderStatus.LOOKING_FOR_PARTNER, OrderStatus.PARTNER_ACCEPTED, OrderStatus.ON_THE_WAY -> Color(0xFF00897B)
    OrderStatus.DELIVERED, OrderStatus.COMPLETED -> Color(0xFF43A047)
    OrderStatus.REJECTED, OrderStatus.CANCELLED -> MaterialTheme.colorScheme.error
    OrderStatus.RETURNING_TO_SHOP, OrderStatus.RETURNED_TO_SHOP -> Color(0xFFD84315)
}

@Composable
private fun onlineOrderStatusLabel(status: OrderStatus): String = stringResource(
    when (status) {
        OrderStatus.RECEIVED, OrderStatus.DRAFT -> Res.string.status_received
        OrderStatus.PAYMENT_PENDING -> Res.string.status_payment_pending
        OrderStatus.ACCEPTED -> Res.string.status_accepted
        OrderStatus.PACKING -> Res.string.status_packing
        OrderStatus.LOOKING_FOR_PARTNER -> Res.string.status_looking_partner
        OrderStatus.PARTNER_ACCEPTED -> Res.string.status_partner_accepted
        OrderStatus.ON_THE_WAY -> Res.string.status_on_the_way
        OrderStatus.DELIVERED, OrderStatus.COMPLETED -> Res.string.status_delivered
        OrderStatus.REJECTED -> Res.string.status_rejected
        OrderStatus.CANCELLED -> Res.string.status_cancelled
        OrderStatus.RETURNING_TO_SHOP -> Res.string.status_returning_to_shop
        OrderStatus.RETURNED_TO_SHOP -> Res.string.status_returned_to_shop
    },
)
