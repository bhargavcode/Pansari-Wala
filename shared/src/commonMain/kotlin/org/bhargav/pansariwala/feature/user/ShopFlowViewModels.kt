package org.bhargav.pansariwala.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.api.PlaceOrderItemDto
import org.bhargav.pansariwala.api.PlaceOrderRequest
import org.bhargav.pansariwala.api.QuoteDto
import org.bhargav.pansariwala.api.VerifyPaymentRequest
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.domain.model.CatalogSection
import org.bhargav.pansariwala.domain.model.FulfillmentStep
import org.bhargav.pansariwala.domain.model.MarketplaceShop
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.domain.model.SavedAddress
import org.bhargav.pansariwala.domain.model.ShopOffer
import org.bhargav.pansariwala.domain.model.toFulfillmentStep
import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.platform.RazorpayCheckout
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.ui.AsyncUiState
import org.bhargav.pansariwala.ui.beginLoad
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.notification.NotificationGateway
import org.bhargav.pansariwala.notification.OrderUpdates
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_checkout_address_required
import pansariwala.shared.generated.resources.error_checkout_empty_cart
import pansariwala.shared.generated.resources.error_checkout_out_of_range
import pansariwala.shared.generated.resources.error_checkout_profile_incomplete
import pansariwala.shared.generated.resources.error_checkout_quote_missing
import pansariwala.shared.generated.resources.error_generic
import pansariwala.shared.generated.resources.error_razorpay_cancelled
import pansariwala.shared.generated.resources.error_razorpay_failed
import pansariwala.shared.generated.resources.error_razorpay_unavailable

enum class CatalogTab { PRODUCTS, RATINGS }

data class CatalogData(
    val shop: MarketplaceShop? = null,
    val sections: List<CatalogSection> = emptyList(),
    val searchSections: List<CatalogSection> = emptyList(),
    val searching: Boolean = false,
    val ratings: List<org.bhargav.pansariwala.domain.model.ShopReview> = emptyList(),
    val ratingsLoaded: Boolean = false,
    val cartCount: Int = 0,
    val productQuery: String = "",
    val selectedTab: CatalogTab = CatalogTab.PRODUCTS,
    val ratingFilterStars: Set<Int> = emptySet(),
    val showRatingFilterSheet: Boolean = false,
) {
    val isSearchActive: Boolean get() = productQuery.isNotBlank()
    val visibleSections: List<CatalogSection> get() = if (isSearchActive) searchSections else sections
}

typealias CatalogUiState = AsyncUiState<CatalogData>

