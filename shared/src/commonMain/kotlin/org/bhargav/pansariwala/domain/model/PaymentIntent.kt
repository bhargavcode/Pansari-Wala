package org.bhargav.pansariwala.domain.model

import org.bhargav.pansariwala.util.AppConstants

enum class PaymentIntentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    /** Paid, but the order could not be placed; money returned. */
    REFUNDED,
    ;

    companion object {
        fun fromName(value: String): PaymentIntentStatus = entries.firstOrNull { it.name == value } ?: PENDING
    }
}

/** Checkout payment transaction (PTX…); the shop order [orderId] exists only once [status] is SUCCESS. */
data class PaymentIntent(
    val id: String,
    val orderId: String,
    val status: PaymentIntentStatus,
    val method: String,
    val amountInr: Double,
    val shopId: String,
    val shopName: String,
    val customerName: String,
    /** Shop UPI ID for direct payments; null when a gateway collects. */
    val payeeUpi: String?,
    /** `upi://pay` link to open while [status] is PENDING. */
    val upiPayUri: String?,
    val attemptNo: Int,
    /** In this device's clock. */
    val expiresAtEpochMs: Long,
    val utr: String?,
    val failureReason: String?,
    val refundId: String?,
    val items: List<OrderItem>,
    val quote: CheckoutQuote?,
    val selfPickup: Boolean,
    val addressId: String?,
    val deliveryAddress: String,
    val createdAtEpochMs: Long,
) {
    val isDirectUpi: Boolean get() = method == AppConstants.Payment.METHOD_UPI_DIRECT
    val isFinal: Boolean get() = status != PaymentIntentStatus.PENDING
}
