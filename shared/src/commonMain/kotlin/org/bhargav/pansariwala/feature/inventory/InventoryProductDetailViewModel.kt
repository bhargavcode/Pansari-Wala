package org.bhargav.pansariwala.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.util.asQuantity
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.inv_error_invalid_qty
import pansariwala.shared.generated.resources.inv_error_stock_negative
import pansariwala.shared.generated.resources.inv_msg_stock_updated

data class InventoryProductDetailUiState(
    val loading: Boolean = true,
    val product: Product? = null,
    val missing: Boolean = false,
    val qtyInput: String = "",
    /** Variant whose stock the update card edits; null for products without options. */
    val selectedVariantId: String? = null,
    val message: UiText? = null,
    val error: UiText? = null,
) {
    val editedStock: Double get() = product?.stockOf(selectedVariantId) ?: 0.0
}

class InventoryProductDetailViewModel(
    private val shopRepository: ShopRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(InventoryProductDetailUiState())
    val uiState: StateFlow<InventoryProductDetailUiState> = _uiState.asStateFlow()

    private var productId: String? = null
    private var observeJob: Job? = null

    fun load(id: String) {
        if (productId == id) return
        productId = id
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            shopRepository.observeProduct(id)
                .catch { error -> _uiState.update { it.copy(loading = false, error = error.toApiUiText()) } }
                .collect { product ->
                    _uiState.update { state ->
                        val stockIds = product?.stockVariants.orEmpty().map { it.id }
                        val selected = state.selectedVariantId?.takeIf { it in stockIds } ?: stockIds.firstOrNull()
                        state.copy(loading = false, product = product, missing = product == null, selectedVariantId = selected)
                    }
                }
        }
    }

    fun selectVariant(id: String) = _uiState.update { it.copy(selectedVariantId = id, error = null) }

    fun onQtyChange(value: String) = _uiState.update { it.copy(qtyInput = value, error = null) }
    fun dismissMessage() = _uiState.update { it.copy(message = null) }
    fun dismissError() = _uiState.update { it.copy(error = null) }

    fun stockIn(step: Double? = null) {
        val qty = step ?: parsedQty() ?: return
        applyStock(qty)
    }

    fun stockOut(step: Double? = null) {
        val qty = step ?: parsedQty() ?: return
        if (qty > _uiState.value.editedStock) {
            _uiState.update { it.copy(error = UiText.res(Res.string.inv_error_stock_negative)) }
            return
        }
        applyStock(-qty)
    }

    fun setStock() {
        val qty = parsedQty(allowZero = true) ?: return
        applyStock(qty - _uiState.value.editedStock)
    }

    fun setActive(active: Boolean) {
        val product = _uiState.value.product ?: return
        viewModelScope.launch { shopRepository.setProductActive(product.id, active) }
    }

    /** Variant products keep stock per option: customers only see options whose own stock is above zero. */
    private fun applyStock(delta: Double) {
        val state = _uiState.value
        val product = state.product ?: return
        val variantId = state.selectedVariantId?.takeIf { product.variant(it) != null }
        val newQty = product.stockOf(variantId) + delta
        viewModelScope.launch {
            runCatching {
                if (variantId != null) {
                    shopRepository.upsertProduct(product.withStock(variantId, newQty))
                } else {
                    shopRepository.adjustProductStock(product.id, delta)
                }
            }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            qtyInput = "",
                            error = null,
                            message = UiText.res(Res.string.inv_msg_stock_updated, newQty.asQuantity(), product.unit.label),
                        )
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.toApiUiText()) } }
        }
    }

    private fun parsedQty(allowZero: Boolean = false): Double? {
        val qty = _uiState.value.qtyInput.trim().toDoubleOrNull()
        val valid = qty != null && (if (allowZero) qty >= 0.0 else qty > 0.0)
        if (!valid) {
            _uiState.update { it.copy(error = UiText.res(Res.string.inv_error_invalid_qty)) }
            return null
        }
        return qty
    }
}
