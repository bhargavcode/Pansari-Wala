package org.bhargav.pansariwala.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.i18n.localizedLabel
import org.bhargav.pansariwala.i18n.localizedName
import org.bhargav.pansariwala.media.ProductImageCarousel
import org.bhargav.pansariwala.theme.stockHealthyColor
import org.bhargav.pansariwala.theme.stockOutColor
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_edit
import pansariwala.shared.generated.resources.field_barcode
import pansariwala.shared.generated.resources.field_category
import pansariwala.shared.generated.resources.field_cost_price
import pansariwala.shared.generated.resources.field_selling_price
import pansariwala.shared.generated.resources.inv_action_set_stock
import pansariwala.shared.generated.resources.inv_action_stock_in
import pansariwala.shared.generated.resources.inv_action_stock_out
import pansariwala.shared.generated.resources.inv_active_desc_off
import pansariwala.shared.generated.resources.inv_active_desc_on
import pansariwala.shared.generated.resources.inv_active_label
import pansariwala.shared.generated.resources.inv_detail_title
import pansariwala.shared.generated.resources.inv_label_brand
import pansariwala.shared.generated.resources.inv_label_margin
import pansariwala.shared.generated.resources.inv_label_master_id
import pansariwala.shared.generated.resources.inv_label_product_id
import pansariwala.shared.generated.resources.inv_product_missing
import pansariwala.shared.generated.resources.inv_qty_field
import pansariwala.shared.generated.resources.inv_section_info
import pansariwala.shared.generated.resources.inv_section_stock
import pansariwala.shared.generated.resources.inv_section_update
import pansariwala.shared.generated.resources.inv_section_variants
import pansariwala.shared.generated.resources.inv_stock_variant_pick
import pansariwala.shared.generated.resources.inv_variant_stock
import pansariwala.shared.generated.resources.inv_stock_threshold
import pansariwala.shared.generated.resources.inv_stock_value
import pansariwala.shared.generated.resources.inv_value_none
import pansariwala.shared.generated.resources.stock_req_action

private val DETAIL_MAX_WIDTH = 720.dp

