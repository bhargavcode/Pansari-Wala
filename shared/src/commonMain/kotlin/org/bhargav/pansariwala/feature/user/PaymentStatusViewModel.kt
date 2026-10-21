package org.bhargav.pansariwala.feature.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.domain.model.PaymentIntent
import org.bhargav.pansariwala.domain.model.PaymentIntentStatus
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.notification.NotificationGateway
import org.bhargav.pansariwala.notification.PaymentUpdates
import org.bhargav.pansariwala.util.AppConstants
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.order_pay_still_pending
import pansariwala.shared.generated.resources.order_pay_upi_open_failed
import kotlin.time.Clock

data class PaymentStatusUiState(
    val intent: PaymentIntent? = null,
    val nowMs: Long = 0L,
    val utr: String = "",
    val busy: Boolean = false,
    val note: UiText? = null,
    val error: UiText? = null,
    /** Opens the UPI app by itself (fresh checkout); false when opened from a notification. */
    val autoLaunch: Boolean = false,
    /** Highest attempt whose UPI app was already opened, so each attempt launches once. */
    val launchedAttempt: Int = 0,
    val confirmLeave: Boolean = false,
) {
    val pending: Boolean get() = intent?.status == PaymentIntentStatus.PENDING
    val shouldLaunch: Boolean
        get() = autoLaunch && pending && intent?.upiPayUri != null && (intent.attemptNo > launchedAttempt)
    val secondsLeft: Long get() = intent?.let { ((it.expiresAtEpochMs - nowMs) / 1_000L).coerceAtLeast(0L) } ?: 0L
}

/**
 * Live state of one payment transaction: WebSocket push, with polling as a safety net (and the
 * server's own expiry), so the screen settles even if the socket or the app was interrupted.
 */
class PaymentStatusViewModel(
    private val api: PansariApi,
    private val cart: CartStore,
    private val notifications: NotificationGateway,
) : ViewModel() {
    private val _state = MutableStateFlow(PaymentStatusUiState(nowMs = now()))
    val state: StateFlow<PaymentStatusUiState> = _state.asStateFlow()
    private var intentId: String? = null
    private var lastGatewayCheck = 0L

    fun start(intentId: String, autoLaunch: Boolean) {
        if (this.intentId == intentId) return
        this.intentId = intentId
        _state.update { it.copy(autoLaunch = autoLaunch) }
        viewModelScope.launch { fetch() }
        viewModelScope.launch {
            while (isActive && !isFinal()) {
                runCatching { api.paymentIntentUpdates(intentId).collect(::apply) }
                    .onFailure { it.rethrowIfStructuredCancellation() }
                if (isFinal()) break
                delay(AppConstants.Payment.SOCKET_RETRY_MS)
            }
        }
        viewModelScope.launch {
            while (isActive && !isFinal()) {
                delay(AppConstants.Payment.POLL_MS)
                if (!isFinal()) fetch()
            }
        }
        viewModelScope.launch {
            while (isActive) {
                _state.update { it.copy(nowMs = now()) }
                delay(AppConstants.Payment.COUNTDOWN_TICK_MS)
            }
        }
        viewModelScope.launch {
            PaymentUpdates.events.collect { if (it == intentId) fetch() }
        }
    }

    fun onLaunched(attemptNo: Int) {
        _state.update { it.copy(launchedAttempt = maxOf(it.launchedAttempt, attemptNo)) }
    }

    fun onUpiAppMissing() {
        _state.update { it.copy(note = UiText.res(Res.string.order_pay_upi_open_failed)) }
    }

    fun setUtr(value: String) {
        val clean = value.filter { it.isLetterOrDigit() }.take(AppConstants.Payment.UTR_MAX_LENGTH)
        _state.update { it.copy(utr = clean, note = null) }
    }

    fun canSubmitUtr(): Boolean = _state.value.utr.length >= AppConstants.Payment.UTR_MIN_LENGTH && !_state.value.busy

    fun submitUtr() {
        val id = intentId ?: return
        if (!canSubmitUtr()) return
        runAction { api.submitPaymentUtr(id, _state.value.utr) }
    }

    /** "I have paid": asks the server to re-check the gateway right away. */
    fun checkNow() {
        val id = intentId ?: return
        runAction {
            val latest = api.refreshPaymentIntent(id)
            if (latest.status == PaymentIntentStatus.PENDING) {
                _state.update { it.copy(note = UiText.res(Res.string.order_pay_still_pending)) }
            }
            latest
        }
    }

    /** Puts the transaction's items back in the cart; the next checkout reuses this transaction id. */
    fun retry(onCartReady: (String) -> Unit) {
        val intent = _state.value.intent ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            restoreCartForRetry(api, cart, intent)
            _state.update { it.copy(busy = false) }
            onCartReady(intent.shopId)
        }
    }

    fun requestLeave() { _state.update { it.copy(confirmLeave = true) } }
    fun cancelLeave() { _state.update { it.copy(confirmLeave = false) } }
    fun dismissError() { _state.update { it.copy(error = null) } }

    private suspend fun fetch() {
        val id = intentId ?: return
        val current = _state.value.intent
        val checkGateway = current != null && current.status == PaymentIntentStatus.PENDING && !current.isDirectUpi &&
            now() - lastGatewayCheck >= AppConstants.Payment.GATEWAY_CHECK_MS
        runCatching {
            if (checkGateway) {
                lastGatewayCheck = now()
                api.refreshPaymentIntent(id)
            } else {
                api.paymentIntent(id)
            }
        }
            .onSuccess(::apply)
            .onFailure { err ->
                err.rethrowIfStructuredCancellation()
                if (_state.value.intent == null) _state.update { it.copy(error = err.toApiUiText()) }
            }
    }

    private fun runAction(block: suspend () -> PaymentIntent) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, note = null) }
            runCatching { block() }
                .onSuccess { apply(it); _state.update { s -> s.copy(busy = false) } }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { it.copy(busy = false, error = err.toApiUiText()) }
                }
        }
    }

    private fun apply(intent: PaymentIntent) {
        val current = _state.value.intent
        // Socket and polling race: never step back to an older attempt or out of a final state.
        if (current != null && (intent.attemptNo < current.attemptNo || (current.isFinal && !intent.isFinal && intent.attemptNo == current.attemptNo))) {
            return
        }
        _state.update { it.copy(intent = intent, error = null) }
        if (intent.status == PaymentIntentStatus.SUCCESS) {
            if (cart.shopId.value == intent.shopId) cart.clear()
            cart.clearRetry()
        }
        if (intent.isFinal) notifications.clearOrder(intent.id)
    }

    private fun isFinal(): Boolean = _state.value.intent?.isFinal == true

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()
}

/** Puts an unpaid checkout's items back in the cart; only a FAILED transaction is retried under its own id. */
internal suspend fun restoreCartForRetry(api: PansariApi, cart: CartStore, intent: PaymentIntent) {
    val catalog = runCatching { api.shopCatalog(intent.shopId) }
        .onFailure { it.rethrowIfStructuredCancellation() }
        .getOrDefault(emptyList())
        .associateBy { it.id }
    val lines = intent.items.mapNotNull { item ->
        val product = catalog[item.productId] ?: return@mapNotNull null
        CartStore.Line(product, item.variantId?.let { product.variant(it) }, item.quantity)
    }
    val retryId = intent.id.takeIf { intent.status == PaymentIntentStatus.FAILED }
    cart.restoreForRetry(intent.shopId, intent.shopName, lines, retryId)
}
