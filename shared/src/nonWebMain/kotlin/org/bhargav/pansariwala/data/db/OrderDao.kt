package org.bhargav.pansariwala.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class SalesRow(
    val orderCount: Int,
    val totalValue: Double,
)

@Dao
abstract class OrderDao {
    @Query("SELECT * FROM orders WHERE shopId = :shopId ORDER BY createdAt DESC LIMIT :limit")
    abstract fun observeRecent(shopId: String, limit: Int): Flow<List<OrderEntity>>

    @Query(
        "SELECT COUNT(*) AS orderCount, COALESCE(SUM(totalValue), 0.0) AS totalValue " +
            "FROM orders WHERE shopId = :shopId AND status = 'COMPLETED' AND createdAt >= :start",
    )
    abstract fun observeTodaySales(shopId: String, start: Long): Flow<SalesRow>

    @Query("SELECT * FROM orders WHERE id = :id LIMIT 1")
    abstract suspend fun getOrder(id: String): OrderEntity?

    @Query("SELECT * FROM order_items WHERE orderId = :orderId")
    abstract suspend fun getItems(orderId: String): List<OrderItemEntity>

    @Upsert
    abstract suspend fun upsertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertItems(items: List<OrderItemEntity>)

    @Query("DELETE FROM order_items WHERE orderId = :orderId")
    abstract suspend fun deleteItems(orderId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertOrders(orders: List<OrderEntity>)

    @Transaction
    open suspend fun replaceOrder(order: OrderEntity, items: List<OrderItemEntity>) {
        upsertOrder(order)
        deleteItems(order.id)
        insertItems(items)
    }
}
