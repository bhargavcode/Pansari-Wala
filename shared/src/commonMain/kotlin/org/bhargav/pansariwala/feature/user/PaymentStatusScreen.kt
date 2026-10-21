package org.bhargav.pansariwala.feature.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.domain.model.PaymentIntent
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.domain.model.PaymentIntentStatus
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.order_pay_check_status
import pansariwala.shared.generated.resources.order_pay_utr_hint
import pansariwala.shared.generated.resources.order_pay_utr_label
import pansariwala.shared.generated.resources.order_pay_utr_submit
import pansariwala.shared.generated.resources.order_pay_utr_submitted
import pansariwala.shared.generated.resources.pay_detail_amount
import pansariwala.shared.generated.resources.pay_detail_attempt
import pansariwala.shared.generated.resources.pay_detail_item
import pansariwala.shared.generated.resources.pay_detail_method
import pansariwala.shared.generated.resources.pay_detail_order
import pansariwala.shared.generated.resources.pay_detail_refund
import pansariwala.shared.generated.resources.pay_detail_shop
import pansariwala.shared.generated.resources.pay_detail_txn
import pansariwala.shared.generated.resources.pay_detail_upi
import pansariwala.shared.generated.resources.pay_detail_utr
import pansariwala.shared.generated.resources.pay_details_title
import pansariwala.shared.generated.resources.pay_do_not_close
import pansariwala.shared.generated.resources.pay_failed_body
import pansariwala.shared.generated.resources.pay_failed_title
import pansariwala.shared.generated.resources.pay_home
import pansariwala.shared.generated.resources.pay_leave_body
import pansariwala.shared.generated.resources.pay_leave_confirm
import pansariwala.shared.generated.resources.pay_leave_stay
import pansariwala.shared.generated.resources.pay_leave_title
import pansariwala.shared.generated.resources.pay_method_direct
import pansariwala.shared.generated.resources.pay_method_gateway
import pansariwala.shared.generated.resources.pay_open_upi
import pansariwala.shared.generated.resources.pay_reason_not_received
import pansariwala.shared.generated.resources.pay_reason_out_of_stock
import pansariwala.shared.generated.resources.pay_reason_shop_not_received
import pansariwala.shared.generated.resources.pay_refunded_body
import pansariwala.shared.generated.resources.pay_refunded_direct_body
import pansariwala.shared.generated.resources.pay_refunded_title
import pansariwala.shared.generated.resources.pay_retry
import pansariwala.shared.generated.resources.pay_success_body
import pansariwala.shared.generated.resources.pay_success_title
import pansariwala.shared.generated.resources.pay_time_left
import pansariwala.shared.generated.resources.pay_time_up
import pansariwala.shared.generated.resources.pay_title
import pansariwala.shared.generated.resources.pay_view_order
import pansariwala.shared.generated.resources.pay_waiting_body
import pansariwala.shared.generated.resources.pay_waiting_direct_body
import pansariwala.shared.generated.resources.pay_waiting_title

/**
 * Opens the UPI app once per attempt, then waits for the server's verdict (WebSocket + polling).
 * Back asks for confirmation while the payment is pending; the server keeps waiting regardless.
 */
