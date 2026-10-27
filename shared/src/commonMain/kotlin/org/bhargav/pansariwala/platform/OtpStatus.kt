package org.bhargav.pansariwala.platform

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_close
import pansariwala.shared.generated.resources.otp_status_cancelled
import pansariwala.shared.generated.resources.otp_status_checking
import pansariwala.shared.generated.resources.otp_status_failed_title
import pansariwala.shared.generated.resources.otp_status_sending
import pansariwala.shared.generated.resources.otp_status_sent_body
import pansariwala.shared.generated.resources.otp_status_sent_title
import pansariwala.shared.generated.resources.otp_status_to
import pansariwala.shared.generated.resources.otp_status_verified
import pansariwala.shared.generated.resources.otp_status_verify_failed_title
import pansariwala.shared.generated.resources.otp_status_verifying_human

/** Progress of the latest OTP request/verification, shown app-wide by [OtpStatusHost]. */
sealed interface OtpStatus {
    data class VerifyingHuman(val maskedPhone: String) : OtpStatus
    data class Sending(val maskedPhone: String) : OtpStatus
    data class Sent(val maskedPhone: String, val validForMin: Int) : OtpStatus
    data class SendFailed(val reason: UiText?) : OtpStatus
    data object Cancelled : OtpStatus
    data object Checking : OtpStatus
    data object Verified : OtpStatus
    data class VerifyFailed(val reason: UiText?) : OtpStatus

    val inProgress: Boolean get() = this is VerifyingHuman || this is Sending || this is Checking
}

object OtpStatusCenter {
    private val _status = MutableStateFlow<OtpStatus?>(null)
    val status: StateFlow<OtpStatus?> = _status.asStateFlow()

    fun post(status: OtpStatus) { _status.value = status }
    fun clear() { _status.value = null }
}

fun maskPhoneForStatus(digits: String): String =
    if (digits.length >= MASK_VISIBLE_DIGITS) "******" + digits.takeLast(MASK_VISIBLE_DIGITS) else digits

private const val MASK_VISIBLE_DIGITS = 4

/** Top status card for OTP progress; finished states auto-hide, errors stay a little longer. */
@Composable
fun OtpStatusHost() {
    val status by OtpStatusCenter.status.collectAsState()
    var shown by remember { mutableStateOf<OtpStatus?>(null) }
    LaunchedEffect(status) {
        val s = status ?: return@LaunchedEffect run { shown = null }
        shown = s
        if (s.inProgress) return@LaunchedEffect
        val failed = s is OtpStatus.SendFailed || s is OtpStatus.VerifyFailed
        delay(if (failed) AppConstants.OTP_STATUS_ERROR_MS else AppConstants.OTP_STATUS_INFO_MS)
        OtpStatusCenter.clear()
    }
    Box(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 12.dp, vertical = 8.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = status != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
        ) {
            shown?.let { OtpStatusCard(it) }
        }
    }
}

@Composable
private fun OtpStatusCard(status: OtpStatus) {
    val colors = MaterialTheme.colorScheme
    val (title, body) = when (status) {
        is OtpStatus.VerifyingHuman -> stringResource(Res.string.otp_status_verifying_human) to stringResource(Res.string.otp_status_to, status.maskedPhone)
        is OtpStatus.Sending -> stringResource(Res.string.otp_status_sending) to stringResource(Res.string.otp_status_to, status.maskedPhone)
        is OtpStatus.Sent -> stringResource(Res.string.otp_status_sent_title) to stringResource(Res.string.otp_status_sent_body, status.maskedPhone, status.validForMin)
        is OtpStatus.SendFailed -> stringResource(Res.string.otp_status_failed_title) to status.reason?.asString()
        OtpStatus.Cancelled -> stringResource(Res.string.otp_status_cancelled) to null
        OtpStatus.Checking -> stringResource(Res.string.otp_status_checking) to null
        OtpStatus.Verified -> stringResource(Res.string.otp_status_verified) to null
        is OtpStatus.VerifyFailed -> stringResource(Res.string.otp_status_verify_failed_title) to status.reason?.asString()
    }
    val failed = status is OtpStatus.SendFailed || status is OtpStatus.VerifyFailed
    val success = status is OtpStatus.Sent || status is OtpStatus.Verified
    val container = when {
        failed -> colors.errorContainer
        success -> colors.primaryContainer
        else -> colors.surfaceContainerHigh
    }
    val content = when {
        failed -> colors.onErrorContainer
        success -> colors.onPrimaryContainer
        else -> colors.onSurface
    }
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 6.dp,
        modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Row(Modifier.padding(start = 14.dp, top = 10.dp, bottom = 10.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            when {
                status.inProgress -> CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = content)
                failed -> Icon(Icons.Default.ErrorOutline, null, tint = content)
                success -> Icon(Icons.Default.CheckCircle, null, tint = content)
                else -> Icon(Icons.Default.Info, null, tint = content)
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                body?.takeIf { it.isNotBlank() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.85f))
                }
            }
            IconButton(onClick = OtpStatusCenter::clear) {
                Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.action_close), tint = content)
            }
        }
    }
}
