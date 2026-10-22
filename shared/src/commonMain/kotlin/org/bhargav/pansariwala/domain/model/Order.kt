package org.bhargav.pansariwala.domain.model

import org.bhargav.pansariwala.media.isHttpImageUrl
import org.bhargav.pansariwala.util.AppConstants

enum class OrderStatus {
    DRAFT,
    /** Legacy: orders created before payment; new checkouts use [PaymentIntent] instead. */
    PAYMENT_PENDING,
    RECEIVED,
    ACCEPTED,
    PACKING,
    LOOKING_FOR_PARTNER,
    PARTNER_ACCEPTED,
    ON_THE_WAY,
    DELIVERED,
    COMPLETED,
    REJECTED,
    CANCELLED,
    /** Partner could not reach the customer and is taking the parcel back. */
    RETURNING_TO_SHOP,
    /** Shop verified the returned parcel; the customer collects it at the shop. */
    RETURNED_TO_SHOP,
    ;

    companion object {
        fun fromName(value: String): OrderStatus =
            entries.firstOrNull { it.name == value } ?: COMPLETED
    }
}

/** Customer-facing order number, see [Order.displayNumber]. */
fun publicOrderNumber(shopId: String, orderId: String): String {
    val shopPart = shopId.filter { it.isDigit() }.ifBlank { shopId.substringAfterLast('_', shopId) }
    val raw = orderId.substringAfterLast('_').ifBlank { orderId }
    val orderPart = raw.filter { it.isLetterOrDigit() }
        .takeLast(AppConstants.PUBLIC_ORDER_CODE_LENGTH)
        .uppercase()
    return "$shopPart$orderPart"
}

data class OrderItem(
    val productId: String,
    val productName: String,
    val unit: ProductUnit,
    val quantity: Double,
    val unitPrice: Double,
    val imageUrl: String? = null,
    val variantId: String? = null,
    val variantLabel: String? = null,
) {
    val lineTotal: Double get() = quantity * unitPrice
}

/** One sold line with its order time, for product / category sales reports. */
data class ItemSale(
    val orderId: String,
    val productId: String,
    val productName: String,
    val variantLabel: String?,
    val quantity: Double,
    val lineTotal: Double,
    val createdAtEpochMs: Long,
    val channel: OrderChannel,
)

data class OrderSummary(
    val id: String,
    val createdAtEpochMs: Long,
    val itemCount: Int,
    val totalValue: Double,
    val status: OrderStatus,
    val customerName: String?,
)

data class Order(
    val id: String,
    val shopId: String,
    val createdAtEpochMs: Long,
    val status: OrderStatus,
    val customerName: String?,
    val items: List<OrderItem>,
    val cancelReason: String? = null,
    val channel: OrderChannel = OrderChannel.POS,
    val customerId: String? = null,
    val deliveryAddress: String? = null,
    val dropoffInstructions: String? = null,
    val deliveryOtp: String? = null,
    val pickupPhotoUrls: List<String> = emptyList(),
    /** Customer collects at the shop; no delivery partner involved. */
    val selfPickup: Boolean = false,
    val partnerId: String? = null,
    val partnerName: String? = null,
    val partnerPhone: String? = null,
    val partnerVehicleReg: String? = null,
    val rating: OrderRating? = null,
    /** Delivery partner's rating of the customer for this order (shop / partner views only). */
    val customerRatingByPartner: OrderRating? = null,
    /** Customer's average partner rating across all orders; null when never rated. */
    val customerRatingAvg: Double? = null,
    val customerRatingCount: Int = 0,
    val quote: CheckoutQuote? = null,
    val paymentId: String? = null,
    val paymentMethod: String = "ONLINE",
    val customerPhone: String? = null,
    val shopName: String? = null,
    val shopAddress: String? = null,
    val shopLat: Double? = null,
    val shopLng: Double? = null,
    val customerLat: Double? = null,
    val customerLng: Double? = null,
    val totalDistanceKm: Double? = null,
    val deliveryDurationMin: Int? = null,
    val partnerPayoutInr: Double? = null,
    val partnerProgress: String = "",
    /** Items the shop checked into the delivery packet; empty means all items (legacy orders). */
    val packedProductIds: Set<String> = emptySet(),
    /** Shop delivers this order itself instead of a platform partner. */
    val isSelfDelivery: Boolean = false,
    /** POS bill discount (offers + manual), in INR; online orders carry theirs in [quote]. */
    val discount: Double = 0.0,
    /** Payment transaction (PTX…) this order was created from, and its UPI reference. */
    val paymentIntentId: String? = null,
    val paymentUtr: String? = null,
) {
    val subtotal: Double get() = items.sumOf { it.lineTotal }
    val deliveringItems: List<OrderItem>
        get() = if (packedProductIds.isEmpty()) items else items.filter { it.productId in packedProductIds }
    val isPackedForDelivery: Boolean
        get() = status == OrderStatus.LOOKING_FOR_PARTNER || status == OrderStatus.PARTNER_ACCEPTED ||
            status == OrderStatus.ON_THE_WAY || status == OrderStatus.DELIVERED ||
            status == OrderStatus.COMPLETED
    val totalValue: Double get() = quote?.payable ?: (subtotal - discount).coerceAtLeast(0.0)
    val itemCount: Int get() = items.size
    val hasAssignedPartner: Boolean
        get() = !partnerName.isNullOrBlank() || !partnerPhone.isNullOrBlank()
    val canCancel: Boolean
        get() = status != OrderStatus.DELIVERED &&
            status != OrderStatus.COMPLETED &&
            status != OrderStatus.CANCELLED &&
            status != OrderStatus.REJECTED
    val isActiveDelivery: Boolean
        get() = status == OrderStatus.PARTNER_ACCEPTED || status == OrderStatus.ON_THE_WAY
    /** Shop digits + 8-char order code, e.g. shop_12 + 24B112A4 → 1224B112A4. */
    val displayNumber: String
        get() = publicOrderNumber(shopId, id)
    val visiblePickupPhotos: List<String>
        get() = pickupPhotoUrls.filter(::isHttpImageUrl)
    val resumeProgress: String
        get() = partnerProgress.ifBlank {
            if (status == OrderStatus.ON_THE_WAY) AppConstants.PartnerProgress.TO_CUSTOMER
            else AppConstants.PartnerProgress.TO_STORE
        }
}
