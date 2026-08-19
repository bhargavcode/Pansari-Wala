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
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderItem
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.util.AppClock
import org.bhargav.pansariwala.util.generateId
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.i18n.WALK_IN_CUSTOMER_KEY
import org.bhargav.pansariwala.notification.ShopNotifier
import org.bhargav.pansariwala.voice.ProductFuzzyMatcher
import org.bhargav.pansariwala.voice.SpeechEvent
import org.bhargav.pansariwala.voice.SpeechToText
import org.bhargav.pansariwala.voice.VoiceIntentParser
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_mic_permission
import pansariwala.shared.generated.resources.error_order_empty
import pansariwala.shared.generated.resources.error_speech_unavailable
import pansariwala.shared.generated.resources.voice_not_understood
import pansariwala.shared.generated.resources.voice_out_of_stock
import pansariwala.shared.generated.resources.voice_partial_stock

data class CartLine(
    val product: Product,
    val quantity: Double,
) {
    val lineTotal: Double get() = quantity * product.sellingPrice
}

data class OrderEditorUiState(
    val loading: Boolean = true,
    val isEditing: Boolean = false,
    val customerName: String = "",
    val searchQuery: String = "",
    val catalog: List<Product> = emptyList(),
    val cart: List<CartLine> = emptyList(),
    val saved: Boolean = false,
    val error: UiText? = null,
    val voiceMessages: List<UiText> = emptyList(),
    val isListening: Boolean = false,
    val partialTranscript: String = "",
    val requestMicPermission: Boolean = false,
) {
    val total: Double get() = cart.sumOf { it.lineTotal }
    val itemCount: Int get() = cart.size
    val filteredCatalog: List<Product>
        get() {
            if (searchQuery.isBlank()) return catalog
            val q = searchQuery.trim().lowercase()
            return catalog.filter {
                it.name.lowercase().contains(q) ||
                    it.nameHi.contains(searchQuery.trim()) ||
                    (it.voiceAlias?.lowercase()?.contains(q) == true) ||
                    (it.barcode?.contains(q) == true)
            }
        }
}

class OrderEditorViewModel(
    private val shopRepository: ShopRepository,
    private val preferences: AppPreferences,
    private val analytics: Analytics,
    private val speechToText: SpeechToText,
    private val shopNotifier: ShopNotifier,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderEditorUiState())
    val uiState: StateFlow<OrderEditorUiState> = _uiState.asStateFlow()

    private var shopId: String = SeedData.DEMO_SHOP_ID
    private var editingOrderId: String? = null
    private var createdAt: Long = AppClock.nowMillis()
    private var speechJob: Job? = null
    private var partialCommitJob: Job? = null
    private var lastAppliedUtterance: String? = null

    init {
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
            val catalog = shopRepository.observeProducts(shopId).first()
            val byId = catalog.associateBy { it.id }

            val existing = orderId?.let { shopRepository.getOrder(it) }
            if (existing != null) {
                editingOrderId = existing.id
                createdAt = existing.createdAtEpochMs
                val cart = existing.items.mapNotNull { item ->
                    byId[item.productId]?.let { CartLine(it, item.quantity) }
                }
                _uiState.update {
                    it.copy(
                        loading = false,
                        isEditing = true,
                        customerName = existing.customerName.orEmpty(),
                        catalog = catalog,
                        cart = cart,
                    )
                }
            } else {
                _uiState.update { it.copy(loading = false, catalog = catalog) }
            }
        }
    }

    fun onSearchChange(value: String) = _uiState.update { it.copy(searchQuery = value) }
    fun onCustomerNameChange(value: String) = _uiState.update { it.copy(customerName = value) }

    fun onMicClick() {
        analytics.log(AnalyticsEvent.ButtonClicked("voice_mic", "order_editor"))
        if (_uiState.value.isListening) {
            cancelListening()
            return
        }
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
        addToCart(product, 1.0)
    }

    fun changeQuantity(productId: String, delta: Double) {
        _uiState.update { state ->
            val cart = state.cart.mapNotNull { line ->
                if (line.product.id != productId) {
                    line
                } else {
                    val next = line.quantity + delta
                    if (next <= 0.0) null else line.copy(quantity = next)
                }
            }
            state.copy(cart = cart)
        }
    }

    fun removeLine(productId: String) {
        _uiState.update { state -> state.copy(cart = state.cart.filterNot { it.product.id == productId }) }
    }

    fun save() {
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
            status = if (state.isEditing) {
                // Keep existing status when editing; default received for brand-new.
                OrderStatus.RECEIVED
            } else {
                OrderStatus.RECEIVED
            },
            customerName = state.customerName.trim().ifBlank { WALK_IN_CUSTOMER_KEY },
            items = state.cart.map { line ->
                OrderItem(
                    productId = line.product.id,
                    productName = line.product.name,
                    unit = line.product.unit,
                    quantity = line.quantity,
                    unitPrice = line.product.sellingPrice,
                )
            },
        )
        viewModelScope.launch {
            if (_uiState.value.isListening) speechToText.cancel()
            val existing = editingOrderId?.let { shopRepository.getOrder(it) }
            val toSave = if (existing != null) {
                order.copy(status = existing.status, cancelReason = existing.cancelReason)
            } else {
                order
            }
            shopRepository.saveOrder(toSave)
            val catalogAfter = shopRepository.observeProducts(shopId).first()
            if (existing == null) {
                shopNotifier.onOrderSaved(toSave, catalogAfter)
            }
            _uiState.update { it.copy(saved = true, error = null, isListening = false) }
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
            val msg = UiText.res(Res.string.voice_not_understood, normalized)
            _uiState.update {
                it.copy(
                    voiceMessages = it.voiceMessages + msg,
                    error = msg,
                )
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
                messages += UiText.res(Res.string.voice_out_of_stock, line.productQuery)
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
            val existing = state.cart.firstOrNull { it.product.id == product.id }
            val cart = if (existing != null) {
                state.cart.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + quantity) else it
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
