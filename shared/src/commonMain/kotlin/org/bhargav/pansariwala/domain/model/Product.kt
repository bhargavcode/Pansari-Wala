package org.bhargav.pansariwala.domain.model

enum class ProductCategory(val displayName: String, val displayNameHi: String) {
    FORTUNE_BRANDED("Fortune / Branded", "फॉर्च्यून / ब्रांडेड"),
    GENERAL_GROCERY("General Grocery", "किराना"),
    PUJA_SAMAGRI("Puja Samagri", "पूजा सामग्री"),
    STANDARD_SPICES("Standard Spices", "मसाले"),
    HIGH_VALUE_SPICES("High-Value Spices", "विशेष मसाले"),
    ;

    companion object {
        fun fromName(value: String): ProductCategory =
            entries.firstOrNull { it.name == value } ?: GENERAL_GROCERY
    }
}

enum class ProductUnit(val label: String) {
    KG("kg"),
    LITRE("ltr"),
    GRAM("g"),
    PACKET("packet"),
    PIECE("pcs"),
    ;

    companion object {
        fun fromName(value: String): ProductUnit =
            entries.firstOrNull { it.name == value } ?: PIECE
    }
}

data class Product(
    val id: String,
    val shopId: String,
    val name: String,
    val nameHi: String,
    val category: ProductCategory,
    val unit: ProductUnit,
    val barcode: String?,
    val sellingPrice: Double,
    val costPrice: Double,
    val stockQty: Double,
    val lowStockThreshold: Double,
    val voiceAlias: String?,
) {
    val isLowStock: Boolean get() = stockQty <= lowStockThreshold
    val stockValue: Double get() = stockQty * costPrice
}
