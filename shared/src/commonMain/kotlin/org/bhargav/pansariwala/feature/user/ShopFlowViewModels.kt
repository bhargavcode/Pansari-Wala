package org.bhargav.pansariwala.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.PlaceOrderItemDto
import org.bhargav.pansariwala.api.PlaceOrderRequest
import org.bhargav.pansariwala.api.QuoteDto
import org.bhargav.pansariwala.api.VerifyPaymentRequest
import org.bhargav.pansariwala.data.local.AppPreferences
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

data class CatalogUiState(
    val shop: org.bhargav.pansariwala.domain.model.MarketplaceShop? = null,
    val products: List<Product> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val cartCount: Int = 0,
)

class ShopCatalogViewModel(
    private val api: PansariApi,
    private val cart: CartStore,
    private val location: org.bhargav.pansariwala.platform.DeviceLocation,
) : ViewModel() {
    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    fun load(shopId: String) {
        cart.bindShop(shopId)
        viewModelScope.launch {
            cart.lines.collect { lines ->
                _state.update { it.copy(cartCount = lines.sumOf { line -> line.quantity.toInt() }) }
            }
        }
        viewModelScope.launch {
            runCatching { api.shopCatalog(shopId) }
                .onSuccess { products ->
                    _state.update { it.copy(loading = false, products = products) }
                }
                .onFailure { err ->
                    _state.update { it.copy(loading = false, error = err.message) }
                }
        }
        viewModelScope.launch {
            runCatching {
                val geo = location.currentOrDefault()
                api.nearbyShops(geo.lat, geo.lng, AppConstants.MAX_SEARCH_RADIUS_KM, "")
                    .firstOrNull { it.id == shopId }
            }.onSuccess { shop ->
                shop?.let {
                    cart.bindShop(shopId, it.name)
                    _state.update { s -> s.copy(shop = it) }
                }
            }
        }
    }

    fun add(product: Product) { cart.add(product) }
    fun increment(productId: String) { cart.increment(productId) }
    fun decrement(productId: String) { cart.decrement(productId) }
    fun quantityOf(productId: String): Int = cart.quantityOf(productId)
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
    else -> message.takeIf { it.isNotBlank() && !it.startsWith("Client request") }
        ?.let { UiText.Plain(it) }
        ?: UiText.res(Res.string.error_generic)
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
    val error: String? = null,
)

class OrderDetailsViewModel(
    private val api: PansariApi,
) : ViewModel() {
    private val _state = MutableStateFlow(OrderDetailsUiState())
    val state: StateFlow<OrderDetailsUiState> = _state.asStateFlow()
    private var pollJob: Job? = null

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
                        if (_state.value.order == null) {
                            _state.update { it.copy(error = err.message?.takeIf { m -> m.isNotBlank() } ?: "load_failed") }
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
