package org.bhargav.pansariwala.data.db

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** [active] < 0 = any; [query] exact-matches ids/barcode, [pattern] is its escaped `%…%` LIKE form. */
private const val PRODUCT_FILTER =
    "shopId = :shopId" +
        " AND (:category IS NULL OR category = :category)" +
        " AND (:active < 0 OR isActive = :active)" +
        " AND (:lowOnly = 0 OR stockQty <= lowStockThreshold)" +
        " AND (:query = '' OR id = :query OR barcode = :query OR masterProductId = :query" +
        " OR name LIKE :pattern ESCAPE '\\' OR nameHi LIKE :pattern ESCAPE '\\'" +
        " OR brandName LIKE :pattern ESCAPE '\\' OR voiceAlias LIKE :pattern ESCAPE '\\')"

private const val QUEUE_SYNC = "pendingSync = 1, syncRevision = syncRevision + 1"

data class MasterLink(val masterProductId: String?, val id: String)

@Dao
abstract class ProductDao {
    @Query("SELECT * FROM products WHERE shopId = :shopId ORDER BY name ASC")
    abstract fun observeAll(shopId: String): Flow<List<ProductEntity>>

    @Query("SELECT * FROM products WHERE $PRODUCT_FILTER ORDER BY name COLLATE NOCASE ASC, id ASC LIMIT :limit")
    abstract fun observePage(
        shopId: String,
        category: String?,
        active: Int,
        lowOnly: Int,
        query: String,
        pattern: String,
        limit: Int,
    ): Flow<List<ProductEntity>>

    @Query("SELECT COUNT(*) FROM products WHERE $PRODUCT_FILTER")
    abstract fun observeCount(
        shopId: String,
        category: String?,
        active: Int,
        lowOnly: Int,
        query: String,
        pattern: String,
    ): Flow<Int>

    @Query("SELECT * FROM products WHERE id = :id LIMIT 1")
    abstract fun observeById(id: String): Flow<ProductEntity?>

    @Query("SELECT masterProductId, id FROM products WHERE shopId = :shopId AND masterProductId IN (:masterIds)")
    abstract suspend fun masterLinks(shopId: String, masterIds: List<String>): List<MasterLink>

    @Query("UPDATE products SET stockQty = stockQty + :delta, $QUEUE_SYNC WHERE id = :id")
    abstract suspend fun adjustStockLocal(id: String, delta: Double)

    @Query("UPDATE products SET stockQty = :stockQty, $QUEUE_SYNC WHERE id = :id")
    abstract suspend fun setStockLocal(id: String, stockQty: Double)

    @Query("UPDATE products SET isActive = :active, $QUEUE_SYNC WHERE id = :id")
    abstract suspend fun setActiveLocal(id: String, active: Boolean)

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
