package org.bhargav.pansariwala.feature.user

import org.bhargav.pansariwala.designsystem.AppAlertDialog
import org.bhargav.pansariwala.designsystem.AppBottomSheet
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.runtime.getValue
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import pansariwala.shared.generated.resources.action_not_now
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.domain.model.MarketplaceShop
import org.bhargav.pansariwala.domain.model.ShopReview
import org.bhargav.pansariwala.domain.model.ShopSortOption
import org.bhargav.pansariwala.domain.model.ShopType
import org.bhargav.pansariwala.designsystem.PansariElevation
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_continue
import pansariwala.shared.generated.resources.checkout_payable
import pansariwala.shared.generated.resources.home_continue_last_order
import pansariwala.shared.generated.resources.checkout_subtotal
import pansariwala.shared.generated.resources.home_resume_checkout
import pansariwala.shared.generated.resources.home_resume_clear_cart
import pansariwala.shared.generated.resources.home_resume_later
import pansariwala.shared.generated.resources.home_resume_more_items
import pansariwala.shared.generated.resources.location_fetching
import pansariwala.shared.generated.resources.order_number_label
import pansariwala.shared.generated.resources.shop_distance
import pansariwala.shared.generated.resources.shop_open_now
import pansariwala.shared.generated.resources.shop_offers_active
import pansariwala.shared.generated.resources.shop_offers_active_one
import pansariwala.shared.generated.resources.shop_offers_show_all
import pansariwala.shared.generated.resources.shop_rating
import pansariwala.shared.generated.resources.shop_type_electronics
import pansariwala.shared.generated.resources.shop_type_general_store
import pansariwala.shared.generated.resources.shop_type_hardware
import pansariwala.shared.generated.resources.shop_type_medical_store
import pansariwala.shared.generated.resources.shop_type_stationery
import pansariwala.shared.generated.resources.shop_type_sweet_shop
import pansariwala.shared.generated.resources.shop_type_auto_parts
import pansariwala.shared.generated.resources.shop_type_ayurvedic
import pansariwala.shared.generated.resources.shop_type_bakery
import pansariwala.shared.generated.resources.shop_type_books
import pansariwala.shared.generated.resources.shop_type_boutique
import pansariwala.shared.generated.resources.shop_type_clothing
import pansariwala.shared.generated.resources.shop_type_cosmetics
import pansariwala.shared.generated.resources.shop_type_dairy
import pansariwala.shared.generated.resources.shop_type_electrical
import pansariwala.shared.generated.resources.shop_type_flowers
import pansariwala.shared.generated.resources.shop_type_footwear
import pansariwala.shared.generated.resources.shop_type_fruits_vegetables
import pansariwala.shared.generated.resources.shop_type_furniture
import pansariwala.shared.generated.resources.shop_type_gas_water
import pansariwala.shared.generated.resources.shop_type_grocery
import pansariwala.shared.generated.resources.shop_type_home_kitchen
import pansariwala.shared.generated.resources.shop_type_jewellery
import pansariwala.shared.generated.resources.shop_type_meat_fish
import pansariwala.shared.generated.resources.shop_type_mobile
import pansariwala.shared.generated.resources.shop_type_organic
import pansariwala.shared.generated.resources.shop_type_other
import pansariwala.shared.generated.resources.shop_type_paan_shop
import pansariwala.shared.generated.resources.shop_type_pet_supplies
import pansariwala.shared.generated.resources.shop_type_restaurant
import pansariwala.shared.generated.resources.shop_type_sports
import pansariwala.shared.generated.resources.shop_type_supermarket
import pansariwala.shared.generated.resources.shop_type_toys_gifts
import pansariwala.shared.generated.resources.shop_closed
import pansariwala.shared.generated.resources.shop_outside_delivery
import pansariwala.shared.generated.resources.status_accepted
import pansariwala.shared.generated.resources.status_cancelled
import pansariwala.shared.generated.resources.status_delivered
import pansariwala.shared.generated.resources.status_draft
import pansariwala.shared.generated.resources.status_payment_pending
import pansariwala.shared.generated.resources.status_on_the_way
import pansariwala.shared.generated.resources.status_packing
import pansariwala.shared.generated.resources.status_partner_accepted
import pansariwala.shared.generated.resources.status_pending_acceptance
import pansariwala.shared.generated.resources.status_received
import pansariwala.shared.generated.resources.status_rejected
import pansariwala.shared.generated.resources.user_brand_title
import pansariwala.shared.generated.resources.user_cart_items
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlinx.coroutines.delay
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.produceState
import org.bhargav.pansariwala.domain.model.PaymentIntent
import org.bhargav.pansariwala.domain.model.PaymentIntentStatus
import pansariwala.shared.generated.resources.pay_check_status
import pansariwala.shared.generated.resources.pay_detail_txn
import pansariwala.shared.generated.resources.pay_retry
import pansariwala.shared.generated.resources.pay_status_failed
import pansariwala.shared.generated.resources.pay_status_not_received
import pansariwala.shared.generated.resources.pay_status_pending
import pansariwala.shared.generated.resources.pay_status_refunded
import pansariwala.shared.generated.resources.pay_time_left

