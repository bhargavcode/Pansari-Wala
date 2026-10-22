package org.bhargav.pansariwala.feature.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.designsystem.SectionCard
import org.bhargav.pansariwala.designsystem.StatTile
import org.bhargav.pansariwala.designsystem.handleErrorBannerAction
import org.bhargav.pansariwala.domain.model.ProductCategory
import org.bhargav.pansariwala.i18n.localizedName
import org.bhargav.pansariwala.ui.toErrorBanner
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_refresh
import pansariwala.shared.generated.resources.report_month_short
import pansariwala.shared.generated.resources.report_revenue
import pansariwala.shared.generated.resources.sales_category_empty
import pansariwala.shared.generated.resources.sales_category_meta
import pansariwala.shared.generated.resources.sales_category_products
import pansariwala.shared.generated.resources.sales_category_section
import pansariwala.shared.generated.resources.sales_orders
import pansariwala.shared.generated.resources.sales_product_bars_title
import pansariwala.shared.generated.resources.sales_product_empty
import pansariwala.shared.generated.resources.sales_product_meta
import pansariwala.shared.generated.resources.sales_qty_sold

/** "Sales by category" card list on the main reports screen. */
@Composable
internal fun CategorySalesSection(data: List<CategorySales>, onOpen: (ProductCategory) -> Unit) {
    SectionCard(title = stringResource(Res.string.sales_category_section)) {
        if (data.isEmpty()) {
            Text(
                stringResource(Res.string.sales_category_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            return@SectionCard
        }
        val maxRevenue = data.maxOf { it.revenue }.coerceAtLeast(1.0)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            data.forEach { row ->
                CategoryCard(row, share = (row.revenue / maxRevenue).toFloat(), onClick = { onOpen(row.category) })
            }
        }
    }
}

@Composable
private fun CategoryCard(row: CategorySales, share: Float, onClick: () -> Unit) {
    val accent = categoryColor(row.category)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = 0.08f),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    row.category.localizedName().take(1),
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        row.category.localizedName(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(row.revenue.asMoney(), fontWeight = FontWeight.SemiBold, color = accent)
                }
                Text(
                    stringResource(Res.string.sales_category_meta, row.orderCount, row.productCount, row.quantity.asQuantity()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { share.coerceIn(0f, 1f) },
                    color = accent,
                    trackColor = accent.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = accent)
        }
    }
}

