package org.bhargav.pansariwala.feature.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.analytics.Analytics
import org.bhargav.pansariwala.analytics.AnalyticsEvent
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.ShopOfferDto
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderChannel
import org.bhargav.pansariwala.domain.model.OrderItem
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductUnit
import org.bhargav.pansariwala.domain.model.ProductVariant
import org.bhargav.pansariwala.domain.model.SellMode
import org.bhargav.pansariwala.api.MasterCategoryDto
import org.bhargav.pansariwala.api.ProductVariantDto
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.api.toModel
import org.bhargav.pansariwala.feature.catalog.ProductDetailsDraft
import org.bhargav.pansariwala.feature.catalog.legacyCategory
import org.bhargav.pansariwala.feature.catalog.toDraftInput
import org.bhargav.pansariwala.feature.user.CartStore
import org.bhargav.pansariwala.feature.user.PurchaseOption
import org.bhargav.pansariwala.feature.user.VariantPickerState
import org.bhargav.pansariwala.feature.user.needsPicker
import org.bhargav.pansariwala.feature.user.options
import org.bhargav.pansariwala.util.AppClock
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.generateId
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.i18n.WALK_IN_CUSTOMER_KEY
import org.bhargav.pansariwala.notification.ShopNotifier
import org.bhargav.pansariwala.printing.PosPrintService
import org.bhargav.pansariwala.voice.ProductFuzzyMatcher
import org.bhargav.pansariwala.voice.SpeechEvent
import org.bhargav.pansariwala.voice.SpeechToText
import org.bhargav.pansariwala.voice.VoiceIntentParser
import org.bhargav.pansariwala.voice.VoiceLineItem
import pansariwala.shared.generated.resources.Res
import org.bhargav.pansariwala.feature.user.displayName
import org.bhargav.pansariwala.i18n.ANONYMOUS_CUSTOMER_KEY
import org.bhargav.pansariwala.util.asQuantity
import pansariwala.shared.generated.resources.unbilled_sale_over_stock
import pansariwala.shared.generated.resources.unbilled_sale_saved
import pansariwala.shared.generated.resources.error_generic
import pansariwala.shared.generated.resources.variant_saved
import pansariwala.shared.generated.resources.error_mic_permission
import pansariwala.shared.generated.resources.error_order_empty
import pansariwala.shared.generated.resources.error_shop_upi_missing
import pansariwala.shared.generated.resources.error_speech_unavailable
import pansariwala.shared.generated.resources.voice_confirm_product
import pansariwala.shared.generated.resources.voice_confirm_quantity
import pansariwala.shared.generated.resources.voice_not_understood
import pansariwala.shared.generated.resources.voice_out_of_stock
import pansariwala.shared.generated.resources.voice_partial_stock
import pansariwala.shared.generated.resources.scanner_added
import pansariwala.shared.generated.resources.scanner_camera_permission
import pansariwala.shared.generated.resources.scanner_not_found
import org.bhargav.pansariwala.api.ShopBillingDto
import org.bhargav.pansariwala.api.SubscriptionPlanDto
import org.bhargav.pansariwala.scanner.ScannerSettings
import org.bhargav.pansariwala.scanner.barcodeScannerSupported
import org.bhargav.pansariwala.scanner.scanFeedback

data class CartLine(
    val product: Product,
    val quantity: Double,
    val variant: ProductVariant? = null,
    val lockedUnitPrice: Double? = null,
) {
    val option: PurchaseOption get() = PurchaseOption(product, variant)
    val key: String get() = option.key
    val unitPrice: Double get() = lockedUnitPrice ?: option.unitPrice
    val lineTotal: Double get() = quantity * unitPrice
    /** Line total before the product offer (saved lines keep their stored price). */
    val baseLineTotal: Double get() = quantity * (lockedUnitPrice ?: option.basePrice)
}

/** A cart add paused until [option] gets a price; [quantity] / [targetKey] replay the original add. */
data class PriceRequest(val option: PurchaseOption, val quantity: Double? = null, val targetKey: String? = null)

