package org.bhargav.pansariwala.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.bhargav.pansariwala.api.MasterCategoryDto
import org.bhargav.pansariwala.domain.model.ProductCategory
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.field_category
import pansariwala.shared.generated.resources.field_cost_price
import pansariwala.shared.generated.resources.field_low_stock_at
import pansariwala.shared.generated.resources.field_selling_price
import pansariwala.shared.generated.resources.field_sub_category
import pansariwala.shared.generated.resources.inv_opening_stock

/** Editable shop values for one product; numbers stay as typed text until saved. */
data class ProductDetailsDraft(
    val sellingPrice: String = "",
    val costPrice: String = "",
    val stockQty: String = "",
    val lowStockThreshold: String = "",
    val categoryId: String? = null,
    val subCategoryId: String? = null,
) {
    val sellingPriceValue: Double get() = sellingPrice.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val costPriceValue: Double get() = costPrice.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val stockQtyValue: Double get() = stockQty.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
    val lowStockValue: Double get() = lowStockThreshold.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 0.0
}

fun Double.toDraftInput(): String = when {
    this <= 0.0 -> ""
    this == this.toLong().toDouble() -> this.toLong().toString()
    else -> this.toString()
}

/** Root (parent-less) master categories. */
val List<MasterCategoryDto>.rootCategories: List<MasterCategoryDto>
    get() = filter { it.parentId.isNullOrBlank() }

/**
 * Resolves a master item's category into (root, sub) ids. A [categoryId] that is itself a child
 * becomes the sub-category under its parent.
 */
fun List<MasterCategoryDto>.resolveCategory(categoryId: String?, subCategoryId: String?): Pair<String?, String?> {
    val id = categoryId?.takeIf { it.isNotBlank() } ?: return null to subCategoryId?.takeIf { it.isNotBlank() }
    val parent = firstOrNull { it.id == id }?.parentId?.takeIf { it.isNotBlank() }
    return if (parent != null) parent to id else id to subCategoryId?.takeIf { it.isNotBlank() }
}

/** Fixed shop bucket for a master category, used where the legacy enum is still required. */
fun List<MasterCategoryDto>.legacyCategory(categoryId: String?, fallback: ProductCategory): ProductCategory {
    val name = firstOrNull { it.id == categoryId }?.name ?: return fallback
    return ProductCategory.fromMasterCategoryName(name)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MasterCategoryPicker(
    categories: List<MasterCategoryDto>,
    categoryId: String?,
    subCategoryId: String?,
    onChange: (categoryId: String?, subCategoryId: String?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val roots = remember(categories) { categories.rootCategories }
    if (roots.isEmpty()) return
    val children = remember(categories, categoryId) {
        if (categoryId == null) emptyList() else categories.filter { it.parentId == categoryId }
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(Res.string.field_category), style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            roots.forEach { root ->
                FilterChip(
                    selected = categoryId == root.id,
                    onClick = { if (categoryId != root.id) onChange(root.id, null) },
                    label = { Text(root.name) },
                )
            }
        }
        if (children.isNotEmpty()) {
            Text(stringResource(Res.string.field_sub_category), style = MaterialTheme.typography.labelLarge)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                children.forEach { child ->
                    FilterChip(
                        selected = subCategoryId == child.id,
                        onClick = { onChange(categoryId, if (subCategoryId == child.id) null else child.id) },
                        label = { Text(child.name) },
                    )
                }
            }
        }
    }
}

/** Shop values (price, cost, stock, category) for a product being added or completed. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsSheet(
    title: String,
    draft: ProductDetailsDraft,
    categories: List<MasterCategoryDto>,
    confirmLabel: String,
    onDraftChange: ((ProductDetailsDraft) -> ProductDetailsDraft) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    subtitle: String? = null,
    priceRequired: Boolean = false,
    stockLabel: String = stringResource(Res.string.inv_opening_stock),
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding()
                .padding(start = 20.dp, end = 20.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            subtitle?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            OutlinedTextField(
                value = draft.sellingPrice,
                onValueChange = { v -> onDraftChange { it.copy(sellingPrice = v) } },
                label = { Text(stringResource(Res.string.field_selling_price)) },
                singleLine = true,
                isError = priceRequired && draft.sellingPriceValue <= 0.0,
                keyboardOptions = decimal,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = draft.costPrice,
                onValueChange = { v -> onDraftChange { it.copy(costPrice = v) } },
                label = { Text(stringResource(Res.string.field_cost_price)) },
                singleLine = true,
                keyboardOptions = decimal,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = draft.stockQty,
                    onValueChange = { v -> onDraftChange { it.copy(stockQty = v) } },
                    label = { Text(stockLabel) },
                    singleLine = true,
                    keyboardOptions = decimal,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = draft.lowStockThreshold,
                    onValueChange = { v -> onDraftChange { it.copy(lowStockThreshold = v) } },
                    label = { Text(stringResource(Res.string.field_low_stock_at)) },
                    singleLine = true,
                    keyboardOptions = decimal,
                    modifier = Modifier.weight(1f),
                )
            }
            MasterCategoryPicker(
                categories = categories,
                categoryId = draft.categoryId,
                subCategoryId = draft.subCategoryId,
                onChange = { cat, sub -> onDraftChange { it.copy(categoryId = cat, subCategoryId = sub) } },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                    Text(stringResource(Res.string.action_cancel))
                }
                Button(
                    onClick = onConfirm,
                    enabled = !priceRequired || draft.sellingPriceValue > 0.0,
                    modifier = Modifier.weight(1f),
                ) { Text(confirmLabel) }
            }
        }
    }
}
