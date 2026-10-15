package org.bhargav.pansariwala.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
abstract class ProductDao {
    @Query("SELECT * FROM products WHERE shopId = :shopId ORDER BY name ASC")
    abstract fun observeAll(shopId: String): Flow<List<ProductEntity>>

    @Query("SELECT COUNT(*) FROM products WHERE shopId = :shopId")
    abstract suspend fun count(shopId: String): Int

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    abstract suspend fun findById(id: String): ProductEntity?

    @Query("SELECT * FROM products WHERE id IN (:ids)")
    abstract suspend fun findByIds(ids: List<String>): List<ProductEntity>

    @Query("SELECT * FROM products WHERE barcode = :barcode LIMIT 1")
    abstract suspend fun findByBarcode(barcode: String): ProductEntity?

    @Upsert
    abstract suspend fun upsert(product: ProductEntity)

    @Upsert
    abstract suspend fun upsertAll(products: List<ProductEntity>)

    @Query("UPDATE products SET stockQty = stockQty + :delta WHERE id = :id")
    abstract suspend fun adjustStock(id: String, delta: Double)

    @Query("SELECT * FROM products WHERE shopId = :shopId AND pendingSync = 1")
    abstract suspend fun pending(shopId: String): List<ProductEntity>

    @Query("UPDATE products SET pendingSync = 0 WHERE id = :id AND syncRevision = :revision")
    abstract suspend fun markSynced(id: String, revision: Long): Int

    /** Local edit: queue for upload with a new revision. */
    @Transaction
    open suspend fun saveLocal(product: ProductEntity) {
        val revision = (findById(product.id)?.syncRevision ?: 0L) + 1L
        upsert(product.copy(pendingSync = 1, syncRevision = revision))
    }

    /** Server copy wins unless the row has unsent edits or is in an unsent bill ([blockedIds]). */
    @Transaction
    open suspend fun applyRemote(products: List<ProductEntity>, blockedIds: Set<String>) {
        products.forEach { remote ->
            if (remote.id in blockedIds) return@forEach
            val local = findById(remote.id)
            if (local?.pendingSync == 1) return@forEach
            upsert(remote.copy(pendingSync = 0, syncRevision = local?.syncRevision ?: 0L))
        }
    }
}
