package org.bhargav.pansariwala.feature.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.PansariElevation
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.domain.model.DiscountType
import org.bhargav.pansariwala.domain.model.ShopOffer
import org.bhargav.pansariwala.ui.AsyncUiState
import org.bhargav.pansariwala.ui.errorBannerOrNull
import org.bhargav.pansariwala.ui.isBlockingLoad
import org.bhargav.pansariwala.ui.isRefreshing
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.offer_off_label
import pansariwala.shared.generated.resources.shop_offers_bill_wide
import pansariwala.shared.generated.resources.shop_offers_browse_shop
import pansariwala.shared.generated.resources.shop_offers_empty
import pansariwala.shared.generated.resources.shop_offers_min_bill
import pansariwala.shared.generated.resources.shop_offers_products_header
import pansariwala.shared.generated.resources.shop_offers_title
import pansariwala.shared.generated.resources.shop_offers_title_fallback
import kotlin.math.roundToInt

@Composable
fun ShopOffersScreen(
    shopId: String,
    onOpenProduct: (String) -> Unit,
    onOpenShop: () -> Unit,
    onOpenCart: () -> Unit,
    onBack: () -> Unit,
    viewModel: ShopOffersViewModel = koinViewModel(),
    cart: CartStore = koinInject(),
) {
    LaunchedEffect(shopId) { viewModel.load(shopId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val data = (state as? AsyncUiState.Success)?.data
    val lines = rememberCartLines(cart)
    val picker = rememberVariantPicker()
    VariantPickerHost(picker, cart)

    PansariScreen(
        title = data?.shopName?.let { stringResource(Res.string.shop_offers_title, it) }
            ?: stringResource(Res.string.shop_offers_title_fallback),
        onBack = onBack,
        error = state.errorBannerOrNull(),
        onErrorAction = { handleErrorBannerAction(it, onRetry = viewModel::retry, onDismiss = viewModel::dismissError) },
        isLoading = state.isBlockingLoad(),
        isRefreshing = state.isRefreshing(),
    ) {
        if (data == null) return@PansariScreen
        Column(Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (data.offers.isEmpty()) {
                    item(key = "empty", contentType = "empty") {
                        Text(
                            stringResource(Res.string.shop_offers_empty),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                items(data.offers, key = { it.id }, contentType = { "offer" }) { offer ->
                    ShopOfferCard(
                        offer = offer,
                        lines = lines,
                        cart = cart,
                        picker = picker,
                        onOpenProduct = onOpenProduct,
                    )
                }
                item(key = "browse", contentType = "footer") {
                    OutlinedButton(onClick = onOpenShop, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.shop_offers_browse_shop))
                    }
                }
            }
            CatalogCartBar(cartCount = CartStore.countItems(lines), onOpenCart = onOpenCart)
        }
    }
}

@Composable
private fun ShopOfferCard(
    offer: ShopOffer,
    lines: List<CartStore.Line>,
    cart: CartStore,
    picker: VariantPickerState,
    onOpenProduct: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.card),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    offer.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.weight(1f),
                )
                OfferTag(offer.valueLabel())
            }
            if (offer.description.isNotBlank()) {
                Text(
                    offer.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (offer.minBillAmount > 0.0) {
                Text(
                    stringResource(Res.string.shop_offers_min_bill, offer.minBillAmount.asMoney()),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (offer.products.isEmpty()) {
                Text(
                    stringResource(Res.string.shop_offers_bill_wide),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Text(
                    stringResource(Res.string.shop_offers_products_header),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                offer.products.forEach { product ->
                    CatalogProductRow(
                        product = product,
                        lines = lines,
                        cart = cart,
                        picker = picker,
                        onClick = { onOpenProduct(product.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ShopOffer.valueLabel(): String {
    val amount = when (type) {
        DiscountType.PERCENT -> "${value.roundToInt()}%"
        DiscountType.FLAT -> value.asMoney()
    }
    return stringResource(Res.string.offer_off_label, amount)
}
