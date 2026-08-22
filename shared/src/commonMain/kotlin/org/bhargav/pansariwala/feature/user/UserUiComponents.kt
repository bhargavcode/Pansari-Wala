package org.bhargav.pansariwala.feature.user

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.domain.model.MarketplaceShop
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
import pansariwala.shared.generated.resources.location_fetching
import pansariwala.shared.generated.resources.order_number_label
import pansariwala.shared.generated.resources.shop_distance
import pansariwala.shared.generated.resources.shop_open_now
import pansariwala.shared.generated.resources.shop_rating
import pansariwala.shared.generated.resources.shop_closed
import pansariwala.shared.generated.resources.status_accepted
import pansariwala.shared.generated.resources.status_cancelled
import pansariwala.shared.generated.resources.status_delivered
import pansariwala.shared.generated.resources.status_draft
import pansariwala.shared.generated.resources.status_on_the_way
import pansariwala.shared.generated.resources.status_packing
import pansariwala.shared.generated.resources.status_partner_accepted
import pansariwala.shared.generated.resources.status_pending_acceptance
import pansariwala.shared.generated.resources.status_received
import pansariwala.shared.generated.resources.status_rejected
import pansariwala.shared.generated.resources.user_brand_title
import pansariwala.shared.generated.resources.user_cart_items
import kotlin.math.roundToInt

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
) {
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.card),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = shop.name.take(2).uppercase(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Column(
                modifier = Modifier.padding(start = 12.dp).weight(1f),
            ) {
                Text(
                    text = shop.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = stringResource(Res.string.shop_rating, shop.rating.toString(), shop.ratingCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(
                            Res.string.shop_distance,
                            shop.distanceKm.let { (it * 10).roundToInt() / 10.0 }.toString(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Surface(
                        color = if (shop.isOpen) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                        shape = RoundedCornerShape(8.dp),
                        shadowElevation = PansariElevation.chip,
                    ) {
                        Text(
                            text = stringResource(
                                if (shop.isOpen) Res.string.shop_open_now else Res.string.shop_closed,
                            ),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ContinueLastOrderCard(
    shopName: String,
    lines: List<CartStore.Line>,
    subtotal: Double,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (lines.isEmpty()) return
    Card(
        modifier = modifier.fillMaxWidth().clickable(onClick = onContinue),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = PansariElevation.raisedCard),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(Res.string.home_continue_last_order),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(shopName, fontWeight = FontWeight.SemiBold)
            lines.forEach { line ->
                Text(
                    "${line.product.name} × ${line.quantity.asQuantity()} · ${line.product.sellingPrice.asMoney()}",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(Res.string.checkout_payable), fontWeight = FontWeight.Medium)
                Text(subtotal.asMoney(), fontWeight = FontWeight.Bold)
            }
            UserPrimaryButton(
                text = stringResource(Res.string.action_continue),
                onClick = onContinue,
            )
        }
    }
}

@Composable
fun CartFloatingPill(
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (itemCount <= 0) return
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.primary,
        shadowElevation = PansariElevation.fab,
    ) {
        Text(
            text = stringResource(Res.string.user_cart_items, itemCount),
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            color = MaterialTheme.colorScheme.onPrimary,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun QuantityStepper(
    quantity: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
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
        IconButton(onClick = onDecrement, enabled = quantity > 0, modifier = Modifier.size(36.dp)) {
            Text("-", fontWeight = FontWeight.Bold)
        }
        Text(
            text = quantity.toString(),
            modifier = Modifier.width(28.dp),
            fontWeight = FontWeight.SemiBold,
        )
        IconButton(onClick = onIncrement, modifier = Modifier.size(36.dp)) {
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
                text = stringResource(Res.string.order_number_label, order.id.takeLast(6)),
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

@Composable
fun userOrderStatusLabel(status: OrderStatus): String = stringResource(
    when (status) {
        OrderStatus.DRAFT -> Res.string.status_draft
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
