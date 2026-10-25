package org.bhargav.pansariwala.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import org.bhargav.pansariwala.api.ShopPartnerDto
import org.bhargav.pansariwala.api.ShopPartnerLedgerDto
import org.bhargav.pansariwala.api.ShopPartnerPayoutRequest
import org.bhargav.pansariwala.api.ShopPartnerRequest
import org.bhargav.pansariwala.api.ShopPartnerUpdateRequest
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.designsystem.AppAlertDialog
import org.bhargav.pansariwala.designsystem.AppBottomSheet
import org.bhargav.pansariwala.designsystem.OtpStepController
import org.bhargav.pansariwala.designsystem.OtpStepSection
import org.bhargav.pansariwala.designsystem.OutlinedTextField
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.domain.model.publicOrderNumber
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.platform.PhoneAuthGateway
import org.bhargav.pansariwala.printing.formatPrintDateTime
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.action_save
import pansariwala.shared.generated.resources.action_send_otp
import pansariwala.shared.generated.resources.error_login_failed
import pansariwala.shared.generated.resources.field_name
import pansariwala.shared.generated.resources.field_phone
import pansariwala.shared.generated.resources.field_vehicle_reg_optional
import pansariwala.shared.generated.resources.own_partner_add
import pansariwala.shared.generated.resources.rider_balance
import pansariwala.shared.generated.resources.rider_edit
import pansariwala.shared.generated.resources.rider_history
import pansariwala.shared.generated.resources.rider_ledger_earning
import pansariwala.shared.generated.resources.rider_ledger_empty
import pansariwala.shared.generated.resources.rider_ledger_payout
import pansariwala.shared.generated.resources.rider_ledger_returned
import pansariwala.shared.generated.resources.rider_ledger_title
import pansariwala.shared.generated.resources.rider_pay
import pansariwala.shared.generated.resources.rider_pay_amount
import pansariwala.shared.generated.resources.rider_pay_due
import pansariwala.shared.generated.resources.rider_pay_note
import pansariwala.shared.generated.resources.rider_pay_title
import pansariwala.shared.generated.resources.rider_rate_hint
import pansariwala.shared.generated.resources.rider_rate_label
import pansariwala.shared.generated.resources.rider_stats
import pansariwala.shared.generated.resources.rider_verify_add
import pansariwala.shared.generated.resources.riders_empty
import pansariwala.shared.generated.resources.riders_subtitle
import pansariwala.shared.generated.resources.riders_title

data class RiderDraft(
    val id: String? = null,
    val name: String = "",
    val phone: String = "",
    val vehicleReg: String = "",
    val rate: String = "",
) {
    val rateInr: Double? get() = rate.trim().ifBlank { "0" }.toDoubleOrNull()?.takeIf { it >= 0.0 }
    val valid: Boolean
        get() = name.trim().length >= 2 && rateInr != null &&
            (id != null || phone.count { it.isDigit() } >= AppConstants.PHONE_LOCAL_DIGITS)
}

data class RiderPayDraft(val rider: ShopPartnerDto, val amount: String = "", val note: String = "")

data class ShopRidersUiState(
    val riders: List<ShopPartnerDto> = emptyList(),
    val loading: Boolean = false,
    val busy: Boolean = false,
    val error: UiText? = null,
    val draft: RiderDraft? = null,
    val pay: RiderPayDraft? = null,
    val ledger: ShopPartnerLedgerDto? = null,
)

/** Shop's own riders: OTP-verified onboarding, fixed per-delivery pay, earnings and payouts. */
class ShopRidersViewModel(private val api: PansariApi, phoneAuth: PhoneAuthGateway) : ViewModel() {
    private val _uiState = MutableStateFlow(ShopRidersUiState())
    val uiState: StateFlow<ShopRidersUiState> = _uiState.asStateFlow()
    val otp = OtpStepController(phoneAuth, viewModelScope)

    fun load() = launchSafe(loading = true) {
        val riders = api.shopPartners()
        _uiState.update { it.copy(riders = riders) }
    }

    fun startAdd() {
        otp.reset()
        _uiState.update { it.copy(draft = RiderDraft(), error = null) }
    }

    fun startEdit(rider: ShopPartnerDto) {
        otp.reset()
        val rate = rider.payoutPerDeliveryInr.takeIf { it > 0.0 }?.toString().orEmpty()
        _uiState.update { it.copy(draft = RiderDraft(rider.id, rider.name, rider.phone, rider.vehicleReg, rate), error = null) }
    }

    fun updateDraft(transform: (RiderDraft) -> RiderDraft) =
        _uiState.update { s -> s.copy(draft = s.draft?.let(transform), error = null) }

    fun cancelDraft() {
        otp.reset()
        _uiState.update { it.copy(draft = null, error = null) }
    }

