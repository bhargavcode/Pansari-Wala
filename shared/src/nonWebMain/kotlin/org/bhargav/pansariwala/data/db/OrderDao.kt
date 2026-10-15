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

    @Query(
        "SELECT * FROM orders WHERE shopId = :shopId AND createdAt >= :from AND createdAt < :to " +
            "ORDER BY createdAt DESC",
    )
    abstract suspend fun ordersBetween(shopId: String, from: Long, to: Long): List<OrderEntity>

    @Query(
        "SELECT * FROM orders WHERE shopId = :shopId AND pendingSync = 1 ORDER BY createdAt ASC",
    )
    abstract suspend fun pending(shopId: String): List<OrderEntity>

    @Query("UPDATE orders SET pendingSync = 0 WHERE id = :id AND syncRevision = :revision")
    abstract suspend fun markSynced(id: String, revision: Long): Int

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

    @Query(
        "SELECT DISTINCT i.productId FROM order_items i JOIN orders o ON o.id = i.orderId " +
            "WHERE o.shopId = :shopId AND o.pendingSync = 1",
    )
    abstract suspend fun pendingProductIds(shopId: String): List<String>

    @Query("SELECT syncValue FROM sync_state WHERE syncKey = :key LIMIT 1")
    abstract suspend fun syncState(key: String): String?

    @Upsert
    abstract suspend fun putSyncState(state: SyncStateEntity)

    /** Server copies from other devices; rows with unsent local edits are kept. */
    @Transaction
    open suspend fun applyRemote(orders: List<Pair<OrderEntity, List<OrderItemEntity>>>) {
        orders.forEach { (order, items) ->
            val local = getOrder(order.id)
            if (local?.pendingSync == 1) return@forEach
            replaceOrder(order.copy(pendingSync = 0, syncRevision = local?.syncRevision ?: 0L), items)
        }
    }

    @Transaction
    open suspend fun saveOrderGraph(order: OrderEntity, items: List<OrderItemEntity>) {
        val revision = (getOrder(order.id)?.syncRevision ?: 0L) + 1L
        replaceOrder(order.copy(pendingSync = 1, syncRevision = revision), items)
    }
}
