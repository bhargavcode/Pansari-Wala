package org.bhargav.pansariwala.feature.order

import pansariwala.shared.generated.resources.stock_left
import pansariwala.shared.generated.resources.pos_no_products_match
import pansariwala.shared.generated.resources.pos_out_of_stock
import pansariwala.shared.generated.resources.pos_category_all
import org.bhargav.pansariwala.i18n.localizedName
import org.bhargav.pansariwala.domain.model.ProductCategory
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.BorderStroke
import org.bhargav.pansariwala.designsystem.AppBottomSheet
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.ui.text.style.TextOverflow
import org.bhargav.pansariwala.api.BillingModel
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.feature.user.QuantityStepper
import pansariwala.shared.generated.resources.action_edit
import org.bhargav.pansariwala.feature.catalog.ProductDetailsSheet
import pansariwala.shared.generated.resources.category_price
import pansariwala.shared.generated.resources.field_stock_qty
import pansariwala.shared.generated.resources.pos_price_required_hint
import pansariwala.shared.generated.resources.pos_price_required_title
import pansariwala.shared.generated.resources.pos_price_save_add
import pansariwala.shared.generated.resources.pos_cart_bar_items
import pansariwala.shared.generated.resources.pos_choose_option
import pansariwala.shared.generated.resources.pos_view_cart
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import org.bhargav.pansariwala.media.NetworkImage
import org.bhargav.pansariwala.feature.user.CartStore
import org.bhargav.pansariwala.feature.user.LineOfferPrice
import org.bhargav.pansariwala.feature.user.OfferPrice
import org.bhargav.pansariwala.feature.user.OptionPriceSummary
import org.bhargav.pansariwala.feature.user.PackCountStepper
import org.bhargav.pansariwala.feature.user.PurchaseOption
import org.bhargav.pansariwala.feature.user.VariantPickerHost
import org.bhargav.pansariwala.feature.user.displayName
import org.bhargav.pansariwala.feature.user.needsPicker
import org.bhargav.pansariwala.feature.user.options
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
import org.bhargav.pansariwala.printing.formatPrintDateTime
import org.bhargav.pansariwala.util.asQuantity
import org.bhargav.pansariwala.voice.RequestMicrophonePermission
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import androidx.compose.material.icons.filled.Edit
import pansariwala.shared.generated.resources.pos_line_price_edit_cd
import pansariwala.shared.generated.resources.action_add
import pansariwala.shared.generated.resources.action_back
import pansariwala.shared.generated.resources.action_remove
import pansariwala.shared.generated.resources.bill_discount_flat
import pansariwala.shared.generated.resources.bill_discount_hint
import pansariwala.shared.generated.resources.bill_discount_label
import pansariwala.shared.generated.resources.bill_discount_percent
import pansariwala.shared.generated.resources.bill_manual_discount
import pansariwala.shared.generated.resources.bill_offer_discount
import pansariwala.shared.generated.resources.bill_product_discount
import pansariwala.shared.generated.resources.bill_subtotal
import pansariwala.shared.generated.resources.cart_count
import pansariwala.shared.generated.resources.cart_empty_hint
import pansariwala.shared.generated.resources.create_order
import pansariwala.shared.generated.resources.customer_name
import pansariwala.shared.generated.resources.edit_order
import pansariwala.shared.generated.resources.pos_payment_title
import pansariwala.shared.generated.resources.pos_udhaar_confirm
import pansariwala.shared.generated.resources.pos_udhaar_hint
import pansariwala.shared.generated.resources.pos_udhaar_name
import pansariwala.shared.generated.resources.pos_udhaar_phone
import pansariwala.shared.generated.resources.pos_udhaar_title
import pansariwala.shared.generated.resources.pos_udhaar_pick_hint
import pansariwala.shared.generated.resources.pos_udhaar_search
import pansariwala.shared.generated.resources.pos_udhaar_not_found
import pansariwala.shared.generated.resources.pos_udhaar_add_new
import pansariwala.shared.generated.resources.pos_udhaar_current_due
import pansariwala.shared.generated.resources.pos_udhaar_this_bill
import pansariwala.shared.generated.resources.pos_udhaar_new_due
import pansariwala.shared.generated.resources.pos_udhaar_confirm_question
import pansariwala.shared.generated.resources.pos_udhaar_photo
import pansariwala.shared.generated.resources.pos_udhaar_take_photo
import pansariwala.shared.generated.resources.pos_udhaar_retake_photo
import pansariwala.shared.generated.resources.pos_udhaar_address
import pansariwala.shared.generated.resources.udhaar_due
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PhotoCamera
import org.bhargav.pansariwala.api.BorrowerDto
import org.bhargav.pansariwala.feature.delivery.PartnerProfileCircle
import org.bhargav.pansariwala.feature.udhaar.BorrowerAvatar
import androidx.compose.material.icons.automirrored.filled.MenuBook
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
    onOpenSubscription: () -> Unit = {},
    reorderFromId: String? = null,
    viewModel: OrderEditorViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(orderId, reorderFromId) { viewModel.load(orderId, reorderFromId) }
    LaunchedEffect(Unit) { viewModel.refreshEntitlements() }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }
    if (state.showPaymentPicker) {
        PosPaymentSheet(state = state, viewModel = viewModel)
    }
    if (state.showScannerSettings) {
        ScannerSettingsSheet(
            initial = state.scannerSettings,
            cameraOn = !state.scannerPaused,
            onSave = viewModel::saveScannerSettings,
            onDismiss = viewModel::dismissScannerSettings,
        )
    }
    if (state.showScannerUpsell) {
        ScannerUpsellSheet(
            plans = state.scannerPlans,
            canPurchase = state.billing?.model != BillingModel.COMMISSION,
            onSubscribe = {
                viewModel.dismissScannerUpsell()
                onOpenSubscription()
            },
            onDismiss = viewModel::dismissScannerUpsell,
        )
    }
    if (state.showCartSheet) {
        PosCartSheet(state, viewModel)
    }
    state.variantPicker?.let { request ->
        VariantPickerHost(
            request = request,
            lines = state.cart.map { CartStore.Line(it.product, it.variant, it.quantity) },
            onConfirm = viewModel::upsertOption,
            onDismiss = viewModel::dismissPicker,
        )
    }
    state.infoOption?.let { option ->
        PosProductInfoSheet(
            option = option,
            inCartQty = state.cart.firstOrNull { it.key == option.key }?.quantity ?: 0.0,
            onAdd = {
                viewModel.dismissProductInfo()
                viewModel.addVariant(option)
            },
            onRecordUnbilled = { viewModel.openUnbilledSale(option) },
            onDismiss = viewModel::dismissProductInfo,
        )
    }
    state.unbilledSaleOption?.let { option ->
        UnbilledSaleDialog(option = option, onSave = viewModel::recordUnbilledSale, onDismiss = viewModel::dismissUnbilledSale)
    }
    state.priceEditKey?.let { key ->
        state.cart.firstOrNull { it.key == key }?.let { line ->
            PosLinePriceDialog(line = line, onSave = { viewModel.setLinePrice(key, it) }, onDismiss = viewModel::dismissLinePrice)
        }
    }
    state.addVariantFor?.let { product ->
        PosAddVariantDialog(
            product = product,
            onSave = { draft, currentLabel -> viewModel.saveVariant(product, draft, currentLabel) },
            onDismiss = viewModel::dismissAddVariant,
        )
    }

    state.priceRequest?.let { request ->
        val product = request.option.product
        val name = request.option.variant?.let { stringResource(Res.string.category_price, product.name, it.label) }
            ?: product.name
        ProductDetailsSheet(
            title = stringResource(Res.string.pos_price_required_title, name),
            subtitle = stringResource(Res.string.pos_price_required_hint),
            draft = state.priceDraft,
            categories = state.masterCategories,
            confirmLabel = stringResource(Res.string.pos_price_save_add),
            onDraftChange = viewModel::updatePriceDraft,
            onConfirm = viewModel::confirmPrice,
            onDismiss = viewModel::dismissPriceRequest,
            priceRequired = true,
            stockLabel = stringResource(Res.string.field_stock_qty),
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
        BoxWithConstraints(Modifier.fillMaxSize()) {
            if (maxHeight > maxWidth) {
                PortraitOrderLayout(state, viewModel, onBack, onOpenSubscription)
            } else {
                LandscapeOrderLayout(state, viewModel, onBack, onOpenSubscription)
            }
        }
    }
}

