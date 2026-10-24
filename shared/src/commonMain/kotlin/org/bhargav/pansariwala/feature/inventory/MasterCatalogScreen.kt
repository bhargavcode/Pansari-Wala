package org.bhargav.pansariwala.feature.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.draw.clip
import org.bhargav.pansariwala.designsystem.CatalogCascadeFilter
import org.bhargav.pansariwala.designsystem.DropdownOption
import org.bhargav.pansariwala.domain.model.ShopType
import org.bhargav.pansariwala.feature.user.shopTypeLabel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import org.bhargav.pansariwala.designsystem.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.api.MasterProductDto
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.feature.catalog.ProductDetailsSheet
import org.bhargav.pansariwala.i18n.asString
import org.bhargav.pansariwala.media.ProductImageCarousel
import org.bhargav.pansariwala.theme.stockHealthyColor
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asMoney
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_add
import pansariwala.shared.generated.resources.action_clear
import pansariwala.shared.generated.resources.inv_master_add_all
import pansariwala.shared.generated.resources.inv_master_add_title
import pansariwala.shared.generated.resources.inv_master_added
import pansariwala.shared.generated.resources.inv_master_count
import pansariwala.shared.generated.resources.inv_master_empty
import pansariwala.shared.generated.resources.inv_master_search_hint
import pansariwala.shared.generated.resources.inv_master_selected
import pansariwala.shared.generated.resources.inv_master_title
import org.bhargav.pansariwala.i18n.localizedBrand
import org.bhargav.pansariwala.i18n.localizedName

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
            val shopTypeOptions = state.shopTypes.map { DropdownOption(it.id, it.name) }
                .ifEmpty { ShopType.entries.map { DropdownOption(it.apiValue, shopTypeLabel(it)) } }
            CatalogCascadeFilter(
                shopTypes = shopTypeOptions,
                shopTypeId = state.selectedShopTypeId,
                onShopType = viewModel::onShopTypeSelected,
                categories = state.visibleCategories.map { DropdownOption(it.id, it.name) },
                categoryId = state.selectedCategoryId,
                onCategory = viewModel::onCategorySelected,
                subcategories = state.subcategories.map { DropdownOption(it.id, it.name) },
                subcategoryId = state.selectedSubcategoryId,
                onSubcategory = viewModel::onSubcategorySelected,
                fieldMinWidth = 150.dp,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
            )
            Text(
                text = stringResource(Res.string.inv_master_count, state.totalCount),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
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
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.items, key = { it.id }) { item ->
                        val shopProductId = state.addedIds[item.id]
                        MasterRow(
                            item = item,
                            added = shopProductId != null,
                            selected = item.id in state.selected,
                            onToggle = { viewModel.toggleSelected(item) },
                            onAdd = { viewModel.startAdd(item) },
                            onOpen = { shopProductId?.let(onOpenProduct) },
                        )
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
            if (state.selected.isNotEmpty()) {
                AddAllBar(
                    count = state.selected.size,
                    busy = state.addingAll,
                    onClear = viewModel::clearSelection,
                    onAddAll = viewModel::addAllSelected,
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }

    state.pending?.let { item ->
        ProductDetailsSheet(
            title = stringResource(Res.string.inv_master_add_title, item.localizedName),
            subtitle = item.localizedBrand.takeIf { it.isNotBlank() },
            draft = state.draft,
            confirmLabel = stringResource(Res.string.action_add),
            onDraftChange = viewModel::updateDraft,
            onConfirm = viewModel::confirmAdd,
            onDismiss = viewModel::cancelAdd,
        )
    }
}

@Composable
private fun AddAllBar(count: Int, busy: Boolean, onClear: () -> Unit, onAddAll: () -> Unit) {
    Surface(tonalElevation = 3.dp, shadowElevation = 6.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(Res.string.inv_master_selected, count),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onClear, enabled = !busy) { Text(stringResource(Res.string.action_clear)) }
            Button(onClick = onAddAll, enabled = !busy) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(Res.string.inv_master_add_all, count))
                }
            }
        }
    }
}

@Composable
private fun MasterRow(
    item: MasterProductDto,
    added: Boolean,
    selected: Boolean,
    onToggle: () -> Unit,
    onAdd: () -> Unit,
    onOpen: () -> Unit,
) {
    val accent = when {
        added -> stockHealthyColor
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.surfaceContainerLow,
            )
            .border(1.dp, accent.copy(alpha = if (added || selected) 0.7f else 0.5f), shape)
            .padding(start = 4.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Checkbox(
            checked = added || selected,
            onCheckedChange = if (added) null else { _ -> onToggle() },
            enabled = !added,
            colors = CheckboxDefaults.colors(
                checkedColor = if (added) stockHealthyColor else MaterialTheme.colorScheme.primary,
                disabledCheckedColor = stockHealthyColor,
            ),
        )
        ProductImageCarousel(
            imageUrls = item.imageUrls.take(1),
            height = 60.dp,
            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(12.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = item.localizedName,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.brandName.isNotBlank()) {
                Text(
                    item.localizedBrand,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (item.salePrice > 0.0) {
                Text(
                    text = item.salePrice.asMoney(),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        if (added) {
            FilledTonalButton(
                onClick = onOpen,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = stockHealthyColor.copy(alpha = 0.15f),
                    contentColor = stockHealthyColor,
                ),
            ) {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(Res.string.inv_master_added), modifier = Modifier.padding(start = 6.dp))
            }
        } else {
            Button(
                onClick = onAdd,
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
            ) {
                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(Res.string.action_add), modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}
