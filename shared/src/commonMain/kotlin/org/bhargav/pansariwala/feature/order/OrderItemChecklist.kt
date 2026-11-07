package org.bhargav.pansariwala.feature.order

import androidx.compose.foundation.layout.Arrangement
import pansariwala.shared.generated.resources.order_items_show_less
import pansariwala.shared.generated.resources.order_items_show_all
import org.bhargav.pansariwala.i18n.localizedLabel
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import pansariwala.shared.generated.resources.order_item_in_stock
import pansariwala.shared.generated.resources.order_item_out_of_stock
import pansariwala.shared.generated.resources.order_line_added
import pansariwala.shared.generated.resources.order_line_by
import pansariwala.shared.generated.resources.order_line_replaced
import pansariwala.shared.generated.resources.order_line_returned
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
import org.bhargav.pansariwala.domain.model.ProductUnit
import org.bhargav.pansariwala.util.AppConstants
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
    OrderItemRow(item, modifier, showPrice = showPrice, struck = struck)
}

/** One order line: name (+ option) over "qty unit × price", line total right-aligned. */
@Composable
fun OrderItemRow(
    item: OrderItem,
    modifier: Modifier = Modifier,
    showPrice: Boolean = true,
    struck: Boolean = false,
    /** Non-null shows in-stock / out-of-stock text above the price. */
    inStock: Boolean? = null,
) {
    val decoration = if (struck) TextDecoration.LineThrough else null
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = buildString {
                    append(item.productName)
                    item.variantLabel?.takeIf(String::isNotBlank)?.let { append(" (").append(it).append(")") }
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = decoration,
                color = if (struck) muted else MaterialTheme.colorScheme.onSurface,
            )
            LineChangeChip(item)
            if (inStock != null) {
                StockChip(inStock)
                Spacer(Modifier.size(4.dp))
            }
            Text(
                text = buildString {
                    append(item.quantity.asQuantity()).append(' ').append(item.unit.localizedLabel())
                    if (showPrice) append(" × ").append(item.unitPrice.asMoney())
                },
                style = MaterialTheme.typography.bodySmall,
                color = muted,
                textDecoration = decoration,
            )
        }
        if (showPrice) {
            Text(
                item.lineTotal.asMoney(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                textDecoration = decoration,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

/** Readable item list; long orders collapse to [collapsedCount] rows with a show-all toggle. */
@Composable
fun OrderItemsTable(
    items: List<OrderItem>,
    modifier: Modifier = Modifier,
    collapsedCount: Int = Int.MAX_VALUE,
    stockOf: ((OrderItem) -> Boolean?)? = null,
) {
    var expanded by remember(items) { mutableStateOf(false) }
    val visible = if (expanded || items.size <= collapsedCount) items else items.take(collapsedCount)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        visible.forEachIndexed { index, item ->
            OrderItemRow(item, inStock = stockOf?.invoke(item))
            if (index < visible.lastIndex) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        }
        if (items.size > collapsedCount) {
            TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(horizontal = 0.dp)) {
                Text(
                    if (expanded) stringResource(Res.string.order_items_show_less)
                    else stringResource(Res.string.order_items_show_all, items.size),
                )
            }
        }
    }
}

@Composable
private fun StockChip(inStock: Boolean) {
    val color = if (inStock) PackedGreen else Color(0xFFC62828)
    Surface(color = color.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp)) {
        Text(
            stringResource(if (inStock) Res.string.order_item_in_stock else Res.string.order_item_out_of_stock),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
        )
    }
}

@Composable
internal fun LineChangeChip(item: OrderItem) {
    val change = item.lineChange ?: return
    val label = when (change) {
        AppConstants.OrderLine.RETURNED -> stringResource(
            Res.string.order_line_returned,
            "${item.changeQty.asQuantity()} ${item.unit.localizedLabel()}",
        )
        AppConstants.OrderLine.REPLACED -> stringResource(Res.string.order_line_replaced)
        else -> stringResource(Res.string.order_line_added)
    }
    val who = item.changedByName?.takeIf { it.isNotBlank() }?.let { stringResource(Res.string.order_line_by, it) }
    Surface(color = MaterialTheme.colorScheme.secondaryContainer, shape = RoundedCornerShape(8.dp)) {
        Text(
            listOfNotNull(label, who).joinToString(" · "),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    }
}