data class OrderEditorUiState(
    val loading: Boolean = true,
    val isEditing: Boolean = false,
    /** Original order time when editing; null for a new order. */
    val createdAtEpochMs: Long? = null,
    val customerName: String = "",
    val searchQuery: String = "",
    val catalog: List<Product> = emptyList(),
    val cart: List<CartLine> = emptyList(),
    val variantPicker: VariantPickerState.Request? = null,
    /** Product whose "Add variant" dialog is open (long-press on a POS card). */
    val addVariantFor: Product? = null,
    val priceRequest: PriceRequest? = null,
    val priceDraft: ProductDetailsDraft = ProductDetailsDraft(),
    val masterCategories: List<MasterCategoryDto> = emptyList(),
    val saved: Boolean = false,
    val showPaymentPicker: Boolean = false,
    val showUpiQr: Boolean = false,
    val pendingPrint: Boolean = false,
    val shopUpiId: String = "",
    val shopName: String = "",
    val error: UiText? = null,
    val voiceMessages: List<UiText> = emptyList(),
    val isListening: Boolean = false,
    val partialTranscript: String = "",
    val requestMicPermission: Boolean = false,
    val offers: List<ShopOfferDto> = emptyList(),
    val manualDiscountInput: String = "",
    val manualDiscountPercent: Boolean = false,
    val voiceSearchEnabled: Boolean = true,
    /** Device setting: confident voice matches go to the cart; off = always confirm in the product sheet. */
    val voiceAutoAdd: Boolean = true,
    val barcodeSearchEnabled: Boolean = true,
    /** Camera scanner entitlement from master / subscription. */
    val barcodeScannerEnabled: Boolean = false,
    val scannerSettings: ScannerSettings = ScannerSettings(),
    /** User paused the camera from the toolbar; preview stays hidden until resumed. */
    val scannerPaused: Boolean = false,
    val showScannerSettings: Boolean = false,
    val showScannerUpsell: Boolean = false,
    val scanMessage: UiText? = null,
    /** Portrait-only: cart sheet expanded over the product list. */
    val showCartSheet: Boolean = false,
    val billing: ShopBillingDto? = null,
    val scannerPlans: List<SubscriptionPlanDto> = emptyList(),
    /** Product whose full details sheet is open (tap on the product image). */
    val infoOption: PurchaseOption? = null,
    /** Cart line whose price is being changed for this bill only. */
    val priceEditKey: String? = null,
    /** Option being recorded as sold earlier without a bill (anonymous customer). */
    val unbilledSaleOption: PurchaseOption? = null,
) {
    val cartQuantityByProduct: Map<String, Double>
        get() = cart.groupBy { it.product.id }.mapValues { (_, lines) -> lines.sumOf { it.quantity } }
    val bill: BillTotals get() = computeBill(cart, offers, manualDiscountInput, manualDiscountPercent)
    val total: Double get() = bill.total
    val itemCount: Int get() = cart.size
    val filteredCatalog: List<Product>
        get() {
            if (searchQuery.isBlank()) return catalog
            val q = searchQuery.trim().lowercase()
            return catalog.filter {
                it.name.lowercase().contains(q) ||
                    it.nameHi.contains(searchQuery.trim()) ||
                    (it.voiceAlias?.lowercase()?.contains(q) == true) ||
                    (barcodeSearchEnabled && it.barcode?.contains(q) == true)
            }
        }
}

