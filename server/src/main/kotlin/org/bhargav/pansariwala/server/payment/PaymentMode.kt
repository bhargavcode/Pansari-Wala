package org.bhargav.pansariwala.server.payment

/** Platform-wide checkout switch, stored on `PlatformSettingsDoc.paymentMode` and edited from Master. */
object PaymentMode {
    /** Customer pays the shop's own UPI ID; the shop confirms receipt. Zero gateway cost. */
    const val DIRECT_UPI = "DIRECT_UPI"
    /** Cashfree payment link; confirmed by webhook / status check. */
    const val CASHFREE = "CASHFREE"
    /** Legacy Razorpay checkout (pay first, then the order is created). */
    const val RAZORPAY = "RAZORPAY"
    val ALL = listOf(DIRECT_UPI, CASHFREE, RAZORPAY)
}

/** `OrderDoc.paymentMethod` values. */
object PaymentMethod {
    const val RAZORPAY = "ONLINE"
    const val COD = "COD"
    const val UPI_DIRECT = "UPI_DIRECT"
    const val CASHFREE = "CASHFREE"
}

const val STATUS_PAYMENT_PENDING = "PAYMENT_PENDING"
