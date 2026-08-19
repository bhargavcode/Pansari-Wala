package org.bhargav.pansariwala.notification

import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.generateId

class ShopNotifier(
    private val gateway: NotificationGateway,
    private val preferences: AppPreferences,
) {
    suspend fun onOrderSaved(order: Order, catalogAfter: List<Product>) {
        val settings = preferences.getUserSettings()
        if (settings.notifyOrderEvents) {
            gateway.show(
                ShopNotification(
                    id = generateId("notif"),
                    title = "Order received",
                    body = "New order ${order.id.takeLast(6)} · ${order.totalValue.asMoney()}",
                    orderId = order.id,
                ),
            )
        }
        if (settings.notifyLowStock) {
            order.items.mapNotNull { line ->
                catalogAfter.firstOrNull { it.id == line.productId }
            }.filter { it.stockQty <= 0.0 || it.isLowStock }
                .distinctBy { it.id }
                .forEach { product ->
                    gateway.show(
                        ShopNotification(
                            id = generateId("notif"),
                            title = "Low stock alert",
                            body = "${product.name} is low or out of stock",
                            orderId = order.id,
                        ),
                    )
                }
        }
    }

    suspend fun onOrderCancelled(order: Order) {
        val settings = preferences.getUserSettings()
        if (!settings.notifyOrderEvents) return
        gateway.show(
            ShopNotification(
                id = generateId("notif"),
                title = "Order cancelled",
                body = "Order ${order.id.takeLast(6)} was cancelled",
                orderId = order.id,
            ),
        )
    }
}
