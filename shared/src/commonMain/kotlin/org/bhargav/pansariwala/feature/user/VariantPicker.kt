package org.bhargav.pansariwala.feature.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductUnit
import org.bhargav.pansariwala.domain.model.SellMode
import org.bhargav.pansariwala.i18n.localizedLabel
import org.bhargav.pansariwala.media.NetworkImage
import org.bhargav.pansariwala.util.AppConstants.LooseQty
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_add
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.action_close
import pansariwala.shared.generated.resources.action_edit
import pansariwala.shared.generated.resources.action_save
import pansariwala.shared.generated.resources.variant_pack_count_label
import pansariwala.shared.generated.resources.variant_pack_count_title
import pansariwala.shared.generated.resources.catalog_out_of_stock
import pansariwala.shared.generated.resources.catalog_per_unit
import pansariwala.shared.generated.resources.qty_with_unit
import pansariwala.shared.generated.resources.unit_g
import pansariwala.shared.generated.resources.unit_kg
import pansariwala.shared.generated.resources.unit_ltr
import pansariwala.shared.generated.resources.unit_ml
import pansariwala.shared.generated.resources.variant_add_amount
import pansariwala.shared.generated.resources.variant_bundle_of
import pansariwala.shared.generated.resources.variant_choose
import pansariwala.shared.generated.resources.variant_custom_qty
import pansariwala.shared.generated.resources.variant_from_price
import pansariwala.shared.generated.resources.variant_in_cart
import pansariwala.shared.generated.resources.variant_loose
import pansariwala.shared.generated.resources.variant_min_qty
import pansariwala.shared.generated.resources.variant_mrp
import pansariwala.shared.generated.resources.variant_options_count
import pansariwala.shared.generated.resources.variant_picker_title
import pansariwala.shared.generated.resources.variant_price_per
import pansariwala.shared.generated.resources.variant_quantity
import pansariwala.shared.generated.resources.variant_update_amount

/** "250 g", "1.5 kg", "500 ml", or a plain pack count. */
@Composable
fun PurchaseOption.quantityLabel(quantity: Double): String {
    if (!isLoose) return quantity.asQuantity()
    val subUnit = quantity < 1.0 - LooseQty.EPSILON
    return when (quantityUnit) {
        ProductUnit.KG -> if (subUnit) qtyText(quantity * LooseQty.SUB_UNIT_FACTOR, Res.string.unit_g)
        else qtyText(quantity, Res.string.unit_kg)
        ProductUnit.LITRE -> if (subUnit) qtyText(quantity * LooseQty.SUB_UNIT_FACTOR, Res.string.unit_ml)
        else qtyText(quantity, Res.string.unit_ltr)
        else -> stringResource(Res.string.qty_with_unit, quantity.asQuantity(), quantityUnit.localizedLabel())
    }
}

@Composable
private fun qtyText(value: Double, unit: org.jetbrains.compose.resources.StringResource): String =
    stringResource(Res.string.qty_with_unit, value.asQuantity(), stringResource(unit))

/** "₹60.00 / kg" for loose, "₹62.00" for packs. */
@Composable
fun PurchaseOption.priceLabel(): String =
    if (isLoose) stringResource(Res.string.variant_price_per, unitPrice.asMoney(), quantityUnit.localizedLabel())
    else unitPrice.asMoney()

/** Opens [VariantPickerSheet] for a product, optionally editing an existing cart line. */
@Stable
class VariantPickerState {
    var request by mutableStateOf<Request?>(null)
        private set

    /**
     * [optionKey] limits the sheet to one option (a variant card in a flat list). [initialOptionKey] and
     * [initialQty] only preselect (e.g. what a voice command asked for); the cashier can still change them.
     */
    data class Request(
        val product: Product,
        val editKey: String?,
        val optionKey: String? = null,
        val initialOptionKey: String? = null,
        val initialQty: Double? = null,
    )

    fun open(product: Product, editKey: String? = null, optionKey: String? = null) {
        request = Request(product, editKey, optionKey)
    }

    fun open(option: PurchaseOption, editKey: String? = null) = open(option.product, editKey, option.key)

    fun dismiss() {
        request = null
    }
}

@Composable
fun rememberVariantPicker(): VariantPickerState = remember { VariantPickerState() }

/** Renders the sheet while [state] has a request. Place once per screen. */
@Composable
fun VariantPickerHost(state: VariantPickerState, cart: CartStore) {
    val request = state.request ?: return
    val lines by cart.lines.collectAsStateWithLifecycle()
    VariantPickerSheet(
        product = request.product,
        editKey = request.editKey,
        optionKey = request.optionKey,
        cartLines = lines.filter { it.product.id == request.product.id },
        onConfirm = { targetKey, option, quantity ->
            cart.update(targetKey, option, quantity)
            state.dismiss()
        },
        onDismiss = state::dismiss,
    )
}

