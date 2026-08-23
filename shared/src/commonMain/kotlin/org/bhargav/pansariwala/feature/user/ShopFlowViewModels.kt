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
import org.bhargav.pansariwala.domain.model.FulfillmentStep
import org.bhargav.pansariwala.domain.model.MarketplaceShop
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.SavedAddress
import org.bhargav.pansariwala.domain.model.ShopOffer
import org.bhargav.pansariwala.domain.model.toFulfillmentStep
import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.platform.RazorpayCheckout
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.ui.AsyncUiState
import org.bhargav.pansariwala.ui.beginLoad
import org.bhargav.pansariwala.util.AppConstants
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
    val products: List<Product> = emptyList(),
    val ratings: List<org.bhargav.pansariwala.domain.model.ShopReview> = emptyList(),
    val cartCount: Int = 0,
    val productQuery: String = "",
    val selectedTab: CatalogTab = CatalogTab.PRODUCTS,
    val ratingFilterStars: Set<Int> = emptySet(),
    val showRatingFilterSheet: Boolean = false,
)

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

    fun load(shopId: String) {
        cart.bindShop(shopId)
        viewModelScope.launch {
            cart.lines.collect { lines ->
                val count = lines.sumOf { line -> line.quantity.toInt() }
                val current = _state.value
                if (current is AsyncUiState.Success) {
                    _state.value = current.copy(data = current.data.copy(cartCount = count))
                }
            }
        }
        viewModelScope.launch {
            _state.value = _state.value.beginLoad()
            runCatching {
                coroutineScope {
                    val productsDef = async { api.shopCatalog(shopId) }
                    val ratingsDef = async { api.shopRatings(shopId) }
                    val shopDef = async {
                        val geo = location.currentOrDefault()
                        api.nearbyShops(geo.lat, geo.lng, AppConstants.MAX_SEARCH_RADIUS_KM, "")
                            .firstOrNull { it.id == shopId }
                    }
                    val products = productsDef.await()
                    val shop = shopDef.await()
                    val ratings = ratingsDef.await()
                    shop?.let { cart.bindShop(shopId, it.name) }
                    CatalogData(shop = shop, products = products, ratings = ratings, cartCount = cart.itemCount)
                }
            }.onSuccess { data ->
                _state.value = AsyncUiState.Success(data)
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

    fun add(product: Product) { cart.add(product) }
    fun increment(productId: String) { cart.increment(productId) }
    fun decrement(productId: String) { cart.decrement(productId) }
    fun quantityOf(productId: String): Int = cart.quantityOf(productId)

    fun setProductQuery(value: String) {
        updateSuccess { it.copy(productQuery = value) }
    }

    fun selectTab(tab: CatalogTab) {
        updateSuccess { it.copy(selectedTab = tab) }
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

    fun filteredProducts(data: CatalogData): List<Product> {
        val query = data.productQuery.trim()
        if (query.isBlank()) return data.products
        return data.products.filter {
            it.name.contains(query, ignoreCase = true) ||
                it.nameHi.contains(query, ignoreCase = true)
        }
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

    fun increment(productId: String) { cart.increment(productId) }
    fun decrement(productId: String) { cart.decrement(productId) }
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
            val profile = runCatching { api.me() }.getOrNull()
            val addresses = profile?.addresses.orEmpty()
            val selected = addresses.firstOrNull { it.isDefault } ?: addresses.firstOrNull()
            val dropLat = selected?.location?.lat ?: profile?.location?.lat ?: geo.lat
            val dropLng = selected?.location?.lng ?: profile?.location?.lng ?: geo.lng
            val items = cart.lines.value.map { PlaceOrderItemDto(it.product.id, it.quantity) }
            val quote = runCatching {
                api.quote(
                    org.bhargav.pansariwala.api.QuoteRequest(shopId, items, dropLat, dropLng),
                )
            }.getOrNull()
            val offers = runCatching { api.shopOffers(shopId) }.getOrDefault(emptyList())
            val shop = runCatching {
                api.nearbyShops(dropLat, dropLng, AppConstants.MAX_SEARCH_RADIUS_KM, "")
                    .firstOrNull { it.id == shopId }
            }.getOrNull()
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
            _state.update { it.copy(selectedAddressId = addressId) }
            load(shopId)
        }
    }

    fun place(shopId: String, onPlaced: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(placing = true, error = null, needsProfile = false, snackbar = null) }
            runCatching {
                val items = cart.lines.value.map { PlaceOrderItemDto(it.product.id, it.quantity) }
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
                val profile = api.me()

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

class OrderDetailsViewModel(
    private val api: PansariApi,
) : ViewModel() {
    private val _state = MutableStateFlow(OrderDetailsUiState())
    val state: StateFlow<OrderDetailsUiState> = _state.asStateFlow()
    private var pollJob: Job? = null

    fun dismissError() { _state.update { it.copy(error = null) } }

    fun load(orderId: String) {
        pollJob?.cancel()
        pollJob = viewModelScope.launch {
            while (true) {
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
                delay(AppConstants.LIVE_ALERT_POLL_MS)
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
