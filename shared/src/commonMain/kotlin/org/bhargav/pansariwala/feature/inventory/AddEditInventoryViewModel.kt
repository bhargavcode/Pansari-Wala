package org.bhargav.pansariwala.feature.inventory

import kotlinx.coroutines.flow.first
import org.bhargav.pansariwala.voice.SpeechToText
import org.bhargav.pansariwala.feature.catalog.looksLikeBarcode
import org.bhargav.pansariwala.feature.catalog.ProductFinder
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.analytics.Analytics
import org.bhargav.pansariwala.analytics.AnalyticsEvent
import org.bhargav.pansariwala.api.MasterCategoryDto
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.ProductVariantDto
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.feature.catalog.legacyCategory
import org.bhargav.pansariwala.api.toDto
import org.bhargav.pansariwala.api.toModel
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductCategory
import org.bhargav.pansariwala.domain.model.ProductUnit
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.media.ImageSlotState
import org.bhargav.pansariwala.media.ImageSource
import org.bhargav.pansariwala.media.ImageUploadFeature
import org.bhargav.pansariwala.media.LifecycleImageUploader
import org.bhargav.pansariwala.media.UploadedImage
import org.bhargav.pansariwala.media.prepareImageForUpload
import org.bhargav.pansariwala.platform.ImagePicker
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.generateId
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_image_upload_failed
import pansariwala.shared.generated.resources.error_expiry_before_mfg
import pansariwala.shared.generated.resources.error_product_name_required
import pansariwala.shared.generated.resources.error_product_not_found
import pansariwala.shared.generated.resources.msg_loaded_product
import pansariwala.shared.generated.resources.msg_saved_product
import org.bhargav.pansariwala.i18n.appLanguageName
import org.bhargav.pansariwala.i18n.AppLanguage
import org.bhargav.pansariwala.platform.TextTranslator
import pansariwala.shared.generated.resources.error_translation_failed

data class AddEditInventoryUiState(
    val loading: Boolean = false,
    val isEditing: Boolean = false,
    val lookupQuery: String = "",
    val id: String = "",
    val name: String = "",
    val nameHi: String = "",
    val category: ProductCategory = ProductCategory.GENERAL_GROCERY,
    val categoryId: String? = null,
    val subCategoryId: String? = null,
    /** Master categories incl. sub-categories; [category] is derived from the picked one for legacy reports. */
    val masterCategories: List<MasterCategoryDto> = emptyList(),
    val unit: ProductUnit = ProductUnit.PIECE,
    val barcode: String = "",
    val sellingPrice: String = "",
    val costPrice: String = "",
    val stockQty: String = "",
    val lowStockThreshold: String = "",
    /** Blank / 0 = no per-order limit for customers. */
    val maxPerOrder: String = "",
    val voiceAlias: String = "",
    val brandName: String = "",
    val variants: List<ProductVariantDto> = emptyList(),
    /** False = single product: pricing/stock/dates on the product; true = each variant carries its own. */
    val hasVariants: Boolean = false,
    val mfgDate: Long? = null,
    val expiryDate: Long? = null,
    /** Languages this device can translate into offline; empty hides the auto-translate picker. */
    val translatableLanguages: List<AppLanguage> = emptyList(),
    /** Languages whose name / brand fields are shown; picking one fills them by on-device translation. */
    val translationLanguages: Set<String> = emptySet(),
    /** Name per language code except Hindi, which stays in [nameHi]. */
    val nameTranslations: Map<String, String> = emptyMap(),
    val brandTranslations: Map<String, String> = emptyMap(),
    val translating: Set<String> = emptySet(),
    val imageSlots: List<ImageSlotState> = List(AppConstants.PHOTO_MAX_PRODUCT_IMAGES) { ImageSlotState.Empty },
    val message: UiText? = null,
    val error: UiText? = null,
    val saved: Boolean = false,
)

