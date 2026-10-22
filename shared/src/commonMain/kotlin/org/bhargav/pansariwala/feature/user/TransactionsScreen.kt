package org.bhargav.pansariwala.feature.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.AppBottomSheet
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.domain.model.MoneyTxn
import org.bhargav.pansariwala.domain.model.publicOrderNumber
import org.bhargav.pansariwala.printing.formatPrintDate
import org.bhargav.pansariwala.printing.formatPrintDateTime
import org.bhargav.pansariwala.ui.AsyncUiState
import org.bhargav.pansariwala.ui.errorBannerOrNull
import org.bhargav.pansariwala.ui.isBlockingLoad
import org.bhargav.pansariwala.ui.isRefreshing
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.account_transactions
import pansariwala.shared.generated.resources.order_number_label
import pansariwala.shared.generated.resources.pay_label_order
import pansariwala.shared.generated.resources.pay_label_refund
import pansariwala.shared.generated.resources.pay_label_shop
import pansariwala.shared.generated.resources.pay_label_txn
import pansariwala.shared.generated.resources.pay_method_direct
import pansariwala.shared.generated.resources.pay_method_gateway
import pansariwala.shared.generated.resources.pay_view_order
import pansariwala.shared.generated.resources.payment_method_cod
import pansariwala.shared.generated.resources.txn_detail_title
import pansariwala.shared.generated.resources.txn_empty_body
import pansariwala.shared.generated.resources.txn_empty_title
import pansariwala.shared.generated.resources.txn_fulfilment_delivery
import pansariwala.shared.generated.resources.txn_fulfilment_pickup
import pansariwala.shared.generated.resources.txn_label_date
import pansariwala.shared.generated.resources.txn_label_fulfilment
import pansariwala.shared.generated.resources.txn_label_items
import pansariwala.shared.generated.resources.txn_label_method
import pansariwala.shared.generated.resources.txn_label_ref
import pansariwala.shared.generated.resources.txn_label_status
import pansariwala.shared.generated.resources.txn_items_count
import pansariwala.shared.generated.resources.txn_summary_count
import pansariwala.shared.generated.resources.txn_summary_paid
import pansariwala.shared.generated.resources.txn_summary_refunded
import pansariwala.shared.generated.resources.txn_type_payment
import pansariwala.shared.generated.resources.txn_type_refund

@Composable
fun TransactionsScreen(
    onBack: () -> Unit,
    onOpenOrder: (String) -> Unit = {},
    viewModel: AccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val txns = (state as? AsyncUiState.Success)?.data?.txns.orEmpty()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val grouped = remember(txns) { txns.groupBy { formatPrintDate(it.createdAtEpochMs) } }
    PansariScreen(
        title = stringResource(Res.string.account_transactions),
        onBack = onBack,
        error = state.errorBannerOrNull(),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = viewModel::refresh, onDismiss = viewModel::dismissError)
        },
        isLoading = state.isBlockingLoad(),
        isRefreshing = state.isRefreshing(),
    ) {
        if (txns.isEmpty() && state is AsyncUiState.Success) {
            TxnEmptyState()
            return@PansariScreen
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item(key = "summary") { TxnSummaryCard(txns) }
            grouped.forEach { (date, rows) ->
                item(key = "date_$date") {
                    Text(
                        date,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp, start = 4.dp),
                    )
                }
                items(rows, key = { it.id }) { txn -> TxnRow(txn, onClick = { selectedId = txn.id }) }
            }
        }
    }
    txns.firstOrNull { it.id == selectedId }?.let { txn ->
        TxnDetailSheet(
            txn = txn,
            onOpenOrder = { selectedId = null; onOpenOrder(txn.orderId) },
            onDismiss = { selectedId = null },
        )
    }
}

@Composable
private fun TxnSummaryCard(txns: List<MoneyTxn>) {
    val paid = txns.filterNot { it.isRefund }.sumOf { it.amount }
    val refunded = txns.filter { it.isRefund }.sumOf { it.amount }
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(20.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(Res.string.txn_summary_count, txns.size), style = MaterialTheme.typography.labelLarge)
            Row(Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.txn_summary_paid), style = MaterialTheme.typography.labelMedium)
                    Text(paid.asMoney(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(stringResource(Res.string.txn_summary_refunded), style = MaterialTheme.typography.labelMedium)
                    Text(
                        refunded.asMoney(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (refunded > 0) TXN_REFUND_COLOR else Color.Unspecified,
                    )
                }
            }
        }
    }
}

