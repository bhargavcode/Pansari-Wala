package org.bhargav.pansariwala.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.MasterCategoryDto
import org.bhargav.pansariwala.api.MasterProductDto
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.api.toModel
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductCategory
import org.bhargav.pansariwala.domain.model.ProductUnit
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.generateId
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.inv_msg_added

data class MasterAddDraft(
    val sellingPrice: String = "",
    val costPrice: String = "",
    val stockQty: String = "",
    val lowStockThreshold: String = "",
    val category: ProductCategory = ProductCategory.GENERAL_GROCERY,
)

data class MasterCatalogUiState(
    val searchText: String = "",
    val categories: List<MasterCategoryDto> = emptyList(),
    val selectedCategoryId: String? = null,
    val items: List<MasterProductDto> = emptyList(),
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    /** masterProductId → shop productId for items already in this shop. */
    val addedIds: Map<String, String> = emptyMap(),
    val pending: MasterProductDto? = null,
    val draft: MasterAddDraft = MasterAddDraft(),
    val message: UiText? = null,
    val error: UiText? = null,
)

@OptIn(FlowPreview::class)
class MasterCatalogViewModel(
    private val api: PansariApi,
    private val shopRepository: ShopRepository,
    private val preferences: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(MasterCatalogUiState())
    val uiState: StateFlow<MasterCatalogUiState> = _uiState.asStateFlow()

    private val searchText = MutableStateFlow("")
    private var shopId: String = SeedData.DEMO_SHOP_ID
    private var nextPage = 0
    private var pageJob: Job? = null

    init {
        viewModelScope.launch {
            shopId = preferences.getShopId() ?: SeedData.DEMO_SHOP_ID
            reload()
            launch {
                runCatching { api.masterCategories() }
                    .onSuccess { all ->
                        _uiState.update { s -> s.copy(categories = all.filter { it.parentId.isNullOrBlank() }) }
                    }
            }
            searchText
                .drop(1)
                .debounce(AppConstants.CATALOG_SEARCH_DEBOUNCE_MS)
                .map { it.trim() }
                .distinctUntilChanged()
                .collect { reload() }
        }
    }

    fun onSearchChange(value: String) {
        _uiState.update { it.copy(searchText = value) }
        searchText.value = value
    }

    fun onCategorySelected(categoryId: String?) {
        if (_uiState.value.selectedCategoryId == categoryId) return
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
        reload()
    }

    fun retry() = reload()
    fun dismissError() = _uiState.update { it.copy(error = null) }
    fun dismissMessage() = _uiState.update { it.copy(message = null) }

    fun loadMore() {
        val state = _uiState.value
        if (state.loading || state.loadingMore || !state.hasMore) return
        _uiState.update { it.copy(loadingMore = true) }
        pageJob = viewModelScope.launch { fetchPage(append = true) }
    }

    private fun reload() {
        pageJob?.cancel()
        nextPage = 0
        _uiState.update { it.copy(loading = true, loadingMore = false, items = emptyList(), hasMore = false, error = null) }
        pageJob = viewModelScope.launch { fetchPage(append = false) }
    }

    private suspend fun fetchPage(append: Boolean) {
        val state = _uiState.value
        runCatching {
            api.masterProductPage(
                query = state.searchText.trim(),
                categoryId = state.selectedCategoryId,
                page = nextPage,
                pageSize = AppConstants.MASTER_PAGE_SIZE,
            )
        }.onSuccess { page ->
            val links = shopRepository.productIdsByMasterIds(shopId, page.items.map { it.id })
            nextPage = page.page + 1
            _uiState.update { s ->
                val merged = if (append) (s.items + page.items).distinctBy { it.id } else page.items
                s.copy(
                    items = merged,
                    totalCount = page.totalCount,
                    hasMore = page.hasMore,
                    loading = false,
                    loadingMore = false,
                    addedIds = s.addedIds + links,
                )
            }
        }.onFailure { error ->
            if (error is kotlinx.coroutines.CancellationException) throw error
            _uiState.update { it.copy(loading = false, loadingMore = false, error = error.toApiUiText()) }
        }
    }

    fun startAdd(item: MasterProductDto) {
        val price = item.salePrice.takeIf { it > 0.0 } ?: item.variants.minOfOrNull { it.price } ?: 0.0
        _uiState.update {
            it.copy(
                pending = item,
                draft = MasterAddDraft(
                    sellingPrice = price.toInputString(),
                    costPrice = item.cost.toInputString(),
                    stockQty = "",
                    lowStockThreshold = item.lowStockThreshold.toInputString(),
                    category = guessCategory(item.categoryId, it.categories),
                ),
            )
        }
    }

    fun cancelAdd() = _uiState.update { it.copy(pending = null) }
    fun updateDraft(transform: (MasterAddDraft) -> MasterAddDraft) = _uiState.update { it.copy(draft = transform(it.draft)) }

    fun confirmAdd() {
        val state = _uiState.value
        val item = state.pending ?: return
        val draft = state.draft
        val product = Product(
            id = generateId("prod"),
            shopId = shopId,
            name = item.name,
            nameHi = item.nameHi,
            category = draft.category,
            unit = ProductUnit.fromName(item.unit),
            barcode = item.barcode?.takeIf { it.isNotBlank() },
            sellingPrice = draft.sellingPrice.toDoubleOrNull() ?: 0.0,
            costPrice = draft.costPrice.toDoubleOrNull() ?: 0.0,
            stockQty = draft.stockQty.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
            lowStockThreshold = draft.lowStockThreshold.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
            voiceAlias = null,
            imageUrls = item.imageUrls.filter { it.isNotBlank() }.take(AppConstants.PHOTO_MAX_PRODUCT_IMAGES),
            variants = item.variants.filter { it.active }.map { it.toModel() },
            brandName = item.brandName,
            masterProductId = item.id,
        )
        viewModelScope.launch {
            runCatching { shopRepository.upsertProduct(product) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            pending = null,
                            addedIds = it.addedIds + (item.id to product.id),
                            message = UiText.res(Res.string.inv_msg_added, item.name),
                        )
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.toApiUiText()) } }
        }
    }
}

private fun Double.toInputString(): String = when {
    this <= 0.0 -> ""
    this == this.toLong().toDouble() -> this.toLong().toString()
    else -> this.toString()
}

/** Master categories are admin-defined; map by name onto the shop's fixed categories. */
private fun guessCategory(categoryId: String, categories: List<MasterCategoryDto>): ProductCategory {
    val name = categories.firstOrNull { it.id == categoryId }?.name ?: categoryId
    val key = name.trim().lowercase()
    if (key.isEmpty()) return ProductCategory.GENERAL_GROCERY
    return ProductCategory.entries.firstOrNull { category ->
        val display = category.displayName.lowercase()
        category.name.equals(key.replace(' ', '_'), ignoreCase = true) ||
            display.contains(key) || key.contains(display) ||
            display.split(" / ", " ").any { it.length > 3 && key.contains(it) }
    } ?: ProductCategory.GENERAL_GROCERY
}
