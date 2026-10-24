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
    OrderStatus.DRAFT, OrderStatus.PAYMENT_PENDING, OrderStatus.RECEIVED -> FulfillmentStep.PLACED
    OrderStatus.ACCEPTED -> FulfillmentStep.ACCEPTED
    OrderStatus.PACKING, OrderStatus.LOOKING_FOR_PARTNER, OrderStatus.PARTNER_ACCEPTED ->
        FulfillmentStep.PACKING
    OrderStatus.ON_THE_WAY, OrderStatus.RETURNING_TO_SHOP, OrderStatus.RETURNED_TO_SHOP -> FulfillmentStep.ON_THE_WAY
    OrderStatus.DELIVERED, OrderStatus.COMPLETED -> FulfillmentStep.DELIVERED
    OrderStatus.REJECTED, OrderStatus.CANCELLED -> FulfillmentStep.PLACED
}

@kotlinx.serialization.Serializable
data class GeoPoint(
    val lat: Double,
    val lng: Double,
)

/** Ids must match server `defaultShopTypes()` (server/db/Mongo.kt). */
enum class ShopType(val apiValue: String) {
    GROCERY("GROCERY"),
    SUPERMARKET("SUPERMARKET"),
    GENERAL_STORE("GENERAL_STORE"),
    FRUITS_VEGETABLES("FRUITS_VEGETABLES"),
    DAIRY("DAIRY"),
    BAKERY("BAKERY"),
    MEAT_FISH("MEAT_FISH"),
    SWEET_SHOP("SWEET_SHOP"),
    RESTAURANT("RESTAURANT"),
    MEDICAL_STORE("MEDICAL_STORE"),
    AYURVEDIC("AYURVEDIC"),
    ORGANIC("ORGANIC"),
    HARDWARE("HARDWARE"),
    PLUMBING("PLUMBING"),
    ELECTRICAL("ELECTRICAL"),
    ELECTRONICS("ELECTRONICS"),
    MOBILE("MOBILE"),
    STATIONERY("STATIONERY"),
    BOOKS("BOOKS"),
    CLOTHING("CLOTHING"),
    BOUTIQUE("BOUTIQUE"),
    FOOTWEAR("FOOTWEAR"),
    COSMETICS("COSMETICS"),
    JEWELLERY("JEWELLERY"),
    HOME_KITCHEN("HOME_KITCHEN"),
    FURNITURE("FURNITURE"),
    TOYS_GIFTS("TOYS_GIFTS"),
    SPORTS("SPORTS"),
    PET_SUPPLIES("PET_SUPPLIES"),
    FLOWERS("FLOWERS"),
    PAAN_SHOP("PAAN_SHOP"),
    GAS_WATER("GAS_WATER"),
    AUTO_PARTS("AUTO_PARTS"),
    OTHER("OTHER"),
    ;

    companion object {
        fun fromApi(value: String): ShopType =
            entries.firstOrNull { it.apiValue.equals(value.trim(), ignoreCase = true) } ?: OTHER
    }
}

enum class ShopSortOption {
    DISTANCE_ASC,
    DISTANCE_DESC,
    RATING_DESC,
    NAME_ASC,
    NAME_DESC,
}

/** App-wide shop discovery filter; empty [shopTypes] means every type. */
data class ShopFilter(
    val radiusKm: Double,
    val shopTypes: Set<ShopType> = emptySet(),
    val sort: ShopSortOption = ShopSortOption.DISTANCE_ASC,
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
    val shopType: ShopType = ShopType.GENERAL_STORE,
    val offerCount: Int = 0,
    /** Titles of the biggest live offers, largest first. */
    val topOffers: List<String> = emptyList(),
    val discountPercent: Double = 0.0,
    val deliveryRadiusKm: Double = 20.0,
    val deliversToYou: Boolean = true,
    val address: String = "",
    /** "HH:mm" daily delivery cutoff; blank = delivers all day. */
    val deliveryUntil: String = "",
    /** False past [deliveryUntil]: orders become self-pickup with no delivery charge. */
    val deliveryAvailable: Boolean = true,
    /** Checkout stays disabled until the cart subtotal reaches this; 0 = no minimum. */
    val minOrderValueInr: Double = 0.0,
    /** Delivery is free at or above this subtotal; 0 = shop offers no free delivery. */
    val freeDeliveryAboveInr: Double = 0.0,
)

/** Admin-defined master category; [id] is [AppConstants.UNCATEGORIZED_CATEGORY_ID] for products without one. */
data class CatalogCategory(val id: String, val name: String) {
    val isUncategorized: Boolean
        get() = id == org.bhargav.pansariwala.util.AppConstants.UNCATEGORIZED_CATEGORY_ID || name.isBlank()
}

data class CatalogSection(
    val category: CatalogCategory,
    val totalCount: Int,
    val preview: List<Product>,
)

data class ProductPage(
    val items: List<Product>,
    val page: Int,
    val totalCount: Int,
    val hasMore: Boolean,
)

data class ProductDetail(
    val product: Product,
    val brandName: String = "",
    val companyName: String = "",
    val description: String = "",
    val sku: String = "",
    val tags: List<String> = emptyList(),
    val weightKg: Double = 0.0,
    val dimensions: String = "",
    val priceHistory: List<PricePoint> = emptyList(),
    val categoryName: String = "",
)

data class PricePoint(val atEpochMs: Long, val price: Double)

data class ShopReview(
    val id: String,
    val customerName: String,
    val stars: Int,
    val comment: String?,
    val createdAtEpochMs: Long,
    val reply: String? = null,
    val replyAtEpochMs: Long? = null,
)

data class ShopOffer(
    val id: String,
    val title: String,
    val description: String,
    val discountPercent: Double,
    val type: DiscountType = DiscountType.PERCENT,
    val value: Double = discountPercent,
    val minBillAmount: Double = 0.0,
    val endsAtEpochMs: Long? = null,
    /** Products a product-scope offer applies to; empty for whole-bill offers. */
    val products: List<Product> = emptyList(),
)

@kotlinx.serialization.Serializable
data class SavedAddress(
    val id: String,
    val line: String,
    val locality: String,
    val location: GeoPoint,
    val isDefault: Boolean,
)

@kotlinx.serialization.Serializable
data class CustomerProfile(
    val id: String,
    val phone: String,
    val name: String,
    val address: String,
    val locality: String = "",
    val location: GeoPoint?,
    val addresses: List<SavedAddress> = emptyList(),
    val imageUrl: String? = null,
    /** Set while the customer's account deletion is pending. */
    val deletionScheduledAtEpochMs: Long? = null,
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
    val isRefund: Boolean = false,
    val shopId: String? = null,
    val shopName: String? = null,
    val orderStatus: OrderStatus? = null,
    val paymentMethod: String? = null,
    /** Gateway payment id or UPI UTR. */
    val paymentRef: String? = null,
    val refundId: String? = null,
    val itemCount: Int = 0,
    val selfPickup: Boolean = false,
)