@Composable
private fun LandscapeOrderLayout(
    state: OrderEditorUiState,
    viewModel: OrderEditorViewModel,
    onBack: () -> Unit,
    onOpenSubscription: () -> Unit,
) {
    AdaptivePane(Modifier.fillMaxSize()) { widthClass ->
        if (widthClass == WindowWidthClass.Expanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Column(modifier = Modifier.weight(1f).padding(16.dp)) {
                    OrderHeader(state, viewModel, onBack)
                    SubscriptionRenewBanner(state.billing, onOpenSubscription, Modifier.padding(bottom = 8.dp))
                    CustomerField(state.customerName, viewModel::onCustomerNameChange)
                    CartSection(state, viewModel)
                }
                Column(modifier = Modifier.weight(1f)) {
                    SearchOrSuggestedItems(state, viewModel, showHeader = false)
                }
            }
        } else {
            SearchOrSuggestedItems(state, viewModel, showHeader = true, onBack = onBack, onOpenSubscription = onOpenSubscription)
        }
    }
}

/** Phone/tablet portrait: user-app style product list with a sticky cart bar; the cart opens as a sheet. */
@Composable
private fun PortraitOrderLayout(
    state: OrderEditorUiState,
    viewModel: OrderEditorViewModel,
    onBack: () -> Unit,
    onOpenSubscription: () -> Unit,
) {
    val linesByProduct = remember(state.cart) { state.cart.groupBy { it.product.id } }
    var category by rememberSaveable { mutableStateOf<ProductCategory?>(null) }
    val categories = remember(state.filteredCatalog) {
        state.filteredCatalog.map { it.category }.distinct().sortedBy { it.ordinal }
    }
    LaunchedEffect(categories) { if (category != null && category !in categories) category = null }
    val visible = remember(state.filteredCatalog, category) {
        category?.let { c -> state.filteredCatalog.filter { it.category == c } } ?: state.filteredCatalog
    }
    Column(Modifier.fillMaxSize()) {
        PansariSearchTopBar(
            title = stringResource(if (state.isEditing) Res.string.edit_order else Res.string.create_order),
            searchQuery = state.searchQuery,
            searchLabel = stringResource(Res.string.search_products),
            onSearchChange = viewModel::onSearchChange,
            onBack = onBack,
            trailingSearchContent = micButton(state, viewModel),
            actions = { ScannerToolbarActions(state, viewModel) },
        )
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            state.createdAtEpochMs?.let {
                Text(formatPrintDateTime(it), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            SubscriptionRenewBanner(state.billing, onOpenSubscription)
            ScanMessageStrip(state)
            VoiceArea(state, viewModel)
        }
        if (categories.size > 1) {
            PosCategoryChips(
                categories = categories,
                selected = category,
                total = state.filteredCatalog.size,
                onSelect = { category = it },
            )
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (visible.isEmpty() && !state.loading) {
                item(key = "empty") {
                    Text(
                        stringResource(Res.string.pos_no_products_match),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            items(visible, key = { it.id }, contentType = { "product" }) { product ->
                PosCatalogRow(product, linesByProduct[product.id].orEmpty(), viewModel)
            }
        }
        PosCartBar(state, onOpen = { viewModel.setCartSheetVisible(true) })
    }
}

@Composable
private fun PosCategoryChips(
    categories: List<ProductCategory>,
    selected: ProductCategory?,
    total: Int,
    onSelect: (ProductCategory?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
    ) {
        item(key = "all") {
            FilterChip(
                selected = selected == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(Res.string.pos_category_all, total)) },
            )
        }
        items(categories, key = { it.name }) { c ->
            FilterChip(
                selected = selected == c,
                onClick = { onSelect(if (selected == c) null else c) },
                label = { Text(c.localizedName()) },
            )
        }
    }
}

/** Every variant of [product] is its own card; long-press a card to add another variant. */
@Composable
private fun PosCatalogRow(product: Product, productLines: List<CartLine>, viewModel: OrderEditorViewModel) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        product.options().forEach { option ->
            PosOptionRow(option, productLines.firstOrNull { it.key == option.key }, viewModel)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PosOptionRow(option: PurchaseOption, line: CartLine?, viewModel: OrderEditorViewModel) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(14.dp)
    Surface(
        shape = shape,
        color = if (line != null) colors.primaryContainer.copy(alpha = 0.35f) else colors.surfaceContainerLow,
        border = if (line != null) BorderStroke(1.dp, colors.primary.copy(alpha = 0.4f)) else null,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(shape)
                .combinedClickable(
                    onClick = { if (line == null) viewModel.addVariant(option) },
                    onLongClick = { viewModel.startAddVariant(option.product) },
                )
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NetworkImage(
                url = option.imageUrl,
                contentDescription = option.displayName,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)).clickable { viewModel.openProductInfo(option) },
            )
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    option.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                OptionStockSummary(option)
            }
            when {
                option.needsPicker && line != null -> OutlinedButton(onClick = { viewModel.openPicker(option.product, line.key) }) {
                    Text(option.quantityLabel(line.quantity), maxLines = 1)
                }
                option.needsPicker -> OutlinedButton(onClick = { viewModel.addVariant(option) }, enabled = option.inStock) {
                    Text(stringResource(Res.string.pos_choose_option), maxLines = 1)
                }
                line != null -> PackCountStepper(
                    option = option,
                    quantity = line.quantity,
                    onSet = { viewModel.setQuantity(line.key, it) },
                    onIncrement = { viewModel.changeQuantity(line.key, 1) },
                    onDecrement = { viewModel.changeQuantity(line.key, -1) },
                )
                else -> FilledTonalButton(onClick = { viewModel.addVariant(option) }, enabled = option.inStock) {
                    Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(Res.string.action_add))
                }
            }
        }
    }
}