class ShopCatalogViewModel(
    private val api: PansariApi,
    private val cart: CartStore,
    private val location: org.bhargav.pansariwala.platform.DeviceLocation,
) : ViewModel() {
    private val _state = MutableStateFlow<CatalogUiState>(AsyncUiState.Idle)
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    fun dismissError() {
        _state.value = when (val current = _state.value) {
            is AsyncUiState.Error -> AsyncUiState.Idle
            is AsyncUiState.Success -> current.copy(bannerError = null)
            else -> current
        }
    }

    private var shopId: String? = null
    private var cartJob: Job? = null
    private var searchJob: Job? = null
    private var ratingsJob: Job? = null

    fun refresh() {
        shopId?.let { load(it, force = true) }
    }

    fun load(shopId: String, force: Boolean = false) {
        if (!force && this.shopId == shopId && _state.value is AsyncUiState.Success) return
        this.shopId = shopId
        cart.bindShop(shopId)
        cartJob?.cancel()
        cartJob = viewModelScope.launch {
            cart.lines.collect { lines ->
                updateSuccess { it.copy(cartCount = CartStore.countItems(lines)) }
            }
        }
        viewModelScope.launch {
            _state.value = _state.value.beginLoad()
            runCatching {
                coroutineScope {
                    val sectionsDef = async {
                        api.catalogSections(shopId, "", AppConstants.CATALOG_PREVIEW_PER_CATEGORY)
                    }
                    val shopDef = async {
                        runCatching {
                            val geo = location.currentOrDefault()
                            api.shopInfo(shopId, geo.lat, geo.lng)
                        }.getOrNull()
                    }
                    val shop = shopDef.await()
                    val sections = sectionsDef.await().sortedBy { it.category.ordinal }
                    shop?.let { cart.bindShop(shopId, it.name) }
                    val previous = (_state.value as? AsyncUiState.Success)?.data ?: CatalogData()
                    previous.copy(shop = shop ?: previous.shop, sections = sections, cartCount = cart.itemCount)
                }
            }.onSuccess { data ->
                _state.value = AsyncUiState.Success(data)
                if (data.selectedTab == CatalogTab.RATINGS) loadRatings(force = true)
            }.onFailure { err ->
                err.rethrowIfStructuredCancellation()
                val message = err.toApiUiText()
                _state.value = when (val current = _state.value) {
                    is AsyncUiState.Success -> current.copy(isRefreshing = false, bannerError = message)
                    else -> AsyncUiState.Error(message)
                }
            }
        }
    }

    fun setProductQuery(value: String) {
        updateSuccess { it.copy(productQuery = value, searching = value.isNotBlank()) }
        searchJob?.cancel()
        val id = shopId ?: return
        val query = value.trim()
        if (query.isEmpty()) {
            updateSuccess { it.copy(searchSections = emptyList(), searching = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(AppConstants.CATALOG_SEARCH_DEBOUNCE_MS)
            runCatching { api.catalogSections(id, query, AppConstants.CATALOG_PREVIEW_PER_CATEGORY) }
                .onSuccess { results ->
                    updateSuccess {
                        it.copy(searchSections = results.sortedBy { s -> s.category.ordinal }, searching = false)
                    }
                }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    val current = _state.value
                    if (current is AsyncUiState.Success) {
                        _state.value = current.copy(
                            data = current.data.copy(searching = false),
                            bannerError = err.toApiUiText(),
                        )
                    }
                }
        }
    }

    fun selectTab(tab: CatalogTab) {
        updateSuccess { it.copy(selectedTab = tab) }
        if (tab == CatalogTab.RATINGS) loadRatings()
    }

    private fun loadRatings(force: Boolean = false) {
        val id = shopId ?: return
        val data = (_state.value as? AsyncUiState.Success)?.data ?: return
        if ((data.ratingsLoaded && !force) || ratingsJob?.isActive == true) return
        ratingsJob = viewModelScope.launch {
            runCatching { api.shopRatings(id) }
                .onSuccess { ratings -> updateSuccess { it.copy(ratings = ratings, ratingsLoaded = true) } }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    val current = _state.value
                    if (current is AsyncUiState.Success) _state.value = current.copy(bannerError = err.toApiUiText())
                }
        }
    }

    fun showRatingFilterSheet() {
        updateSuccess { it.copy(showRatingFilterSheet = true) }
    }

    fun hideRatingFilterSheet() {
        updateSuccess { it.copy(showRatingFilterSheet = false) }
    }

    fun toggleRatingFilter(stars: Int) {
        updateSuccess { data ->
            val next = data.ratingFilterStars.toMutableSet().apply {
                if (contains(stars)) remove(stars) else add(stars)
            }
            data.copy(ratingFilterStars = next)
        }
    }

    fun setRatingFilter(stars: Set<Int>) {
        updateSuccess { it.copy(ratingFilterStars = stars) }
    }

    fun clearRatingFilter() {
        updateSuccess { it.copy(ratingFilterStars = emptySet()) }
    }

    fun filteredRatings(data: CatalogData): List<org.bhargav.pansariwala.domain.model.ShopReview> {
        if (data.ratingFilterStars.isEmpty()) return data.ratings
        return data.ratings.filter { it.stars in data.ratingFilterStars }
    }

    private inline fun updateSuccess(block: (CatalogData) -> CatalogData) {
        val current = _state.value
        if (current is AsyncUiState.Success) {
            _state.value = current.copy(data = block(current.data))
        }
    }
}

