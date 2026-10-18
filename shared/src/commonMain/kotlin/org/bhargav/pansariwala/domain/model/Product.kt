package org.bhargav.pansariwala.domain.model

import kotlinx.serialization.Serializable

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

        /** Master categories are admin-defined; map one by name onto the shop's fixed categories. */
        fun fromMasterCategoryName(name: String): ProductCategory {
            val key = name.trim().lowercase()
            if (key.isEmpty()) return GENERAL_GROCERY
            return entries.firstOrNull { category ->
                val display = category.displayName.lowercase()
                category.name.equals(key.replace(' ', '_'), ignoreCase = true) ||
                    display.contains(key) || key.contains(display) ||
                    display.split(" / ", " ").any { it.length > 3 && key.contains(it) }
            } ?: GENERAL_GROCERY
        }
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
@Serializable
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
    /** Inactive options stay on the product but are hidden from customers and billing. */
    val active: Boolean = true,
    /** Net content of one pack ("100 g" Parle-G); describes the pack, never a sell quantity. */
    val packSize: Double = 0.0,
    val packUnit: ProductUnit = ProductUnit.PIECE,
    /** For [SellMode.BUNDLE]: the pack variant whose stock drops by [bundleCount] per bundle sold. */
    val baseVariantId: String = "",
) {
    val isLoose: Boolean get() = sellMode == SellMode.LOOSE
    val hasPackSize: Boolean get() = !isLoose && packSize > 0.0
    val primaryImageUrl: String? get() = imageUrls.firstOrNull { it.isNotBlank() }
}

@Serializable
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
    /** Best live product offer; lowers the unit price shown and charged. */
    val offer: ProductOffer? = null,
    /** Master catalog category (root) and optional sub-category; [category] stays the fixed shop bucket. */
    val categoryId: String? = null,
    val subCategoryId: String? = null,
) {
    /** Price after [offer] for one unit priced [base] (a variant price or [sellingPrice]). */
    fun salePrice(base: Double = sellingPrice): Double = offer?.unitPriceOn(base) ?: base
    val hasOffer: Boolean get() = offer != null && salePrice() < sellingPrice

    val isLowStock: Boolean get() = stockQty <= lowStockThreshold
    val stockLevel: StockLevel get() = StockLevel.of(stockQty, lowStockThreshold)
    val stockValue: Double get() = stockQty * costPrice
    val primaryImageUrl: String? get() =
        imageUrls.firstOrNull { it.isNotBlank() } ?: variants.firstNotNullOfOrNull { it.primaryImageUrl }
    /** Options a customer or cashier can buy; inactive variants are hidden. */
    val activeVariants: List<ProductVariant> get() = variants.filter { it.active }
    val hasVariants: Boolean get() = activeVariants.isNotEmpty()

    /** Weight/volume products without variants are sold loose by [unit]. */
    val isLooseUnit: Boolean get() = unit == ProductUnit.KG || unit == ProductUnit.LITRE || unit == ProductUnit.GRAM

    /** Lowest price across options, for "from ₹x" labels. */
    val startingPrice: Double get() = activeVariants.minOfOrNull { it.price } ?: sellingPrice

    fun variant(id: String?): ProductVariant? = id?.let { vid -> variants.firstOrNull { it.id == vid } }

    /**
     * Stock after one order line moves [delta] (negative = sold). A bundle linked to a pack variant
     * (carton of 24) moves that pack's stock by `bundleCount × delta` instead of its own.
     */
    fun stockChanged(variantId: String?, delta: Double): Product {
        val sold = variant(variantId) ?: return copy(stockQty = stockQty + delta)
        val base = sold.takeIf { it.sellMode == SellMode.BUNDLE && it.bundleCount > 0 && it.baseVariantId.isNotBlank() }
            ?.let { variant(it.baseVariantId) }
        val target = base ?: sold
        val units = if (base != null) delta * sold.bundleCount else delta
        return copy(
            variants = variants.map { if (it.id == target.id) it.copy(stockQty = (it.stockQty + units).coerceAtLeast(0.0)) else it },
        )
    }
}

enum class DiscountType {
    PERCENT,
    FLAT,
    ;

    companion object {
        fun fromName(value: String?): DiscountType = entries.firstOrNull { it.name == value } ?: PERCENT
    }
}

/** Product-scope shop offer: [DiscountType.PERCENT] off, or [DiscountType.FLAT] ₹ off each unit. */
@Serializable
data class ProductOffer(
    val id: String,
    val title: String,
    val type: DiscountType,
    val value: Double,
) {
    fun unitPriceOn(unitPrice: Double): Double = when (type) {
        DiscountType.FLAT -> unitPrice - value
        DiscountType.PERCENT -> unitPrice * (1.0 - value / 100.0)
    }.coerceIn(0.0, unitPrice)
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
