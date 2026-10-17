package org.bhargav.pansariwala.data.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.domain.auth.Credentials
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.domain.model.OrderSummary
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.SalesSnapshot
import org.bhargav.pansariwala.domain.model.ShopProductFilter
import org.bhargav.pansariwala.domain.model.ShopUser
import org.bhargav.pansariwala.util.AppClock

/**
 * Pure-Kotlin fallback store for platforms without Room (js/wasm). Mirrors the
 * behaviour of the Room-backed repository so the whole app works everywhere.
 */
class InMemoryShopRepository : ShopRepository {

    private val users = LinkedHashMap<String, Pair<ShopUser, String>>() // username -> (user, hash)
    private val productsFlow = MutableStateFlow<List<Product>>(emptyList())
    private val ordersFlow = MutableStateFlow<List<Order>>(emptyList())
    private val orderRevisions = HashMap<String, Long>()
    private val pendingOrders = HashSet<String>()
    private val productRevisions = HashMap<String, Long>()
    private val pendingProducts = HashSet<String>()
    private val syncStates = HashMap<String, String>()
    private val mutex = Mutex()
    private var seeded = false

    override suspend fun ensureSeeded() {
        mutex.withLock {
            if (seeded) return
            SeedData.users().forEach { seed ->
                users[seed.user.username] = seed.user to seed.passwordHash
            }
            val products = SeedData.products()
            productsFlow.value = products
            ordersFlow.value = SeedData.orders(AppClock.nowMillis(), products)
            seeded = true
        }
    }

    override suspend fun authenticate(username: String, password: String): ShopUser? {
        val entry = users[username.trim()] ?: return null
        return if (entry.second == Credentials.hash(password)) entry.first else null
    }

    override fun observeProducts(shopId: String): Flow<List<Product>> =
        productsFlow.map { list -> list.filter { it.shopId == shopId }.sortedBy { it.name } }

    override fun observeRecentOrders(shopId: String, limit: Int): Flow<List<OrderSummary>> =
        ordersFlow.map { list ->
            list.filter { it.shopId == shopId }
                .sortedByDescending { it.createdAtEpochMs }
                .take(limit)
                .map { it.toSummary() }
        }

    override fun observeTodaySales(shopId: String, startOfDayEpochMs: Long): Flow<SalesSnapshot> =
        ordersFlow.map { list ->
            val today = list.filter {
                it.shopId == shopId &&
                    it.status == OrderStatus.COMPLETED &&
                    it.createdAtEpochMs >= startOfDayEpochMs
            }
            SalesSnapshot(orderCount = today.size, totalValue = today.sumOf { it.totalValue })
        }

    override suspend fun ordersBetween(shopId: String, fromEpochMs: Long, toEpochMs: Long): List<OrderSummary> =
        ordersFlow.value
            .filter { it.shopId == shopId && it.createdAtEpochMs >= fromEpochMs && it.createdAtEpochMs < toEpochMs }
            .sortedByDescending { it.createdAtEpochMs }
            .map { it.toSummary() }

    override suspend fun productCount(shopId: String): Int =
        productsFlow.value.count { it.shopId == shopId }

    override suspend fun findProduct(idOrBarcode: String): Product? {
        val query = idOrBarcode.trim()
        return productsFlow.value.firstOrNull { it.id == query || it.barcode == query }
    }

    override suspend fun productsByIds(ids: Collection<String>): List<Product> {
        val wanted = ids.toSet()
        return productsFlow.value.filter { it.id in wanted }
    }

    override suspend fun upsertProduct(product: Product) {
        mutex.withLock {
            val current = productsFlow.value.toMutableList()
            val index = current.indexOfFirst { it.id == product.id }
            if (index >= 0) current[index] = product else current.add(product)
            productsFlow.value = current
            productRevisions[product.id] = (productRevisions[product.id] ?: 0L) + 1L
            pendingProducts += product.id
        }
    }

    override fun observeProductPage(shopId: String, filter: ShopProductFilter, limit: Int): Flow<List<Product>> =
        productsFlow.map { list ->
            list.asSequence()
                .filter { it.shopId == shopId && filter.matches(it) }
                .sortedWith(compareBy<Product, String>(String.CASE_INSENSITIVE_ORDER) { it.name }.thenBy { it.id })
                .take(limit)
                .toList()
        }

    override fun observeProductCount(shopId: String, filter: ShopProductFilter): Flow<Int> =
        productsFlow.map { list -> list.count { it.shopId == shopId && filter.matches(it) } }

    override fun observeProduct(productId: String): Flow<Product?> =
        productsFlow.map { list -> list.firstOrNull { it.id == productId } }

