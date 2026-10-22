package org.bhargav.pansariwala.feature.account

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import org.bhargav.pansariwala.api.AccountDeletionDto
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.api.toApiUiText
import org.bhargav.pansariwala.designsystem.AppBottomSheet
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.printing.formatPrintDateTime
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.closure_cancel
import pansariwala.shared.generated.resources.closure_confirm
import pansariwala.shared.generated.resources.closure_keep
import pansariwala.shared.generated.resources.closure_partner_point_login
import pansariwala.shared.generated.resources.closure_partner_point_offline
import pansariwala.shared.generated.resources.closure_partner_subtitle
import pansariwala.shared.generated.resources.closure_partner_title
import pansariwala.shared.generated.resources.closure_pending_body
import pansariwala.shared.generated.resources.closure_pending_title
import pansariwala.shared.generated.resources.closure_point_active
import pansariwala.shared.generated.resources.closure_point_undo
import pansariwala.shared.generated.resources.closure_sheet_intro
import pansariwala.shared.generated.resources.closure_sheet_title
import pansariwala.shared.generated.resources.closure_shop_point_hidden
import pansariwala.shared.generated.resources.closure_shop_point_logins
import pansariwala.shared.generated.resources.closure_shop_subtitle
import pansariwala.shared.generated.resources.closure_shop_title

enum class ClosableAccount { SHOP, PARTNER }

data class AccountClosureUiState(
    val scheduledAt: Long? = null,
    val loaded: Boolean = false,
    val busy: Boolean = false,
    val showSheet: Boolean = false,
    val error: UiText? = null,
)

class AccountClosureViewModel(
    private val account: ClosableAccount,
    private val api: PansariApi,
) : ViewModel() {
    private val partner = account == ClosableAccount.PARTNER
    private val _state = MutableStateFlow(AccountClosureUiState())
    val state: StateFlow<AccountClosureUiState> = _state.asStateFlow()

    init {
        execute { api.accountClosureStatus(partner) }
    }

    fun openSheet() = _state.update { it.copy(showSheet = true, error = null) }
    fun dismissSheet() = _state.update { it.copy(showSheet = false) }
    fun request() = execute { api.requestAccountClosure(partner) }
    fun cancel() = execute { api.cancelAccountClosure(partner) }

    private fun execute(call: suspend () -> AccountDeletionDto) {
        if (_state.value.busy) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                val result = call()
                _state.update { it.copy(scheduledAt = result.scheduledAt, loaded = true, busy = false, showSheet = false) }
            } catch (e: Exception) {
                e.rethrowIfStructuredCancellation()
                _state.update { it.copy(busy = false, error = e.toApiUiText()) }
            }
        }
    }
}

/** Delete-account card (or pending-closure card) for a shop admin or delivery partner. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountClosureSection(
    account: ClosableAccount,
    modifier: Modifier = Modifier,
    viewModel: AccountClosureViewModel = koinViewModel(key = "closure_${account.name}") { parametersOf(account) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    if (!state.loaded) return
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val scheduledAt = state.scheduledAt
        if (scheduledAt == null) {
            ClosureCard(account, viewModel::openSheet)
        } else {
            PendingClosureCard(scheduledAt, state.busy, viewModel::cancel)
        }
        state.error?.let {
            Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }
    }
    if (state.showSheet) {
        ClosureSheet(account, state.busy, state.error, onConfirm = viewModel::request, onDismiss = viewModel::dismissSheet)
    }
}

@Composable
private fun ClosureCard(account: ClosableAccount, onClick: () -> Unit) {
    val error = MaterialTheme.colorScheme.error
    val shop = account == ClosableAccount.SHOP
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        border = BorderStroke(1.dp, error.copy(alpha = 0.5f)),
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DeleteForever, null, tint = error)
            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                Text(
                    stringResource(if (shop) Res.string.closure_shop_title else Res.string.closure_partner_title),
                    color = error,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    stringResource(if (shop) Res.string.closure_shop_subtitle else Res.string.closure_partner_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = error)
        }
    }
}

@Composable
private fun PendingClosureCard(scheduledAt: Long, busy: Boolean, onKeep: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colors.errorContainer, contentColor = colors.onErrorContainer),
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WarningAmber, null, tint = colors.error)
                Text(
                    stringResource(Res.string.closure_pending_title),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 10.dp),
                )
            }
            Text(
                stringResource(Res.string.closure_pending_body, formatPrintDateTime(scheduledAt)),
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onKeep, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = colors.onPrimary, strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.closure_cancel), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClosureSheet(
    account: ClosableAccount,
    busy: Boolean,
    error: UiText?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                Modifier.align(Alignment.CenterHorizontally).size(64.dp).clip(CircleShape).background(colors.errorContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Default.DeleteForever, null, tint = colors.error, modifier = Modifier.size(34.dp))
            }
            Text(
                stringResource(Res.string.closure_sheet_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Text(stringResource(Res.string.closure_sheet_intro), style = MaterialTheme.typography.bodyMedium)
            if (account == ClosableAccount.SHOP) {
                ClosurePoint(stringResource(Res.string.closure_shop_point_hidden), colors.error)
                ClosurePoint(stringResource(Res.string.closure_shop_point_logins), colors.error)
            } else {
                ClosurePoint(stringResource(Res.string.closure_partner_point_offline), colors.error)
                ClosurePoint(stringResource(Res.string.closure_partner_point_login), colors.error)
            }
            ClosurePoint(stringResource(Res.string.closure_point_active), colors.error)
            ClosurePoint(stringResource(Res.string.closure_point_undo), colors.primary)
            error?.let { Text(it.asString(), color = colors.error, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.size(4.dp))
            Button(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(Res.string.closure_keep), fontWeight = FontWeight.SemiBold)
            }
            OutlinedButton(
                onClick = onConfirm,
                enabled = !busy,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, colors.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), color = colors.error, strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.closure_confirm), fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ClosurePoint(text: String, accent: Color) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.padding(top = 7.dp).size(6.dp).clip(CircleShape).background(accent))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
