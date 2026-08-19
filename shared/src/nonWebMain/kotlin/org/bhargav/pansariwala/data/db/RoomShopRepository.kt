package org.bhargav.pansariwala.data.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.domain.auth.Credentials
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderItem
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.domain.model.OrderSummary
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductCategory
import org.bhargav.pansariwala.domain.model.ProductUnit
import org.bhargav.pansariwala.domain.model.SalesSnapshot
import org.bhargav.pansariwala.domain.model.ShopUser
import org.bhargav.pansariwala.util.AppClock
import org.bhargav.pansariwala.util.generateId

class RoomShopRepository(
    private val database: ShopDatabase,
) : ShopRepository {

    private val userDao = database.userDao()
    private val productDao = database.productDao()
    private val orderDao = database.orderDao()

    override suspend fun ensureSeeded() {
        val products = SeedData.products()
        if (userDao.count() == 0) {
            userDao.insertAll(
                SeedData.users().map {
                    UserEntity(
                        id = it.user.id,
                        username = it.user.username,
                        passwordHash = it.passwordHash,
                        displayName = it.user.displayName,
                        shopId = it.user.shopId,
                    )
                },
            )
            productDao.upsertAll(products.map { it.toEntity() })
            val orders = SeedData.orders(AppClock.nowMillis(), products)
            orderDao.insertOrders(orders.map { it.toEntity() })
            orders.forEach { order ->
                orderDao.insertItems(order.items.map { it.toEntity(order.id) })
            }
        } else {
            // Add newly introduced catalog items without resetting stock or
            // overwriting products edited by the shopkeeper.
            products.forEach { product ->
                if (productDao.findById(product.id) == null) {
                    productDao.upsert(product.toEntity())
                }
            }
        }
    }

    override suspend fun authenticate(username: String, password: String): ShopUser? {
        val entity = userDao.findByUsername(username.trim()) ?: return null
        return if (entity.passwordHash == Credentials.hash(password)) entity.toUser() else null
    }

    override fun observeProducts(shopId: String): Flow<List<Product>> =
        productDao.observeAll(shopId).map { list -> list.map { it.toDomain() } }

    override fun observeRecentOrders(shopId: String, limit: Int): Flow<List<OrderSummary>> =
        orderDao.observeRecent(shopId, limit).map { list -> list.map { it.toSummary() } }

    override fun observeTodaySales(shopId: String, startOfDayEpochMs: Long): Flow<SalesSnapshot> =
        orderDao.observeTodaySales(shopId, startOfDayEpochMs).map {
            SalesSnapshot(orderCount = it.orderCount, totalValue = it.totalValue)
        }

    override suspend fun productCount(shopId: String): Int = productDao.count(shopId)

    override suspend fun findProduct(idOrBarcode: String): Product? {
        val query = idOrBarcode.trim()
        return (productDao.findById(query) ?: productDao.findByBarcode(query))?.toDomain()
    }

    override suspend fun upsertProduct(product: Product) {
        productDao.upsert(product.toEntity())
    }

    override suspend fun getOrder(orderId: String): Order? {
        val order = orderDao.getOrder(orderId) ?: return null
        val items = orderDao.getItems(orderId)
        return order.toDomain(items)
    }

    override suspend fun saveOrder(order: Order) {
        // Restore stock for the previous version (edits), then deduct for the new one
        // unless the order is cancelled (stock already restored / should not deduct).
        val previousItems = orderDao.getItems(order.id)
        previousItems.forEach { productDao.adjustStock(it.productId, it.quantity) }
        orderDao.replaceOrder(order.toEntity(), order.items.map { it.toEntity(order.id) })
        if (order.status != OrderStatus.CANCELLED) {
            order.items.forEach { productDao.adjustStock(it.productId, -it.quantity) }
        }
    }
}

private fun Product.toEntity() = ProductEntity(
    id = id,
    shopId = shopId,
    name = name,
    nameHi = nameHi,
    category = category.name,
    unit = unit.name,
    barcode = barcode,
    sellingPrice = sellingPrice,
    costPrice = costPrice,
    stockQty = stockQty,
    lowStockThreshold = lowStockThreshold,
    voiceAlias = voiceAlias,
)

private fun ProductEntity.toDomain() = Product(
    id = id,
    shopId = shopId,
    name = name,
    nameHi = nameHi,
    category = ProductCategory.fromName(category),
    unit = ProductUnit.fromName(unit),
    barcode = barcode,
    sellingPrice = sellingPrice,
    costPrice = costPrice,
    stockQty = stockQty,
    lowStockThreshold = lowStockThreshold,
    voiceAlias = voiceAlias,
)

private fun UserEntity.toUser() = ShopUser(
    id = id,
    username = username,
    displayName = displayName,
    shopId = shopId,
)

private fun Order.toEntity() = OrderEntity(
    id = id,
    shopId = shopId,
    createdAt = createdAtEpochMs,
    status = status.name,
    customerName = customerName,
    totalValue = totalValue,
    itemCount = itemCount,
    cancelReason = cancelReason,
)

private fun OrderItem.toEntity(orderId: String) = OrderItemEntity(
    id = generateId("item"),
    orderId = orderId,
    productId = productId,
    productName = productName,
    unit = unit.name,
    quantity = quantity,
    unitPrice = unitPrice,
    lineTotal = lineTotal,
)

private fun OrderEntity.toSummary() = OrderSummary(
    id = id,
    createdAtEpochMs = createdAt,
    itemCount = itemCount,
    totalValue = totalValue,
    status = OrderStatus.fromName(status),
    customerName = customerName,
)

private fun OrderEntity.toDomain(items: List<OrderItemEntity>) = Order(
    id = id,
    shopId = shopId,
    createdAtEpochMs = createdAt,
    status = OrderStatus.fromName(status),
    customerName = customerName,
    cancelReason = cancelReason,
    items = items.map {
        OrderItem(
            productId = it.productId,
            productName = it.productName,
            unit = ProductUnit.fromName(it.unit),
            quantity = it.quantity,
            unitPrice = it.unitPrice,
        )
    },
)
