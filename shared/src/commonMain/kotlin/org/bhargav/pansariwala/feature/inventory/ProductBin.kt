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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.RestoreFromTrash
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.bhargav.pansariwala.data.db.ShopRepository
import org.bhargav.pansariwala.data.local.AppPreferences
import org.bhargav.pansariwala.data.seed.SeedData
import org.bhargav.pansariwala.designsystem.AppAlertDialog
import org.bhargav.pansariwala.designsystem.PansariScreen
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.i18n.localizedLabel
import org.bhargav.pansariwala.i18n.localizedName
import org.bhargav.pansariwala.media.ProductImageCarousel
import org.bhargav.pansariwala.util.asQuantity
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.bin_banner_message
import pansariwala.shared.generated.resources.bin_banner_title
import pansariwala.shared.generated.resources.bin_deleted_on
import pansariwala.shared.generated.resources.bin_empty
import pansariwala.shared.generated.resources.bin_recover
import pansariwala.shared.generated.resources.bin_title
import pansariwala.shared.generated.resources.inv_delete_confirm
import pansariwala.shared.generated.resources.inv_delete_message
import pansariwala.shared.generated.resources.inv_delete_title
import kotlin.time.Instant

/** Confirmation before moving a product to the bin. */
@Composable
fun DeleteProductDialog(productName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AppAlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
        title = { Text(stringResource(Res.string.inv_delete_title)) },
        text = { Text(stringResource(Res.string.inv_delete_message, productName)) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            ) { Text(stringResource(Res.string.inv_delete_confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(Res.string.action_cancel)) } },
    )
}

/** Shown on top of a binned product's detail page. */
@Composable
internal fun BinnedProductBanner(product: Product, onRecover: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    stringResource(Res.string.bin_banner_title),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                Text(
                    stringResource(Res.string.bin_banner_message, deletedDate(product.deletedAt)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
            FilledTonalButton(onClick = onRecover) {
                Icon(Icons.Outlined.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(Res.string.bin_recover), modifier = Modifier.padding(start = 6.dp))
            }
        }
    }
}

class DeletedProductsViewModel(
    private val shopRepository: ShopRepository,
    private val preferences: AppPreferences,
) : ViewModel() {
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val products: StateFlow<List<Product>?> = flow { emit(preferences.getShopId() ?: SeedData.DEMO_SHOP_ID) }
        .flatMapLatest { shopId -> shopRepository.observeDeletedProducts(shopId) }
        .map<List<Product>, List<Product>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    fun recover(productId: String) {
        viewModelScope.launch { shopRepository.setProductDeleted(productId, null) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

@Composable
fun DeletedProductsScreen(
    onBack: () -> Unit,
    onOpenProduct: (String) -> Unit,
    viewModel: DeletedProductsViewModel = koinViewModel(),
) {
    val products by viewModel.products.collectAsStateWithLifecycle()
    PansariScreen(
        title = stringResource(Res.string.bin_title),
        onBack = onBack,
        isLoading = products == null,
    ) {
        val list = products.orEmpty()
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            if (products != null && list.isEmpty()) {
                Text(
                    stringResource(Res.string.bin_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                )
            }
            LazyColumn(
                modifier = Modifier.widthIn(max = BIN_MAX_WIDTH).fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(list, key = { it.id }) { product ->
                    DeletedProductRow(
                        product = product,
                        onClick = { onOpenProduct(product.id) },
                        onRecover = { viewModel.recover(product.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun DeletedProductRow(product: Product, onClick: () -> Unit, onRecover: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ProductImageCarousel(
            imageUrls = listOfNotNull(product.primaryImageUrl),
            height = 56.dp,
            modifier = Modifier.size(56.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                product.localizedName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                "${product.totalStock.asQuantity()} ${product.unit.localizedLabel()}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(Res.string.bin_deleted_on, deletedDate(product.deletedAt)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        FilledTonalButton(onClick = onRecover) {
            Icon(Icons.Outlined.RestoreFromTrash, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(stringResource(Res.string.bin_recover), modifier = Modifier.padding(start = 6.dp))
        }
    }
}

private fun deletedDate(epochMs: Long?): String {
    if (epochMs == null) return ""
    val d = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    fun Int.two() = toString().padStart(2, '0')
    return "${d.day.two()}/${(d.month.ordinal + 1).two()}/${d.year} ${d.hour.two()}:${d.minute.two()}"
}

private val BIN_MAX_WIDTH = 840.dp
