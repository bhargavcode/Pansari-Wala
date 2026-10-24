package org.bhargav.pansariwala.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.StockSupplier
import org.bhargav.pansariwala.domain.model.SupplierBook
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.platform.digitsPhone
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asQuantity
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_phone_invalid
import pansariwala.shared.generated.resources.inv_error_invalid_qty
import pansariwala.shared.generated.resources.stock_req_error_name
import pansariwala.shared.generated.resources.stock_req_error_supplier
import kotlin.math.ceil
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class StockRequestChannel { WHATSAPP, SMS }

data class StockRequestUiState(
    val product: Product? = null,
    val suppliers: List<StockSupplier> = emptyList(),
    val selectedId: String? = null,
    val addingSupplier: Boolean = false,
    val nameInput: String = "",
    val phoneInput: String = "",
    val qtyInput: String = "",
    val note: String = "",
    val shopName: String = "",
    val error: UiText? = null,
) {
    val selected: StockSupplier? get() = suppliers.firstOrNull { it.id == selectedId }
    val qty: Double? get() = qtyInput.trim().toDoubleOrNull()?.takeIf { it > 0.0 }
}

class StockRequestViewModel(
    private val prefs: AppPreferences,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockRequestUiState())
    val uiState: StateFlow<StockRequestUiState> = _uiState.asStateFlow()

    private var shopId: String = AppConstants.DEMO_SHOP_ID
    private var book = SupplierBook()

    fun open(product: Product) {
        if (_uiState.value.product?.id == product.id) return
        viewModelScope.launch {
            shopId = prefs.getShopId() ?: AppConstants.DEMO_SHOP_ID
            book = prefs.getSupplierBook(shopId)
            val linked = book.productSupplier[product.id]?.takeIf { id -> book.suppliers.any { it.id == id } }
            _uiState.value = StockRequestUiState(
                product = product,
                suppliers = book.suppliers,
                selectedId = linked ?: book.suppliers.firstOrNull()?.id,
                addingSupplier = book.suppliers.isEmpty(),
                qtyInput = suggestedQty(product)?.asQuantity().orEmpty(),
                shopName = prefs.getShopName(),
            )
        }
    }

    fun selectSupplier(id: String) = _uiState.update { it.copy(selectedId = id, error = null) }

    fun showAddSupplier(show: Boolean) =
        _uiState.update { it.copy(addingSupplier = show, nameInput = "", phoneInput = "", error = null) }

    fun onNameChange(value: String) = _uiState.update { it.copy(nameInput = value, error = null) }

    fun onPhoneChange(value: String) =
        _uiState.update { it.copy(phoneInput = value.filter { c -> c.isDigit() || c == '+' || c == ' ' }, error = null) }

    fun onQtyChange(value: String) = _uiState.update { it.copy(qtyInput = value, error = null) }

    fun onNoteChange(value: String) = _uiState.update { it.copy(note = value) }

    @OptIn(ExperimentalUuidApi::class)
    fun saveSupplier() {
        val state = _uiState.value
        val name = state.nameInput.trim()
        val phone = state.phoneInput.trim()
        val error = when {
            name.isBlank() -> UiText.res(Res.string.stock_req_error_name)
            digitsPhone(phone).length != AppConstants.PHONE_LOCAL_DIGITS -> UiText.res(Res.string.error_phone_invalid)
            else -> null
        }
        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }
        val supplier = StockSupplier(id = Uuid.random().toString(), name = name, phone = phone)
        persist(book.copy(suppliers = book.suppliers + supplier))
        _uiState.update {
            it.copy(
                suppliers = book.suppliers,
                selectedId = supplier.id,
                addingSupplier = false,
                nameInput = "",
                phoneInput = "",
            )
        }
    }

    fun removeSupplier(id: String) {
        persist(
            book.copy(
                suppliers = book.suppliers.filterNot { it.id == id },
                productSupplier = book.productSupplier.filterValues { it != id },
            ),
        )
        _uiState.update { state ->
            state.copy(
                suppliers = book.suppliers,
                selectedId = state.selectedId.takeIf { it != id } ?: book.suppliers.firstOrNull()?.id,
                addingSupplier = book.suppliers.isEmpty(),
            )
        }
    }

    /** Validates the request; on success remembers the supplier for this product and returns it. */
    fun confirmSend(): StockSupplier? {
        val state = _uiState.value
        val supplier = state.selected
        val error = when {
            supplier == null -> UiText.res(Res.string.stock_req_error_supplier)
            state.qty == null -> UiText.res(Res.string.inv_error_invalid_qty)
            else -> null
        }
        if (error != null || supplier == null) {
            _uiState.update { it.copy(error = error) }
            return null
        }
        state.product?.let { product ->
            persist(book.copy(productSupplier = book.productSupplier + (product.id to supplier.id)))
        }
        return supplier
    }

    private fun persist(updated: SupplierBook) {
        book = updated
        viewModelScope.launch { prefs.setSupplierBook(shopId, updated) }
    }

    /** Enough to bring stock back to twice the low-stock threshold. */
    private fun suggestedQty(product: Product): Double? {
        val threshold = product.lowStockThreshold
        if (threshold <= 0.0) return null
        return ceil(maxOf(threshold * 2 - product.stockQty.coerceAtLeast(0.0), threshold))
    }
}
