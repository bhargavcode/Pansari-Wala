package org.bhargav.pansariwala.server.db

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** `_id` = "$shopId:$phone" (10-digit local number). */
@Serializable
data class BorrowerDoc(
    @SerialName("_id") val id: String,
    val shopId: String,
    val phone: String,
    val name: String = "",
    val photoKey: String? = null,
    val address: String = "",
    val notes: String = "",
    val createdAt: Long,
    val updatedAt: Long,
)

@Serializable
data class CreditTxnItemDoc(
    val name: String,
    val variantLabel: String? = null,
    val quantity: Double = 0.0,
    val unit: String = "",
    val lineTotal: Double = 0.0,
)

/** CREDIT rows use `_id` = "credit:$orderId" so a re-synced bill updates instead of duplicating. */
@Serializable
data class CreditTxnDoc(
    @SerialName("_id") val id: String,
    val shopId: String,
    val phone: String,
    val type: String,
    val amount: Double,
    val status: String,
    val paymentMode: String = "",
    val receivedBy: String = "",
    val processedBy: String = "",
    val source: String = "POS",
    val orderId: String? = null,
    val items: List<CreditTxnItemDoc> = emptyList(),
    val note: String = "",
    val createdAt: Long,
    val updatedAt: Long,
    /** App repayments: CASHFREE (confirmed by the gateway) or UPI_DIRECT (straight to the shop's UPI). */
    val gateway: String = "",
    val gatewayOrderId: String? = null,
    /** Gateway / UPI reference once paid. */
    val paymentRef: String? = null,
    /** A PENDING app repayment fails after this (0 = never). */
    val expiresAt: Long = 0L,
    val failureReason: String = "",
    /** POS CREDIT rows: full bill, and the part paid at the counter ([amount] is the rest). */
    val billTotal: Double = 0.0,
    val paidNow: Double = 0.0,
    val paidNowMethod: String? = null,
)
