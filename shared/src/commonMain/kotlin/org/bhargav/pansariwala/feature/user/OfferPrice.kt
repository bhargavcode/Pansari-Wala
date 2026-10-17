package org.bhargav.pansariwala.feature.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.domain.model.DiscountType
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.feature.order.PackedGreen
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.offer_off_label
import kotlin.math.roundToInt

/** Whole-percent saving of [sale] against [was], or null when there is no real saving. */
fun discountPercent(was: Double?, sale: Double): Int? {
    if (was == null || was <= 0.0 || sale >= was) return null
    return ((was - sale) / was * 100.0).roundToInt().takeIf { it > 0 }
}

/**
 * Sale price in bold, the struck-through "was" price, and a green "20% OFF" tag — the standard
 * e-commerce pattern. Renders just the price when there is no saving.
 */
@Composable
fun OfferPriceRow(
    price: String,
    strikePrice: String?,
    offLabel: String?,
    modifier: Modifier = Modifier,
    priceStyle: TextStyle = MaterialTheme.typography.bodyMedium,
    priceColor: Color = MaterialTheme.colorScheme.primary,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(price, style = priceStyle, fontWeight = FontWeight.Bold, color = priceColor)
        if (strikePrice != null) {
            Text(
                strikePrice,
                style = MaterialTheme.typography.labelSmall,
                textDecoration = TextDecoration.LineThrough,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (offLabel != null) OfferTag(offLabel)
    }
}

/** Green "20% OFF" pill. */
@Composable
fun OfferTag(label: String, modifier: Modifier = Modifier) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = PackedGreen,
        modifier = modifier
            .background(PackedGreen.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}

/** "20% OFF" from a was/sale pair. */
@Composable
fun offLabel(was: Double?, sale: Double): String? =
    discountPercent(was, sale)?.let { stringResource(Res.string.offer_off_label, "$it%") }

/** Offer tag for a product with options: "20% OFF" or "₹10.00 OFF". */
@Composable
fun Product.offerTag(): String? {
    val offer = offer?.takeIf { hasOffer } ?: return null
    val amount = when (offer.type) {
        DiscountType.PERCENT -> "${offer.value.roundToInt()}%"
        DiscountType.FLAT -> offer.value.asMoney()
    }
    return stringResource(Res.string.offer_off_label, amount)
}

/** Unit price of one purchasable option, with MRP / pre-offer price struck through. */
@Composable
fun PurchaseOption.OfferPrice(
    modifier: Modifier = Modifier,
    priceStyle: TextStyle = MaterialTheme.typography.bodyMedium,
) {
    val was = strikePrice
    OfferPriceRow(
        price = priceLabel(),
        strikePrice = was?.asMoney(),
        offLabel = offLabel(was, unitPrice),
        modifier = modifier,
        priceStyle = priceStyle,
    )
}

/** Line total at sale price, with the pre-offer total struck through. */
@Composable
fun LineOfferPrice(
    lineTotal: Double,
    baseLineTotal: Double,
    modifier: Modifier = Modifier,
) {
    val was = baseLineTotal.takeIf { it > lineTotal + 0.005 }
    OfferPriceRow(
        price = lineTotal.asMoney(),
        strikePrice = was?.asMoney(),
        offLabel = offLabel(was, lineTotal),
        modifier = modifier,
        priceStyle = MaterialTheme.typography.bodySmall,
    )
}
