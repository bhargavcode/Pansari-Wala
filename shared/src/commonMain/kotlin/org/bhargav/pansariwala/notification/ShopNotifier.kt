package org.bhargav.pansariwala.notification

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.util.generateId
import org.jetbrains.compose.resources.getString
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.notif_low_stock_body
import pansariwala.shared.generated.resources.notif_low_stock_title
import pansariwala.shared.generated.resources.notif_order_cancelled_title
import pansariwala.shared.generated.resources.notif_order_cancelled_body

class ShopNotifier(
    private val gateway: NotificationGateway,
    private val preferences: AppPreferences,
    private val shopRepository: ShopRepository,
) {
    private val background = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Counter sales need no "new order" alert; only the sold items are re-checked for low stock. */
    fun onPosSaleSaved(order: Order) {
        val productIds = order.items.map { it.productId }
        if (productIds.isEmpty()) return
        background.launch {
            if (!preferences.getUserSettings().notifyLowStock) return@launch
            val low = shopRepository.productsByIds(productIds)
                .filter { it.stockQty <= 0.0 || it.isLowStock }
            if (low.isEmpty()) return@launch
            gateway.show(
                ShopNotification(
                    id = generateId("notif"),
                    title = getString(Res.string.notif_low_stock_title),
                    body = getString(Res.string.notif_low_stock_body, low.joinToString { it.name }),
                ),
            )
        }
    }

    suspend fun onOrderCancelled(order: Order) {
        val settings = preferences.getUserSettings()
        if (!settings.notifyOrderEvents) return
        gateway.show(
            ShopNotification(
                id = generateId("notif"),
                title = getString(Res.string.notif_order_cancelled_title),
                body = getString(Res.string.notif_order_cancelled_body, order.id.takeLast(6)),
                orderId = order.id,
            ),
        )
    }
}
