package org.bhargav.pansariwala.feature.delivery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AssignmentReturn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import org.bhargav.pansariwala.designsystem.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.domain.model.OrderStatus
import org.bhargav.pansariwala.platform.openExternalNavigation
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.partner_navigate
import pansariwala.shared.generated.resources.return_dialog_body
import pansariwala.shared.generated.resources.return_dialog_confirm
import pansariwala.shared.generated.resources.return_dialog_title
import pansariwala.shared.generated.resources.return_partner_accepted_body
import pansariwala.shared.generated.resources.return_partner_accepted_title
import pansariwala.shared.generated.resources.return_partner_done
import pansariwala.shared.generated.resources.return_partner_screen_title
import pansariwala.shared.generated.resources.return_partner_waiting_body
import pansariwala.shared.generated.resources.return_partner_waiting_title
import pansariwala.shared.generated.resources.return_reason_label
import pansariwala.shared.generated.resources.return_reason_not_responding
import pansariwala.shared.generated.resources.return_reason_other
import pansariwala.shared.generated.resources.return_reason_other_hint
import pansariwala.shared.generated.resources.return_reason_unreachable

private const val REASON_OTHER = "OTHER"

/** Asks why the parcel could not be handed over; [onConfirm] gets a reason code or the typed text. */
@Composable
fun ReturnToShopDialog(
    busy: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var choice by rememberSaveable { mutableStateOf(AppConstants.ReturnReason.CUSTOMER_UNREACHABLE) }
    var other by rememberSaveable { mutableStateOf("") }
    val options = listOf(
        AppConstants.ReturnReason.CUSTOMER_UNREACHABLE to stringResource(Res.string.return_reason_unreachable),
        AppConstants.ReturnReason.CUSTOMER_NOT_RESPONDING to stringResource(Res.string.return_reason_not_responding),
        REASON_OTHER to stringResource(Res.string.return_reason_other),
    )
    val reason = if (choice == REASON_OTHER) other.trim() else choice
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        icon = { Icon(Icons.AutoMirrored.Filled.AssignmentReturn, contentDescription = null) },
        title = { Text(stringResource(Res.string.return_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(Res.string.return_dialog_body), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.size(4.dp))
                options.forEach { (code, label) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = choice == code, role = Role.RadioButton, onClick = { choice = code })
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = choice == code, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                if (choice == REASON_OTHER) {
                    OutlinedTextField(
                        value = other,
                        onValueChange = { other = it.take(AppConstants.ReturnReason.MAX_LENGTH) },
                        placeholder = { Text(stringResource(Res.string.return_reason_other_hint)) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(reason) },
                enabled = !busy && reason.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.return_dialog_confirm))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(Res.string.action_cancel)) }
        },
    )
}

/** Localized text for a stored return reason; free text is shown as typed. */
@Composable
fun returnReasonLabel(reason: String?): String? = when (reason) {
    null, "" -> null
    AppConstants.ReturnReason.CUSTOMER_UNREACHABLE -> stringResource(Res.string.return_reason_unreachable)
    AppConstants.ReturnReason.CUSTOMER_NOT_RESPONDING -> stringResource(Res.string.return_reason_not_responding)
    else -> reason
}

/** Partner heads back to the shop and waits until the shop verifies the returned parcel. */
@Composable
fun PartnerReturnToShopScreen(
    orderId: String,
    onDone: () -> Unit,
    onBack: () -> Unit,
    viewModel: PartnerJobViewModel = koinViewModel(),
) {
    LaunchedEffect(orderId) { viewModel.load(orderId) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val order = state.order
    val accepted = order?.status == OrderStatus.RETURNED_TO_SHOP
    LaunchedEffect(order?.id, accepted) {
        if (order == null || accepted) return@LaunchedEffect
        while (true) {
            delay(AppConstants.ReturnReason.POLL_MS)
            viewModel.refresh()
        }
    }
    PansariScreen(
        title = stringResource(Res.string.return_partner_screen_title),
        onBack = onBack,
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = { viewModel.load(orderId) }, onDismiss = viewModel::dismissError)
        },
        isLoading = state.loading && order == null,
    ) {
        if (order == null) return@PansariScreen
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(
                color = if (accepted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        if (accepted) Icons.Filled.CheckCircle else Icons.Filled.Storefront,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = if (accepted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                    )
                    Text(
                        stringResource(if (accepted) Res.string.return_partner_accepted_title else Res.string.return_partner_waiting_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        stringResource(
                            if (accepted) Res.string.return_partner_accepted_body else Res.string.return_partner_waiting_body,
                            order.shopName.orEmpty(),
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    if (!accepted) CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                }
            }
            returnReasonLabel(order.cancelReason)?.let {
                Text(
                    stringResource(Res.string.return_reason_label, it),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            order.shopAddress?.takeIf { it.isNotBlank() }?.let {
                Text(it, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
            }
            if (accepted) {
                PartnerPrimaryButton(stringResource(Res.string.return_partner_done), onClick = onDone)
            } else {
                val lat = order.shopLat
                val lng = order.shopLng
                if (lat != null && lng != null) {
                    PartnerPrimaryButton(stringResource(Res.string.partner_navigate), onClick = { openExternalNavigation(lat, lng) })
                }
            }
        }
    }
}
