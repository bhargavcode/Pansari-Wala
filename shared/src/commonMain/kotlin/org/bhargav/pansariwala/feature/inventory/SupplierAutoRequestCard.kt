package org.bhargav.pansariwala.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.domain.model.SupplierAutoRequest
import org.bhargav.pansariwala.feature.profile.ChannelRow
import org.bhargav.pansariwala.feature.profile.SettingHint
import org.bhargav.pansariwala.feature.profile.ToggleRow
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.supply_auto_app
import pansariwala.shared.generated.resources.supply_auto_app_hint
import pansariwala.shared.generated.resources.supply_auto_hint
import pansariwala.shared.generated.resources.supply_auto_label
import pansariwala.shared.generated.resources.supply_auto_title
import pansariwala.shared.generated.resources.supply_auto_whatsapp
import pansariwala.shared.generated.resources.supply_auto_whatsapp_hint

/** Suppliers screen: POS controls for automatic restock requests. */
@Composable
internal fun SupplierAutoRequestCard(settings: SupplierAutoRequest, onChange: (SupplierAutoRequest) -> Unit) {
    SectionCard(title = stringResource(Res.string.supply_auto_title), modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ToggleRow(stringResource(Res.string.supply_auto_label), settings.enabled, enabled = true) {
                onChange(settings.copy(enabled = it))
            }
            SettingHint(stringResource(Res.string.supply_auto_hint))
            if (!settings.enabled) return@Column
            HorizontalDivider()
            ChannelRow(
                Icons.Default.NotificationsActive,
                stringResource(Res.string.supply_auto_app),
                stringResource(Res.string.supply_auto_app_hint),
                paid = false,
            ) {
                Switch(checked = settings.appNotify, onCheckedChange = { onChange(settings.copy(appNotify = it)) })
            }
            ChannelRow(
                Icons.AutoMirrored.Filled.Chat,
                stringResource(Res.string.supply_auto_whatsapp),
                stringResource(Res.string.supply_auto_whatsapp_hint),
                paid = true,
            ) {
                Switch(checked = settings.whatsapp, onCheckedChange = { onChange(settings.copy(whatsapp = it)) })
            }
        }
    }
}
