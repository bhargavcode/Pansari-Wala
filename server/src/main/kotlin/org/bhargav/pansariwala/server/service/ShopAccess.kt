package org.bhargav.pansariwala.server.service

import org.bhargav.pansariwala.server.db.OfferDoc
import org.bhargav.pansariwala.server.db.ShopUserDoc

internal const val MAX_RATING_REPLY = 500

/** Keep in sync with the client `ShopPermission` enum. */
object ShopPerm {
    const val ADMIN = "ADMIN"
    const val BILLING = "BILLING"
    const val ONLINE_ORDERS = "ONLINE_ORDERS"
    const val INVENTORY = "INVENTORY"
    const val VIEW_REPORTS = "VIEW_REPORTS"
    const val PRINT_REPORTS = "PRINT_REPORTS"
    const val TOGGLE_FEATURES = "TOGGLE_FEATURES"
    const val SETTINGS = "SETTINGS"
    const val SHOP_PROFILE = "SHOP_PROFILE"
    const val MANAGE_STAFF = "MANAGE_STAFF"
    const val OFFERS = "OFFERS"
    const val RATINGS = "RATINGS"
    const val VISITORS = "VISITORS"

    val ALL: List<String> = listOf(
        ADMIN, BILLING, ONLINE_ORDERS, INVENTORY, VIEW_REPORTS, PRINT_REPORTS, TOGGLE_FEATURES,
        SETTINGS, SHOP_PROFILE, MANAGE_STAFF, OFFERS, RATINGS, VISITORS,
    )
}

object ShopRole {
    const val ADMIN = "SHOP"
    const val EXECUTIVE = "EXECUTIVE"
}

object OfferScope {
    const val BILL = "BILL"
    const val PRODUCT = "PRODUCT"
}

object OfferType {
    const val PERCENT = "PERCENT"
    const val FLAT = "FLAT"
}

/** Shop admins (legacy `SHOP` role) and executives granted [ShopPerm.ADMIN] get every permission. */
internal fun effectivePermissions(user: ShopUserDoc): List<String> =
    if (user.role != ShopRole.EXECUTIVE || ShopPerm.ADMIN in user.permissions) ShopPerm.ALL
    else user.permissions.filter { it in ShopPerm.ALL }

internal fun OfferDoc.isLive(now: Long): Boolean =
    active && (startsAt == null || now >= startsAt) && (endsAt == null || now <= endsAt)

private fun OfferDoc.amountOn(base: Double): Double = when (discountType) {
    OfferType.FLAT -> value.coerceAtMost(base)
    else -> base * value / 100.0
}.coerceAtLeast(0.0)

/** Unit price after a PRODUCT offer: percent off, or flat ₹ off each unit (never below 0). */
internal fun OfferDoc.unitPriceOn(unitPrice: Double): Double = when (discountType) {
    OfferType.FLAT -> unitPrice - value
    else -> unitPrice * (1.0 - value / 100.0)
}.coerceIn(0.0, unitPrice)

/** Lowest-price live PRODUCT offer for [productId] at [unitPrice], if any. */
internal fun List<OfferDoc>.bestProductOffer(productId: String, unitPrice: Double): OfferDoc? =
    filter { it.scope == OfferScope.PRODUCT && productId in it.productIds }
        .minByOrNull { it.unitPriceOn(unitPrice) }
        ?.takeIf { it.unitPriceOn(unitPrice) < unitPrice }

internal data class PricedLine(val productId: String, val unitPrice: Double, val quantity: Double) {
    val lineTotal: Double get() = unitPrice * quantity
}

/**
 * PRODUCT offers lower each unit price (cart shows the sale price). The best of (shop-wide %, best
 * eligible BILL offer) then applies to the amount after product offers; BILL eligibility uses that
 * same amount. Offers never stack within the same scope.
 */
internal fun offerDiscount(
    offers: List<OfferDoc>,
    lines: List<PricedLine>,
    shopDiscountPercent: Double,
): Double {
    val subtotal = lines.sumOf { it.lineTotal }
    val lineDiscount = lines.sumOf { line ->
        val offer = offers.bestProductOffer(line.productId, line.unitPrice) ?: return@sumOf 0.0
        (line.unitPrice - offer.unitPriceOn(line.unitPrice)) * line.quantity
    }
    val afterLines = (subtotal - lineDiscount).coerceAtLeast(0.0)
    val billDiscount = offers
        .filter { it.scope == OfferScope.BILL && afterLines >= it.minBillAmount }
        .maxOfOrNull { it.amountOn(afterLines) } ?: 0.0
    val shopWide = afterLines * shopDiscountPercent / 100.0
    return (lineDiscount + maxOf(billDiscount, shopWide)).coerceAtMost(subtotal)
}
