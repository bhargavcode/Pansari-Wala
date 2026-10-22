package org.bhargav.pansariwala.feature.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextOverflow
import pansariwala.shared.generated.resources.pay_time_left_label
import pansariwala.shared.generated.resources.pay_amount_to_pay
import pansariwala.shared.generated.resources.pay_method_direct_short
import pansariwala.shared.generated.resources.pay_method_gateway_short
import pansariwala.shared.generated.resources.pay_steps_title
import pansariwala.shared.generated.resources.pay_step_open
import pansariwala.shared.generated.resources.pay_step_pay
import pansariwala.shared.generated.resources.pay_step_return
import pansariwala.shared.generated.resources.pay_step_return_direct
import pansariwala.shared.generated.resources.pay_label_amount
import pansariwala.shared.generated.resources.pay_label_shop
import pansariwala.shared.generated.resources.pay_label_method
import pansariwala.shared.generated.resources.pay_label_upi
import pansariwala.shared.generated.resources.pay_label_txn
import pansariwala.shared.generated.resources.pay_label_order
import pansariwala.shared.generated.resources.pay_label_attempt
import pansariwala.shared.generated.resources.pay_label_utr
import pansariwala.shared.generated.resources.pay_label_refund
import pansariwala.shared.generated.resources.pay_label_items
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleResumeEffect
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
    LifecycleResumeEffect(viewModel) {
        viewModel.onResumed()
        onPauseOrDispose { }
    }
    // Survives activity recreation / process death while the user is in the UPI app or browser checkout.
    var autoOpenedAttempt by rememberSaveable(intentId) { mutableIntStateOf(0) }
    LaunchedEffect(intent?.attemptNo, state.shouldLaunch) {
        val target = intent ?: return@LaunchedEffect
        if (!state.shouldLaunch) return@LaunchedEffect
        if (target.attemptNo <= autoOpenedAttempt) {
            viewModel.onLaunched(target.attemptNo)
            return@LaunchedEffect
        }
        autoOpenedAttempt = target.attemptNo
        openUpi(target)
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
    val seconds = state.secondsLeft
    val total = maxOf(AppConstants.Payment.WINDOW_MAX_SECONDS, seconds).coerceAtLeast(1L)
    val urgent = seconds in 1..PAY_URGENT_SECONDS
    val ringColor = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier.padding(vertical = 24.dp, horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(132.dp)) {
                if (seconds > 0) {
                    CircularProgressIndicator(
                        progress = { seconds.toFloat() / total },
                        modifier = Modifier.fillMaxSize(),
                        color = ringColor,
                        strokeWidth = 8.dp,
                        trackColor = ringColor.copy(alpha = 0.15f),
                        strokeCap = StrokeCap.Round,
                    )
                } else {
                    CircularProgressIndicator(Modifier.fillMaxSize(), color = ringColor, strokeWidth = 8.dp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (seconds > 0) countdown(seconds) else "…",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = ringColor,
                    )
                    Text(stringResource(Res.string.pay_time_left_label), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(
                stringResource(Res.string.pay_waiting_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (seconds > 0) {
                    if (intent.isDirectUpi) {
                        stringResource(Res.string.pay_waiting_direct_body, intent.shopName)
                    } else {
                        stringResource(Res.string.pay_waiting_body)
                    }
                } else {
                    stringResource(Res.string.pay_time_up)
                },
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = CircleShape, modifier = Modifier.size(44.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Storefront, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(Res.string.pay_amount_to_pay), style = MaterialTheme.typography.labelMedium)
                Text(intent.amountInr.asMoney(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    intent.shopName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            StatusPill(
                stringResource(if (intent.isDirectUpi) Res.string.pay_method_direct_short else Res.string.pay_method_gateway_short),
                container = MaterialTheme.colorScheme.tertiaryContainer,
                content = MaterialTheme.colorScheme.onTertiaryContainer,
            )
        }
    }
    SectionCard(title = stringResource(Res.string.pay_steps_title)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PayStep(1, stringResource(Res.string.pay_step_open))
            PayStep(2, stringResource(Res.string.pay_step_pay, intent.amountInr.asMoney()))
            PayStep(3, stringResource(if (intent.isDirectUpi) Res.string.pay_step_return_direct else Res.string.pay_step_return))
        }
    }
    if (intent.upiPayUri != null) {
        UserPrimaryButton(stringResource(Res.string.pay_open_upi), onClick = onOpenUpi, enabled = !state.busy)
    }
    if (intent.isDirectUpi) {
        val submitted = intent.utr
        SectionCard(title = stringResource(Res.string.order_pay_utr_label)) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (submitted != null) {
                    Text(stringResource(Res.string.order_pay_utr_submitted, submitted), style = MaterialTheme.typography.bodyMedium)
                } else {
                    Text(
                        stringResource(Res.string.order_pay_utr_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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
            }
        }
    } else {
        OutlinedButton(
            onClick = viewModel::checkNow,
            enabled = !state.busy,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
        ) { Text(stringResource(Res.string.order_pay_check_status)) }
    }
    state.note?.let {
        Text(it.asString(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
    }
    PaymentDetailsCard(intent, collapsible = true)
    Surface(
        color = PAY_NOTICE_CONTAINER,
        contentColor = PAY_NOTICE_CONTENT,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Info, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(Res.string.pay_do_not_close), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PayStep(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(color = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, shape = CircleShape) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                Text(number.toString(), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
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
private fun PaymentDetailsCard(intent: PaymentIntent, collapsible: Boolean = false) {
    var expanded by rememberSaveable(intent.id) { mutableStateOf(!collapsible) }
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                Modifier.fillMaxWidth().then(if (collapsible) Modifier.clickable { expanded = !expanded } else Modifier),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.ReceiptLong, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(Res.string.pay_details_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                if (collapsible) {
                    Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, null)
                }
            }
            if (!expanded) return@Column
            HorizontalDivider()
            LabelValueRow(stringResource(Res.string.pay_label_amount), intent.amountInr.asMoney(), emphasize = true)
            LabelValueRow(stringResource(Res.string.pay_label_shop), intent.shopName)
            LabelValueRow(
                stringResource(Res.string.pay_label_method),
                stringResource(if (intent.isDirectUpi) Res.string.pay_method_direct else Res.string.pay_method_gateway),
            )
            intent.payeeUpi?.let { LabelValueRow(stringResource(Res.string.pay_label_upi), it) }
            LabelValueRow(stringResource(Res.string.pay_label_txn), intent.id)
            LabelValueRow(stringResource(Res.string.pay_label_order), intent.orderId)
            if (intent.attemptNo > 1) LabelValueRow(stringResource(Res.string.pay_label_attempt), intent.attemptNo.toString())
            intent.utr?.let { LabelValueRow(stringResource(Res.string.pay_label_utr), it) }
            intent.refundId?.let { LabelValueRow(stringResource(Res.string.pay_label_refund), it) }
            if (intent.items.isNotEmpty()) {
                HorizontalDivider()
                Text(
                    stringResource(Res.string.pay_label_items, intent.items.size),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                intent.items.forEach { item ->
                    LabelValueRow(
                        listOfNotNull(item.productName, item.variantLabel).joinToString(" "),
                        "× ${item.quantity.asQuantity()}",
                    )
                }
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

private const val PAY_URGENT_SECONDS = 60L
private val PAY_NOTICE_CONTAINER = Color(0xFFFFF8E1)
private val PAY_NOTICE_CONTENT = Color(0xFF7A5200)

internal fun countdown(seconds: Long): String {
    val m = seconds / 60
    val s = (seconds % 60).toString().padStart(2, '0')
    return "$m:$s"
}
