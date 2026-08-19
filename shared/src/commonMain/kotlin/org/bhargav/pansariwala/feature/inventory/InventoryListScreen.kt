package org.bhargav.pansariwala.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import org.bhargav.pansariwala.designsystem.PansariTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.i18n.localizedLabel
import org.bhargav.pansariwala.i18n.localizedName
import org.bhargav.pansariwala.util.asMoney
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_edit
import pansariwala.shared.generated.resources.all_inventory
import pansariwala.shared.generated.resources.category_price
import pansariwala.shared.generated.resources.low_stock_items
import pansariwala.shared.generated.resources.products_count

@Composable
fun InventoryListScreen(
    lowStockOnly: Boolean,
    onBack: () -> Unit,
    onEditProduct: (String) -> Unit,
    viewModel: InventoryListViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val products = if (lowStockOnly) state.products.filter { it.isLowStock } else state.products

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        PansariTopBar(
            title = stringResource(if (lowStockOnly) Res.string.low_stock_items else Res.string.all_inventory),
            onBack = onBack,
            modifier = Modifier.padding(bottom = 12.dp),
        )
        Text(
            text = stringResource(Res.string.products_count, products.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(products, key = { it.id }) { product ->
                InventoryRow(product, onEditProduct)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun InventoryRow(product: Product, onEditProduct: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = stringResource(
                    Res.string.category_price,
                    product.category.localizedName(),
                    product.sellingPrice.asMoney(),
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${product.stockQty.asQuantity()} ${product.unit.localizedLabel()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (product.isLowStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            )
            TextButton(onClick = { onEditProduct(product.id) }) {
                Text(stringResource(Res.string.action_edit))
            }
        }
    }
}
