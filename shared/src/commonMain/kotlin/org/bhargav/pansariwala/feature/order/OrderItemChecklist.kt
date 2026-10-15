package org.bhargav.pansariwala.feature.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.domain.model.Order
import org.bhargav.pansariwala.domain.model.OrderItem
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_clear_all
import pansariwala.shared.generated.resources.action_select_all
import pansariwala.shared.generated.resources.delivering_items_title
import pansariwala.shared.generated.resources.item_not_included
import pansariwala.shared.generated.resources.packing_checklist_hint
import pansariwala.shared.generated.resources.packing_checklist_progress
import pansariwala.shared.generated.resources.packing_checklist_title

val PackedGreen = Color(0xFF2E7D32)

/** POS packing checklist: shop ticks each item as it goes into the delivery packet. */
@Composable
fun PackingChecklist(
    order: Order,
    checked: Set<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit,
    onSetAll: (Boolean) -> Unit,
) {
    val total = order.items.size
    val done = order.items.count { it.productId in checked }
    val allDone = total > 0 && done == total
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(Res.string.packing_checklist_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { onSetAll(!allDone) }, enabled = enabled) {
                Text(stringResource(if (allDone) Res.string.action_clear_all else Res.string.action_select_all))
            }
        }
        LinearProgressIndicator(
            progress = { if (total == 0) 0f else done.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
            color = PackedGreen,
        )
        Text(
            stringResource(Res.string.packing_checklist_progress, done, total),
            style = MaterialTheme.typography.labelMedium,
            color = if (allDone) PackedGreen else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        order.items.forEach { item ->
            val isChecked = item.productId in checked
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = isChecked,
                        enabled = enabled,
                        role = Role.Checkbox,
                        onValueChange = { onToggle(item.productId) },
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(checked = isChecked, onCheckedChange = null, enabled = enabled)
                ItemLine(item, Modifier.padding(start = 8.dp).weight(1f))
            }
        }
        if (done == 0) {
            Text(
                stringResource(Res.string.packing_checklist_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * Items handed to the delivery partner, each with a green check. Items the shop left out of the
 * packet are shown struck through so customer and partner see exactly what is coming.
 */
@Composable
fun DeliveringItemsList(order: Order, showPrices: Boolean = true) {
    val packed = order.deliveringItems.mapTo(HashSet()) { it.productId }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            stringResource(Res.string.delivering_items_title, packed.size),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        order.items.forEach { item ->
            val included = item.productId in packed
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (included) Icons.Filled.CheckCircle else Icons.Outlined.RemoveCircleOutline,
                    contentDescription = null,
                    tint = if (included) PackedGreen else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp),
                )
                Column(Modifier.padding(start = 8.dp).weight(1f)) {
                    ItemLine(item, showPrice = showPrices, struck = !included)
                    if (!included) {
                        Text(
                            stringResource(Res.string.item_not_included),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ItemLine(
    item: OrderItem,
    modifier: Modifier = Modifier,
    showPrice: Boolean = true,
    struck: Boolean = false,
) {
    val text = buildString {
        append(item.productName).append(" × ").append(item.quantity.asQuantity())
        if (showPrice) append(" · ").append(item.lineTotal.asMoney())
    }
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.bodyMedium,
        textDecoration = if (struck) TextDecoration.LineThrough else null,
        color = if (struck) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
    )
}
