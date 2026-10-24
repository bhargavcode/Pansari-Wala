package org.bhargav.pansariwala.data.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import org.bhargav.pansariwala.api.ProductVariantDto
import org.bhargav.pansariwala.api.toDto
import org.bhargav.pansariwala.api.toModel
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.domain.auth.Credentials
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.ItemSale
import org.bhargav.pansariwala.domain.model.OrderChannel
import org.bhargav.pansariwala.domain.model.OrderItem
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.domain.model.OrderSummary
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductCategory
import org.bhargav.pansariwala.domain.model.ProductStatusFilter
import org.bhargav.pansariwala.domain.model.ProductUnit
import org.bhargav.pansariwala.domain.model.ShopProductFilter
import org.bhargav.pansariwala.domain.model.ProductVariant
import org.bhargav.pansariwala.domain.model.SalesSnapshot
import org.bhargav.pansariwala.domain.model.ShopUser
import org.bhargav.pansariwala.util.AppClock
import org.bhargav.pansariwala.util.generateId

private const val SQLITE_MAX_BIND_ARGS = 900

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
            orderDao.insertOrders(orders.map { it.toEntity().copy(pendingSync = 0) })
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

    override suspend fun ordersBetween(shopId: String, fromEpochMs: Long, toEpochMs: Long): List<OrderSummary> =
        orderDao.ordersBetween(shopId, fromEpochMs, toEpochMs).map { it.toSummary() }

    override suspend fun itemSalesBetween(shopId: String, fromEpochMs: Long, toEpochMs: Long): List<ItemSale> =
        orderDao.itemSalesBetween(shopId, fromEpochMs, toEpochMs).map {
            ItemSale(
                orderId = it.orderId,
                productId = it.productId,
                productName = it.productName,
                variantLabel = it.variantLabel,
                quantity = it.quantity,
                lineTotal = it.lineTotal,
                createdAtEpochMs = it.createdAt,
                channel = OrderChannel.POS,
            )
        }

    override fun observeProductPage(shopId: String, filter: ShopProductFilter, limit: Int): Flow<List<Product>> {
        val q = filter.query.trim()
        return productDao.observePage(
            shopId = shopId,
            categoryId = filter.categoryId,
            active = filter.activeArg(),
            lowOnly = if (filter.lowStockOnly) 1 else 0,
            query = q,
            pattern = likePattern(q),
            limit = limit,
        ).map { list -> list.map { it.toDomain() } }
    }

    override fun observeProductCount(shopId: String, filter: ShopProductFilter): Flow<Int> {
        val q = filter.query.trim()
        return productDao.observeCount(
            shopId = shopId,
            categoryId = filter.categoryId,
            active = filter.activeArg(),
            lowOnly = if (filter.lowStockOnly) 1 else 0,
            query = q,
            pattern = likePattern(q),
        )
    }

    override fun observeProduct(productId: String): Flow<Product?> =
        productDao.observeById(productId).map { it?.toDomain() }

    override suspend fun productIdsByMasterIds(shopId: String, masterIds: Collection<String>): Map<String, String> =
        masterIds.distinct().chunked(SQLITE_MAX_BIND_ARGS).flatMap { chunk ->
            productDao.masterLinks(shopId, chunk)
        }.mapNotNull { link -> link.masterProductId?.let { it to link.id } }.toMap()

    override suspend fun adjustProductStock(productId: String, delta: Double) {
        productDao.adjustStockLocal(productId, delta)
    }

    override suspend fun setProductStock(productId: String, stockQty: Double) {
        productDao.setStockLocal(productId, stockQty)
    }

    override suspend fun setProductActive(productId: String, active: Boolean) {
        productDao.setActiveLocal(productId, active)
    }

    override fun observeDeletedProducts(shopId: String): Flow<List<Product>> =
        productDao.observeDeleted(shopId).map { list -> list.map { it.toDomain() } }

    override suspend fun setProductDeleted(productId: String, deletedAt: Long?) {
        productDao.setDeletedLocal(productId, deletedAt)
    }

    override suspend fun productCount(shopId: String): Int = productDao.count(shopId)

    override suspend fun findProduct(idOrBarcode: String): Product? {
        val query = idOrBarcode.trim()
        val byId = productDao.findById(query)?.takeIf { it.deletedAt == null }
        return (byId ?: productDao.findByBarcode(query))?.toDomain()
    }

    override suspend fun productsByIds(ids: Collection<String>): List<Product> =
        ids.distinct().chunked(SQLITE_MAX_BIND_ARGS).flatMap { chunk ->
            productDao.findByIds(chunk).map { it.toDomain() }
        }

    override suspend fun upsertProduct(product: Product) {
        productDao.saveLocal(product.toEntity())
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
        previousItems.forEach { adjustItemStock(it.productId, it.variantId, it.quantity) }
        orderDao.saveOrderGraph(order.toEntity(), order.items.map { it.toEntity(order.id) })
        if (order.status != OrderStatus.CANCELLED) {
            order.items.forEach { adjustItemStock(it.productId, it.variantId, -it.quantity) }
        }
    }

    /** Variant lines (incl. linked bundles) update the variant JSON; plain lines the product stock. */
    private suspend fun adjustItemStock(productId: String, variantId: String?, delta: Double) {
        val entity = variantId?.takeIf { it.isNotBlank() }?.let { productDao.findById(productId) }
        val product = entity?.toDomain()
        if (product?.variant(variantId) == null) {
            productDao.adjustStock(productId, delta)
            return
        }
        val updated = product.stockChanged(variantId, delta)
        productDao.setVariantsJson(productId, variantJson.encodeToString(updated.variants.map { it.toDto() }))
    }

    override suspend fun pendingOfflineOrders(shopId: String): List<PendingOfflineOrder> =
        orderDao.pending(shopId).map { entity ->
            PendingOfflineOrder(
                revision = entity.syncRevision,
                order = entity.toDomain(orderDao.getItems(entity.id)),
            )
        }

    override suspend fun markOfflineOrderSynced(orderId: String, revision: Long) {
        orderDao.markSynced(orderId, revision)
    }

    override suspend fun pendingOfflineProducts(shopId: String): List<PendingOfflineProduct> =
        productDao.pending(shopId).map { PendingOfflineProduct(it.syncRevision, it.toDomain()) }

    override suspend fun markOfflineProductSynced(productId: String, revision: Long) {
        productDao.markSynced(productId, revision)
    }

    override suspend fun applyRemoteOrders(orders: List<Order>) {
        if (orders.isEmpty()) return
        orderDao.applyRemote(orders.map { it.toEntity() to it.items.map { item -> item.toEntity(it.id) } })
    }

    override suspend fun applyRemoteProducts(shopId: String, products: List<Product>) {
        if (products.isEmpty()) return
        val blocked = orderDao.pendingProductIds(shopId).toSet()
        products.chunked(SQLITE_MAX_BIND_ARGS).forEach { chunk ->
            productDao.applyRemote(chunk.map { it.toEntity() }, blocked)
        }
    }

    override suspend fun syncState(key: String): String? = orderDao.syncState(key)

    override suspend fun setSyncState(key: String, value: String) {
        orderDao.putSyncState(SyncStateEntity(key, value))
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
    imageUrls = imageUrls.filter { it.isNotBlank() }.take(4).joinToString("|"),
    variantsJson = if (variants.isEmpty()) "" else variantJson.encodeToString(variants.map { it.toDto() }),
    brandName = brandName,
    masterProductId = masterProductId,
    isActive = isActive,
    categoryId = categoryId,
    subCategoryId = subCategoryId,
    translationsJson = if (translations.isEmpty()) "" else variantJson.encodeToString(translations),
    brandTranslationsJson = if (brandTranslations.isEmpty()) "" else variantJson.encodeToString(brandTranslations),
    deletedAt = deletedAt,
)

