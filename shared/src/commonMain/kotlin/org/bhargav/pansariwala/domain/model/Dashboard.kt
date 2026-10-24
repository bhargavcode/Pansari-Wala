package org.bhargav.pansariwala.domain.model

data class SalesSnapshot(
    val orderCount: Int,
    val totalValue: Double,
)

data class CategoryStock(
    val category: ProductCategory,
    val itemCount: Int,
    val stockValue: Double,
    /** Master category root; when set it names the bucket instead of the legacy [category]. */
    val categoryId: String? = null,
)

/** Stock value per master category, falling back to the legacy shop bucket for unlinked products. */
fun List<Product>.categoryBreakdown(): List<CategoryStock> =
    groupBy { it.categoryId?.takeIf(String::isNotBlank) ?: it.category.name }
        .map { (_, products) ->
            CategoryStock(
                category = products.first().category,
                itemCount = products.size,
                stockValue = products.sumOf { it.stockValue },
                categoryId = products.first().categoryId?.takeIf(String::isNotBlank),
            )
        }
        .sortedByDescending { it.stockValue }

data class ShopUser(
    val id: String,
    val username: String,
    val displayName: String,
    val shopId: String,
)
