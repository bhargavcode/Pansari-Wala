package org.bhargav.pansariwala.feature.order

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RemoveShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import org.bhargav.pansariwala.designsystem.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.designsystem.AppAlertDialog
import org.bhargav.pansariwala.designsystem.AppBottomSheet
import org.bhargav.pansariwala.feature.user.LabelValueRow
import org.bhargav.pansariwala.feature.user.OfferPrice
import org.bhargav.pansariwala.feature.user.PurchaseOption
import org.bhargav.pansariwala.feature.user.StatusPill
import org.bhargav.pansariwala.feature.user.displayName
import org.bhargav.pansariwala.feature.user.options
import org.bhargav.pansariwala.i18n.localizedLabel
import org.bhargav.pansariwala.i18n.localizedBrand
import org.bhargav.pansariwala.i18n.localizedName
import org.bhargav.pansariwala.media.NetworkImage
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_add
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.action_save
import pansariwala.shared.generated.resources.field_barcode
import pansariwala.shared.generated.resources.field_category
import pansariwala.shared.generated.resources.pos_info_brand
import pansariwala.shared.generated.resources.pos_info_in_cart
import pansariwala.shared.generated.resources.pos_info_low_stock
import pansariwala.shared.generated.resources.pos_info_mrp
import pansariwala.shared.generated.resources.pos_info_out_of_stock
import pansariwala.shared.generated.resources.pos_info_price
import pansariwala.shared.generated.resources.pos_info_sku
import pansariwala.shared.generated.resources.pos_info_stock
import pansariwala.shared.generated.resources.pos_info_unit
import pansariwala.shared.generated.resources.pos_info_variants
import pansariwala.shared.generated.resources.pos_line_price_catalog
import pansariwala.shared.generated.resources.pos_line_price_field
import pansariwala.shared.generated.resources.pos_line_price_hint
import pansariwala.shared.generated.resources.pos_line_price_reset
import pansariwala.shared.generated.resources.pos_line_price_title
import pansariwala.shared.generated.resources.unbilled_sale_action
import pansariwala.shared.generated.resources.unbilled_sale_body
import pansariwala.shared.generated.resources.unbilled_sale_qty
import pansariwala.shared.generated.resources.unbilled_sale_stock
import pansariwala.shared.generated.resources.unbilled_sale_title

