package org.bhargav.pansariwala.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderChannel
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.feature.order.OrderItemsTable
import org.bhargav.pansariwala.feature.order.paymentMethodLabelRes
import org.bhargav.pansariwala.feature.printer.PrintMenuButton
import org.bhargav.pansariwala.feature.printer.orderPrintActions
import org.bhargav.pansariwala.feature.user.userOrderStatusLabel
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.i18n.WALK_IN_CUSTOMER_KEY
import org.bhargav.pansariwala.printing.formatPrintDateTime
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.cancel_reason_label
import pansariwala.shared.generated.resources.error_order_not_found
import pansariwala.shared.generated.resources.order_card_total
import pansariwala.shared.generated.resources.order_detail_customer
import pansariwala.shared.generated.resources.order_detail_discount
import pansariwala.shared.generated.resources.order_detail_items
import pansariwala.shared.generated.resources.order_detail_open_editor
import pansariwala.shared.generated.resources.order_detail_payment
import pansariwala.shared.generated.resources.order_detail_reorder
import pansariwala.shared.generated.resources.order_detail_subtotal
import pansariwala.shared.generated.resources.order_detail_title
import pansariwala.shared.generated.resources.report_offline
import pansariwala.shared.generated.resources.report_online
import pansariwala.shared.generated.resources.walk_in

data class ShopOrderDetailUiState(
    val loading: Boolean = true,
    val order: Order? = null,
    val error: UiText? = null,
)

/** Read-only bill view for one shop order: POS bills come from the local DB, online orders from the server. */
class ShopOrderDetailViewModel(
    private val orderId: String,
    private val online: Boolean,
    private val repository: ShopRepository,
    private val api: PansariApi,
) : ViewModel() {
    private val _state = MutableStateFlow(ShopOrderDetailUiState())
    val state: StateFlow<ShopOrderDetailUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { if (online) api.order(orderId) else repository.getOrder(orderId) }
                .onSuccess { order ->
                    _state.update {
                        it.copy(
                            loading = false,
                            order = order,
                            error = if (order == null) UiText.res(Res.string.error_order_not_found) else null,
                        )
                    }
                }
                .onFailure { err ->
                    err.rethrowIfStructuredCancellation()
                    _state.update { it.copy(loading = false, error = err.toApiUiText()) }
                }
        }
    }

    fun dismissError() = _state.update { it.copy(error = null) }
}

@Composable
fun ShopOrderDetailScreen(
    orderId: String,
    online: Boolean,
    onBack: () -> Unit,
    onEditOrder: (orderId: String) -> Unit,
    onReorder: (orderId: String) -> Unit,
    viewModel: ShopOrderDetailViewModel = koinViewModel(key = "shop_order_$orderId") { parametersOf(orderId, online) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val order = state.order
    PansariScreen(
        title = order?.let { stringResource(Res.string.order_detail_title, it.displayNumber) },
        onBack = onBack,
        actions = {
            if (order != null) PrintMenuButton(actions = remember(order) { orderPrintActions(order) })
        },
        error = state.error.toErrorBanner(retryable = order == null),
        onErrorAction = { handleErrorBannerAction(it, onRetry = viewModel::refresh, onDismiss = viewModel::dismissError) },
        isLoading = state.loading && order == null,
    ) {
        if (order == null) return@PansariScreen
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OrderHeaderCard(order)
            SectionCard(
                title = stringResource(Res.string.order_detail_items, order.itemCount),
                modifier = Modifier.fillMaxWidth(),
            ) {
                OrderItemsTable(order.items, Modifier.padding(top = 8.dp))
            }
            SectionCard(title = stringResource(Res.string.order_detail_payment), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AmountRow(stringResource(Res.string.order_detail_subtotal), order.subtotal.asMoney())
                    val discount = order.quote?.discount ?: order.discount
                    if (discount > 0) AmountRow(stringResource(Res.string.order_detail_discount), "−${discount.asMoney()}")
                    HorizontalDivider()
                    AmountRow(stringResource(Res.string.order_card_total), order.totalValue.asMoney(), emphasize = true)
                    Text(
                        stringResource(paymentMethodLabelRes(order.paymentMethod)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (order.channel == OrderChannel.POS) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = { onEditOrder(order.id) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.order_detail_open_editor))
                    }
                    Button(onClick = { onReorder(order.id) }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.order_detail_reorder))
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderHeaderCard(order: Order) {
    val online = order.channel == OrderChannel.ONLINE
    val accent = if (online) OnlineColor else OfflineColor
    SectionCard(
        title = "#${order.displayNumber}",
        subtitle = formatPrintDateTime(order.createdAtEpochMs),
        modifier = Modifier.fillMaxWidth(),
        action = {
            Surface(color = accent.copy(alpha = 0.15f), shape = RoundedCornerShape(50)) {
                Text(
                    "${stringResource(if (online) Res.string.report_online else Res.string.report_offline)} · ${userOrderStatusLabel(order.status)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = accent,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        },
    ) {
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val name = order.customerName?.takeUnless { it.isBlank() || it == WALK_IN_CUSTOMER_KEY }
                ?: stringResource(Res.string.walk_in)
            Text(stringResource(Res.string.order_detail_customer, name), style = MaterialTheme.typography.bodyMedium)
            order.customerPhone?.takeIf(String::isNotBlank)?.let {
                Text("+91 $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            order.deliveryAddress?.takeIf { online && it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (order.status == OrderStatus.CANCELLED || order.status == OrderStatus.REJECTED) {
                order.cancelReason?.takeIf(String::isNotBlank)?.let {
                    Text(
                        "${stringResource(Res.string.cancel_reason_label)}: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun AmountRow(label: String, value: String, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            label,
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            value,
            style = if (emphasize) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
