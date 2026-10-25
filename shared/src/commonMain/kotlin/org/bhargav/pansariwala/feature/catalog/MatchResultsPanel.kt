package org.bhargav.pansariwala.feature.catalog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import org.bhargav.pansariwala.feature.user.PurchaseOption
import org.bhargav.pansariwala.feature.user.displayName
import org.bhargav.pansariwala.feature.user.packSizeLabel
import org.bhargav.pansariwala.feature.user.priceLabel
import org.bhargav.pansariwala.util.asQuantity
import org.bhargav.pansariwala.voice.CatalogMatchKind
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_close
import pansariwala.shared.generated.resources.match_results_brand
import pansariwala.shared.generated.resources.match_results_category
import pansariwala.shared.generated.resources.match_results_product
import pansariwala.shared.generated.resources.match_results_stock
import pansariwala.shared.generated.resources.match_results_out_of_stock

/**
 * Bottom list of voice / barcode / search matches. Rows stream in pages: reaching the end calls [onLoadMore]
 * while [hasMore]; tapping a row runs the screen's current action via [onPick].
 */
@Composable
fun MatchResultsPanel(
    kind: CatalogMatchKind,
    label: String,
    options: List<PurchaseOption>,
    hasMore: Boolean,
    onLoadMore: () -> Unit,
    onPick: (PurchaseOption) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val atEnd by remember {
        derivedStateOf {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= listState.layoutInfo.totalItemsCount - PREFETCH_ROWS
        }
    }
    LaunchedEffect(atEnd, hasMore, options.size) { if (atEnd && hasMore) onLoadMore() }
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Column {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(
                        when (kind) {
                            CatalogMatchKind.BRAND -> Res.string.match_results_brand
                            CatalogMatchKind.CATEGORY -> Res.string.match_results_category
                            CatalogMatchKind.PRODUCT -> Res.string.match_results_product
                        },
                        label,
                        options.size,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.action_close))
                }
            }
            HorizontalDivider()
            LazyColumn(state = listState, modifier = Modifier.fillMaxWidth().heightIn(max = MAX_HEIGHT_DP.dp)) {
                itemsIndexed(options, key = { _, o -> o.key }) { index, option ->
                    if (index > 0) HorizontalDivider()
                    MatchRow(option, onClick = { onPick(option) })
                }
            }
        }
    }
}

@Composable
private fun MatchRow(option: PurchaseOption, onClick: () -> Unit) {
    val inStock = option.inStock
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(option.displayName, fontWeight = FontWeight.Medium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val meta = listOfNotNull(option.product.brandName.takeIf { it.isNotBlank() }, option.packSizeLabel())
            if (meta.isNotEmpty()) {
                Text(meta.joinToString(" · "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(option.priceLabel(), fontWeight = FontWeight.SemiBold)
            Text(
                if (inStock) stringResource(Res.string.match_results_stock, option.stock.asQuantity()) else stringResource(Res.string.match_results_out_of_stock),
                style = MaterialTheme.typography.labelSmall,
                color = if (inStock) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
            )
        }
    }
}

private const val PREFETCH_ROWS = 3
private const val MAX_HEIGHT_DP = 320
