package org.bhargav.pansariwala.feature.user

import org.bhargav.pansariwala.designsystem.AppAlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.KeyboardType
import org.bhargav.pansariwala.domain.model.Order
import kotlin.time.Clock
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import org.bhargav.pansariwala.util.AppConstants
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.PansariTopBar
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.domain.model.FulfillmentStep
import org.bhargav.pansariwala.feature.order.DeliveringItemsList
import org.bhargav.pansariwala.domain.model.OrderStatus
import androidx.compose.material3.Card
import org.bhargav.pansariwala.domain.model.CatalogCategory
import org.bhargav.pansariwala.feature.delivery.PickupPhotoStrip
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.ui.AsyncUiState
import org.bhargav.pansariwala.ui.ErrorBannerState
import org.bhargav.pansariwala.ui.errorBannerOrNull
import org.bhargav.pansariwala.ui.isBlockingLoad
import org.bhargav.pansariwala.ui.isRefreshing
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.ui.graphics.Color
import org.bhargav.pansariwala.feature.delivery.returnReasonLabel
import pansariwala.shared.generated.resources.order_returned_title
import pansariwala.shared.generated.resources.order_returning_title
import pansariwala.shared.generated.resources.order_returned_body
import pansariwala.shared.generated.resources.order_returning_body
import pansariwala.shared.generated.resources.order_returned_collect_at
import pansariwala.shared.generated.resources.return_reason_label
import pansariwala.shared.generated.resources.checkout_policy_shop_fallback
import pansariwala.shared.generated.resources.cart_min_order_short
import pansariwala.shared.generated.resources.cart_free_delivery_short
import pansariwala.shared.generated.resources.cart_free_delivery_unlocked
import pansariwala.shared.generated.resources.catalog_shop_categories
import pansariwala.shared.generated.resources.account_all_orders
import pansariwala.shared.generated.resources.action_add_address
import pansariwala.shared.generated.resources.action_complete_profile
import pansariwala.shared.generated.resources.action_place_order
import pansariwala.shared.generated.resources.action_retry
import pansariwala.shared.generated.resources.checkout_confirm_address
import pansariwala.shared.generated.resources.checkout_loading_addresses
import pansariwala.shared.generated.resources.action_proceed_payment
import pansariwala.shared.generated.resources.action_save_rating
import pansariwala.shared.generated.resources.action_update_rating
import pansariwala.shared.generated.resources.cart_title
import pansariwala.shared.generated.resources.catalog_no_products
import pansariwala.shared.generated.resources.catalog_search_products
import pansariwala.shared.generated.resources.checkout_delivery
import pansariwala.shared.generated.resources.action_okay
import pansariwala.shared.generated.resources.shop_closed_message
import pansariwala.shared.generated.resources.shop_closed_title
import pansariwala.shared.generated.resources.checkout_pickup_address_fallback
import pansariwala.shared.generated.resources.checkout_pickup_body
import pansariwala.shared.generated.resources.checkout_pickup_title
import pansariwala.shared.generated.resources.checkout_discount
import pansariwala.shared.generated.resources.checkout_discount_applied
import pansariwala.shared.generated.resources.checkout_offers_expand
import pansariwala.shared.generated.resources.checkout_payable
import pansariwala.shared.generated.resources.checkout_platform_fee
import pansariwala.shared.generated.resources.checkout_razorpay_hint
import pansariwala.shared.generated.resources.checkout_cashfree_hint
import pansariwala.shared.generated.resources.pay_details_title
import pansariwala.shared.generated.resources.pay_detail_txn
import pansariwala.shared.generated.resources.pay_detail_utr
import pansariwala.shared.generated.resources.checkout_direct_upi_hint
import pansariwala.shared.generated.resources.checkout_subtotal
import pansariwala.shared.generated.resources.checkout_title
import pansariwala.shared.generated.resources.delivery_partner_name
import pansariwala.shared.generated.resources.delivery_partner_phone
import pansariwala.shared.generated.resources.delivery_partner_title
import pansariwala.shared.generated.resources.delivery_partner_vehicle
import pansariwala.shared.generated.resources.order_cancelled_banner
import pansariwala.shared.generated.resources.order_details_title
import pansariwala.shared.generated.resources.error_order_load_failed
import pansariwala.shared.generated.resources.order_items_title
import pansariwala.shared.generated.resources.order_number_label
import pansariwala.shared.generated.resources.order_otp_label
import pansariwala.shared.generated.resources.order_otp_share_hint
import pansariwala.shared.generated.resources.order_progress_title
import pansariwala.shared.generated.resources.order_rejected_banner
import pansariwala.shared.generated.resources.order_step_accepted
import pansariwala.shared.generated.resources.order_step_delivered
import pansariwala.shared.generated.resources.order_step_on_the_way
import pansariwala.shared.generated.resources.order_step_packing
import pansariwala.shared.generated.resources.order_step_placed
import pansariwala.shared.generated.resources.order_total_label
import pansariwala.shared.generated.resources.rate_order_title
import pansariwala.shared.generated.resources.status_cancelled
import pansariwala.shared.generated.resources.status_rejected
import pansariwala.shared.generated.resources.thank_you_body
import pansariwala.shared.generated.resources.thank_you_title
import pansariwala.shared.generated.resources.user_cart_items

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopCatalogScreen(
    shopId: String,
    onOpenCart: () -> Unit,
    onOpenCategory: (category: CatalogCategory?, query: String) -> Unit,
    onOpenProduct: (productId: String) -> Unit,
    onBack: () -> Unit,
    viewModel: ShopCatalogViewModel = koinViewModel(),
    cart: CartStore = koinInject(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(shopId, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.load(shopId, force = true)
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val data = (state as? AsyncUiState.Success)?.data
    val cartLines = rememberCartLines(cart)
    val picker = rememberVariantPicker()
    VariantPickerHost(picker, cart)
    if (data?.showRatingsSheet == true) {
        ShopRatingsSheet(
            shop = data.shop,
            ratings = viewModel.filteredRatings(data),
            allRatings = data.ratings,
            loaded = data.ratingsLoaded,
            selectedStars = data.ratingFilterStars,
            onToggleStars = viewModel::toggleRatingFilter,
            onClearFilter = viewModel::clearRatingFilter,
            onDismiss = viewModel::closeRatings,
        )
    }

    PansariScreen(
        title = data?.shop?.name ?: "…",
        onBack = onBack,
        error = state.errorBannerOrNull(),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = viewModel::refresh, onDismiss = viewModel::dismissError)
        },
        isLoading = state.isBlockingLoad(),
        isRefreshing = state.isRefreshing(),
    ) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                data?.shop?.let { shop ->
                    if (shop.isOpen && !shop.deliveryAvailable) {
                        item(key = "pickup_only", contentType = "strip") {
                            PickupOnlyStrip(Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp), shop.deliveryUntil)
                        }
                    }
                    item(key = "shop", contentType = "shop") {
                        ShopHeroHeader(
                            shop = shop,
                            onRatingsClick = viewModel::openRatings,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                item(key = "search", contentType = "search") {
                    OutlinedTextField(
                        value = data?.productQuery.orEmpty(),
                        onValueChange = viewModel::setProductQuery,
                        label = { Text(stringResource(Res.string.catalog_search_products)) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true,
                    )
                }
                when {
                    data == null -> Unit
                    else -> {
                        if (!data.isSearchActive && data.sections.size > 1) {
                            item(key = "category_chips", contentType = "chips") {
                                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                                    Text(
                                        stringResource(Res.string.catalog_shop_categories),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(bottom = 4.dp),
                                    )
                                    CatalogCategoryChips(
                                        sections = data.sections,
                                        onClick = { onOpenCategory(it, "") },
                                    )
                                }
                            }
                        }
                        if (data.searching) {
                            item(key = "searching", contentType = "progress") {
                                LinearProgressIndicator(Modifier.fillMaxWidth().padding(horizontal = 16.dp))
                            }
                        }
                        val sections = data.visibleSections
                        if (sections.isEmpty() && !data.searching) {
                            item(key = "empty", contentType = "empty") {
                                Text(
                                    stringResource(Res.string.catalog_no_products),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(16.dp),
                                )
                            }
                        }
                        val searchQuery = data.productQuery.trim()
                        catalogSectionItems(
                            sections = sections,
                            lines = cartLines,
                            picker = picker,
                            keyPrefix = if (data.isSearchActive) "s" else "c",
                            onOpenCategory = { onOpenCategory(it, searchQuery) },
                            onOpenProduct = onOpenProduct,
                            cart = cart,
                        )
                    }
                }
            }
            CatalogCartBar(cartCount = data?.cartCount ?: 0, onOpenCart = onOpenCart)
        }
    }
}

