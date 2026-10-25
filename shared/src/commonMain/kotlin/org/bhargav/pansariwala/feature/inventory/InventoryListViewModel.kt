package org.bhargav.pansariwala.feature.inventory

import kotlinx.coroutines.flow.first
import org.bhargav.pansariwala.voice.SpeechToText
import org.bhargav.pansariwala.feature.catalog.looksLikeBarcode
import org.bhargav.pansariwala.feature.catalog.ProductFinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.MasterCategoryDto
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductStatusFilter
import org.bhargav.pansariwala.domain.model.ShopProductFilter
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.util.AppClock
import org.bhargav.pansariwala.util.AppConstants

data class InventoryListUiState(
    val loading: Boolean = true,
    val searchText: String = "",
    val filter: ShopProductFilter = ShopProductFilter(),
    val products: List<Product> = emptyList(),
    val totalCount: Int = 0,
    /** Admin-defined master categories (roots and sub-categories); empty until loaded or offline. */
    val categories: List<MasterCategoryDto> = emptyList(),
    val error: UiText? = null,
) {
    val hasMore: Boolean get() = products.size < totalCount
    val rootCategories: List<MasterCategoryDto> get() = categories.filter { it.parentId.isNullOrBlank() }
    val categoryNames: Map<String, String> get() = categories.associate { it.id to it.name }
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class InventoryListViewModel(
    private val shopRepository: ShopRepository,
    private val preferences: AppPreferences,
    private val api: PansariApi,
    speech: SpeechToText,
) : ViewModel() {

    /** Voice / barcode lookup; a pick opens that product like tapping its row. */
    val finder = ProductFinder(speech, viewModelScope) {
        shopRepository.observeProducts(preferences.getShopId() ?: SeedData.DEMO_SHOP_ID).first()
    }

    /** Enter in the search field: a hardware scanner's code, otherwise a name / brand / category search. */
    fun onSearchSubmit() {
        val text = _uiState.value.searchText.trim()
        if (text.isEmpty()) return
        if (looksLikeBarcode(text)) finder.onBarcode(text) else finder.search(text)
    }

    override fun onCleared() {
        finder.stopListening()
        super.onCleared()
    }

    private val _uiState = MutableStateFlow(InventoryListUiState())
    val uiState: StateFlow<InventoryListUiState> = _uiState.asStateFlow()

    private val searchText = MutableStateFlow("")
    private val filter = MutableStateFlow(ShopProductFilter())
    private val limit = MutableStateFlow(AppConstants.LIST_PAGE_SIZE)
    private var initialized = false

    /** Applies the route's initial filter once; later filter changes come from the user. */
    fun start(lowStockOnly: Boolean) {
        if (initialized) return
        initialized = true
        filter.value = filter.value.copy(lowStockOnly = lowStockOnly)
        viewModelScope.launch {
            runCatching { api.masterCategories() }
                .onSuccess { all -> _uiState.update { it.copy(categories = all.sortedBy { c -> c.name.lowercase() }) } }
                .onFailure { it.rethrowIfStructuredCancellation() }
        }
        viewModelScope.launch {
            shopRepository.ensureSeeded()
            val shopId = preferences.getShopId() ?: SeedData.DEMO_SHOP_ID
            launch {
                searchText
                    .debounce(AppConstants.INVENTORY_SEARCH_DEBOUNCE_MS)
                    .map { it.trim() }
                    .distinctUntilChanged()
                    .collect { q -> updateFilter { it.copy(query = q) } }
            }
            combine(filter, limit) { f, l -> f to l }
                .flatMapLatest { (f, l) ->
                    combine(
                        shopRepository.observeProductPage(shopId, f, l),
                        shopRepository.observeProductCount(shopId, f),
                    ) { items, total -> Triple(f, items, total) }
                }
                .catch { error -> _uiState.update { it.copy(loading = false, error = error.toApiUiText()) } }
                .collect { (f, items, total) ->
                    _uiState.update {
                        it.copy(loading = false, filter = f, products = items, totalCount = total, error = null)
                    }
                }
        }
    }

    fun onSearchChange(value: String) {
        searchText.value = value
        _uiState.update { it.copy(searchText = value) }
    }

    fun onCategorySelected(categoryId: String?) = updateFilter { it.copy(categoryId = categoryId) }
    fun onStatusSelected(status: ProductStatusFilter) = updateFilter { it.copy(status = status) }
    fun onLowStockToggle() = updateFilter { it.copy(lowStockOnly = !it.lowStockOnly) }
    fun dismissError() = _uiState.update { it.copy(error = null) }

    /** Moves the product to the bin; it leaves this list but keeps its stock and details. */
    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            runCatching { shopRepository.setProductDeleted(productId, AppClock.nowMillis()) }
                .onFailure { error ->
                    error.rethrowIfStructuredCancellation()
                    _uiState.update { it.copy(error = error.toApiUiText()) }
                }
        }
    }

    fun loadMore() {
        val state = _uiState.value
        if (state.hasMore && state.products.size >= limit.value) limit.value += AppConstants.LIST_PAGE_SIZE
    }

    private fun updateFilter(transform: (ShopProductFilter) -> ShopProductFilter) {
        val next = transform(filter.value)
        if (next == filter.value) return
        limit.value = AppConstants.LIST_PAGE_SIZE
        filter.value = next
    }
}
