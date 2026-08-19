package org.bhargav.pansariwala.platform

import androidx.activity.ComponentActivity
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import kotlinx.coroutines.suspendCancellableCoroutine
import org.bhargav.pansariwala.util.AppConstants
import org.json.JSONObject
import kotlin.coroutines.resume

open class RazorpayCheckoutActivity : ComponentActivity(), PaymentResultWithDataListener {
    override fun onPaymentSuccess(razorpayPaymentId: String?, paymentData: PaymentData?) {
        AndroidRazorpayBridge.onSuccess(paymentData)
    }

    override fun onPaymentError(code: Int, description: String?, paymentData: PaymentData?) {
        AndroidRazorpayBridge.onError(code)
    }
}

class AndroidRazorpayCheckout : RazorpayCheckout {
    override suspend fun pay(
        keyId: String,
        orderId: String,
        amountPaise: Long,
        customerName: String,
        customerPhone: String,
        description: String,
    ): Result<RazorpayPaymentResult> {
        devRazorpayOrNull(keyId, orderId)?.let { return it }
        val activity = AndroidActivityHolder.activity
            ?: return Result.failure(IllegalStateException(AppConstants.Razorpay.ERROR_UNAVAILABLE))
        return suspendCancellableCoroutine { cont ->
            AndroidRazorpayBridge.awaiting = { result ->
                if (cont.isActive) cont.resume(result)
            }
            cont.invokeOnCancellation { AndroidRazorpayBridge.awaiting = null }
            try {
                val checkout = Checkout()
                checkout.setKeyID(keyId)
                checkout.open(
                    activity,
                    JSONObject().apply {
                        put("name", AppConstants.Razorpay.MERCHANT_NAME)
                        put("description", description)
                        put("order_id", orderId)
                        put("currency", AppConstants.Razorpay.CURRENCY)
                        put("amount", amountPaise)
                        put(
                            "prefill",
                            JSONObject().apply {
                                put("name", customerName)
                                put("contact", customerPhone)
                                if (keyId.startsWith(AppConstants.Razorpay.TEST_KEY_PREFIX)) {
                                    put("method", "upi")
                                    put("vpa", AppConstants.Razorpay.TEST_UPI_VPA)
                                }
                            },
                        )
                    },
                )
            } catch (_: Exception) {
                AndroidRazorpayBridge.awaiting = null
                if (cont.isActive) {
                    cont.resume(Result.failure(IllegalStateException(AppConstants.Razorpay.ERROR_UNAVAILABLE)))
                }
            }
        }
    }
}

internal object AndroidRazorpayBridge {
    @Volatile
    var awaiting: ((Result<RazorpayPaymentResult>) -> Unit)? = null

    fun onSuccess(data: PaymentData?) {
        val paymentId = data?.paymentId.orEmpty()
        val orderId = data?.orderId.orEmpty()
        val signature = data?.signature.orEmpty()
        val result = if (paymentId.isBlank() || signature.isBlank()) {
            Result.failure(IllegalStateException(AppConstants.Razorpay.ERROR_FAILED))
        } else {
            Result.success(RazorpayPaymentResult(paymentId, orderId, signature))
        }
        awaiting?.invoke(result)
        awaiting = null
    }

    fun onError(code: Int) {
        val message = if (code == Checkout.PAYMENT_CANCELED) {
            AppConstants.Razorpay.ERROR_CANCELLED
        } else {
            AppConstants.Razorpay.ERROR_FAILED
        }
        awaiting?.invoke(Result.failure(IllegalStateException(message)))
        awaiting = null
    }
}
