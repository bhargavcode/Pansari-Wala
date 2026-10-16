package org.bhargav.pansariwala.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_resend_otp
import pansariwala.shared.generated.resources.action_resend_otp_in
import pansariwala.shared.generated.resources.field_otp

/** Visible, digits-only OTP entry; submits on the keyboard's Done once the code is complete. */
@Composable
fun OtpCodeField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onDone: () -> Unit = {},
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(Res.string.field_otp)) },
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        shape = RoundedCornerShape(14.dp),
        textStyle = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 8.sp,
            textAlign = TextAlign.Center,
        ),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { if (value.length == AppConstants.OTP_LENGTH) onDone() }),
    )
}

@Composable
fun ResendOtpButton(resendInSec: Int, loading: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    PansariLinkButton(
        text = if (resendInSec > 0) {
            stringResource(Res.string.action_resend_otp_in, resendInSec)
        } else {
            stringResource(Res.string.action_resend_otp)
        },
        onClick = onClick,
        enabled = !loading && resendInSec <= 0,
        modifier = modifier,
    )
}
