package org.bhargav.pansariwala.product

enum class AppProduct {
    POS,
    USER,
    DELIVERY,
    ;

    companion object {
        fun fromName(value: String?): AppProduct =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: POS
    }
}

object AppProductHolder {
    var current: AppProduct = AppProduct.POS
}

expect fun currentAppProduct(): AppProduct