@Composable
fun InventoryProductDetailScreen(
    productId: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    viewModel: InventoryProductDetailViewModel = koinViewModel(),
) {
    LaunchedEffect(productId) { viewModel.load(productId) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var showStockRequest by rememberSaveable { mutableStateOf(false) }
    val message = state.message?.asString()
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.dismissMessage()
        }
    }

    PansariScreen(
        title = state.product?.name ?: stringResource(Res.string.inv_detail_title),
        onBack = onBack,
        isLoading = state.loading,
        error = state.error.toErrorBanner(retryable = false),
        onErrorAction = { handleErrorBannerAction(it, onRetry = {}, onDismiss = viewModel::dismissError) },
        actions = {
            if (state.product != null) {
                IconButton(onClick = { onEdit(productId) }) {
                    Icon(Icons.Filled.Edit, contentDescription = stringResource(Res.string.action_edit))
                }
            }
        },
    ) {
        val product = state.product
        when {
            product != null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Column(
                    modifier = Modifier.widthIn(max = DETAIL_MAX_WIDTH).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ProductHeader(product)
                    StockCard(product, onRequestStock = { showStockRequest = true })
                    UpdateInventoryCard(
                        product = product,
                        selectedVariantId = state.selectedVariantId,
                        editedStock = state.editedStock,
                        onSelectVariant = viewModel::selectVariant,
                        qtyInput = state.qtyInput,
                        onQtyChange = viewModel::onQtyChange,
                        onStockIn = viewModel::stockIn,
                        onStockOut = viewModel::stockOut,
                        onSetStock = viewModel::setStock,
                        onActiveChange = viewModel::setActive,
                    )
                    InfoCard(product)
                    if (product.hasVariants) VariantsCard(product)
                }
            }
            state.missing -> Text(
                text = stringResource(Res.string.inv_product_missing),
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
    val product = state.product
    if (showStockRequest && product != null) {
        StockRequestSheet(product = product, onDismiss = { showStockRequest = false })
    }
}

@Composable
private fun ProductHeader(product: Product) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val images = product.imageUrls.ifEmpty { listOfNotNull(product.primaryImageUrl) }
        if (images.isNotEmpty()) {
            ProductImageCarousel(imageUrls = images, height = 200.dp, modifier = Modifier.fillMaxWidth())
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = product.localizedName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (!product.isActive) InactiveTag()
        }
        if (product.nameHi.isNotBlank()) {
            Text(product.nameHi, style = MaterialTheme.typography.titleMedium)
        }
        Text(
            text = listOf(product.brandName, product.category.localizedName()).filter { it.isNotBlank() }
                .joinToString(" · "),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
private fun StockCard(product: Product, onRequestStock: () -> Unit) {
    val level = product.stockLevel
    val unit = product.unit.localizedLabel()
    SectionCard(
        title = stringResource(Res.string.inv_section_stock),
        modifier = Modifier.fillMaxWidth(),
        action = { StockBadge(level) },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = product.totalStock.asQuantity(),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = level.color(),
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.titleMedium,
                    color = level.color(),
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            StockGauge(
                stockQty = product.totalStock,
                lowStockThreshold = product.lowStockThreshold,
                modifier = Modifier.fillMaxWidth().height(10.dp),
            )
            Text(
                text = stringResource(Res.string.inv_stock_threshold, product.lowStockThreshold.asQuantity(), unit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(Res.string.inv_stock_value, product.stockValue.asMoney()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            val requestLabel: @Composable () -> Unit = {
                Icon(Icons.Filled.LocalShipping, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(Res.string.stock_req_action), Modifier.padding(start = 8.dp))
            }
            if (product.isLowStock) {
                Button(
                    onClick = onRequestStock,
                    colors = ButtonDefaults.buttonColors(containerColor = level.color()),
                    modifier = Modifier.fillMaxWidth(),
                ) { requestLabel() }
            } else {
                OutlinedButton(onClick = onRequestStock, modifier = Modifier.fillMaxWidth()) { requestLabel() }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UpdateInventoryCard(
    product: Product,
    selectedVariantId: String?,
    editedStock: Double,
    onSelectVariant: (String) -> Unit,
    qtyInput: String,
    onQtyChange: (String) -> Unit,
    onStockIn: (Double?) -> Unit,
    onStockOut: (Double?) -> Unit,
    onSetStock: () -> Unit,
    onActiveChange: (Boolean) -> Unit,
) {
    val inColors = ButtonDefaults.buttonColors(containerColor = stockHealthyColor)
    val outColors = ButtonDefaults.outlinedButtonColors(contentColor = stockOutColor)
    SectionCard(title = stringResource(Res.string.inv_section_update), modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val stockVariants = product.stockVariants
            if (stockVariants.isNotEmpty()) {
                Text(
                    stringResource(Res.string.inv_stock_variant_pick),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    stockVariants.forEach { variant ->
                        FilterChip(
                            selected = variant.id == selectedVariantId,
                            onClick = { onSelectVariant(variant.id) },
                            label = { Text("${variant.label} · ${variant.stockQty.asQuantity()}") },
                        )
                    }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppConstants.INVENTORY_QUICK_STEPS.forEach { step ->
                    Button(onClick = { onStockIn(step) }, colors = inColors) { Text("+${step.asQuantity()}") }
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                AppConstants.INVENTORY_QUICK_STEPS.forEach { step ->
                    OutlinedButton(
                        onClick = { onStockOut(step) },
                        enabled = editedStock >= step,
                        colors = outColors,
                    ) { Text("−${step.asQuantity()}") }
                }
            }
            OutlinedTextField(
                value = qtyInput,
                onValueChange = onQtyChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(Res.string.inv_qty_field)) },
                suffix = { Text(product.unit.localizedLabel()) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { onStockIn(null) }, colors = inColors) {
                    Text(stringResource(Res.string.inv_action_stock_in))
                }
                OutlinedButton(onClick = { onStockOut(null) }, colors = outColors) {
                    Text(stringResource(Res.string.inv_action_stock_out))
                }
                OutlinedButton(onClick = onSetStock) { Text(stringResource(Res.string.inv_action_set_stock)) }
            }
            HorizontalDivider()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(Res.string.inv_active_label), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = stringResource(
                            if (product.isActive) Res.string.inv_active_desc_on else Res.string.inv_active_desc_off,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = product.isActive, onCheckedChange = onActiveChange)
            }
        }
    }
}

@Composable
private fun InfoCard(product: Product) {
    val none = stringResource(Res.string.inv_value_none)
    val margin = product.sellingPrice - product.costPrice
    val marginPct = if (product.sellingPrice > 0.0) (margin / product.sellingPrice * 100).asQuantity() else null
    SectionCard(title = stringResource(Res.string.inv_section_info), modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            InfoRow(stringResource(Res.string.inv_label_product_id), product.id)
            InfoRow(stringResource(Res.string.inv_label_master_id), product.masterProductId ?: none)
            InfoRow(stringResource(Res.string.field_barcode), product.barcode ?: none)
            InfoRow(stringResource(Res.string.inv_label_brand), product.brandName.ifBlank { none })
            InfoRow(stringResource(Res.string.field_category), product.category.localizedName())
            InfoRow(stringResource(Res.string.field_selling_price), product.sellingPrice.asMoney())
            InfoRow(stringResource(Res.string.field_cost_price), product.costPrice.asMoney())
            InfoRow(
                stringResource(Res.string.inv_label_margin),
                marginPct?.let { "${margin.asMoney()} ($it%)" } ?: margin.asMoney(),
            )
        }
    }
}

@Composable
private fun VariantsCard(product: Product) {
    SectionCard(title = stringResource(Res.string.inv_section_variants), modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            product.variants.forEach { variant ->
                val stock = if (variant.isLinkedBundle) "" else " · ${stringResource(Res.string.inv_variant_stock, variant.stockQty.asQuantity())}"
                InfoRow(variant.label, variant.price.asMoney() + stock)
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Box(Modifier.weight(1.4f), contentAlignment = Alignment.CenterEnd) {
            Text(text = value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}