@Composable
fun CategorySalesScreen(
    category: String,
    window: SalesWindow,
    onBack: () -> Unit,
    onOpenProduct: (productId: String, productName: String, window: SalesWindow) -> Unit,
    viewModel: SalesDrillViewModel = koinViewModel(key = "category_$category"),
) {
    LaunchedEffect(category) { viewModel.start(category = category, productId = null, window = window) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cat = ProductCategory.fromName(category)
    val accent = categoryColor(cat)
    DrillScaffold(cat.localizedName(), state, viewModel, onBack) {
        item(key = "totals") {
            val total = state.categoryTotal
            DrillTotals(
                revenue = total?.revenue ?: 0.0,
                orders = total?.orderCount ?: 0,
                quantity = total?.quantity ?: 0.0,
            )
        }
        item(key = "header") {
            Text(
                stringResource(Res.string.sales_category_products, state.products.size),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        if (state.products.isEmpty() && !state.loading) {
            item(key = "empty") {
                Text(stringResource(Res.string.sales_category_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val maxOrders = state.products.maxOfOrNull { it.orderCount }?.coerceAtLeast(1) ?: 1
        itemsIndexed(state.products) { index, product ->
            ProductSalesRow(
                rank = index + 1,
                product = product,
                share = product.orderCount.toFloat() / maxOrders,
                accent = accent,
                onClick = { onOpenProduct(product.productId, product.name, viewModel.window()) },
            )
        }
    }
}

@Composable
private fun ProductSalesRow(rank: Int, product: ProductSales, share: Float, accent: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#$rank",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.widthIn(min = 32.dp),
            )
            Column(Modifier.weight(1f).padding(end = 12.dp)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    stringResource(Res.string.sales_product_meta, product.quantity.asQuantity(), product.revenue.asMoney()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { share.coerceIn(0f, 1f) },
                    color = accent,
                    trackColor = accent.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    product.orderCount.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
                Text(
                    stringResource(Res.string.sales_orders),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null)
        }
    }
}

@Composable
fun ProductSalesScreen(
    productId: String,
    productName: String,
    window: SalesWindow,
    onBack: () -> Unit,
    viewModel: SalesDrillViewModel = koinViewModel(key = "product_$productId"),
) {
    LaunchedEffect(productId) { viewModel.start(category = null, productId = productId, window = window) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val months = stringArrayResource(Res.array.report_month_short)
    val multiYear = state.range?.let { it.start.year != it.endInclusive.year } == true
    val labelFor: (ReportBucket) -> String = remember(months, multiYear) {
        { b -> bucketLabel(b, months, multiYear) }
    }
    DrillScaffold(productName, state, viewModel, onBack) {
        item(key = "totals") {
            val p = state.product
            DrillTotals(revenue = p?.revenue ?: 0.0, orders = p?.orderCount ?: 0, quantity = p?.quantity ?: 0.0)
        }
        item(key = "bars") {
            SectionCard(title = stringResource(Res.string.sales_product_bars_title)) {
                if (state.buckets.none { it.onlineOrders + it.offlineOrders > 0 }) {
                    Text(
                        stringResource(Res.string.sales_product_empty),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    OrderCountBars(state.buckets, labelFor)
                }
            }
        }
    }
}

/** Shared chrome for both drill-down screens: filters, refresh, error banner, date picker. */
@Composable
private fun DrillScaffold(
    title: String,
    state: SalesDrillUiState,
    viewModel: SalesDrillViewModel,
    onBack: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit,
) {
    PansariScreen(
        title = title,
        onBack = onBack,
        actions = {
            IconButton(onClick = viewModel::refresh) {
                Icon(Icons.Default.Refresh, contentDescription = stringResource(Res.string.action_refresh))
            }
        },
        error = state.error.toErrorBanner(),
        onErrorAction = {
            handleErrorBannerAction(it, onRetry = viewModel::refresh, onDismiss = viewModel::dismissError)
        },
        isLoading = state.loading && state.range == null,
        isRefreshing = state.loading && state.range != null,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "filters") {
                ReportFilters(
                    channel = state.channel,
                    period = state.period,
                    range = state.range,
                    onChannel = viewModel::setChannel,
                    onPeriod = viewModel::setPeriod,
                )
            }
            content()
        }
    }
    if (state.showDatePicker) {
        CustomRangeDialog(onDismiss = viewModel::dismissDatePicker, onConfirm = viewModel::setCustomRange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DrillTotals(revenue: Double, orders: Int, quantity: Double) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val tile = Modifier.widthIn(min = 100.dp).weight(1f)
        StatTile(stringResource(Res.string.report_revenue), revenue.asMoney(), tile)
        StatTile(
            stringResource(Res.string.sales_orders),
            orders.toString(),
            tile,
            container = MaterialTheme.colorScheme.secondaryContainer,
            onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        StatTile(
            stringResource(Res.string.sales_qty_sold),
            quantity.asQuantity(),
            tile,
            container = MaterialTheme.colorScheme.tertiaryContainer,
            onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
        )
    }
}

/** Order count per bucket as vertical bars; scrolls horizontally for long ranges. */
@Composable
private fun OrderCountBars(buckets: List<ReportBucket>, labelFor: (ReportBucket) -> String) {
    val counts = buckets.map { it.onlineOrders + it.offlineOrders }
    val max = counts.maxOrNull()?.coerceAtLeast(1) ?: 1
    val barColor = MaterialTheme.colorScheme.primary
    val chartHeight = 160.dp
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        buckets.forEachIndexed { i, bucket ->
            val count = counts[i]
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(36.dp),
            ) {
                Text(
                    if (count > 0) count.toString() else "",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Box(Modifier.height(chartHeight), contentAlignment = Alignment.BottomCenter) {
                    Box(
                        Modifier
                            .width(22.dp)
                            .height(if (count == 0) 2.dp else chartHeight * (count.toFloat() / max))
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(if (count == 0) barColor.copy(alpha = 0.15f) else barColor),
                    )
                }
                Text(
                    labelFor(bucket),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

private fun categoryColor(category: ProductCategory): Color = pieColors[category.ordinal % pieColors.size]