@Composable
fun shopTypeLabel(type: ShopType): String = stringResource(
    when (type) {
        ShopType.GROCERY -> Res.string.shop_type_grocery
        ShopType.SUPERMARKET -> Res.string.shop_type_supermarket
        ShopType.GENERAL_STORE -> Res.string.shop_type_general_store
        ShopType.FRUITS_VEGETABLES -> Res.string.shop_type_fruits_vegetables
        ShopType.DAIRY -> Res.string.shop_type_dairy
        ShopType.BAKERY -> Res.string.shop_type_bakery
        ShopType.MEAT_FISH -> Res.string.shop_type_meat_fish
        ShopType.SWEET_SHOP -> Res.string.shop_type_sweet_shop
        ShopType.RESTAURANT -> Res.string.shop_type_restaurant
        ShopType.MEDICAL_STORE -> Res.string.shop_type_medical_store
        ShopType.AYURVEDIC -> Res.string.shop_type_ayurvedic
        ShopType.ORGANIC -> Res.string.shop_type_organic
        ShopType.HARDWARE -> Res.string.shop_type_hardware
        ShopType.ELECTRICAL -> Res.string.shop_type_electrical
        ShopType.ELECTRONICS -> Res.string.shop_type_electronics
        ShopType.MOBILE -> Res.string.shop_type_mobile
        ShopType.STATIONERY -> Res.string.shop_type_stationery
        ShopType.BOOKS -> Res.string.shop_type_books
        ShopType.CLOTHING -> Res.string.shop_type_clothing
        ShopType.BOUTIQUE -> Res.string.shop_type_boutique
        ShopType.FOOTWEAR -> Res.string.shop_type_footwear
        ShopType.COSMETICS -> Res.string.shop_type_cosmetics
        ShopType.JEWELLERY -> Res.string.shop_type_jewellery
        ShopType.HOME_KITCHEN -> Res.string.shop_type_home_kitchen
        ShopType.FURNITURE -> Res.string.shop_type_furniture
        ShopType.TOYS_GIFTS -> Res.string.shop_type_toys_gifts
        ShopType.SPORTS -> Res.string.shop_type_sports
        ShopType.PET_SUPPLIES -> Res.string.shop_type_pet_supplies
        ShopType.FLOWERS -> Res.string.shop_type_flowers
        ShopType.PAAN_SHOP -> Res.string.shop_type_paan_shop
        ShopType.GAS_WATER -> Res.string.shop_type_gas_water
        ShopType.AUTO_PARTS -> Res.string.shop_type_auto_parts
        ShopType.OTHER -> Res.string.shop_type_other
    },
)

@Composable
fun ShopRatingBadge(
    rating: Double,
    ratingCount: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val displayRating = ((rating * 10).roundToInt() / 10.0).toString()
    Text(
        text = stringResource(Res.string.shop_rating, displayRating, ratingCount),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Medium,
        modifier = modifier.then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    )
}