@Composable
private fun TxnRow(txn: MoneyTxn, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            TxnIcon(txn.isRefund)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    txn.shopName?.takeIf { it.isNotBlank() } ?: txn.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOfNotNull(
                        txnOrderNumber(txn),
                        txn.itemCount.takeIf { it > 0 }?.let { stringResource(Res.string.txn_items_count, it) },
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    formatPrintDateTime(txn.createdAtEpochMs).substringAfter(' '),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TxnAmount(txn, MaterialTheme.typography.titleMedium)
                txn.orderStatus?.let { status ->
                    StatusPill(
                        userOrderStatusLabel(status),
                        container = MaterialTheme.colorScheme.secondaryContainer,
                        content = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TxnDetailSheet(txn: MoneyTxn, onOpenOrder: () -> Unit, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    AppBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                TxnIcon(txn.isRefund, size = 56)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(Res.string.txn_detail_title), style = MaterialTheme.typography.labelLarge)
                TxnAmount(txn, MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(4.dp))
                StatusPill(
                    stringResource(if (txn.isRefund) Res.string.txn_type_refund else Res.string.txn_type_payment),
                    container = if (txn.isRefund) TXN_REFUND_COLOR.copy(alpha = 0.15f) else MaterialTheme.colorScheme.primaryContainer,
                    content = if (txn.isRefund) TXN_REFUND_COLOR else MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    txn.shopName?.takeIf { it.isNotBlank() }?.let { LabelValueRow(stringResource(Res.string.pay_label_shop), it) }
                    LabelValueRow(stringResource(Res.string.txn_label_date), formatPrintDateTime(txn.createdAtEpochMs))
                    txn.orderStatus?.let { LabelValueRow(stringResource(Res.string.txn_label_status), userOrderStatusLabel(it)) }
                    HorizontalDivider()
                    LabelValueRow(stringResource(Res.string.pay_label_order), txnOrderNumber(txn) ?: txn.orderId)
                    LabelValueRow(stringResource(Res.string.pay_label_txn), txn.id)
                    txn.paymentMethod?.let { LabelValueRow(stringResource(Res.string.txn_label_method), paymentMethodLabel(it)) }
                    txn.paymentRef?.takeIf { it.isNotBlank() }?.let { LabelValueRow(stringResource(Res.string.txn_label_ref), it) }
                    txn.refundId?.takeIf { txn.isRefund && it.isNotBlank() }?.let {
                        LabelValueRow(stringResource(Res.string.pay_label_refund), it)
                    }
                    if (txn.itemCount > 0) {
                        HorizontalDivider()
                        LabelValueRow(stringResource(Res.string.txn_label_items), txn.itemCount.toString())
                        LabelValueRow(
                            stringResource(Res.string.txn_label_fulfilment),
                            stringResource(if (txn.selfPickup) Res.string.txn_fulfilment_pickup else Res.string.txn_fulfilment_delivery),
                        )
                    }
                }
            }
            UserPrimaryButton(stringResource(Res.string.pay_view_order), onClick = onOpenOrder)
        }
    }
}

@Composable
private fun TxnEmptyState() {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(12.dp))
        Text(stringResource(Res.string.txn_empty_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text(
            stringResource(Res.string.txn_empty_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TxnIcon(refund: Boolean, size: Int = 44) {
    val tint = if (refund) TXN_REFUND_COLOR else MaterialTheme.colorScheme.primary
    Surface(color = tint.copy(alpha = 0.12f), shape = CircleShape, modifier = Modifier.size(size.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                if (refund) Icons.AutoMirrored.Filled.Undo else Icons.Filled.ShoppingBag,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size((size / 2).dp),
            )
        }
    }
}

@Composable
private fun TxnAmount(txn: MoneyTxn, style: androidx.compose.ui.text.TextStyle) {
    Text(
        (if (txn.isRefund) "+ " else "− ") + txn.amount.asMoney(),
        style = style,
        fontWeight = FontWeight.Bold,
        color = if (txn.isRefund) TXN_REFUND_COLOR else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun txnOrderNumber(txn: MoneyTxn): String? =
    txn.shopId?.let { stringResource(Res.string.order_number_label, publicOrderNumber(it, txn.orderId)) }

@Composable
private fun paymentMethodLabel(method: String): String = when (method) {
    AppConstants.Payment.METHOD_UPI_DIRECT -> stringResource(Res.string.pay_method_direct)
    AppConstants.Payment.METHOD_CASHFREE, ONLINE_METHOD -> stringResource(Res.string.pay_method_gateway)
    COD_METHOD -> stringResource(Res.string.payment_method_cod)
    else -> method
}

private const val ONLINE_METHOD = "ONLINE"
private const val COD_METHOD = "COD"
private val TXN_REFUND_COLOR = Color(0xFF2E7D32)