data class CartUiState(
    val lines: List<CartStore.Line> = emptyList(),
    val subtotal: Double = 0.0,
)

class CartViewModel(
    private val cart: CartStore,
) : ViewModel() {
    private val _state = MutableStateFlow(CartUiState())
    val state: StateFlow<CartUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            cart.lines.collect { lines ->
                _state.update { CartUiState(lines = lines, subtotal = cart.subtotal) }
            }
        }
    }

    fun increment(lineKey: String) { cart.increment(lineKey) }
    fun decrement(lineKey: String) { cart.decrement(lineKey) }
}

data class CheckoutUiState(
    val quote: QuoteDto? = null,
    val offers: List<ShopOffer> = emptyList(),
    val offersExpanded: Boolean = false,
    val placing: Boolean = false,
    val needsProfile: Boolean = false,
    val addresses: List<SavedAddress> = emptyList(),
    val selectedAddressId: String? = null,
    val shop: MarketplaceShop? = null,
    val snackbar: UiText? = null,
    val error: UiText? = null,
)

class CheckoutViewModel(
    private val api: PansariApi,
    private val cart: CartStore,
    private val location: DeviceLocation,
    private val razorpay: RazorpayCheckout,
    private val preferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(CheckoutUiState())
    val state: StateFlow<CheckoutUiState> = _state.asStateFlow()

    fun load(shopId: String) {
        viewModelScope.launch {
            val geo = location.currentOrDefault()
            val profile = preferences.getCachedCustomerProfile()
                ?: runCatching { api.me() }.getOrNull()?.also { preferences.setCachedCustomerProfile(it) }
            val addresses = profile?.addresses.orEmpty()
            val selected = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
            val dropLat = selected?.location?.lat ?: profile?.location?.lat ?: geo.lat
            val dropLng = selected?.location?.lng ?: profile?.location?.lng ?: geo.lng
            val items = cart.lines.value.map { PlaceOrderItemDto(it.product.id, it.quantity, it.variant?.id) }
            val quote = runCatching {
                api.quote(
                    org.bhargav.pansariwala.api.QuoteRequest(shopId, items, dropLat, dropLng),
                )
            }.getOrNull()
            val offers = runCatching { api.shopOffers(shopId) }.getOrDefault(emptyList())
            val shop = runCatching { api.shopInfo(shopId, dropLat, dropLng) }.getOrNull()
            _state.update {
                it.copy(
                    quote = quote,
                    offers = offers,
                    addresses = addresses,
                    selectedAddressId = selected?.id,
                    shop = shop,
                    needsProfile = profile == null || profile.name.isBlank() || addresses.isEmpty(),
                )
            }
        }
    }

    fun toggleOffers() { _state.update { it.copy(offersExpanded = !it.offersExpanded) } }

    fun consumeSnackbar() { _state.update { it.copy(snackbar = null) } }
    fun dismissError() { _state.update { it.copy(error = null, snackbar = null) } }

    fun selectAddress(shopId: String, addressId: String) {
        viewModelScope.launch {
            runCatching { api.selectAddress(addressId) }
                .onSuccess { preferences.setCachedCustomerProfile(it) }
            _state.update { it.copy(selectedAddressId = addressId) }
            load(shopId)
        }
    }

    fun place(shopId: String, onPlaced: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(placing = true, error = null, needsProfile = false, snackbar = null) }
            runCatching {
                val items = cart.lines.value.map { PlaceOrderItemDto(it.product.id, it.quantity, it.variant?.id) }
                require(items.isNotEmpty()) { AppConstants.Checkout.ERROR_EMPTY_CART }
                val quote = _state.value.quote ?: error(AppConstants.Checkout.ERROR_MISSING_QUOTE)
                val selectedId = _state.value.selectedAddressId
                require(!selectedId.isNullOrBlank()) { AppConstants.Checkout.ERROR_ADDRESS_REQUIRED }

                val userRadius = preferences.getSearchRadiusKm()
                val shopRadius = _state.value.shop?.deliveryRadiusKm
                    ?: AppConstants.DEFAULT_SHOP_DELIVERY_RADIUS_KM
                if (quote.deliveryDistanceKm > userRadius || quote.deliveryDistanceKm > shopRadius) {
                    error(AppConstants.Checkout.ERROR_OUT_OF_RANGE)
                }

                val request = PlaceOrderRequest(
                    shopId = shopId,
                    items = items,
                    addressId = selectedId,
                    userLat = _state.value.addresses.firstOrNull { it.id == selectedId }?.location?.lat,
                    userLng = _state.value.addresses.firstOrNull { it.id == selectedId }?.location?.lng,
                )
                api.validateOrder(request)
                val profile = preferences.getCachedCustomerProfile()
                    ?: api.me().also { preferences.setCachedCustomerProfile(it) }

                val amountPaise = (quote.payable * 100).toLong()
                val rzp = api.createRazorpayOrder(shopId, amountPaise)
                val paid = razorpay.pay(
                    keyId = rzp.keyId,
                    orderId = rzp.orderId,
                    amountPaise = rzp.amountPaise,
                    customerName = profile.name,
                    customerPhone = profile.phone,
                    description = "Pansari order",
                ).getOrThrow()
                val verified = api.verifyPayment(
                    VerifyPaymentRequest(paid.orderId, paid.paymentId, paid.signature),
                )
                require(verified) { "Payment could not be verified" }
                val order = api.placeOrder(
                    request.copy(
                        razorpayPaymentId = paid.paymentId,
                        razorpayOrderId = paid.orderId,
                        razorpaySignature = paid.signature,
                    ),
                )
                cart.clear()
                order.id
            }.onSuccess(onPlaced)
                .onFailure { err ->
                    val msg = checkoutErrorMessage(err)
                    val outOfRange = msg.contains(AppConstants.Checkout.ERROR_OUT_OF_RANGE)
                    _state.update { s ->
                        s.copy(
                            placing = false,
                            needsProfile = msg.contains(AppConstants.Checkout.ERROR_PROFILE) ||
                                msg.contains(AppConstants.Checkout.ERROR_ADDRESS_REQUIRED),
                            snackbar = if (outOfRange) UiText.res(Res.string.error_checkout_out_of_range) else null,
                            error = if (outOfRange) null else checkoutError(err, msg),
                        )
                    }
                }
        }
    }
}