/** Same sheet for carts not backed by [CartStore] (e.g. POS billing). */
@Composable
fun VariantPickerHost(
    request: VariantPickerState.Request,
    lines: List<CartStore.Line>,
    onConfirm: (targetKey: String, option: PurchaseOption, quantity: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    VariantPickerSheet(
        product = request.product,
        editKey = request.editKey,
        optionKey = request.optionKey,
        cartLines = lines.filter { it.product.id == request.product.id },
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        initialOptionKey = request.initialOptionKey,
        initialQty = request.initialQty,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun VariantPickerSheet(
    product: Product,
    editKey: String?,
    optionKey: String?,
    cartLines: List<CartStore.Line>,
    onConfirm: (targetKey: String, option: PurchaseOption, quantity: Double) -> Unit,
    onDismiss: () -> Unit,
    initialOptionKey: String? = null,
    initialQty: Double? = null,
) {
    val options = remember(product, optionKey, editKey) {
        val all = product.options()
        val focus = optionKey ?: editKey
        all.filter { it.key == focus }.ifEmpty { all }
    }
    val editing = cartLines.firstOrNull { it.key == editKey }
    var selected by remember(product, editKey, initialOptionKey) {
        mutableStateOf(
            editing?.option
                ?: options.firstOrNull { it.key == initialOptionKey }
                ?: options.firstOrNull { it.inStock }
                ?: options.first(),
        )
    }
    var quantity by remember(product, editKey, initialQty) {
        mutableStateOf(
            editing?.quantity
                ?: initialQty?.takeIf { it > 0.0 }?.let { selected.normalize(it) }
                ?: cartLines.firstOrNull { it.key == selected.key }?.quantity
                ?: selected.defaultQty,
        )
    }
    val targetKey = editKey ?: selected.key
    val isUpdate = editKey != null || cartLines.any { it.key == selected.key }

    fun select(option: PurchaseOption) {
        selected = option
        quantity = cartLines.firstOrNull { it.key == option.key }?.quantity ?: option.defaultQty
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                NetworkImage(
                    url = selected.imageUrl,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                    contentDescription = product.name,
                )
                Column(Modifier.weight(1f)) {
                    Text(
                        if (options.size == 1) selected.displayName else product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    if (options.size > 1) {
                        Text(
                            stringResource(Res.string.variant_picker_title),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = stringResource(Res.string.action_close))
                }
            }
            if (options.size > 1) {
                options.forEach { option ->
                    OptionRow(
                        option = option,
                        selected = option.key == selected.key,
                        inCart = cartLines.firstOrNull { it.key == option.key }?.quantity,
                        onClick = { select(option) },
                    )
                }
            }
            if (selected.inStock) {
                Text(stringResource(Res.string.variant_quantity), style = MaterialTheme.typography.titleSmall)
                if (selected.isLoose) {
                    LooseQuantityEditor(option = selected, quantity = quantity, onChange = { quantity = it })
                } else {
                    PackCountStepper(
                        option = selected,
                        quantity = quantity,
                        onSet = { quantity = selected.normalize(it) },
                        onIncrement = { quantity = selected.normalize(quantity + 1) },
                        onDecrement = { quantity = selected.normalize(quantity - 1) },
                        decrementEnabled = quantity > selected.minQty,
                    )
                }
                val total = selected.priceFor(quantity).asMoney()
                UserPrimaryButton(
                    text = stringResource(
                        if (isUpdate) Res.string.variant_update_amount else Res.string.variant_add_amount,
                        total,
                    ),
                    onClick = { onConfirm(targetKey, selected, quantity) },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                )
            } else {
                Text(
                    stringResource(Res.string.catalog_out_of_stock),
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }
        }
    }
}

@Composable
private fun OptionRow(option: PurchaseOption, selected: Boolean, inCart: Double?, onClick: () -> Unit) {
    val variant = option.variant ?: return
    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            if (selected) 2.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(enabled = option.inStock, onClick = onClick),
    ) {
        Row(
            Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NetworkImage(
                url = option.imageUrl,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(6.dp)),
                contentDescription = variant.label,
            )
            Column(Modifier.weight(1f)) {
                Text(variant.label, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val badge = when (variant.sellMode) {
                    SellMode.LOOSE -> stringResource(Res.string.variant_loose)
                    SellMode.BUNDLE -> stringResource(Res.string.variant_bundle_of, variant.bundleCount)
                    SellMode.PACK -> (listOfNotNull(option.packSizeLabel()) + variant.attributes.values).joinToString(" · ")
                }
                if (badge.isNotBlank()) {
                    Text(badge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                when {
                    !option.inStock -> Text(
                        stringResource(Res.string.catalog_out_of_stock),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                    inCart != null -> Text(
                        stringResource(Res.string.variant_in_cart, option.quantityLabel(inCart)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(option.priceLabel(), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                option.strikePrice?.let { was ->
                    Text(
                        stringResource(Res.string.variant_mrp, was.asMoney()),
                        style = MaterialTheme.typography.labelSmall,
                        textDecoration = TextDecoration.LineThrough,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                offLabel(option.strikePrice, option.unitPrice)?.let { OfferTag(it) }
            }
        }
    }
}

/** Quick picks (100 g, 250 g, 1 kg…), a step stepper, and a free-form amount in g/kg or ml/l. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LooseQuantityEditor(option: PurchaseOption, quantity: Double, onChange: (Double) -> Unit) {
    val hasSubUnit = option.quantityUnit == ProductUnit.KG || option.quantityUnit == ProductUnit.LITRE
    var useSubUnit by remember(option.key) { mutableStateOf(hasSubUnit && quantity < 1.0) }
    var text by remember(option.key) { mutableStateOf(displayValue(quantity, useSubUnit)) }

    fun apply(value: Double) {
        val normalized = option.normalize(value)
        onChange(normalized)
        text = displayValue(normalized, useSubUnit)
    }

    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        option.quickPicks.forEach { pick ->
            FilterChip(
                selected = kotlin.math.abs(pick - quantity) < LooseQty.EPSILON,
                onClick = { apply(pick) },
                label = { Text(option.quantityLabel(pick)) },
            )
        }
    }
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        QuantityStepper(
            label = option.quantityLabel(quantity),
            onIncrement = { apply(quantity + option.step) },
            onDecrement = { apply(quantity - option.step) },
            decrementEnabled = quantity - option.step + LooseQty.EPSILON >= option.minQty,
            incrementEnabled = quantity + option.step <= option.maxQty + LooseQty.EPSILON,
        )
        Text(
            stringResource(Res.string.variant_min_qty, option.quantityLabel(option.minQty)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = text,
            onValueChange = { raw ->
                text = raw.filter { it.isDigit() || it == '.' }
                text.toDoubleOrNull()?.let { typed ->
                    val base = if (useSubUnit) typed / LooseQty.SUB_UNIT_FACTOR else typed
                    if (base > 0.0) onChange(option.normalize(base))
                }
            },
            label = { Text(stringResource(Res.string.variant_custom_qty)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
        )
        if (hasSubUnit) {
            val (sub, base) = if (option.quantityUnit == ProductUnit.KG) Res.string.unit_g to Res.string.unit_kg
            else Res.string.unit_ml to Res.string.unit_ltr
            FilterChip(selected = useSubUnit, onClick = {
                useSubUnit = true
                text = displayValue(quantity, true)
            }, label = { Text(stringResource(sub)) })
            FilterChip(selected = !useSubUnit, onClick = {
                useSubUnit = false
                text = displayValue(quantity, false)
            }, label = { Text(stringResource(base)) })
        }
    }
}

private fun displayValue(quantity: Double, subUnit: Boolean): String =
    (if (subUnit) quantity * LooseQty.SUB_UNIT_FACTOR else quantity).asQuantity()

/** "Parle-G · Large" for a variant card, the plain product name otherwise. */
val PurchaseOption.displayName: String
    get() = variant?.label?.takeIf { it.isNotBlank() }?.let { "${product.name} · $it" } ?: product.name

/** "100 g" net content of a pack variant, or null when not set. */
@Composable
fun PurchaseOption.packSizeLabel(): String? {
    val v = variant?.takeIf { it.hasPackSize } ?: return null
    return stringResource(Res.string.qty_with_unit, v.packSize.asQuantity(), v.packUnit.localizedLabel())
}

/** Pack-count stepper whose number can be tapped to type an exact count. */
@Composable
fun PackCountStepper(
    option: PurchaseOption,
    quantity: Double,
    onSet: (Double) -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    decrementEnabled: Boolean = true,
) {
    var typing by remember { mutableStateOf(false) }
    QuantityStepper(
        label = quantity.asQuantity(),
        onIncrement = onIncrement,
        onDecrement = onDecrement,
        modifier = modifier,
        decrementEnabled = decrementEnabled,
        incrementEnabled = quantity < option.maxQty,
        onLabelClick = { typing = true },
    )
    if (typing) {
        PackCountDialog(
            initial = quantity,
            max = option.maxQty,
            onConfirm = { typing = false; onSet(it) },
            onDismiss = { typing = false },
        )
    }
}

@Composable
private fun PackCountDialog(initial: Double, max: Double, onConfirm: (Double) -> Unit, onDismiss: () -> Unit) {
    var text by remember { mutableStateOf(initial.asQuantity()) }
    val value = text.toIntOrNull()?.takeIf { it > 0 }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.variant_pack_count_title)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { raw -> text = raw.filter { it.isDigit() }.take(MAX_PACK_COUNT_DIGITS) },
                label = { Text(stringResource(Res.string.variant_pack_count_label, max.asQuantity())) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(onClick = { value?.let { onConfirm(it.toDouble()) } }, enabled = value != null) {
                Text(stringResource(Res.string.action_save))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

private const val MAX_PACK_COUNT_DIGITS = 5

/**
 * Add / stepper control for one option card. Packs and bundles add in one tap and show a stepper
 * (tap the number to type a count); loose options open the amount sheet.
 */
@Composable
fun OptionCartAction(
    option: PurchaseOption,
    lines: List<CartStore.Line>,
    cart: CartStore,
    picker: VariantPickerState,
    modifier: Modifier = Modifier,
) {
    val line = lines.firstOrNull { it.key == option.key }
    when {
        option.needsPicker && line != null -> OutlinedButton(onClick = { picker.open(option, line.key) }, modifier = modifier) {
            Text(option.quantityLabel(line.quantity), maxLines = 1)
        }
        option.needsPicker -> OutlinedButton(onClick = { picker.open(option) }, enabled = option.inStock, modifier = modifier) {
            Text(stringResource(Res.string.variant_choose), maxLines = 1)
        }
        line != null -> PackCountStepper(
            option = option,
            quantity = line.quantity,
            onSet = { cart.setQuantity(line.key, it) },
            onIncrement = { cart.increment(line.key) },
            onDecrement = { cart.decrement(line.key) },
            modifier = modifier,
        )
        else -> TextButton(onClick = { cart.add(option, option.defaultQty) }, enabled = option.inStock, modifier = modifier) {
            Text(stringResource(Res.string.action_add))
        }
    }
}

/** Cart row: primary image, chosen option, amount × price, and edit (re-opens the picker). */
@Composable
fun CartLineRow(
    line: CartStore.Line,
    onEdit: () -> Unit,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    modifier: Modifier = Modifier,
    onSetQuantity: ((Double) -> Unit)? = null,
) {
    val option = line.option
    Row(
        modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NetworkImage(
            url = option.imageUrl,
            modifier = Modifier.size(52.dp).clip(RoundedCornerShape(8.dp)),
            contentDescription = line.product.name,
        )
        Column(Modifier.weight(1f)) {
            Text(line.product.name, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            line.variant?.let {
                val detail = listOfNotNull(it.label, option.packSizeLabel()).distinct().joinToString(" · ")
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "${option.quantityLabel(line.quantity)} × ${option.priceLabel()}",
                style = MaterialTheme.typography.bodySmall,
            )
            LineOfferPrice(line.lineTotal, line.baseLineTotal)
            if (option.needsPicker) {
                TextButton(onClick = onEdit, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                    Text(stringResource(Res.string.action_edit))
                }
            }
        }
        if (!option.isLoose && onSetQuantity != null) {
            PackCountStepper(
                option = option,
                quantity = line.quantity,
                onSet = onSetQuantity,
                onIncrement = onIncrement,
                onDecrement = onDecrement,
            )
        } else {
            QuantityStepper(
                label = option.quantityLabel(line.quantity),
                onIncrement = onIncrement,
                onDecrement = onDecrement,
                incrementEnabled = line.quantity + option.step <= option.maxQty + LooseQty.EPSILON,
            )
        }
    }
}

/** Price line for one option card: "₹10.00 · 100 g", "₹48.00 / kg · Loose", "₹220.00 · Bundle of 24". */
@Composable
fun OptionPriceSummary(option: PurchaseOption, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        option.OfferPrice()
        val variant = option.variant
        val detail = when {
            option.isLoose -> stringResource(Res.string.variant_loose)
            variant?.sellMode == SellMode.BUNDLE -> stringResource(Res.string.variant_bundle_of, variant.bundleCount)
            variant != null -> option.packSizeLabel().orEmpty()
            else -> stringResource(Res.string.catalog_per_unit, option.quantityUnit.localizedLabel())
        }
        if (detail.isNotBlank()) {
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** "₹48.00 per kg", "from ₹62.00 · 3 options". */
@Composable
fun ProductPriceSummary(product: Product, modifier: Modifier = Modifier) {
    val options = product.options()
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        if (options.size > 1) {
            Text(
                stringResource(Res.string.variant_from_price, product.salePrice(product.startingPrice).asMoney()),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
            )
            product.offerTag()?.let { OfferTag(it) }
            Text(
                stringResource(Res.string.variant_options_count, options.size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            val option = options.first()
            option.OfferPrice()
            Text(
                stringResource(Res.string.catalog_per_unit, option.variant?.label ?: option.quantityUnit.localizedLabel()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
