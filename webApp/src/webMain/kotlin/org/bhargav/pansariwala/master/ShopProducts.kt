package org.bhargav.pansariwala.master

import org.bhargav.pansariwala.designsystem.appDialogProperties
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.domain.model.ProductCategory
import org.bhargav.pansariwala.domain.model.StockLevel
import org.bhargav.pansariwala.feature.inventory.StockBadge
import org.bhargav.pansariwala.feature.inventory.StockGauge
import org.bhargav.pansariwala.feature.inventory.color
import org.bhargav.pansariwala.i18n.localizedName
import org.bhargav.pansariwala.theme.stockHealthyColor
import org.bhargav.pansariwala.theme.stockLowColor
import org.bhargav.pansariwala.theme.stockOutColor
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_add
import pansariwala.shared.generated.resources.action_back
import pansariwala.shared.generated.resources.action_cancel
import pansariwala.shared.generated.resources.action_next
import pansariwala.shared.generated.resources.action_previous
import pansariwala.shared.generated.resources.action_save
import pansariwala.shared.generated.resources.field_barcode
import pansariwala.shared.generated.resources.field_cost_price
import pansariwala.shared.generated.resources.field_low_stock_at
import pansariwala.shared.generated.resources.field_name
import pansariwala.shared.generated.resources.field_name_hi
import pansariwala.shared.generated.resources.field_selling_price
import pansariwala.shared.generated.resources.inv_action_set_stock
import pansariwala.shared.generated.resources.inv_action_stock_in
import pansariwala.shared.generated.resources.inv_action_stock_out
import pansariwala.shared.generated.resources.inv_active_desc_off
import pansariwala.shared.generated.resources.inv_active_desc_on
import pansariwala.shared.generated.resources.inv_active_label
import pansariwala.shared.generated.resources.inv_add_from_master
import pansariwala.shared.generated.resources.inv_empty
import pansariwala.shared.generated.resources.inv_error_invalid_qty
import pansariwala.shared.generated.resources.inv_filter_all_categories
import pansariwala.shared.generated.resources.inv_filter_low_stock
import pansariwala.shared.generated.resources.inv_label_brand
import pansariwala.shared.generated.resources.inv_label_master_id
import pansariwala.shared.generated.resources.inv_label_product_id
import pansariwala.shared.generated.resources.inv_master_add_title
import pansariwala.shared.generated.resources.inv_master_added
import pansariwala.shared.generated.resources.inv_master_all
import pansariwala.shared.generated.resources.inv_master_empty
import pansariwala.shared.generated.resources.inv_master_search_hint
import pansariwala.shared.generated.resources.inv_opening_stock
import pansariwala.shared.generated.resources.inv_qty_field
import pansariwala.shared.generated.resources.inv_search_hint
import pansariwala.shared.generated.resources.inv_section_stock
import pansariwala.shared.generated.resources.inv_section_update
import pansariwala.shared.generated.resources.inv_status_active
import pansariwala.shared.generated.resources.inv_status_all
import pansariwala.shared.generated.resources.inv_status_inactive
import pansariwala.shared.generated.resources.inv_stock_low
import pansariwala.shared.generated.resources.inv_stock_out
import pansariwala.shared.generated.resources.inv_stock_threshold
import pansariwala.shared.generated.resources.inv_stock_value
import pansariwala.shared.generated.resources.inv_value_none
import pansariwala.shared.generated.resources.master_actions
import pansariwala.shared.generated.resources.master_category
import pansariwala.shared.generated.resources.master_close
import pansariwala.shared.generated.resources.master_edit_details
import pansariwala.shared.generated.resources.master_image_column
import pansariwala.shared.generated.resources.master_manage
import pansariwala.shared.generated.resources.master_page_info
import pansariwala.shared.generated.resources.master_product_name
import pansariwala.shared.generated.resources.master_sale_price
import pansariwala.shared.generated.resources.master_shop_products_title
import pansariwala.shared.generated.resources.master_status
import pansariwala.shared.generated.resources.master_stock
import pansariwala.shared.generated.resources.master_stock_total

