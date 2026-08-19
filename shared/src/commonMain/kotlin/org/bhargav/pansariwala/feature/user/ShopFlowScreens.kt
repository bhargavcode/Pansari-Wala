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
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.PansariTopBar
import org.bhargav.pansariwala.domain.model.FulfillmentStep
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.account_all_orders
import pansariwala.shared.generated.resources.account_transactions
import pansariwala.shared.generated.resources.action_add_to_cart
import pansariwala.shared.generated.resources.action_checkout
import pansariwala.shared.generated.resources.action_place_order
import pansariwala.shared.generated.resources.action_save_rating
import pansariwala.shared.generated.resources.action_update_rating
import pansariwala.shared.generated.resources.catalog_title
import pansariwala.shared.generated.resources.checkout_delivery
import pansariwala.shared.generated.resources.checkout_discount
import pansariwala.shared.generated.resources.checkout_offers
import pansariwala.shared.generated.resources.checkout_payable
import pansariwala.shared.generated.resources.checkout_platform_fee
import pansariwala.shared.generated.resources.checkout_subtotal
import pansariwala.shared.generated.resources.checkout_title
import pansariwala.shared.generated.resources.delivery_partner_name
import pansariwala.shared.generated.resources.delivery_partner_phone
import pansariwala.shared.generated.resources.delivery_partner_title
import pansariwala.shared.generated.resources.delivery_partner_vehicle
import pansariwala.shared.generated.resources.order_cancelled_banner
import pansariwala.shared.generated.resources.order_items_title
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

@Composable
fun ShopCatalogScreen(
    shopId: String,
    onCheckout: () -> Unit,
    onBack: () -> Unit,
    viewModel: ShopCatalogViewModel = koinViewModel(),
) {
    LaunchedEffect(shopId) { viewModel.load(shopId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PansariTopBar(
            title = stringResource(Res.string.catalog_title),
            onBack = onBack,
        )
        state.products.forEach { product ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text(product.name, fontWeight = FontWeight.SemiBold)
                    Text("₹${product.sellingPrice}")
                }
                Button(onClick = { viewModel.add(product) }) { Text(stringResource(Res.string.action_add_to_cart)) }
            }
        }
        Button(onClick = onCheckout, enabled = state.cartCount > 0, modifier = Modifier.fillMaxWidth()) {
            Text("${stringResource(Res.string.action_checkout)} (${state.cartCount})")
        }
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun CheckoutScreen(
    shopId: String,
    onPlaced: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: CheckoutViewModel = koinViewModel(),
) {
    LaunchedEffect(shopId) { viewModel.load(shopId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PansariTopBar(
            title = stringResource(Res.string.checkout_title),
            onBack = onBack,
        )
        val quote = state.quote
        if (quote != null) {
            SectionCard(
                title = stringResource(Res.string.checkout_offers, state.offers.size),
                modifier = Modifier.clickable(enabled = !state.placing, onClick = viewModel::toggleOffers),
            ) {
                if (state.offersExpanded) {
                    state.offers.forEach { Text("${it.title} · ${it.discountPercent}%") }
                }
            }
            Text("${stringResource(Res.string.checkout_subtotal)}  ₹${quote.itemsSubtotal}")
            Text("${stringResource(Res.string.checkout_discount)}  ₹${quote.discount}")
            Text("${stringResource(Res.string.checkout_platform_fee)}  ₹${quote.platformFee}")
            Text("${stringResource(Res.string.checkout_delivery)}  ₹${quote.deliveryCharge}")
            Text("${stringResource(Res.string.checkout_payable)}  ₹${quote.payable}", fontWeight = FontWeight.Bold)
        }
        Button(
            onClick = { viewModel.place(shopId, onPlaced) },
            enabled = !state.placing && quote != null,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.placing) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(Res.string.action_place_order))
            }
        }
        state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error) }
    }
}

