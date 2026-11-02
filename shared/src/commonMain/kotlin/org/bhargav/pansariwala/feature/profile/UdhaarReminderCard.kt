package org.bhargav.pansariwala.feature.profile

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.api.UdhaarReminderDto
import org.bhargav.pansariwala.api.UdhaarReminderPatch
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.udhaar_auto_app
import pansariwala.shared.generated.resources.udhaar_auto_app_hint
import pansariwala.shared.generated.resources.udhaar_auto_every
import pansariwala.shared.generated.resources.udhaar_auto_every_days
import pansariwala.shared.generated.resources.udhaar_auto_hint
import pansariwala.shared.generated.resources.udhaar_auto_label
import pansariwala.shared.generated.resources.udhaar_auto_min_due
import pansariwala.shared.generated.resources.udhaar_auto_paid_badge
import pansariwala.shared.generated.resources.udhaar_auto_paid_hint
import pansariwala.shared.generated.resources.udhaar_auto_sms
import pansariwala.shared.generated.resources.udhaar_auto_title
import pansariwala.shared.generated.resources.udhaar_auto_whatsapp

/** Shop settings: automatic udhaar reminders (schedule, threshold, channels). */
@Composable
internal fun UdhaarReminderCard(
    settings: UdhaarReminderDto,
    canEdit: Boolean,
    onChange: (UdhaarReminderPatch) -> Unit,
) {
    SectionCard(title = stringResource(Res.string.udhaar_auto_title), modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ToggleRow(stringResource(Res.string.udhaar_auto_label), settings.enabled, canEdit) {
                onChange(UdhaarReminderPatch(enabled = it))
            }
            SettingHint(stringResource(Res.string.udhaar_auto_hint))
            if (!settings.enabled) return@Column
            HorizontalDivider()
            Text(stringResource(Res.string.udhaar_auto_every), style = MaterialTheme.typography.labelLarge)
            ChipRow(
                options = AppConstants.UDHAAR_REMIND_DAY_OPTIONS,
                selected = settings.everyDays,
                enabled = canEdit,
                label = { stringResource(Res.string.udhaar_auto_every_days, it) },
                onSelect = { onChange(UdhaarReminderPatch(everyDays = it)) },
            )
            Text(stringResource(Res.string.udhaar_auto_min_due), style = MaterialTheme.typography.labelLarge)
            ChipRow(
                options = AppConstants.UDHAAR_REMIND_MIN_OPTIONS,
                selected = settings.minDueInr,
                enabled = canEdit,
                label = { it.asMoney() },
                onSelect = { onChange(UdhaarReminderPatch(minDueInr = it)) },
            )
            HorizontalDivider()
            ChannelRow(Icons.Default.NotificationsActive, stringResource(Res.string.udhaar_auto_app), stringResource(Res.string.udhaar_auto_app_hint), paid = false) {
                Switch(checked = true, onCheckedChange = null, enabled = false)
            }
            ChannelRow(Icons.Default.Sms, stringResource(Res.string.udhaar_auto_sms), null, paid = true) {
                Switch(checked = settings.sms, onCheckedChange = { onChange(UdhaarReminderPatch(sms = it)) }, enabled = canEdit)
            }
            ChannelRow(Icons.AutoMirrored.Filled.Chat, stringResource(Res.string.udhaar_auto_whatsapp), null, paid = true) {
                Switch(checked = settings.whatsapp, onCheckedChange = { onChange(UdhaarReminderPatch(whatsapp = it)) }, enabled = canEdit)
            }
            SettingHint(stringResource(Res.string.udhaar_auto_paid_hint))
        }
    }
}

@Composable
private fun <T> ChipRow(
    options: List<T>,
    selected: T,
    enabled: Boolean,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
) {
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { option ->
            FilterChip(
                selected = option == selected,
                onClick = { if (option != selected) onSelect(option) },
                enabled = enabled,
                label = { Text(label(option)) },
            )
        }
    }
}

@Composable
internal fun ChannelRow(
    icon: ImageVector,
    title: String,
    hint: String?,
    paid: Boolean,
    toggle: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            color = if (paid) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(10.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (paid) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.padding(8.dp).size(20.dp),
            )
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold)
                if (paid) {
                    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(6.dp)) {
                        Text(
                            stringResource(Res.string.udhaar_auto_paid_badge),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                        )
                    }
                }
            }
            hint?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        toggle()
    }
}
