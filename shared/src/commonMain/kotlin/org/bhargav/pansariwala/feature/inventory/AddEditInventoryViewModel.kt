package org.bhargav.pansariwala.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.analytics.Analytics
import org.bhargav.pansariwala.analytics.AnalyticsEvent
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductCategory
import org.bhargav.pansariwala.domain.model.ProductUnit
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.util.generateId
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_product_name_required
import pansariwala.shared.generated.resources.error_product_not_found
import pansariwala.shared.generated.resources.msg_loaded_product
import pansariwala.shared.generated.resources.msg_saved_product

data class AddEditInventoryUiState(
    val loading: Boolean = false,
    val isEditing: Boolean = false,
    val lookupQuery: String = "",
    val id: String = "",
    val name: String = "",
    val nameHi: String = "",
    val category: ProductCategory = ProductCategory.GENERAL_GROCERY,
    val unit: ProductUnit = ProductUnit.PIECE,
    val barcode: String = "",
    val sellingPrice: String = "",
    val costPrice: String = "",
    val stockQty: String = "",
    val lowStockThreshold: String = "",
    val voiceAlias: String = "",
    val message: UiText? = null,
    val error: UiText? = null,
    val saved: Boolean = false,
)

class AddEditInventoryViewModel(
    private val shopRepository: ShopRepository,
    private val preferences: AppPreferences,
    private val analytics: Analytics,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddEditInventoryUiState())
    val uiState: StateFlow<AddEditInventoryUiState> = _uiState.asStateFlow()

    private var shopId: String = SeedData.DEMO_SHOP_ID
    private var loadedProductId: String? = null

    fun load(productId: String?) {
        viewModelScope.launch {
            shopRepository.ensureSeeded()
            shopId = preferences.getShopId() ?: SeedData.DEMO_SHOP_ID
            if (productId != null) {
                shopRepository.findProduct(productId)?.let { fillFrom(it) }
            }
        }
    }

    fun onLookupQueryChange(value: String) = _uiState.update { it.copy(lookupQuery = value, error = null) }
    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onNameHiChange(value: String) = _uiState.update { it.copy(nameHi = value) }
    fun onCategoryChange(value: ProductCategory) = _uiState.update { it.copy(category = value) }
    fun onUnitChange(value: ProductUnit) = _uiState.update { it.copy(unit = value) }
    fun onBarcodeChange(value: String) = _uiState.update { it.copy(barcode = value) }
    fun onSellingPriceChange(value: String) = _uiState.update { it.copy(sellingPrice = value) }
    fun onCostPriceChange(value: String) = _uiState.update { it.copy(costPrice = value) }
    fun onStockQtyChange(value: String) = _uiState.update { it.copy(stockQty = value) }
    fun onThresholdChange(value: String) = _uiState.update { it.copy(lowStockThreshold = value) }
    fun onVoiceAliasChange(value: String) = _uiState.update { it.copy(voiceAlias = value) }

    fun onLookup() {
        val query = _uiState.value.lookupQuery.trim()
        if (query.isBlank()) return
        analytics.log(AnalyticsEvent.ButtonClicked("inventory_lookup", "add_edit_inventory"))
        viewModelScope.launch {
            val product = shopRepository.findProduct(query)
            if (product != null) {
                fillFrom(product)
                _uiState.update {
                    it.copy(
                        message = UiText.res(Res.string.msg_loaded_product, product.name),
                        error = null,
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        message = null,
                        error = UiText.res(Res.string.error_product_not_found, query),
                    )
                }
            }
        }
    }

    fun onSave() {
        analytics.log(AnalyticsEvent.ButtonClicked("inventory_save", "add_edit_inventory"))
        val state = _uiState.value
        val name = state.name.trim()
        if (name.isBlank()) {
            _uiState.update { it.copy(error = UiText.res(Res.string.error_product_name_required)) }
            return
        }
        val product = Product(
            id = loadedProductId ?: generateId("prod"),
            shopId = shopId,
            name = name,
            nameHi = state.nameHi.trim(),
            category = state.category,
            unit = state.unit,
            barcode = state.barcode.trim().ifBlank { null },
            sellingPrice = state.sellingPrice.toDoubleOrNull() ?: 0.0,
            costPrice = state.costPrice.toDoubleOrNull() ?: 0.0,
            stockQty = state.stockQty.toDoubleOrNull() ?: 0.0,
            lowStockThreshold = state.lowStockThreshold.toDoubleOrNull() ?: 0.0,
            voiceAlias = state.voiceAlias.trim().ifBlank { null },
        )
        viewModelScope.launch {
            shopRepository.upsertProduct(product)
            _uiState.update {
                it.copy(
                    saved = true,
                    message = UiText.res(Res.string.msg_saved_product, product.name),
                    error = null,
                )
            }
        }
    }

    private fun fillFrom(product: Product) {
        loadedProductId = product.id
        _uiState.update {
            it.copy(
                isEditing = true,
                id = product.id,
                name = product.name,
                nameHi = product.nameHi,
                category = product.category,
                unit = product.unit,
                barcode = product.barcode.orEmpty(),
                sellingPrice = product.sellingPrice.toString(),
                costPrice = product.costPrice.toString(),
                stockQty = product.stockQty.toString(),
                lowStockThreshold = product.lowStockThreshold.toString(),
                voiceAlias = product.voiceAlias.orEmpty(),
            )
        }
    }
}
