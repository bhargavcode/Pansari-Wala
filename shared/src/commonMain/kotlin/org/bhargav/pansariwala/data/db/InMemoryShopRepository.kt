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

    override suspend fun productCount(shopId: String): Int =
        productsFlow.value.count { it.shopId == shopId }

    override suspend fun findProduct(idOrBarcode: String): Product? {
        val query = idOrBarcode.trim()
        return productsFlow.value.firstOrNull { it.id == query || it.barcode == query }
    }

    override suspend fun upsertProduct(product: Product) {
        mutex.withLock {
            val current = productsFlow.value.toMutableList()
            val index = current.indexOfFirst { it.id == product.id }
            if (index >= 0) current[index] = product else current.add(product)
            productsFlow.value = current
        }
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
            // Deduct stock for the new version.
            order.items.forEach { item ->
                products[item.productId]?.let { p ->
                    products[item.productId] = p.copy(stockQty = p.stockQty - item.quantity)
                }
            }
            productsFlow.value = products.values.toList()

            val index = orders.indexOfFirst { it.id == order.id }
            if (index >= 0) orders[index] = order else orders.add(order)
            ordersFlow.value = orders
        }
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
