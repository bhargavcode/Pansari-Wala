package org.bhargav.pansariwala.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.domain.auth.ShopAccess
import org.bhargav.pansariwala.domain.auth.ShopPermission
import org.bhargav.pansariwala.domain.model.OrderSummary
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.i18n.labelRes
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.bhargav.pansariwala.voice.ProductFuzzyMatcher
import org.bhargav.pansariwala.voice.SpeechEvent
import org.bhargav.pansariwala.voice.SpeechToText
import org.bhargav.pansariwala.voice.TextToSpeech
import org.bhargav.pansariwala.voice.Transliterator
import org.jetbrains.compose.resources.getString
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_mic_permission
import pansariwala.shared.generated.resources.error_speech_unavailable
import pansariwala.shared.generated.resources.gsearch_answer_found
import pansariwala.shared.generated.resources.gsearch_answer_none
import pansariwala.shared.generated.resources.gsearch_answer_open
import pansariwala.shared.generated.resources.gsearch_answer_price
import pansariwala.shared.generated.resources.gsearch_answer_stock

/** Screens reachable from the search bar (`/` prefix or a spoken "open …"). */
enum class SearchCommand(val permission: ShopPermission?, val keywords: List<String>) {
    NEW_SALE(ShopPermission.BILLING, listOf("new", "sale", "bill", "billing", "naya", "bikri", "becho")),
    ORDERS(ShopPermission.BILLING, listOf("orders", "order", "bills", "history")),
    ONLINE_ORDERS(ShopPermission.ONLINE_ORDERS, listOf("online", "delivery", "pickup")),
    INVENTORY(ShopPermission.INVENTORY, listOf("inventory", "stock", "maal", "saman", "products")),
    LOW_STOCK(ShopPermission.INVENTORY, listOf("low", "kam", "khatam", "short")),
    SUPPLIERS(ShopPermission.INVENTORY, listOf("supplier", "suppliers", "dealer", "vendor")),
    REPORTS(ShopPermission.VIEW_REPORTS, listOf("report", "reports", "profit", "munafa", "hisab", "kamai")),
    UDHAAR(null, listOf("udhaar", "udhar", "credit", "khata", "borrowers")),
    PRINTER(null, listOf("printer", "print")),
    SETTINGS(null, listOf("settings", "setting", "language", "theme")),
    PROFILE(null, listOf("profile", "shop", "staff", "dukan")),
    ;

    /** [typed] = `/` filtering, where any keyword prefix counts; speech needs a whole or 3+ letter prefix match. */
    fun matches(word: String, typed: Boolean = false): Boolean = keywords.any {
        it == word || if (typed) it.startsWith(word) else word.length >= MIN_SPOKEN_PREFIX && (it.startsWith(word) || word.startsWith(it))
    }

    private companion object {
        const val MIN_SPOKEN_PREFIX = 4
    }
}

sealed interface SearchHit {
    data class Command(val command: SearchCommand) : SearchHit
    data class ProductHit(val product: Product) : SearchHit
    data class OrderHit(val order: OrderSummary) : SearchHit
}

enum class SearchMode { PRODUCTS, COMMANDS, ORDERS }

data class GlobalSearchUiState(
    val query: String = "",
    val mode: SearchMode = SearchMode.PRODUCTS,
    val hits: List<SearchHit> = emptyList(),
    val listening: Boolean = false,
    val requestMicPermission: Boolean = false,
    /** Last spoken answer, shown in a card and read aloud. */
    val answer: String? = null,
    val error: UiText? = null,
    val languageCode: String = "en",
    val voiceAvailable: Boolean = false,
)

