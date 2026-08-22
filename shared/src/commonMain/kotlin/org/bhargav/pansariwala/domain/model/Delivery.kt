package org.bhargav.pansariwala.domain.model

data class DeliveryPartner(
    val id: String,
    val name: String,
    val email: String,
    val address: String,
    val phone: String,
    val vehicleReg: String,
    val location: GeoPoint?,
)

data class DeliveryOffer(
    val id: String,
    val orderId: String,
    val shop: MarketplaceShop,
    val shopAddress: String? = null,
    val dropAddress: String,
    val dropDistanceKm: Double,
    val shopDistanceKm: Double,
    val totalDistanceKm: Double = 0.0,
    val payoutInr: Double,
    val expiresAtEpochMs: Long,
    val status: DeliveryOfferStatus,
    val acceptedByPartnerId: String? = null,
    val customerName: String? = null,
    val estimatedMinutes: Int = 0,
)

enum class DeliveryOfferStatus {
    RINGING,
    ACCEPTED,
    REJECTED,
    EXPIRED,
    TAKEN_BY_OTHER,
    ;

    companion object {
        fun fromName(value: String): DeliveryOfferStatus =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: EXPIRED
    }
}

data class PartnerDashboard(
    val deliveredCount: Int,
    val acceptedCount: Int,
    val earnings: Double,
    val fromEpochMs: Long,
    val toEpochMs: Long,
)

data class PartnerProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String,
    val address: String,
    val vehicleReg: String,
    val verified: Boolean,
    val online: Boolean,
    val joinedAtEpochMs: Long,
    val todayEarnings: Double,
    val totalEarnings: Double,
    val deliveredCount: Int,
    val profilePhoto: String = "",
)

data class PartnerDailyEarning(
    val dayLabel: String,
    val amount: Double,
)

data class PartnerEarnings(
    val todayEarnings: Double,
    val totalEarnings: Double,
    val deliveredCount: Int,
    val acceptanceRatePercent: Int,
    val weeklyEarnings: List<PartnerDailyEarning>,
)