@Composable
fun CartScreen(
    shopId: String,
    onCheckout: () -> Unit,
    onBack: () -> Unit,
    viewModel: CartViewModel = koinViewModel(),
    cart: CartStore = koinInject(),
) {
    LaunchedEffect(shopId) { viewModel.load(shopId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val picker = rememberVariantPicker()
    VariantPickerHost(picker, cart)
    state.closedShopName?.let { name ->
        AppAlertDialog(
            onDismissRequest = viewModel::dismissShopClosed,
            title = { Text(stringResource(Res.string.shop_closed_title)) },
            text = { Text(stringResource(Res.string.shop_closed_message, name)) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissShopClosed) { Text(stringResource(Res.string.action_okay)) }
            },
        )
    }
    PansariScreen(
        title = stringResource(Res.string.cart_title),
        onBack = onBack,
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(
                it,
                onRetry = { viewModel.proceedToCheckout(shopId, onCheckout) },
                onDismiss = viewModel::dismissError,
            )
        },
        isLoading = state.checkingShop,
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            state.lines.forEach { line ->
                CartLineRow(
                    line = line,
                    onEdit = { picker.open(line.product, line.key) },
                    onIncrement = { viewModel.increment(line.key) },
                    onDecrement = { viewModel.decrement(line.key) },
                    onSetQuantity = { viewModel.setQuantity(line.key, it) },
                )
                HorizontalDivider()
            }
        }
        if (state.lines.isNotEmpty()) CartOrderRules(state)
        UserPrimaryButton(
            text = stringResource(Res.string.action_proceed_payment, state.subtotal.asMoney()),
            onClick = { viewModel.proceedToCheckout(shopId, onCheckout) },
            enabled = state.canCheckout,
            modifier = Modifier.padding(top = 12.dp),
        )
        Text(
            stringResource(Res.string.checkout_razorpay_hint),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
        )
        }
    }
}