class GlobalSearchViewModel(
    private val prefs: AppPreferences,
    private val repository: ShopRepository,
    private val speech: SpeechToText,
    private val tts: TextToSpeech,
) : ViewModel() {
    private val _state = MutableStateFlow(GlobalSearchUiState(voiceAvailable = speech.isAvailable()))
    val state: StateFlow<GlobalSearchUiState> = _state.asStateFlow()

    private val _commands = MutableSharedFlow<SearchHit>(extraBufferCapacity = 1)
    /** Navigation requested by a spoken command; the screen routes it like a tapped hit. */
    val commands: SharedFlow<SearchHit> = _commands.asSharedFlow()

    private var products: List<Product> = emptyList()
    private var orders: List<OrderSummary> = emptyList()
    private var access: ShopAccess = ShopAccess.FULL
    private var speechJob: Job? = null

    init {
        viewModelScope.launch {
            val shopId = prefs.getShopId() ?: SeedData.DEMO_SHOP_ID
            access = prefs.getShopAccess()
            _state.update { it.copy(languageCode = prefs.getUserSettings().language.code) }
            launch { repository.observeProducts(shopId).collect { list -> products = list.filter { it.isActive && !it.isDeleted } } }
            launch { repository.observeRecentOrders(shopId, RECENT_ORDER_LIMIT).collect { orders = it } }
        }
    }

    fun onQueryChange(value: String) {
        _state.update { it.copy(query = value, answer = null, error = null) }
        refreshHits()
    }

    fun clear() {
        tts.stop()
        _state.update { it.copy(query = "", hits = emptyList(), answer = null, error = null) }
    }

    private fun refreshHits() {
        val raw = _state.value.query
        val (mode, text) = when {
            raw.startsWith(PREFIX_COMMAND) -> SearchMode.COMMANDS to raw.drop(1).trim()
            raw.startsWith(PREFIX_ORDER) -> SearchMode.ORDERS to raw.drop(1).trim()
            else -> SearchMode.PRODUCTS to raw.trim()
        }
        val hits = when (mode) {
            SearchMode.COMMANDS -> commandHits(text, limitTo = Int.MAX_VALUE)
            SearchMode.ORDERS -> orderHits(text)
            SearchMode.PRODUCTS -> if (text.isEmpty()) emptyList() else productHits(text) + commandHits(text, limitTo = 2)
        }
        _state.update { it.copy(mode = mode, hits = hits) }
    }

    private fun allowedCommands() = SearchCommand.entries.filter { cmd -> cmd.permission?.let(access::can) ?: true }

    private fun commandHits(text: String, limitTo: Int): List<SearchHit> {
        val words = words(text)
        val typed = _state.value.query.startsWith(PREFIX_COMMAND)
        val list = if (words.isEmpty()) allowedCommands() else allowedCommands().filter { cmd -> words.any { cmd.matches(it, typed) } }
        return list.take(limitTo).map { SearchHit.Command(it) }
    }

    private fun orderHits(text: String): List<SearchHit> {
        val q = text.lowercase()
        return orders.asSequence()
            .filter { o ->
                q.isEmpty() || o.id.lowercase().contains(q) ||
                    o.customerName.orEmpty().lowercase().contains(q) ||
                    o.createdByName.orEmpty().lowercase().contains(q)
            }
            .take(MAX_HITS)
            .map { SearchHit.OrderHit(it) }
            .toList()
    }

    private fun productHits(text: String): List<SearchHit> {
        val byCode = products.filter { p -> p.barcode == text || p.variants.any { it.barcode == text } }
            .takeIf { text.length >= MIN_BARCODE_LENGTH }.orEmpty()
        val list = byCode.ifEmpty { ProductFuzzyMatcher.search(text, products, MAX_HITS)?.products.orEmpty() }
        return list.take(MAX_HITS).map { SearchHit.ProductHit(it) }
    }

    // ---- Voice ----

    fun onMicClick() {
        if (_state.value.listening) {
            speech.cancel()
            speechJob?.cancel()
            _state.update { it.copy(listening = false) }
            return
        }
        if (!speech.isAvailable()) {
            _state.update { it.copy(error = UiText.res(Res.string.error_speech_unavailable)) }
            return
        }
        tts.stop()
        _state.update { it.copy(requestMicPermission = true, error = null, answer = null) }
    }

    fun consumeMicPermissionRequest() = _state.update { it.copy(requestMicPermission = false) }

    fun onMicPermissionResult(granted: Boolean) {
        _state.update { it.copy(requestMicPermission = false) }
        if (!granted) {
            _state.update { it.copy(error = UiText.res(Res.string.error_mic_permission)) }
            return
        }
        speechJob?.cancel()
        speechJob = viewModelScope.launch {
            speech.events.collect { event ->
                when (event) {
                    SpeechEvent.Started -> _state.update { it.copy(listening = true) }
                    is SpeechEvent.PartialResult -> onQueryChange(event.text)
                    is SpeechEvent.FinalResult -> {
                        speech.stopListening()
                        _state.update { it.copy(listening = false) }
                        handleUtterance(event.text)
                        speechJob?.cancel()
                    }
                    is SpeechEvent.Error -> {
                        _state.update {
                            it.copy(
                                listening = false,
                                error = UiText.res(if (event.needsPermission) Res.string.error_mic_permission else event.message),
                            )
                        }
                        speechJob?.cancel()
                    }
                    SpeechEvent.Ended -> _state.update { it.copy(listening = false) }
                }
            }
        }
        speech.startListening()
    }

    /** Rule-based intent: open a screen, ask stock / price of a product, or plain product search. */
    private suspend fun handleUtterance(raw: String) {
        val text = raw.trim()
        if (text.isEmpty()) return
        onQueryChange(text)
        val words = words(text)
        val lang = _state.value.languageCode
        val wantsOpen = words.any { it in OPEN_WORDS }
        val command = allowedCommands().firstOrNull { cmd -> words.any { cmd.matches(it) } }
        val productQuery = words.filterNot { it in OPEN_WORDS || it in STOCK_WORDS || it in PRICE_WORDS || it in FILLER_WORDS }
            .joinToString(" ")
        val product = productQuery.takeIf { it.isNotBlank() }?.let { ProductFuzzyMatcher.findBest(it, products)?.product }
        val answer = when {
            command != null && (wantsOpen || product == null) -> {
                _commands.tryEmit(SearchHit.Command(command))
                getString(Res.string.gsearch_answer_open, getString(command.labelRes))
            }
            product != null && words.any { it in STOCK_WORDS } ->
                getString(
                    Res.string.gsearch_answer_stock,
                    product.displayName(lang),
                    product.totalStock.asQuantity(),
                    getString(product.unit.labelRes),
                )
            product != null && words.any { it in PRICE_WORDS } ->
                getString(
                    Res.string.gsearch_answer_price,
                    product.displayName(lang),
                    product.salePrice().asMoney(),
                    getString(product.unit.labelRes),
                )
            _state.value.hits.isNotEmpty() -> getString(Res.string.gsearch_answer_found, _state.value.hits.size)
            else -> getString(Res.string.gsearch_answer_none)
        }
        _state.update { it.copy(answer = answer) }
        tts.speak(answer, "$lang-$SPEECH_REGION")
    }

    fun replayAnswer() {
        val answer = _state.value.answer ?: return
        tts.speak(answer, "${_state.value.languageCode}-$SPEECH_REGION")
    }

    override fun onCleared() {
        speechJob?.cancel()
        tts.stop()
        if (_state.value.listening) speech.cancel()
    }

    private fun words(text: String): List<String> =
        Transliterator.toLatin(text.lowercase())
            .split(' ', ',', '?', '.', '!')
            .filter { it.isNotBlank() }

    companion object {
        const val PREFIX_COMMAND = "/"
        const val PREFIX_ORDER = "@"
        private const val MAX_HITS = 8
        private const val MIN_BARCODE_LENGTH = 6
        private const val RECENT_ORDER_LIMIT = 200
        private const val SPEECH_REGION = "IN"
        private val OPEN_WORDS = setOf("open", "show", "go", "kholo", "khol", "dikhao", "dikha", "jao", "chalo")
        private val STOCK_WORDS = setOf("stock", "kitna", "kitni", "kitne", "bacha", "bachi", "left", "quantity", "available")
        private val PRICE_WORDS = setOf("price", "rate", "daam", "dam", "kimat", "keemat", "bhav", "cost", "mrp")
        private val FILLER_WORDS = setOf("hai", "he", "ka", "ki", "ke", "kya", "what", "is", "the", "of", "me", "mein", "batao", "bolo", "tell", "please")
    }
}