class AddEditInventoryViewModel(
    private val shopRepository: ShopRepository,
    private val preferences: AppPreferences,
    private val analytics: Analytics,
    private val api: PansariApi,
    private val imagePicker: ImagePicker,
    speech: SpeechToText,
    private val translator: TextTranslator,
) : ViewModel() {

    /** Voice / barcode lookup of an existing product; a pick loads it into the form for editing. */
    val finder = ProductFinder(speech, viewModelScope) { shopRepository.observeProducts(shopId).first() }

    fun pickFound(product: Product) {
        fillFrom(product)
        _uiState.update { it.copy(message = UiText.res(Res.string.msg_loaded_product, product.appLanguageName), error = null) }
    }

    override fun onCleared() {
        finder.stopListening()
        super.onCleared()
    }

    private val _uiState = MutableStateFlow(AddEditInventoryUiState())
    val uiState: StateFlow<AddEditInventoryUiState> = _uiState.asStateFlow()

    private val imageUploader = LifecycleImageUploader(
        scope = viewModelScope,
        picker = imagePicker,
        upload = { prefix, compressed ->
            val result = api.uploadImage(
                prefix,
                compressed.displayName,
                compressed.bytes,
                compressed.mimeType,
                compressed.thumbnailBytes,
            )
            UploadedImage(result.url, result.thumbnailUrl)
        },
        slotCount = AppConstants.PHOTO_MAX_PRODUCT_IMAGES,
    )

    init {
        viewModelScope.launch {
            val supported = translator.supportedLanguages()
            _uiState.update { state ->
                state.copy(translatableLanguages = AppLanguage.entries.filter { it.code in supported })
            }
        }
        viewModelScope.launch {
            imageUploader.slots.collect { slots ->
                _uiState.update { state ->
                    val failed = slots.filterIsInstance<ImageSlotState.Failed>().isNotEmpty()
                    state.copy(
                        imageSlots = slots,
                        error = if (failed) UiText.res(Res.string.error_image_upload_failed) else state.error,
                    )
                }
            }
        }
    }

    fun dismissError() { _uiState.update { it.copy(error = null) } }

    private var shopId: String = SeedData.DEMO_SHOP_ID
    private var loadedProduct: Product? = null
    private val loadedProductId: String? get() = loadedProduct?.id
    private var started = false

    fun load(productId: String?) {
        // Re-invoked when the screen is recreated (rotation, activity restart); keep the in-progress form.
        if (started) return
        started = true
        viewModelScope.launch {
            shopRepository.ensureSeeded()
            shopId = preferences.getShopId() ?: SeedData.DEMO_SHOP_ID
            if (productId != null) {
                shopRepository.findProduct(productId)?.let { fillFrom(it) }
            }
        }
        viewModelScope.launch {
            runCatching { api.masterCategories() }
                .onSuccess { all -> _uiState.update { it.copy(masterCategories = all) } }
                .onFailure { it.rethrowIfStructuredCancellation() }
        }
    }

    fun onMasterCategoryChange(categoryId: String?, subCategoryId: String?) = _uiState.update {
        it.copy(
            categoryId = categoryId,
            subCategoryId = subCategoryId,
            category = it.masterCategories.legacyCategory(categoryId, it.category),
        )
    }

    fun onLookupQueryChange(value: String) = _uiState.update { it.copy(lookupQuery = value, error = null) }
    fun onNameChange(value: String) = _uiState.update { it.copy(name = value) }
    fun onNameHiChange(value: String) = _uiState.update { it.copy(nameHi = value) }
    fun onUnitChange(value: ProductUnit) = _uiState.update { it.copy(unit = value) }
    fun onBarcodeChange(value: String) = _uiState.update { it.copy(barcode = value) }
    fun onSellingPriceChange(value: String) = _uiState.update { it.copy(sellingPrice = value) }
    fun onCostPriceChange(value: String) = _uiState.update { it.copy(costPrice = value) }
    fun onStockQtyChange(value: String) = _uiState.update { it.copy(stockQty = value) }
    fun onThresholdChange(value: String) = _uiState.update { it.copy(lowStockThreshold = value) }
    fun onMaxPerOrderChange(value: String) = _uiState.update { it.copy(maxPerOrder = value) }
    fun onVoiceAliasChange(value: String) = _uiState.update { it.copy(voiceAlias = value) }
    fun onBrandNameChange(value: String) = _uiState.update { it.copy(brandName = value) }
    fun onVariantsChange(value: List<ProductVariantDto>) = _uiState.update { it.copy(variants = value) }
    fun onHasVariantsChange(value: Boolean) = _uiState.update { it.copy(hasVariants = value) }
    fun onMfgDateChange(value: Long?) = _uiState.update { it.copy(mfgDate = value) }
    fun onExpiryDateChange(value: Long?) = _uiState.update { it.copy(expiryDate = value) }
    fun onNameTranslationChange(code: String, value: String) =
        _uiState.update { it.copy(nameTranslations = it.nameTranslations + (code to value)) }
    fun onBrandTranslationChange(code: String, value: String) =
        _uiState.update { it.copy(brandTranslations = it.brandTranslations + (code to value)) }

    /** Picking fills that language's blank fields by translation; unpicking drops its name and brand. */
    fun onTranslationLanguageToggle(code: String) {
        if (code in _uiState.value.translationLanguages) {
            _uiState.update {
                it.copy(
                    translationLanguages = it.translationLanguages - code,
                    nameTranslations = it.nameTranslations - code,
                    brandTranslations = it.brandTranslations - code,
                )
            }
        } else {
            _uiState.update { it.copy(translationLanguages = it.translationLanguages + code) }
            translateInto(listOf(code), overwrite = false)
        }
    }

    /** Re-translates the current English name and brand into every picked language. */
    fun retranslate() = translateInto(_uiState.value.translationLanguages, overwrite = true)

    private fun translateInto(codes: Collection<String>, overwrite: Boolean) {
        val supported = _uiState.value.translatableLanguages.map { it.code }
        val todo = codes.filter { it in supported && it !in _uiState.value.translating }
        if (todo.isEmpty()) return
        _uiState.update { it.copy(translating = it.translating + todo) }
        viewModelScope.launch {
            val failed = mutableListOf<String>()
            todo.forEach { code ->
                val source = _uiState.value
                val name = source.name.trim().takeIf { it.isNotEmpty() }?.let { translator.translate(it, code) }
                val brand = source.brandName.trim().takeIf { it.isNotEmpty() }?.let { translator.translate(it, code) }
                if ((source.name.isNotBlank() && name == null) || (source.brandName.isNotBlank() && brand == null)) {
                    failed += code
                }
                _uiState.update { state ->
                    val done = state.copy(translating = state.translating - code)
                    if (code in state.translationLanguages) done.withTranslation(code, name, brand, overwrite) else done
                }
            }
            if (failed.isNotEmpty()) {
                val labels = failed.joinToString { AppLanguage.fromCode(it).displayLabel }
                _uiState.update { it.copy(error = UiText.res(Res.string.error_translation_failed, labels)) }
            }
        }
    }

    private fun AddEditInventoryUiState.withTranslation(
        code: String,
        name: String?,
        brand: String?,
        overwrite: Boolean,
    ): AddEditInventoryUiState {
        fun pick(current: String?, translated: String?): String =
            if (translated.isNullOrBlank() || (!overwrite && !current.isNullOrBlank())) current.orEmpty() else translated
        val brands = brandTranslations + (code to pick(brandTranslations[code], brand))
        return if (code == AppLanguage.HINDI.code) {
            copy(nameHi = pick(nameHi, name), brandTranslations = brands)
        } else {
            copy(nameTranslations = nameTranslations + (code to pick(nameTranslations[code], name)), brandTranslations = brands)
        }
    }

    private var baseline: List<Any?> = AddEditInventoryUiState().formKey()

    /** True when the form differs from what was loaded (or from blank for a new product). */
    fun hasUnsavedChanges(): Boolean = !_uiState.value.saved && _uiState.value.formKey() != baseline

    private fun AddEditInventoryUiState.formKey(): List<Any?> = listOf(
        name, nameHi, brandName, categoryId, subCategoryId, unit, barcode, sellingPrice, costPrice, stockQty,
        lowStockThreshold, maxPerOrder, voiceAlias, variants, hasVariants, mfgDate, expiryDate,
        imageSlots.count { it !is ImageSlotState.Empty },
        nameTranslations.cleaned(), brandTranslations.cleaned(),
    )

    private fun Map<String, String>.cleaned(): Map<String, String> =
        mapValues { it.value.trim() }.filterValues { it.isNotEmpty() }

    suspend fun uploadVariantImage(): String? {
        val picked = runCatching { imagePicker.pickImage() }.getOrNull() ?: return null
        return runCatching {
            val compressed = prepareImageForUpload(picked)
            api.uploadImage(
                ImageUploadFeature.INVENTORY_PRODUCT.prefix,
                compressed.displayName,
                compressed.bytes,
                compressed.mimeType,
                compressed.thumbnailBytes,
            ).url
        }.onFailure {
            _uiState.update { it.copy(error = UiText.res(Res.string.error_image_upload_failed)) }
        }.getOrNull()
    }

    fun addImage(slot: Int, source: ImageSource) =
        imageUploader.pickAndUpload(slot, ImageUploadFeature.INVENTORY_PRODUCT.prefix, source)
    fun retryImage(slot: Int) = imageUploader.retry(slot)
    fun clearImage(slot: Int) = imageUploader.clear(slot)

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
                        message = UiText.res(Res.string.msg_loaded_product, product.appLanguageName),
                        error = null,
                    )
                }
            } else if (looksLikeBarcode(query)) {
                _uiState.update {
                    it.copy(
                        message = null,
                        error = UiText.res(Res.string.error_product_not_found, query),
                    )
                }
            } else {
                _uiState.update { it.copy(message = null, error = null) }
                finder.search(query)
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
        if (imageUploader.hasInFlight()) {
            _uiState.update { it.copy(error = UiText.res(Res.string.error_image_upload_failed)) }
            return
        }
        if (imageUploader.hasFailures()) {
            _uiState.update { it.copy(error = UiText.res(Res.string.error_image_upload_failed)) }
            return
        }
        if (state.mfgDate != null && state.expiryDate != null && state.expiryDate < state.mfgDate) {
            _uiState.update { it.copy(error = UiText.res(Res.string.error_expiry_before_mfg)) }
            return
        }
        val keptVariants = if (state.hasVariants) state.variants.filter { it.name.isNotBlank() } else emptyList()
        val nameHi = state.nameHi.trim()
        val product = Product(
            id = loadedProductId ?: generateId("prod"),
            shopId = shopId,
            name = name,
            nameHi = nameHi,
            translations = (state.nameTranslations - AppLanguage.HINDI.code).cleaned() +
                listOfNotNull(nameHi.takeIf { it.isNotEmpty() }?.let { AppLanguage.HINDI.code to it }),
            brandTranslations = state.brandTranslations.cleaned(),
            category = state.category,
            unit = state.unit,
            barcode = state.barcode.trim().ifBlank { null },
            sellingPrice = state.sellingPrice.toDoubleOrNull() ?: 0.0,
            costPrice = state.costPrice.toDoubleOrNull() ?: 0.0,
            stockQty = state.stockQty.toDoubleOrNull() ?: 0.0,
            lowStockThreshold = state.lowStockThreshold.toDoubleOrNull() ?: 0.0,
            maxPerOrder = state.maxPerOrder.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0,
            voiceAlias = state.voiceAlias.trim().ifBlank { null },
            imageUrls = imageUploader.readyUrls(),
            variants = keptVariants
                .map { v -> v.copy(id = v.id.ifBlank { generateId("v") }).toModel() },
            brandName = state.brandName.trim(),
            masterProductId = loadedProduct?.masterProductId,
            isActive = loadedProduct?.isActive ?: true,
            categoryId = state.categoryId,
            subCategoryId = state.subCategoryId,
            mfgDate = state.mfgDate,
            expiryDate = state.expiryDate,
        )
        viewModelScope.launch {
            shopRepository.upsertProduct(product)
            _uiState.update {
                it.copy(
                    saved = true,
                    message = UiText.res(Res.string.msg_saved_product, product.appLanguageName),
                    error = null,
                )
            }
        }
    }

    private fun fillFrom(product: Product) {
        loadedProduct = product
        imageUploader.seedUrls(product.imageUrls)
        _uiState.update {
            it.copy(
                isEditing = true,
                id = product.id,
                name = product.name,
                nameHi = product.translations[AppLanguage.HINDI.code]?.takeIf { t -> t.isNotBlank() } ?: product.nameHi,
                nameTranslations = product.translations - AppLanguage.HINDI.code,
                brandTranslations = product.brandTranslations,
                translationLanguages = (product.translations - AppLanguage.HINDI.code).cleaned().keys +
                    product.brandTranslations.cleaned().keys,
                category = product.category,
                categoryId = product.categoryId,
                subCategoryId = product.subCategoryId,
                unit = product.unit,
                barcode = product.barcode.orEmpty(),
                sellingPrice = product.sellingPrice.toString(),
                costPrice = product.costPrice.toString(),
                stockQty = product.stockQty.toString(),
                lowStockThreshold = product.lowStockThreshold.toString(),
                maxPerOrder = product.maxPerOrder.takeIf { it > 0.0 }?.toString().orEmpty(),
                voiceAlias = product.voiceAlias.orEmpty(),
                brandName = product.brandName,
                variants = product.variants.map { it.toDto() },
                hasVariants = product.variants.isNotEmpty(),
                mfgDate = product.mfgDate,
                expiryDate = product.expiryDate,
            )
        }
        baseline = _uiState.value.formKey()
    }
}
