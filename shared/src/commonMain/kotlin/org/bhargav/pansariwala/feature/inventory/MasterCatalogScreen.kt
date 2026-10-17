package org.bhargav.pansariwala.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.api.MasterProductDto
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.domain.model.ProductCategory
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.i18n.localizedName
import org.bhargav.pansariwala.media.ProductImageCarousel
import org.bhargav.pansariwala.theme.stockHealthyColor
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_add
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.action_clear
import pansariwala.shared.generated.resources.field_category
import pansariwala.shared.generated.resources.field_cost_price
import pansariwala.shared.generated.resources.field_low_stock_at
import pansariwala.shared.generated.resources.field_selling_price
import pansariwala.shared.generated.resources.inv_master_add_title
import pansariwala.shared.generated.resources.inv_master_added
import pansariwala.shared.generated.resources.inv_master_all
import pansariwala.shared.generated.resources.inv_master_count
import pansariwala.shared.generated.resources.inv_master_empty
import pansariwala.shared.generated.resources.inv_master_search_hint
import pansariwala.shared.generated.resources.inv_master_title
import pansariwala.shared.generated.resources.inv_opening_stock

@Composable
fun MasterCatalogScreen(
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
    viewModel: MasterCatalogViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val message = state.message?.asString()
    LaunchedEffect(message) {
        if (message != null) {
            snackbar.showSnackbar(message)
            viewModel.dismissMessage()
        }
    }

    PansariScreen(
        title = stringResource(Res.string.inv_master_title),
        onBack = onBack,
        error = state.error.toErrorBanner(retryable = true),
        onErrorAction = { handleErrorBannerAction(it, onRetry = viewModel::retry, onDismiss = viewModel::dismissError) },
    ) {
        val listState = rememberLazyListState()
        val nearEnd by remember {
            derivedStateOf {
                val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                last >= listState.layoutInfo.totalItemsCount - AppConstants.LIST_PREFETCH_DISTANCE
            }
        }
        LaunchedEffect(nearEnd, state.items.size) { if (nearEnd) viewModel.loadMore() }

        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = state.searchText,
                onValueChange = viewModel::onSearchChange,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
                singleLine = true,
                placeholder = { Text(stringResource(Res.string.inv_master_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchText.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(Res.string.action_clear))
                        }
                    }
                },
            )
            if (state.categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "all") {
                        FilterChip(
                            selected = state.selectedCategoryId == null,
                            onClick = { viewModel.onCategorySelected(null) },
                            label = { Text(stringResource(Res.string.inv_master_all)) },
                        )
                    }
                    items(state.categories, key = { it.id }) { category ->
                        FilterChip(
                            selected = state.selectedCategoryId == category.id,
                            onClick = { viewModel.onCategorySelected(category.id) },
                            label = { Text(category.name) },
                        )
                    }
                }
            }
            Text(
                text = stringResource(Res.string.inv_master_count, state.totalCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                    state.items.isEmpty() -> Text(
                        text = stringResource(Res.string.inv_master_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        val shopProductId = state.addedIds[item.id]
                        MasterRow(
                            item = item,
                            added = shopProductId != null,
                            onAdd = { viewModel.startAdd(item) },
                            onOpen = { shopProductId?.let(onOpenProduct) },
                        )
                        HorizontalDivider()
                    }
                    if (state.loadingMore) {
                        item(key = "more") {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    state.pending?.let { item ->
        AddFromMasterDialog(
            item = item,
            draft = state.draft,
            onDraftChange = viewModel::updateDraft,
            onConfirm = viewModel::confirmAdd,
            onDismiss = viewModel::cancelAdd,
        )
    }
}

@Composable
private fun MasterRow(item: MasterProductDto, added: Boolean, onAdd: () -> Unit, onOpen: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProductImageCarousel(
            imageUrls = item.imageUrls.take(1),
            height = 56.dp,
            modifier = Modifier.size(56.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.brandName.isNotBlank()) {
                Text(item.brandName, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (item.salePrice > 0.0) {
                Text(
                    text = item.salePrice.asMoney(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (added) {
            TextButton(onClick = onOpen) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = stockHealthyColor)
                Text(
                    text = stringResource(Res.string.inv_master_added),
                    color = stockHealthyColor,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        } else {
            FilledTonalButton(onClick = onAdd) { Text(stringResource(Res.string.action_add)) }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddFromMasterDialog(
    item: MasterProductDto,
    draft: MasterAddDraft,
    onDraftChange: ((MasterAddDraft) -> MasterAddDraft) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val decimal = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(Res.string.inv_master_add_title, item.name)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = draft.sellingPrice,
                    onValueChange = { v -> onDraftChange { it.copy(sellingPrice = v) } },
                    label = { Text(stringResource(Res.string.field_selling_price)) },
                    singleLine = true,
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
                OutlinedTextField(
                    value = draft.stockQty,
                    onValueChange = { v -> onDraftChange { it.copy(stockQty = v) } },
                    label = { Text(stringResource(Res.string.inv_opening_stock)) },
                    singleLine = true,
                    keyboardOptions = decimal,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = draft.lowStockThreshold,
                    onValueChange = { v -> onDraftChange { it.copy(lowStockThreshold = v) } },
                    label = { Text(stringResource(Res.string.field_low_stock_at)) },
                    singleLine = true,
                    keyboardOptions = decimal,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(Res.string.field_category), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ProductCategory.entries.forEach { category ->
                        FilterChip(
                            selected = draft.category == category,
                            onClick = { onDraftChange { it.copy(category = category) } },
                            label = { Text(category.localizedName()) },
                        )
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(Res.string.action_add)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}
