package org.bhargav.pansariwala.platform

import kotlinx.coroutines.suspendCancellableCoroutine
import org.bhargav.pansariwala.util.AppConstants
import kotlin.coroutines.resume

interface IosRazorpayCallback {
    fun onSuccess(paymentId: String, orderId: String, signature: String)
    fun onCancelled()
    fun onFailed()
    fun onUnavailable()
}

interface IosRazorpayHost {
    fun pay(
        keyId: String,
        orderId: String,
        amountPaise: Long,
        currency: String,
        merchantName: String,
        customerName: String,
        customerPhone: String,
        description: String,
        callback: IosRazorpayCallback,
    )
}

object IosRazorpayBridge {
    var host: IosRazorpayHost? = null
}

class IosRazorpayCheckout : RazorpayCheckout {
    override suspend fun pay(
        keyId: String,
        orderId: String,
        amountPaise: Long,
        customerName: String,
        customerPhone: String,
        description: String,
    ): Result<RazorpayPaymentResult> {
        devRazorpayOrNull(keyId, orderId)?.let { return it }
        val host = IosRazorpayBridge.host
            ?: return Result.failure(IllegalStateException(AppConstants.Razorpay.ERROR_UNAVAILABLE))
        return suspendCancellableCoroutine { cont ->
            host.pay(
                keyId,
                orderId,
                amountPaise,
                AppConstants.Razorpay.CURRENCY,
                AppConstants.Razorpay.MERCHANT_NAME,
                customerName,
                customerPhone,
                description,
                object : IosRazorpayCallback {
                    override fun onSuccess(paymentId: String, orderId: String, signature: String) {
                        if (!cont.isActive) return
                        cont.resume(
                            Result.success(RazorpayPaymentResult(paymentId, orderId, signature)),
                        )
                    }

                    override fun onCancelled() {
                        if (!cont.isActive) return
                        cont.resume(Result.failure(IllegalStateException(AppConstants.Razorpay.ERROR_CANCELLED)))
                    }

                    override fun onFailed() {
                        if (!cont.isActive) return
                        cont.resume(Result.failure(IllegalStateException(AppConstants.Razorpay.ERROR_FAILED)))
                    }

                    override fun onUnavailable() {
                        if (!cont.isActive) return
                        cont.resume(Result.failure(IllegalStateException(AppConstants.Razorpay.ERROR_UNAVAILABLE)))
                    }
                },
            )
        }
    }
}