@Composable
fun ThankYouScreen(
    onContinue: () -> Unit,
    viewModel: ThankYouViewModel = koinViewModel(),
) {
    LaunchedEffect(Unit) { viewModel.goNext(onContinue) }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text(stringResource(Res.string.thank_you_title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(stringResource(Res.string.thank_you_body))
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 16.dp))
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
    Column(
        Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (order != null) {
            PansariTopBar(
                title = order.shopName ?: order.shopId,
                onBack = onBack,
            )

            Text(
                order.id,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                stringResource(Res.string.order_total_label, order.totalValue.asMoney()),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
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
                    title = stringResource(
                        if (isCancelled) Res.string.status_cancelled
                        else Res.string.status_rejected,
                    ),
                ) {
                    Text(
                        stringResource(
                            if (isCancelled) Res.string.order_cancelled_banner
                            else Res.string.order_rejected_banner,
                        ),
                        color = MaterialTheme.colorScheme.error,
                    )
                    order.cancelReason?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else {
                SectionCard(title = stringResource(Res.string.order_progress_title)) {
                    OrderProgressStepper(current = state.step)
                }
            }
            SectionCard(title = stringResource(Res.string.order_items_title)) {
                order.items.forEach { item ->
                    Text(
                        text = "${item.productName}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "${item.quantity.asQuantity()} · ${item.unitPrice.asMoney()} · ${item.lineTotal.asMoney()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, bottom = 8.dp),
                    )
                }
            }
            if (order.hasAssignedPartner) {
                SectionCard(title = stringResource(Res.string.delivery_partner_title)) {
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
            }
            val delivered = order.status == OrderStatus.DELIVERED || order.status == OrderStatus.COMPLETED
            if (delivered) {
                SectionCard(title = stringResource(Res.string.rate_order_title)) {
                    Text("${state.stars} / 5")
                    Slider(
                        value = state.stars.toFloat(),
                        onValueChange = { viewModel.setStars(it.toInt()) },
                        valueRange = 0f..5f,
                        steps = 4,
                        enabled = state.editingRating,
                    )
                    OutlinedTextField(state.comment, viewModel::setComment, enabled = state.editingRating, modifier = Modifier.fillMaxWidth())
                    if (state.editingRating) {
                        Button(onClick = viewModel::saveRating, enabled = viewModel.canSaveRating()) {
                            Text(stringResource(if (order.rating == null) Res.string.action_save_rating else Res.string.action_update_rating))
                        }
                    } else {
                        TextButton(onClick = viewModel::startEdit) { Text(stringResource(Res.string.action_update_rating)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderProgressStepper(current: FulfillmentStep) {
    val steps = listOf(
        FulfillmentStep.PLACED to Res.string.order_step_placed,
        FulfillmentStep.ACCEPTED to Res.string.order_step_accepted,
        FulfillmentStep.PACKING to Res.string.order_step_packing,
        FulfillmentStep.ON_THE_WAY to Res.string.order_step_on_the_way,
        FulfillmentStep.DELIVERED to Res.string.order_step_delivered,
    )
    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
        steps.forEachIndexed { index, (step, labelRes) ->
            val completed = step.ordinal < current.ordinal ||
                (current == FulfillmentStep.DELIVERED && step.ordinal <= current.ordinal)
            val isCurrent = step == current && current != FulfillmentStep.DELIVERED
            val circleColor = when {
                completed -> MaterialTheme.colorScheme.primary
                isCurrent -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.outline
            }
            Row(verticalAlignment = Alignment.Top) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(28.dp).clip(CircleShape).background(circleColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (completed) {
                            Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (index != steps.lastIndex) {
                        Box(
                            Modifier.width(2.dp).height(28.dp).background(
                                if (completed) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                            ),
                        )
                    }
                }
                Text(
                    stringResource(labelRes),
                    modifier = Modifier.padding(start = 12.dp, top = 4.dp),
                    fontWeight = if (completed || isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (completed || isCurrent) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
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
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        PansariTopBar(
            title = stringResource(Res.string.account_all_orders),
            onBack = onBack,
        )
        state.orders.forEach { OrderRow(it, onClick = { onOpen(it.id) }) }
    }
}

@Composable
fun TransactionsScreen(
    onBack: () -> Unit,
    viewModel: AccountViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(16.dp).verticalScroll(rememberScrollState())) {
        PansariTopBar(
            title = stringResource(Res.string.account_transactions),
            onBack = onBack,
        )
        state.txns.forEach { Text("${it.title}  ₹${it.amount}") }
    }
}