/** Minimum-order shortfall (blocks checkout) and progress towards the shop's free-delivery amount. */
@Composable
private fun CartOrderRules(state: CartUiState) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (state.shortOfMinimum > 0.0) {
            Text(
                stringResource(
                    Res.string.cart_min_order_short,
                    state.shortOfMinimum.asMoney(),
                    state.minOrderValue.asMoney(),
                ),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (state.freeDeliveryAbove > 0.0) {
            val short = state.freeDeliveryAbove - state.subtotal
            Text(
                if (short > 0.0) {
                    stringResource(Res.string.cart_free_delivery_short, short.asMoney())
                } else {
                    stringResource(Res.string.cart_free_delivery_unlocked)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (short > 0.0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    shopId: String,
    onPlaced: (String) -> Unit,
    onPaymentStarted: (String) -> Unit,
    onCompleteProfile: () -> Unit,
    onAddAddress: () -> Unit,
    onBack: () -> Unit,
    viewModel: CheckoutViewModel = koinViewModel(),
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(shopId, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.load(shopId)
        }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val checkoutError = state.error.toErrorBanner()
        ?: state.snackbar?.let { ErrorBannerState.ack(it) }
    PansariScreen(
        title = stringResource(Res.string.checkout_title),
        onBack = onBack,
        error = checkoutError,
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = { viewModel.load(shopId) }, onDismiss = viewModel::dismissError)
        },
        isLoading = state.placing,
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        val quote = state.quote
        var addressMenuOpen by remember { mutableStateOf(false) }
        val selected = state.addresses.firstOrNull { it.id == state.selectedAddressId }
        val selectedLabel = when {
            selected != null -> listOf(selected.line, selected.locality).filter { it.isNotBlank() }.joinToString(", ")
            state.loadingAddresses -> stringResource(Res.string.checkout_loading_addresses)
            else -> stringResource(Res.string.checkout_confirm_address)
        }
        ExposedDropdownMenuBox(
            expanded = addressMenuOpen,
            onExpandedChange = { if (!state.loadingAddresses) addressMenuOpen = it },
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                enabled = !state.loadingAddresses,
                label = { Text(stringResource(Res.string.checkout_confirm_address)) },
                trailingIcon = {
                    if (state.loadingAddresses) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = addressMenuOpen)
                    }
                },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            DropdownMenu(expanded = addressMenuOpen, onDismissRequest = { addressMenuOpen = false }) {
                state.addresses.forEach { address ->
                    DropdownMenuItem(
                        text = { Text(listOf(address.line, address.locality).filter { it.isNotBlank() }.joinToString(", ")) },
                        onClick = {
                            addressMenuOpen = false
                            viewModel.selectAddress(shopId, address.id)
                        },
                    )
                }
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.action_add_address)) },
                    onClick = {
                        addressMenuOpen = false
                        onAddAddress()
                    },
                )
            }
        }
        if (state.loadingQuote && quote == null) {
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }
        if (state.selfPickup) {
            SectionCard(title = stringResource(Res.string.checkout_pickup_title)) {
                Text(
                    stringResource(
                        Res.string.checkout_pickup_body,
                        state.shop?.let { shop -> listOf(shop.name, shop.address).filter { it.isNotBlank() }.joinToString(", ") }
                            ?.ifBlank { null }
                            ?: stringResource(Res.string.checkout_pickup_address_fallback),
                    ),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        if (quote != null) {
            SectionCard(
                title = stringResource(Res.string.checkout_offers_expand),
                modifier = Modifier.clickable(enabled = !state.placing, onClick = viewModel::toggleOffers),
            ) {
                if (state.offersExpanded) {
                    state.offers.forEach { offer ->
                        Text("${offer.title} · ${offer.discountPercent}%")
                    }
                }
                if (quote.discount > 0) {
                    Text(
                        stringResource(Res.string.checkout_discount_applied, quote.discount.asMoney()),
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
            CheckoutLine(stringResource(Res.string.checkout_subtotal), quote.itemsSubtotal.asMoney())
            if (quote.discount > 0) CheckoutLine(stringResource(Res.string.checkout_discount), "-${quote.discount.asMoney()}")
            CheckoutLine(stringResource(Res.string.checkout_platform_fee), quote.platformFee.asMoney())
            CheckoutLine(stringResource(Res.string.checkout_delivery), quote.deliveryCharge.asMoney())
            HorizontalDivider()
            CheckoutLine(stringResource(Res.string.checkout_payable), quote.payable.asMoney(), bold = true)
        }
        var showPolicy by rememberSaveable { mutableStateOf(false) }
        UserPrimaryButton(
            text = stringResource(Res.string.action_place_order),
            onClick = { showPolicy = true },
            enabled = !state.placing && quote != null,
        )
        if (showPolicy && quote != null) {
            CheckoutPolicySheet(
                shopName = state.shop?.name,
                payableLabel = quote.payable.asMoney(),
                onConfirm = {
                    showPolicy = false
                    viewModel.place(shopId, onPlaced, onPaymentStarted)
                },
                onDismiss = { showPolicy = false },
            )
        }
        if (state.needsProfile) {
            UserPrimaryButton(
                text = stringResource(Res.string.action_complete_profile),
                onClick = onCompleteProfile,
                enabled = !state.placing,
            )
        } else {
            Text(
                stringResource(
                    when (quote?.paymentMode) {
                        AppConstants.Payment.MODE_DIRECT_UPI -> Res.string.checkout_direct_upi_hint
                        AppConstants.Payment.MODE_CASHFREE -> Res.string.checkout_cashfree_hint
                        else -> Res.string.checkout_razorpay_hint
                    },
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        }
    }
}

@Composable
private fun CheckoutLine(label: String, value: String, bold: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
        Text(value, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun ThankYouScreen(
    onContinue: () -> Unit,
    viewModel: ThankYouViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.goNext(onContinue) }
    PansariScreen {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(Res.string.thank_you_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(Res.string.thank_you_body), modifier = Modifier.padding(top = 8.dp))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
        }
    }
}

@Composable
private fun OrderPaymentCard(order: Order) {
    val txn = order.paymentIntentId ?: return
    SectionCard(title = stringResource(Res.string.pay_details_title)) {
        Text(stringResource(Res.string.pay_detail_txn, txn), style = MaterialTheme.typography.bodySmall)
        order.paymentUtr?.let {
            Text(stringResource(Res.string.pay_detail_utr, it), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun OrderDetailsScreen(
    orderId: String,
    onBack: () -> Unit,
    viewModel: OrderDetailsViewModel = koinViewModel(),
) {
    LaunchedEffect(orderId) { viewModel.load(orderId) }
    LifecycleResumeEffect(viewModel) {
        viewModel.onScreenVisible(true)
        onPauseOrDispose { viewModel.onScreenVisible(false) }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val order = state.order
    PansariScreen(
        title = order?.shopName?.takeIf { it.isNotBlank() }
            ?: order?.shopId
            ?: stringResource(Res.string.order_details_title),
        onBack = onBack,
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = { viewModel.load(orderId) }, onDismiss = viewModel::dismissError)
        },
        isLoading = order == null && state.error == null,
    ) {
        Column(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            when {
                order == null && state.error == null -> CircularProgressIndicator()
                order != null -> {
                    Text(
                        stringResource(Res.string.order_number_label, order.displayNumber),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        stringResource(Res.string.order_total_label, order.totalValue.asMoney()),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val isCancelled = order.status == OrderStatus.CANCELLED
                    val isRejected = order.status == OrderStatus.REJECTED
                    OrderPaymentCard(order)
                    if (!isCancelled && !isRejected) {
                        order.deliveryOtp?.let {
                            SectionCard(title = stringResource(Res.string.order_otp_label, it)) {
                                Text(stringResource(Res.string.order_otp_share_hint))
                            }
                        }
                    }
                    if (order.status == OrderStatus.RETURNING_TO_SHOP || order.status == OrderStatus.RETURNED_TO_SHOP) {
                        OrderReturnBanner(order)
                    }
                    if (isCancelled || isRejected) {
                        SectionCard(
                            title = stringResource(if (isCancelled) Res.string.status_cancelled else Res.string.status_rejected),
                        ) {
                            Text(
                                stringResource(if (isCancelled) Res.string.order_cancelled_banner else Res.string.order_rejected_banner),
                                color = MaterialTheme.colorScheme.error,
                            )
                            order.cancelReason?.takeIf { it.isNotBlank() }?.let {
                                Spacer(Modifier.height(4.dp))
                                Text(it, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    } else {
                        SectionCard(title = stringResource(Res.string.order_progress_title)) {
                            OrderProgressStepper(
                                current = state.step,
                                pickupPhotos = order.visiblePickupPhotos,
                            )
                        }
                    }
                    SectionCard(title = stringResource(Res.string.order_items_title)) {
                        if (order.isPackedForDelivery) {
                            DeliveringItemsList(order)
                        } else order.items.forEach { item ->
                            Text(item.productName, fontWeight = FontWeight.SemiBold)
                            Text(
                                text = "${item.quantity.asQuantity()} · ${item.unitPrice.asMoney()} · ${item.lineTotal.asMoney()}",
                                modifier = Modifier.padding(bottom = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        order.quote?.let { q ->
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(Modifier.height(8.dp))
                            CheckoutLine(stringResource(Res.string.checkout_subtotal), q.itemsSubtotal.asMoney())
                            CheckoutLine(stringResource(Res.string.checkout_platform_fee), q.platformFee.asMoney())
                            CheckoutLine(stringResource(Res.string.checkout_delivery), q.deliveryCharge.asMoney())
                            CheckoutLine(stringResource(Res.string.checkout_payable), q.payable.asMoney(), bold = true)
                        }
                    }
                    if (order.hasAssignedPartner) {
                        SectionCard(title = stringResource(Res.string.delivery_partner_title)) {
                            order.partnerName?.takeIf { it.isNotBlank() }?.let { Text(stringResource(Res.string.delivery_partner_name, it)) }
                            order.partnerPhone?.takeIf { it.isNotBlank() }?.let { Text(stringResource(Res.string.delivery_partner_phone, it)) }
                            order.partnerVehicleReg?.takeIf { it.isNotBlank() }?.let { Text(stringResource(Res.string.delivery_partner_vehicle, it)) }
                        }
                    }
                    val delivered = order.status == OrderStatus.DELIVERED || order.status == OrderStatus.COMPLETED
                    if (delivered) {
                        SectionCard(title = stringResource(Res.string.rate_order_title)) {
                            InteractiveStarRating(
                                stars = state.stars,
                                onStarsChange = viewModel::setStars,
                                enabled = state.editingRating,
                            )
                            OutlinedTextField(
                                value = state.comment,
                                onValueChange = viewModel::setComment,
                                enabled = state.editingRating,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            if (state.editingRating) {
                                UserPrimaryButton(
                                    text = stringResource(if (order.rating == null) Res.string.action_save_rating else Res.string.action_update_rating),
                                    onClick = viewModel::saveRating,
                                    enabled = viewModel.canSaveRating(),
                                )
                            } else {
                                TextButton(onClick = viewModel::startEdit) {
                                    Text(stringResource(Res.string.action_update_rating))
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        }
    }
}

@Composable
private fun OrderReturnBanner(order: Order) {
    val returned = order.status == OrderStatus.RETURNED_TO_SHOP
    val shop = order.shopName?.takeIf { it.isNotBlank() } ?: stringResource(Res.string.checkout_policy_shop_fallback)
    Surface(
        color = RETURN_BANNER_CONTAINER,
        contentColor = RETURN_BANNER_CONTENT,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, RETURN_BANNER_BORDER),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(
                if (returned) Icons.Filled.Storefront
                else Icons.AutoMirrored.Filled.AssignmentReturn,
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(if (returned) Res.string.order_returned_title else Res.string.order_returning_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(if (returned) Res.string.order_returned_body else Res.string.order_returning_body, shop),
                    style = MaterialTheme.typography.bodyMedium,
                )
                returnReasonLabel(order.cancelReason)?.let {
                    Text(
                        stringResource(Res.string.return_reason_label, it),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                order.shopAddress?.takeIf { returned && it.isNotBlank() }?.let {
                    Text(
                        stringResource(Res.string.order_returned_collect_at, it),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

private val RETURN_BANNER_CONTAINER = Color(0xFFFFF3E0)
private val RETURN_BANNER_BORDER = Color(0xFFFFB74D)
private val RETURN_BANNER_CONTENT = Color(0xFF8A3A00)

@Composable
private fun OrderProgressStepper(
    current: FulfillmentStep,
    pickupPhotos: List<String> = emptyList(),
) {
    val steps = listOf(
        FulfillmentStep.PLACED to Res.string.order_step_placed,
        FulfillmentStep.ACCEPTED to Res.string.order_step_accepted,
        FulfillmentStep.PACKING to Res.string.order_step_packing,
        FulfillmentStep.ON_THE_WAY to Res.string.order_step_on_the_way,
        FulfillmentStep.DELIVERED to Res.string.order_step_delivered,
    )
    Column {
        steps.forEachIndexed { index, (step, labelRes) ->
            val completed = step.ordinal < current.ordinal || (current == FulfillmentStep.DELIVERED && step.ordinal <= current.ordinal)
            val isCurrent = step == current && current != FulfillmentStep.DELIVERED
            val circleColor = when {
                completed -> MaterialTheme.colorScheme.primary
                isCurrent -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.outline
            }
            val showPhotos = step == FulfillmentStep.ON_THE_WAY && pickupPhotos.isNotEmpty() &&
                current.ordinal >= FulfillmentStep.ON_THE_WAY.ordinal
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(28.dp).clip(CircleShape).background(circleColor), contentAlignment = Alignment.Center) {
                        if (completed) Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                    }
                    if (index != steps.lastIndex) {
                        Box(
                            Modifier
                                .width(2.dp)
                                .height(if (showPhotos) 180.dp else 28.dp)
                                .background(if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        )
                    }
                }
                Column(Modifier.padding(start = 12.dp, top = 4.dp).weight(1f)) {
                    Text(
                        stringResource(labelRes),
                        fontWeight = if (completed || isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    )
                    if (showPhotos) {
                        PickupPhotoStrip(
                            photos = pickupPhotos,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OrdersListScreen(
    onOpen: (String) -> Unit,
    onOpenPayment: (String) -> Unit,
    onRetryPayment: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: AccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val data = (state as? AsyncUiState.Success)?.data
    val orders = data?.orders.orEmpty()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val nearEnd by remember {
        androidx.compose.runtime.derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && last >= info.totalItemsCount - 1 - AppConstants.LIST_PREFETCH_DISTANCE
        }
    }
    LaunchedEffect(nearEnd, data?.ordersHasMore, orders.size) {
        if (nearEnd && data?.ordersHasMore == true) viewModel.loadMoreOrders()
    }
    PansariScreen(
        title = stringResource(Res.string.account_all_orders),
        onBack = onBack,
        error = state.errorBannerOrNull(),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = viewModel::refresh, onDismiss = viewModel::dismissError)
        },
        isLoading = state.isBlockingLoad(),
        isRefreshing = state.isRefreshing(),
    ) {
        androidx.compose.foundation.lazy.LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
        ) {
            val payments = data?.payments.orEmpty()
            items(payments.size, key = { "pay_" + payments[it].id }) { index ->
                val intent = payments[index]
                PaymentAccountTile(
                    intent = intent,
                    busy = data?.paymentBusyId == intent.id,
                    onClick = { onOpenPayment(intent.id) },
                    onCheckStatus = { viewModel.checkPayment(intent.id) },
                    onRetry = { viewModel.retryPayment(intent.id, onRetryPayment) },
                )
                HorizontalDivider()
            }
            items(orders.size, key = { orders[it].id }) { index ->
                val order = orders[index]
                OrderAccountTile(order = order, onClick = { onOpen(order.id) })
                HorizontalDivider()
            }
            if (data?.loadingMoreOrders == true) {
                item(key = "loadingMore") {
                    Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                }
            }
        }
    }
}
