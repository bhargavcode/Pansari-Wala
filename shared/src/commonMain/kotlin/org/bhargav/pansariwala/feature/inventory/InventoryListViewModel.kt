package org.bhargav.pansariwala.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.ui.AsyncUiState

data class InventoryListData(
    val products: List<Product> = emptyList(),
)

typealias InventoryListUiState = AsyncUiState<InventoryListData>

class InventoryListViewModel(
    private val shopRepository: ShopRepository,
    private val preferences: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow<InventoryListUiState>(AsyncUiState.Idle)
    val uiState: StateFlow<InventoryListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = AsyncUiState.Loading
            shopRepository.ensureSeeded()
            val shopId = preferences.getShopId() ?: SeedData.DEMO_SHOP_ID
            shopRepository.observeProducts(shopId)
                .catch { error -> _uiState.value = AsyncUiState.Error(error.toApiUiText()) }
                .collect { products ->
                    _uiState.value = AsyncUiState.Success(InventoryListData(products))
                }
        }
    }
}