    override suspend fun productIdsByMasterIds(shopId: String, masterIds: Collection<String>): Map<String, String> {
        val wanted = masterIds.toSet()
        return productsFlow.value
            .filter { it.shopId == shopId && it.masterProductId in wanted }
            .associate { it.masterProductId!! to it.id }
    }

    override suspend fun adjustProductStock(productId: String, delta: Double) =
        editProduct(productId) { it.copy(stockQty = it.stockQty + delta) }

    override suspend fun setProductStock(productId: String, stockQty: Double) =
        editProduct(productId) { it.copy(stockQty = stockQty) }

    override suspend fun setProductActive(productId: String, active: Boolean) =
        editProduct(productId) { it.copy(isActive = active) }

    private suspend fun editProduct(productId: String, transform: (Product) -> Product) {
        val current = productsFlow.value.firstOrNull { it.id == productId } ?: return
        upsertProduct(transform(current))
    }

    override suspend fun getOrder(orderId: String): Order? =
        ordersFlow.value.firstOrNull { it.id == orderId }

    override suspend fun saveOrder(order: Order) {
        mutex.withLock {
            val orders = ordersFlow.value.toMutableList()
            val existing = orders.firstOrNull { it.id == order.id }
            val products = productsFlow.value.associateBy { it.id }.toMutableMap()

            // Restore stock from the previous version (for edits).
            existing?.items?.forEach { item ->
                products[item.productId]?.let { p ->
                    products[item.productId] = p.copy(stockQty = p.stockQty + item.quantity)
                }
            }
            // Deduct stock for the new version unless cancelled.
            if (order.status != OrderStatus.CANCELLED) {
                order.items.forEach { item ->
                    products[item.productId]?.let { p ->
                        products[item.productId] = p.copy(stockQty = p.stockQty - item.quantity)
                    }
                }
            }
            productsFlow.value = products.values.toList()

            val index = orders.indexOfFirst { it.id == order.id }
            if (index >= 0) orders[index] = order else orders.add(order)
            ordersFlow.value = orders
            orderRevisions[order.id] = (orderRevisions[order.id] ?: 0L) + 1L
            pendingOrders += order.id
        }
    }

    override suspend fun pendingOfflineOrders(shopId: String): List<PendingOfflineOrder> =
        mutex.withLock {
            ordersFlow.value
                .filter { it.shopId == shopId && it.id in pendingOrders }
                .map { PendingOfflineOrder(orderRevisions[it.id] ?: 1L, it) }
                .sortedBy { it.order.createdAtEpochMs }
        }

    override suspend fun markOfflineOrderSynced(orderId: String, revision: Long) {
        mutex.withLock {
            if (orderRevisions[orderId] == revision) pendingOrders -= orderId
        }
    }

    override suspend fun pendingOfflineProducts(shopId: String): List<PendingOfflineProduct> =
        mutex.withLock {
            productsFlow.value
                .filter { it.shopId == shopId && it.id in pendingProducts }
                .map { PendingOfflineProduct(productRevisions[it.id] ?: 1L, it) }
        }

    override suspend fun markOfflineProductSynced(productId: String, revision: Long) {
        mutex.withLock {
            if (productRevisions[productId] == revision) pendingProducts -= productId
        }
    }

    override suspend fun applyRemoteOrders(orders: List<Order>) {
        mutex.withLock {
            val byId = ordersFlow.value.associateBy { it.id }.toMutableMap()
            orders.forEach { if (it.id !in pendingOrders) byId[it.id] = it }
            ordersFlow.value = byId.values.toList()
        }
    }

    override suspend fun applyRemoteProducts(shopId: String, products: List<Product>) {
        mutex.withLock {
            val blocked = ordersFlow.value
                .filter { it.shopId == shopId && it.id in pendingOrders }
                .flatMapTo(HashSet()) { o -> o.items.map { it.productId } }
            val byId = productsFlow.value.associateBy { it.id }.toMutableMap()
            products.forEach {
                if (it.id !in pendingProducts && it.id !in blocked) byId[it.id] = it
            }
            productsFlow.value = byId.values.toList()
        }
    }

    override suspend fun syncState(key: String): String? = mutex.withLock { syncStates[key] }

    override suspend fun setSyncState(key: String, value: String) {
        mutex.withLock { syncStates[key] = value }
    }

    private fun Order.toSummary() = OrderSummary(
        id = id,
        createdAtEpochMs = createdAtEpochMs,
        itemCount = itemCount,
        totalValue = totalValue,
        status = status,
        customerName = customerName,
    )
}
