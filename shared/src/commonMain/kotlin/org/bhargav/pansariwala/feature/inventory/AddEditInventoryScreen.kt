package org.bhargav.pansariwala.feature.inventory

import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import org.bhargav.pansariwala.feature.catalog.ProductFinderResults
import org.bhargav.pansariwala.feature.catalog.ProductFinderActions
import pansariwala.shared.generated.resources.field_max_per_order_hint
import pansariwala.shared.generated.resources.field_max_per_order
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Layers
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import org.bhargav.pansariwala.designsystem.AppAlertDialog
import org.bhargav.pansariwala.designsystem.BarcodeField
import org.bhargav.pansariwala.designsystem.DatePickerField
import org.bhargav.pansariwala.designsystem.DiscardChangesDialog
import pansariwala.shared.generated.resources.field_expiry_date
import pansariwala.shared.generated.resources.field_mfg_date
import pansariwala.shared.generated.resources.product_variant_mode_has
import pansariwala.shared.generated.resources.product_variant_mode_has_hint
import pansariwala.shared.generated.resources.product_variant_mode_none
import pansariwala.shared.generated.resources.product_variant_mode_none_hint
import pansariwala.shared.generated.resources.product_variant_mode_title
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import org.bhargav.pansariwala.designsystem.OutlinedTextField
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
import org.bhargav.pansariwala.feature.catalog.ProductCategoryPicker
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.domain.model.ProductUnit
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.i18n.localizedLabel
import org.bhargav.pansariwala.ui.toErrorBanner
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.product_draft_message
import pansariwala.shared.generated.resources.product_draft_resume
import pansariwala.shared.generated.resources.product_draft_start_new
import pansariwala.shared.generated.resources.product_draft_title
import pansariwala.shared.generated.resources.action_find
import pansariwala.shared.generated.resources.add_product
import pansariwala.shared.generated.resources.field_barcode
import pansariwala.shared.generated.resources.field_cost_price
import pansariwala.shared.generated.resources.field_low_stock_at
import pansariwala.shared.generated.resources.field_name
import pansariwala.shared.generated.resources.field_name_hi
import pansariwala.shared.generated.resources.field_selling_price
import pansariwala.shared.generated.resources.field_stock_qty
import pansariwala.shared.generated.resources.field_unit
import pansariwala.shared.generated.resources.field_voice_alias
import pansariwala.shared.generated.resources.inv_label_brand
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
    var confirmDiscard by remember { mutableStateOf(false) }
    val guardedBack = { if (viewModel.hasUnsavedChanges()) confirmDiscard = true else onBack() }
    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = !state.saved,
        onBackCompleted = guardedBack,
    )
    if (state.pendingDraft != null) {
        ResumeProductDraftDialog(
            onResume = viewModel::resumeDraft,
            onStartNew = viewModel::discardDraft,
        )
    }
    if (confirmDiscard) {
        DiscardChangesDialog(
            onDiscard = {
                confirmDiscard = false
                viewModel.abandonUnsavedWork()
                onBack()
            },
            onKeepEditing = { confirmDiscard = false },
        )
    }

    PansariScreen(
        title = stringResource(if (state.isEditing) Res.string.update_product else Res.string.add_product),
        onBack = guardedBack,
        error = state.error.toErrorBanner(retryable = false),
        onErrorAction = { handleErrorBannerAction(it, onRetry = {}, onDismiss = viewModel::dismissError) },
        isLoading = state.loading,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            org.bhargav.pansariwala.media.ProductImageSlots(
                slots = state.imageSlots,
                feature = org.bhargav.pansariwala.media.ImageUploadFeature.INVENTORY_PRODUCT,
                onAdd = viewModel::addImage,
                onRetry = viewModel::retryImage,
                onClear = viewModel::clearImage,
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
                trailingIcon = { ProductFinderActions(viewModel.finder) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { viewModel.onLookup() }, onDone = { viewModel.onLookup() }),
                modifier = Modifier.weight(1f),
            )
            Button(onClick = viewModel::onLookup) {
                Text(stringResource(Res.string.action_find))
            }
        }
        ProductFinderResults(finder = viewModel.finder, onPick = { viewModel.pickFound(it.product) })

        state.message?.let {
            Text(
                it.asString(),
                color = MaterialTheme.colorScheme.primary,
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
        OutlinedTextField(
            value = state.brandName,
            onValueChange = viewModel::onBrandNameChange,
            label = { Text(stringResource(Res.string.inv_label_brand)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        ProductTranslationsSection(
            state = state,
            onToggleLanguage = viewModel::onTranslationLanguageToggle,
            onNameChange = viewModel::onNameTranslationChange,
            onBrandChange = viewModel::onBrandTranslationChange,
            modifier = Modifier.fillMaxWidth(),
        )

        ProductCategoryPicker(
            categoryId = state.categoryId,
            subCategoryId = state.subCategoryId,
            onChange = viewModel::onMasterCategoryChange,
            modifier = Modifier.fillMaxWidth(),
        )
        EnumDropdown(
            label = stringResource(Res.string.field_unit),
            selectedLabel = state.unit.localizedLabel(),
            options = ProductUnit.entries,
            optionLabel = { it.localizedLabel() },
            onSelected = viewModel::onUnitChange,
        )

        VariantModeSelector(hasVariants = state.hasVariants, onChange = viewModel::onHasVariantsChange)

        if (state.hasVariants) {
            org.bhargav.pansariwala.feature.catalog.VariantListEditor(
                variants = state.variants,
                onChange = viewModel::onVariantsChange,
                onPickImage = viewModel::uploadVariantImage,
                modifier = Modifier.fillMaxWidth(),
            )
        } else {
        BarcodeField(
            value = state.barcode,
            onValueChange = viewModel::onBarcodeChange,
            label = stringResource(Res.string.field_barcode),
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
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DatePickerField(
                label = stringResource(Res.string.field_mfg_date),
                millis = state.mfgDate,
                onChange = viewModel::onMfgDateChange,
                modifier = Modifier.weight(1f),
            )
            DatePickerField(
                label = stringResource(Res.string.field_expiry_date),
                millis = state.expiryDate,
                onChange = viewModel::onExpiryDateChange,
                isError = state.expiryDate != null && state.mfgDate != null && state.expiryDate!! < state.mfgDate!!,
                modifier = Modifier.weight(1f),
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
            value = state.maxPerOrder,
            onValueChange = viewModel::onMaxPerOrderChange,
            label = { Text(stringResource(Res.string.field_max_per_order)) },
            supportingText = { Text(stringResource(Res.string.field_max_per_order_hint)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth(),
        )
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VariantModeSelector(hasVariants: Boolean, onChange: (Boolean) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(Res.string.product_variant_mode_title), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
            listOf(false to Res.string.product_variant_mode_none, true to Res.string.product_variant_mode_has)
                .forEachIndexed { i, (value, label) ->
                    SegmentedButton(
                        selected = hasVariants == value,
                        onClick = { onChange(value) },
                        shape = SegmentedButtonDefaults.itemShape(i, 2),
                        icon = { Icon(if (value) Icons.Default.Layers else Icons.Default.Inventory2, null) },
                    ) { Text(stringResource(label)) }
                }
        }
        Text(
            stringResource(if (hasVariants) Res.string.product_variant_mode_has_hint else Res.string.product_variant_mode_none_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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

@Composable
private fun ResumeProductDraftDialog(onResume: () -> Unit, onStartNew: () -> Unit) {
    AppAlertDialog(
        onDismissRequest = onResume,
        title = { Text(stringResource(Res.string.product_draft_title)) },
        text = { Text(stringResource(Res.string.product_draft_message)) },
        confirmButton = {
            TextButton(onClick = onResume) { Text(stringResource(Res.string.product_draft_resume)) }
        },
        dismissButton = {
            TextButton(onClick = onStartNew) { Text(stringResource(Res.string.product_draft_start_new)) }
        },
    )
}
