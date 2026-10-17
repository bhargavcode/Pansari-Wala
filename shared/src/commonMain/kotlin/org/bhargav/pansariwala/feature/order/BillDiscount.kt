package org.bhargav.pansariwala.feature.order

import org.bhargav.pansariwala.api.OfferDiscountType
import org.bhargav.pansariwala.api.OfferScope
import org.bhargav.pansariwala.api.ShopOfferDto
import org.bhargav.pansariwala.domain.model.DiscountType
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductOffer

data class BillTotals(
    /** Item total at shop price, before product offers. */
    val subtotal: Double = 0.0,
    /** Savings from product offers; already reflected in each line's unit price. */
    val productDiscount: Double = 0.0,
    /** Invoice (BILL) offer on the amount after product offers. */
    val offerDiscount: Double = 0.0,
    val manualDiscount: Double = 0.0,
) {
    /** Sum of line totals at sale price (what the item rows show). */
    val itemsTotal: Double get() = (subtotal - productDiscount).coerceAtLeast(0.0)
    /** Order-level discount stored on the sale (line prices already carry product offers). */
    val discount: Double get() = offerDiscount + manualDiscount
    val total: Double get() = (itemsTotal - discount).coerceAtLeast(0.0)
}

private fun ShopOfferDto.amountOn(base: Double): Double = when (discountType) {
    OfferDiscountType.FLAT -> value.coerceAtMost(base)
    else -> base * value / 100.0
}.coerceAtLeast(0.0)

private fun ShopOfferDto.toProductOffer() = ProductOffer(
    id = id,
    title = title,
    type = if (discountType == OfferDiscountType.FLAT) DiscountType.FLAT else DiscountType.PERCENT,
    value = value,
)

/** Same pick as the server: the live PRODUCT offer giving the lowest unit price. */
fun List<ShopOfferDto>.bestProductOffer(product: Product): ProductOffer? =
    filter { it.active && it.scope == OfferScope.PRODUCT && product.id in it.productIds }
        .map { it.toProductOffer() }
        .minByOrNull { it.unitPriceOn(product.sellingPrice) }
        ?.takeIf { it.unitPriceOn(product.sellingPrice) < product.sellingPrice }

/** Attaches each product's best offer so cart lines are priced at the sale price. */
fun List<Product>.withOffers(offers: List<ShopOfferDto>): List<Product> =
    if (offers.isEmpty()) this else map { it.copy(offer = offers.bestProductOffer(it)) }

/**
 * Same rules as the server quote: product offers lower each line's unit price, then the best eligible
 * BILL offer applies to the amount after product offers (eligibility uses that amount too). The manual
 * discount (flat ₹ or % of the remainder) is applied last.
 */
fun computeBill(
    lines: List<CartLine>,
    offers: List<ShopOfferDto>,
    manualInput: String,
    manualIsPercent: Boolean,
): BillTotals {
    val subtotal = lines.sumOf { it.baseLineTotal }
    val itemsTotal = lines.sumOf { it.lineTotal }
    val productDiscount = (subtotal - itemsTotal).coerceAtLeast(0.0)
    val billDiscount = offers
        .filter { it.active && it.scope == OfferScope.BILL && itemsTotal >= it.minBillAmount }
        .maxOfOrNull { it.amountOn(itemsTotal) } ?: 0.0
    val remaining = itemsTotal - billDiscount
    val raw = manualInput.trim().toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val manual = (if (manualIsPercent) remaining * raw.coerceAtMost(100.0) / 100.0 else raw).coerceAtMost(remaining)
    return BillTotals(subtotal, productDiscount, billDiscount, manual)
}
