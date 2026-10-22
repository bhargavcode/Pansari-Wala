package org.bhargav.pansariwala.feature.reports

import org.bhargav.pansariwala.domain.model.ItemSale
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderChannel
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.domain.model.ProductCategory

data class CategorySales(
    val category: ProductCategory,
    val revenue: Double,
    val quantity: Double,
    val orderCount: Int,
    val productCount: Int,
)

data class ProductSales(
    val productId: String,
    val name: String,
    val revenue: Double,
    val quantity: Double,
    val orderCount: Int,
)

object SalesBreakdown {
    /** Online orders that became sales (paid, not cancelled / rejected) as sold lines. */
    fun onlineItemSales(orders: List<Order>): List<ItemSale> = orders
        .filter { it.status !in NON_SALE_STATUSES }
        .flatMap { order ->
            order.items.map { item ->
                ItemSale(
                    orderId = order.id,
                    productId = item.productId,
                    productName = item.productName,
                    variantLabel = item.variantLabel,
                    quantity = item.quantity,
                    lineTotal = item.lineTotal,
                    createdAtEpochMs = order.createdAtEpochMs,
                    channel = OrderChannel.ONLINE,
                )
            }
        }

    fun filterChannel(items: List<ItemSale>, channel: ReportChannel): List<ItemSale> = when (channel) {
        ReportChannel.ALL -> items
        ReportChannel.ONLINE -> items.filter { it.channel == OrderChannel.ONLINE }
        ReportChannel.OFFLINE -> items.filter { it.channel != OrderChannel.ONLINE }
    }

    fun byCategory(items: List<ItemSale>, categoryOf: (String) -> ProductCategory): List<CategorySales> =
        items.groupBy { categoryOf(it.productId) }
            .map { (category, lines) ->
                CategorySales(
                    category = category,
                    revenue = lines.sumOf { it.lineTotal },
                    quantity = lines.sumOf { it.quantity },
                    orderCount = lines.map { it.orderId }.distinct().size,
                    productCount = lines.map { it.productId }.distinct().size,
                )
            }
            .sortedByDescending { it.revenue }

    fun byProduct(items: List<ItemSale>): List<ProductSales> =
        items.groupBy { it.productId }
            .map { (productId, lines) ->
                ProductSales(
                    productId = productId,
                    name = lines.maxBy { it.createdAtEpochMs }.productName,
                    revenue = lines.sumOf { it.lineTotal },
                    quantity = lines.sumOf { it.quantity },
                    orderCount = lines.map { it.orderId }.distinct().size,
                )
            }
            .sortedWith(compareByDescending<ProductSales> { it.orderCount }.thenByDescending { it.revenue })

    /** One [ReportOrder] per order containing the product, so the shared bucket math gives order counts per bar. */
    fun asOrders(items: List<ItemSale>): List<ReportOrder> =
        items.groupBy { it.channel to it.orderId }.map { (key, lines) ->
            ReportOrder(
                id = key.second,
                displayNumber = "",
                createdAtEpochMs = lines.first().createdAtEpochMs,
                channel = key.first,
                customerName = null,
                itemCount = lines.size,
                total = lines.sumOf { it.lineTotal },
                status = OrderStatus.COMPLETED,
            )
        }

    private val NON_SALE_STATUSES = setOf(
        OrderStatus.CANCELLED,
        OrderStatus.REJECTED,
        OrderStatus.DRAFT,
        OrderStatus.PAYMENT_PENDING,
    )
}
