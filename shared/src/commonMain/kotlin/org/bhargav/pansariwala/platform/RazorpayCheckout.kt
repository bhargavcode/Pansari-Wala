package org.bhargav.pansariwala.platform

import org.bhargav.pansariwala.util.AppConstants

interface RazorpayCheckout {
    suspend fun pay(
        keyId: String,
        orderId: String,
        amountPaise: Long,
        customerName: String,
        customerPhone: String,
        description: String,
    ): Result<RazorpayPaymentResult>
}

data class RazorpayPaymentResult(
    val paymentId: String,
    val orderId: String,
    val signature: String,
)

class NoOpRazorpayCheckout : RazorpayCheckout {
    override suspend fun pay(
        keyId: String,
        orderId: String,
        amountPaise: Long,
        customerName: String,
        customerPhone: String,
        description: String,
    ): Result<RazorpayPaymentResult> =
        devRazorpayOrNull(keyId, orderId)
            ?: Result.failure(IllegalStateException(AppConstants.Razorpay.ERROR_UNAVAILABLE))
}

internal fun devRazorpayOrNull(keyId: String, orderId: String): Result<RazorpayPaymentResult>? {
    if (
        keyId.isBlank() ||
        keyId == AppConstants.Razorpay.DEV_KEY_ID ||
        orderId.startsWith(AppConstants.Razorpay.DEV_ORDER_PREFIX)
    ) {
        return Result.success(
            RazorpayPaymentResult(
                paymentId = AppConstants.Razorpay.DEV_PAYMENT_ID,
                orderId = orderId,
                signature = AppConstants.Razorpay.DEV_SIGNATURE,
            ),
        )
    }
    return null
}
