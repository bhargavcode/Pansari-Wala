package org.bhargav.pansariwala.feature.subscription

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.api.BillingModel
import org.bhargav.pansariwala.api.CommissionDayDto
import org.bhargav.pansariwala.api.CommissionOrderDto
import org.bhargav.pansariwala.api.ShopBillingDto
import org.bhargav.pansariwala.api.SubscriptionPlanDto
import org.bhargav.pansariwala.api.SubscriptionTxnDto
import org.bhargav.pansariwala.api.SubscriptionTxnType
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.printing.formatPrintDate
import org.bhargav.pansariwala.printing.formatPrintDateTime
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.feature_barcode_scanner
import pansariwala.shared.generated.resources.feature_barcode_search
import pansariwala.shared.generated.resources.feature_inventory_alerts
import pansariwala.shared.generated.resources.feature_online_orders
import pansariwala.shared.generated.resources.feature_report_generation
import pansariwala.shared.generated.resources.feature_voice_search
import pansariwala.shared.generated.resources.sub_active
import pansariwala.shared.generated.resources.sub_buy
import pansariwala.shared.generated.resources.sub_commission_day_orders
import pansariwala.shared.generated.resources.sub_commission_empty
import pansariwala.shared.generated.resources.sub_commission_fee
import pansariwala.shared.generated.resources.sub_commission_payout
import pansariwala.shared.generated.resources.sub_commission_rate
import pansariwala.shared.generated.resources.sub_commission_sale
import pansariwala.shared.generated.resources.sub_commission_summary
import pansariwala.shared.generated.resources.sub_commission_title
import pansariwala.shared.generated.resources.sub_current_plan
import pansariwala.shared.generated.resources.sub_days_left
import pansariwala.shared.generated.resources.sub_expired
import pansariwala.shared.generated.resources.sub_expired_warning
import pansariwala.shared.generated.resources.sub_expires_on
import pansariwala.shared.generated.resources.sub_features_included
import pansariwala.shared.generated.resources.sub_free_forever
import pansariwala.shared.generated.resources.sub_history_empty
import pansariwala.shared.generated.resources.sub_history_title
import pansariwala.shared.generated.resources.sub_model_commission
import pansariwala.shared.generated.resources.sub_model_free
import pansariwala.shared.generated.resources.sub_model_monthly
import pansariwala.shared.generated.resources.sub_model_none
import pansariwala.shared.generated.resources.sub_model_yearly
import pansariwala.shared.generated.resources.sub_monthly
import pansariwala.shared.generated.resources.sub_order_channel
import pansariwala.shared.generated.resources.sub_order_discount
import pansariwala.shared.generated.resources.sub_order_items
import pansariwala.shared.generated.resources.sub_order_subtotal
import pansariwala.shared.generated.resources.sub_order_title
import pansariwala.shared.generated.resources.sub_per_month
import pansariwala.shared.generated.resources.sub_per_year
import pansariwala.shared.generated.resources.sub_plan_name
import pansariwala.shared.generated.resources.sub_plans_title
import pansariwala.shared.generated.resources.sub_renew
import pansariwala.shared.generated.resources.sub_renew_warning
import pansariwala.shared.generated.resources.sub_started_on
import pansariwala.shared.generated.resources.sub_title
import pansariwala.shared.generated.resources.sub_txn_change
import pansariwala.shared.generated.resources.sub_txn_free
import pansariwala.shared.generated.resources.sub_txn_order
import pansariwala.shared.generated.resources.sub_txn_payment
import pansariwala.shared.generated.resources.sub_txn_period
import pansariwala.shared.generated.resources.sub_txn_purchase
import pansariwala.shared.generated.resources.sub_txn_renewal
import pansariwala.shared.generated.resources.sub_yearly
import pansariwala.shared.generated.resources.sub_yearly_saving

