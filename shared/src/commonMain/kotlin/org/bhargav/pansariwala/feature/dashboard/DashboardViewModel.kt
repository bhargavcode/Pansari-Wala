package org.bhargav.pansariwala.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.domain.model.CategoryStock
import org.bhargav.pansariwala.domain.model.OrderSummary
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.SalesSnapshot
import org.bhargav.pansariwala.ui.AsyncUiState
import org.bhargav.pansariwala.util.AppClock

data class DashboardData(
    val userName: String = "",
    val todaySales: SalesSnapshot = SalesSnapshot(0, 0.0),
    val recentOrders: List<OrderSummary> = emptyList(),
    val categoryBreakdown: List<CategoryStock> = emptyList(),
    val lowStockItems: List<Product> = emptyList(),
    val lowStockTotalCount: Int = 0,
    val totalInventoryValue: Double = 0.0,
    val totalProducts: Int = 0,
)

typealias DashboardUiState = AsyncUiState<DashboardData>

class DashboardViewModel(
    private val shopRepository: ShopRepository,
    private val preferences: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow<DashboardUiState>(AsyncUiState.Idle)
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = AsyncUiState.Loading
            shopRepository.ensureSeeded()
            val shopId = preferences.getShopId() ?: SeedData.DEMO_SHOP_ID
            val name = preferences.getDisplayName().orEmpty()

            combine(
                shopRepository.observeProducts(shopId),
                shopRepository.observeRecentOrders(shopId, RECENT_ORDERS_LIMIT),
                shopRepository.observeTodaySales(shopId, AppClock.startOfTodayMillis()),
            ) { products, orders, sales ->
                DashboardPayload(products, orders, sales)
            }.catch { error ->
                _uiState.value = AsyncUiState.Error(error.toApiUiText())
            }.collect { payload ->
                val breakdown = payload.products
                    .groupBy { it.category }
                    .map { (category, list) ->
                        CategoryStock(
                            category = category,
                            itemCount = list.size,
                            stockValue = list.sumOf { p -> p.stockValue },
                        )
                    }
                    .sortedByDescending { it.stockValue }
                val lowStock = payload.products.filter { it.isLowStock }.sortedBy { it.stockQty }

                _uiState.value = AsyncUiState.Success(
                    DashboardData(
                        userName = name,
                        todaySales = payload.sales,
                        recentOrders = payload.orders,
                        categoryBreakdown = breakdown,
                        lowStockItems = lowStock.take(LOW_STOCK_LIMIT),
                        lowStockTotalCount = lowStock.size,
                        totalInventoryValue = payload.products.sumOf { p -> p.stockValue },
                        totalProducts = payload.products.size,
                    ),
                )
            }
        }
    }

    private data class DashboardPayload(
        val products: List<Product>,
        val orders: List<OrderSummary>,
        val sales: SalesSnapshot,
    )

    companion object {
        const val RECENT_ORDERS_LIMIT = 10
        const val LOW_STOCK_LIMIT = 15
    }
}
