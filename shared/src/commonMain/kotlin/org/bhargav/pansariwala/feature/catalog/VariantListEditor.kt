package org.bhargav.pansariwala.feature.catalog

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.api.ProductVariantDto
import org.bhargav.pansariwala.domain.model.ProductUnit
import org.bhargav.pansariwala.domain.model.SellMode
import org.bhargav.pansariwala.i18n.localizedLabel
import org.bhargav.pansariwala.media.NetworkImage
import org.bhargav.pansariwala.util.asQuantity
import org.bhargav.pansariwala.util.generateId
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.master_add_variant
import pansariwala.shared.generated.resources.master_remove_variant
import pansariwala.shared.generated.resources.master_sku
import pansariwala.shared.generated.resources.master_variants
import pansariwala.shared.generated.resources.variant_editor_active
import pansariwala.shared.generated.resources.variant_editor_bundle_base
import pansariwala.shared.generated.resources.variant_editor_bundle_base_none
import pansariwala.shared.generated.resources.variant_editor_pack_size
import pansariwala.shared.generated.resources.variant_editor_pack_unit
import pansariwala.shared.generated.resources.variant_editor_attributes
import pansariwala.shared.generated.resources.variant_editor_barcode
import pansariwala.shared.generated.resources.variant_editor_base_unit
import pansariwala.shared.generated.resources.variant_editor_bundle_count
import pansariwala.shared.generated.resources.variant_editor_hint
import pansariwala.shared.generated.resources.variant_editor_image
import pansariwala.shared.generated.resources.variant_editor_label
import pansariwala.shared.generated.resources.variant_editor_max
import pansariwala.shared.generated.resources.variant_editor_min
import pansariwala.shared.generated.resources.variant_editor_move_up
import pansariwala.shared.generated.resources.variant_editor_mrp
import pansariwala.shared.generated.resources.variant_editor_price_loose
import pansariwala.shared.generated.resources.variant_editor_price_pack
import pansariwala.shared.generated.resources.variant_editor_quick_picks
import pansariwala.shared.generated.resources.variant_editor_step
import pansariwala.shared.generated.resources.variant_editor_stock
import pansariwala.shared.generated.resources.variant_sell_bundle
import pansariwala.shared.generated.resources.variant_sell_loose
import pansariwala.shared.generated.resources.variant_sell_pack

private val LOOSE_UNITS = listOf(ProductUnit.KG, ProductUnit.LITRE, ProductUnit.GRAM)
private val PACK_UNITS = listOf(ProductUnit.GRAM, ProductUnit.KG, ProductUnit.LITRE, ProductUnit.PIECE)