@Composable
fun ShopReviewCard(review: ShopReview, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.card),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(review.customerName, fontWeight = FontWeight.SemiBold)
                Text("★ ${review.stars}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
            review.comment?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun UserBrandHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "PW",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(Res.string.user_brand_title),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
fun UserPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    ElevatedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.elevatedButtonElevation(defaultElevation = PansariElevation.button),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ShopDiscoveryCard(
    shop: MarketplaceShop,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onShowAllOffers: () -> Unit = onClick,
) {
    val showPickupStrip = shop.isOpen && !shop.deliveryAvailable
    val hasFooter = showPickupStrip || shop.offerCount > 0
    ShopHeroHeader(
        shop = shop,
        onRatingsClick = onClick,
        onClick = onClick,
        heroHeight = SHOP_LIST_HERO_HEIGHT,
        modifier = modifier,
        footer = if (hasFooter) {
            {
                if (showPickupStrip) PickupOnlyStrip(deliveryUntil = shop.deliveryUntil)
                ShopOffersSummary(shop, onShowAllOffers)
            }
        } else {
            null
        },
    )
}

@Composable
private fun ShopOffersSummary(shop: MarketplaceShop, onShowAllOffers: () -> Unit) {
    if (shop.offerCount <= 0) return
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.45f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.LocalOffer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = if (shop.offerCount == 1) {
                        stringResource(Res.string.shop_offers_active_one)
                    } else {
                        stringResource(Res.string.shop_offers_active, shop.offerCount)
                    },
                    modifier = Modifier.padding(start = 6.dp).weight(1f),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                if (shop.offerCount > AppConstants.SHOP_CARD_TOP_OFFERS) {
                    Text(
                        text = stringResource(Res.string.shop_offers_show_all),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable(onClick = onShowAllOffers).padding(4.dp),
                    )
                }
            }
            shop.topOffers.take(AppConstants.SHOP_CARD_TOP_OFFERS).forEach { title ->
                Text(
                    text = "• $title",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val SHOP_LIST_HERO_HEIGHT = 170.dp

/** Offered on home when a cart was left from an earlier session. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResumeCartSheet(
    shopName: String,
    lines: List<CartStore.Line>,
    subtotal: Double,
    onResume: () -> Unit,
    onClearCart: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShoppingCart, null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(Res.string.home_continue_last_order),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Text(shopName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    lines.take(RESUME_SHEET_MAX_LINES).forEach { line ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                listOfNotNull(line.product.name, line.variant?.label).joinToString(" · ") +
                                    " × ${line.option.quantityLabel(line.quantity)}",
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(line.lineTotal.asMoney(), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (lines.size > RESUME_SHEET_MAX_LINES) {
                        Text(
                            stringResource(Res.string.home_resume_more_items, lines.size - RESUME_SHEET_MAX_LINES),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(stringResource(Res.string.checkout_subtotal), fontWeight = FontWeight.Medium)
                        Text(subtotal.asMoney(), fontWeight = FontWeight.Bold)
                    }
                }
            }
            UserPrimaryButton(
                text = stringResource(Res.string.home_resume_checkout),
                onClick = onResume,
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onClearCart) {
                    Text(stringResource(Res.string.home_resume_clear_cart), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.home_resume_later)) }
            }
        }
    }
}

/** Top-bar cart action; the badge shows the item count when the cart is not empty. */
@Composable
fun CartBadgeButton(itemCount: Int, onClick: () -> Unit) {
    val description = stringResource(Res.string.user_cart_items, itemCount)
    IconButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = description }) {
        BadgedBox(
            badge = {
                if (itemCount > 0) Badge { Text(if (itemCount > MAX_BADGE_COUNT) "$MAX_BADGE_COUNT+" else "$itemCount") }
            },
        ) {
            Icon(Icons.Default.ShoppingCart, contentDescription = null)
        }
    }
}

private const val RESUME_SHEET_MAX_LINES = 4
private const val MAX_BADGE_COUNT = 99

@Composable
fun QuantityStepper(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
) = QuantityStepper(
    label = quantity.toString(),
    onIncrement = onIncrement,
    onDecrement = onDecrement,
    modifier = modifier,
    decrementEnabled = quantity > 0,
)

@Composable
fun QuantityStepper(
    label: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    decrementEnabled: Boolean = true,
    incrementEnabled: Boolean = true,
    onLabelClick: (() -> Unit)? = null,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = PansariElevation.chip,
    ) {
        Row(
            modifier = Modifier
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
        IconButton(onClick = onDecrement, enabled = decrementEnabled, modifier = Modifier.size(36.dp)) {
            Text("-", fontWeight = FontWeight.Bold)
        }
        Text(
            text = label,
            modifier = Modifier.widthIn(min = 28.dp)
                .then(if (onLabelClick != null) Modifier.clickable(onClick = onLabelClick) else Modifier),
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        IconButton(onClick = onIncrement, enabled = incrementEnabled, modifier = Modifier.size(36.dp)) {
            Text("+", fontWeight = FontWeight.Bold)
        }
        }
    }
}

@Composable
fun InteractiveStarRating(
    stars: Int,
    onStarsChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..5).forEach { star ->
            Text(
                text = if (star <= stars) "★" else "☆",
                modifier = Modifier
                    .clickable(enabled = enabled) { onStarsChange(star) }
                    .padding(4.dp),
                style = MaterialTheme.typography.headlineSmall,
                color = if (star <= stars) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )
        }
    }
}

