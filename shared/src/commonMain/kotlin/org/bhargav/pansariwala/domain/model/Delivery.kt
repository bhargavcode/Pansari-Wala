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
    val dropAddress: String,
    val dropDistanceKm: Double,
    val shopDistanceKm: Double,
    val payoutInr: Double,
    val expiresAtEpochMs: Long,
    val status: DeliveryOfferStatus,
    val acceptedByPartnerId: String? = null,
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