@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    onOpenDay: (String) -> Unit,
    viewModel: SubscriptionViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val data = state.data
    PansariScreen(
        title = stringResource(Res.string.sub_title),
        onBack = onBack,
        error = state.error.toErrorBanner(),
        onErrorAction = { handleErrorBannerAction(it, onRetry = viewModel::load, onDismiss = viewModel::dismissMessage) },
        isLoading = state.loading && data == null,
    ) {
        if (data == null) return@PansariScreen
        val billing = data.billing
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.message?.let { message ->
                item(key = "message") {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = MaterialTheme.shapes.medium) {
                        Text(message.asString(), modifier = Modifier.fillMaxWidth().padding(12.dp))
                    }
                }
            }
            item(key = "current") { CurrentPlanCard(billing) }
            if (billing.model == BillingModel.COMMISSION) {
                item(key = "commission") {
                    val summary = state.commission
                    SectionCard(
                        title = stringResource(Res.string.sub_commission_title),
                        subtitle = summary?.let {
                            stringResource(
                                Res.string.sub_commission_summary,
                                it.totalSaleInr.asMoney(),
                                it.commissionInr.asMoney(),
                                it.payoutInr.asMoney(),
                            )
                        },
                    ) {
                        if (summary == null || summary.days.isEmpty()) {
                            Text(stringResource(Res.string.sub_commission_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                items(state.commission?.days.orEmpty(), key = { "day-${it.day}" }) { day ->
                    CommissionDayRow(day, onClick = { onOpenDay(day.day) })
                }
            } else if (billing.model != BillingModel.FREE) {
                item(key = "plans-header") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Res.string.sub_plans_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.period == BillingModel.MONTHLY,
                                onClick = { viewModel.selectPeriod(BillingModel.MONTHLY) },
                                label = { Text(stringResource(Res.string.sub_monthly)) },
                            )
                            FilterChip(
                                selected = state.period == BillingModel.YEARLY,
                                onClick = { viewModel.selectPeriod(BillingModel.YEARLY) },
                                label = { Text(stringResource(Res.string.sub_yearly)) },
                            )
                        }
                    }
                }
                items(data.plans, key = { "plan-${it.id}" }) { plan ->
                    PlanCard(
                        plan = plan,
                        period = state.period,
                        isCurrent = plan.id == billing.planId && state.period == billing.model,
                        purchasing = state.purchasingPlanId == plan.id,
                        enabled = state.purchasingPlanId == null,
                        onBuy = { viewModel.buy(plan) },
                    )
                }
            }
            item(key = "history-header") {
                Text(stringResource(Res.string.sub_history_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            if (data.transactions.isEmpty()) {
                item(key = "history-empty") {
                    Text(stringResource(Res.string.sub_history_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            items(data.transactions, key = { "txn-${it.id}" }) { txn -> TransactionRow(txn) }
        }
    }
}

@Composable
private fun CurrentPlanCard(billing: ShopBillingDto) {
    SectionCard(title = stringResource(Res.string.sub_current_plan), subtitle = stringResource(modelLabel(billing.model))) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (billing.planName.isNotBlank()) Text(stringResource(Res.string.sub_plan_name, billing.planName))
            if (billing.model != BillingModel.NONE) {
                Text(
                    stringResource(if (billing.active) Res.string.sub_active else Res.string.sub_expired),
                    color = if (billing.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            billing.startedAtEpochMs?.let { Text(stringResource(Res.string.sub_started_on, formatPrintDate(it))) }
            when (billing.model) {
                BillingModel.MONTHLY, BillingModel.YEARLY -> billing.expiresAtEpochMs?.let {
                    Text(stringResource(Res.string.sub_expires_on, formatPrintDate(it)))
                }
                BillingModel.FREE -> Text(
                    billing.freeUntilEpochMs?.let { stringResource(Res.string.sub_expires_on, formatPrintDate(it)) }
                        ?: stringResource(Res.string.sub_free_forever),
                )
                BillingModel.COMMISSION -> Text(
                    stringResource(Res.string.sub_commission_rate, billing.commissionPercent.asQuantity()),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (billing.active) billing.daysLeft?.let { Text(stringResource(Res.string.sub_days_left, it)) }
            if (billing.planFeatures.isNotEmpty()) {
                Text(
                    stringResource(Res.string.sub_features_included, billing.planFeatures.map { featureLabel(it) }.joinToString()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (billing.showRenewWarning) {
                Text(
                    if (billing.active) stringResource(Res.string.sub_renew_warning, billing.daysLeft ?: 0)
                    else stringResource(Res.string.sub_expired_warning),
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlanDto,
    period: String,
    isCurrent: Boolean,
    purchasing: Boolean,
    enabled: Boolean,
    onBuy: () -> Unit,
) {
    val yearly = period == BillingModel.YEARLY
    val saving = plan.monthlyPriceInr * MONTHS_PER_YEAR - plan.yearlyPriceInr
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(plan.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (plan.description.isNotBlank()) {
                Text(plan.description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                stringResource(Res.string.sub_features_included, plan.features.map { featureLabel(it) }.joinToString()),
                style = MaterialTheme.typography.bodySmall,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(
                        if (yearly) stringResource(Res.string.sub_per_year, plan.yearlyPriceInr.asMoney())
                        else stringResource(Res.string.sub_per_month, plan.monthlyPriceInr.asMoney()),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (yearly && saving > 0.0) {
                        Text(stringResource(Res.string.sub_yearly_saving, saving.asMoney()), style = MaterialTheme.typography.labelMedium)
                    }
                }
                Button(onClick = onBuy, enabled = enabled) {
                    if (purchasing) {
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(if (isCurrent) Res.string.sub_renew else Res.string.sub_buy))
                    }
                }
            }
        }
    }
}

@Composable
private fun CommissionDayRow(day: CommissionDayDto, onClick: () -> Unit) {
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row {
                Text(formatPrintDate(day.dayStartEpochMs), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text(stringResource(Res.string.sub_commission_day_orders, day.orderCount), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AmountLine(stringResource(Res.string.sub_commission_sale), day.totalSaleInr)
            AmountLine(stringResource(Res.string.sub_commission_fee, day.commissionPercent.asQuantity()), -day.commissionInr)
            AmountLine(stringResource(Res.string.sub_commission_payout), day.payoutInr, emphasize = true)
        }
    }
}

@Composable
private fun TransactionRow(txn: SubscriptionTxnDto) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row {
                Text(stringResource(txnLabel(txn.type)), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (txn.amountInr > 0.0) Text(txn.amountInr.asMoney(), fontWeight = FontWeight.SemiBold)
            }
            val planLine = listOf(txn.planName, txn.period).filter { it.isNotBlank() }.joinToString(" · ")
            if (planLine.isNotBlank()) Text(planLine, style = MaterialTheme.typography.bodySmall)
            Text(formatPrintDateTime(txn.createdAtEpochMs), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            val start = txn.periodStartEpochMs
            val end = txn.periodEndEpochMs
            if (start != null && end != null) {
                Text(stringResource(Res.string.sub_txn_period, formatPrintDate(start), formatPrintDate(end)), style = MaterialTheme.typography.bodySmall)
            }
            txn.paymentId?.let { Text(stringResource(Res.string.sub_txn_payment, it), style = MaterialTheme.typography.labelSmall) }
            txn.razorpayOrderId?.let { Text(stringResource(Res.string.sub_txn_order, it), style = MaterialTheme.typography.labelSmall) }
            if (txn.note.isNotBlank()) Text(txn.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun CommissionDayScreen(
    day: String,
    onBack: () -> Unit,
    onOpenOrder: (String) -> Unit,
    viewModel: CommissionDayViewModel = koinViewModel(),
) {
    LaunchedEffect(day) { viewModel.load(day) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val data = state.data
    PansariScreen(
        title = data?.let { formatPrintDate(it.day.dayStartEpochMs) } ?: day,
        onBack = onBack,
        error = state.error.toErrorBanner(),
        onErrorAction = { handleErrorBannerAction(it, onRetry = { viewModel.load(day, force = true) }, onDismiss = viewModel::dismissError) },
        isLoading = state.loading && data == null,
    ) {
        if (data == null) return@PansariScreen
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "summary") { CommissionDayRow(data.day, onClick = {}) }
            items(data.orders, key = { it.orderId }) { order ->
                OutlinedCard(onClick = { onOpenOrder(order.orderId) }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row {
                            Text(order.shortId(), fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                            Text(formatPrintDateTime(order.createdAtEpochMs).substringAfter(' '), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text(
                            stringResource(Res.string.sub_order_channel, order.channel, order.paymentMethod) +
                                (order.customerName?.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        AmountLine(stringResource(Res.string.sub_commission_sale), order.saleInr)
                        AmountLine(stringResource(Res.string.sub_commission_fee, data.day.commissionPercent.asQuantity()), -order.commissionInr)
                        AmountLine(stringResource(Res.string.sub_commission_payout), order.payoutInr, emphasize = true)
                    }
                }
            }
        }
    }
}

@Composable
fun CommissionOrderScreen(
    day: String,
    orderId: String,
    onBack: () -> Unit,
    viewModel: CommissionDayViewModel = koinViewModel(),
) {
    LaunchedEffect(day) { viewModel.load(day) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val order = state.data?.orders?.firstOrNull { it.orderId == orderId }
    val percent = state.data?.day?.commissionPercent ?: 0.0
    PansariScreen(
        title = stringResource(Res.string.sub_order_title, order?.shortId() ?: orderId.takeLast(AppConstants.ORDER_SHORT_ID_LENGTH)),
        onBack = onBack,
        error = state.error.toErrorBanner(),
        onErrorAction = { handleErrorBannerAction(it, onRetry = { viewModel.load(day, force = true) }, onDismiss = viewModel::dismissError) },
        isLoading = state.loading && order == null,
    ) {
        if (order == null) return@PansariScreen
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item(key = "meta") {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(formatPrintDateTime(order.createdAtEpochMs), fontWeight = FontWeight.SemiBold)
                    Text(stringResource(Res.string.sub_order_channel, order.channel, order.paymentMethod), style = MaterialTheme.typography.bodySmall)
                    Text(order.status, style = MaterialTheme.typography.bodySmall)
                    order.customerName?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                }
            }
            item(key = "items-header") {
                Text(stringResource(Res.string.sub_order_items), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            items(order.items.withIndex().toList(), key = { "item-${it.index}" }) { (_, item) ->
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(item.variantLabel?.let { "${item.name} · $it" } ?: item.name)
                        Text(
                            "${item.quantity.asQuantity()} ${item.unit} × ${item.unitPrice.asMoney()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text((item.quantity * item.unitPrice).asMoney())
                }
            }
            item(key = "totals") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    HorizontalDivider()
                    AmountLine(stringResource(Res.string.sub_order_subtotal), order.itemsSubtotalInr)
                    if (order.discountInr > 0.0) AmountLine(stringResource(Res.string.sub_order_discount), -order.discountInr)
                    AmountLine(stringResource(Res.string.sub_commission_sale), order.saleInr)
                    AmountLine(stringResource(Res.string.sub_commission_fee, percent.asQuantity()), -order.commissionInr)
                    AmountLine(stringResource(Res.string.sub_commission_payout), order.payoutInr, emphasize = true)
                }
            }
        }
    }
}

@Composable
private fun AmountLine(label: String, amount: Double, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(
            if (amount < 0) "−${(-amount).asMoney()}" else amount.asMoney(),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Normal,
            color = if (amount < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun CommissionOrderDto.shortId(): String = "#" + orderId.takeLast(AppConstants.ORDER_SHORT_ID_LENGTH).uppercase()

private const val MONTHS_PER_YEAR = 12

private fun modelLabel(model: String): StringResource = when (model) {
    BillingModel.MONTHLY -> Res.string.sub_model_monthly
    BillingModel.YEARLY -> Res.string.sub_model_yearly
    BillingModel.COMMISSION -> Res.string.sub_model_commission
    BillingModel.FREE -> Res.string.sub_model_free
    else -> Res.string.sub_model_none
}

private fun txnLabel(type: String): StringResource = when (type) {
    SubscriptionTxnType.PURCHASE -> Res.string.sub_txn_purchase
    SubscriptionTxnType.RENEWAL -> Res.string.sub_txn_renewal
    SubscriptionTxnType.FREE_GRANT -> Res.string.sub_txn_free
    else -> Res.string.sub_txn_change
}

@Composable
private fun featureLabel(key: String): String = when (key) {
    AppConstants.ShopFeature.BARCODE_SCANNER -> stringResource(Res.string.feature_barcode_scanner)
    AppConstants.ShopFeature.VOICE_SEARCH -> stringResource(Res.string.feature_voice_search)
    AppConstants.ShopFeature.BARCODE_SEARCH -> stringResource(Res.string.feature_barcode_search)
    AppConstants.ShopFeature.REPORT_GENERATION -> stringResource(Res.string.feature_report_generation)
    AppConstants.ShopFeature.ONLINE_ORDERS -> stringResource(Res.string.feature_online_orders)
    AppConstants.ShopFeature.INVENTORY_ALERTS -> stringResource(Res.string.feature_inventory_alerts)
    else -> key
}
