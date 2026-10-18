package org.bhargav.pansariwala.feature.subscription

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.BillingModel
import org.bhargav.pansariwala.api.CommissionDayOrdersDto
import org.bhargav.pansariwala.api.CommissionSummaryDto
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.ShopSubscriptionDto
import org.bhargav.pansariwala.api.SubscriptionCheckoutRequest
import org.bhargav.pansariwala.api.SubscriptionConfirmRequest
import org.bhargav.pansariwala.api.SubscriptionPlanDto
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.platform.RazorpayCheckout
import org.bhargav.pansariwala.printing.formatPrintDate
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.sub_payment_failed
import pansariwala.shared.generated.resources.sub_purchase_success

data class SubscriptionUiState(
    val loading: Boolean = true,
    val data: ShopSubscriptionDto? = null,
    val commission: CommissionSummaryDto? = null,
    val period: String = BillingModel.YEARLY,
    val purchasingPlanId: String? = null,
    val message: UiText? = null,
    val error: UiText? = null,
)

class SubscriptionViewModel(
    private val api: PansariApi,
    private val razorpay: RazorpayCheckout,
    private val preferences: AppPreferences,
) : ViewModel() {
    private val _state = MutableStateFlow(SubscriptionUiState())
    val state: StateFlow<SubscriptionUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val data = api.shopSubscription()
                val commission = if (data.billing.model == BillingModel.COMMISSION) api.commissionSummary(null, null) else null
                data to commission
            }.onSuccess { (data, commission) ->
                _state.update {
                    it.copy(
                        loading = false,
                        data = data,
                        commission = commission,
                        period = data.billing.model.takeIf { m -> m == BillingModel.MONTHLY || m == BillingModel.YEARLY } ?: it.period,
                    )
                }
            }.onFailure { e ->
                e.rethrowIfStructuredCancellation()
                _state.update { it.copy(loading = false, error = e.toApiUiText()) }
            }
        }
    }

    fun selectPeriod(period: String) = _state.update { it.copy(period = period) }
    fun dismissMessage() = _state.update { it.copy(message = null, error = null) }

    fun buy(plan: SubscriptionPlanDto) {
        if (_state.value.purchasingPlanId != null) return
        val period = _state.value.period
        viewModelScope.launch {
            _state.update { it.copy(purchasingPlanId = plan.id, error = null, message = null) }
            runCatching {
                val checkout = api.subscriptionCheckout(SubscriptionCheckoutRequest(plan.id, period))
                val paid = razorpay.pay(
                    keyId = checkout.keyId,
                    orderId = checkout.razorpayOrderId,
                    amountPaise = checkout.amountPaise,
                    customerName = preferences.getShopName(),
                    customerPhone = "",
                    description = plan.name,
                ).getOrElse { throw SubscriptionPaymentException() }
                val updated = api.confirmSubscription(
                    SubscriptionConfirmRequest(plan.id, period, paid.orderId, paid.paymentId, paid.signature),
                )
                runCatching { preferences.setShopFeatures(api.shopProfile().shop.features) }
                updated
            }.onSuccess { updated ->
                _state.update {
                    it.copy(
                        purchasingPlanId = null,
                        data = updated,
                        message = updated.billing.expiresAtEpochMs?.let { end ->
                            UiText.res(Res.string.sub_purchase_success, formatPrintDate(end))
                        },
                    )
                }
            }.onFailure { e ->
                e.rethrowIfStructuredCancellation()
                val error = if (e is SubscriptionPaymentException) UiText.res(Res.string.sub_payment_failed) else e.toApiUiText()
                _state.update { it.copy(purchasingPlanId = null, error = error) }
            }
        }
    }
}

private class SubscriptionPaymentException : Exception()

data class CommissionDayUiState(
    val loading: Boolean = true,
    val data: CommissionDayOrdersDto? = null,
    val error: UiText? = null,
)

/** Backs both the day's order list and a single order detail (both read the same day payload). */
class CommissionDayViewModel(private val api: PansariApi) : ViewModel() {
    private val _state = MutableStateFlow(CommissionDayUiState())
    val state: StateFlow<CommissionDayUiState> = _state.asStateFlow()
    private var loadedDay: String? = null

    fun load(day: String, force: Boolean = false) {
        if (!force && loadedDay == day && _state.value.data != null) return
        loadedDay = day
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { api.commissionDay(day) }
                .onSuccess { data -> _state.update { it.copy(loading = false, data = data) } }
                .onFailure { e ->
                    e.rethrowIfStructuredCancellation()
                    _state.update { it.copy(loading = false, error = e.toApiUiText()) }
                }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }
}
