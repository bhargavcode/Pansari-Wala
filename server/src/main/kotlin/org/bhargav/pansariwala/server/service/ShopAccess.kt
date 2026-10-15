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

/**
 * Best PRODUCT offer per line + the best of (shop-wide %, best eligible BILL offer) on the
 * remaining subtotal. Offers never stack within the same scope.
 */
internal fun offerDiscount(
    offers: List<OfferDoc>,
    lines: List<Pair<String, Double>>,
    subtotal: Double,
    shopDiscountPercent: Double,
): Double {
    val productOffers = offers.filter { it.scope == OfferScope.PRODUCT }
    val lineDiscount = lines.sumOf { (productId, lineTotal) ->
        productOffers.filter { productId in it.productIds }.maxOfOrNull { it.amountOn(lineTotal) } ?: 0.0
    }
    val afterLines = (subtotal - lineDiscount).coerceAtLeast(0.0)
    val billDiscount = offers
        .filter { it.scope == OfferScope.BILL && subtotal >= it.minBillAmount }
        .maxOfOrNull { it.amountOn(afterLines) } ?: 0.0
    val shopWide = afterLines * shopDiscountPercent / 100.0
    return (lineDiscount + maxOf(billDiscount, shopWide)).coerceAtMost(subtotal)
}
