package org.bhargav.pansariwala.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.PansariTopBar
import org.bhargav.pansariwala.domain.model.ProductCategory
import org.bhargav.pansariwala.domain.model.ProductUnit
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.i18n.localizedLabel
import org.bhargav.pansariwala.i18n.localizedName
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_find
import pansariwala.shared.generated.resources.add_product
import pansariwala.shared.generated.resources.field_barcode
import pansariwala.shared.generated.resources.field_category
import pansariwala.shared.generated.resources.field_cost_price
import pansariwala.shared.generated.resources.field_low_stock_at
import pansariwala.shared.generated.resources.field_name
import pansariwala.shared.generated.resources.field_name_hi
import pansariwala.shared.generated.resources.field_selling_price
import pansariwala.shared.generated.resources.field_stock_qty
import pansariwala.shared.generated.resources.field_unit
import pansariwala.shared.generated.resources.field_voice_alias
import pansariwala.shared.generated.resources.inventory_lookup_hint
import pansariwala.shared.generated.resources.product_id_or_barcode
import pansariwala.shared.generated.resources.update_product

@Composable
fun AddEditInventoryScreen(
    productId: String?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: AddEditInventoryViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(productId) { viewModel.load(productId) }
    LaunchedEffect(state.saved) { if (state.saved) onSaved() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PansariTopBar(
            title = stringResource(if (state.isEditing) Res.string.update_product else Res.string.add_product),
            onBack = onBack,
        )

        Text(
            text = stringResource(Res.string.inventory_lookup_hint),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = state.lookupQuery,
                onValueChange = viewModel::onLookupQueryChange,
                label = { Text(stringResource(Res.string.product_id_or_barcode)) },
                singleLine = true,
                modifier = Modifier.weight(1f),
            )
            Button(onClick = viewModel::onLookup) {
                Text(stringResource(Res.string.action_find))
            }
        }

        state.message?.let {
            Text(
                it.asString(),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        state.error?.let {
            Text(
                it.asString(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        OutlinedTextField(
            value = state.name,
            onValueChange = viewModel::onNameChange,
            label = { Text(stringResource(Res.string.field_name)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = state.nameHi,
            onValueChange = viewModel::onNameHiChange,
            label = { Text(stringResource(Res.string.field_name_hi)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        EnumDropdown(
            label = stringResource(Res.string.field_category),
            selectedLabel = state.category.localizedName(),
            options = ProductCategory.entries,
            optionLabel = { it.localizedName() },
            onSelected = viewModel::onCategoryChange,
        )
        EnumDropdown(
            label = stringResource(Res.string.field_unit),
            selectedLabel = state.unit.localizedLabel(),
            options = ProductUnit.entries,
            optionLabel = { it.localizedLabel() },
            onSelected = viewModel::onUnitChange,
        )

        OutlinedTextField(
            value = state.barcode,
            onValueChange = viewModel::onBarcodeChange,
            label = { Text(stringResource(Res.string.field_barcode)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.sellingPrice,
                onValueChange = viewModel::onSellingPriceChange,
                label = { Text(stringResource(Res.string.field_selling_price)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = state.costPrice,
                onValueChange = viewModel::onCostPriceChange,
                label = { Text(stringResource(Res.string.field_cost_price)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedTextField(
                value = state.stockQty,
                onValueChange = viewModel::onStockQtyChange,
                label = { Text(stringResource(Res.string.field_stock_qty)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = state.lowStockThreshold,
                onValueChange = viewModel::onThresholdChange,
                label = { Text(stringResource(Res.string.field_low_stock_at)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f),
            )
        }
        OutlinedTextField(
            value = state.voiceAlias,
            onValueChange = viewModel::onVoiceAliasChange,
            label = { Text(stringResource(Res.string.field_voice_alias)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Button(
            onClick = viewModel::onSave,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                stringResource(
                    if (state.isEditing) Res.string.update_product else Res.string.add_product,
                ),
            )
        }
    }
}

@Composable
private fun <T> EnumDropdown(
    label: String,
    selectedLabel: String,
    options: List<T>,
    optionLabel: @Composable (T) -> String,
    onSelected: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Box {
            OutlinedButton(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth().widthIn(min = 0.dp),
            ) {
                Text(selectedLabel, modifier = Modifier.weight(1f))
                Text("▾")
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}
