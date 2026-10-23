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
import kotlinx.coroutines.withTimeoutOrNull
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
import org.bhargav.pansariwala.feature.catalog.ProductDetailsDraft
import org.bhargav.pansariwala.feature.catalog.legacyCategory
import org.bhargav.pansariwala.feature.catalog.resolveCategory
import org.bhargav.pansariwala.feature.catalog.rootCategories
import org.bhargav.pansariwala.feature.catalog.toDraftInput
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.generateId
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.inv_master_added_many
import pansariwala.shared.generated.resources.inv_msg_added
import org.bhargav.pansariwala.i18n.customAppLocale

data class MasterCatalogUiState(
    val searchText: String = "",
    /** Root categories for the filter row; [allCategories] also holds sub-categories. */
    val categories: List<MasterCategoryDto> = emptyList(),
    val allCategories: List<MasterCategoryDto> = emptyList(),
    /** Defaults to this shop's own type once the profile loads; null = every type. */
    val selectedShopTypeId: String? = null,
    val selectedCategoryId: String? = null,
    val selectedSubcategoryId: String? = null,
    val items: List<MasterProductDto> = emptyList(),
    val totalCount: Int = 0,
    val hasMore: Boolean = false,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    /** masterProductId → shop productId for items already in this shop. */
    val addedIds: Map<String, String> = emptyMap(),
    val pending: MasterProductDto? = null,
    val draft: ProductDetailsDraft = ProductDetailsDraft(),
    /** Master items ticked for "Add all"; already-added items are never selectable. */
    val selected: Map<String, MasterProductDto> = emptyMap(),
    val addingAll: Boolean = false,
    val message: UiText? = null,
    val error: UiText? = null,
) {
    val visibleCategories: List<MasterCategoryDto>
        get() = categories.filter { c ->
            selectedShopTypeId == null || c.shopTypeIds.isEmpty() || selectedShopTypeId in c.shopTypeIds
        }

    val subcategories: List<MasterCategoryDto>
        get() = selectedCategoryId?.let { id -> allCategories.filter { it.parentId == id } }.orEmpty()
}

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
            runCatching { withTimeoutOrNull(AppConstants.SHOP_TYPE_LOOKUP_TIMEOUT_MS) { api.shopProfile().shop.shopType } }
                .getOrNull()
                ?.let { type -> _uiState.update { it.copy(selectedShopTypeId = type.ifBlank { null }) } }
            reload()
            launch {
                runCatching { api.masterCategories() }
                    .onSuccess { all ->
                        _uiState.update { s -> s.copy(categories = all.rootCategories, allCategories = all) }
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

    fun onShopTypeSelected(shopTypeId: String?) {
        if (_uiState.value.selectedShopTypeId == shopTypeId) return
        _uiState.update { it.copy(selectedShopTypeId = shopTypeId, selectedCategoryId = null, selectedSubcategoryId = null) }
        reload()
    }

    fun onCategorySelected(categoryId: String?) {
        if (_uiState.value.selectedCategoryId == categoryId) return
        _uiState.update { it.copy(selectedCategoryId = categoryId, selectedSubcategoryId = null) }
        reload()
    }

    fun onSubcategorySelected(subcategoryId: String?) {
        if (_uiState.value.selectedSubcategoryId == subcategoryId) return
        _uiState.update { it.copy(selectedSubcategoryId = subcategoryId) }
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
                shopTypeId = state.selectedShopTypeId,
                subcategoryId = state.selectedSubcategoryId,
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
        _uiState.update { it.copy(pending = item, draft = defaultDraft(item, it.allCategories)) }
    }

    fun cancelAdd() = _uiState.update { it.copy(pending = null) }
    fun updateDraft(transform: (ProductDetailsDraft) -> ProductDetailsDraft) =
        _uiState.update { it.copy(draft = transform(it.draft)) }

    fun toggleSelected(item: MasterProductDto) = _uiState.update { s ->
        if (item.id in s.addedIds) return@update s
        s.copy(selected = if (item.id in s.selected) s.selected - item.id else s.selected + (item.id to item))
    }

    fun clearSelection() = _uiState.update { it.copy(selected = emptyMap()) }

    fun confirmAdd() {
        val state = _uiState.value
        val item = state.pending ?: return
        val product = item.toShopProduct(state.draft, state.allCategories)
        viewModelScope.launch {
            runCatching { shopRepository.upsertProduct(product) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            pending = null,
                            selected = it.selected - item.id,
                            addedIds = it.addedIds + (item.id to product.id),
                            message = UiText.res(Res.string.inv_msg_added, item.displayName(customAppLocale)),
                        )
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.toApiUiText()) } }
        }
    }

    /** Adds every ticked item with master defaults; items without a price are priced at first billing. */
    fun addAllSelected() {
        val state = _uiState.value
        if (state.addingAll) return
        val items = state.selected.values.filter { it.id !in state.addedIds }
        if (items.isEmpty()) return
        _uiState.update { it.copy(addingAll = true) }
        viewModelScope.launch {
            val added = mutableMapOf<String, String>()
            var failure: Throwable? = null
            for (item in items) {
                val product = item.toShopProduct(defaultDraft(item, state.allCategories), state.allCategories)
                runCatching { shopRepository.upsertProduct(product) }
                    .onSuccess { added[item.id] = product.id }
                    .onFailure { failure = it }
            }
            _uiState.update {
                it.copy(
                    addingAll = false,
                    selected = it.selected - added.keys,
                    addedIds = it.addedIds + added,
                    message = if (added.isEmpty()) it.message else UiText.res(Res.string.inv_master_added_many, added.size),
                    error = failure?.toApiUiText() ?: it.error,
                )
            }
        }
    }

    /** Opening stock goes on the first option for variant products, otherwise customers see them out of stock. */
    private fun MasterProductDto.toShopProduct(draft: ProductDetailsDraft, categories: List<MasterCategoryDto>): Product {
        val product = toShopProductBase(draft, categories)
        val firstStockVariant = product.stockVariants.firstOrNull() ?: return product
        if (product.totalStock > 0.0 || draft.stockQtyValue <= 0.0) return product
        return product.withStock(firstStockVariant.id, draft.stockQtyValue)
    }

    private fun MasterProductDto.toShopProductBase(draft: ProductDetailsDraft, categories: List<MasterCategoryDto>) = Product(
        id = generateId("prod"),
        shopId = shopId,
        name = name,
        nameHi = nameHi,
        category = categories.legacyCategory(draft.categoryId, ProductCategory.fromMasterCategoryName(categoryId)),
        unit = ProductUnit.fromName(unit),
        barcode = barcode?.takeIf { it.isNotBlank() },
        sellingPrice = draft.sellingPriceValue,
        costPrice = draft.costPriceValue,
        stockQty = draft.stockQtyValue,
        lowStockThreshold = draft.lowStockValue,
        voiceAlias = null,
        imageUrls = imageUrls.filter { it.isNotBlank() }.take(AppConstants.PHOTO_MAX_PRODUCT_IMAGES),
        variants = variants.filter { it.active }.map { it.toModel() },
        brandName = brandName,
        masterProductId = id,
        categoryId = draft.categoryId,
        subCategoryId = draft.subCategoryId,
    )
}

private fun defaultDraft(item: MasterProductDto, categories: List<MasterCategoryDto>): ProductDetailsDraft {
    val price = item.salePrice.takeIf { it > 0.0 } ?: item.variants.minOfOrNull { it.price } ?: 0.0
    val (categoryId, subCategoryId) = categories.resolveCategory(item.categoryId, item.subcategoryId)
    return ProductDetailsDraft(
        sellingPrice = price.toDraftInput(),
        costPrice = item.cost.toDraftInput(),
        lowStockThreshold = item.lowStockThreshold.toDraftInput(),
        categoryId = categoryId,
        subCategoryId = subCategoryId,
    )
}
