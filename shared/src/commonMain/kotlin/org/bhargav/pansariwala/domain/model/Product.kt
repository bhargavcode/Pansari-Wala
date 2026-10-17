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

/** PACK = fixed pack bought by count; LOOSE = bought by weight/volume; BUNDLE = several packs together. */
enum class SellMode {
    PACK,
    LOOSE,
    BUNDLE,
    ;

    companion object {
        fun fromName(value: String?): SellMode = entries.firstOrNull { it.name == value } ?: PACK
    }
}

/**
 * A sellable option of a [Product] (size, colour, pack size, item count or loose weight).
 * For [SellMode.LOOSE], [price] is per [baseUnit] and cart quantities are in [baseUnit].
 */
data class ProductVariant(
    val id: String,
    val label: String,
    val sellMode: SellMode = SellMode.PACK,
    val price: Double,
    val mrp: Double = 0.0,
    val attributes: Map<String, String> = emptyMap(),
    val baseUnit: ProductUnit = ProductUnit.PIECE,
    val minQty: Double = 0.0,
    val maxQty: Double = 0.0,
    val stepQty: Double = 0.0,
    val quickPicks: List<Double> = emptyList(),
    val bundleCount: Int = 0,
    val imageUrls: List<String> = emptyList(),
    val stockQty: Double = 0.0,
    val sku: String = "",
    val barcode: String? = null,
) {
    val isLoose: Boolean get() = sellMode == SellMode.LOOSE
    val primaryImageUrl: String? get() = imageUrls.firstOrNull { it.isNotBlank() }
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
    val imageUrls: List<String> = emptyList(),
    val variants: List<ProductVariant> = emptyList(),
    val brandName: String = "",
    /** Master catalog item this shop product was created from, if any. */
    val masterProductId: String? = null,
    /** Inactive products stay in the shop's inventory but are hidden from customers and billing search. */
    val isActive: Boolean = true,
) {
    val isLowStock: Boolean get() = stockQty <= lowStockThreshold
    val stockLevel: StockLevel get() = StockLevel.of(stockQty, lowStockThreshold)
    val stockValue: Double get() = stockQty * costPrice
    val primaryImageUrl: String? get() =
        imageUrls.firstOrNull { it.isNotBlank() } ?: variants.firstNotNullOfOrNull { it.primaryImageUrl }
    val hasVariants: Boolean get() = variants.isNotEmpty()

    /** Weight/volume products without variants are sold loose by [unit]. */
    val isLooseUnit: Boolean get() = unit == ProductUnit.KG || unit == ProductUnit.LITRE || unit == ProductUnit.GRAM

    /** Lowest price across options, for "from ₹x" labels. */
    val startingPrice: Double get() = variants.minOfOrNull { it.price } ?: sellingPrice

    fun variant(id: String?): ProductVariant? = id?.let { vid -> variants.firstOrNull { it.id == vid } }
}

enum class StockLevel {
    OUT_OF_STOCK,
    CRITICAL,
    LOW,
    HEALTHY,
    ;

    companion object {
        fun of(stockQty: Double, lowStockThreshold: Double): StockLevel = when {
            stockQty <= 0.0 -> OUT_OF_STOCK
            stockQty <= lowStockThreshold / 2 -> CRITICAL
            stockQty <= lowStockThreshold -> LOW
            else -> HEALTHY
        }
    }
}

enum class ProductStatusFilter { ALL, ACTIVE, INACTIVE }

data class ShopProductFilter(
    /** Matches name, Hindi name, brand, voice alias (contains) or product id / barcode / master id (exact). */
    val query: String = "",
    val category: ProductCategory? = null,
    val status: ProductStatusFilter = ProductStatusFilter.ALL,
    val lowStockOnly: Boolean = false,
) {
    fun matches(product: Product): Boolean {
        if (category != null && product.category != category) return false
        when (status) {
            ProductStatusFilter.ACTIVE -> if (!product.isActive) return false
            ProductStatusFilter.INACTIVE -> if (product.isActive) return false
            ProductStatusFilter.ALL -> Unit
        }
        if (lowStockOnly && !product.isLowStock) return false
        val q = query.trim()
        if (q.isEmpty()) return true
        return product.id == q || product.barcode == q || product.masterProductId == q ||
            product.name.contains(q, ignoreCase = true) ||
            product.nameHi.contains(q, ignoreCase = true) ||
            product.brandName.contains(q, ignoreCase = true) ||
            product.voiceAlias?.contains(q, ignoreCase = true) == true
    }
}
