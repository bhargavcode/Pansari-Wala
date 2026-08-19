package org.bhargav.pansariwala.domain.model

enum class OrderChannel {
    POS,
    ONLINE,
}

enum class FulfillmentStep {
    PLACED,
    ACCEPTED,
    PACKING,
    ON_THE_WAY,
    DELIVERED,
}

fun OrderStatus.toFulfillmentStep(): FulfillmentStep = when (this) {
    OrderStatus.DRAFT, OrderStatus.RECEIVED -> FulfillmentStep.PLACED
    OrderStatus.ACCEPTED -> FulfillmentStep.ACCEPTED
    OrderStatus.PACKING, OrderStatus.LOOKING_FOR_PARTNER, OrderStatus.PARTNER_ACCEPTED ->
        FulfillmentStep.PACKING
    OrderStatus.ON_THE_WAY -> FulfillmentStep.ON_THE_WAY
    OrderStatus.DELIVERED, OrderStatus.COMPLETED -> FulfillmentStep.DELIVERED
    OrderStatus.REJECTED, OrderStatus.CANCELLED -> FulfillmentStep.PLACED
}

data class GeoPoint(
    val lat: Double,
    val lng: Double,
)

data class MarketplaceShop(
    val id: String,
    val name: String,
    val imageUrl: String?,
    val rating: Double,
    val ratingCount: Int,
    val distanceKm: Double,
    val isOpen: Boolean,
    val location: GeoPoint,
    val offerCount: Int = 0,
    val discountPercent: Double = 0.0,
)

data class ShopOffer(
    val id: String,
    val title: String,
    val description: String,
    val discountPercent: Double,
)

data class CustomerProfile(
    val id: String,
    val phone: String,
    val name: String,
    val address: String,
    val location: GeoPoint?,
)

data class CheckoutQuote(
    val itemsSubtotal: Double,
    val discount: Double,
    val platformFee: Double,
    val deliveryCharge: Double,
    val payable: Double,
    val deliveryDistanceKm: Double,
)

data class OrderRating(
    val stars: Int,
    val comment: String?,
    val updatedAtEpochMs: Long,
)

data class MoneyTxn(
    val id: String,
    val orderId: String,
    val amount: Double,
    val title: String,
    val createdAtEpochMs: Long,
)
