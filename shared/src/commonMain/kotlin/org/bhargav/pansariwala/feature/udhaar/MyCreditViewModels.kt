package org.bhargav.pansariwala.feature.udhaar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.MyCreditShopDetailDto
import org.bhargav.pansariwala.api.MyCreditSummaryDto
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.feature.order.buildUpiUri
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.util.AppConstants
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.borrowed_repay_failed
import pansariwala.shared.generated.resources.borrowed_repay_no_upi
import pansariwala.shared.generated.resources.borrowed_repay_processing
import pansariwala.shared.generated.resources.borrowed_repay_success
import pansariwala.shared.generated.resources.error_udhaar_amount

data class MyCreditUiState(
    val summary: MyCreditSummaryDto? = null,
    val loading: Boolean = false,
    val error: UiText? = null,
)

/** Total Udhaar the signed-in customer owes across shops (matched by mobile number). */
class MyCreditViewModel(private val api: PansariApi) : ViewModel() {
    private val _state = MutableStateFlow(MyCreditUiState())
    val state: StateFlow<MyCreditUiState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { api.myCredit() }
                .onSuccess { summary -> _state.update { it.copy(summary = summary, loading = false) } }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { it.copy(loading = false, error = err.toApiUiText()) }
                }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }
}

/** [gatewayVerified]: the server confirms the payment with the gateway, so the customer is not asked. */
data class PendingRepay(val txnId: String, val upiUri: String, val gatewayVerified: Boolean = false)

data class MyCreditShopUiState(
    val detail: MyCreditShopDetailDto? = null,
    val loading: Boolean = true,
    val error: UiText? = null,
    val filter: UdhaarTxnFilter = UdhaarTxnFilter.ALL,
    val showRepay: Boolean = false,
    val amount: String = "",
    val repayBusy: Boolean = false,
    val repayError: UiText? = null,
    /** Set after the UPI app is opened; cleared once the customer confirms the outcome. */
    val pending: PendingRepay? = null,
    val launchUpi: String? = null,
    val askOutcome: Boolean = false,
    val message: UiText? = null,
)

class MyCreditShopViewModel(
    private val shopId: String,
    private val api: PansariApi,
) : ViewModel() {
    private val _state = MutableStateFlow(MyCreditShopUiState())
    val state: StateFlow<MyCreditShopUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { api.myCreditShop(shopId) }
                .onSuccess { detail -> _state.update { it.copy(detail = detail, loading = false) } }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { it.copy(loading = false, error = err.toApiUiText()) }
                }
        }
    }

    fun setFilter(filter: UdhaarTxnFilter) = _state.update { it.copy(filter = filter) }

    fun dismissError() = _state.update { it.copy(error = null) }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun openRepay() = _state.update { s ->
        val due = s.detail?.shop?.outstanding ?: 0.0
        s.copy(showRepay = true, amount = due.takeIf { it > 0 }?.let(::plainAmount).orEmpty(), repayError = null)
    }

    fun closeRepay() = _state.update { it.copy(showRepay = false, repayBusy = false) }

    fun setAmount(value: String) =
        _state.update { it.copy(amount = value.filter { c -> c.isDigit() || c == '.' }, repayError = null) }

    fun startRepay() {
        val s = _state.value
        val due = s.detail?.shop?.outstanding ?: 0.0
        val amount = parseRepayAmount(s.amount, due)
        if (amount == null) {
            _state.update { it.copy(repayError = UiText.res(Res.string.error_udhaar_amount)) }
            return
        }
        _state.update { it.copy(repayBusy = true) }
        viewModelScope.launch {
            runCatching { api.startCreditRepayment(shopId, amount) }
                .onSuccess { res ->
                    if (res.payUri.isBlank() && res.upiId.isBlank()) {
                        reportOutcome(res.txn.id, success = false, failure = UiText.res(Res.string.borrowed_repay_no_upi))
                        _state.update { it.copy(repayBusy = false, showRepay = false) }
                        return@onSuccess
                    }
                    val uri = res.payUri.ifBlank { buildUpiUri(res.upiId, res.payeeName, amount) }
                    _state.update {
                        it.copy(
                            repayBusy = false,
                            showRepay = false,
                            pending = PendingRepay(res.txn.id, uri, res.gateway == AppConstants.Udhaar.GATEWAY_CASHFREE),
                            launchUpi = uri,
                        )
                    }
                }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { it.copy(repayBusy = false, repayError = err.toApiUiText()) }
                }
        }
    }

    fun onUpiLaunched() = _state.update { it.copy(launchUpi = null) }

    /** The UPI app could not be opened; the pending repayment is marked failed. */
    fun onUpiMissing() {
        val pending = _state.value.pending ?: return
        _state.update { it.copy(launchUpi = null, pending = null) }
        reportOutcome(pending.txnId, success = false)
    }

    /** Returning from the UPI app: ask whether the payment went through. */
    fun onResumed() {
        val pending = _state.value.pending ?: return
        if (_state.value.launchUpi != null) return
        if (pending.gatewayVerified) {
            _state.update { it.copy(pending = null) }
            reportOutcome(pending.txnId, success = true)
        } else {
            _state.update { it.copy(askOutcome = true) }
        }
    }

    fun confirmOutcome(success: Boolean) {
        val pending = _state.value.pending ?: return
        _state.update { it.copy(askOutcome = false, pending = null) }
        reportOutcome(pending.txnId, success)
    }

    private fun reportOutcome(txnId: String, success: Boolean, failure: UiText? = null) {
        viewModelScope.launch {
            runCatching { api.reportCreditRepayment(shopId, txnId, success) }
                .onSuccess { txn ->
                    _state.update {
                        it.copy(
                            message = when (txn.status) {
                                AppConstants.Udhaar.STATUS_SUCCESS -> UiText.res(Res.string.borrowed_repay_success)
                                AppConstants.Udhaar.STATUS_PENDING -> UiText.res(Res.string.borrowed_repay_processing)
                                else -> failure ?: UiText.res(Res.string.borrowed_repay_failed)
                            },
                        )
                    }
                    refresh()
                }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { it.copy(error = err.toApiUiText()) }
                }
        }
    }

    private fun plainAmount(value: Double): String {
        val paise = kotlin.math.round(value * 100).toLong()
        return if (paise % 100 == 0L) (paise / 100).toString() else (paise / 100.0).toString()
    }
}