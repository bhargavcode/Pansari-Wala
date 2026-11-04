package org.bhargav.pansariwala.util

import kotlin.random.Random

/** EAN-13 prefix "2" is reserved for in-store codes, so generated barcodes never clash with brand GTINs. */
private const val STORE_BARCODE_PREFIX = "2"
private const val EAN13_BODY_LENGTH = 12

/** Random in-store EAN-13 (12 digits + check digit) for shop-packed goods. */
fun generateStoreBarcode(random: Random = Random.Default): String {
    val body = STORE_BARCODE_PREFIX + (STORE_BARCODE_PREFIX.length until EAN13_BODY_LENGTH)
        .joinToString("") { random.nextInt(10).toString() }
    return body + ean13CheckDigit(body)
}

fun ean13CheckDigit(body: String): Int {
    val sum = body.mapIndexed { index, c -> (c - '0') * if (index % 2 == 0) 1 else 3 }.sum()
    return (10 - sum % 10) % 10
}
