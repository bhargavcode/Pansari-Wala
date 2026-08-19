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
import org.bhargav.pansariwala.domain.model.FulfillmentStep
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ShopOffer
import org.bhargav.pansariwala.domain.model.toFulfillmentStep
import org.bhargav.pansariwala.platform.DeviceLocation
import org.bhargav.pansariwala.platform.RazorpayCheckout
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.util.AppConstants
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.error_generic
import pansariwala.shared.generated.resources.error_razorpay_cancelled
import pansariwala.shared.generated.resources.error_razorpay_failed
import pansariwala.shared.generated.resources.error_razorpay_unavailable

data class CatalogUiState(
    val products: List<Product> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val cartCount: Int = 0,
)

class ShopCatalogViewModel(
    private val api: PansariApi,
    private val cart: CartStore,
) : ViewModel() {
    private val _state = MutableStateFlow(CatalogUiState())
    val state: StateFlow<CatalogUiState> = _state.asStateFlow()

    fun load(shopId: String) {
        cart.bindShop(shopId)
        viewModelScope.launch {
            runCatching { api.shopCatalog(shopId) }
                .onSuccess { products -> _state.update { it.copy(loading = false, products = products) } }
                .onFailure { err -> _state.update { it.copy(loading = false, error = err.message) } }
            cart.lines.collect { lines ->
                _state.update { it.copy(cartCount = lines.sumOf { line -> line.quantity.toInt() }) }
            }
        }
    }

    fun add(product: Product) { cart.add(product) }
}

data class CheckoutUiState(
    val quote: QuoteDto? = null,
    val offers: List<ShopOffer> = emptyList(),
    val offersExpanded: Boolean = false,
    val placing: Boolean = false,
    val error: UiText? = null,
)

class CheckoutViewModel(
    private val api: PansariApi,
    private val cart: CartStore,
    private val location: DeviceLocation,
    private val razorpay: RazorpayCheckout,
) : ViewModel() {
    private val _state = MutableStateFlow(CheckoutUiState())
    val state: StateFlow<CheckoutUiState> = _state.asStateFlow()

    fun load(shopId: String) {
        viewModelScope.launch {
            val geo = location.currentOrDefault()
            val items = cart.lines.value.map { PlaceOrderItemDto(it.product.id, it.quantity) }
            val quote = runCatching {
                api.quote(
                    org.bhargav.pansariwala.api.QuoteRequest(shopId, items, geo.lat, geo.lng),
                )
            }.getOrNull()
            val offers = runCatching { api.shopOffers(shopId) }.getOrDefault(emptyList())
            _state.update { it.copy(quote = quote, offers = offers) }
        }
    }

    fun toggleOffers() { _state.update { it.copy(offersExpanded = !it.offersExpanded) } }

    fun place(shopId: String, onPlaced: (String) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(placing = true, error = null) }
            runCatching {
                val quote = _state.value.quote ?: error("Missing quote")
                val amountPaise = (quote.payable * 100).toLong()
                val rzp = api.createRazorpayOrder(shopId, amountPaise)
                val paid = razorpay.pay(
                    keyId = rzp.keyId,
                    orderId = rzp.orderId,
                    amountPaise = rzp.amountPaise,
                    customerName = "Customer",
                    customerPhone = "",
                    description = "Pansari order",
                ).getOrThrow()
                val verified = api.verifyPayment(
                    VerifyPaymentRequest(paid.orderId, paid.paymentId, paid.signature),
                )
                require(verified) { "Payment could not be verified" }
                val order = api.placeOrder(
                    PlaceOrderRequest(
                        shopId = shopId,
                        items = cart.lines.value.map { PlaceOrderItemDto(it.product.id, it.quantity) },
                        razorpayPaymentId = paid.paymentId,
                        razorpayOrderId = paid.orderId,
                        razorpaySignature = paid.signature,
                    ),
                )
                cart.clear()
                order.id
            }.onSuccess(onPlaced)
                .onFailure { _state.update { s -> s.copy(placing = false, error = checkoutError(it)) } }
        }
    }
}

private fun checkoutError(err: Throwable): UiText = when (err.message) {
    AppConstants.Razorpay.ERROR_CANCELLED -> UiText.res(Res.string.error_razorpay_cancelled)
    AppConstants.Razorpay.ERROR_UNAVAILABLE -> UiText.res(Res.string.error_razorpay_unavailable)
    AppConstants.Razorpay.ERROR_FAILED -> UiText.res(Res.string.error_razorpay_failed)
    else -> err.message?.let { UiText.Plain(it) } ?: UiText.res(Res.string.error_generic)
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
                            )
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
