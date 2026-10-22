package org.bhargav.pansariwala.feature.udhaar

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import pansariwala.shared.generated.resources.udhaar_tab_orders
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.bhargav.pansariwala.api.CreditTxnDto
import org.bhargav.pansariwala.designsystem.PansariElevation
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.StatTile
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.feature.delivery.PartnerProfileCircle
import org.bhargav.pansariwala.feature.order.paymentMethodLabelRes
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.media.ImageUploadFeature
import org.bhargav.pansariwala.media.rememberImageSourceLauncher
import org.bhargav.pansariwala.printing.formatPrintDateTime
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.udhaar_txn_ref
import pansariwala.shared.generated.resources.udhaar_txn_gateway_upi
import pansariwala.shared.generated.resources.udhaar_txn_gateway_cashfree
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.action_save
import pansariwala.shared.generated.resources.udhaar_address
import pansariwala.shared.generated.resources.udhaar_detail_title
import pansariwala.shared.generated.resources.udhaar_edit_info
import pansariwala.shared.generated.resources.udhaar_filter_all
import pansariwala.shared.generated.resources.udhaar_filter_payments
import pansariwala.shared.generated.resources.udhaar_filter_purchases
import pansariwala.shared.generated.resources.udhaar_name
import pansariwala.shared.generated.resources.udhaar_notes
import pansariwala.shared.generated.resources.udhaar_orders_empty
import pansariwala.shared.generated.resources.udhaar_orders_title
import pansariwala.shared.generated.resources.udhaar_outstanding
import pansariwala.shared.generated.resources.udhaar_repay_amount
import pansariwala.shared.generated.resources.udhaar_repay_confirm
import pansariwala.shared.generated.resources.udhaar_repay_mode
import pansariwala.shared.generated.resources.udhaar_repay_note
import pansariwala.shared.generated.resources.udhaar_repay_received_by
import pansariwala.shared.generated.resources.udhaar_repay_title
import pansariwala.shared.generated.resources.udhaar_repayment
import pansariwala.shared.generated.resources.udhaar_total_credit
import pansariwala.shared.generated.resources.udhaar_total_repaid
import pansariwala.shared.generated.resources.udhaar_txn_mode
import pansariwala.shared.generated.resources.udhaar_txn_payment
import pansariwala.shared.generated.resources.udhaar_txn_processed_by
import pansariwala.shared.generated.resources.udhaar_txn_purchase
import pansariwala.shared.generated.resources.udhaar_txn_received_by
import pansariwala.shared.generated.resources.udhaar_txn_status_failed
import pansariwala.shared.generated.resources.udhaar_txn_status_pending
import pansariwala.shared.generated.resources.udhaar_txn_via_app
import pansariwala.shared.generated.resources.udhaar_txns_empty
import pansariwala.shared.generated.resources.udhaar_txns_title

private const val LOAD_MORE_THRESHOLD = 3
private const val MESSAGE_VISIBLE_MS = 2_500L
private val repaymentModes = listOf(
    AppConstants.PosPayment.CASH,
    AppConstants.PosPayment.UPI,
    AppConstants.PosPayment.CARD,
)