private fun ShopProductFilter.activeArg(): Int = when (status) {
    ProductStatusFilter.ALL -> -1
    ProductStatusFilter.ACTIVE -> 1
    ProductStatusFilter.INACTIVE -> 0
}

private fun likePattern(query: String): String {
    val escaped = query.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")
    return "%$escaped%"
}

private val variantJson = Json { ignoreUnknownKeys = true }

private fun decodeVariants(raw: String): List<ProductVariant> =
    if (raw.isBlank()) emptyList()
    else runCatching { variantJson.decodeFromString<List<ProductVariantDto>>(raw).map { it.toModel() } }
        .getOrDefault(emptyList())

private fun decodeTranslations(raw: String): Map<String, String> =
    if (raw.isBlank()) emptyMap()
    else runCatching { variantJson.decodeFromString<Map<String, String>>(raw) }.getOrDefault(emptyMap())

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
    imageUrls = imageUrls.split('|').map { it.trim() }.filter { it.isNotBlank() }.take(4),
    variants = decodeVariants(variantsJson),
    brandName = brandName,
    masterProductId = masterProductId,
    isActive = isActive,
    categoryId = categoryId,
    subCategoryId = subCategoryId,
    translations = decodeTranslations(translationsJson),
    brandTranslations = decodeTranslations(brandTranslationsJson),
    deletedAt = deletedAt,
)

private fun UserEntity.toUser() = ShopUser(
    id = id,
    username = username,
    displayName = displayName,
    shopId = shopId,
)

private fun Order.toEntity(syncRevision: Long = 1L) = OrderEntity(
    id = id,
    shopId = shopId,
    createdAt = createdAtEpochMs,
    status = status.name,
    customerName = customerName,
    totalValue = totalValue,
    itemCount = itemCount,
    cancelReason = cancelReason,
    paymentMethod = paymentMethod,
    discount = discount,
    pendingSync = 1,
    syncRevision = syncRevision,
    customerPhone = customerPhone,
    paidNow = paidNow,
    paidNowMethod = paidNowMethod,
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
    variantId = variantId,
    variantLabel = variantLabel,
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
    channel = OrderChannel.POS,
    paymentMethod = paymentMethod,
    customerPhone = customerPhone,
    discount = discount,
    paidNow = paidNow,
    paidNowMethod = paidNowMethod,
    items = items.map {
        OrderItem(
            productId = it.productId,
            productName = it.productName,
            unit = ProductUnit.fromName(it.unit),
            quantity = it.quantity,
            unitPrice = it.unitPrice,
            variantId = it.variantId,
            variantLabel = it.variantLabel,
        )
    },
)
