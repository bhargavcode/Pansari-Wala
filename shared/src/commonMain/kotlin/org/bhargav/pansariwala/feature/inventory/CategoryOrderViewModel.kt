package org.bhargav.pansariwala.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.CategorySort
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.ShopCategoryOrderDto
import org.bhargav.pansariwala.api.ShopCategoryOrderItemDto
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.i18n.UiText

/** [priorities] holds the typed text per category id; blank = unranked. */
data class CategoryOrderUiState(
    val loading: Boolean = true,
    val saving: Boolean = false,
    val sort: CategorySort = CategorySort.PRIORITY,
    val items: List<ShopCategoryOrderItemDto> = emptyList(),
    val priorities: Map<String, String> = emptyMap(),
    val error: UiText? = null,
)

/** POS dialog: how the customer app orders this shop's categories (priority numbers or by name). */
class CategoryOrderViewModel(private val api: PansariApi) : ViewModel() {

    private val _state = MutableStateFlow<CategoryOrderUiState?>(null)
    val state: StateFlow<CategoryOrderUiState?> = _state.asStateFlow()

    fun open() {
        _state.value = CategoryOrderUiState()
        viewModelScope.launch {
            runCatching { api.shopCategoryOrder() }
                .onSuccess(::apply)
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    update { it.copy(loading = false, error = err.toApiUiText()) }
                }
        }
    }

    fun dismiss() {
        _state.value = null
    }

    fun onSortChange(sort: CategorySort) = update { it.copy(sort = sort) }

    fun onPriorityChange(categoryId: String, raw: String) = update {
        val digits = raw.filter(Char::isDigit).take(MAX_PRIORITY_DIGITS)
        it.copy(priorities = it.priorities + (categoryId to digits))
    }

    fun save() {
        val current = _state.value ?: return
        update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            val body = ShopCategoryOrderDto(
                sort = current.sort.name,
                items = current.items.map { it.copy(priority = current.priorities[it.id]?.toIntOrNull()) },
            )
            runCatching { api.updateShopCategoryOrder(body) }
                .onSuccess { _state.value = null }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    update { it.copy(saving = false, error = err.toApiUiText()) }
                }
        }
    }

    private fun apply(dto: ShopCategoryOrderDto) = update {
        it.copy(
            loading = false,
            sort = CategorySort.fromName(dto.sort),
            items = dto.items,
            priorities = dto.items.associate { item -> item.id to item.priority?.toString().orEmpty() },
        )
    }

    private fun update(transform: (CategoryOrderUiState) -> CategoryOrderUiState) {
        _state.update { it?.let(transform) }
    }

    private companion object {
        const val MAX_PRIORITY_DIGITS = 3
    }
}
