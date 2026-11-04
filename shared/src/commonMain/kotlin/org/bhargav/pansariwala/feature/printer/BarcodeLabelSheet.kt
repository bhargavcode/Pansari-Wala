package org.bhargav.pansariwala.feature.printer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.designsystem.AppBottomSheet
import org.bhargav.pansariwala.designsystem.OutlinedTextField
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductVariant
import org.bhargav.pansariwala.feature.user.QuantityStepper
import org.bhargav.pansariwala.i18n.localizedName
import org.bhargav.pansariwala.printer.document.PrintTemplates
import org.bhargav.pansariwala.printing.PosPrintService
import org.bhargav.pansariwala.util.asQuantity
import org.bhargav.pansariwala.util.generateStoreBarcode
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.barcode_label_copies
import pansariwala.shared.generated.resources.barcode_label_generate
import pansariwala.shared.generated.resources.barcode_label_generate_failed
import pansariwala.shared.generated.resources.barcode_label_missing
import pansariwala.shared.generated.resources.barcode_label_print
import pansariwala.shared.generated.resources.barcode_label_text
import pansariwala.shared.generated.resources.barcode_label_title

private const val GENERATE_ATTEMPTS = 5
private val PREVIEW_MAX_WIDTH = 280.dp

/**
 * Sticker for shop-packed goods: name + pack size on top, barcode, then the digits.
 * Packs without a barcode can get a generated in-store code, saved on the product before printing.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BarcodeLabelSheet(
    product: Product,
    onDismiss: () -> Unit,
    shopRepository: ShopRepository = koinInject(),
    printService: PosPrintService = koinInject(),
) {
    val live by remember(product.id) { shopRepository.observeProduct(product.id) }.collectAsState(initial = product)
    val current = live ?: product
    val packs = current.activeVariants.filterNot { it.isLoose }
    var variantId by remember(product.id) { mutableStateOf(packs.firstOrNull()?.id) }
    val variant = packs.firstOrNull { it.id == variantId } ?: packs.firstOrNull()
    val barcode = (if (variant != null) variant.barcode else current.barcode)?.trim().orEmpty()
    var title by remember(product.id, variant?.id) { mutableStateOf(labelTitle(current, variant)) }
    var copies by remember { mutableIntStateOf(1) }
    var generating by remember { mutableStateOf(false) }
    var generateFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AppBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(Res.string.barcode_label_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(current.localizedName, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (packs.size > 1) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    packs.forEach { pack ->
                        FilterChip(
                            selected = pack.id == variant?.id,
                            onClick = { variantId = pack.id },
                            label = { Text(pack.label) },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(stringResource(Res.string.barcode_label_text)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (barcode.isEmpty()) {
                Text(
                    stringResource(if (generateFailed) Res.string.barcode_label_generate_failed else Res.string.barcode_label_missing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (generateFailed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(
                    onClick = {
                        generating = true
                        generateFailed = false
                        scope.launch {
                            runCatching {
                                val code = generateSequence { generateStoreBarcode() }
                                    .take(GENERATE_ATTEMPTS)
                                    .firstOrNull { shopRepository.findProduct(it) == null }
                                    ?: error("No free in-store barcode")
                                shopRepository.upsertProduct(current.withBarcode(variant, code))
                            }.onFailure { generateFailed = true }
                            generating = false
                        }
                    },
                    enabled = !generating,
                    modifier = Modifier.fillMaxWidth(),
                ) { Text(stringResource(Res.string.barcode_label_generate)) }
            } else {
                LabelPreview(title, barcode, Modifier.align(Alignment.CenterHorizontally))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(Res.string.barcode_label_copies),
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    QuantityStepper(
                        label = copies.toString(),
                        onIncrement = { copies++ },
                        onDecrement = { copies-- },
                        decrementEnabled = copies > 1,
                        incrementEnabled = copies < PrintTemplates.MAX_LABEL_COPIES,
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(Res.string.action_cancel)) }
                    Button(
                        onClick = {
                            val text = title.trim().ifEmpty { labelTitle(current, variant) }
                            printService.enqueue { printBarcodeLabel(text, barcode, copies) }
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Print, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
                        Text(stringResource(Res.string.barcode_label_print), modifier = Modifier.padding(start = ButtonDefaults.IconSpacing))
                    }
                }
            }
        }
    }
}

/** Paper-style preview; the bars are decorative, the printer draws the real barcode. */
@Composable
private fun LabelPreview(title: String, barcode: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .widthIn(max = PREVIEW_MAX_WIDTH)
            .fillMaxWidth()
            .background(Color.White, MaterialTheme.shapes.small)
            .border(1.dp, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.small)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, color = Color.Black, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium)
        Canvas(Modifier.fillMaxWidth().height(48.dp)) {
            val widths = barcode.flatMap { c -> listOf(1 + c.code % 3, 1 + (c.code / 3) % 2) }
            val unit = size.width / (widths.sum() + 2 * widths.size).coerceAtLeast(1)
            var x = 0f
            widths.forEach { w ->
                drawRect(Color.Black, Offset(x, 0f), Size(w * unit, size.height))
                x += (w + 2) * unit
            }
        }
        Text(barcode, color = Color.Black, style = MaterialTheme.typography.labelLarge)
    }
}

/** English name + pack size ("Haldi Powder 200 g"); the shop can edit it before printing. */
private fun labelTitle(product: Product, variant: ProductVariant?): String {
    val pack = when {
        variant == null -> ""
        variant.hasPackSize -> "${variant.packSize.asQuantity()} ${variant.packUnit.label}"
        else -> variant.label
    }
    return listOf(product.name, pack).filter { it.isNotBlank() }.joinToString(" ")
}

private fun Product.withBarcode(variant: ProductVariant?, code: String): Product =
    if (variant == null) {
        copy(barcode = code)
    } else {
        copy(variants = variants.map { if (it.id == variant.id) it.copy(barcode = code) else it })
    }
