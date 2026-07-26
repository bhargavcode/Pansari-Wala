package org.bhargav.pansariwala.domain.model

data class SalesSnapshot(
    val orderCount: Int,
    val totalValue: Double,
)

data class CategoryStock(
    val category: ProductCategory,
    val itemCount: Int,
    val stockValue: Double,
)

data class ShopUser(
    val id: String,
    val username: String,
    val displayName: String,
    val shopId: String,
)
