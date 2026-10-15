package org.bhargav.pansariwala.feature.order

import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.StringResource
import kotlin.math.roundToLong
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.payment_method_cod
import pansariwala.shared.generated.resources.payment_method_online
import pansariwala.shared.generated.resources.pos_payment_card
import pansariwala.shared.generated.resources.pos_payment_cash
import pansariwala.shared.generated.resources.pos_payment_upi

/** NPCI UPI deep link (`upi://pay`) with the bill amount locked in. */
fun buildUpiUri(upiId: String, payeeName: String, amount: Double): String {
    val paise = (amount * 100).roundToLong().coerceAtLeast(0)
    val formatted = "${paise / 100}.${(paise % 100).toString().padStart(2, '0')}"
    return buildString {
        append("upi://pay?pa=").append(upiEncode(upiId.trim()))
        if (payeeName.isNotBlank()) append("&pn=").append(upiEncode(payeeName.trim()))
        append("&am=").append(formatted)
        append("&cu=").append(AppConstants.PosPayment.UPI_CURRENCY)
    }
}

private fun upiEncode(value: String): String = buildString {
    value.encodeToByteArray().forEach { byte ->
        val c = byte.toInt().toChar()
        if (c.isLetterOrDigit() && c.code < 128 || c in "-._~@") {
            append(c)
        } else {
            append('%').append((byte.toInt() and 0xFF).toString(16).uppercase().padStart(2, '0'))
        }
    }
}

fun paymentMethodLabelRes(method: String): StringResource = when (method) {
    AppConstants.PosPayment.CASH -> Res.string.pos_payment_cash
    AppConstants.PosPayment.UPI -> Res.string.pos_payment_upi
    AppConstants.PosPayment.CARD -> Res.string.pos_payment_card
    "COD" -> Res.string.payment_method_cod
    else -> Res.string.payment_method_online
}
