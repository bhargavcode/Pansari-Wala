package org.bhargav.pansariwala.feature.order

import org.bhargav.pansariwala.api.OfferDiscountType
import org.bhargav.pansariwala.api.OfferScope
import org.bhargav.pansariwala.api.ShopOfferDto

data class BillTotals(
    val subtotal: Double = 0.0,
    val offerDiscount: Double = 0.0,
    val manualDiscount: Double = 0.0,
) {
    val discount: Double get() = offerDiscount + manualDiscount
    val total: Double get() = (subtotal - discount).coerceAtLeast(0.0)
}

private fun ShopOfferDto.amountOn(base: Double): Double = when (discountType) {
    OfferDiscountType.FLAT -> value.coerceAtMost(base)
    else -> base * value / 100.0
}.coerceAtLeast(0.0)

/**
 * Same rules as the server quote: best PRODUCT offer per line, then the best eligible BILL offer on
 * the remainder. The manual discount (flat ₹ or % of the remainder) is applied last.
 */
fun computeBill(
    lines: List<CartLine>,
    offers: List<ShopOfferDto>,
    manualInput: String,
    manualIsPercent: Boolean,
): BillTotals {
    val subtotal = lines.sumOf { it.lineTotal }
    val productOffers = offers.filter { it.active && it.scope == OfferScope.PRODUCT }
    val lineDiscount = lines.sumOf { line ->
        productOffers.filter { line.product.id in it.productIds }.maxOfOrNull { it.amountOn(line.lineTotal) } ?: 0.0
    }
    val afterLines = (subtotal - lineDiscount).coerceAtLeast(0.0)
    val billDiscount = offers
        .filter { it.active && it.scope == OfferScope.BILL && subtotal >= it.minBillAmount }
        .maxOfOrNull { it.amountOn(afterLines) } ?: 0.0
    val offerDiscount = (lineDiscount + billDiscount).coerceAtMost(subtotal)
    val remaining = subtotal - offerDiscount
    val raw = manualInput.trim().toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val manual = (if (manualIsPercent) remaining * raw.coerceAtMost(100.0) / 100.0 else raw).coerceAtMost(remaining)
    return BillTotals(subtotal, offerDiscount, manual)
}