/** Full product details from the POS (tap on a product image). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosProductInfoSheet(
    option: PurchaseOption,
    inCartQty: Double,
    onAdd: () -> Unit,
    onRecordUnbilled: () -> Unit,
    onDismiss: () -> Unit,
) {
    val product = option.product
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    AppBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            NetworkImage(
                url = option.imageUrl,
                contentDescription = option.displayName,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 10f).clip(RoundedCornerShape(16.dp)),
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(option.displayName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                val secondaryName = product.name.takeIf { it != product.localizedName }
                    ?: product.nameHi.takeIf { it.isNotBlank() && it != product.name }
                if (secondaryName != null) {
                    Text(secondaryName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    option.OfferPrice(priceStyle = MaterialTheme.typography.titleMedium)
                    val stock = option.stock
                    when {
                        stock <= 0.0 -> StatusPill(
                            stringResource(Res.string.pos_info_out_of_stock),
                            MaterialTheme.colorScheme.errorContainer,
                            MaterialTheme.colorScheme.onErrorContainer,
                        )
                        stock <= product.lowStockThreshold -> StatusPill(
                            stringResource(Res.string.pos_info_low_stock),
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                        else -> Unit
                    }
                }
            }
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LabelValueRow(stringResource(Res.string.pos_info_price), option.unitPrice.asMoney(), emphasize = true)
                    if (option.mrp > 0.0) LabelValueRow(stringResource(Res.string.pos_info_mrp), option.mrp.asMoney())
                    LabelValueRow(
                        stringResource(Res.string.pos_info_stock),
                        "${option.stock.asQuantity()} ${option.quantityUnit.localizedLabel()}",
                    )
                    if (inCartQty > 0.0) LabelValueRow(stringResource(Res.string.pos_info_in_cart), inCartQty.asQuantity())
                    LabelValueRow(stringResource(Res.string.pos_info_unit), product.unit.localizedLabel())
                    LabelValueRow(stringResource(Res.string.field_category), product.category.localizedName())
                    product.localizedBrand.takeIf { it.isNotBlank() }?.let { LabelValueRow(stringResource(Res.string.pos_info_brand), it) }
                    (option.variant?.barcode ?: product.barcode)?.takeIf { it.isNotBlank() }?.let {
                        LabelValueRow(stringResource(Res.string.field_barcode), it)
                    }
                    option.variant?.sku?.takeIf { it.isNotBlank() }?.let { LabelValueRow(stringResource(Res.string.pos_info_sku), it) }
                }
            }
            val others = product.options()
            if (others.size > 1) {
                Text(stringResource(Res.string.pos_info_variants), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                ) {
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        others.forEachIndexed { index, other ->
                            if (index > 0) HorizontalDivider()
                            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    other.variant?.label ?: other.displayName,
                                    modifier = Modifier.weight(1f),
                                    fontWeight = if (other.key == option.key) FontWeight.Bold else FontWeight.Normal,
                                )
                                Text(
                                    "${other.unitPrice.asMoney()} · ${other.stock.asQuantity()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onRecordUnbilled,
                    enabled = option.stock > 0.0,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Filled.RemoveShoppingCart, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(Res.string.unbilled_sale_action), maxLines = 1)
                }
                Button(onClick = onAdd, enabled = option.inStock, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.action_add))
                }
            }
        }
    }
}

/** Changes the unit price of one cart line for this bill only. */
@Composable
fun PosLinePriceDialog(
    line: CartLine,
    onSave: (Double?) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by rememberSaveable(line.key) { mutableStateOf(line.unitPrice.asPriceInput()) }
    val parsed = input.trim().toDoubleOrNull()?.takeIf { it >= 0.0 }
    val catalogPrice = line.option.unitPrice
    AppAlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.pos_line_price_title, line.option.displayName)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(Res.string.pos_line_price_hint), style = MaterialTheme.typography.bodySmall)
                Text(
                    stringResource(Res.string.pos_line_price_catalog, catalogPrice.asMoney()),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { value -> input = value.filter { it.isDigit() || it == '.' }.take(PRICE_INPUT_MAX) },
                    label = { Text(stringResource(Res.string.pos_line_price_field)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (line.lockedUnitPrice != null) {
                    TextButton(onClick = { onSave(null) }) {
                        Text(stringResource(Res.string.pos_line_price_reset, catalogPrice.asMoney()))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(parsed) }, enabled = parsed != null) { Text(stringResource(Res.string.action_save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

/** Records stock that was sold earlier without a bill (anonymous customer). */
@Composable
fun UnbilledSaleDialog(
    option: PurchaseOption,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit,
) {
    var input by rememberSaveable(option.key) { mutableStateOf("1") }
    val qty = input.trim().toDoubleOrNull()?.takeIf { it > 0.0 }
    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.RemoveShoppingCart, null) },
        title = { Text(stringResource(Res.string.unbilled_sale_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(option.displayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(
                    stringResource(Res.string.unbilled_sale_stock, option.stock.asQuantity(), option.quantityUnit.localizedLabel()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(stringResource(Res.string.unbilled_sale_body), style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = input,
                    onValueChange = { value ->
                        input = value.filter { it.isDigit() || (option.isLoose && it == '.') }.take(QTY_INPUT_MAX)
                    },
                    label = { Text(stringResource(Res.string.unbilled_sale_qty, option.quantityUnit.localizedLabel())) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = if (option.isLoose) KeyboardType.Decimal else KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { qty?.let(onSave) }, enabled = qty != null && qty <= option.stock) {
                Text(stringResource(Res.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

private fun Double.asPriceInput(): String =
    if (this % 1.0 == 0.0) toLong().toString() else ((this * 100).toLong() / 100.0).toString()

private const val PRICE_INPUT_MAX = 9
private const val QTY_INPUT_MAX = 7
