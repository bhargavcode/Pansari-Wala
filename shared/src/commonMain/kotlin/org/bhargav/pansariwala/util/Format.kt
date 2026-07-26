package org.bhargav.pansariwala.util

import kotlin.math.roundToLong

/** Formats a number with 2 decimals and thousands grouping, prefixed with the rupee sign. */
fun Double.asMoney(): String {
    val cents = (this * 100).roundToLong()
    val whole = cents / 100
    val frac = (cents % 100).toInt().let { if (it < 0) -it else it }
    val grouped = groupThousands(whole)
    val fracStr = frac.toString().padStart(2, '0')
    return "₹$grouped.$fracStr"
}

/** Drops trailing ".0" for whole quantities, keeps up to 2 decimals otherwise. */
fun Double.asQuantity(): String {
    if (this == this.toLong().toDouble()) return this.toLong().toString()
    val rounded = (this * 100).roundToLong() / 100.0
    return rounded.toString()
}

private fun groupThousands(value: Long): String {
    val negative = value < 0
    val digits = (if (negative) -value else value).toString()
    val sb = StringBuilder()
    val n = digits.length
    for (i in 0 until n) {
        if (i > 0 && (n - i) % 3 == 0) sb.append(',')
        sb.append(digits[i])
    }
    return if (negative) "-$sb" else sb.toString()
}