    /** Edits save directly; a new rider first gets an OTP on their mobile, then is added on verify. */
    fun saveDraft() {
        val draft = _uiState.value.draft?.takeIf { it.valid } ?: return
        val rate = draft.rateInr ?: return
        if (draft.id != null) {
            launchSafe {
                val saved = api.updateShopPartner(draft.id, ShopPartnerUpdateRequest(draft.name.trim(), draft.vehicleReg.trim(), rate))
                _uiState.update { s -> s.copy(riders = s.riders.map { if (it.id == saved.id) saved else it }, draft = null) }
            }
            return
        }
        if (!otp.state.value.codeSent) {
            otp.send(draft.phone.filter { it.isDigit() }.takeLast(AppConstants.PHONE_LOCAL_DIGITS))
            return
        }
        otp.verify { phone, code, sessionId ->
            val added = api.addShopPartner(
                ShopPartnerRequest(draft.name.trim(), phone, draft.vehicleReg.trim(), code, sessionId, rate),
            )
            _uiState.update { s -> s.copy(riders = (s.riders + added).sortedBy { it.name }, draft = null) }
        }
    }

    fun startPay(rider: ShopPartnerDto) = _uiState.update {
        it.copy(pay = RiderPayDraft(rider, amount = rider.balanceInr.takeIf { b -> b > 0.0 }?.toString().orEmpty()), error = null)
    }

    fun updatePay(transform: (RiderPayDraft) -> RiderPayDraft) =
        _uiState.update { s -> s.copy(pay = s.pay?.let(transform), error = null) }

    fun cancelPay() = _uiState.update { it.copy(pay = null, error = null) }

    fun confirmPay() {
        val pay = _uiState.value.pay ?: return
        val amount = pay.amount.toDoubleOrNull()?.takeIf { it > 0.0 } ?: return
        launchSafe {
            val saved = api.payShopPartner(pay.rider.id, ShopPartnerPayoutRequest(amount, pay.note.trim()))
            _uiState.update { s -> s.copy(riders = s.riders.map { if (it.id == saved.id) saved else it }, pay = null) }
        }
    }

    fun openLedger(rider: ShopPartnerDto) = launchSafe {
        val ledger = api.shopPartnerLedger(rider.id)
        _uiState.update { it.copy(ledger = ledger) }
    }

    fun closeLedger() = _uiState.update { it.copy(ledger = null) }

    private fun launchSafe(loading: Boolean = false, block: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(loading = loading || it.loading, busy = true, error = null) }
            runCatching { block() }.onFailure { e ->
                e.rethrowIfStructuredCancellation()
                val msg = e.message?.let { UiText.Plain(it) } ?: UiText.res(Res.string.error_login_failed)
                _uiState.update { it.copy(error = msg) }
            }
            _uiState.update { it.copy(loading = false, busy = false) }
        }
    }
}

fun LazyListScope.riderItems(shopId: String) {
    item(key = "shop_riders") { ShopRidersCard(shopId) }
}

