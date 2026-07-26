package org.bhargav.pansariwala.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.domain.model.CategoryStock
import org.bhargav.pansariwala.domain.model.OrderSummary
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.SalesSnapshot
import org.bhargav.pansariwala.util.AppClock

data class DashboardUiState(
    val loading: Boolean = true,
    val userName: String = "",
    val todaySales: SalesSnapshot = SalesSnapshot(0, 0.0),
    val recentOrders: List<OrderSummary> = emptyList(),
    val categoryBreakdown: List<CategoryStock> = emptyList(),
    val lowStockItems: List<Product> = emptyList(),
    val lowStockTotalCount: Int = 0,
    val totalInventoryValue: Double = 0.0,
    val totalProducts: Int = 0,
)

class DashboardViewModel(
    private val shopRepository: ShopRepository,
    private val preferences: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            shopRepository.ensureSeeded()
            val shopId = preferences.getShopId() ?: SeedData.DEMO_SHOP_ID
            val name = preferences.getDisplayName() ?: "Shopkeeper"
            _uiState.update { it.copy(userName = name) }

            combine(
                shopRepository.observeProducts(shopId),
                shopRepository.observeRecentOrders(shopId, RECENT_ORDERS_LIMIT),
                shopRepository.observeTodaySales(shopId, AppClock.startOfTodayMillis()),
            ) { products, orders, sales ->
                DashboardData(products, orders, sales)
            }.collect { data ->
                val breakdown = data.products
                    .groupBy { it.category }
                    .map { (category, list) ->
                        CategoryStock(
                            category = category,
                            itemCount = list.size,
                            stockValue = list.sumOf { p -> p.stockValue },
                        )
                    }
                    .sortedByDescending { it.stockValue }
                val lowStock = data.products.filter { it.isLowStock }.sortedBy { it.stockQty }

                _uiState.update {
                    it.copy(
                        loading = false,
                        todaySales = data.sales,
                        recentOrders = data.orders,
                        categoryBreakdown = breakdown,
                        lowStockItems = lowStock.take(LOW_STOCK_LIMIT),
                        lowStockTotalCount = lowStock.size,
                        totalInventoryValue = data.products.sumOf { p -> p.stockValue },
                        totalProducts = data.products.size,
                    )
                }
            }
        }
    }

    private data class DashboardData(
        val products: List<Product>,
        val orders: List<OrderSummary>,
        val sales: SalesSnapshot,
    )

    companion object {
        const val RECENT_ORDERS_LIMIT = 10
        const val LOW_STOCK_LIMIT = 15
    }
}
