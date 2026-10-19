package org.bhargav.pansariwala.api

import kotlinx.serialization.Serializable

@Serializable
data class ShopFeaturesDto(
    val voiceSearch: Boolean = true,
    val barcodeSearch: Boolean = true,
    val reportGeneration: Boolean = true,
    val onlineOrders: Boolean = true,
    val inventoryAlerts: Boolean = true,
    val barcodeScanner: Boolean = false,
)

@Serializable
data class ShopHoursDayDto(
    val day: String,
    val start: String = "09:00",
    val end: String = "21:00",
    val closed: Boolean = false,
)

@Serializable
data class ShopDetailsDto(
    val id: String,
    val name: String,
    val imageUrl: String? = null,
    val rating: Double = 0.0,
    val ratingCount: Int = 0,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val isOpen: Boolean = true,
    val active: Boolean = true,
    val paymentsEnabled: Boolean = true,
    val discountPercent: Double = 0.0,
    val upiId: String = "",
    val address: String = "",
    val deliveryRadiusKm: Double = 20.0,
    val shopType: String = "GENERAL_STORE",
    val joinedAtEpochMs: Long = 0L,
    val features: ShopFeaturesDto = ShopFeaturesDto(),
    val ownerName: String = "",
    val ownerPhone: String = "",
    val ownerEmail: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = "",
    val country: String = "India",
    val registrationNumber: String = "",
    val taxId: String = "",
    val operatingHours: List<ShopHoursDayDto> = emptyList(),
    val loginUsername: String = "",
    val deliveryUntil: String = "",
    val homeDeliveryEnabled: Boolean = true,
)

@Serializable
data class ShopStaffDto(
    val id: String = "",
    val username: String = "",
    val displayName: String = "",
    val role: String = "",
    val permissions: List<String> = emptyList(),
    val active: Boolean = true,
)

@Serializable
data class ShopStaffUpsert(
    val username: String = "",
    val displayName: String = "",
    val password: String = "",
    val permissions: List<String> = emptyList(),
    val active: Boolean = true,
)

@Serializable
data class ShopProfileDto(
    val shop: ShopDetailsDto,
    val me: ShopStaffDto,
)

/** Customer-catalog category order. [sort]: [CategorySort] name. */
@Serializable
data class ShopCategoryOrderDto(
    val sort: String = CategorySort.PRIORITY.name,
    val items: List<ShopCategoryOrderItemDto> = emptyList(),
)

/** [priority] null = unranked; those follow ranked categories, sorted by name. */
@Serializable
data class ShopCategoryOrderItemDto(
    val id: String,
    val name: String = "",
    val priority: Int? = null,
    val productCount: Int = 0,
)

enum class CategorySort {
    PRIORITY,
    NAME,
    ;

    companion object {
        fun fromName(value: String?): CategorySort = entries.firstOrNull { it.name == value } ?: PRIORITY
    }
}

/** Partial update of shop details; null fields are left unchanged by the server. */
@Serializable
data class ShopProfilePatch(
    val active: Boolean? = null,
    val paymentsEnabled: Boolean? = null,
    val features: ShopFeaturesDto? = null,
    val name: String? = null,
    val address: String? = null,
    val ownerName: String? = null,
    val ownerPhone: String? = null,
    val ownerEmail: String? = null,
    val city: String? = null,
    val state: String? = null,
    val zip: String? = null,
    val registrationNumber: String? = null,
    val taxId: String? = null,
    val upiId: String? = null,
    val deliveryRadiusKm: Double? = null,
    val discountPercent: Double? = null,
    /** "HH:mm"; empty string clears the cutoff. */
    val deliveryUntil: String? = null,
    val homeDeliveryEnabled: Boolean? = null,
    val operatingHours: List<ShopHoursDayDto>? = null,
)

@Serializable
data class ShopOfferDto(
    val id: String = "",
    val title: String,
    val description: String = "",
    val scope: String = OfferScope.BILL,
    val discountType: String = OfferDiscountType.PERCENT,
    val value: Double,
    val productIds: List<String> = emptyList(),
    val minBillAmount: Double = 0.0,
    val active: Boolean = true,
    val startsAtEpochMs: Long? = null,
    val endsAtEpochMs: Long? = null,
)

object OfferScope {
    const val BILL = "BILL"
    const val PRODUCT = "PRODUCT"
}

object OfferDiscountType {
    const val PERCENT = "PERCENT"
    const val FLAT = "FLAT"
}

@Serializable
data class RatingReplyRequest(val reply: String)

@Serializable
data class ChartPointDto(val label: String, val value: Double)

@Serializable
data class VisitorStatsDto(
    val uniqueVisitors: Int = 0,
    val visitDays: Int = 0,
    val daily: List<ChartPointDto> = emptyList(),
)
