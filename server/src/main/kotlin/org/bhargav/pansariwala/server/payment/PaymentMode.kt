package org.bhargav.pansariwala.server.payment

/** Platform-wide checkout switch, stored on `PlatformSettingsDoc.paymentMode` and edited from Master. */
object PaymentMode {
    /** Customer pays the shop's own UPI ID; the shop confirms receipt. Zero gateway cost. */
    const val DIRECT_UPI = "DIRECT_UPI"
    /** Cashfree UPI intent, confirmed automatically; falls back to DIRECT_UPI if Cashfree fails. */
    const val CASHFREE = "CASHFREE"
    /** Legacy Razorpay checkout (pay first, then the order is created). */
    const val RAZORPAY = "RAZORPAY"
    val ALL = listOf(DIRECT_UPI, CASHFREE, RAZORPAY)
}

/** `OrderDoc.paymentMethod` / `PaymentAttemptDoc.method` values. */
object PaymentMethod {
    const val RAZORPAY = "ONLINE"
    const val COD = "COD"
    const val UPI_DIRECT = "UPI_DIRECT"
    const val CASHFREE = "CASHFREE"
}

/** `PaymentIntentDoc.status` / `PaymentAttemptDoc.status`. */
object IntentStatus {
    const val PENDING = "PENDING"
    const val SUCCESS = "SUCCESS"
    const val FAILED = "FAILED"
    /** Paid, but the order could not be created (e.g. stock gone); money returned. */
    const val REFUNDED = "REFUNDED"
    const val ATTEMPT_PAID = "PAID"
}

object SettlementStatus {
    const val PENDING = "PENDING"
    const val SPLIT = "SPLIT"
    const val MANUAL = "MANUAL"
    const val FAILED = "FAILED"
}
