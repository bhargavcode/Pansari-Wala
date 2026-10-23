package org.bhargav.pansariwala.feature.udhaar

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.designsystem.AppBottomSheet
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.currency_symbol
import pansariwala.shared.generated.resources.udhaar_outstanding
import pansariwala.shared.generated.resources.udhaar_remind_amount
import pansariwala.shared.generated.resources.udhaar_remind_full
import pansariwala.shared.generated.resources.udhaar_remind_half
import pansariwala.shared.generated.resources.udhaar_remind_message
import pansariwala.shared.generated.resources.udhaar_remind_reset
import pansariwala.shared.generated.resources.udhaar_remind_sms
import pansariwala.shared.generated.resources.udhaar_remind_subtitle
import pansariwala.shared.generated.resources.udhaar_remind_title
import pansariwala.shared.generated.resources.udhaar_remind_tokens
import pansariwala.shared.generated.resources.udhaar_remind_whatsapp

private val WhatsAppGreen = Color(0xFF1DA851)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PayRequestSheet(
    form: PayRequestForm,
    borrowerName: String,
    due: Double,
    onChange: ((PayRequestForm) -> PayRequestForm) -> Unit,
    onSend: (PayRequestChannel) -> Unit,
    onDismiss: () -> Unit,
) {
    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    stringResource(Res.string.udhaar_remind_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    stringResource(Res.string.udhaar_remind_subtitle, borrowerName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "${stringResource(Res.string.udhaar_outstanding)}: ${due.asMoney()}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            OutlinedTextField(
                value = form.amount,
                onValueChange = { v -> onChange { it.copy(amount = v.filter { c -> c.isDigit() || c == '.' }) } },
                label = { Text(stringResource(Res.string.udhaar_remind_amount)) },
                prefix = { Text(stringResource(Res.string.currency_symbol)) },
                singleLine = true,
                isError = form.error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = { onChange { it.copy(amount = formatAmount(due)) } },
                    label = { Text(stringResource(Res.string.udhaar_remind_full)) },
                )
                AssistChip(
                    onClick = { onChange { it.copy(amount = formatAmount(due / 2)) } },
                    label = { Text(stringResource(Res.string.udhaar_remind_half)) },
                )
            }
            OutlinedTextField(
                value = form.template,
                onValueChange = { v -> onChange { it.copy(template = v) } },
                label = { Text(stringResource(Res.string.udhaar_remind_message)) },
                supportingText = { Text(stringResource(Res.string.udhaar_remind_tokens)) },
                minLines = 4,
                maxLines = 8,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            if (form.template.trim() != form.defaultTemplate.trim()) {
                TextButton(onClick = { onChange { it.copy(template = it.defaultTemplate) } }) {
                    Icon(Icons.Default.RestartAlt, null, Modifier.size(18.dp))
                    Text(stringResource(Res.string.udhaar_remind_reset), modifier = Modifier.padding(start = 6.dp))
                }
            }
            form.error?.let {
                Text(it.asString(), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SendButton(
                    label = stringResource(Res.string.udhaar_remind_whatsapp),
                    icon = { Icon(Icons.AutoMirrored.Filled.Chat, null, Modifier.size(20.dp)) },
                    container = WhatsAppGreen,
                    busy = form.busy,
                    onClick = { onSend(PayRequestChannel.WHATSAPP) },
                    modifier = Modifier.weight(1f),
                )
                SendButton(
                    label = stringResource(Res.string.udhaar_remind_sms),
                    icon = { Icon(Icons.Default.Sms, null, Modifier.size(20.dp)) },
                    container = MaterialTheme.colorScheme.primary,
                    busy = form.busy,
                    onClick = { onSend(PayRequestChannel.SMS) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SendButton(
    label: String,
    icon: @Composable () -> Unit,
    container: Color,
    busy: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !busy,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = Color.White),
        modifier = modifier.height(52.dp),
    ) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp, color = Color.White)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                icon()
                Text(label, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
