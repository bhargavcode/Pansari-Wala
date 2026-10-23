package org.bhargav.pansariwala.feature.user

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import org.bhargav.pansariwala.designsystem.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.domain.model.CatalogCategory
import org.bhargav.pansariwala.domain.model.CatalogSection
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.domain.model.ProductDetail
import org.bhargav.pansariwala.i18n.localizedLabel
import org.bhargav.pansariwala.media.NetworkImage
import org.bhargav.pansariwala.media.ProductImageCarousel
import org.bhargav.pansariwala.ui.AsyncUiState
import org.bhargav.pansariwala.ui.errorBannerOrNull
import org.bhargav.pansariwala.ui.isBlockingLoad
import org.bhargav.pansariwala.ui.isRefreshing
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.catalog_category_other
import pansariwala.shared.generated.resources.catalog_end_of_list
import pansariwala.shared.generated.resources.catalog_in_stock
import pansariwala.shared.generated.resources.catalog_low_stock
import pansariwala.shared.generated.resources.catalog_no_products
import pansariwala.shared.generated.resources.catalog_out_of_stock
import pansariwala.shared.generated.resources.catalog_per_unit
import pansariwala.shared.generated.resources.catalog_product_count
import pansariwala.shared.generated.resources.catalog_search_all_hint
import pansariwala.shared.generated.resources.catalog_search_in_category
import pansariwala.shared.generated.resources.catalog_show_more
import pansariwala.shared.generated.resources.catalog_tab_products
import pansariwala.shared.generated.resources.field_barcode
import pansariwala.shared.generated.resources.field_category
import pansariwala.shared.generated.resources.field_unit
import pansariwala.shared.generated.resources.master_brand
import pansariwala.shared.generated.resources.master_company
import pansariwala.shared.generated.resources.master_description
import pansariwala.shared.generated.resources.master_dimensions
import pansariwala.shared.generated.resources.master_general_info
import pansariwala.shared.generated.resources.master_sku
import pansariwala.shared.generated.resources.master_tags
import pansariwala.shared.generated.resources.master_variants
import pansariwala.shared.generated.resources.master_weight
import pansariwala.shared.generated.resources.product_add_to_cart
import pansariwala.shared.generated.resources.product_detail_title
import pansariwala.shared.generated.resources.price_trend_title
import org.bhargav.pansariwala.feature.reports.PriceTrendChart
import pansariwala.shared.generated.resources.product_weight_kg
import pansariwala.shared.generated.resources.user_cart_items
import pansariwala.shared.generated.resources.variant_in_cart
import org.bhargav.pansariwala.i18n.localizedName

private const val PRODUCT_COUNT_ROUNDING = 100

internal fun formatProductCount(count: Int): String =
    if (count < PRODUCT_COUNT_ROUNDING) count.toString()
    else "${count / PRODUCT_COUNT_ROUNDING * PRODUCT_COUNT_ROUNDING}+"

@Composable
internal fun rememberCartLines(cart: CartStore): List<CartStore.Line> {
    val lines by cart.lines.collectAsStateWithLifecycle()
    return lines
}

@Composable
internal fun CatalogProductRow(
    product: Product,
    lines: List<CartStore.Line>,
    cart: CartStore,
    picker: VariantPickerState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxWidth()) {
        product.options().forEach { option ->
            CatalogOptionRow(option = option, lines = lines, cart = cart, picker = picker, onClick = onClick)
        }
    }
}

