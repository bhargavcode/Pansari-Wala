package org.bhargav.pansariwala.feature.order

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import org.bhargav.pansariwala.media.NetworkImage
import org.bhargav.pansariwala.feature.user.CartStore
import org.bhargav.pansariwala.feature.user.ProductPriceSummary
import org.bhargav.pansariwala.feature.user.VariantPickerHost
import org.bhargav.pansariwala.feature.user.needsPicker
import org.bhargav.pansariwala.feature.user.priceLabel
import org.bhargav.pansariwala.feature.user.quantityLabel
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.alexzhirkevich.qrose.rememberQrCodePainter
import pansariwala.shared.generated.resources.pos_upi_payee
import pansariwala.shared.generated.resources.pos_upi_received
import pansariwala.shared.generated.resources.pos_upi_scan_title
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import pansariwala.shared.generated.resources.update_and_print_order
import pansariwala.shared.generated.resources.save_and_print_order
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.AdaptivePane
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.PansariSearchTopBar
import org.bhargav.pansariwala.designsystem.PansariTopBar
import org.bhargav.pansariwala.designsystem.WindowWidthClass
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.i18n.localizedLabel
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.bhargav.pansariwala.voice.RequestMicrophonePermission
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_add
import pansariwala.shared.generated.resources.action_back
import pansariwala.shared.generated.resources.action_remove
import pansariwala.shared.generated.resources.bill_discount_flat
import pansariwala.shared.generated.resources.bill_discount_hint
import pansariwala.shared.generated.resources.bill_discount_label
import pansariwala.shared.generated.resources.bill_discount_percent
import pansariwala.shared.generated.resources.bill_manual_discount
import pansariwala.shared.generated.resources.bill_offer_discount
import pansariwala.shared.generated.resources.bill_subtotal
import pansariwala.shared.generated.resources.cart_count
import pansariwala.shared.generated.resources.cart_empty_hint
import pansariwala.shared.generated.resources.create_order
import pansariwala.shared.generated.resources.customer_name
import pansariwala.shared.generated.resources.edit_order
import pansariwala.shared.generated.resources.in_stock
import pansariwala.shared.generated.resources.pos_payment_title
import pansariwala.shared.generated.resources.save_order
import pansariwala.shared.generated.resources.search_products
import pansariwala.shared.generated.resources.total_label
import pansariwala.shared.generated.resources.update_order
import pansariwala.shared.generated.resources.voice_hint_example

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderEditorScreen(
    orderId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: OrderEditorViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(orderId) { viewModel.load(orderId) }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    if (state.showPaymentPicker) {
        PosPaymentSheet(state = state, viewModel = viewModel)
    }
    state.variantPicker?.let { request ->
        VariantPickerHost(
            request = request,
            lines = state.cart.map { CartStore.Line(it.product, it.variant, it.quantity) },
            onConfirm = viewModel::upsertOption,
            onDismiss = viewModel::dismissPicker,
        )
    }

    RequestMicrophonePermission(
        trigger = state.requestMicPermission,
        onConsumed = viewModel::consumeMicPermissionRequest,
        onResult = viewModel::onMicPermissionResult,
    )
    PansariScreen(
        error = state.error.toErrorBanner(retryable = false),
        onErrorAction = { handleErrorBannerAction(it, onRetry = {}, onDismiss = viewModel::dismissError) },
        isLoading = state.loading,
    ) {
        AdaptivePane(Modifier.fillMaxSize()) { widthClass ->
        if (widthClass == WindowWidthClass.Expanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                    OrderHeader(state.isEditing, onBack)
                    CustomerField(state.customerName, viewModel::onCustomerNameChange)
                    CartSection(state, viewModel)
                }
                Column(modifier = Modifier.weight(1f)) {
                    SearchOrSuggestedItems(state, viewModel, showHeader = false)
                }
            }
        } else {
            SearchOrSuggestedItems(state, viewModel, showHeader = true, onBack = onBack)
        }
        }
    }
}

