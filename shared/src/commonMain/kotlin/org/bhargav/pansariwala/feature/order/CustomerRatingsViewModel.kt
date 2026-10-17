package org.bhargav.pansariwala.feature.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.CustomerRatingDto
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.util.AppConstants

data class CustomerRatingsUiState(
    val items: List<CustomerRatingDto> = emptyList(),
    val selectedStars: Set<Int> = emptySet(),
    val average: Double = 0.0,
    val ratingCount: Int = 0,
    val starCounts: Map<Int, Int> = emptyMap(),
    val nextPage: Int = 0,
    val hasMore: Boolean = true,
    val loadingInitial: Boolean = false,
    val loadingMore: Boolean = false,
    val error: UiText? = null,
)

class CustomerRatingsViewModel(
    private val api: PansariApi,
) : ViewModel() {
    private val _state = MutableStateFlow(CustomerRatingsUiState())
    val state: StateFlow<CustomerRatingsUiState> = _state.asStateFlow()

    private var customerId: String? = null
    private var loadJob: Job? = null

    fun start(customerId: String) {
        if (this.customerId == customerId) return
        this.customerId = customerId
        reload()
    }

    fun toggleStars(stars: Int) {
        _state.update {
            val next = if (stars in it.selectedStars) it.selectedStars - stars else it.selectedStars + stars
            it.copy(selectedStars = next)
        }
        reload()
    }

    fun clearFilter() {
        if (_state.value.selectedStars.isEmpty()) return
        _state.update { it.copy(selectedStars = emptySet()) }
        reload()
    }

    fun retry() = if (_state.value.items.isEmpty()) reload() else loadNextPage()

    fun dismissError() {
        _state.update { it.copy(error = null) }
    }

    private fun reload() {
        loadJob?.cancel()
        _state.update { it.copy(items = emptyList(), nextPage = 0, hasMore = true, loadingMore = false) }
        loadPage(initial = true)
    }

    fun loadNextPage() {
        val s = _state.value
        if (!s.hasMore || s.loadingInitial || s.loadingMore) return
        loadPage(initial = false)
    }

    private fun loadPage(initial: Boolean) {
        val id = customerId ?: return
        val page = _state.value.nextPage
        val stars = _state.value.selectedStars
        _state.update { it.copy(loadingInitial = initial, loadingMore = !initial, error = null) }
        loadJob = viewModelScope.launch {
            runCatching { api.customerRatings(id, stars, page, AppConstants.CUSTOMER_RATINGS_PAGE_SIZE) }
                .onSuccess { result ->
                    _state.update {
                        val seen = it.items.mapTo(HashSet()) { item -> item.orderId }
                        it.copy(
                            items = it.items + result.items.filterNot { item -> item.orderId in seen },
                            average = result.average,
                            ratingCount = result.ratingCount,
                            starCounts = result.starCounts,
                            nextPage = page + 1,
                            hasMore = result.hasMore,
                            loadingInitial = false,
                            loadingMore = false,
                        )
                    }
                }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { it.copy(loadingInitial = false, loadingMore = false, error = err.toApiUiText()) }
                }
        }
    }
}