/** One variant (or a variant-less product) as its own list card. */
@Composable
private fun CatalogOptionRow(
    option: PurchaseOption,
    lines: List<CartStore.Line>,
    cart: CartStore,
    picker: VariantPickerState,
    onClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NetworkImage(
            url = option.imageUrl,
            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
            contentDescription = option.displayName,
        )
        Column(Modifier.weight(1f)) {
            Text(option.displayName, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            OptionPriceSummary(option)
            if (!option.inStock) {
                Text(
                    stringResource(Res.string.catalog_out_of_stock),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        OptionCartAction(option = option, lines = lines, cart = cart, picker = picker)
    }
}

@Composable
internal fun CatalogCategory.displayName(): String =
    if (isUncategorized) stringResource(Res.string.catalog_category_other) else name

@Composable
internal fun CatalogSectionHeader(
    category: CatalogCategory,
    totalCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            category.displayName(),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            stringResource(Res.string.catalog_product_count, formatProductCount(totalCount)),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}

@Composable
internal fun CatalogCategoryChips(
    sections: List<CatalogSection>,
    onClick: (CatalogCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(sections, key = { it.category.id }) { section ->
            AssistChip(
                onClick = { onClick(section.category) },
                label = { Text("${section.category.displayName()} · ${formatProductCount(section.totalCount)}") },
            )
        }
    }
}

/** Category header, up to [CatalogSection.preview] items, then "Show more" when the category has more. */
internal fun LazyListScope.catalogSectionItems(
    sections: List<CatalogSection>,
    lines: List<CartStore.Line>,
    keyPrefix: String,
    onOpenCategory: (CatalogCategory) -> Unit,
    onOpenProduct: (String) -> Unit,
    cart: CartStore,
    picker: VariantPickerState,
) {
    sections.forEach { section ->
        item(key = "$keyPrefix-h-${section.category.id}", contentType = "section_header") {
            CatalogSectionHeader(
                category = section.category,
                totalCount = section.totalCount,
                onClick = { onOpenCategory(section.category) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        items(
            section.preview,
            key = { "$keyPrefix-p-${section.category.id}-${it.id}" },
            contentType = { "product" },
        ) { product ->
            CatalogProductRow(
                product = product,
                lines = lines,
                cart = cart,
                picker = picker,
                onClick = { onOpenProduct(product.id) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        if (section.totalCount > section.preview.size) {
            item(key = "$keyPrefix-m-${section.category.id}", contentType = "show_more") {
                TextButton(
                    onClick = { onOpenCategory(section.category) },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                ) {
                    Text(stringResource(Res.string.catalog_show_more))
                }
            }
        }
        item(key = "$keyPrefix-d-${section.category.id}", contentType = "divider") {
            HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
        }
    }
}

@Composable
internal fun CatalogCartBar(cartCount: Int, onOpenCart: () -> Unit) {
    if (cartCount <= 0) return
    UserPrimaryButton(
        text = stringResource(Res.string.user_cart_items, cartCount),
        onClick = onOpenCart,
        modifier = Modifier.padding(16.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShopCategoryScreen(
    shopId: String,
    category: CatalogCategory?,
    initialQuery: String,
    onOpenProduct: (String) -> Unit,
    onOpenCart: () -> Unit,
    onBack: () -> Unit,
    viewModel: CategoryProductsViewModel = koinViewModel(),
    cart: CartStore = koinInject(),
) {
    LifecycleResumeEffect(shopId, category?.id) {
        viewModel.start(shopId, category?.id, initialQuery)
        onPauseOrDispose { }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val lines = rememberCartLines(cart)
    val cartCount = CartStore.countItems(lines)
    val picker = rememberVariantPicker()
    VariantPickerHost(picker, cart)
    val listState = rememberLazyListState()
    val categoryName = category?.displayName()

    LaunchedEffect(listState) {
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }.distinctUntilChanged().collect { (lastVisible, total) ->
            if (total > 0 && lastVisible >= total - 1 - AppConstants.CATALOG_PREFETCH_DISTANCE) {
                viewModel.loadNextPage()
            }
        }
    }

    PansariScreen(
        title = categoryName ?: stringResource(Res.string.catalog_tab_products),
        onBack = onBack,
        error = state.error.toErrorBanner(),
        onErrorAction = { handleErrorBannerAction(it, onRetry = viewModel::retry, onDismiss = viewModel::dismissError) },
        isLoading = state.loadingInitial && state.items.isEmpty(),
    ) {
        Column(Modifier.fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                label = {
                    Text(
                        if (categoryName != null) stringResource(Res.string.catalog_search_in_category, categoryName)
                        else stringResource(Res.string.catalog_search_all_hint),
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                singleLine = true,
            )
            if (state.totalCount > 0) {
                Text(
                    stringResource(Res.string.catalog_product_count, state.totalCount.toString()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
          PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = viewModel::pullRefresh,
            modifier = Modifier.weight(1f),
          ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(state.items, key = { it.id }, contentType = { "product" }) { product ->
                    CatalogProductRow(
                        product = product,
                        lines = lines,
                        cart = cart,
                        picker = picker,
                        onClick = { onOpenProduct(product.id) },
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                when {
                    state.loadingMore -> item(key = "loading_more", contentType = "footer") {
                        Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    }
                    state.items.isEmpty() && !state.loadingInitial && state.error == null ->
                        item(key = "empty", contentType = "footer") {
                            Text(
                                stringResource(Res.string.catalog_no_products),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp),
                            )
                        }
                    !state.hasMore && state.items.isNotEmpty() -> item(key = "end", contentType = "footer") {
                        Text(
                            stringResource(Res.string.catalog_end_of_list),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                        )
                    }
                }
            }
          }
            CatalogCartBar(cartCount = cartCount, onOpenCart = onOpenCart)
        }
    }
}

@Composable
fun ShopProductDetailScreen(
    shopId: String,
    productId: String,
    onOpenCart: () -> Unit,
    onBack: () -> Unit,
    viewModel: ProductDetailViewModel = koinViewModel(),
    cart: CartStore = koinInject(),
) {
    LifecycleResumeEffect(shopId, productId) {
        viewModel.load(shopId, productId, force = true)
        onPauseOrDispose { }
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val detail = (state as? AsyncUiState.Success)?.data
    val lines = rememberCartLines(cart)
    val picker = rememberVariantPicker()
    VariantPickerHost(picker, cart)

    PansariScreen(
        title = detail?.product?.name ?: stringResource(Res.string.product_detail_title),
        onBack = onBack,
        error = state.errorBannerOrNull(),
        onErrorAction = { handleErrorBannerAction(it, onRetry = viewModel::retry, onDismiss = viewModel::dismissError) },
        isLoading = state.isBlockingLoad(),
        isRefreshing = state.isRefreshing(),
    ) {
        if (detail != null) {
            Column(Modifier.fillMaxSize()) {
                ProductDetailContent(
                    detail = detail,
                    lines = lines,
                    cart = cart,
                    picker = picker,
                    modifier = Modifier.weight(1f),
                )
                CatalogCartBar(cartCount = CartStore.countItems(lines), onOpenCart = onOpenCart)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProductDetailContent(
    detail: ProductDetail,
    lines: List<CartStore.Line>,
    cart: CartStore,
    picker: VariantPickerState,
    modifier: Modifier = Modifier,
) {
    val product = detail.product
    val options = product.options()
    val outOfStock = options.none { it.inStock }
    val productLines = lines.filter { it.product.id == product.id }
    Column(
        modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProductImageCarousel(
            imageUrls = (product.imageUrls + product.variants.flatMap { it.imageUrls }).distinct(),
            modifier = Modifier.fillMaxWidth(),
            height = 260.dp,
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(product.localizedName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            if (product.nameHi.isNotBlank()) {
                Text(product.nameHi, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            val maker = listOf(detail.brandName, detail.companyName).filter { it.isNotBlank() }.distinct()
            if (maker.isNotEmpty()) {
                Text(maker.joinToString(" · "), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
        }
        ProductPriceSummary(product)
        Text(
            when {
                outOfStock -> stringResource(Res.string.catalog_out_of_stock)
                !product.hasVariants && product.isLowStock ->
                    stringResource(Res.string.catalog_low_stock, product.stockQty.asQuantity())
                else -> stringResource(Res.string.catalog_in_stock)
            },
            style = MaterialTheme.typography.labelLarge,
            color = if (outOfStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
        )
        SectionCard(title = stringResource(Res.string.price_trend_title)) {
            PriceTrendChart(
                history = detail.priceHistory,
                currentPrice = options.minOfOrNull { it.unitPrice } ?: product.sellingPrice,
            )
        }
        if (options.size > 1) {
            SectionCard(title = stringResource(Res.string.master_variants)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    options.forEach { option ->
                        val inCart = productLines.firstOrNull { it.key == option.key }
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                            NetworkImage(
                                url = option.imageUrl,
                                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(6.dp)),
                                contentDescription = option.variant?.label,
                            )
                            Column(Modifier.weight(1f).padding(horizontal = 10.dp)) {
                                Text(option.variant?.label.orEmpty(), fontWeight = FontWeight.Medium)
                                OptionPriceSummary(option)
                                if (inCart != null) {
                                    Text(
                                        stringResource(Res.string.variant_in_cart, option.quantityLabel(inCart.quantity)),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.tertiary,
                                    )
                                } else if (!option.inStock) {
                                    Text(
                                        stringResource(Res.string.catalog_out_of_stock),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                            OptionCartAction(option = option, lines = lines, cart = cart, picker = picker)
                        }
                    }
                }
            }
        } else {
            val option = options.first()
            val line = productLines.firstOrNull()
            if (option.needsPicker || line == null) {
                UserPrimaryButton(
                    text = stringResource(Res.string.product_add_to_cart),
                    onClick = { if (option.needsPicker) picker.open(option, line?.key) else cart.add(option, option.defaultQty) },
                    enabled = !outOfStock,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                OptionCartAction(option = option, lines = lines, cart = cart, picker = picker)
            }
        }
        SectionCard(title = stringResource(Res.string.master_general_info)) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailRow(stringResource(Res.string.field_category), detail.categoryName)
                DetailRow(stringResource(Res.string.field_unit), product.unit.localizedLabel())
                DetailRow(stringResource(Res.string.master_brand), detail.brandName)
                DetailRow(stringResource(Res.string.master_company), detail.companyName)
                DetailRow(stringResource(Res.string.field_barcode), product.barcode.orEmpty())
                DetailRow(stringResource(Res.string.master_sku), detail.sku)
                if (detail.weightKg > 0.0) {
                    DetailRow(
                        stringResource(Res.string.master_weight),
                        stringResource(Res.string.product_weight_kg, detail.weightKg.asQuantity()),
                    )
                }
                DetailRow(stringResource(Res.string.master_dimensions), detail.dimensions)
            }
        }
        if (detail.description.isNotBlank()) {
            SectionCard(title = stringResource(Res.string.master_description)) {
                Text(detail.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (detail.tags.isNotEmpty()) {
            SectionCard(title = stringResource(Res.string.master_tags)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    detail.tags.forEach { tag -> SuggestionChip(onClick = {}, label = { Text(tag) }) }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    if (value.isBlank()) return
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
    }
}