@Composable
private fun OrderHeader(isEditing: Boolean, onBack: () -> Unit) {
    PansariTopBar(
        title = stringResource(if (isEditing) Res.string.edit_order else Res.string.create_order),
        onBack = onBack,
        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
    )
}

@Composable
private fun CustomerField(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(Res.string.customer_name)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun ColumnScope.CartSection(state: OrderEditorUiState, viewModel: OrderEditorViewModel) {
    Text(
        text = stringResource(Res.string.cart_count, state.itemCount),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
    )
    HorizontalDivider()

    if (state.cart.isEmpty()) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(Res.string.cart_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // Takes up remaining available vertical space
        ) {
            items(state.cart, key = { it.key }) { line ->
                CartLineRow(line, viewModel)
            }
        }
    }

    HorizontalDivider()
    if (state.cart.isNotEmpty()) BillDiscountSection(state, viewModel)
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(Res.string.total_label, state.total.asMoney()),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { viewModel.save(print = true) }) {
                Text(
                    stringResource(
                        if (state.isEditing) Res.string.update_and_print_order else Res.string.save_and_print_order,
                    ),
                )
            }
            Button(onClick = { viewModel.save() }) {
                Text(
                    stringResource(
                        if (state.isEditing) Res.string.update_order else Res.string.save_order,
                    ),
                )
            }
        }
    }
}

@Composable
private fun BillDiscountSection(state: OrderEditorUiState, viewModel: OrderEditorViewModel) {
    val bill = state.bill
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = state.manualDiscountInput,
                onValueChange = viewModel::onManualDiscountChange,
                label = { Text(stringResource(Res.string.bill_discount_label)) },
                placeholder = { Text(stringResource(Res.string.bill_discount_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = !state.manualDiscountPercent,
                onClick = { viewModel.onManualDiscountPercentChange(false) },
                label = { Text(stringResource(Res.string.bill_discount_flat)) },
            )
            FilterChip(
                selected = state.manualDiscountPercent,
                onClick = { viewModel.onManualDiscountPercentChange(true) },
                label = { Text(stringResource(Res.string.bill_discount_percent)) },
            )
        }
        if (bill.discount > 0.0) {
            val muted = MaterialTheme.colorScheme.onSurfaceVariant
            Text(stringResource(Res.string.bill_subtotal, bill.subtotal.asMoney()), style = MaterialTheme.typography.bodySmall, color = muted)
            if (bill.offerDiscount > 0.0) {
                Text(stringResource(Res.string.bill_offer_discount, bill.offerDiscount.asMoney()), style = MaterialTheme.typography.bodySmall, color = muted)
            }
            if (bill.manualDiscount > 0.0) {
                Text(stringResource(Res.string.bill_manual_discount, bill.manualDiscount.asMoney()), style = MaterialTheme.typography.bodySmall, color = muted)
            }
        }
    }
}

@Composable
private fun CartLineRow(line: CartLine, viewModel: OrderEditorViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NetworkImage(
            url = line.option.imageUrl,
            contentDescription = line.product.name,
            modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.small),
        )
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f).clickable(enabled = line.product.needsPicker) {
                viewModel.openPicker(line.product, line.key)
            },
        ) {
            Text(
                text = line.variant?.let { "${line.product.name} · ${it.label}" } ?: line.product.name,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${line.option.priceLabel()} · ${line.lineTotal.asMoney()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = {
            viewModel.changeQuantity(line.key, -1)
        }) { Text("−") }
        Text(
            text = line.option.quantityLabel(line.quantity),
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = {
            viewModel.changeQuantity(line.key, 1)
        }) { Text("+") }
        TextButton(onClick = { viewModel.removeLine(line.key) }) {
            Text(stringResource(Res.string.action_remove))
        }
    }
}