@Composable
private fun OptionStockSummary(option: PurchaseOption) {
    OptionPriceSummary(option)
    val colors = MaterialTheme.colorScheme
    val (label, tint) = when {
        !option.inStock -> stringResource(Res.string.pos_out_of_stock) to colors.error
        option.stock <= option.product.lowStockThreshold -> stringResource(
            Res.string.stock_left,
            option.stock.asQuantity(),
            option.quantityUnit.localizedLabel(),
        ) to colors.tertiary
        else -> stringResource(
            Res.string.stock_left,
            option.stock.asQuantity(),
            option.quantityUnit.localizedLabel(),
        ) to colors.onSurfaceVariant
    }
    Surface(shape = RoundedCornerShape(6.dp), color = tint.copy(alpha = 0.12f)) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun PosCartBar(state: OrderEditorUiState, onOpen: () -> Unit) {
    if (state.cart.isEmpty()) return
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(18.dp),
        shadowElevation = 8.dp,
        color = colors.primary,
        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(colors.onPrimary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Default.ShoppingCart, null, tint = colors.onPrimary) }
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    stringResource(Res.string.pos_cart_bar_items, state.itemCount),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onPrimary.copy(alpha = 0.85f),
                )
                Text(
                    state.total.asMoney(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimary,
                )
            }
            Text(
                stringResource(Res.string.pos_view_cart),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.onPrimary,
            )
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = colors.onPrimary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosCartSheet(state: OrderEditorUiState, viewModel: OrderEditorViewModel) {
    AppBottomSheet(
        onDismissRequest = { viewModel.setCartSheetVisible(false) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 16.dp)) {
            CustomerField(state.customerName, viewModel::onCustomerNameChange)
            Text(
                text = stringResource(Res.string.cart_count, state.itemCount),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp),
            )
            HorizontalDivider()
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = CART_SHEET_LIST_MAX)) {
                items(state.cart, key = { it.key }) { line -> CartLineRow(line, viewModel) }
            }
            HorizontalDivider()
            CartTotalsAndActions(state, viewModel, onBeforeSave = { viewModel.setCartSheetVisible(false) })
        }
    }
}

