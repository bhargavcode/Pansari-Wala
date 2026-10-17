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
import org.bhargav.pansariwala.domain.model.ProductVariant
import org.bhargav.pansariwala.feature.user.CartStore
import org.bhargav.pansariwala.feature.user.PurchaseOption
import org.bhargav.pansariwala.feature.user.VariantPickerState
import org.bhargav.pansariwala.feature.user.needsPicker
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
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_mic_permission
import pansariwala.shared.generated.resources.error_order_empty
import pansariwala.shared.generated.resources.error_shop_upi_missing
import pansariwala.shared.generated.resources.error_speech_unavailable
import pansariwala.shared.generated.resources.voice_not_understood
import pansariwala.shared.generated.resources.voice_out_of_stock
import pansariwala.shared.generated.resources.voice_partial_stock

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

data class OrderEditorUiState(
    val loading: Boolean = true,
    val isEditing: Boolean = false,
    val customerName: String = "",
    val searchQuery: String = "",
    val catalog: List<Product> = emptyList(),
    val cart: List<CartLine> = emptyList(),
    val variantPicker: VariantPickerState.Request? = null,
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
    val barcodeSearchEnabled: Boolean = true,
) {
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

    init {
        viewModelScope.launch {
            preferences.shopFeatures.collect { features ->
                if (!features.voiceSearch && _uiState.value.isListening) cancelListening()
                _uiState.update {
                    it.copy(
                        voiceSearchEnabled = features.voiceSearch,
                        barcodeSearchEnabled = features.barcodeSearch,
                    )
                }
            }
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
        if (product.needsPicker) openPicker(product) else addToCart(product, 1.0)
    }

    fun openPicker(product: Product, editKey: String? = null) {
        _uiState.update { it.copy(variantPicker = VariantPickerState.Request(product, editKey)) }
    }

    fun dismissPicker() {
        _uiState.update { it.copy(variantPicker = null) }
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

    fun removeLine(key: String) {
        _uiState.update { state -> state.copy(cart = state.cart.filterNot { it.key == key }) }
    }

    /** Adds or replaces a line from the variant picker; [targetKey] is the line being edited. */
    fun upsertOption(targetKey: String, option: PurchaseOption, quantity: Double) {
        _uiState.update { state ->
            val qty = option.normalize(quantity)
            val replaced = CartLine(option.product, qty, option.variant)
            val others = state.cart.filterNot { it.key == targetKey || it.key == option.key }
            val index = state.cart.indexOfFirst { it.key == targetKey }.takeIf { it >= 0 } ?: others.size
            state.copy(
                cart = others.toMutableList().apply { add(index.coerceAtMost(size), replaced) },
                variantPicker = null,
                error = null,
            )
        }
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
        val messages = mutableListOf<UiText>()
        var addedAny = false
        for (line in lines) {
            val match = ProductFuzzyMatcher.findBest(line.productQuery, catalog)
            if (match == null) {
                // No unit + no catalog match → background speech, not an order line.
                if (line.unit != null) {
                    messages += UiText.res(Res.string.voice_out_of_stock, line.productQuery)
                }
                continue
            }
            val product = match.product
            val qty = ProductFuzzyMatcher.convertQuantity(line.quantity, line.unit, product.unit)
            val alreadyInCart = _uiState.value.cart
                .firstOrNull { it.product.id == product.id }
                ?.quantity
                ?: 0.0
            val available = product.stockQty - alreadyInCart
            if (available <= 0.0) {
                val label = product.nameHi.ifBlank { product.name }
                messages += UiText.res(Res.string.voice_out_of_stock, label)
                continue
            }
            val addQty = qty.coerceAtMost(available)
            addToCart(product, addQty)
            addedAny = true
            if (addQty < qty) {
                val label = product.nameHi.ifBlank { product.name }
                messages += UiText.res(Res.string.voice_partial_stock, label, available.toInt())
            }
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

    private fun addToCart(product: Product, quantity: Double) {
        if (quantity <= 0.0) return
        _uiState.update { state ->
            val key = CartStore.lineKey(product.id, null)
            val existing = state.cart.firstOrNull { it.key == key }
            val cart = if (existing != null) {
                state.cart.map {
                    if (it.key == key) it.copy(quantity = it.quantity + quantity) else it
                }
            } else {
                state.cart + CartLine(product, quantity)
            }
            state.copy(cart = cart, error = null)
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
    }
}

private fun Double.asMoneyInput(): String {
    val paise = kotlin.math.round(this * 100).toLong()
    return if (paise % 100 == 0L) (paise / 100).toString() else "${paise / 100}.${(paise % 100).toString().padStart(2, '0')}"
}