private fun checkoutErrorMessage(err: Throwable): String {
    val raw = err.message.orEmpty()
    return Regex("\"error\"\\s*:\\s*\"([^\"]+)\"").find(raw)?.groupValues?.getOrNull(1) ?: raw
}

private fun checkoutError(err: Throwable, message: String = checkoutErrorMessage(err)): UiText = when {
    message == AppConstants.Razorpay.ERROR_CANCELLED ||
        err.message == AppConstants.Razorpay.ERROR_CANCELLED ->
        UiText.res(Res.string.error_razorpay_cancelled)
    message == AppConstants.Razorpay.ERROR_UNAVAILABLE ||
        err.message == AppConstants.Razorpay.ERROR_UNAVAILABLE ->
        UiText.res(Res.string.error_razorpay_unavailable)
    message == AppConstants.Razorpay.ERROR_FAILED ||
        err.message == AppConstants.Razorpay.ERROR_FAILED ->
        UiText.res(Res.string.error_razorpay_failed)
    message.contains(AppConstants.Checkout.ERROR_OUT_OF_RANGE) ->
        UiText.res(Res.string.error_checkout_out_of_range)
    message.contains(AppConstants.Checkout.ERROR_ADDRESS_REQUIRED) ->
        UiText.res(Res.string.error_checkout_address_required)
    message.contains(AppConstants.Checkout.ERROR_PROFILE) ->
        UiText.res(Res.string.error_checkout_profile_incomplete)
    message.contains(AppConstants.Checkout.ERROR_EMPTY_CART) ->
        UiText.res(Res.string.error_checkout_empty_cart)
    message.contains(AppConstants.Checkout.ERROR_MISSING_QUOTE) ->
        UiText.res(Res.string.error_checkout_quote_missing)
    else -> err.toApiUiText()
}