class OrderEditorViewModel(
    private val shopRepository: ShopRepository,
    private val preferences: AppPreferences,
    private val analytics: Analytics,
    private val speechToText: SpeechToText,
    private val shopNotifier: ShopNotifier,
    private val printService: PosPrintService,
    private val api: PansariApi,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderEditorUiState())
    val uiState: StateFlow<OrderEditorUiState> = _uiState.asStateFlow()

    fun dismissError() { _uiState.update { it.copy(error = null) } }

    private var shopId: String = SeedData.DEMO_SHOP_ID
    private var editingOrderId: String? = null
    private var createdAt: Long = AppClock.nowMillis()
    private var speechJob: Job? = null
    private var partialCommitJob: Job? = null
    private var lastAppliedUtterance: String? = null
    /** Voice lines waiting for the cashier to confirm in the product sheet, shown one at a time. */
    private val pendingVoicePicks = ArrayDeque<VariantPickerState.Request>()

    private var barcodeIndex: Map<String, PurchaseOption> = emptyMap()
    private var indexedCatalog: List<Product>? = null
    private var scanMessageJob: Job? = null

    init {
        viewModelScope.launch {
            preferences.shopFeatures.collect { features ->
                if (!features.voiceSearch && _uiState.value.isListening) cancelListening()
                _uiState.update {
                    it.copy(
                        voiceSearchEnabled = features.voiceSearch,
                        barcodeSearchEnabled = features.barcodeSearch,
                        barcodeScannerEnabled = features.barcodeScanner && barcodeScannerSupported,
                    )
                }
            }
        }
        viewModelScope.launch {
            preferences.scannerSettings.collect { settings -> _uiState.update { it.copy(scannerSettings = settings) } }
        }
        viewModelScope.launch {
            preferences.voiceAutoAdd.collect { enabled -> _uiState.update { it.copy(voiceAutoAdd = enabled) } }
        }
        speechJob = viewModelScope.launch {
            speechToText.events.collect { event ->
                when (event) {
                    SpeechEvent.Started -> {
                        partialCommitJob?.cancel()
                        // Keep lastAppliedUtterance across Android/iOS recognition
                        // restarts inside one mic session so we don't re-add the
                        // previous item when a new cycle begins.
                        _uiState.update {
                            it.copy(
                                isListening = true,
                                error = null,
                                partialTranscript = "",
                            )
                        }
                    }
                    is SpeechEvent.PartialResult -> {
                        handleIncomingTranscript(event.text, isFinal = false)
                    }
                    is SpeechEvent.FinalResult -> {
                        partialCommitJob?.cancel()
                        handleIncomingTranscript(event.text, isFinal = true)
                    }
                    is SpeechEvent.Error -> {
                        if (event.needsPermission) {
                            _uiState.update {
                                it.copy(
                                    isListening = false,
                                    requestMicPermission = true,
                                    error = UiText.res(Res.string.error_mic_permission),
                                )
                            }
                        } else {
                            // Never surface raw platform STT errors (e.g. iOS
                            // kAFAssistantErrorDomain 216) into the order screen.
                            // Soft failures are recovered by continuous restart.
                            if (!_uiState.value.isListening) {
                                _uiState.update {
                                    it.copy(
                                        error = event.message.takeIf { msg -> msg.isNotBlank() }
                                            ?.let { UiText.Plain(it) },
                                    )
                                }
                            }
                        }
                    }
                    SpeechEvent.Ended -> {
                        partialCommitJob?.cancel()
                        val leftover = _uiState.value.partialTranscript
                        if (leftover.isNotBlank()) {
                            applyVoiceUtterance(leftover)
                        }
                        _uiState.update {
                            it.copy(isListening = false, partialTranscript = "")
                        }
                    }
                }
            }
        }
    }

    /**
     * Shows only the not-yet-added part of the transcript. After an item is
     * committed, the previous phrase is hidden so the UI is ready for the next item.
     */
    private fun handleIncomingTranscript(text: String, isFinal: Boolean) {
        val normalized = text.trim().replace(Regex("""\s+"""), " ")
        if (normalized.isBlank()) return

        val applied = lastAppliedUtterance
        val displayText = when {
            applied == null -> normalized
            // STT is still echoing the phrase we already added — keep UI empty.
            normalized.equals(applied, ignoreCase = true) ||
                applied.startsWith(normalized, ignoreCase = true) -> ""
            // Same session grew with a new item after the previous one.
            normalized.startsWith(applied, ignoreCase = true) -> {
                normalized
                    .drop(applied.length)
                    .trim()
                    .replace(
                        Regex("""^(,|;|और|\baur\b|\band\b|\bplus\b)\s*""", RegexOption.IGNORE_CASE),
                        "",
                    )
                    .trim()
            }
            else -> normalized
        }

        _uiState.update {
            it.copy(partialTranscript = displayText, isListening = true)
        }

        if (isFinal) {
            applyVoiceUtterance(normalized)
        } else if (displayText.isNotBlank()) {
            schedulePartialCommit(normalized)
        }
    }

    /**
     * Android/iOS may keep an utterance as a partial result while continuous
     * listening remains active. Once a complete command (quantity + unit) is
     * stable briefly, commit it without waiting for cancel or a platform final.
     */
    private fun schedulePartialCommit(text: String) {
        partialCommitJob?.cancel()
        val toParse = pendingParseText(text)
        if (toParse.isBlank()) return
        val parsed = VoiceIntentParser.parse(toParse)
        if (parsed.isEmpty() || parsed.any { it.unit == null }) return

        partialCommitJob = viewModelScope.launch {
            delay(PARTIAL_COMMIT_DELAY_MS)
            applyVoiceUtterance(text)
        }
    }

    private fun pendingParseText(utterance: String): String {
        val normalized = utterance.trim().replace(Regex("""\s+"""), " ")
        val previouslyApplied = lastAppliedUtterance ?: return normalized
        if (!normalized.startsWith(previouslyApplied, ignoreCase = true)) return normalized
        return normalized
            .drop(previouslyApplied.length)
            .trim()
            .replace(Regex("""^(,|;|और|\baur\b|\band\b|\bplus\b)\s*""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    fun load(orderId: String?) {
        viewModelScope.launch {
            shopRepository.ensureSeeded()
            shopId = preferences.getShopId() ?: SeedData.DEMO_SHOP_ID
            val allProducts = shopRepository.observeProducts(shopId).first()
            val byId = allProducts.associateBy { it.id }
            val catalog = allProducts.filter { it.isActive }

            val existing = orderId?.let { shopRepository.getOrder(it) }
            if (existing != null) {
                editingOrderId = existing.id
                createdAt = existing.createdAtEpochMs
                val cart = existing.items.mapNotNull { item ->
                    byId[item.productId]?.let {
                        CartLine(it, item.quantity, item.variantId?.let(it::variant), lockedUnitPrice = item.unitPrice)
                    }
                }
                // Edits keep the original bill discount as a flat amount instead of re-applying today's offers.
                _uiState.update {
                    it.copy(
                        loading = false,
                        isEditing = true,
                        createdAtEpochMs = existing.createdAtEpochMs,
                        customerName = existing.customerName.orEmpty(),
                        catalog = catalog,
                        cart = cart,
                        manualDiscountInput = if (existing.discount > 0.0) existing.discount.asMoneyInput() else "",
                    )
                }
            } else {
                _uiState.update { it.copy(loading = false, catalog = catalog) }
                loadOffers()
            }
        }
        loadShopPaymentInfo()
    }

    private fun loadOffers() {
        viewModelScope.launch {
            runCatching { api.shopManagedOffers(all = false) }
                .onSuccess { offers ->
                    _uiState.update { s ->
                        val catalog = s.catalog.withOffers(offers)
                        val byId = catalog.associateBy { it.id }
                        s.copy(
                            offers = offers,
                            catalog = catalog,
                            cart = s.cart.map { line ->
                                if (line.lockedUnitPrice != null) line else line.copy(product = byId[line.product.id] ?: line.product)
                            },
                        )
                    }
                }
                .onFailure { it.rethrowIfStructuredCancellation() }
        }
    }

    private fun loadShopPaymentInfo() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(shopUpiId = preferences.getShopUpiId(), shopName = preferences.getShopName())
            }
            runCatching { api.shopPaymentInfo() }
                .onSuccess { info ->
                    preferences.setShopPaymentInfo(info.shopName, info.upiId)
                    _uiState.update { it.copy(shopUpiId = info.upiId, shopName = info.shopName) }
                }
                .onFailure { it.rethrowIfStructuredCancellation() }
        }
    }

    fun onSearchChange(value: String) = _uiState.update { it.copy(searchQuery = value) }
    fun onCustomerNameChange(value: String) = _uiState.update { it.copy(customerName = value) }

    fun onManualDiscountChange(value: String) {
        val cleaned = value.filter { it.isDigit() || it == '.' }
        if (cleaned.count { it == '.' } > 1) return
        _uiState.update { it.copy(manualDiscountInput = cleaned) }
    }

    fun onManualDiscountPercentChange(percent: Boolean) =
        _uiState.update { it.copy(manualDiscountPercent = percent) }

    fun onMicClick() {
        analytics.log(AnalyticsEvent.ButtonClicked("voice_mic", "order_editor"))
        if (_uiState.value.isListening) {
            cancelListening()
            return
        }
        if (!_uiState.value.voiceSearchEnabled) return
        if (!speechToText.isAvailable()) {
            _uiState.update {
                it.copy(error = UiText.res(Res.string.error_speech_unavailable))
            }
            return
        }
        _uiState.update {
            it.copy(
                requestMicPermission = true,
                error = null,
                voiceMessages = emptyList(),
            )
        }
    }

    fun onMicPermissionResult(granted: Boolean) {
        _uiState.update { it.copy(requestMicPermission = false) }
        if (!granted) {
            _uiState.update {
                it.copy(error = UiText.res(Res.string.error_mic_permission))
            }
            return
        }
        if (!_uiState.value.voiceSearchEnabled) return
        lastAppliedUtterance = null
        partialCommitJob?.cancel()
        _uiState.update { it.copy(error = null, partialTranscript = "", voiceMessages = emptyList()) }
        speechToText.startListening()
    }

    fun consumeMicPermissionRequest() {
        _uiState.update { it.copy(requestMicPermission = false) }
    }

    fun cancelListening() {
        analytics.log(AnalyticsEvent.ButtonClicked("voice_cancel", "order_editor"))
        partialCommitJob?.cancel()
        speechToText.cancel()
        lastAppliedUtterance = null
        _uiState.update {
            it.copy(isListening = false, partialTranscript = "", error = null)
        }
    }

    fun addProduct(product: Product) {
        if (product.needsPicker) openPicker(product) else addOption(product.options().first())
    }

    /** One variant card: packs add one, loose options open the amount sheet. */
    fun addVariant(option: PurchaseOption) {
        if (option.needsPicker) openPicker(option.product, optionKey = option.key) else addOption(option)
    }

    fun openPicker(product: Product, editKey: String? = null, optionKey: String? = null) {
        _uiState.update { it.copy(variantPicker = VariantPickerState.Request(product, editKey, optionKey)) }
    }

    fun startAddVariant(product: Product) = _uiState.update { it.copy(addVariantFor = product) }

    fun dismissAddVariant() = _uiState.update { it.copy(addVariantFor = null) }

    fun dismissPriceRequest() = _uiState.update { it.copy(priceRequest = null) }

    fun updatePriceDraft(transform: (ProductDetailsDraft) -> ProductDetailsDraft) =
        _uiState.update { it.copy(priceDraft = transform(it.priceDraft)) }

    /** Opens the price sheet when [option] has no price; returns true when the add must wait. */
    private fun needsPrice(option: PurchaseOption, quantity: Double? = null, targetKey: String? = null): Boolean {
        if (option.basePrice > 0.0) return false
        val product = option.product
        _uiState.update {
            it.copy(
                priceRequest = PriceRequest(option, quantity, targetKey),
                priceDraft = ProductDetailsDraft(
                    costPrice = product.costPrice.toDraftInput(),
                    stockQty = (option.variant?.stockQty ?: product.stockQty).toDraftInput(),
                    lowStockThreshold = product.lowStockThreshold.toDraftInput(),
                    categoryId = product.categoryId,
                    subCategoryId = product.subCategoryId,
                ),
                variantPicker = null,
            )
        }
        loadMasterCategories()
        return true
    }

    private fun loadMasterCategories() {
        if (_uiState.value.masterCategories.isNotEmpty()) return
        viewModelScope.launch {
            runCatching { api.masterCategories() }
                .onSuccess { all -> _uiState.update { it.copy(masterCategories = all) } }
                .onFailure { it.rethrowIfStructuredCancellation() }
        }
    }

    /** Saves the price (and edited stock/category) on the product, syncs it, then finishes the add. */
    fun confirmPrice() {
        val state = _uiState.value
        val request = state.priceRequest ?: return
        val draft = state.priceDraft
        val price = draft.sellingPriceValue
        if (price <= 0.0) return
        val product = request.option.product
        val variant = request.option.variant
        val linkedBundle = variant?.sellMode == SellMode.BUNDLE && variant.baseVariantId.isNotBlank()
        val updated = product.copy(
            sellingPrice = if (variant == null || product.sellingPrice <= 0.0) price else product.sellingPrice,
            costPrice = draft.costPriceValue,
            stockQty = if (variant == null) draft.stockQtyValue else product.stockQty,
            lowStockThreshold = draft.lowStockValue,
            category = state.masterCategories.legacyCategory(draft.categoryId, product.category),
            categoryId = draft.categoryId,
            subCategoryId = draft.subCategoryId,
            variants = product.variants.map { v ->
                if (v.id != variant?.id) v
                else v.copy(price = price, stockQty = if (linkedBundle) v.stockQty else draft.stockQtyValue)
            },
        )
        val option = PurchaseOption(updated, updated.variant(variant?.id))
        viewModelScope.launch {
            runCatching { shopRepository.upsertProduct(updated.copy(offer = null)) }
                .onSuccess {
                    _uiState.update { s ->
                        s.copy(
                            priceRequest = null,
                            catalog = s.catalog.map { if (it.id == product.id) updated else it },
                            cart = s.cart.map { if (it.product.id == product.id) it.copy(product = updated) else it },
                        )
                    }
                    when {
                        request.targetKey != null ->
                            upsertOption(request.targetKey, option, request.quantity ?: maxOf(option.minQty, option.step))
                        request.quantity != null -> addToCart(option, request.quantity)
                        else -> addOption(option)
                    }
                }
                .onFailure { error -> _uiState.update { it.copy(error = error.toApiUiText()) } }
        }
    }

    /**
     * Adds [draft] to the product. A product without variants first turns its current pack into a
     * variant labelled [currentLabel], so its price, stock and barcode stay sellable.
     */
    fun saveVariant(product: Product, draft: ProductVariantDto, currentLabel: String) {
        val created = draft.copy(name = draft.name.trim(), id = generateId("v")).toModel()
        val existing = product.variants.ifEmpty {
            listOf(
                ProductVariant(
                    id = generateId("v"),
                    label = currentLabel.trim(),
                    sellMode = if (product.isLooseUnit) SellMode.LOOSE else SellMode.PACK,
                    price = product.sellingPrice,
                    baseUnit = if (product.isLooseUnit) product.unit else ProductUnit.PIECE,
                    stockQty = product.stockQty,
                    barcode = product.barcode,
                ),
            )
        }
        val updated = product.copy(variants = existing + created)
        viewModelScope.launch {
            shopRepository.upsertProduct(updated.copy(offer = null))
            _uiState.update { state ->
                state.copy(
                    addVariantFor = null,
                    catalog = state.catalog.map { if (it.id == product.id) updated else it },
                    cart = state.cart.map { if (it.product.id == product.id) it.copy(product = updated) else it },
                )
            }
            showScanMessage(UiText.res(Res.string.variant_saved, product.name))
        }
    }

    fun setQuantity(key: String, quantity: Double) {
        _uiState.update { state ->
            state.copy(
                cart = state.cart.mapNotNull { line ->
                    when {
                        line.key != key -> line
                        quantity <= AppConstants.LooseQty.EPSILON -> null
                        else -> line.copy(quantity = line.option.normalize(quantity))
                    }
                },
            )
        }
    }

    fun dismissPicker() {
        _uiState.update { it.copy(variantPicker = null) }
        showNextVoicePick()
    }

    private fun queueVoicePick(request: VariantPickerState.Request) {
        if (_uiState.value.variantPicker == null) {
            _uiState.update { it.copy(variantPicker = request) }
        } else if (pendingVoicePicks.none { it.product.id == request.product.id }) {
            pendingVoicePicks.addLast(request)
        }
    }

    private fun showNextVoicePick() {
        if (_uiState.value.variantPicker != null) return
        val next = pendingVoicePicks.removeFirstOrNull() ?: return
        _uiState.update { it.copy(variantPicker = next) }
    }

    fun changeQuantity(key: String, direction: Int) {
        _uiState.update { state ->
            val cart = state.cart.mapNotNull { line ->
                if (line.key != key) {
                    line
                } else {
                    val next = line.quantity + direction * line.option.step
                    if (next < line.option.minQty - AppConstants.LooseQty.EPSILON) null
                    else line.copy(quantity = line.option.normalize(next))
                }
            }
            state.copy(cart = cart)
        }
    }

    fun openProductInfo(option: PurchaseOption) = _uiState.update { it.copy(infoOption = option) }

    fun dismissProductInfo() = _uiState.update { it.copy(infoOption = null) }

    fun openLinePrice(key: String) = _uiState.update { it.copy(priceEditKey = key) }

    fun dismissLinePrice() = _uiState.update { it.copy(priceEditKey = null) }

    /** Price for this bill only; null goes back to the catalog price. The product itself is not changed. */
    fun setLinePrice(key: String, price: Double?) {
        if (price != null && price < 0.0) return
        _uiState.update { state ->
            state.copy(
                cart = state.cart.map { if (it.key == key) it.copy(lockedUnitPrice = price) else it },
                priceEditKey = null,
            )
        }
    }

    fun openUnbilledSale(option: PurchaseOption) =
        _uiState.update { it.copy(unbilledSaleOption = option, infoOption = null) }

    fun dismissUnbilledSale() = _uiState.update { it.copy(unbilledSaleOption = null) }

    /**
     * Stock that left the shop without a bill: saved as a completed sale to an anonymous customer so
     * stock and sales reports match the shelf. Does not touch the open cart.
     */
    fun recordUnbilledSale(quantity: Double) {
        val option = _uiState.value.unbilledSaleOption ?: return
        val qty = option.normalize(quantity)
        if (qty <= AppConstants.LooseQty.EPSILON) return
        if (qty > option.stock + AppConstants.LooseQty.EPSILON) {
            _uiState.update { it.copy(error = UiText.res(Res.string.unbilled_sale_over_stock)) }
            return
        }
        val order = Order(
            id = generateId("order"),
            shopId = shopId,
            createdAtEpochMs = AppClock.nowMillis(),
            status = OrderStatus.COMPLETED,
            customerName = ANONYMOUS_CUSTOMER_KEY,
            channel = OrderChannel.POS,
            paymentMethod = AppConstants.PosPayment.CASH,
            items = listOf(
                OrderItem(
                    productId = option.product.id,
                    productName = option.product.name,
                    unit = option.product.unit,
                    quantity = qty,
                    unitPrice = option.unitPrice,
                    variantId = option.variant?.id,
                    variantLabel = option.variant?.label,
                ),
            ),
        )
        viewModelScope.launch {
            _uiState.update { it.copy(unbilledSaleOption = null) }
            runCatching { shopRepository.saveOrder(order) }
                .onSuccess {
                    analytics.log(AnalyticsEvent.ButtonClicked(buttonId = "unbilled_sale", screen = "order_editor"))
                    showScanMessage(UiText.res(Res.string.unbilled_sale_saved, qty.asQuantity(), option.displayName))
                }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _uiState.update { it.copy(error = UiText.res(Res.string.error_generic)) }
                }
        }
    }

    fun removeLine(key: String) {
        _uiState.update { state -> state.copy(cart = state.cart.filterNot { it.key == key }) }
    }

    /** Adds or replaces a line from the variant picker; [targetKey] is the line being edited. */
    fun upsertOption(targetKey: String, option: PurchaseOption, quantity: Double) {
        if (needsPrice(option, quantity, targetKey)) return
        _uiState.update { state ->
            val qty = option.normalize(quantity)
            val keptPrice = state.cart.firstOrNull { it.key == targetKey && it.key == option.key }?.lockedUnitPrice
            val replaced = CartLine(option.product, qty, option.variant, keptPrice)
            val others = state.cart.filterNot { it.key == targetKey || it.key == option.key }
            val index = state.cart.indexOfFirst { it.key == targetKey }.takeIf { it >= 0 } ?: others.size
            state.copy(
                cart = others.toMutableList().apply { add(index.coerceAtMost(size), replaced) },
                variantPicker = null,
                error = null,
            )
        }
        showNextVoicePick()
    }

    fun save(print: Boolean = false) {
        if (_uiState.value.cart.isEmpty()) {
            _uiState.update { it.copy(error = UiText.res(Res.string.error_order_empty)) }
            return
        }
        _uiState.update { it.copy(showPaymentPicker = true, pendingPrint = print, error = null) }
    }

    fun dismissPaymentPicker() {
        _uiState.update { it.copy(showPaymentPicker = false, showUpiQr = false) }
    }

    fun selectPayment(method: String) {
        when (method) {
            AppConstants.PosPayment.UPI -> {
                if (_uiState.value.shopUpiId.isBlank()) {
                    _uiState.update { it.copy(error = UiText.res(Res.string.error_shop_upi_missing)) }
                } else {
                    _uiState.update { it.copy(showUpiQr = true) }
                }
            }
            AppConstants.PosPayment.CASH, AppConstants.PosPayment.CARD -> completePayment(method)
        }
    }

    fun backFromUpiQr() {
        _uiState.update { it.copy(showUpiQr = false) }
    }

    fun confirmUpiReceived() = completePayment(AppConstants.PosPayment.UPI)

    private fun completePayment(method: String) {
        val print = _uiState.value.pendingPrint
        _uiState.update { it.copy(showPaymentPicker = false, showUpiQr = false) }
        commitSale(print, method)
    }

    private fun commitSale(print: Boolean, paymentMethod: String) {
        val state = _uiState.value
        if (state.cart.isEmpty()) {
            _uiState.update { it.copy(error = UiText.res(Res.string.error_order_empty)) }
            return
        }
        analytics.log(
            AnalyticsEvent.ButtonClicked(
                buttonId = if (state.isEditing) "order_update" else "order_create",
                screen = "order_editor",
            ),
        )
        val order = Order(
            id = editingOrderId ?: generateId("order"),
            shopId = shopId,
            createdAtEpochMs = createdAt,
            status = OrderStatus.COMPLETED,
            customerName = state.customerName.trim().ifBlank { WALK_IN_CUSTOMER_KEY },
            channel = OrderChannel.POS,
            paymentMethod = paymentMethod,
            discount = state.bill.discount,
            items = state.cart.map { line ->
                OrderItem(
                    productId = line.product.id,
                    productName = line.product.name,
                    unit = line.product.unit,
                    quantity = line.quantity,
                    unitPrice = line.unitPrice,
                    variantId = line.variant?.id,
                    variantLabel = line.variant?.label,
                )
            },
        )
        viewModelScope.launch {
            if (_uiState.value.isListening) speechToText.cancel()
            val existing = editingOrderId?.let { shopRepository.getOrder(it) }
            val toSave = if (existing?.status == OrderStatus.CANCELLED) {
                order.copy(status = OrderStatus.CANCELLED, cancelReason = existing.cancelReason)
            } else {
                order.copy(cancelReason = existing?.cancelReason)
            }
            shopRepository.saveOrder(toSave)
            if (toSave.status != OrderStatus.CANCELLED) shopNotifier.onPosSaleSaved(toSave)
            if (existing == null || print) {
                printService.onOrderSaved(toSave, printRequested = print)
            }
            pendingVoicePicks.clear()
            _uiState.update {
                it.copy(saved = true, error = null, isListening = false, showPaymentPicker = false)
            }
        }
    }

    private fun applyVoiceUtterance(utterance: String) {
        val normalized = utterance.trim().replace(Regex("""\s+"""), " ")
        if (normalized.isBlank()) return
        // Continuous STT can emit the same phrase via FinalResult and again on Ended.
        if (normalized.equals(lastAppliedUtterance, ignoreCase = true)) return

        val textToParse = pendingParseText(normalized)
        if (textToParse.isBlank()) return

        val lines = VoiceIntentParser.parse(textToParse)
        if (lines.isEmpty()) {
            // Continuous mic picks up shop chatter; only complain when it wasn't open-mic noise.
            if (!_uiState.value.isListening) {
                val msg = UiText.res(Res.string.voice_not_understood, normalized)
                _uiState.update {
                    it.copy(
                        voiceMessages = it.voiceMessages + msg,
                        error = msg,
                    )
                }
            }
            return
        }

        lastAppliedUtterance = normalized
        partialCommitJob?.cancel()

        val catalog = _uiState.value.catalog
        val autoAdd = _uiState.value.voiceAutoAdd
        val messages = mutableListOf<UiText>()
        var addedAny = false
        for (line in lines) {
            val candidates = ProductFuzzyMatcher.findCandidates(line.productQuery, catalog, VOICE_MATCH_CANDIDATES)
            val match = candidates.firstOrNull()
            if (match == null) {
                // No unit + no catalog match → background speech, not an order line.
                if (line.unit != null) {
                    messages += UiText.res(Res.string.voice_out_of_stock, line.productQuery)
                }
                continue
            }
            val product = match.product
            val label = product.nameHi.ifBlank { product.name }
            val confident = ProductFuzzyMatcher.isConfident(candidates)
            val variant = ProductFuzzyMatcher.findVariant(line.productQuery, line.quantity, line.unit, product)
            val option = variant?.let { PurchaseOption(product, it) } ?: product.options().singleOrNull()
            val qty = option?.let { voiceQuantity(it, line) }
            val alreadyInCart = option?.let { o -> _uiState.value.cart.firstOrNull { it.key == o.key }?.quantity } ?: 0.0
            val available = option?.let { it.stock - alreadyInCart } ?: 0.0

            if (option != null && confident && available <= 0.0) {
                messages += UiText.res(Res.string.voice_out_of_stock, label)
                continue
            }
            val fits = option != null && qty != null && qty <= available + AppConstants.LooseQty.EPSILON
            if (autoAdd && confident && fits && option != null && qty != null) {
                addToCart(option, qty)
                addedAny = true
                continue
            }
            // Never guess: unsure product, unit, quantity or stock → cashier confirms in the product sheet.
            when {
                !confident -> messages += UiText.res(Res.string.voice_confirm_product, label)
                option != null && qty != null && qty > available -> messages +=
                    UiText.res(Res.string.voice_partial_stock, label, available.toInt())
                autoAdd -> messages += UiText.res(Res.string.voice_confirm_quantity, label)
            }
            queueVoicePick(
                VariantPickerState.Request(
                    product = product,
                    editKey = null,
                    initialOptionKey = option?.key,
                    initialQty = qty?.let { it + alreadyInCart },
                ),
            )
            addedAny = true
        }

        // Reset listened text so the next spoken item starts with an empty transcript.
        _uiState.update {
            it.copy(
                partialTranscript = "",
                voiceMessages = if (messages.isEmpty()) it.voiceMessages else it.voiceMessages + messages,
                error = messages.lastOrNull() ?: if (addedAny) null else it.error,
            )
        }
    }

    /**
     * Cart quantity a voice line asks for in [option]'s unit, or null when it can't be read without guessing
     * (no unit for a loose item, weight spoken for a counted item, below the minimum, …).
     */
    private fun voiceQuantity(option: PurchaseOption, line: VoiceLineItem): Double? {
        val spokenUnit = line.unit
        val qty = when {
            option.isLoose -> {
                if (spokenUnit == null || !ProductFuzzyMatcher.unitsCompatible(spokenUnit, option.quantityUnit)) return null
                ProductFuzzyMatcher.convertQuantity(line.quantity, spokenUnit, option.quantityUnit)
            }
            spokenUnit == null || spokenUnit == ProductUnit.PIECE || spokenUnit == ProductUnit.PACKET -> line.quantity
            option.variant != null && ProductFuzzyMatcher.matchesPackSize(option.variant, line.quantity, spokenUnit) -> 1.0
            option.variant == null && ProductFuzzyMatcher.unitsCompatible(spokenUnit, option.product.unit) ->
                ProductFuzzyMatcher.convertQuantity(line.quantity, spokenUnit, option.product.unit)
            else -> return null
        }
        if (qty.isNaN() || qty <= 0.0 || qty < option.minQty - AppConstants.LooseQty.EPSILON) return null
        return qty
    }

    private fun addToCart(option: PurchaseOption, quantity: Double) {
        if (quantity <= 0.0) return
        if (needsPrice(option, quantity)) return
        _uiState.update { state ->
            val key = option.key
            val existing = state.cart.firstOrNull { it.key == key }
            val cart = if (existing != null) {
                state.cart.map {
                    if (it.key == key) it.copy(quantity = it.quantity + quantity) else it
                }
            } else {
                state.cart + CartLine(option.product, quantity, option.variant)
            }
            state.copy(cart = cart, error = null)
        }
    }

    /** Re-reads features + billing so a purchase made from the upsell takes effect without re-login. */
    fun refreshEntitlements() {
        viewModelScope.launch {
            runCatching { api.shopProfile() }
                .onSuccess { preferences.setShopFeatures(it.shop.features) }
                .onFailure { it.rethrowIfStructuredCancellation() }
            runCatching { api.shopSubscription() }
                .onSuccess { sub ->
                    _uiState.update { s ->
                        s.copy(
                            billing = sub.billing,
                            scannerPlans = sub.plans.filter { AppConstants.ShopFeature.BARCODE_SCANNER in it.features },
                        )
                    }
                }
                .onFailure { it.rethrowIfStructuredCancellation() }
        }
    }

    fun onScannerAvatarClick() {
        val state = _uiState.value
        if (state.barcodeScannerEnabled) {
            _uiState.update { it.copy(showScannerSettings = true) }
        } else {
            analytics.log(AnalyticsEvent.ButtonClicked("barcode_scanner_locked", "order_editor"))
            _uiState.update { it.copy(showScannerUpsell = true) }
        }
    }

    fun dismissScannerSettings() = _uiState.update { it.copy(showScannerSettings = false) }
    fun dismissScannerUpsell() = _uiState.update { it.copy(showScannerUpsell = false) }

    fun saveScannerSettings(settings: ScannerSettings, cameraOn: Boolean = !_uiState.value.scannerPaused) {
        _uiState.update { it.copy(showScannerSettings = false, scannerSettings = settings, scannerPaused = !cameraOn) }
        viewModelScope.launch { preferences.setScannerSettings(settings) }
    }

    fun switchCamera() = saveScannerSettings(_uiState.value.scannerSettings.switchLens())

    fun onScannerPermissionDenied() {
        _uiState.update { it.copy(scannerPaused = true, error = UiText.res(Res.string.scanner_camera_permission)) }
    }

    fun onBarcodeScanned(code: String) {
        val state = _uiState.value
        if (!state.barcodeScannerEnabled || state.variantPicker != null || state.showPaymentPicker) return
        val option = barcodeOption(state.catalog, code)
        if (option == null) {
            showScanMessage(UiText.res(Res.string.scanner_not_found, code))
            return
        }
        scanFeedback(state.scannerSettings.beepOnScan, state.scannerSettings.vibrateOnScan)
        if (option.variant == null && option.product.needsPicker) {
            openPicker(option.product)
        } else if (option.needsPicker) {
            openPicker(option.product, optionKey = option.key)
        } else if (!needsPrice(option)) {
            addOption(option)
            showScanMessage(UiText.res(Res.string.scanner_added, option.product.name))
        }
    }

    fun setCartSheetVisible(visible: Boolean) = _uiState.update { it.copy(showCartSheet = visible) }

    private fun barcodeOption(catalog: List<Product>, code: String): PurchaseOption? {
        if (indexedCatalog !== catalog) {
            barcodeIndex = buildMap {
                catalog.forEach { product ->
                    product.barcode?.trim()?.takeIf(String::isNotEmpty)?.let { code ->
                        if (code !in this) put(code, PurchaseOption(product, null))
                    }
                    product.activeVariants.forEach { variant ->
                        variant.barcode?.trim()?.takeIf(String::isNotEmpty)?.let { put(it, PurchaseOption(product, variant)) }
                    }
                }
            }
            indexedCatalog = catalog
        }
        return barcodeIndex[code.trim()]
    }

    private fun addOption(option: PurchaseOption) {
        if (needsPrice(option)) return
        _uiState.update { state ->
            val existing = state.cart.firstOrNull { it.key == option.key }
            val cart = if (existing != null) {
                state.cart.map {
                    if (it.key == option.key) it.copy(quantity = option.normalize(it.quantity + option.step)) else it
                }
            } else {
                state.cart + CartLine(option.product, option.normalize(maxOf(option.minQty, option.step)), option.variant)
            }
            state.copy(cart = cart, error = null)
        }
    }

    private fun showScanMessage(message: UiText) {
        scanMessageJob?.cancel()
        _uiState.update { it.copy(scanMessage = message) }
        scanMessageJob = viewModelScope.launch {
            delay(SCAN_MESSAGE_MS)
            _uiState.update { it.copy(scanMessage = null) }
        }
    }

    override fun onCleared() {
        speechToText.cancel()
        partialCommitJob?.cancel()
        speechJob?.cancel()
        super.onCleared()
    }

    companion object {
        private const val PARTIAL_COMMIT_DELAY_MS = 700L
        /** Best + runner-up, enough to detect an ambiguous match. */
        private const val VOICE_MATCH_CANDIDATES = 2
        private const val SCAN_MESSAGE_MS = 2_000L
    }
}

private fun Double.asMoneyInput(): String {
    val paise = kotlin.math.round(this * 100).toLong()
    return if (paise % 100 == 0L) (paise / 100).toString() else "${paise / 100}.${(paise % 100).toString().padStart(2, '0')}"
}
