package org.bhargav.pansariwala.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "orders",
    indices = [
        Index(value = ["shopId"]),
        Index(value = ["createdAt"]),
    ],
)
data class OrderEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val createdAt: Long,
    val status: String,
    val customerName: String?,
    val totalValue: Double,
    val itemCount: Int,
)

@Entity(
    tableName = "order_items",
    indices = [Index(value = ["orderId"])],
)
data class OrderItemEntity(
    @PrimaryKey val id: String,
    val orderId: String,
    val productId: String,
    val productName: String,
    val unit: String,
    val quantity: Double,
    val unitPrice: Double,
    val lineTotal: Double,
)
