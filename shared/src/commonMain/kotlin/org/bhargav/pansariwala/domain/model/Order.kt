package org.bhargav.pansariwala.domain.model

enum class OrderStatus {
    DRAFT,
    COMPLETED,
    ;

    companion object {
        fun fromName(value: String): OrderStatus =
            entries.firstOrNull { it.name == value } ?: COMPLETED
    }
}

data class OrderItem(
    val productId: String,
    val productName: String,
    val unit: ProductUnit,
    val quantity: Double,
    val unitPrice: Double,
) {
    val lineTotal: Double get() = quantity * unitPrice
}

data class OrderSummary(
    val id: String,
    val createdAtEpochMs: Long,
    val itemCount: Int,
    val totalValue: Double,
    val status: OrderStatus,
    val customerName: String?,
)

data class Order(
    val id: String,
    val shopId: String,
    val createdAtEpochMs: Long,
    val status: OrderStatus,
    val customerName: String?,
    val items: List<OrderItem>,
) {
    val totalValue: Double get() = items.sumOf { it.lineTotal }
    val itemCount: Int get() = items.size
}
