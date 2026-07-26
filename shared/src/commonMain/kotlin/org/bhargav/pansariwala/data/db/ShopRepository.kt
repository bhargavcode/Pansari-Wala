package org.bhargav.pansariwala.data.db

import kotlinx.coroutines.flow.Flow
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderSummary
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.SalesSnapshot
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

    suspend fun productCount(shopId: String): Int
    suspend fun findProduct(idOrBarcode: String): Product?
    suspend fun upsertProduct(product: Product)

    suspend fun getOrder(orderId: String): Order?
    suspend fun saveOrder(order: Order)
}

expect fun createShopRepository(): ShopRepository
