package org.bhargav.pansariwala.feature.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductStatusFilter
import org.bhargav.pansariwala.feature.catalog.ProductSheetImportDialog
import org.bhargav.pansariwala.feature.printer.PrintAction
import org.bhargav.pansariwala.feature.printer.PrintMenuButton
import org.bhargav.pansariwala.i18n.localizedLabel
import org.bhargav.pansariwala.media.ProductImageCarousel
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_clear
import pansariwala.shared.generated.resources.category_price
import pansariwala.shared.generated.resources.inv_add_custom
import pansariwala.shared.generated.resources.inv_add_from_master
import pansariwala.shared.generated.resources.inv_badge_inactive
import pansariwala.shared.generated.resources.inv_empty
import pansariwala.shared.generated.resources.inv_filter_all_categories
import pansariwala.shared.generated.resources.inv_filter_low_stock
import pansariwala.shared.generated.resources.inv_products_title
import pansariwala.shared.generated.resources.inv_search_hint
import pansariwala.shared.generated.resources.inv_showing_count
import pansariwala.shared.generated.resources.inv_status_active
import pansariwala.shared.generated.resources.inv_status_all
import pansariwala.shared.generated.resources.inv_status_inactive
import pansariwala.shared.generated.resources.print_action_inventory
import pansariwala.shared.generated.resources.print_action_labels_all
import pansariwala.shared.generated.resources.print_action_low_stock
import pansariwala.shared.generated.resources.sheet_import_action
import pansariwala.shared.generated.resources.cat_order_title

