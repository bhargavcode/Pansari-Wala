package org.bhargav.pansariwala.api

import kotlinx.serialization.Serializable

object BillingModel {
    const val NONE = "NONE"
    const val MONTHLY = "MONTHLY"
    const val YEARLY = "YEARLY"
    const val COMMISSION = "COMMISSION"
    const val FREE = "FREE"
    val ALL = listOf(NONE, MONTHLY, YEARLY, COMMISSION, FREE)
}

object SubscriptionTxnType {
    const val PURCHASE = "PURCHASE"
    const val RENEWAL = "RENEWAL"
    const val FREE_GRANT = "FREE_GRANT"
    const val MODEL_CHANGE = "MODEL_CHANGE"
}

@Serializable
data class SubscriptionPlanDto(
    val id: String = "",
    val name: String,
    val description: String = "",
    val features: List<String> = emptyList(),
    val monthlyPriceInr: Double = 0.0,
    val yearlyPriceInr: Double = 0.0,
    val active: Boolean = true,
    val sortOrder: Int = 0,
)

@Serializable
data class ShopBillingDto(
    val shopId: String,
    val model: String = BillingModel.NONE,
    val planId: String? = null,
    val planName: String = "",
    val planFeatures: List<String> = emptyList(),
    val commissionPercent: Double = 0.0,
    val startedAtEpochMs: Long? = null,
    val expiresAtEpochMs: Long? = null,
    val freeUntilEpochMs: Long? = null,
    val note: String = "",
    /** False once a paid period or free grant has lapsed. */
    val active: Boolean = true,
    val daysLeft: Int? = null,
    /** True inside the expiry warning window or after expiry: POS shows a pay-to-continue message. */
    val showRenewWarning: Boolean = false,
    val updatedAtEpochMs: Long = 0L,
)

@Serializable
data class SubscriptionTxnDto(
    val id: String,
    val shopId: String,
    val type: String,
    val planId: String? = null,
    val planName: String = "",
    val period: String = "",
    val amountInr: Double = 0.0,
    val paymentId: String? = null,
    val razorpayOrderId: String? = null,
    val status: String = "SUCCESS",
    val periodStartEpochMs: Long? = null,
    val periodEndEpochMs: Long? = null,
    val note: String = "",
    val by: String = "",
    val createdAtEpochMs: Long,
)

@Serializable
data class ShopSubscriptionDto(
    val billing: ShopBillingDto,
    val plans: List<SubscriptionPlanDto> = emptyList(),
    val transactions: List<SubscriptionTxnDto> = emptyList(),
    val defaultCommissionPercent: Double = 0.0,
    val expiryWarningDays: Int = 7,
)

@Serializable
data class SubscriptionCheckoutRequest(val planId: String, val period: String)

@Serializable
data class SubscriptionCheckoutDto(
    val razorpayOrderId: String,
    val amountPaise: Long,
    val currency: String = "INR",
    val keyId: String,
    val planId: String,
    val period: String,
)

@Serializable
data class SubscriptionConfirmRequest(
    val planId: String,
    val period: String,
    val razorpayOrderId: String,
    val razorpayPaymentId: String,
    val razorpaySignature: String,
)

@Serializable
data class CommissionDayDto(
    /** `yyyy-MM-dd` in shop time. */
    val day: String,
    val dayStartEpochMs: Long,
    val orderCount: Int,
    val totalSaleInr: Double,
    val commissionPercent: Double,
    val commissionInr: Double,
    val payoutInr: Double,
)

@Serializable
data class CommissionSummaryDto(
    val commissionPercent: Double,
    val totalSaleInr: Double = 0.0,
    val commissionInr: Double = 0.0,
    val payoutInr: Double = 0.0,
    val days: List<CommissionDayDto> = emptyList(),
)

@Serializable
data class CommissionOrderItemDto(
    val name: String,
    val variantLabel: String? = null,
    val quantity: Double,
    val unit: String,
    val unitPrice: Double,
)

@Serializable
data class CommissionOrderDto(
    val orderId: String,
    val createdAtEpochMs: Long,
    val channel: String,
    val status: String,
    val paymentMethod: String,
    val customerName: String? = null,
    val itemsSubtotalInr: Double,
    val discountInr: Double,
    val saleInr: Double,
    val commissionInr: Double,
    val payoutInr: Double,
    val items: List<CommissionOrderItemDto> = emptyList(),
)

@Serializable
data class CommissionDayOrdersDto(
    val day: CommissionDayDto,
    val orders: List<CommissionOrderDto> = emptyList(),
)
