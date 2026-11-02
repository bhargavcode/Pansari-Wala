package org.bhargav.pansariwala.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.StockSupplier
import org.bhargav.pansariwala.domain.model.SupplierAutoRequest
import org.bhargav.pansariwala.domain.model.SupplierBook
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.platform.digitsPhone
import org.bhargav.pansariwala.util.AppConstants
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_phone_invalid
import pansariwala.shared.generated.resources.stock_req_error_name
import kotlin.math.ceil
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Product filter on the suppliers screen; LINKED = products last ordered from the selected supplier. */
enum class SupplierProductFilter { LINKED, OUT_OF_STOCK, LOW_STOCK, ALL }

data class SuppliersUiState(
    val loading: Boolean = true,
    val suppliers: List<StockSupplier> = emptyList(),
    val productSupplier: Map<String, String> = emptyMap(),
    val products: List<Product> = emptyList(),
    val selectedId: String? = null,
    val filter: SupplierProductFilter = SupplierProductFilter.LINKED,
    /** productId → requested quantity text. */
    val picked: Map<String, String> = emptyMap(),
    val addingSupplier: Boolean = false,
    val nameInput: String = "",
    val phoneInput: String = "",
    val shopName: String = "",
    val autoRequest: SupplierAutoRequest = SupplierAutoRequest(),
    val error: UiText? = null,
) {
    val selected: StockSupplier? get() = suppliers.firstOrNull { it.id == selectedId }

    fun linkedCount(supplierId: String): Int = productSupplier.count { it.value == supplierId }

    val visibleProducts: List<Product>
        get() = when (filter) {
            SupplierProductFilter.LINKED -> products.filter { productSupplier[it.id] == selectedId }
            SupplierProductFilter.OUT_OF_STOCK -> products.filter { it.totalStock <= 0.0 }
            SupplierProductFilter.LOW_STOCK -> products.filter { it.isLowStock }
            SupplierProductFilter.ALL -> products
        }.sortedBy { it.totalStock - it.lowStockThreshold }

    /** Picked lines with a valid quantity, in list order. */
    val requestLines: List<Pair<Product, Double>>
        get() = products.mapNotNull { p ->
            picked[p.id]?.trim()?.toDoubleOrNull()?.takeIf { it > 0.0 }?.let { p to it }
        }
}

class SuppliersViewModel(
    private val prefs: AppPreferences,
    private val repository: ShopRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SuppliersUiState())
    val state: StateFlow<SuppliersUiState> = _state.asStateFlow()

    private var shopId = SeedData.DEMO_SHOP_ID
    private var book = SupplierBook()

    init {
        viewModelScope.launch {
            shopId = prefs.getShopId() ?: SeedData.DEMO_SHOP_ID
            book = prefs.getSupplierBook(shopId)
            _state.update {
                it.copy(
                    suppliers = book.suppliers,
                    productSupplier = book.productSupplier,
                    selectedId = book.suppliers.firstOrNull()?.id,
                    filter = if (book.suppliers.isEmpty()) SupplierProductFilter.OUT_OF_STOCK else SupplierProductFilter.LINKED,
                    shopName = prefs.getShopName(),
                    autoRequest = book.autoRequest,
                )
            }
            repository.observeProducts(shopId).collect { products ->
                _state.update { it.copy(products = products.filter { p -> p.isActive && !p.isDeleted }, loading = false) }
            }
        }
    }

    fun selectSupplier(id: String) = _state.update { it.copy(selectedId = id, error = null) }

    fun setFilter(filter: SupplierProductFilter) = _state.update { it.copy(filter = filter) }

    fun togglePick(product: Product) = _state.update { s ->
        val next = if (product.id in s.picked) s.picked - product.id else s.picked + (product.id to suggestedQty(product))
        s.copy(picked = next)
    }

    fun setQty(productId: String, text: String) = _state.update { s ->
        s.copy(picked = s.picked + (productId to text.filter { it.isDigit() || it == '.' }))
    }

    /** Picks every visible product that is out of or low on stock. */
    fun pickAllLow() = _state.update { s ->
        val add = s.visibleProducts.filter { it.isLowStock && it.id !in s.picked }.associate { it.id to suggestedQty(it) }
        s.copy(picked = s.picked + add)
    }

    fun clearPicks() = _state.update { it.copy(picked = emptyMap()) }

    fun showAddSupplier(show: Boolean) =
        _state.update { it.copy(addingSupplier = show, nameInput = "", phoneInput = "", error = null) }

    fun onNameChange(value: String) = _state.update { it.copy(nameInput = value, error = null) }

    fun onPhoneChange(value: String) =
        _state.update { it.copy(phoneInput = value.filter { c -> c.isDigit() || c == '+' || c == ' ' }, error = null) }

    @OptIn(ExperimentalUuidApi::class)
    fun saveSupplier() {
        val s = _state.value
        val name = s.nameInput.trim()
        val phone = s.phoneInput.trim()
        val error = when {
            name.isBlank() -> UiText.res(Res.string.stock_req_error_name)
            digitsPhone(phone).length != AppConstants.PHONE_LOCAL_DIGITS -> UiText.res(Res.string.error_phone_invalid)
            else -> null
        }
        if (error != null) {
            _state.update { it.copy(error = error) }
            return
        }
        val supplier = StockSupplier(id = Uuid.random().toString(), name = name, phone = phone)
        persist(book.copy(suppliers = book.suppliers + supplier))
        _state.update { it.copy(selectedId = supplier.id, addingSupplier = false, nameInput = "", phoneInput = "") }
    }

    fun removeSupplier(id: String) {
        persist(
            book.copy(
                suppliers = book.suppliers.filterNot { it.id == id },
                productSupplier = book.productSupplier.filterValues { it != id },
            ),
        )
        _state.update { s -> s.copy(selectedId = s.selectedId.takeIf { it != id } ?: book.suppliers.firstOrNull()?.id) }
    }

    /** Links the requested products to the supplier and clears the picks; returns the supplier to message. */
    fun confirmSend(): StockSupplier? {
        val s = _state.value
        val supplier = s.selected ?: return null
        if (s.requestLines.isEmpty()) return null
        persist(book.copy(productSupplier = book.productSupplier + s.requestLines.associate { it.first.id to supplier.id }))
        _state.update { it.copy(picked = emptyMap()) }
        return supplier
    }

    fun setAutoRequest(settings: SupplierAutoRequest) = persist(book.copy(autoRequest = settings))

    private fun persist(updated: SupplierBook) {
        book = updated
        _state.update {
            it.copy(suppliers = updated.suppliers, productSupplier = updated.productSupplier, autoRequest = updated.autoRequest)
        }
        viewModelScope.launch { prefs.setSupplierBook(shopId, updated) }
    }

    /** Enough to bring stock back to twice the low-stock threshold; at least 1. */
    private fun suggestedQty(product: Product): String {
        val threshold = product.lowStockThreshold
        val qty = if (threshold <= 0.0) 1.0 else ceil(maxOf(threshold * 2 - product.totalStock.coerceAtLeast(0.0), threshold))
        return qty.toLong().toString()
    }
}
