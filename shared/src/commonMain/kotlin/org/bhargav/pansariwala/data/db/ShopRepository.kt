package org.bhargav.pansariwala.data.db

import kotlinx.coroutines.flow.Flow
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.ItemSale
import org.bhargav.pansariwala.domain.model.OrderSummary
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.SalesSnapshot
import org.bhargav.pansariwala.domain.model.ShopProductFilter
import org.bhargav.pansariwala.domain.model.ShopUser

/**
 * Single local-data entry point for the shop. Room-backed on Android/iOS
 * (see nonWebMain), in-memory on js/wasm where Room is unavailable.
 */
interface ShopRepository {
    suspend fun ensureSeeded()

    suspend fun authenticate(username: String, password: String): ShopUser?

    fun observeProducts(shopId: String): Flow<List<Product>>
    fun observeRecentOrders(shopId: String, limit: Int): Flow<List<OrderSummary>>
    fun observeTodaySales(shopId: String, startOfDayEpochMs: Long): Flow<SalesSnapshot>
    /** Local (POS/offline) orders with `from <= createdAt < to`, newest first. */
    suspend fun ordersBetween(shopId: String, fromEpochMs: Long, toEpochMs: Long): List<OrderSummary>
    /** Sold lines of local orders in the range, skipping cancelled / rejected / draft orders. */
    suspend fun itemSalesBetween(shopId: String, fromEpochMs: Long, toEpochMs: Long): List<ItemSale>

    /** First [limit] products matching [filter], ordered by name; grow [limit] to page. */
    fun observeProductPage(shopId: String, filter: ShopProductFilter, limit: Int): Flow<List<Product>>
    fun observeProductCount(shopId: String, filter: ShopProductFilter): Flow<Int>
    fun observeProduct(productId: String): Flow<Product?>

    suspend fun productCount(shopId: String): Int
    suspend fun findProduct(idOrBarcode: String): Product?
    suspend fun productsByIds(ids: Collection<String>): List<Product>
    /** masterProductId → shop productId for master items already added to [shopId]. */
    suspend fun productIdsByMasterIds(shopId: String, masterIds: Collection<String>): Map<String, String>
    suspend fun upsertProduct(product: Product)

    /** Atomic stock edits queued for sync; [delta] may be negative. */
    suspend fun adjustProductStock(productId: String, delta: Double)
    suspend fun setProductStock(productId: String, stockQty: Double)
    suspend fun setProductActive(productId: String, active: Boolean)

    suspend fun getOrder(orderId: String): Order?
    suspend fun saveOrder(order: Order)

    /** POS bills not yet confirmed by the server, oldest first. */
    suspend fun pendingOfflineOrders(shopId: String): List<PendingOfflineOrder>

    /**
     * Clears the pending flag only when [revision] is still the saved copy.
     * A newer edit keeps the bill queued.
     */
    suspend fun markOfflineOrderSynced(orderId: String, revision: Long)

    /** Inventory edits not yet confirmed by the server. */
    suspend fun pendingOfflineProducts(shopId: String): List<PendingOfflineProduct>
    suspend fun markOfflineProductSynced(productId: String, revision: Long)

    /** Bills pulled from the server (other devices). Unsent local edits win; stock is untouched. */
    suspend fun applyRemoteOrders(orders: List<Order>)

    /** Server catalog. Skips unsent local edits and products in unsent bills. */
    suspend fun applyRemoteProducts(shopId: String, products: List<Product>)

    suspend fun syncState(key: String): String?
    suspend fun setSyncState(key: String, value: String)
}

data class PendingOfflineOrder(
    val revision: Long,
    val order: Order,
)

data class PendingOfflineProduct(
    val revision: Long,
    val product: Product,
)

expect fun createShopRepository(): ShopRepository