@Composable
fun UserMenuRow(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: String? = null,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            subtitle?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
        Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun UserLocationOnboardingHero(
    fetching: Boolean,
    modifier: Modifier = Modifier,
) {
    val pulse = rememberInfiniteTransition(label = "locationPulse")
    val ringScale by pulse.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ringScale",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(220.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .graphicsLayer { scaleX = ringScale; scaleY = ringScale }
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
        )
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("📍", style = MaterialTheme.typography.displayMedium)
        }
        Text(
            text = "🏪",
            modifier = Modifier.align(Alignment.TopStart).padding(28.dp),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "🛵",
            modifier = Modifier.align(Alignment.TopEnd).padding(28.dp),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = "⭐",
            modifier = Modifier.align(Alignment.BottomStart).padding(28.dp),
            style = MaterialTheme.typography.headlineMedium,
        )
        if (fetching) {
            Text(
                text = stringResource(Res.string.location_fetching),
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

/** Explains why location is needed before the OS permission (and GPS check) is shown. */
@Composable
fun LocationAccessPromptDialog(
    title: String,
    subtitle: String,
    reasons: List<Pair<String, String>>,
    allowLabel: String,
    onAllow: () -> Unit,
    onNotNow: () -> Unit,
) {
    AppAlertDialog(
        onDismissRequest = onNotNow,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
                reasons.forEach { (emoji, text) -> UserLocationReasonRow(text, emoji) }
            }
        },
        confirmButton = {
            androidx.compose.material3.Button(onClick = onAllow) { Text(allowLabel) }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onNotNow) {
                Text(stringResource(Res.string.action_not_now))
            }
        },
    )
}

@Composable
fun UserLocationReasonRow(
    text: String,
    emoji: String,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = emoji, style = MaterialTheme.typography.titleLarge)
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
fun LocationMapPlaceholder(
    fetching: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("📍", style = MaterialTheme.typography.displaySmall)
            if (fetching) {
                Text(
                    text = stringResource(Res.string.location_fetching),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
fun OrderAccountTile(
    order: Order,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(Res.string.order_number_label, order.displayNumber),
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = userOrderStatusLabel(order.status),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            order.quote?.deliveryDistanceKm?.let { km ->
                Text(
                    text = stringResource(Res.string.shop_distance, (km * 10).roundToInt() / 10.0),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(order.totalValue.asMoney(), fontWeight = FontWeight.SemiBold)
            Text("›", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Orders-list row for a checkout that is not an order yet (payment pending, not received, failed or refunded). */
@Composable
fun PaymentAccountTile(
    intent: PaymentIntent,
    busy: Boolean,
    onClick: () -> Unit,
    onCheckStatus: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val nowMs by produceState(Clock.System.now().toEpochMilliseconds(), intent.id, intent.status) {
        while (intent.status == PaymentIntentStatus.PENDING) {
            value = Clock.System.now().toEpochMilliseconds()
            delay(AppConstants.Payment.COUNTDOWN_TICK_MS)
        }
    }
    val secondsLeft = ((intent.expiresAtEpochMs - nowMs) / 1000).coerceAtLeast(0)
    val waiting = intent.status == PaymentIntentStatus.PENDING && secondsLeft > 0
    val (label, color) = when {
        waiting -> stringResource(Res.string.pay_status_pending) to MaterialTheme.colorScheme.tertiary
        intent.status == PaymentIntentStatus.PENDING -> stringResource(Res.string.pay_status_not_received) to MaterialTheme.colorScheme.error
        intent.status == PaymentIntentStatus.REFUNDED -> stringResource(Res.string.pay_status_refunded) to MaterialTheme.colorScheme.onSurfaceVariant
        else -> stringResource(Res.string.pay_status_failed) to MaterialTheme.colorScheme.error
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(intent.shopName, fontWeight = FontWeight.SemiBold)
                Text(label, style = MaterialTheme.typography.bodySmall, color = color, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(Res.string.pay_detail_txn, intent.id),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (waiting) {
                    Text(
                        stringResource(Res.string.pay_time_left, countdown(secondsLeft)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(intent.amountInr.asMoney(), fontWeight = FontWeight.SemiBold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            if (waiting) {
                OutlinedButton(onClick = onCheckStatus, enabled = !busy) { Text(stringResource(Res.string.pay_check_status)) }
            } else {
                Button(onClick = onRetry, enabled = !busy) { Text(stringResource(Res.string.pay_retry)) }
            }
            if (busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
fun userOrderStatusLabel(status: OrderStatus): String = stringResource(
    when (status) {
        OrderStatus.DRAFT -> Res.string.status_draft
        OrderStatus.PAYMENT_PENDING -> Res.string.status_payment_pending
        OrderStatus.RECEIVED -> Res.string.status_pending_acceptance
        OrderStatus.ACCEPTED -> Res.string.status_accepted
        OrderStatus.PACKING, OrderStatus.LOOKING_FOR_PARTNER -> Res.string.status_packing
        OrderStatus.PARTNER_ACCEPTED -> Res.string.status_partner_accepted
        OrderStatus.ON_THE_WAY -> Res.string.status_on_the_way
        OrderStatus.DELIVERED, OrderStatus.COMPLETED -> Res.string.status_delivered
        OrderStatus.REJECTED -> Res.string.status_rejected
        OrderStatus.CANCELLED -> Res.string.status_cancelled
    },
)
