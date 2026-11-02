package org.bhargav.pansariwala.feature.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.checkout_fulfil_delivery
import pansariwala.shared.generated.resources.checkout_fulfil_pickup
import pansariwala.shared.generated.resources.checkout_pay_at_collect
import pansariwala.shared.generated.resources.checkout_pay_at_collect_hint
import pansariwala.shared.generated.resources.notify_ready_pickup_body
import pansariwala.shared.generated.resources.notify_ready_pickup_title
import org.jetbrains.compose.resources.stringResource

/** Delivery vs collect-at-shop choice; shown only when the shop delivers and allows self-collection. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FulfilmentSelector(pickup: Boolean, enabled: Boolean, onPickupChange: (Boolean) -> Unit) {
    val options = listOf(
        false to (Icons.Default.LocalShipping to Res.string.checkout_fulfil_delivery),
        true to (Icons.Default.Storefront to Res.string.checkout_fulfil_pickup),
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (value, meta) ->
            SegmentedButton(
                selected = pickup == value,
                onClick = { onPickupChange(value) },
                enabled = enabled,
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                icon = { Icon(meta.first, contentDescription = null, modifier = Modifier.size(18.dp)) },
            ) { Text(stringResource(meta.second)) }
        }
    }
}

/** Customer order detail: shop packed a self-collection order; come with the OTP. */
@Composable
internal fun ReadyToCollectBanner(shopName: String) {
    Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Default.Storefront,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(32.dp),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.notify_ready_pickup_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Text(
                    stringResource(Res.string.notify_ready_pickup_body, shopName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

/** "Pay at the counter" switch for self-collection orders when the shop allows it. */
@Composable
internal fun PayAtCollectRow(checked: Boolean, enabled: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.checkout_pay_at_collect),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
                Text(
                    stringResource(Res.string.checkout_pay_at_collect_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange, enabled = enabled)
        }
    }
}
