package org.bhargav.pansariwala.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductCategory
import org.bhargav.pansariwala.domain.model.ProductDetail
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.ui.AsyncUiState
import org.bhargav.pansariwala.ui.beginLoad
import org.bhargav.pansariwala.util.AppConstants

data class CategoryProductsUiState(
    val category: ProductCategory? = null,
    val query: String = "",
    val items: List<Product> = emptyList(),
    val totalCount: Int = 0,
    val nextPage: Int = 0,
    val hasMore: Boolean = true,
    val loadingInitial: Boolean = false,
    val loadingMore: Boolean = false,
    val error: UiText? = null,
)

class CategoryProductsViewModel(
    private val api: PansariApi,
    private val pageSize: Int = AppConstants.CATALOG_PAGE_SIZE,
) : ViewModel() {
    private val _state = MutableStateFlow(CategoryProductsUiState())
    val state: StateFlow<CategoryProductsUiState> = _state.asStateFlow()

    private var shopId: String? = null
    private var pageJob: Job? = null
    private var searchJob: Job? = null
    /** Bumped on every reset so responses from a superseded query are dropped. */
    private var generation = 0

    fun start(shopId: String, category: ProductCategory?, initialQuery: String) {
        if (this.shopId == shopId && _state.value.category == category) return
        this.shopId = shopId
        _state.update { CategoryProductsUiState(category = category, query = initialQuery) }
        reset()
    }

    fun setQuery(value: String) {
        if (value == _state.value.query) return
        _state.update { it.copy(query = value) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(AppConstants.CATALOG_SEARCH_DEBOUNCE_MS)
            reset()
        }
    }

    fun retry() {
        if (_state.value.items.isEmpty()) reset() else loadNextPage()
    }

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    fun loadNextPage() {
        val current = _state.value
        if (!current.hasMore || current.loadingInitial || current.loadingMore || pageJob?.isActive == true) return
        fetch(page = current.nextPage, gen = generation)
    }

    private fun reset() {
        pageJob?.cancel()
        generation++
        _state.update {
            it.copy(items = emptyList(), nextPage = 0, hasMore = true, totalCount = 0, error = null)
        }
        fetch(page = 0, gen = generation)
    }

    private fun fetch(page: Int, gen: Int) {
        val id = shopId ?: return
        val snapshot = _state.value
        _state.update { it.copy(loadingInitial = page == 0, loadingMore = page > 0, error = null) }
        pageJob = viewModelScope.launch {
            runCatching {
                api.catalogPage(id, snapshot.category, snapshot.query.trim(), page, pageSize)
            }.onSuccess { result ->
                if (gen != generation) return@onSuccess
                _state.update { s ->
                    val known = s.items.mapTo(HashSet()) { it.id }
                    s.copy(
                        items = s.items + result.items.filter { it.id !in known },
                        totalCount = result.totalCount,
                        nextPage = page + 1,
                        hasMore = result.hasMore,
                        loadingInitial = false,
                        loadingMore = false,
                    )
                }
            }.onFailure { err ->
                err.rethrowIfStructuredCancellation()
                if (gen != generation) return@onFailure
                _state.update { it.copy(loadingInitial = false, loadingMore = false, error = err.toApiUiText()) }
            }
        }
    }
}

class ProductDetailViewModel(
    private val api: PansariApi,
) : ViewModel() {
    private val _state = MutableStateFlow<AsyncUiState<ProductDetail>>(AsyncUiState.Idle)
    val state: StateFlow<AsyncUiState<ProductDetail>> = _state.asStateFlow()

    private var key: Pair<String, String>? = null

    fun load(shopId: String, productId: String, force: Boolean = false) {
        val next = shopId to productId
        if (!force && key == next && _state.value is AsyncUiState.Success) return
        key = next
        viewModelScope.launch {
            _state.value = _state.value.beginLoad()
            runCatching { api.shopProductDetail(shopId, productId) }
                .onSuccess { _state.value = AsyncUiState.Success(it) }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    val message = err.toApiUiText()
                    _state.value = when (val current = _state.value) {
                        is AsyncUiState.Success -> current.copy(isRefreshing = false, bannerError = message)
                        else -> AsyncUiState.Error(message)
                    }
                }
        }
    }

    fun retry() {
        key?.let { (shop, product) -> load(shop, product, force = true) }
    }

    fun dismissError() {
        _state.value = when (val current = _state.value) {
            is AsyncUiState.Error -> AsyncUiState.Idle
            is AsyncUiState.Success -> current.copy(bannerError = null)
            else -> current
        }
    }
}
