package org.bhargav.pansariwala.feature.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.PansariTopBar
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.settings.CancelOrderReason
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_accept_order
import pansariwala.shared.generated.resources.action_back
import pansariwala.shared.generated.resources.action_cancel_order
import pansariwala.shared.generated.resources.action_no
import pansariwala.shared.generated.resources.action_packing
import pansariwala.shared.generated.resources.action_request_delivery
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

@Composable
fun OnlineOrdersScreen(
    onBack: () -> Unit,
    viewModel: OnlineOrdersViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PansariTopBar(
            title = stringResource(Res.string.online_orders_title),
            onBack = onBack,
        )
        state.error?.let { message ->
            SectionCard(title = message.asString()) {
                Button(onClick = { viewModel.refresh() }) {
                    Text(stringResource(Res.string.action_retry))
                }
            }
        }
        if (state.loading && state.orders.isEmpty() && state.error == null) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        } else if (state.orders.isEmpty() && state.error == null) {
            Text(stringResource(Res.string.online_orders_empty))
        }
        state.orders.forEach { order ->
            val busy = state.busyId == order.id
            SectionCard(title = "${order.id} · ${order.customerName.orEmpty().ifBlank { order.id }} · ${onlineOrderStatusLabel(order.status)}") {
                order.items.forEach { item ->
                    Text("${item.productName} × ${item.quantity.asQuantity()} · ${item.lineTotal.asMoney()}")
                }
                OnlineOrderStatusNote(order)
                order.cancelReason?.let { reason ->
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
                    onAccept = { viewModel.accept(order.id) },
                    onPacking = { viewModel.markPacking(order.id) },
                    onRequestPartner = { viewModel.requestDeliveryPartner(order.id) },
                    onCancel = { viewModel.openCancel(order.id) },
                )
            }
        }
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
}

@Composable
private fun OnlineOrderStatusNote(order: Order) {
    val note = when (order.status) {
        OrderStatus.LOOKING_FOR_PARTNER -> stringResource(Res.string.online_order_looking_partner)
        OrderStatus.PARTNER_ACCEPTED -> stringResource(Res.string.online_order_waiting_pickup)
        else -> null
    }
    note?.let { Text(it) }
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
    onAccept: () -> Unit,
    onPacking: () -> Unit,
    onRequestPartner: () -> Unit,
    onCancel: () -> Unit,
) {
    val nextLabel = when (order.status) {
        OrderStatus.RECEIVED -> stringResource(Res.string.action_accept_order)
        OrderStatus.ACCEPTED -> stringResource(Res.string.action_packing)
        OrderStatus.PACKING -> stringResource(Res.string.action_request_delivery)
        else -> null
    }
    val nextAction = when (order.status) {
        OrderStatus.RECEIVED -> onAccept
        OrderStatus.ACCEPTED -> onPacking
        OrderStatus.PACKING -> onRequestPartner
        else -> null
    }
    if (nextAction == null && !order.canCancel) return
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        if (nextLabel != null && nextAction != null) {
            Button(onClick = nextAction, enabled = !busy) {
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
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
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

@Composable
private fun onlineOrderStatusLabel(status: OrderStatus): String = stringResource(
    when (status) {
        OrderStatus.RECEIVED, OrderStatus.DRAFT -> Res.string.status_received
        OrderStatus.ACCEPTED -> Res.string.status_accepted
        OrderStatus.PACKING -> Res.string.status_packing
        OrderStatus.LOOKING_FOR_PARTNER -> Res.string.status_looking_partner
        OrderStatus.PARTNER_ACCEPTED -> Res.string.status_partner_accepted
        OrderStatus.ON_THE_WAY -> Res.string.status_on_the_way
        OrderStatus.DELIVERED, OrderStatus.COMPLETED -> Res.string.status_delivered
        OrderStatus.REJECTED -> Res.string.status_rejected
        OrderStatus.CANCELLED -> Res.string.status_cancelled
    },
)