class ThankYouViewModel : ViewModel() {
    fun goNext(onDone: () -> Unit) {
        viewModelScope.launch {
            delay(AppConstants.THANK_YOU_DELAY_MS)
            onDone()
        }
    }
}

data class OrderDetailsUiState(
    val order: Order? = null,
    val step: FulfillmentStep = FulfillmentStep.PLACED,
    val stars: Int = 0,
    val comment: String = "",
    val editingRating: Boolean = false,
    val error: UiText? = null,
)

/**
 * Live via FCM ([OrderUpdates]) — no polling. Status is applied from the payload instantly, then the
 * order is re-read once for partner/OTP details. Tray alerts for this order clear while it's on screen.
 */
class OrderDetailsViewModel(
    private val api: PansariApi,
    private val notifications: NotificationGateway,
) : ViewModel() {
    private val _state = MutableStateFlow(OrderDetailsUiState())
    val state: StateFlow<OrderDetailsUiState> = _state.asStateFlow()
    private var loadJob: Job? = null
    private var orderId: String? = null
    private var visible = false

    fun dismissError() { _state.update { it.copy(error = null) } }

    init {
        viewModelScope.launch {
            OrderUpdates.events.collect { update ->
                if (update.orderId != orderId) return@collect
                update.status?.let { status ->
                    _state.update { s ->
                        s.copy(order = s.order?.copy(status = status), step = status.toFulfillmentStep())
                    }
                }
                if (visible) notifications.clearOrder(update.orderId)
                fetch(update.orderId)
            }
        }
    }

    /** Resume after the first load re-reads once (app back to foreground / missed pushes). */
    fun onScreenVisible(isVisible: Boolean) {
        visible = isVisible
        val id = orderId ?: return
        if (!isVisible) return
        notifications.clearOrder(id)
        if (_state.value.order != null) fetch(id)
    }

    fun load(orderId: String) {
        this.orderId = orderId
        if (visible) notifications.clearOrder(orderId)
        fetch(orderId)
    }

    private fun fetch(orderId: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            runCatching { api.order(orderId) }
                    .onSuccess { order ->
                        _state.update {
                            it.copy(
                                order = order,
                                step = order.status.toFulfillmentStep(),
                                stars = if (it.editingRating && it.order != null) it.stars else order.rating?.stars ?: 0,
                                comment = if (it.editingRating && it.order != null) it.comment else order.rating?.comment.orEmpty(),
                                editingRating = if (it.order == null) order.rating == null else it.editingRating,
                                error = null,
                            )
                        }
                    }
                    .onFailure { err ->
                        err.rethrowIfStructuredCancellation()
                        if (_state.value.order == null) {
                            _state.update { it.copy(error = err.toApiUiText()) }
                        }
                    }
        }
    }

    fun setStars(value: Int) { _state.update { it.copy(stars = value) } }
    fun setComment(value: String) { _state.update { it.copy(comment = value) } }
    fun startEdit() { _state.update { it.copy(editingRating = true) } }

    fun saveRating() {
        val order = _state.value.order ?: return
        if (_state.value.stars !in 1..5) return
        viewModelScope.launch {
            val updated = api.rateOrder(order.id, _state.value.stars, _state.value.comment)
            _state.update { it.copy(order = updated, editingRating = false) }
        }
    }

    fun canSaveRating(): Boolean {
        val order = _state.value.order ?: return false
        val delivered = order.status == OrderStatus.DELIVERED || order.status == OrderStatus.COMPLETED
        return delivered && _state.value.editingRating && _state.value.stars in 1..5
    }
}
