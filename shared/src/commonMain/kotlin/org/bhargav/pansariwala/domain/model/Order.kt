package org.bhargav.pansariwala.domain.model

enum class OrderStatus {
    DRAFT,
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
    ;

    companion object {
        fun fromName(value: String): OrderStatus =
            entries.firstOrNull { it.name == value } ?: COMPLETED
    }
}

data class OrderItem(
    val productId: String,
    val productName: String,
    val unit: ProductUnit,
    val quantity: Double,
    val unitPrice: Double,
    val imageUrl: String? = null,
) {
    val lineTotal: Double get() = quantity * unitPrice
}

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
    val partnerId: String? = null,
    val partnerName: String? = null,
    val partnerPhone: String? = null,
    val partnerVehicleReg: String? = null,
    val rating: OrderRating? = null,
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
) {
    val totalValue: Double get() = quote?.payable ?: items.sumOf { it.lineTotal }
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
}