private val CART_SHEET_LIST_MAX = 360.dp

@Composable
private fun micButton(state: OrderEditorUiState, viewModel: OrderEditorViewModel): (@Composable () -> Unit)? =
    if (state.voiceSearchEnabled) {
        {
            ListeningMicButton(
                isListening = state.isListening,
                onClick = viewModel::onMicClick,
                modifier = Modifier.size(40.dp),
            )
        }
    } else {
        null
    }

@Composable
private fun VoiceArea(state: OrderEditorUiState, viewModel: OrderEditorViewModel) {
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
}

@Composable
private fun OrderHeader(state: OrderEditorUiState, viewModel: OrderEditorViewModel, onBack: () -> Unit) {
    val createdAtEpochMs = state.createdAtEpochMs
    Column(Modifier.fillMaxWidth()) {
        PansariTopBar(
            title = stringResource(if (state.isEditing) Res.string.edit_order else Res.string.create_order),
            onBack = onBack,
            modifier = Modifier.fillMaxWidth().padding(bottom = if (createdAtEpochMs != null) 4.dp else 12.dp),
            actions = { ScannerToolbarActions(state, viewModel) },
        )
        if (createdAtEpochMs != null) {
            Text(
                formatPrintDateTime(createdAtEpochMs),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, bottom = 12.dp),
            )
        }
    }
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
    CartTotalsAndActions(state, viewModel)
}