/**
 * Edits a product's sellable options (Master admin and shop inventory). [onPickImage] uploads an
 * image and returns its URL; pass null to hide per-option images.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VariantListEditor(
    variants: List<ProductVariantDto>,
    onChange: (List<ProductVariantDto>) -> Unit,
    modifier: Modifier = Modifier,
    onPickImage: (suspend () -> String?)? = null,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.master_variants), fontWeight = FontWeight.SemiBold)
        Text(
            stringResource(Res.string.variant_editor_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        variants.forEachIndexed { index, variant ->
            fun set(updated: ProductVariantDto) = onChange(variants.toMutableList().also { it[index] = updated })
            VariantCard(
                slot = "${variants.size}-$index-${variant.id}",
                variant = variant,
                onChange = ::set,
                onRemove = { onChange(variants.toMutableList().also { it.removeAt(index) }) },
                onMoveUp = if (index > 0) {
                    { onChange(variants.toMutableList().also { it.add(index - 1, it.removeAt(index)) }) }
                } else null,
                onPickImage = onPickImage,
                siblings = variants.filterIndexed { i, _ -> i != index },
            )
        }
        TextButton(onClick = { onChange(variants + ProductVariantDto(name = "", id = generateId("v"))) }) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(4.dp))
            Text(stringResource(Res.string.master_add_variant))
        }
    }
}

/** One option's fields; also used alone by the POS "Add variant" dialog. [siblings] feed the bundle stock link. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun VariantCard(
    slot: String,
    variant: ProductVariantDto,
    onChange: (ProductVariantDto) -> Unit,
    onRemove: (() -> Unit)?,
    onMoveUp: (() -> Unit)?,
    onPickImage: (suspend () -> String?)?,
    siblings: List<ProductVariantDto> = emptyList(),
) {
    val mode = SellMode.fromName(variant.sellMode)
    val scope = rememberCoroutineScope()
    var uploading by remember(slot) { mutableStateOf(false) }
    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = variant.name,
                onValueChange = { onChange(variant.copy(name = it)) },
                label = { Text(stringResource(Res.string.variant_editor_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(Res.string.variant_editor_active), modifier = Modifier.weight(1f))
                Switch(checked = variant.active, onCheckedChange = { onChange(variant.copy(active = it)) })
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SellMode.entries.forEach { option ->
                    FilterChip(
                        selected = option == mode,
                        onClick = {
                            onChange(
                                variant.copy(
                                    sellMode = option.name,
                                    baseUnit = if (option == SellMode.LOOSE) variant.baseUnit.ifBlank { ProductUnit.KG.name } else "",
                                    bundleCount = if (option == SellMode.BUNDLE) variant.bundleCount.coerceAtLeast(2) else 0,
                                    packSize = if (option == SellMode.LOOSE) 0.0 else variant.packSize,
                                    packUnit = if (option == SellMode.LOOSE) "" else variant.packUnit,
                                    baseVariantId = if (option == SellMode.BUNDLE) variant.baseVariantId else "",
                                ),
                            )
                        },
                        label = { Text(stringResource(option.labelRes())) },
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField(
                    slot = "$slot-price",
                    value = variant.price,
                    label = stringResource(
                        if (mode == SellMode.LOOSE) Res.string.variant_editor_price_loose else Res.string.variant_editor_price_pack,
                    ),
                    onValue = { onChange(variant.copy(price = it)) },
                )
                NumberField("$slot-mrp", variant.mrp, stringResource(Res.string.variant_editor_mrp)) {
                    onChange(variant.copy(mrp = it))
                }
                NumberField("$slot-stock", variant.stockQty, stringResource(Res.string.variant_editor_stock)) {
                    onChange(variant.copy(stockQty = it))
                }
                if (mode == SellMode.BUNDLE) {
                    NumberField("$slot-bundle", variant.bundleCount.toDouble(), stringResource(Res.string.variant_editor_bundle_count)) {
                        onChange(variant.copy(bundleCount = it.toInt()))
                    }
                }
                if (mode != SellMode.LOOSE) {
                    NumberField("$slot-pack", variant.packSize, stringResource(Res.string.variant_editor_pack_size)) {
                        onChange(variant.copy(packSize = it, packUnit = variant.packUnit.ifBlank { ProductUnit.GRAM.name }))
                    }
                }
            }
            if (mode != SellMode.LOOSE && variant.packSize > 0.0) {
                Text(stringResource(Res.string.variant_editor_pack_unit), style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PACK_UNITS.forEach { unit ->
                        FilterChip(
                            selected = variant.packUnit == unit.name,
                            onClick = { onChange(variant.copy(packUnit = unit.name)) },
                            label = { Text(unit.localizedLabel()) },
                        )
                    }
                }
            }
            val bundleBases = siblings.filter { SellMode.fromName(it.sellMode) == SellMode.PACK && it.id.isNotBlank() }
            if (mode == SellMode.BUNDLE && bundleBases.isNotEmpty()) {
                Text(stringResource(Res.string.variant_editor_bundle_base), style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = variant.baseVariantId.isBlank(),
                        onClick = { onChange(variant.copy(baseVariantId = "")) },
                        label = { Text(stringResource(Res.string.variant_editor_bundle_base_none)) },
                    )
                    bundleBases.forEach { base ->
                        FilterChip(
                            selected = variant.baseVariantId == base.id,
                            onClick = { onChange(variant.copy(baseVariantId = base.id)) },
                            label = { Text(base.name) },
                        )
                    }
                }
            }
            if (mode == SellMode.LOOSE) {
                Text(stringResource(Res.string.variant_editor_base_unit), style = MaterialTheme.typography.labelMedium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LOOSE_UNITS.forEach { unit ->
                        FilterChip(
                            selected = variant.baseUnit == unit.name,
                            onClick = { onChange(variant.copy(baseUnit = unit.name)) },
                            label = { Text(unit.localizedLabel()) },
                        )
                    }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("$slot-min", variant.minQty, stringResource(Res.string.variant_editor_min)) {
                        onChange(variant.copy(minQty = it))
                    }
                    NumberField("$slot-max", variant.maxQty, stringResource(Res.string.variant_editor_max)) {
                        onChange(variant.copy(maxQty = it))
                    }
                    NumberField("$slot-step", variant.stepQty, stringResource(Res.string.variant_editor_step)) {
                        onChange(variant.copy(stepQty = it))
                    }
                }
                var picks by remember(slot) { mutableStateOf(variant.quickPicks.joinToString(", ") { it.asQuantity() }) }
                OutlinedTextField(
                    value = picks,
                    onValueChange = { raw ->
                        picks = raw
                        onChange(variant.copy(quickPicks = raw.split(',').mapNotNull { it.trim().toDoubleOrNull() }.filter { it > 0 }))
                    },
                    label = { Text(stringResource(Res.string.variant_editor_quick_picks)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            var attrs by remember(slot) {
                mutableStateOf(variant.attributes.entries.joinToString(", ") { "${it.key}=${it.value}" })
            }
            OutlinedTextField(
                value = attrs,
                onValueChange = { raw ->
                    attrs = raw
                    onChange(variant.copy(attributes = parseAttributes(raw)))
                },
                label = { Text(stringResource(Res.string.variant_editor_attributes)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = variant.sku,
                    onValueChange = { onChange(variant.copy(sku = it)) },
                    label = { Text(stringResource(Res.string.master_sku)) },
                    singleLine = true,
                    modifier = Modifier.widthIn(min = 140.dp, max = 220.dp),
                )
                OutlinedTextField(
                    value = variant.barcode.orEmpty(),
                    onValueChange = { onChange(variant.copy(barcode = it.ifBlank { null })) },
                    label = { Text(stringResource(Res.string.variant_editor_barcode)) },
                    singleLine = true,
                    modifier = Modifier.widthIn(min = 140.dp, max = 220.dp),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onPickImage != null) {
                    variant.imageUrls.firstOrNull()?.let { url ->
                        NetworkImage(url = url, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)))
                    }
                    OutlinedButton(
                        enabled = !uploading,
                        onClick = {
                            scope.launch {
                                uploading = true
                                runCatching { onPickImage() }.getOrNull()?.let { url ->
                                    onChange(variant.copy(imageUrls = listOf(url)))
                                }
                                uploading = false
                            }
                        },
                    ) { Text(if (uploading) "…" else stringResource(Res.string.variant_editor_image)) }
                }
                Spacer(Modifier.weight(1f))
                if (onMoveUp != null) {
                    TextButton(onClick = onMoveUp) { Text(stringResource(Res.string.variant_editor_move_up)) }
                }
                if (onRemove != null) {
                    TextButton(onClick = onRemove) {
                        Text(stringResource(Res.string.master_remove_variant), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

/** Keeps the raw text while typing ("0.", "1.2") and reports parsed values. */
@Composable
private fun NumberField(slot: String, value: Double, label: String, onValue: (Double) -> Unit) {
    var text by remember(slot) { mutableStateOf(if (value == 0.0) "" else value.asQuantity()) }
    OutlinedTextField(
        value = text,
        onValueChange = { raw ->
            text = raw.filter { it.isDigit() || it == '.' }
            onValue(text.toDoubleOrNull() ?: 0.0)
        },
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.widthIn(min = 120.dp, max = 180.dp),
    )
}

private fun parseAttributes(raw: String): Map<String, String> =
    raw.split(',').mapNotNull { part ->
        val key = part.substringBefore('=', "").trim()
        val value = part.substringAfter('=', "").trim()
        if (key.isBlank() || value.isBlank()) null else key to value
    }.toMap()

private fun SellMode.labelRes() = when (this) {
    SellMode.PACK -> Res.string.variant_sell_pack
    SellMode.LOOSE -> Res.string.variant_sell_loose
    SellMode.BUNDLE -> Res.string.variant_sell_bundle
}
