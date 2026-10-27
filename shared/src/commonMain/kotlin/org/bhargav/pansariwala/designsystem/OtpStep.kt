package org.bhargav.pansariwala.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.platform.OtpStatus
import org.bhargav.pansariwala.platform.OtpStatusCenter
import org.bhargav.pansariwala.platform.PhoneAuthGateway
import org.bhargav.pansariwala.platform.PhoneOtpSession
import org.bhargav.pansariwala.platform.mapPhoneAuthError
import org.bhargav.pansariwala.platform.otpRetryAfterSec
import org.bhargav.pansariwala.platform.sanitizeOtpInput
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.otp_sent_to

data class OtpStepState(
    val phone: String = "",
    val session: PhoneOtpSession? = null,
    val code: String = "",
    val resendInSec: Int = 0,
    val sending: Boolean = false,
    val verifying: Boolean = false,
    val error: UiText? = null,
) {
    val codeSent: Boolean get() = session != null
    val busy: Boolean get() = sending || verifying
}

/**
 * Send / resend / verify an OTP for one phone. The host ViewModel owns it and runs the protected action in
 * [verify]; the server consumes the code there (no separate OTP verify call).
 */
class OtpStepController(private val gateway: PhoneAuthGateway, private val scope: CoroutineScope) {
    private val _state = MutableStateFlow(OtpStepState())
    val state: StateFlow<OtpStepState> = _state.asStateFlow()
    private var countdown: Job? = null

    fun reset() {
        countdown?.cancel()
        _state.value = OtpStepState()
    }

    fun send(phone: String = _state.value.phone) {
        if (_state.value.busy || phone.isBlank()) return
        scope.launch {
            _state.update { it.copy(phone = phone, sending = true, error = null) }
            gateway.sendOtp(phone)
                .onSuccess { session ->
                    _state.update { it.copy(session = session, sending = false, code = session.devOtp.orEmpty()) }
                    startCountdown(session.resendAfterSec)
                }
                .onFailure { e ->
                    _state.update { it.copy(sending = false, error = mapPhoneAuthError(e, verifying = false)) }
                    e.otpRetryAfterSec()?.let(::startCountdown)
                }
        }
    }

    fun onCode(value: String) = _state.update { it.copy(code = sanitizeOtpInput(value), error = null) }

    fun setError(error: UiText?) = _state.update { it.copy(error = error, verifying = false) }

    /** Runs [action] with the entered code; on failure the mapped error stays on the step. */
    fun verify(action: suspend (phone: String, code: String, sessionId: String) -> Unit) {
        val s = _state.value
        val session = s.session ?: return
        if (s.busy || s.code.length != AppConstants.OTP_LENGTH) return
        scope.launch {
            _state.update { it.copy(verifying = true, error = null) }
            OtpStatusCenter.post(OtpStatus.Checking)
            runCatching { action(s.phone, s.code, session.sessionId) }
                .onSuccess {
                    OtpStatusCenter.post(OtpStatus.Verified)
                    reset()
                }
                .onFailure { e ->
                    e.rethrowIfStructuredCancellation()
                    val error = mapPhoneAuthError(e, verifying = true)
                    OtpStatusCenter.post(OtpStatus.VerifyFailed(error))
                    _state.update { it.copy(verifying = false, error = error) }
                }
        }
    }

    private fun startCountdown(seconds: Int) {
        countdown?.cancel()
        countdown = scope.launch {
            for (left in seconds downTo 0) {
                _state.update { it.copy(resendInSec = left) }
                if (left > 0) delay(AppConstants.OTP_COUNTDOWN_TICK_MS)
            }
        }
    }
}

/** Code entry + resend + submit for an [OtpStepController] that already sent a code. */
@Composable
fun OtpStepSection(
    state: OtpStepState,
    submitLabel: String,
    onCode: (String) -> Unit,
    onResend: () -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val masked = state.session?.maskedPhone?.ifBlank { null } ?: state.phone
        Text(stringResource(Res.string.otp_sent_to, masked), style = MaterialTheme.typography.bodyMedium)
        OtpCodeField(value = state.code, onValueChange = onCode, enabled = !state.verifying, onDone = onSubmit)
        ResendOtpButton(resendInSec = state.resendInSec, loading = state.sending, onClick = onResend)
        state.error?.let { Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
        Button(
            onClick = onSubmit,
            enabled = !state.busy && state.code.length == AppConstants.OTP_LENGTH,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.verifying) CircularProgressIndicator(strokeWidth = 2.dp) else Text(submitLabel)
        }
    }
}