@Composable
private fun SearchOrSuggestedItems(
    state: OrderEditorUiState,
    viewModel: OrderEditorViewModel,
    showHeader: Boolean,
    onBack: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (showHeader) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OrderHeader(state.isEditing, onBack)
                CustomerField(state.customerName, viewModel::onCustomerNameChange)
                CartSection(state, viewModel)
            }
        }

        PansariSearchTopBar(
            title = stringResource(if (state.isEditing) Res.string.edit_order else Res.string.create_order),
            searchQuery = state.searchQuery,
            searchLabel = stringResource(Res.string.search_products),
            onSearchChange = viewModel::onSearchChange,
            onBack = if (showHeader) null else onBack,
            trailingSearchContent = if (state.voiceSearchEnabled) {
                {
                    ListeningMicButton(
                        isListening = state.isListening,
                        onClick = viewModel::onMicClick,
                        modifier = Modifier.size(40.dp),
                    )
                }
            } else {
                null
            },
        )

        if (!state.voiceSearchEnabled) {
            Spacer(Modifier.height(8.dp))
        } else if (state.isListening || state.partialTranscript.isNotBlank()) {
            VoiceSearchControls(
                isListening = state.isListening,
                partialTranscript = state.partialTranscript,
                onMicClick = viewModel::onMicClick,
                onCancel = viewModel::cancelListening,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp),
                showMicButton = false,
            )
        } else {
            Text(
                text = stringResource(Res.string.voice_hint_example),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            items(state.filteredCatalog, key = { it.id }) { product ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NetworkImage(
                        url = product.primaryImageUrl,
                        contentDescription = product.name,
                        modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small),
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(product.name, style = MaterialTheme.typography.bodyMedium)
                        if (product.hasVariants) {
                            ProductPriceSummary(product)
                        } else {
                            Text(
                                text = stringResource(
                                    Res.string.in_stock,
                                    product.sellingPrice.asMoney(),
                                    product.stockQty.asQuantity(),
                                    product.unit.localizedLabel(),
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    OutlinedButton(onClick = { viewModel.addProduct(product) }) {
                        Text(stringResource(Res.string.action_add))
                    }
                }
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosPaymentSheet(state: OrderEditorUiState, viewModel: OrderEditorViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = viewModel::dismissPaymentPicker, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.showUpiQr) {
                UpiQrContent(state, viewModel)
            } else {
                Text(
                    stringResource(Res.string.pos_payment_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    stringResource(Res.string.total_label, state.total.asMoney()),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    posPaymentOptions.forEach { (method, icon) ->
                        PaymentOptionCard(
                            label = stringResource(paymentMethodLabelRes(method)),
                            icon = icon,
                            onClick = { viewModel.selectPayment(method) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                state.error?.let { error ->
                    Text(
                        error.asString(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private val posPaymentOptions: List<Pair<String, ImageVector>> = listOf(
    AppConstants.PosPayment.CASH to Icons.Filled.CurrencyRupee,
    AppConstants.PosPayment.UPI to Icons.Filled.QrCode2,
    AppConstants.PosPayment.CARD to Icons.Filled.CreditCard,
)

@Composable
private fun PaymentOptionCard(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(onClick = onClick, modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp),
            )
            Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ColumnScope.UpiQrContent(state: OrderEditorUiState, viewModel: OrderEditorViewModel) {
    val amount = state.total.asMoney()
    val uri = remember(state.shopUpiId, state.shopName, state.total) {
        buildUpiUri(state.shopUpiId, state.shopName, state.total)
    }
    val qrPainter = rememberQrCodePainter(uri)
    Text(
        stringResource(Res.string.pos_upi_scan_title, amount),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
    )
    Box(
        modifier = Modifier
            .size(240.dp)
            .background(Color.White)
            .padding(12.dp),
    ) {
        Image(painter = qrPainter, contentDescription = null, modifier = Modifier.fillMaxSize())
    }
    Text(
        stringResource(Res.string.pos_upi_payee, state.shopName.ifBlank { state.shopUpiId }),
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(
        state.shopUpiId,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        OutlinedButton(onClick = viewModel::backFromUpiQr, modifier = Modifier.weight(1f)) {
            Text(stringResource(Res.string.action_back))
        }
        Button(onClick = viewModel::confirmUpiReceived, modifier = Modifier.weight(1f)) {
            Text(stringResource(Res.string.pos_upi_received))
        }
    }
}