@Composable
fun PaymentStatusScreen(
    intentId: String,
    autoLaunch: Boolean,
    onViewOrder: (String) -> Unit,
    onRetry: (String) -> Unit,
    onHome: () -> Unit,
    viewModel: PaymentStatusViewModel = koinViewModel(),
) {
    LaunchedEffect(intentId) { viewModel.start(intentId, autoLaunch) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val intent = state.intent
    val uriHandler = LocalUriHandler.current
    fun openUpi(target: PaymentIntent) {
        val uri = target.upiPayUri ?: return
        runCatching { uriHandler.openUri(uri) }.onFailure { viewModel.onUpiAppMissing() }
        viewModel.onLaunched(target.attemptNo)
    }
    LaunchedEffect(intent?.attemptNo, state.shouldLaunch) {
        if (state.shouldLaunch) intent?.let(::openUpi)
    }
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = state.pending,
        onBackCompleted = viewModel::requestLeave,
    )
    if (state.confirmLeave) {
        AlertDialog(
            onDismissRequest = viewModel::cancelLeave,
            title = { Text(stringResource(Res.string.pay_leave_title)) },
            text = { Text(stringResource(Res.string.pay_leave_body)) },
            confirmButton = {
                TextButton(onClick = { viewModel.cancelLeave(); onHome() }) {
                    Text(stringResource(Res.string.pay_leave_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelLeave) { Text(stringResource(Res.string.pay_leave_stay)) }
            },
        )
    }
    PansariScreen(
        title = stringResource(Res.string.pay_title),
        onBack = { if (state.pending) viewModel.requestLeave() else onHome() },
        error = state.error.toErrorBanner(),
        onErrorAction = { handleErrorBannerAction(it, onRetry = { viewModel.checkNow() }, onDismiss = viewModel::dismissError) },
        isLoading = intent == null && state.error == null,
    ) {
        if (intent == null) return@PansariScreen
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (intent.status) {
                PaymentIntentStatus.PENDING -> PendingBlock(intent, state, viewModel, onOpenUpi = { openUpi(intent) })
                PaymentIntentStatus.SUCCESS -> {
                    ResultHeader(
                        Icons.Filled.CheckCircle,
                        MaterialTheme.colorScheme.primary,
                        stringResource(Res.string.pay_success_title),
                        stringResource(Res.string.pay_success_body, intent.shopName),
                    )
                    PaymentDetailsCard(intent)
                    UserPrimaryButton(stringResource(Res.string.pay_view_order), onClick = { onViewOrder(intent.orderId) })
                }
                PaymentIntentStatus.FAILED -> {
                    ResultHeader(
                        Icons.Filled.Error,
                        MaterialTheme.colorScheme.error,
                        stringResource(Res.string.pay_failed_title),
                        failureReason(intent) + "\n" + stringResource(Res.string.pay_failed_body),
                    )
                    PaymentDetailsCard(intent)
                    UserPrimaryButton(
                        stringResource(Res.string.pay_retry),
                        onClick = { viewModel.retry(onRetry) },
                        enabled = !state.busy,
                    )
                    OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.pay_home))
                    }
                }
                PaymentIntentStatus.REFUNDED -> {
                    ResultHeader(
                        Icons.Filled.Warning,
                        MaterialTheme.colorScheme.error,
                        stringResource(Res.string.pay_refunded_title),
                        failureReason(intent) + "\n" + if (intent.isDirectUpi) {
                            stringResource(Res.string.pay_refunded_direct_body, intent.shopName)
                        } else {
                            stringResource(Res.string.pay_refunded_body)
                        },
                    )
                    PaymentDetailsCard(intent)
                    UserPrimaryButton(
                        stringResource(Res.string.pay_retry),
                        onClick = { viewModel.retry(onRetry) },
                        enabled = !state.busy,
                    )
                    OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(Res.string.pay_home))
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingBlock(
    intent: PaymentIntent,
    state: PaymentStatusUiState,
    viewModel: PaymentStatusViewModel,
    onOpenUpi: () -> Unit,
) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(Res.string.pay_waiting_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Text(
            if (intent.isDirectUpi) {
                stringResource(Res.string.pay_waiting_direct_body, intent.shopName)
            } else {
                stringResource(Res.string.pay_waiting_body)
            },
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            if (state.secondsLeft > 0) {
                stringResource(Res.string.pay_time_left, countdown(state.secondsLeft))
            } else {
                stringResource(Res.string.pay_time_up)
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Text(
            stringResource(Res.string.pay_do_not_close),
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(12.dp),
        )
    }
    PaymentDetailsCard(intent)
    if (intent.upiPayUri != null) {
        UserPrimaryButton(stringResource(Res.string.pay_open_upi), onClick = onOpenUpi, enabled = !state.busy)
    }
    if (intent.isDirectUpi) {
        val submitted = intent.utr
        if (submitted != null) {
            Text(stringResource(Res.string.order_pay_utr_submitted, submitted), style = MaterialTheme.typography.bodySmall)
        } else {
            Text(stringResource(Res.string.order_pay_utr_hint), style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(
                value = state.utr,
                onValueChange = viewModel::setUtr,
                label = { Text(stringResource(Res.string.order_pay_utr_label)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = viewModel::submitUtr,
                enabled = viewModel.canSubmitUtr(),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(Res.string.order_pay_utr_submit)) }
        }
    } else {
        OutlinedButton(
            onClick = viewModel::checkNow,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(Res.string.order_pay_check_status)) }
    }
    state.note?.let {
        Text(it.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun ResultHeader(icon: ImageVector, tint: androidx.compose.ui.graphics.Color, title: String, body: String) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(64.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        Text(body, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun PaymentDetailsCard(intent: PaymentIntent) {
    SectionCard(title = stringResource(Res.string.pay_details_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(Res.string.pay_detail_amount, intent.amountInr.asMoney()),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(stringResource(Res.string.pay_detail_shop, intent.shopName))
            intent.payeeUpi?.let { Text(stringResource(Res.string.pay_detail_upi, it)) }
            Text(
                stringResource(
                    Res.string.pay_detail_method,
                    stringResource(if (intent.isDirectUpi) Res.string.pay_method_direct else Res.string.pay_method_gateway),
                ),
            )
            Text(stringResource(Res.string.pay_detail_txn, intent.id), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(Res.string.pay_detail_order, intent.orderId), style = MaterialTheme.typography.bodySmall)
            Text(stringResource(Res.string.pay_detail_attempt, intent.attemptNo), style = MaterialTheme.typography.bodySmall)
            intent.utr?.let { Text(stringResource(Res.string.pay_detail_utr, it), style = MaterialTheme.typography.bodySmall) }
            intent.refundId?.let { Text(stringResource(Res.string.pay_detail_refund, it), style = MaterialTheme.typography.bodySmall) }
            intent.items.forEach { item ->
                Text(
                    stringResource(
                        Res.string.pay_detail_item,
                        item.quantity.asQuantity(),
                        listOfNotNull(item.productName, item.variantLabel).joinToString(" "),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun failureReason(intent: PaymentIntent): String = when (intent.failureReason) {
    AppConstants.Payment.REASON_SHOP_NOT_RECEIVED -> stringResource(Res.string.pay_reason_shop_not_received, intent.shopName)
    AppConstants.Payment.REASON_OUT_OF_STOCK -> stringResource(Res.string.pay_reason_out_of_stock)
    else -> stringResource(Res.string.pay_reason_not_received)
}

internal fun countdown(seconds: Long): String {
    val m = seconds / 60
    val s = (seconds % 60).toString().padStart(2, '0')
    return "$m:$s"
}
