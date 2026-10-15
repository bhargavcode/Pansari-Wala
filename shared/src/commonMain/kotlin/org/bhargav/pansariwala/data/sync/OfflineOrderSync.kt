package org.bhargav.pansariwala.data.sync

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import org.bhargav.pansariwala.api.OfflineOrderSyncDto
import org.bhargav.pansariwala.api.OfflineProductSyncDto
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.SyncPushRequest
import org.bhargav.pansariwala.api.toDto
import org.bhargav.pansariwala.api.toModel
import org.bhargav.pansariwala.api.toSyncDto
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderChannel
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.util.AppClock
import org.bhargav.pansariwala.util.AppConstants
import kotlin.concurrent.atomics.AtomicBoolean
import kotlin.concurrent.atomics.ExperimentalAtomicApi

private const val PULL_PAGES_PER_CYCLE = 5

/**
 * Wakes [OfflineOrderSync] the moment a POS bill/product is saved or the shop signs in.
 * Conflated so a burst of sales still results in one immediate drain.
 */
@OptIn(ExperimentalAtomicApi::class)
class OrderSyncSignal {
    private val channel = Channel<Unit>(Channel.CONFLATED)
    private val pullRequested = AtomicBoolean(true)

    /** [pull] = also refresh other devices' bills + catalog now (sign-in / shop switch). */
    fun kick(pull: Boolean = false) {
        if (pull) pullRequested.store(true)
        channel.trySend(Unit)
    }

    internal fun consumePullRequest(): Boolean = pullRequested.exchange(false)

    internal fun requestPull() = pullRequested.store(true)

    suspend fun await() {
        channel.receive()
    }
}

class SyncingShopRepository(
    private val inner: ShopRepository,
    private val signal: OrderSyncSignal,
) : ShopRepository by inner {
    override suspend fun saveOrder(order: Order) {
        inner.saveOrder(order)
        signal.kick()
    }

    override suspend fun upsertProduct(product: Product) {
        inner.upsertProduct(product)
        signal.kick()
    }
}

private enum class Drain {
    PROGRESS,
    IDLE,
    RETRY,
}

/**
 * Two-way POS sync in one background worker (never blocks UI):
 * - Push: every local bill / inventory edit stays pending until the server confirms that
 *   exact revision; retries with backoff plus a periodic sweep.
 * - Pull: after the push queue is empty, pages other devices' bills (cursor in Room) and
 *   refreshes the catalog so dashboard, reports and stock match on any device.
 */
@OptIn(ExperimentalAtomicApi::class)
class OfflineOrderSync(
    private val repository: ShopRepository,
    private val api: PansariApi,
    private val preferences: AppPreferences,
    private val signal: OrderSyncSignal,
) {
    private val started = AtomicBoolean(false)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private var pulledShopId: String? = null
    private var lastOrderPullAt = 0L
    private var lastCatalogPullAt = 0L
    private var pullContinues = false

    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch { run() }
    }

    private suspend fun run() {
        var failureDelay = 0L
        while (currentCoroutineContext().isActive) {
            val next = when (drain()) {
                Drain.PROGRESS -> 0L
                Drain.IDLE -> pullIfDue()
                Drain.RETRY -> -1L
            }
            if (next >= 0L) failureDelay = 0L else {
                failureDelay = if (failureDelay == 0L) {
                    AppConstants.OFFLINE_SYNC_RETRY_MS
                } else {
                    (failureDelay * 2).coerceAtMost(AppConstants.OFFLINE_SYNC_RETRY_MAX_MS)
                }
            }
            val wait = if (next >= 0L) next else failureDelay
            if (wait > 0L) withTimeoutOrNull(wait) { signal.await() }
        }
    }

    private suspend fun syncShopId(): String? {
        val token = preferences.getAccessToken().orEmpty()
        if (!token.startsWith(AppConstants.JWT_PREFIX)) return null
        return preferences.getShopId()
    }

    private suspend fun drain(): Drain {
        val shopId = syncShopId() ?: return Drain.IDLE
        val orders = repository.pendingOfflineOrders(shopId).take(AppConstants.OFFLINE_SYNC_BATCH)
        val products = repository.pendingOfflineProducts(shopId).take(AppConstants.OFFLINE_SYNC_BATCH)
        if (orders.isEmpty() && products.isEmpty()) return Drain.IDLE
        return try {
            val response = api.pushSync(
                SyncPushRequest(
                    offlineOrders = orders.map { OfflineOrderSyncDto(it.revision, it.order.toSyncDto()) },
                    offlineProducts = products.map { OfflineProductSyncDto(it.revision, it.product.toDto()) },
                ),
            )
            response.accepted.forEach { repository.markOfflineOrderSynced(it.id, it.revision) }
            response.acceptedProducts.forEach { repository.markOfflineProductSynced(it.id, it.revision) }
            if (response.accepted.isEmpty() && response.acceptedProducts.isEmpty()) Drain.RETRY else Drain.PROGRESS
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            Drain.RETRY
        }
    }

    /** Returns ms until the next pull is due, 0 to continue now, or -1 to back off after a failure. */
    private suspend fun pullIfDue(): Long {
        val shopId = syncShopId() ?: return AppConstants.OFFLINE_SYNC_FALLBACK_MS
        val forced = signal.consumePullRequest() || shopId != pulledShopId
        val now = AppClock.nowMillis()
        val ordersDue = forced || pullContinues || now - lastOrderPullAt >= AppConstants.SYNC_PULL_INTERVAL_MS
        val catalogDue = forced || now - lastCatalogPullAt >= AppConstants.SYNC_CATALOG_INTERVAL_MS
        if (!ordersDue && !catalogDue) return nextPullDelay(now)
        return try {
            if (catalogDue) {
                repository.applyRemoteProducts(shopId, api.shopCatalog(shopId))
                lastCatalogPullAt = AppClock.nowMillis()
            }
            if (ordersDue) {
                pullContinues = pullOrderPages(shopId)
                lastOrderPullAt = AppClock.nowMillis()
            }
            pulledShopId = shopId
            if (pullContinues) 0L else nextPullDelay(AppClock.nowMillis())
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            if (forced) signal.requestPull()
            -1L
        }
    }

    /** Pulls a few pages per cycle so new local sales are pushed between pages. True if more remain. */
    private suspend fun pullOrderPages(shopId: String): Boolean {
        val key = AppConstants.SYNC_ORDERS_CURSOR_KEY_PREFIX + shopId
        val cursor = repository.syncState(key)
        var since = cursor?.substringBefore('|')?.toLongOrNull() ?: -1L
        var afterId = cursor?.substringAfter('|', "").orEmpty()
        repeat(PULL_PAGES_PER_CYCLE) {
            val page = api.syncOrders(since, afterId, AppConstants.SYNC_PULL_PAGE_SIZE)
            repository.applyRemoteOrders(
                page.orders.map { it.toModel().copy(channel = OrderChannel.POS) },
            )
            since = page.nextSince
            afterId = page.nextAfterId
            repository.setSyncState(key, "$since|$afterId")
            if (!page.hasMore) return false
        }
        return true
    }

    private fun nextPullDelay(now: Long): Long {
        val untilOrders = AppConstants.SYNC_PULL_INTERVAL_MS - (now - lastOrderPullAt)
        val untilCatalog = AppConstants.SYNC_CATALOG_INTERVAL_MS - (now - lastCatalogPullAt)
        return minOf(untilOrders, untilCatalog, AppConstants.OFFLINE_SYNC_FALLBACK_MS).coerceAtLeast(1L)
    }
}