@Composable
private fun CartTotalsAndActions(
    state: OrderEditorUiState,
    viewModel: OrderEditorViewModel,
    onBeforeSave: () -> Unit = {},
) {
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
            OutlinedButton(onClick = { onBeforeSave(); viewModel.save(print = true) }) {
                Text(
                    stringResource(
                        if (state.isEditing) Res.string.update_and_print_order else Res.string.save_and_print_order,
                    ),
                )
            }
            Button(onClick = { onBeforeSave(); viewModel.save() }) {
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
        if (bill.discount > 0.0 || bill.productDiscount > 0.0) {
            val muted = MaterialTheme.colorScheme.onSurfaceVariant
            Text(stringResource(Res.string.bill_subtotal, bill.subtotal.asMoney()), style = MaterialTheme.typography.bodySmall, color = muted)
            if (bill.productDiscount > 0.0) {
                Text(stringResource(Res.string.bill_product_discount, bill.productDiscount.asMoney()), style = MaterialTheme.typography.bodySmall, color = PackedGreen)
            }
            if (bill.offerDiscount > 0.0) {
                Text(stringResource(Res.string.bill_offer_discount, bill.offerDiscount.asMoney()), style = MaterialTheme.typography.bodySmall, color = PackedGreen)
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
            modifier = Modifier.size(40.dp).clip(MaterialTheme.shapes.small).clickable { viewModel.openProductInfo(line.option) },
        )
        Spacer(Modifier.width(8.dp))
        Column(
            modifier = Modifier.weight(1f).clickable(enabled = line.option.needsPicker) {
                viewModel.openPicker(line.product, line.key)
            },
        ) {
            Text(
                text = line.option.displayName,
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.clickable { viewModel.openLinePrice(line.key) },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (line.lockedUnitPrice == null) {
                    line.option.OfferPrice(priceStyle = MaterialTheme.typography.labelSmall)
                } else {
                    Text(
                        text = line.unitPrice.asMoney(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (line.unitPrice != line.option.unitPrice) {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(Res.string.pos_line_price_edit_cd),
                    modifier = Modifier.padding(start = 4.dp).size(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            LineOfferPrice(line.lineTotal, line.baseLineTotal)
        }
        if (line.option.isLoose) {
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
        } else {
            PackCountStepper(
                option = line.option,
                quantity = line.quantity,
                onSet = { viewModel.setQuantity(line.key, it) },
                onIncrement = { viewModel.changeQuantity(line.key, 1) },
                onDecrement = { viewModel.changeQuantity(line.key, -1) },
            )
        }
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
    onOpenSubscription: () -> Unit = {},
) {
    Column(modifier = Modifier.fillMaxSize()) {
        if (showHeader) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                OrderHeader(state, viewModel, onBack)
                SubscriptionRenewBanner(state.billing, onOpenSubscription, Modifier.padding(bottom = 8.dp))
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
            trailingSearchContent = micButton(state, viewModel),
        )

        Column(Modifier.padding(horizontal = 16.dp)) {
            ScanMessageStrip(state)
            VoiceArea(state, viewModel)
        }

        val linesByProduct = remember(state.cart) { state.cart.groupBy { it.product.id } }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            items(state.filteredCatalog, key = { it.id }) { product ->
                PosCatalogRow(product, linesByProduct[product.id].orEmpty(), viewModel)
                HorizontalDivider()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosPaymentSheet(state: OrderEditorUiState, viewModel: OrderEditorViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    AppBottomSheet(onDismissRequest = viewModel::dismissPaymentPicker, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (state.showUpiQr) {
                UpiQrContent(state, viewModel)
            } else if (state.showUdhaarForm) {
                UdhaarContent(state, viewModel)
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
                PaymentOptionCard(
                    label = stringResource(paymentMethodLabelRes(AppConstants.PosPayment.UDHAAR)),
                    icon = Icons.AutoMirrored.Filled.MenuBook,
                    onClick = { viewModel.selectPayment(AppConstants.PosPayment.UDHAAR) },
                    modifier = Modifier.fillMaxWidth(),
                )
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

@Composable
private fun ColumnScope.UdhaarContent(state: OrderEditorUiState, viewModel: OrderEditorViewModel) {
    Text(
        stringResource(Res.string.pos_udhaar_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
    )
    val selected = state.udhaarSelected
    when {
        selected != null -> UdhaarConfirmStep(selected, state)
        state.udhaarAddingNew -> UdhaarNewBorrowerStep(state, viewModel)
        else -> UdhaarSearchStep(state, viewModel)
    }
    state.error?.let { error ->
        Text(error.asString(), color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth())
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = viewModel::backFromUdhaar, modifier = Modifier.weight(1f)) {
            Text(stringResource(Res.string.action_back))
        }
        if (selected != null || state.udhaarAddingNew) {
            Button(
                onClick = viewModel::confirmUdhaar,
                enabled = state.udhaarPhone.length == AppConstants.PHONE_LOCAL_DIGITS,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(Res.string.pos_udhaar_confirm, state.total.asMoney()))
            }
        }
    }
}

@Composable
private fun UdhaarSearchStep(state: OrderEditorUiState, viewModel: OrderEditorViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.pos_udhaar_pick_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val query = state.udhaarQuery.trim()
        when {
            state.udhaarResults.isNotEmpty() -> LazyColumn(
                modifier = Modifier.fillMaxWidth().heightIn(max = 280.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(state.udhaarResults, key = { it.phone }) { borrower ->
                    UdhaarBorrowerRow(borrower, onClick = { viewModel.selectBorrower(borrower) })
                }
            }
            state.udhaarSearching -> Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }
            query.isNotEmpty() -> Text(
                stringResource(Res.string.pos_udhaar_not_found, query),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        OutlinedTextField(
            value = state.udhaarQuery,
            onValueChange = viewModel::onUdhaarQueryChange,
            label = { Text(stringResource(Res.string.pos_udhaar_search)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = if (state.udhaarSearching && state.udhaarResults.isNotEmpty()) {
                { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) }
            } else null,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(onClick = viewModel::startNewBorrower, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.pos_udhaar_add_new))
        }
    }
}

@Composable
private fun UdhaarBorrowerRow(borrower: BorrowerDto, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BorrowerAvatar(borrower.name.ifBlank { borrower.phone }, borrower.photoUrl, size = 40)
            Column(Modifier.weight(1f)) {
                Text(
                    borrower.name.ifBlank { borrower.phone },
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    listOf(borrower.phone, borrower.address).filter(String::isNotBlank).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (borrower.outstanding > 0) {
                Text(
                    stringResource(Res.string.udhaar_due, borrower.outstanding.asMoney()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun UdhaarConfirmStep(borrower: BorrowerDto, state: OrderEditorUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                BorrowerAvatar(borrower.name.ifBlank { borrower.phone }, borrower.photoUrl, size = 56)
                Column(Modifier.weight(1f)) {
                    Text(borrower.name.ifBlank { borrower.phone }, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text("+91 ${borrower.phone}", style = MaterialTheme.typography.bodyMedium)
                    if (borrower.address.isNotBlank()) {
                        Text(borrower.address, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            HorizontalDivider()
            UdhaarAmountRow(stringResource(Res.string.pos_udhaar_current_due), borrower.outstanding.asMoney())
            UdhaarAmountRow(stringResource(Res.string.pos_udhaar_this_bill), state.total.asMoney())
            UdhaarAmountRow(
                stringResource(Res.string.pos_udhaar_new_due),
                (borrower.outstanding + state.total).asMoney(),
                emphasize = true,
            )
        }
    }
    Text(
        stringResource(Res.string.pos_udhaar_confirm_question, borrower.name.ifBlank { borrower.phone }),
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun UdhaarAmountRow(label: String, value: String, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium)
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else null,
        )
    }
}

@Composable
private fun UdhaarNewBorrowerStep(state: OrderEditorUiState, viewModel: OrderEditorViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Text(
            stringResource(Res.string.pos_udhaar_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            PartnerProfileCircle(
                imageUrl = "",
                onClick = viewModel::takeBorrowerPhoto,
                showEditBadge = true,
                localPreviewBytes = state.udhaarPhotoPreview,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(Res.string.pos_udhaar_photo), style = MaterialTheme.typography.titleSmall)
                Row {
                    TextButton(onClick = viewModel::takeBorrowerPhoto) {
                        Icon(Icons.Filled.PhotoCamera, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            stringResource(
                                if (state.udhaarPhotoPreview == null) Res.string.pos_udhaar_take_photo else Res.string.pos_udhaar_retake_photo,
                            ),
                        )
                    }
                    if (state.udhaarPhotoPreview != null) {
                        TextButton(onClick = viewModel::removeBorrowerPhoto) { Text(stringResource(Res.string.action_remove)) }
                    }
                }
            }
        }
        OutlinedTextField(
            value = state.udhaarPhone,
            onValueChange = viewModel::onUdhaarPhoneChange,
            label = { Text(stringResource(Res.string.pos_udhaar_phone)) },
            prefix = { Text("+91 ") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.customerName,
            onValueChange = viewModel::onCustomerNameChange,
            label = { Text(stringResource(Res.string.pos_udhaar_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.udhaarAddress,
            onValueChange = viewModel::onUdhaarAddressChange,
            label = { Text(stringResource(Res.string.pos_udhaar_address)) },
            modifier = Modifier.fillMaxWidth(),
        )
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