@Composable
fun BorrowerDetailScreen(
    phone: String,
    onBack: () -> Unit,
    viewModel: BorrowerDetailViewModel = koinViewModel(key = "borrower_$phone") { parametersOf(phone) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pickPhoto = rememberImageSourceLauncher(ImageUploadFeature.BORROWER_PHOTO, viewModel::changePhoto)
    val borrower = state.detail?.borrower
    LaunchedEffect(state.message) {
        if (state.message != null) {
            delay(MESSAGE_VISIBLE_MS)
            viewModel.consumeMessage()
        }
    }
    PansariScreen(
        title = borrower?.name?.ifBlank { null } ?: stringResource(Res.string.udhaar_detail_title),
        onBack = onBack,
        error = state.error.toErrorBanner(),
        onErrorAction = { handleErrorBannerAction(it, onRetry = viewModel::refresh, onDismiss = viewModel::dismissError) },
        isLoading = state.loading && state.detail == null,
    ) {
        val detail = state.detail ?: return@PansariScreen
        val b = detail.borrower
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "header") {
                Card(
                    Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.raisedCard),
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PartnerProfileCircle(
                            imageUrl = b.photoUrl.orEmpty(),
                            onClick = pickPhoto,
                            showEditBadge = true,
                            uploading = state.photoUploading,
                            localPreviewBytes = state.photoPreview,
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                b.name.ifBlank { b.phone },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, null, tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.padding(end = 6.dp))
                                Text(b.phone, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.9f))
                            }
                            if (b.address.isNotBlank()) {
                                Text(b.address, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
                            }
                            if (b.notes.isNotBlank()) {
                                Text(b.notes, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.8f))
                            }
                            OutlinedButton(
                                onClick = viewModel::openEdit,
                                modifier = Modifier.padding(top = 6.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            ) {
                                Icon(Icons.Default.Edit, null, Modifier.padding(end = 6.dp))
                                Text(stringResource(Res.string.udhaar_edit_info))
                            }
                        }
                    }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StatTile(
                        label = stringResource(Res.string.udhaar_outstanding),
                        value = b.outstanding.asMoney(),
                        modifier = Modifier.weight(1f),
                        container = MaterialTheme.colorScheme.errorContainer,
                        onContainer = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    StatTile(
                        label = stringResource(Res.string.udhaar_total_credit),
                        value = b.totalCredit.asMoney(),
                        modifier = Modifier.weight(1f),
                    )
                    StatTile(
                        label = stringResource(Res.string.udhaar_total_repaid),
                        value = b.totalRepaid.asMoney(),
                        modifier = Modifier.weight(1f),
                        container = MaterialTheme.colorScheme.tertiaryContainer,
                        onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
                Button(
                    onClick = viewModel::openRepay,
                    enabled = b.outstanding > 0.0,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) {
                    Icon(Icons.Default.Payments, null, Modifier.padding(end = 8.dp))
                    Text(stringResource(Res.string.udhaar_repayment), style = MaterialTheme.typography.titleMedium)
                }
            }
            item(key = "tabs") {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryTabRow(selectedTabIndex = state.tab.ordinal) {
                        Tab(
                            selected = state.tab == BorrowerDetailTab.ORDERS,
                            onClick = { viewModel.selectTab(BorrowerDetailTab.ORDERS) },
                            text = { Text(stringResource(Res.string.udhaar_tab_orders, state.orders.totalCount)) },
                        )
                        Tab(
                            selected = state.tab == BorrowerDetailTab.TRANSACTIONS,
                            onClick = { viewModel.selectTab(BorrowerDetailTab.TRANSACTIONS) },
                            text = { Text(stringResource(Res.string.udhaar_txns_title)) },
                        )
                    }
                    if (state.tab == BorrowerDetailTab.TRANSACTIONS) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                UdhaarTxnFilter.ALL to Res.string.udhaar_filter_all,
                                UdhaarTxnFilter.PURCHASES to Res.string.udhaar_filter_purchases,
                                UdhaarTxnFilter.PAYMENTS to Res.string.udhaar_filter_payments,
                            ).forEach { (f, label) ->
                                FilterChip(
                                    selected = state.filter == f,
                                    onClick = { viewModel.setFilter(f) },
                                    label = { Text(stringResource(label)) },
                                )
                            }
                        }
                    }
                }
            }
            val tab = state.tab
            val page = if (tab == BorrowerDetailTab.ORDERS) state.orders else state.txns
            if (page.items.isEmpty() && !page.loading) {
                item(key = "empty_$tab") {
                    EmptyLine(
                        stringResource(
                            if (tab == BorrowerDetailTab.ORDERS) Res.string.udhaar_orders_empty else Res.string.udhaar_txns_empty,
                        ),
                    )
                }
            }
            itemsIndexed(page.items, key = { _, txn -> "${tab}_${txn.id}" }) { index, txn ->
                if (index >= page.items.lastIndex - LOAD_MORE_THRESHOLD) {
                    LaunchedEffect(tab, page.page) { viewModel.loadMore(tab) }
                }
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                    Box(Modifier.padding(14.dp)) {
                        if (tab == BorrowerDetailTab.ORDERS) CreditOrderBlock(txn) else CreditTxnRow(txn, showItems = false)
                    }
                }
            }
            if (page.loading) {
                item(key = "loading_$tab") {
                    Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    }
                }
            }
            item(key = "bottom_space") { Spacer(Modifier.height(16.dp)) }
        }
        state.message?.let { msg ->
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.inverseSurface,
            ) {
                Text(
                    msg.asString(),
                    color = MaterialTheme.colorScheme.inverseOnSurface,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }
    }

    state.repay?.let { form ->
        RepayDialog(
            form = form,
            due = borrower?.outstanding ?: 0.0,
            staff = state.staff,
            onChange = viewModel::updateRepay,
            onConfirm = viewModel::submitRepay,
            onDismiss = viewModel::closeRepay,
        )
    }
    state.edit?.let { form ->
        AlertDialog(
            onDismissRequest = viewModel::closeEdit,
            title = { Text(stringResource(Res.string.udhaar_edit_info)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = form.name,
                        onValueChange = { v -> viewModel.updateEdit { it.copy(name = v) } },
                        label = { Text(stringResource(Res.string.udhaar_name)) },
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = form.address,
                        onValueChange = { v -> viewModel.updateEdit { it.copy(address = v) } },
                        label = { Text(stringResource(Res.string.udhaar_address)) },
                    )
                    OutlinedTextField(
                        value = form.notes,
                        onValueChange = { v -> viewModel.updateEdit { it.copy(notes = v) } },
                        label = { Text(stringResource(Res.string.udhaar_notes)) },
                        minLines = 2,
                    )
                }
            },
            confirmButton = {
                Button(onClick = viewModel::saveEdit, enabled = !form.busy) { Text(stringResource(Res.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = viewModel::closeEdit) { Text(stringResource(Res.string.action_cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RepayDialog(
    form: RepayForm,
    due: Double,
    staff: List<String>,
    onChange: ((RepayForm) -> RepayForm) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!form.busy) onDismiss() },
        title = { Text(stringResource(Res.string.udhaar_repay_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = form.amount,
                    onValueChange = { v -> onChange { it.copy(amount = v.filter { c -> c.isDigit() || c == '.' }) } },
                    label = { Text(stringResource(Res.string.udhaar_repay_amount, due.asMoney())) },
                    prefix = { Text("₹ ") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(Res.string.udhaar_repay_mode), style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    repaymentModes.forEach { mode ->
                        FilterChip(
                            selected = form.mode == mode,
                            onClick = { onChange { it.copy(mode = mode) } },
                            label = { Text(stringResource(paymentMethodLabelRes(mode))) },
                        )
                    }
                }
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = form.receivedBy,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(Res.string.udhaar_repay_received_by)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        staff.forEach { name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    expanded = false
                                    onChange { it.copy(receivedBy = name) }
                                },
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = form.note,
                    onValueChange = { v -> onChange { it.copy(note = v) } },
                    label = { Text(stringResource(Res.string.udhaar_repay_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                form.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm, enabled = !form.busy && form.amount.isNotBlank()) {
                if (form.busy) {
                    CircularProgressIndicator(Modifier.height(18.dp).width(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.udhaar_repay_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !form.busy) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

@Composable
private fun EmptyLine(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun CreditOrderBlock(txn: CreditTxnDto) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(formatPrintDateTime(txn.createdAt), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                txn.processedBy.takeIf(String::isNotBlank)?.let {
                    Text(
                        stringResource(Res.string.udhaar_txn_processed_by, it),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(txn.amount.asMoney(), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        txn.items.forEach { item ->
            Row {
                Text(
                    buildString {
                        append(item.name)
                        item.variantLabel?.takeIf(String::isNotBlank)?.let { append(" ($it)") }
                        append(" × ${item.quantity.asQuantity()} ${item.unit}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                )
                Text(item.lineTotal.asMoney(), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
fun CreditTxnsCard(
    transactions: List<CreditTxnDto>,
    filter: UdhaarTxnFilter,
    onFilter: (UdhaarTxnFilter) -> Unit,
    showItems: Boolean = false,
) {
    val visible = transactions.filterBy(filter)
    SectionCard(title = stringResource(Res.string.udhaar_txns_title), modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    UdhaarTxnFilter.ALL to Res.string.udhaar_filter_all,
                    UdhaarTxnFilter.PURCHASES to Res.string.udhaar_filter_purchases,
                    UdhaarTxnFilter.PAYMENTS to Res.string.udhaar_filter_payments,
                ).forEach { (f, label) ->
                    FilterChip(selected = filter == f, onClick = { onFilter(f) }, label = { Text(stringResource(label)) })
                }
            }
            if (visible.isEmpty()) {
                EmptyLine(stringResource(Res.string.udhaar_txns_empty))
            } else {
                visible.forEachIndexed { i, txn ->
                    CreditTxnRow(txn, showItems)
                    if (i < visible.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun CreditTxnRow(txn: CreditTxnDto, showItems: Boolean) {
    val isCredit = txn.type == AppConstants.Udhaar.TYPE_CREDIT
    val failed = txn.status == AppConstants.Udhaar.STATUS_FAILED
    val pending = txn.status == AppConstants.Udhaar.STATUS_PENDING
    val accent = when {
        failed -> MaterialTheme.colorScheme.outline
        isCredit -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(4.dp).height(36.dp).background(accent, RoundedCornerShape(2.dp)))
            Column(Modifier.weight(1f).padding(start = 10.dp)) {
                Text(
                    stringResource(if (isCredit) Res.string.udhaar_txn_purchase else Res.string.udhaar_txn_payment),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    formatPrintDateTime(txn.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    (if (isCredit) "+ " else "− ") + txn.amount.asMoney(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                if (pending || failed) {
                    Text(
                        stringResource(if (pending) Res.string.udhaar_txn_status_pending else Res.string.udhaar_txn_status_failed),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
        val meta = buildList {
            if (!isCredit && txn.paymentMode.isNotBlank()) {
                add(stringResource(Res.string.udhaar_txn_mode, stringResource(paymentMethodLabelRes(txn.paymentMode))))
            }
            if (txn.receivedBy.isNotBlank()) add(stringResource(Res.string.udhaar_txn_received_by, txn.receivedBy))
            if (isCredit && txn.processedBy.isNotBlank()) add(stringResource(Res.string.udhaar_txn_processed_by, txn.processedBy))
            if (txn.source == AppConstants.Udhaar.SOURCE_USER_APP) add(stringResource(Res.string.udhaar_txn_via_app))
            when (txn.gateway) {
                AppConstants.Udhaar.GATEWAY_CASHFREE -> add(stringResource(Res.string.udhaar_txn_gateway_cashfree))
                AppConstants.Udhaar.GATEWAY_UPI_DIRECT -> add(stringResource(Res.string.udhaar_txn_gateway_upi))
            }
            txn.paymentRef?.takeIf { it.isNotBlank() }?.let { add(stringResource(Res.string.udhaar_txn_ref, it)) }
            if (failed && txn.failureReason.isNotBlank()) add(txn.failureReason)
            if (txn.note.isNotBlank()) add(txn.note)
        }
        if (meta.isNotEmpty()) {
            Text(
                meta.joinToString(" · "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 14.dp),
            )
        }
        if (showItems && isCredit && txn.items.isNotEmpty()) {
            Text(
                txn.items.joinToString { "${it.name} × ${it.quantity.asQuantity()} ${it.unit}" },
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 14.dp),
            )
        }
    }
}
