package org.bhargav.pansariwala.server.dto

import kotlinx.serialization.Serializable

/** Self-service shop onboarding draft; saved after every step so the owner can resume or edit later. */
@Serializable
data class ShopRegistrationDraftDto(
    val phone: String = "",
    /** Index of the furthest step the owner reached. */
    val step: Int = 0,
    val ownerName: String = "",
    val ownerEmail: String = "",
    val shopName: String = "",
    val shopTypes: List<String> = emptyList(),
    val address: String = "",
    val city: String = "",
    val state: String = "",
    val zip: String = "",
    val country: String = "India",
    val lat: Double? = null,
    val lng: Double? = null,
    /** Device GPS fix taken on the location step; the shop pin must be close to it. */
    val deviceLat: Double? = null,
    val deviceLng: Double? = null,
    val locationVerified: Boolean = false,
    val registrationNumber: String = "",
    val taxId: String = "",
    val operatingHours: List<ShopHoursDayDto> = emptyList(),
    val loginUsername: String = "",
    /** `android`, `ios` or `web`; app drafts get push reminders, web drafts get SMS. */
    val platform: String = "",
    val pushToken: String? = null,
)

@Serializable
data class ShopRegistrationVerifyRequest(val phone: String, val otp: String, val sessionId: String? = null)

@Serializable
data class ShopRegistrationSessionDto(
    val token: String,
    val expiresInSec: Long,
    val draft: ShopRegistrationDraftDto,
)

@Serializable
data class ShopRegistrationSubmit(
    val draft: ShopRegistrationDraftDto,
    val password: String,
)

@Serializable
data class ShopRegistrationResultDto(
    val shopId: String,
    val shopName: String,
    val loginUsername: String,
)