private const val SHOP_PRODUCTS_PAGE_SIZE = 25
private const val MASTER_PICKER_PAGE_SIZE = 20
private const val SHOP_SEARCH_DEBOUNCE_MS = 350L
private const val STATUS_ALL = "all"
private const val STATUS_ACTIVE = "active"
private const val STATUS_INACTIVE = "inactive"

private val ShopProductDto.level: StockLevel get() = StockLevel.of(stockQty, lowStockThreshold)
private val ShopProductDto.categoryEnum: ProductCategory get() = ProductCategory.fromName(category)

private fun String.toQty(): Double? = trim().toDoubleOrNull()
private fun Double.plainQty(): String = if (this == this.toLong().toDouble()) toLong().toString() else toString()

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ShopProductsScreen(token: String, shopId: String, shopName: String?, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var pageData by remember { mutableStateOf(ShopProductPageDto()) }
    var summary by remember { mutableStateOf(ShopStockSummaryDto()) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var status by rememberSaveable { mutableStateOf(STATUS_ALL) }
    var lowStock by rememberSaveable { mutableStateOf(false) }
    var reload by remember { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    var showPicker by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<ShopProductDto?>(null) }
    fun refresh() { reload++ }

    LaunchedEffect(token, shopId, query, category, status, lowStock, page, reload) {
        if (query.isNotBlank()) delay(SHOP_SEARCH_DEBOUNCE_MS)
        loading = true
        runCatching {
            pageData = api.shopProducts(token, shopId, query, category, status, lowStock, page, SHOP_PRODUCTS_PAGE_SIZE)
        }.onFailure { onStatus(it.message.orEmpty()) }
        loading = false
    }
    LaunchedEffect(token, shopId, reload) {
        runCatching { summary = api.shopStockSummary(token, shopId) }
    }
    val pageCount = ((pageData.totalCount + SHOP_PRODUCTS_PAGE_SIZE - 1) / SHOP_PRODUCTS_PAGE_SIZE).coerceAtLeast(1)

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MasterTopBar(stringResource(Res.string.master_shop_products_title, shopName ?: shopId))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryTile(stringResource(Res.string.master_stock_total), summary.total, MaterialTheme.colorScheme.primary) {
                    status = STATUS_ALL; lowStock = false; page = 0
                }
                SummaryTile(stringResource(Res.string.inv_status_active), summary.active, stockHealthyColor) {
                    status = STATUS_ACTIVE; page = 0
                }
                SummaryTile(stringResource(Res.string.inv_status_inactive), summary.inactive, MaterialTheme.colorScheme.outline) {
                    status = STATUS_INACTIVE; page = 0
                }
                SummaryTile(stringResource(Res.string.inv_stock_low), summary.lowStock, stockLowColor) {
                    lowStock = true; page = 0
                }
                SummaryTile(stringResource(Res.string.inv_stock_out), summary.outOfStock, stockOutColor) {
                    lowStock = true; page = 0
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Button(onClick = { showPicker = true }) { Text(stringResource(Res.string.inv_add_from_master)) }
                OutlinedButton(onClick = LocalMasterBack.current) { Text(stringResource(Res.string.action_back)) }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; page = 0 },
                label = { Text(stringResource(Res.string.inv_search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                FilterChip(
                    selected = category == null,
                    onClick = { category = null; page = 0 },
                    label = { Text(stringResource(Res.string.inv_filter_all_categories)) },
                )
                ProductCategory.entries.forEach { c ->
                    FilterChip(
                        selected = category == c.name,
                        onClick = { category = c.name; page = 0 },
                        label = { Text(c.localizedName()) },
                    )
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    STATUS_ALL to Res.string.inv_status_all,
                    STATUS_ACTIVE to Res.string.inv_status_active,
                    STATUS_INACTIVE to Res.string.inv_status_inactive,
                ).forEach { (value, label) ->
                    FilterChip(
                        selected = status == value,
                        onClick = { status = value; page = 0 },
                        label = { Text(stringResource(label)) },
                    )
                }
                FilterChip(
                    selected = lowStock,
                    onClick = { lowStock = !lowStock; page = 0 },
                    label = { Text(stringResource(Res.string.inv_filter_low_stock)) },
                )
            }
            MasterSectionCard {
                ResponsiveTable(columns = 8) {
                    TableHeader(
                        stringResource(Res.string.master_image_column),
                        stringResource(Res.string.master_product_name),
                        stringResource(Res.string.inv_label_brand),
                        stringResource(Res.string.master_category),
                        stringResource(Res.string.master_sale_price),
                        stringResource(Res.string.master_stock),
                        stringResource(Res.string.master_status),
                        stringResource(Res.string.master_actions),
                    )
                    if (loading && pageData.items.isEmpty()) {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else if (pageData.items.isEmpty()) {
                        Text(
                            stringResource(Res.string.inv_empty),
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    pageData.items.forEach { p ->
                        TableRow(onClick = { selected = p }) {
                            Cell { ImageThumb(p.imageUrls.firstOrNull(), Modifier.size(48.dp)) }
                            CellText(p.name)
                            CellText(p.brandName.ifBlank { "—" })
                            CellText(p.categoryEnum.localizedName())
                            CellText(formatInr(p.sellingPrice))
                            Cell {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        "${p.stockQty.plainQty()} ${p.unit.lowercase()}",
                                        color = p.level.color(),
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                    StockBadge(p.level)
                                    StockGauge(p.stockQty, p.lowStockThreshold, Modifier.width(96.dp))
                                }
                            }
                            Cell {
                                OnOffToggle(p.active) {
                                    scope.launch {
                                        runCatching { api.setShopProductActive(token, shopId, p.id, !p.active) }
                                            .onSuccess { refresh() }
                                            .onFailure { onStatus(it.message.orEmpty()) }
                                    }
                                }
                            }
                            Cell {
                                TextButton(onClick = { selected = p }) { Text(stringResource(Res.string.master_manage)) }
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { page-- }, enabled = page > 0) { Text(stringResource(Res.string.action_previous)) }
                Text(stringResource(Res.string.master_page_info, page + 1, pageCount, pageData.totalCount))
                OutlinedButton(onClick = { page++ }, enabled = pageData.hasMore) { Text(stringResource(Res.string.action_next)) }
            }
        }
    }

    if (showPicker) {
        MasterPickerDialog(
            token = token,
            shopId = shopId,
            onStatus = onStatus,
            onAdded = { refresh() },
            onDismiss = { showPicker = false },
        )
    }
    selected?.let { product ->
        ShopProductDialog(
            token = token,
            shopId = shopId,
            initial = product,
            onStatus = onStatus,
            onChanged = { refresh() },
            onDismiss = { selected = null },
        )
    }
}

@Composable
private fun SummaryTile(label: String, value: Int, accent: Color, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.widthIn(min = 150.dp),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.10f)),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall, color = accent, fontWeight = FontWeight.SemiBold)
            Text("$value", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = accent)
        }
    }
}

@Composable
private fun WideDialog(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = appDialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier.padding(24.dp).widthIn(max = 920.dp).fillMaxWidth().heightIn(max = 860.dp),
        ) { content() }
    }
}

