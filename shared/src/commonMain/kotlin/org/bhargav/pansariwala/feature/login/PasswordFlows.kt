package org.bhargav.pansariwala.feature.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
import org.bhargav.pansariwala.designsystem.AppAlertDialog
import org.bhargav.pansariwala.designsystem.AppBottomSheet
import org.bhargav.pansariwala.designsystem.OtpStepController
import org.bhargav.pansariwala.designsystem.OtpStepSection
import org.bhargav.pansariwala.designsystem.OutlinedTextField
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.platform.PhoneAuthGateway
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.action_save
import pansariwala.shared.generated.resources.action_send_otp
import pansariwala.shared.generated.resources.error_login_failed
import pansariwala.shared.generated.resources.login_username
import pansariwala.shared.generated.resources.pwd_change_title
import pansariwala.shared.generated.resources.pwd_changed
import pansariwala.shared.generated.resources.pwd_current_label
import pansariwala.shared.generated.resources.pwd_mobile_label
import pansariwala.shared.generated.resources.pwd_new_label
import pansariwala.shared.generated.resources.pwd_reset_done
import pansariwala.shared.generated.resources.pwd_reset_hint
import pansariwala.shared.generated.resources.pwd_reset_submit
import pansariwala.shared.generated.resources.pwd_reset_title
import pansariwala.shared.generated.resources.pwd_too_short

data class ForgotPasswordUiState(
    val username: String = "",
    val phone: String = "",
    val newPassword: String = "",
    val done: Boolean = false,
)

/** POS forgot password: OTP to the owner's (or the login's own) mobile, then a new password. */
class ForgotPasswordViewModel(private val api: PansariApi, phoneAuth: PhoneAuthGateway) : ViewModel() {
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()
    val otp = OtpStepController(phoneAuth, viewModelScope)

    fun start(username: String) {
        otp.reset()
        _uiState.value = ForgotPasswordUiState(username = username.trim())
    }

    fun onUsername(value: String) = _uiState.update { it.copy(username = value) }
    fun onPhone(value: String) = _uiState.update { it.copy(phone = value.filter(Char::isDigit).take(AppConstants.PHONE_LOCAL_DIGITS)) }
    fun onNewPassword(value: String) = _uiState.update { it.copy(newPassword = value) }

    fun sendOtp() {
        val s = _uiState.value
        if (s.username.isBlank() || s.phone.length != AppConstants.PHONE_LOCAL_DIGITS) return
        if (s.newPassword.trim().length < AppConstants.MIN_SHOP_PASSWORD) {
            return otp.setError(UiText.res(Res.string.pwd_too_short, AppConstants.MIN_SHOP_PASSWORD))
        }
        otp.send(s.phone)
    }

    fun submit() {
        val s = _uiState.value
        otp.verify { phone, code, sessionId ->
            api.resetShopPassword(s.username.trim(), phone, code, sessionId, s.newPassword.trim())
            _uiState.update { it.copy(done = true) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordSheet(initialUsername: String, onDismiss: () -> Unit, onDone: (UiText) -> Unit) {
    val viewModel = koinViewModel<ForgotPasswordViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val otp by viewModel.otp.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.start(initialUsername) }
    LaunchedEffect(state.done) { if (state.done) onDone(UiText.res(Res.string.pwd_reset_done)) }
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(stringResource(Res.string.pwd_reset_title), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(Res.string.pwd_reset_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val locked = otp.codeSent
            OutlinedTextField(
                value = state.username,
                onValueChange = viewModel::onUsername,
                label = { Text(stringResource(Res.string.login_username)) },
                singleLine = true,
                enabled = !locked,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.phone,
                onValueChange = viewModel::onPhone,
                label = { Text(stringResource(Res.string.pwd_mobile_label)) },
                singleLine = true,
                enabled = !locked,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.newPassword,
                onValueChange = viewModel::onNewPassword,
                label = { Text(stringResource(Res.string.pwd_new_label)) },
                singleLine = true,
                enabled = !locked,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
            )
            if (locked) {
                OtpStepSection(
                    state = otp,
                    submitLabel = stringResource(Res.string.pwd_reset_submit),
                    onCode = viewModel.otp::onCode,
                    onResend = viewModel::sendOtp,
                    onSubmit = viewModel::submit,
                )
            } else {
                otp.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                Button(
                    onClick = viewModel::sendOtp,
                    enabled = !otp.busy && state.username.isNotBlank() && state.phone.length == AppConstants.PHONE_LOCAL_DIGITS,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.action_send_otp)) }
            }
        }
    }
}

data class ChangePasswordUiState(
    val current: String = "",
    val next: String = "",
    val saving: Boolean = false,
    val error: UiText? = null,
    val done: Boolean = false,
)

class ChangePasswordViewModel(private val api: PansariApi) : ViewModel() {
    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    fun reset() { _uiState.value = ChangePasswordUiState() }
    fun onCurrent(value: String) = _uiState.update { it.copy(current = value, error = null) }
    fun onNext(value: String) = _uiState.update { it.copy(next = value, error = null) }

    fun save() {
        val s = _uiState.value
        if (s.saving || s.current.isBlank()) return
        if (s.next.trim().length < AppConstants.MIN_SHOP_PASSWORD) {
            _uiState.update { it.copy(error = UiText.res(Res.string.pwd_too_short, AppConstants.MIN_SHOP_PASSWORD)) }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true, error = null) }
            runCatching { api.changeShopPassword(s.current.trim(), s.next.trim()) }
                .onSuccess { _uiState.update { it.copy(saving = false, done = true) } }
                .onFailure { e ->
                    e.rethrowIfStructuredCancellation()
                    val msg = e.message?.let { UiText.Plain(it) } ?: UiText.res(Res.string.error_login_failed)
                    _uiState.update { it.copy(saving = false, error = msg) }
                }
        }
    }
}

@Composable
fun ChangePasswordDialog(onDismiss: () -> Unit, onDone: (UiText) -> Unit) {
    val viewModel = koinViewModel<ChangePasswordViewModel>()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.reset() }
    LaunchedEffect(state.done) { if (state.done) onDone(UiText.res(Res.string.pwd_changed)) }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.pwd_change_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.current,
                    onValueChange = viewModel::onCurrent,
                    label = { Text(stringResource(Res.string.pwd_current_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = state.next,
                    onValueChange = viewModel::onNext,
                    label = { Text(stringResource(Res.string.pwd_new_label)) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(onClick = viewModel::save, enabled = !state.saving) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