@Composable
fun InventoryListScreen(
    lowStockOnly: Boolean,
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
    onAddCustomProduct: () -> Unit,
    onAddFromMaster: () -> Unit,
    viewModel: InventoryListViewModel = koinViewModel(),
    sheetViewModel: ShopSheetImportViewModel = koinViewModel(),
    categoryOrderViewModel: CategoryOrderViewModel = koinViewModel(),
) {
    val categoryOrder by categoryOrderViewModel.state.collectAsStateWithLifecycle()
    categoryOrder?.let {
        CategoryOrderDialog(
            state = it,
            onSortChange = categoryOrderViewModel::onSortChange,
            onPriorityChange = categoryOrderViewModel::onPriorityChange,
            onSave = categoryOrderViewModel::save,
            onDismiss = categoryOrderViewModel::dismiss,
        )
    }
    LaunchedEffect(lowStockOnly) { viewModel.start(lowStockOnly) }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState by sheetViewModel.state.collectAsStateWithLifecycle()
    val sheetUrl by sheetViewModel.openUrl.collectAsStateWithLifecycle()
    val uriHandler = LocalUriHandler.current
    LaunchedEffect(sheetUrl) {
        sheetUrl?.let { url ->
            runCatching { uriHandler.openUri(url) }
            sheetViewModel.urlOpened()
        }
    }
    sheetState?.let {
        ProductSheetImportDialog(
            state = it,
            onDownloadSample = sheetViewModel::downloadSample,
            onExport = sheetViewModel::export,
            onPickFile = sheetViewModel::pickFile,
            onApply = sheetViewModel::apply,
            onDismiss = sheetViewModel::dismiss,
        )
    }
    val filter = state.filter
    val products = state.products
    val categoryNames = remember(state.categories) { state.categoryNames }

    PansariScreen(
        title = stringResource(Res.string.inv_products_title),
        onBack = onBack,
        error = state.error.toErrorBanner(retryable = false),
        isLoading = state.loading,
        onErrorAction = { handleErrorBannerAction(it, onRetry = {}, onDismiss = viewModel::dismissError) },
        actions = {
            IconButton(onClick = onAddCustomProduct) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(Res.string.inv_add_custom))
            }
            IconButton(onClick = sheetViewModel::open) {
                Icon(Icons.Filled.UploadFile, contentDescription = stringResource(Res.string.sheet_import_action))
            }
            IconButton(onClick = categoryOrderViewModel::open) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(Res.string.cat_order_title))
            }
            PrintMenuButton(
                actions = remember(products, filter.lowStockOnly) {
                    listOf(
                        if (filter.lowStockOnly) {
                            PrintAction(Res.string.print_action_low_stock) { printInventory(lowStockOnly = true) }
                        } else {
                            PrintAction(Res.string.print_action_inventory) { printInventory(lowStockOnly = false) }
                        },
                        PrintAction(Res.string.print_action_labels_all) { printLabels(products) },
                    )
                },
            )
        },
    ) {
        val listState = rememberLazyListState()
        val nearEnd by remember {
            derivedStateOf {
                val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                last >= listState.layoutInfo.totalItemsCount - AppConstants.LIST_PREFETCH_DISTANCE
            }
        }
        LaunchedEffect(nearEnd, products.size) { if (nearEnd) viewModel.loadMore() }
        LaunchedEffect(filter) { listState.scrollToItem(0) }

        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = state.searchText,
                onValueChange = viewModel::onSearchChange,
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp),
                singleLine = true,
                placeholder = { Text(stringResource(Res.string.inv_search_hint)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.searchText.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = stringResource(Res.string.action_clear))
                        }
                    }
                },
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "all") {
                    FilterChip(
                        selected = filter.categoryId == null,
                        onClick = { viewModel.onCategorySelected(null) },
                        label = { Text(stringResource(Res.string.inv_filter_all_categories)) },
                    )
                }
                items(state.rootCategories, key = { it.id }) { category ->
                    FilterChip(
                        selected = filter.categoryId == category.id,
                        onClick = { viewModel.onCategorySelected(category.id) },
                        label = { Text(category.name) },
                    )
                }
            }
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(ProductStatusFilter.entries, key = { it.name }) { status ->
                    FilterChip(
                        selected = filter.status == status,
                        onClick = { viewModel.onStatusSelected(status) },
                        label = { Text(stringResource(status.label())) },
                    )
                }
                item(key = "low") {
                    FilterChip(
                        selected = filter.lowStockOnly,
                        onClick = viewModel::onLowStockToggle,
                        label = { Text(stringResource(Res.string.inv_filter_low_stock)) },
                    )
                }
            }
            Text(
                text = stringResource(Res.string.inv_showing_count, products.size, state.totalCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            Box(Modifier.weight(1f).fillMaxWidth()) {
                if (!state.loading && products.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.inv_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    )
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                ) {
                    items(products, key = { it.id }) { product ->
                        ShopProductRow(
                            product,
                            categoryName = categoryNames[product.subCategoryId ?: product.categoryId].orEmpty(),
                            onClick = { onOpenProduct(product.id) },
                        )
                        HorizontalDivider()
                    }
                    if (state.hasMore) {
                        item(key = "more") {
                            Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                            }
                        }
                    }
                }
                ExtendedFloatingActionButton(
                    onClick = onAddFromMaster,
                    icon = { Icon(Icons.Filled.LibraryAdd, contentDescription = null) },
                    text = { Text(stringResource(Res.string.inv_add_from_master)) },
                    modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                )
            }
        }
    }
}

private fun ProductStatusFilter.label() = when (this) {
    ProductStatusFilter.ALL -> Res.string.inv_status_all
    ProductStatusFilter.ACTIVE -> Res.string.inv_status_active
    ProductStatusFilter.INACTIVE -> Res.string.inv_status_inactive
}

@Composable
private fun ShopProductRow(product: Product, categoryName: String, onClick: () -> Unit) {
    val level = product.stockLevel
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProductImageCarousel(
            imageUrls = listOfNotNull(product.primaryImageUrl),
            height = 64.dp,
            modifier = Modifier.size(64.dp),
        )
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (!product.isActive) InactiveTag()
            }
            if (product.brandName.isNotBlank()) {
                Text(
                    text = product.brandName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                )
            }
            Text(
                text = if (categoryName.isBlank()) {
                    product.sellingPrice.asMoney()
                } else {
                    stringResource(Res.string.category_price, categoryName, product.sellingPrice.asMoney())
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            StockGauge(
                stockQty = product.stockQty,
                lowStockThreshold = product.lowStockThreshold,
                modifier = Modifier.padding(top = 6.dp).fillMaxWidth(0.7f),
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "${product.stockQty.asQuantity()} ${product.unit.localizedLabel()}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = level.color(),
            )
            StockBadge(level)
        }
    }
}

@Composable
internal fun InactiveTag() {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Text(
            text = stringResource(Res.string.inv_badge_inactive),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
    }
}
