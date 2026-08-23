package org.bhargav.pansariwala.feature.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.PansariTopBar
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.domain.model.FulfillmentStep
import org.bhargav.pansariwala.domain.model.OrderStatus
import androidx.compose.material3.Card
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.rememberModalBottomSheetState
import org.bhargav.pansariwala.domain.model.ProductCategory
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
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.account_all_orders
import pansariwala.shared.generated.resources.account_transactions
import pansariwala.shared.generated.resources.action_add_address
import pansariwala.shared.generated.resources.action_complete_profile
import pansariwala.shared.generated.resources.action_place_order
import pansariwala.shared.generated.resources.action_retry
import pansariwala.shared.generated.resources.checkout_confirm_address
import pansariwala.shared.generated.resources.action_proceed_payment
import pansariwala.shared.generated.resources.action_save_rating
import pansariwala.shared.generated.resources.action_update_rating
import pansariwala.shared.generated.resources.cart_title
import pansariwala.shared.generated.resources.shop_distance
import pansariwala.shared.generated.resources.catalog_no_products
import pansariwala.shared.generated.resources.catalog_no_ratings
import pansariwala.shared.generated.resources.catalog_rating_filter
import pansariwala.shared.generated.resources.catalog_rating_filter_all
import pansariwala.shared.generated.resources.catalog_rating_filter_title
import pansariwala.shared.generated.resources.catalog_rating_stars
import pansariwala.shared.generated.resources.catalog_search_products
import pansariwala.shared.generated.resources.catalog_shop_rating_count
import pansariwala.shared.generated.resources.catalog_tab_products
import pansariwala.shared.generated.resources.catalog_tab_ratings
import pansariwala.shared.generated.resources.action_apply
import pansariwala.shared.generated.resources.action_clear
import pansariwala.shared.generated.resources.checkout_delivery
import pansariwala.shared.generated.resources.checkout_discount
import pansariwala.shared.generated.resources.checkout_discount_applied
import pansariwala.shared.generated.resources.checkout_offers_expand
import pansariwala.shared.generated.resources.checkout_payable
import pansariwala.shared.generated.resources.checkout_platform_fee
import pansariwala.shared.generated.resources.checkout_razorpay_hint
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
    onBack: () -> Unit,
    viewModel: ShopCatalogViewModel = koinViewModel(),
) {
    LaunchedEffect(shopId) { viewModel.load(shopId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val data = (state as? AsyncUiState.Success)?.data
    val ratingFilterSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draftRatingStars by remember(data?.showRatingFilterSheet) {
        mutableStateOf(data?.ratingFilterStars ?: emptySet())
    }

    if (data?.showRatingFilterSheet == true) {
        ModalBottomSheet(
            onDismissRequest = viewModel::hideRatingFilterSheet,
            sheetState = ratingFilterSheetState,
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.catalog_rating_filter_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                (5 downTo 1).forEach { stars ->
                    Row(
                        Modifier.fillMaxWidth().clickable {
                            draftRatingStars = draftRatingStars.toMutableSet().apply {
                                if (contains(stars)) remove(stars) else add(stars)
                            }
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Checkbox(checked = stars in draftRatingStars, onCheckedChange = null)
                        Text(stringResource(Res.string.catalog_rating_stars, stars))
                    }
                }
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TextButton(onClick = { draftRatingStars = emptySet() }) {
                        Text(stringResource(Res.string.action_clear))
                    }
                    UserPrimaryButton(
                        text = stringResource(Res.string.action_apply),
                        onClick = {
                            viewModel.setRatingFilter(draftRatingStars)
                            viewModel.hideRatingFilterSheet()
                        },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }

    PansariScreen(
        title = data?.shop?.name ?: "…",
        onBack = onBack,
        error = state.errorBannerOrNull(),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = { viewModel.load(shopId) }, onDismiss = viewModel::dismissError)
        },
        isLoading = state.isBlockingLoad(),
        isRefreshing = state.isRefreshing(),
    ) {
        Column(Modifier.fillMaxSize()) {
            Column(
                Modifier.weight(1f).padding(horizontal = 16.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = data?.productQuery.orEmpty(),
                    onValueChange = viewModel::setProductQuery,
                    label = { Text(stringResource(Res.string.catalog_search_products)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                )
                data?.shop?.let { shop ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    ) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier.size(48.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(shop.name.take(1), fontWeight = FontWeight.Bold)
                            }
                            Column(modifier = Modifier.padding(start = 12.dp)) {
                                Text(shop.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                                ShopRatingBadge(
                                    rating = shop.rating,
                                    ratingCount = shop.ratingCount,
                                    onClick = { viewModel.selectTab(CatalogTab.RATINGS) },
                                )
                                Text(
                                    stringResource(
                                        Res.string.shop_distance,
                                        shop.distanceKm.let { (it * 10).toInt() / 10.0 }.toString(),
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
                TabRow(selectedTabIndex = if (data?.selectedTab == CatalogTab.RATINGS) 1 else 0) {
                    Tab(
                        selected = data?.selectedTab == CatalogTab.PRODUCTS,
                        onClick = { viewModel.selectTab(CatalogTab.PRODUCTS) },
                        text = { Text(stringResource(Res.string.catalog_tab_products)) },
                    )
                    Tab(
                        selected = data?.selectedTab == CatalogTab.RATINGS,
                        onClick = { viewModel.selectTab(CatalogTab.RATINGS) },
                        text = { Text(stringResource(Res.string.catalog_tab_ratings)) },
                    )
                }
                when (data?.selectedTab) {
                    CatalogTab.RATINGS -> {
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable(onClick = viewModel::showRatingFilterSheet),
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(stringResource(Res.string.catalog_rating_filter), fontWeight = FontWeight.Medium)
                                Text(
                                    if (data.ratingFilterStars.isEmpty()) {
                                        stringResource(Res.string.catalog_rating_filter_all)
                                    } else {
                                        data.ratingFilterStars.sortedDescending().joinToString(", ") { "$it★" }
                                    },
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        val ratings = viewModel.filteredRatings(data)
                        if (ratings.isEmpty()) {
                            Text(stringResource(Res.string.catalog_no_ratings), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            ratings.forEach { review ->
                                ShopReviewCard(review = review)
                            }
                        }
                    }
                    else -> {
                        val products = data?.let { viewModel.filteredProducts(it) }.orEmpty()
                        if (products.isEmpty()) {
                            Text(stringResource(Res.string.catalog_no_products), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            products.groupBy { it.category }.forEach { (category, grouped) ->
                                Text(
                                    category.displayName,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 8.dp),
                                )
                                grouped.forEach { product ->
                                    CatalogProductRow(
                                        name = product.name,
                                        price = product.sellingPrice.asMoney(),
                                        quantity = viewModel.quantityOf(product.id),
                                        onIncrement = {
                                            if (viewModel.quantityOf(product.id) == 0) viewModel.add(product)
                                            else viewModel.increment(product.id)
                                        },
                                        onDecrement = { viewModel.decrement(product.id) },
                                        onAdd = { viewModel.add(product) },
                                    )
                                }
                            }
                        }
                    }
                }
            }
            if ((data?.cartCount ?: 0) > 0) {
                UserPrimaryButton(
                    text = stringResource(Res.string.user_cart_items, data?.cartCount ?: 0),
                    onClick = onOpenCart,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

@Composable
private fun CatalogProductRow(
    name: String,
    price: String,
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onAdd: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Medium)
            Text(price, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
        }
        if (quantity > 0) {
            QuantityStepper(quantity = quantity, onIncrement = onIncrement, onDecrement = onDecrement)
        } else {
            TextButton(onClick = onAdd) { Text("+") }
        }
    }
}

@Composable
fun CartScreen(
    shopId: String,
    onCheckout: () -> Unit,
    onBack: () -> Unit,
    viewModel: CartViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PansariScreen(
        title = stringResource(Res.string.cart_title),
        onBack = onBack,
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            state.lines.forEach { line ->
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(line.product.name, fontWeight = FontWeight.Medium)
                        Text(
                            "${line.quantity.asQuantity()} × ${line.product.sellingPrice.asMoney()}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    QuantityStepper(
                        quantity = line.quantity.toInt(),
                        onIncrement = { viewModel.increment(line.product.id) },
                        onDecrement = { viewModel.decrement(line.product.id) },
                    )
                }
                HorizontalDivider()
            }
        }
        UserPrimaryButton(
            text = stringResource(Res.string.action_proceed_payment, state.subtotal.asMoney()),
            onClick = onCheckout,
            enabled = state.lines.isNotEmpty(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    shopId: String,
    onPlaced: (String) -> Unit,
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
        val selectedLabel = selected?.let { listOf(it.line, it.locality).filter { part -> part.isNotBlank() }.joinToString(", ") }
            ?: stringResource(Res.string.checkout_confirm_address)
        ExposedDropdownMenuBox(
            expanded = addressMenuOpen,
            onExpandedChange = { addressMenuOpen = it },
        ) {
            OutlinedTextField(
                value = selectedLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text(stringResource(Res.string.checkout_confirm_address)) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = addressMenuOpen) },
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
        UserPrimaryButton(
            text = stringResource(Res.string.action_place_order),
            onClick = { viewModel.place(shopId, onPlaced) },
            enabled = !state.placing && quote != null,
        )
        if (state.needsProfile) {
            UserPrimaryButton(
                text = stringResource(Res.string.action_complete_profile),
                onClick = onCompleteProfile,
                enabled = !state.placing,
            )
        } else {
            Text(
                stringResource(Res.string.checkout_razorpay_hint),
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
fun OrderDetailsScreen(
    orderId: String,
    onBack: () -> Unit,
    viewModel: OrderDetailsViewModel = koinViewModel(),
) {
    LaunchedEffect(orderId) { viewModel.load(orderId) }
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
                    if (!isCancelled && !isRejected) {
                        order.deliveryOtp?.let {
                            SectionCard(title = stringResource(Res.string.order_otp_label, it)) {
                                Text(stringResource(Res.string.order_otp_share_hint))
                            }
                        }
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
                        order.items.forEach { item ->
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
    onBack: () -> Unit,
    viewModel: AccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val orders = (state as? AsyncUiState.Success)?.data?.orders.orEmpty()
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
        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            orders.forEach { order ->
                OrderAccountTile(order = order, onClick = { onOpen(order.id) })
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun TransactionsScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val txns = (state as? AsyncUiState.Success)?.data?.txns.orEmpty()
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
        Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
            txns.forEach { Text("${it.title}  ${it.amount.asMoney()}") }
        }
    }
}
