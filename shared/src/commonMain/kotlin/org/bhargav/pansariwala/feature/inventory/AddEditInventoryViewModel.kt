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
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import pansariwala.shared.generated.resources.error_translation_failed

private const val TRANSLATE_DEBOUNCE_MS = 500L
private const val DRAFT_AUTOSAVE_MS = 600L

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
    /** Non-null = a matching local draft was found; the screen asks resume vs start new. */
    val pendingDraft: ProductFormDraft? = null,
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
            if (supported.isNotEmpty()) autoTranslate(supported)
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
            offerMatchingDraft(productId)
            startDraftAutosave(productId)
        }
        viewModelScope.launch {
            runCatching { api.masterCategories() }
                .onSuccess { all -> _uiState.update { it.copy(masterCategories = all) } }
                .onFailure { it.rethrowIfStructuredCancellation() }
        }
    }

    private suspend fun offerMatchingDraft(productId: String?) {
        val draft = preferences.getProductFormDraft() ?: return
        if (!draft.matches(shopId, productId) || draft.isBlank()) return
        // Same as the current form (e.g. already restored by rotation) — nothing to offer.
        if (draft.formSnapshot() == _uiState.value.toDraft(shopId, productId).formSnapshot()) return
        _uiState.update { it.copy(pendingDraft = draft) }
    }

    @OptIn(FlowPreview::class)
    private fun startDraftAutosave(productId: String?) {
        viewModelScope.launch {
            _uiState
                .map { it.toDraft(shopId, productId).formSnapshot() }
                .distinctUntilChanged()
                .debounce(DRAFT_AUTOSAVE_MS)
                .collectLatest {
                    val state = _uiState.value
                    if (state.saved || state.pendingDraft != null || !hasUnsavedChanges()) return@collectLatest
                    val draft = state.toDraft(shopId, productId)
                    if (draft.isBlank()) {
                        preferences.clearProductFormDraft()
                    } else {
                        preferences.setProductFormDraft(draft)
                    }
                }
        }
    }

    fun resumeDraft() {
        val draft = _uiState.value.pendingDraft ?: return
        applyDraft(draft)
        _uiState.update { it.copy(pendingDraft = null) }
    }

    fun discardDraft() {
        viewModelScope.launch { preferences.clearProductFormDraft() }
        _uiState.update { it.copy(pendingDraft = null) }
    }

    /** Clears the local draft when leaving without saving (discard / start new). */
    fun abandonUnsavedWork() {
        viewModelScope.launch { preferences.clearProductFormDraft() }
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
    fun onNameHiChange(value: String) {
        markManual(nameField(AppLanguage.HINDI.code), value)
        _uiState.update { it.copy(nameHi = value) }
    }
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
    fun onNameTranslationChange(code: String, value: String) {
        markManual(nameField(code), value)
        _uiState.update { it.copy(nameTranslations = it.nameTranslations + (code to value)) }
    }

    fun onBrandTranslationChange(code: String, value: String) {
        markManual(brandField(code), value)
        _uiState.update { it.copy(brandTranslations = it.brandTranslations + (code to value)) }
    }

    /** Picking a language auto-fills its fields from the English name / brand; unpicking drops them. */
    fun onTranslationLanguageToggle(code: String) {
        val state = _uiState.value
        translatedFrom.remove(code)
        if (code in state.translationLanguages) {
            manualFields.remove(nameField(code))
            manualFields.remove(brandField(code))
            _uiState.update {
                it.copy(
                    translationLanguages = it.translationLanguages - code,
                    nameTranslations = it.nameTranslations - code,
                    brandTranslations = it.brandTranslations - code,
                )
            }
        } else {
            val currentName = if (code == AppLanguage.HINDI.code) state.nameHi else state.nameTranslations[code]
            markManual(nameField(code), currentName.orEmpty())
            markManual(brandField(code), state.brandTranslations[code].orEmpty())
            _uiState.update { it.copy(translationLanguages = it.translationLanguages + code) }
        }
    }

    private data class TranslationSource(val name: String, val brand: String)
    private data class TranslationInput(val source: TranslationSource, val languages: Set<String>)

    /** English name + brand each language's fields were last translated from. */
    private val translatedFrom = mutableMapOf<String, TranslationSource>()

    /** Fields the user typed into ("name:kn"); auto-translation never overwrites them. */
    private val manualFields = mutableSetOf<String>()

    private fun nameField(code: String) = "name:$code"
    private fun brandField(code: String) = "brand:$code"

    private fun markManual(field: String, value: String) {
        if (value.isBlank()) manualFields.remove(field) else manualFields.add(field)
    }

    /**
     * Re-translates picked languages whenever the English name / brand or the picked set changes.
     * Typing is debounced and a newer edit cancels in-flight work; languages translate in parallel.
     */
    @OptIn(FlowPreview::class)
    private suspend fun autoTranslate(supported: Set<String>) {
        _uiState
            .map { TranslationInput(TranslationSource(it.name.trim(), it.brandName.trim()), it.translationLanguages intersect supported) }
            .distinctUntilChanged()
            .debounce(TRANSLATE_DEBOUNCE_MS)
            .collectLatest(::translatePending)
    }

    private suspend fun translatePending(input: TranslationInput) {
        val source = input.source
        val todo = input.languages.filter { translatedFrom[it] != source }
        if (todo.isEmpty()) return
        val failed = mutableListOf<String>()
        _uiState.update { it.copy(translating = it.translating + todo) }
        try {
            coroutineScope {
                todo.forEach { code ->
                    launch {
                        val result = translator.translate(listOf(source.name, source.brand), code)
                        if (result == null) {
                            failed += code
                        } else {
                            translatedFrom[code] = source
                            _uiState.update { it.withTranslation(code, result[0], result[1]) }
                        }
                        _uiState.update { it.copy(translating = it.translating - code) }
                    }
                }
            }
        } finally {
            _uiState.update { it.copy(translating = it.translating - todo.toSet()) }
        }
        if (failed.isNotEmpty()) {
            val labels = failed.joinToString { AppLanguage.fromCode(it).displayLabel }
            _uiState.update { it.copy(error = UiText.res(Res.string.error_translation_failed, labels)) }
        }
    }

    private fun AddEditInventoryUiState.withTranslation(code: String, name: String, brand: String): AddEditInventoryUiState {
        if (code !in translationLanguages) return this
        var next = this
        if (nameField(code) !in manualFields) {
            next = if (code == AppLanguage.HINDI.code) {
                next.copy(nameHi = name)
            } else {
                next.copy(nameTranslations = next.nameTranslations + (code to name))
            }
        }
        if (brandField(code) !in manualFields) {
            next = next.copy(brandTranslations = next.brandTranslations + (code to brand))
        }
        return next
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
            preferences.clearProductFormDraft()
            _uiState.update {
                it.copy(
                    saved = true,
                    message = UiText.res(Res.string.msg_saved_product, product.appLanguageName),
                    error = null,
                )
            }
        }
    }

    private fun applyDraft(draft: ProductFormDraft) {
        manualFields.clear()
        translatedFrom.clear()
        imageUploader.seedUrls(draft.imageUrls)
        draft.nameTranslations.filterValues { it.isNotBlank() }.keys.forEach { manualFields.add(nameField(it)) }
        if (draft.nameHi.isNotBlank()) manualFields.add(nameField(AppLanguage.HINDI.code))
        draft.brandTranslations.filterValues { it.isNotBlank() }.keys.forEach { manualFields.add(brandField(it)) }
        _uiState.update {
            it.copy(
                name = draft.name,
                nameHi = draft.nameHi,
                brandName = draft.brandName,
                category = runCatching { ProductCategory.valueOf(draft.category) }.getOrDefault(it.category),
                categoryId = draft.categoryId,
                subCategoryId = draft.subCategoryId,
                unit = runCatching { ProductUnit.valueOf(draft.unit) }.getOrDefault(it.unit),
                barcode = draft.barcode,
                sellingPrice = draft.sellingPrice,
                costPrice = draft.costPrice,
                stockQty = draft.stockQty,
                lowStockThreshold = draft.lowStockThreshold,
                maxPerOrder = draft.maxPerOrder,
                voiceAlias = draft.voiceAlias,
                hasVariants = draft.hasVariants,
                variants = draft.variants,
                mfgDate = draft.mfgDate,
                expiryDate = draft.expiryDate,
                translationLanguages = draft.translationLanguages,
                nameTranslations = draft.nameTranslations,
                brandTranslations = draft.brandTranslations,
            )
        }
    }

    private fun AddEditInventoryUiState.toDraft(shopId: String, productId: String?) = ProductFormDraft(
        shopId = shopId,
        productId = productId ?: loadedProductId,
        name = name,
        nameHi = nameHi,
        brandName = brandName,
        category = category.name,
        categoryId = categoryId,
        subCategoryId = subCategoryId,
        unit = unit.name,
        barcode = barcode,
        sellingPrice = sellingPrice,
        costPrice = costPrice,
        stockQty = stockQty,
        lowStockThreshold = lowStockThreshold,
        maxPerOrder = maxPerOrder,
        voiceAlias = voiceAlias,
        hasVariants = hasVariants,
        variants = variants,
        mfgDate = mfgDate,
        expiryDate = expiryDate,
        translationLanguages = translationLanguages,
        nameTranslations = nameTranslations,
        brandTranslations = brandTranslations,
        imageUrls = imageUploader.readyUrls(),
    )

    private fun ProductFormDraft.formSnapshot(): List<Any?> = listOf(
        name, nameHi, brandName, category, categoryId, subCategoryId, unit, barcode, sellingPrice, costPrice,
        stockQty, lowStockThreshold, maxPerOrder, voiceAlias, hasVariants, variants, mfgDate, expiryDate,
        translationLanguages, nameTranslations, brandTranslations, imageUrls,
    )

    private fun fillFrom(product: Product) {
        loadedProduct = product
        manualFields.clear()
        translatedFrom.clear()
        val loadedSource = TranslationSource(product.name.trim(), product.brandName.trim())
        ((product.translations + product.brandTranslations).filterValues { it.isNotBlank() }.keys)
            .forEach { translatedFrom[it] = loadedSource }
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