/** Server-paged master catalog; "Added" items are already linked to this shop. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MasterPickerDialog(
    token: String,
    shopId: String,
    onStatus: (String) -> Unit,
    onAdded: () -> Unit,
    onDismiss: () -> Unit,
) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var categoryId by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableIntStateOf(0) }
    var data by remember { mutableStateOf(ProductPageDto()) }
    var categories by remember { mutableStateOf<List<CategoryDto>>(emptyList()) }
    var links by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var adding by remember { mutableStateOf<ProductDto?>(null) }

    LaunchedEffect(Unit) {
        runCatching { categories = api.categories(token).filter { it.parentId.isNullOrBlank() } }
    }
    LaunchedEffect(query, categoryId, page) {
        if (query.isNotBlank()) delay(SHOP_SEARCH_DEBOUNCE_MS)
        loading = true
        runCatching {
            val result = api.productPage(token, query.trim(), categoryId, page, MASTER_PICKER_PAGE_SIZE)
            data = result
            links = links + api.shopMasterLinks(token, shopId, result.items.map { it.id })
        }.onFailure { onStatus(it.message.orEmpty()) }
        loading = false
    }
    val pageCount = ((data.totalCount + MASTER_PICKER_PAGE_SIZE - 1) / MASTER_PICKER_PAGE_SIZE).coerceAtLeast(1)

    WideDialog(onDismiss) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(Res.string.inv_add_from_master),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.master_close)) }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it; page = 0 },
                label = { Text(stringResource(Res.string.inv_master_search_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            if (categories.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = categoryId == null,
                        onClick = { categoryId = null; page = 0 },
                        label = { Text(stringResource(Res.string.inv_master_all)) },
                    )
                    categories.forEach { c ->
                        FilterChip(
                            selected = categoryId == c.id,
                            onClick = { categoryId = c.id; page = 0 },
                            label = { Text(c.name) },
                        )
                    }
                }
            }
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                when {
                    loading && data.items.isEmpty() ->
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    data.items.isEmpty() -> Text(stringResource(Res.string.inv_master_empty), Modifier.padding(24.dp))
                }
                data.items.forEach { item ->
                    Column {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            ImageThumb(item.imageUrls.firstOrNull() ?: item.imageUrl, Modifier.size(48.dp))
                            Column(Modifier.weight(1f)) {
                                Text(item.name, fontWeight = FontWeight.Medium)
                                Text(
                                    listOf(item.brandName, item.unit, if (item.salePrice > 0) formatInr(item.salePrice) else "")
                                        .filter { it.isNotBlank() }.joinToString(" · "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (links.containsKey(item.id)) {
                                Text(
                                    "✓ ${stringResource(Res.string.inv_master_added)}",
                                    color = stockHealthyColor,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            } else if (adding?.id != item.id) {
                                Button(onClick = { adding = item }) { Text(stringResource(Res.string.action_add)) }
                            }
                        }
                        if (adding?.id == item.id) {
                            AddFromMasterForm(
                                item = item,
                                categoryName = categories.firstOrNull { it.id == item.categoryId }?.name ?: item.categoryId,
                                onCancel = { adding = null },
                                onConfirm = { body ->
                                    scope.launch {
                                        runCatching { api.addShopProductFromMaster(token, shopId, body) }
                                            .onSuccess { created ->
                                                links = links + (item.id to created.id)
                                                adding = null
                                                onAdded()
                                            }
                                            .onFailure { onStatus(it.message.orEmpty()) }
                                    }
                                },
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { page-- }, enabled = page > 0) { Text(stringResource(Res.string.action_previous)) }
                Text(stringResource(Res.string.master_page_info, page + 1, pageCount, data.totalCount))
                OutlinedButton(onClick = { page++ }, enabled = data.hasMore) { Text(stringResource(Res.string.action_next)) }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AddFromMasterForm(
    item: ProductDto,
    categoryName: String,
    onCancel: () -> Unit,
    onConfirm: (AddFromMasterBody) -> Unit,
) {
    val startPrice = item.salePrice.takeIf { it > 0 } ?: item.variants.minOfOrNull { it.price } ?: 0.0
    var price by remember { mutableStateOf(if (startPrice > 0) startPrice.plainQty() else "") }
    var cost by remember { mutableStateOf(if (item.cost > 0) item.cost.plainQty() else "") }
    var stock by remember { mutableStateOf("") }
    var threshold by remember { mutableStateOf(if (item.lowStockThreshold > 0) item.lowStockThreshold.plainQty() else "") }
    var category by remember { mutableStateOf(ProductCategory.fromMasterCategoryName(categoryName)) }
    MasterSectionCard(title = stringResource(Res.string.inv_master_add_title, item.name)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            QtyField(price, stringResource(Res.string.field_selling_price)) { price = it }
            QtyField(cost, stringResource(Res.string.field_cost_price)) { cost = it }
            QtyField(stock, stringResource(Res.string.inv_opening_stock)) { stock = it }
            QtyField(threshold, stringResource(Res.string.field_low_stock_at)) { threshold = it }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ProductCategory.entries.forEach { c ->
                FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.localizedName()) })
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                onConfirm(
                    AddFromMasterBody(
                        masterProductId = item.id,
                        category = category.name,
                        sellingPrice = price.toQty() ?: 0.0,
                        costPrice = cost.toQty() ?: 0.0,
                        stockQty = stock.toQty() ?: 0.0,
                        lowStockThreshold = threshold.toQty() ?: 0.0,
                    ),
                )
            }) { Text(stringResource(Res.string.action_add)) }
            OutlinedButton(onClick = onCancel) { Text(stringResource(Res.string.action_cancel)) }
        }
    }
}

@Composable
private fun QtyField(value: String, label: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.width(180.dp),
    )
}

/** Stock card, inventory updates, active switch and editable details for one shop product. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShopProductDialog(
    token: String,
    shopId: String,
    initial: ShopProductDto,
    onStatus: (String) -> Unit,
    onChanged: () -> Unit,
    onDismiss: () -> Unit,
) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var product by remember(initial.id) { mutableStateOf(initial) }
    var qty by remember { mutableStateOf("") }
    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var nameHi by remember(initial.id) { mutableStateOf(initial.nameHi) }
    var brand by remember(initial.id) { mutableStateOf(initial.brandName) }
    var barcode by remember(initial.id) { mutableStateOf(initial.barcode.orEmpty()) }
    var price by remember(initial.id) { mutableStateOf(initial.sellingPrice.plainQty()) }
    var cost by remember(initial.id) { mutableStateOf(initial.costPrice.plainQty()) }
    var threshold by remember(initial.id) { mutableStateOf(initial.lowStockThreshold.plainQty()) }
    var category by remember(initial.id) { mutableStateOf(initial.categoryEnum) }
    val invalidQty = stringResource(Res.string.inv_error_invalid_qty)

    fun run(block: suspend () -> ShopProductDto) {
        scope.launch {
            runCatching { block() }
                .onSuccess { product = it; qty = ""; onChanged() }
                .onFailure { onStatus(it.message.orEmpty()) }
        }
    }
    fun stockDelta(delta: Double) = run { api.updateShopProductStock(token, shopId, product.id, StockUpdateBody(delta = delta)) }
    fun typedQty(allowZero: Boolean = false): Double? {
        val v = qty.toQty()
        return if (v == null || v < 0.0 || (!allowZero && v == 0.0)) { onStatus(invalidQty); null } else v
    }

    val level = product.level
    val unit = product.unit.lowercase()
    WideDialog(onDismiss) {
        Column(Modifier.padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ImageThumb(product.imageUrls.firstOrNull(), Modifier.size(72.dp))
                Column(Modifier.weight(1f)) {
                    Text(product.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(
                        listOf(product.brandName, product.categoryEnum.localizedName()).filter { it.isNotBlank() }.joinToString(" · "),
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        "${stringResource(Res.string.inv_label_product_id)}: ${product.id} · " +
                            "${stringResource(Res.string.inv_label_master_id)}: ${product.masterProductId ?: stringResource(Res.string.inv_value_none)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onDismiss) { Text(stringResource(Res.string.master_close)) }
            }

            ResponsiveColumns(
                left = {
                    MasterSectionCard(title = stringResource(Res.string.inv_section_stock)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "${product.stockQty.plainQty()} $unit",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold,
                                color = level.color(),
                            )
                            StockBadge(level)
                        }
                        StockGauge(product.stockQty, product.lowStockThreshold, Modifier.fillMaxWidth().height(10.dp))
                        Text(
                            stringResource(Res.string.inv_stock_threshold, product.lowStockThreshold.plainQty(), unit),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            stringResource(Res.string.inv_stock_value, formatInr(product.stockQty * product.costPrice)),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        HorizontalDivider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(Res.string.inv_active_label))
                                Text(
                                    stringResource(if (product.active) Res.string.inv_active_desc_on else Res.string.inv_active_desc_off),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = product.active,
                                onCheckedChange = { checked -> run { api.setShopProductActive(token, shopId, product.id, checked) } },
                            )
                        }
                    }
                },
                right = {
                    MasterSectionCard(title = stringResource(Res.string.inv_section_update)) {
                        val inColors = ButtonDefaults.buttonColors(containerColor = stockHealthyColor)
                        val outColors = ButtonDefaults.outlinedButtonColors(contentColor = stockOutColor)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppConstants.INVENTORY_QUICK_STEPS.forEach { step ->
                                Button(onClick = { stockDelta(step) }, colors = inColors) { Text("+${step.plainQty()}") }
                            }
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            AppConstants.INVENTORY_QUICK_STEPS.forEach { step ->
                                OutlinedButton(
                                    onClick = { stockDelta(-step) },
                                    enabled = product.stockQty >= step,
                                    colors = outColors,
                                ) { Text("−${step.plainQty()}") }
                            }
                        }
                        OutlinedTextField(
                            value = qty,
                            onValueChange = { qty = it },
                            label = { Text(stringResource(Res.string.inv_qty_field)) },
                            suffix = { Text(unit) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = { typedQty()?.let { stockDelta(it) } }, colors = inColors) {
                                Text(stringResource(Res.string.inv_action_stock_in))
                            }
                            OutlinedButton(onClick = { typedQty()?.let { stockDelta(-it) } }, colors = outColors) {
                                Text(stringResource(Res.string.inv_action_stock_out))
                            }
                            OutlinedButton(onClick = {
                                typedQty(allowZero = true)?.let { v ->
                                    run { api.updateShopProductStock(token, shopId, product.id, StockUpdateBody(set = v)) }
                                }
                            }) { Text(stringResource(Res.string.inv_action_set_stock)) }
                        }
                    }
                },
            )

            MasterSectionCard(title = stringResource(Res.string.master_edit_details)) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(name, { name = it }, label = { Text(stringResource(Res.string.field_name)) }, singleLine = true, modifier = Modifier.width(280.dp))
                    OutlinedTextField(nameHi, { nameHi = it }, label = { Text(stringResource(Res.string.field_name_hi)) }, singleLine = true, modifier = Modifier.width(280.dp))
                    OutlinedTextField(brand, { brand = it }, label = { Text(stringResource(Res.string.inv_label_brand)) }, singleLine = true, modifier = Modifier.width(280.dp))
                    OutlinedTextField(barcode, { barcode = it }, label = { Text(stringResource(Res.string.field_barcode)) }, singleLine = true, modifier = Modifier.width(280.dp))
                    QtyField(price, stringResource(Res.string.field_selling_price)) { price = it }
                    QtyField(cost, stringResource(Res.string.field_cost_price)) { cost = it }
                    QtyField(threshold, stringResource(Res.string.field_low_stock_at)) { threshold = it }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    ProductCategory.entries.forEach { c ->
                        FilterChip(selected = category == c, onClick = { category = c }, label = { Text(c.localizedName()) })
                    }
                }
                Button(onClick = {
                    val body = ShopProductEditBody(
                        name = name,
                        nameHi = nameHi,
                        category = category.name,
                        brandName = brand,
                        barcode = barcode.ifBlank { null },
                        sellingPrice = price.toQty() ?: product.sellingPrice,
                        costPrice = cost.toQty() ?: product.costPrice,
                        lowStockThreshold = threshold.toQty() ?: product.lowStockThreshold,
                    )
                    run { api.editShopProduct(token, shopId, product.id, body) }
                }) { Text(stringResource(Res.string.action_save)) }
            }
        }
    }
}