@Composable
private fun ShopRidersCard(shopId: String) {
    val viewModel = koinViewModel<ShopRidersViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    SectionCard(
        title = stringResource(Res.string.riders_title),
        subtitle = stringResource(Res.string.riders_subtitle),
        modifier = Modifier.fillMaxWidth(),
        action = { Button(onClick = viewModel::startAdd) { Text(stringResource(Res.string.own_partner_add)) } },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (state.draft == null && state.pay == null) {
                state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
            if (state.riders.isEmpty() && !state.loading) {
                Text(stringResource(Res.string.riders_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            state.riders.forEachIndexed { index, rider ->
                if (index > 0) HorizontalDivider()
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(rider.name, fontWeight = FontWeight.SemiBold)
                    Text(
                        listOf(rider.phone, rider.vehicleReg).filter { it.isNotBlank() }.joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        stringResource(Res.string.rider_stats, rider.payoutPerDeliveryInr.asMoney(), rider.deliveries),
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Text(
                        stringResource(Res.string.rider_balance, rider.earnedInr.asMoney(), rider.paidInr.asMoney(), rider.balanceInr.asMoney()),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (rider.balanceInr > 0.0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(onClick = { viewModel.startEdit(rider) }) { Text(stringResource(Res.string.rider_edit)) }
                        TextButton(onClick = { viewModel.openLedger(rider) }) { Text(stringResource(Res.string.rider_history)) }
                        TextButton(onClick = { viewModel.startPay(rider) }, enabled = rider.balanceInr > 0.0) {
                            Text(stringResource(Res.string.rider_pay))
                        }
                    }
                }
            }
        }
    }
    state.draft?.let { RiderDraftSheet(it, state, viewModel) }
    state.pay?.let { RiderPayDialog(it, state, viewModel) }
    state.ledger?.let { RiderLedgerSheet(it, shopId, viewModel::closeLedger) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RiderDraftSheet(draft: RiderDraft, state: ShopRidersUiState, viewModel: ShopRidersViewModel) {
    val otp by viewModel.otp.state.collectAsStateWithLifecycle()
    val locked = otp.codeSent
    val isNew = draft.id == null
    AppBottomSheet(onDismissRequest = viewModel::cancelDraft) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                stringResource(if (isNew) Res.string.own_partner_add else Res.string.rider_edit),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = draft.phone,
                onValueChange = { v -> viewModel.updateDraft { it.copy(phone = v.filter(Char::isDigit).take(AppConstants.PHONE_LOCAL_DIGITS)) } },
                label = { Text(stringResource(Res.string.field_phone)) },
                singleLine = true,
                enabled = isNew && !locked,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = draft.name,
                onValueChange = { v -> viewModel.updateDraft { it.copy(name = v) } },
                label = { Text(stringResource(Res.string.field_name)) },
                singleLine = true,
                enabled = !locked,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = draft.vehicleReg,
                onValueChange = { v -> viewModel.updateDraft { it.copy(vehicleReg = v.uppercase()) } },
                label = { Text(stringResource(Res.string.field_vehicle_reg_optional)) },
                singleLine = true,
                enabled = !locked,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = draft.rate,
                onValueChange = { v -> viewModel.updateDraft { it.copy(rate = v.filter { c -> c.isDigit() || c == '.' }.take(7)) } },
                label = { Text(stringResource(Res.string.rider_rate_label)) },
                supportingText = { Text(stringResource(Res.string.rider_rate_hint)) },
                singleLine = true,
                enabled = !locked,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
            )
            if (locked) {
                OtpStepSection(
                    state = otp,
                    submitLabel = stringResource(Res.string.rider_verify_add),
                    onCode = viewModel.otp::onCode,
                    onResend = { viewModel.otp.send() },
                    onSubmit = viewModel::saveDraft,
                )
            } else {
                (otp.error ?: state.error)?.let {
                    Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
                Button(
                    onClick = viewModel::saveDraft,
                    enabled = draft.valid && !state.busy && !otp.busy,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(if (isNew) Res.string.action_send_otp else Res.string.action_save)) }
            }
            OutlinedButton(onClick = viewModel::cancelDraft, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.action_cancel))
            }
        }
    }
}

@Composable
private fun RiderPayDialog(pay: RiderPayDraft, state: ShopRidersUiState, viewModel: ShopRidersViewModel) {
    val amount = pay.amount.toDoubleOrNull()
    AppAlertDialog(
        onDismissRequest = viewModel::cancelPay,
        title = { Text(stringResource(Res.string.rider_pay_title, pay.rider.name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.rider_pay_due, pay.rider.balanceInr.asMoney()), style = MaterialTheme.typography.bodyMedium)
                OutlinedTextField(
                    value = pay.amount,
                    onValueChange = { v -> viewModel.updatePay { it.copy(amount = v.filter { c -> c.isDigit() || c == '.' }.take(9)) } },
                    label = { Text(stringResource(Res.string.rider_pay_amount)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = pay.note,
                    onValueChange = { v -> viewModel.updatePay { it.copy(note = v.take(200)) } },
                    label = { Text(stringResource(Res.string.rider_pay_note)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                onClick = viewModel::confirmPay,
                enabled = !state.busy && amount != null && amount > 0.0 && amount <= pay.rider.balanceInr + 0.001,
            ) { Text(stringResource(Res.string.rider_pay)) }
        },
        dismissButton = { TextButton(onClick = viewModel::cancelPay) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RiderLedgerSheet(ledger: ShopPartnerLedgerDto, shopId: String, onDismiss: () -> Unit) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(Res.string.rider_ledger_title, ledger.partner.name), style = MaterialTheme.typography.titleLarge)
            val p = ledger.partner
            Text(stringResource(Res.string.rider_balance, p.earnedInr.asMoney(), p.paidInr.asMoney(), p.balanceInr.asMoney()))
            if (ledger.entries.isEmpty()) {
                Text(stringResource(Res.string.rider_ledger_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                items(ledger.entries) { entry ->
                    val payout = entry.kind == LEDGER_PAYOUT
                    val number = entry.orderId?.let { publicOrderNumber(shopId, it) }.orEmpty()
                    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                when {
                                    payout -> stringResource(Res.string.rider_ledger_payout)
                                    entry.returned -> stringResource(Res.string.rider_ledger_returned, number)
                                    else -> stringResource(Res.string.rider_ledger_earning, number)
                                },
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                listOf(formatPrintDateTime(entry.atEpochMs), entry.note).filter { it.isNotBlank() }.joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            (if (payout) "−" else "+") + entry.amountInr.asMoney(),
                            color = if (payout) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    HorizontalDivider()
                }
            }
        }
    }
}

private const val LEDGER_PAYOUT = "PAYOUT"
