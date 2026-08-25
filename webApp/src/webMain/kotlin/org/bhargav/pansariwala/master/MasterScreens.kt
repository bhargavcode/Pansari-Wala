package org.bhargav.pansariwala.master

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_back
import pansariwala.shared.generated.resources.field_name
import pansariwala.shared.generated.resources.field_name_hi
import pansariwala.shared.generated.resources.field_unit
import pansariwala.shared.generated.resources.master_accepted_orders
import pansariwala.shared.generated.resources.master_actions
import pansariwala.shared.generated.resources.master_added_date
import pansariwala.shared.generated.resources.master_address
import pansariwala.shared.generated.resources.master_brand
import pansariwala.shared.generated.resources.master_cancel
import pansariwala.shared.generated.resources.master_cancel_order
import pansariwala.shared.generated.resources.master_cancelled_orders
import pansariwala.shared.generated.resources.master_card_partners
import pansariwala.shared.generated.resources.master_card_products
import pansariwala.shared.generated.resources.master_card_shops
import pansariwala.shared.generated.resources.master_card_transactions
import pansariwala.shared.generated.resources.master_card_users
import pansariwala.shared.generated.resources.master_category
import pansariwala.shared.generated.resources.master_charges
import pansariwala.shared.generated.resources.master_company
import pansariwala.shared.generated.resources.master_console_title
import pansariwala.shared.generated.resources.master_contact
import pansariwala.shared.generated.resources.master_cost
import pansariwala.shared.generated.resources.master_customer
import pansariwala.shared.generated.resources.master_date_filter
import pansariwala.shared.generated.resources.master_delete
import pansariwala.shared.generated.resources.master_description
import pansariwala.shared.generated.resources.master_dimensions
import pansariwala.shared.generated.resources.master_edit_product
import pansariwala.shared.generated.resources.master_email
import pansariwala.shared.generated.resources.master_feature_barcode
import pansariwala.shared.generated.resources.master_feature_inventory
import pansariwala.shared.generated.resources.master_feature_online
import pansariwala.shared.generated.resources.master_feature_reports
import pansariwala.shared.generated.resources.master_feature_voice
import pansariwala.shared.generated.resources.master_features
import pansariwala.shared.generated.resources.master_image_url
import pansariwala.shared.generated.resources.master_item_details
import pansariwala.shared.generated.resources.master_join_date
import pansariwala.shared.generated.resources.master_low_stock
import pansariwala.shared.generated.resources.master_name
import pansariwala.shared.generated.resources.master_new_category
import pansariwala.shared.generated.resources.master_new_product
import pansariwala.shared.generated.resources.master_new_shop
import pansariwala.shared.generated.resources.master_new_shop_type
import pansariwala.shared.generated.resources.master_offers
import pansariwala.shared.generated.resources.master_order_history
import pansariwala.shared.generated.resources.master_order_id
import pansariwala.shared.generated.resources.master_paid
import pansariwala.shared.generated.resources.master_parent_category
import pansariwala.shared.generated.resources.master_partner_details
import pansariwala.shared.generated.resources.master_partner_id
import pansariwala.shared.generated.resources.master_partners_title
import pansariwala.shared.generated.resources.master_platform_title
import pansariwala.shared.generated.resources.master_product_id
import pansariwala.shared.generated.resources.master_product_name
import pansariwala.shared.generated.resources.master_products_title
import pansariwala.shared.generated.resources.master_refund
import pansariwala.shared.generated.resources.master_sale_price
import pansariwala.shared.generated.resources.master_save
import pansariwala.shared.generated.resources.master_save_changes
import pansariwala.shared.generated.resources.master_search
import pansariwala.shared.generated.resources.master_settings_title
import pansariwala.shared.generated.resources.master_shop_details
import pansariwala.shared.generated.resources.master_shop_details_col
import pansariwala.shared.generated.resources.master_shop_id
import pansariwala.shared.generated.resources.master_shop_location
import pansariwala.shared.generated.resources.master_shop_name
import pansariwala.shared.generated.resources.master_shop_rating
import pansariwala.shared.generated.resources.master_shop_type
import pansariwala.shared.generated.resources.master_shops_list
import pansariwala.shared.generated.resources.master_sku
import pansariwala.shared.generated.resources.master_status
import pansariwala.shared.generated.resources.master_stock
import pansariwala.shared.generated.resources.master_subcategory
import pansariwala.shared.generated.resources.master_tab_categories
import pansariwala.shared.generated.resources.master_tab_shop_types
import pansariwala.shared.generated.resources.master_tags
import pansariwala.shared.generated.resources.master_total
import pansariwala.shared.generated.resources.master_total_delivered
import pansariwala.shared.generated.resources.master_total_earnings
import pansariwala.shared.generated.resources.master_transaction_no
import pansariwala.shared.generated.resources.master_transactions_title
import pansariwala.shared.generated.resources.master_txn_details
import pansariwala.shared.generated.resources.master_user_details
import pansariwala.shared.generated.resources.master_user_id
import pansariwala.shared.generated.resources.master_users_title
import pansariwala.shared.generated.resources.master_vehicle_brand
import pansariwala.shared.generated.resources.master_vehicle_color
import pansariwala.shared.generated.resources.master_vehicle_name
import pansariwala.shared.generated.resources.master_vehicle_number
import pansariwala.shared.generated.resources.master_vehicle_type
import pansariwala.shared.generated.resources.master_view_more
import pansariwala.shared.generated.resources.master_weight
import pansariwala.shared.generated.resources.action_edit

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun DashboardScreen(token: String, onNavigate: (MasterDest) -> Unit, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    var stats by remember { mutableStateOf<AdminDashboardDto?>(null) }
    var shops by remember { mutableStateOf<List<ShopDto>>(emptyList()) }
    var filter by rememberSaveable { mutableStateOf(AppConstants.DateFilter.TODAY) }
    LaunchedEffect(token, filter) {
        val (from, to) = dateFilterRange(filter)
        runCatching {
            stats = api.dashboard(token, from, to)
            shops = api.shops(token)
        }.onFailure { onStatus(it.message.orEmpty()) }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeaderBar(stringResource(Res.string.master_console_title))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MasterStatCard(
                    title = stringResource(Res.string.master_card_shops),
                    value = "${stats?.shopCount ?: "—"}",
                    actionLabel = stringResource(Res.string.master_new_shop),
                    onAction = { onNavigate(MasterDest.Shops) },
                    onClick = { onNavigate(MasterDest.Shops) },
                )
                MasterStatCard(
                    title = stringResource(Res.string.master_card_products),
                    value = "${stats?.productCount ?: "—"}",
                    actionLabel = stringResource(Res.string.master_new_product),
                    onAction = { onNavigate(MasterDest.ProductEdit(null)) },
                    onClick = { onNavigate(MasterDest.Products) },
                )
                MasterStatCard(
                    title = stringResource(Res.string.master_card_transactions),
                    value = formatInr(stats?.transactionAmount ?: 0.0),
                    onClick = { onNavigate(MasterDest.Transactions) },
                )
                MasterStatCard(
                    title = stringResource(Res.string.master_card_users),
                    value = "${stats?.userCount ?: "—"}",
                    onClick = { onNavigate(MasterDest.Users) },
                )
                MasterStatCard(
                    title = stringResource(Res.string.master_card_partners),
                    value = "${stats?.partnerCount ?: "—"}",
                    onClick = { onNavigate(MasterDest.Partners) },
                )
            }
            Text(stringResource(Res.string.master_date_filter), style = MaterialTheme.typography.titleSmall)
            DateFilterBar(filter) { filter = it }
            MasterSectionCard(title = stringResource(Res.string.master_shops_list)) {
                ShopsTable(shops, onNavigate, token, onStatus) { shops = it }
            }
        }
    }
}

@Composable
internal fun ShopsListScreen(token: String, onNavigate: (MasterDest) -> Unit, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var shops by remember { mutableStateOf<List<ShopDto>>(emptyList()) }
    var name by rememberSaveable { mutableStateOf("") }
    var shopType by rememberSaveable { mutableStateOf("GENERAL_STORE") }
    var address by rememberSaveable { mutableStateOf("") }
    fun refresh() {
        scope.launch {
            runCatching { shops = api.shops(token) }.onFailure { onStatus(it.message.orEmpty()) }
        }
    }
    LaunchedEffect(token) { refresh() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeaderBar(stringResource(Res.string.master_shops_list))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MasterSectionCard(title = stringResource(Res.string.master_new_shop)) {
                OutlinedTextField(name, { name = it }, label = { Text(stringResource(Res.string.master_shop_name)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(shopType, { shopType = it }, label = { Text(stringResource(Res.string.master_shop_type)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(address, { address = it }, label = { Text(stringResource(Res.string.master_address)) }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    scope.launch {
                        runCatching { api.createShop(token, ShopCreate(name = name, shopType = shopType, address = address)) }
                            .onSuccess { name = ""; address = ""; refresh() }
                            .onFailure { onStatus(it.message.orEmpty()) }
                    }
                }) { Text(stringResource(Res.string.master_save)) }
            }
            MasterSectionCard { ShopsTable(shops, onNavigate, token, onStatus) { shops = it } }
        }
    }
}

@Composable
private fun ShopsTable(
    shops: List<ShopDto>,
    onNavigate: (MasterDest) -> Unit,
    token: String,
    onStatus: (String) -> Unit,
    onUpdated: (List<ShopDto>) -> Unit,
) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    Column(Modifier.fillMaxWidth().horizontalScroll(scroll)) {
        TableHeader(
            stringResource(Res.string.master_shop_id),
            stringResource(Res.string.master_shop_name),
            stringResource(Res.string.master_shop_rating),
            stringResource(Res.string.master_shop_location),
            stringResource(Res.string.master_join_date),
            stringResource(Res.string.master_shop_type),
            stringResource(Res.string.master_status),
            stringResource(Res.string.master_actions),
        )
        shops.forEach { s ->
            TableRow {
                CellText(s.id)
                CellText(s.name)
                CellText("${s.rating}")
                CellText(s.address.ifBlank { "—" })
                CellText(formatEpochDate(s.joinedAtEpochMs))
                CellText(s.shopType)
                OnOffToggle(s.active) {
                    scope.launch {
                        runCatching { api.patchShop(token, s.id, active = !s.active) }
                            .onSuccess {
                                onUpdated(shops.map { if (it.id == s.id) it.copy(active = !s.active) else it })
                            }
                            .onFailure { onStatus(it.message.orEmpty()) }
                    }
                }
                TextButton(onClick = { onNavigate(MasterDest.ShopDetail(s.id)) }) {
                    Text(stringResource(Res.string.master_view_more))
                }
            }
        }
    }
}

@Composable
internal fun ShopDetailScreen(token: String, shopId: String, onNavigate: (MasterDest) -> Unit, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<ShopDetailDto?>(null) }
    fun refresh() {
        scope.launch {
            runCatching { detail = api.shopDetail(token, shopId) }.onFailure { onStatus(it.message.orEmpty()) }
        }
    }
    LaunchedEffect(shopId) { refresh() }
    val shop = detail?.shop
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeaderBar("${stringResource(Res.string.master_shop_details)}: ${shop?.name ?: shopId}") {
            TextButton(onClick = { onNavigate(MasterDest.Shops) }) { Text(stringResource(Res.string.action_back)) }
        }
        if (shop != null) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MasterSectionCard {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            LabelValue(stringResource(Res.string.master_shop_id), shop.id)
                            LabelValue(stringResource(Res.string.master_shop_name), shop.name)
                            LabelValue(stringResource(Res.string.master_shop_location), shop.address)
                            LabelValue(stringResource(Res.string.master_shop_rating), "${shop.rating}")
                            LabelValue(stringResource(Res.string.master_join_date), formatEpochDate(shop.joinedAtEpochMs))
                            LabelValue(stringResource(Res.string.master_shop_type), shop.shopType)
                        }
                        Text(shop.imageUrl ?: "—", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall)
                    }
                }
                MasterSectionCard(title = stringResource(Res.string.master_features)) {
                    FeatureToggle(stringResource(Res.string.master_feature_voice), shop.features.voiceSearch) {
                        scope.launch {
                            val f = shop.features.copy(voiceSearch = !shop.features.voiceSearch)
                            runCatching { api.patchShop(token, shop.id, features = f) }.onSuccess { refresh() }.onFailure { onStatus(it.message.orEmpty()) }
                        }
                    }
                    FeatureToggle(stringResource(Res.string.master_feature_barcode), shop.features.barcodeSearch) {
                        scope.launch {
                            val f = shop.features.copy(barcodeSearch = !shop.features.barcodeSearch)
                            runCatching { api.patchShop(token, shop.id, features = f) }.onSuccess { refresh() }.onFailure { onStatus(it.message.orEmpty()) }
                        }
                    }
                    FeatureToggle(stringResource(Res.string.master_feature_reports), shop.features.reportGeneration) {
                        scope.launch {
                            val f = shop.features.copy(reportGeneration = !shop.features.reportGeneration)
                            runCatching { api.patchShop(token, shop.id, features = f) }.onSuccess { refresh() }.onFailure { onStatus(it.message.orEmpty()) }
                        }
                    }
                    FeatureToggle(stringResource(Res.string.master_feature_online), shop.features.onlineOrders) {
                        scope.launch {
                            val f = shop.features.copy(onlineOrders = !shop.features.onlineOrders)
                            runCatching { api.patchShop(token, shop.id, features = f) }.onSuccess { refresh() }.onFailure { onStatus(it.message.orEmpty()) }
                        }
                    }
                    FeatureToggle(stringResource(Res.string.master_feature_inventory), shop.features.inventoryAlerts) {
                        scope.launch {
                            val f = shop.features.copy(inventoryAlerts = !shop.features.inventoryAlerts)
                            runCatching { api.patchShop(token, shop.id, features = f) }.onSuccess { refresh() }.onFailure { onStatus(it.message.orEmpty()) }
                        }
                    }
                }
                MasterSectionCard(title = stringResource(Res.string.master_transactions_title)) {
                    TxnMiniTable(detail?.transactions.orEmpty()) { onNavigate(MasterDest.TxnDetail(it)) }
                }
            }
        }
    }
}

@Composable
private fun FeatureToggle(label: String, checked: Boolean, onToggle: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label)
        Switch(checked, { onToggle() })
    }
}

@Composable
internal fun ProductsListScreen(token: String, onNavigate: (MasterDest) -> Unit, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var query by rememberSaveable { mutableStateOf("") }
    fun refresh() {
        scope.launch {
            runCatching { products = api.products(token) }.onFailure { onStatus(it.message.orEmpty()) }
        }
    }
    LaunchedEffect(token) { refresh() }
    val filtered = products.filter {
        query.isBlank() || it.name.contains(query, true) || it.id.contains(query, true) || it.brandName.contains(query, true)
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeaderBar(stringResource(Res.string.master_products_title))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MasterStatCard(
                    title = stringResource(Res.string.master_card_products),
                    value = "${products.size}",
                    actionLabel = stringResource(Res.string.master_new_product),
                    onAction = { onNavigate(MasterDest.ProductEdit(null)) },
                )
            }
            OutlinedTextField(query, { query = it }, label = { Text(stringResource(Res.string.master_search)) }, modifier = Modifier.fillMaxWidth())
            MasterSectionCard {
                val scroll = rememberScrollState()
                Column(Modifier.horizontalScroll(scroll)) {
                    TableHeader(
                        stringResource(Res.string.master_product_id),
                        stringResource(Res.string.master_product_name),
                        stringResource(Res.string.master_brand),
                        stringResource(Res.string.master_company),
                        stringResource(Res.string.master_sale_price),
                        stringResource(Res.string.master_cost),
                        stringResource(Res.string.master_category),
                        stringResource(Res.string.master_status),
                        stringResource(Res.string.master_added_date),
                        stringResource(Res.string.master_actions),
                    )
                    filtered.forEach { p ->
                        TableRow(onClick = { onNavigate(MasterDest.ProductEdit(p.id)) }) {
                            CellText(p.id)
                            CellText(p.name)
                            CellText(p.brandName.ifBlank { "—" })
                            CellText(p.companyName.ifBlank { "—" })
                            CellText(formatInr(p.salePrice))
                            CellText(formatInr(p.cost))
                            CellText(p.categoryId)
                            OnOffToggle(p.active) {
                                scope.launch {
                                    runCatching {
                                        api.saveProduct(
                                            token,
                                            ProductUpsert(
                                                id = p.id,
                                                name = p.name,
                                                nameHi = p.nameHi,
                                                categoryId = p.categoryId,
                                                unit = p.unit,
                                                barcode = p.barcode,
                                                imageUrl = p.imageUrl,
                                                thumbnailUrl = p.thumbnailUrl,
                                                brandName = p.brandName,
                                                companyName = p.companyName,
                                                subcategoryId = p.subcategoryId,
                                                salePrice = p.salePrice,
                                                cost = p.cost,
                                                active = !p.active,
                                                description = p.description,
                                                sku = p.sku,
                                                stockQty = p.stockQty,
                                                lowStockThreshold = p.lowStockThreshold,
                                                tags = p.tags,
                                                weightKg = p.weightKg,
                                                dimensions = p.dimensions,
                                                variants = p.variants,
                                            ),
                                        )
                                    }.onSuccess { refresh() }.onFailure { onStatus(it.message.orEmpty()) }
                                }
                            }
                            CellText(formatEpochDate(p.addedAtEpochMs))
                            Row {
                                TextButton(onClick = { onNavigate(MasterDest.ProductEdit(p.id)) }) {
                                    Text(stringResource(Res.string.action_edit))
                                }
                                TextButton(onClick = {
                                    scope.launch {
                                        runCatching { api.deleteProduct(token, p.id) }
                                            .onSuccess { refresh() }
                                            .onFailure { onStatus(it.message.orEmpty()) }
                                    }
                                }) { Text(stringResource(Res.string.master_delete)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun ProductEditScreen(token: String, productId: String?, onNavigate: (MasterDest) -> Unit, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var nameHi by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf("cat_grocery") }
    var subcategoryId by rememberSaveable { mutableStateOf("") }
    var brand by rememberSaveable { mutableStateOf("") }
    var company by rememberSaveable { mutableStateOf("") }
    var sale by rememberSaveable { mutableStateOf("0") }
    var cost by rememberSaveable { mutableStateOf("0") }
    var unit by rememberSaveable { mutableStateOf("KG") }
    var imageUrl by rememberSaveable { mutableStateOf("") }
    var sku by rememberSaveable { mutableStateOf("") }
    var stock by rememberSaveable { mutableStateOf("0") }
    var lowStock by rememberSaveable { mutableStateOf("0") }
    var tags by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("0") }
    var dimensions by rememberSaveable { mutableStateOf("") }
    var description by rememberSaveable { mutableStateOf("") }
    var active by rememberSaveable { mutableStateOf(true) }
    var loadedId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(productId) {
        if (productId != null && loadedId != productId) {
            runCatching {
                val p = api.products(token).firstOrNull { it.id == productId } ?: return@runCatching
                loadedId = productId
                name = p.name; nameHi = p.nameHi; categoryId = p.categoryId
                subcategoryId = p.subcategoryId.orEmpty(); brand = p.brandName; company = p.companyName
                sale = p.salePrice.toString(); cost = p.cost.toString(); unit = p.unit
                imageUrl = p.imageUrl.orEmpty(); sku = p.sku; stock = p.stockQty.toString()
                lowStock = p.lowStockThreshold.toString(); tags = p.tags; weight = p.weightKg.toString()
                dimensions = p.dimensions; description = p.description; active = p.active
            }.onFailure { onStatus(it.message.orEmpty()) }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeaderBar(
            if (productId == null) stringResource(Res.string.master_new_product)
            else "${stringResource(Res.string.master_edit_product)}: $name",
        ) {
            TextButton(onClick = { onNavigate(MasterDest.Products) }) { Text(stringResource(Res.string.master_cancel)) }
        }
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(Res.string.master_product_name)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(nameHi, { nameHi = it }, label = { Text(stringResource(Res.string.field_name_hi)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(brand, { brand = it }, label = { Text(stringResource(Res.string.master_brand)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(company, { company = it }, label = { Text(stringResource(Res.string.master_company)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(categoryId, { categoryId = it }, label = { Text(stringResource(Res.string.master_category)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(subcategoryId, { subcategoryId = it }, label = { Text(stringResource(Res.string.master_subcategory)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(sale, { sale = it }, label = { Text(stringResource(Res.string.master_sale_price)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(cost, { cost = it }, label = { Text(stringResource(Res.string.master_cost)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(unit, { unit = it }, label = { Text(stringResource(Res.string.field_unit)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(imageUrl, { imageUrl = it }, label = { Text(stringResource(Res.string.master_image_url)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(sku, { sku = it }, label = { Text(stringResource(Res.string.master_sku)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(stock, { stock = it }, label = { Text(stringResource(Res.string.master_stock)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(lowStock, { lowStock = it }, label = { Text(stringResource(Res.string.master_low_stock)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(tags, { tags = it }, label = { Text(stringResource(Res.string.master_tags)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(weight, { weight = it }, label = { Text(stringResource(Res.string.master_weight)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(dimensions, { dimensions = it }, label = { Text(stringResource(Res.string.master_dimensions)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(description, { description = it }, label = { Text(stringResource(Res.string.master_description)) }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            FeatureToggle(stringResource(Res.string.master_status), active) { active = !active }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    scope.launch {
                        runCatching {
                            api.saveProduct(
                                token,
                                ProductUpsert(
                                    id = productId,
                                    name = name,
                                    nameHi = nameHi,
                                    categoryId = categoryId,
                                    unit = unit,
                                    imageUrl = imageUrl.ifBlank { null },
                                    thumbnailUrl = imageUrl.ifBlank { null },
                                    brandName = brand,
                                    companyName = company,
                                    subcategoryId = subcategoryId.ifBlank { null },
                                    salePrice = sale.toDoubleOrNull() ?: 0.0,
                                    cost = cost.toDoubleOrNull() ?: 0.0,
                                    active = active,
                                    description = description,
                                    sku = sku,
                                    stockQty = stock.toDoubleOrNull() ?: 0.0,
                                    lowStockThreshold = lowStock.toDoubleOrNull() ?: 0.0,
                                    tags = tags,
                                    weightKg = weight.toDoubleOrNull() ?: 0.0,
                                    dimensions = dimensions,
                                ),
                            )
                        }.onSuccess { onNavigate(MasterDest.Products) }
                            .onFailure { onStatus(it.message.orEmpty()) }
                    }
                }) { Text(stringResource(Res.string.master_save_changes)) }
                OutlinedButton(onClick = { onNavigate(MasterDest.Products) }) { Text(stringResource(Res.string.master_cancel)) }
            }
        }
    }
}

@Composable
internal fun TransactionsScreen(token: String, onNavigate: (MasterDest) -> Unit, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var filter by rememberSaveable { mutableStateOf(AppConstants.DateFilter.TODAY) }
    var summary by remember { mutableStateOf<TxnSummaryDto?>(null) }
    fun refresh() {
        scope.launch {
            val (from, to) = dateFilterRange(filter)
            runCatching { summary = api.transactions(token, from, to) }.onFailure { onStatus(it.message.orEmpty()) }
        }
    }
    LaunchedEffect(token, filter) { refresh() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeaderBar(stringResource(Res.string.master_transactions_title))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MasterStatCard(stringResource(Res.string.master_card_transactions), formatInr(summary?.amount ?: 0.0))
            }
            Text(stringResource(Res.string.master_date_filter))
            DateFilterBar(filter) { filter = it }
            MasterSectionCard {
                TxnFullTable(summary?.transactions.orEmpty(), token, onNavigate, onStatus) { refresh() }
            }
        }
    }
}

@Composable
private fun TxnMiniTable(rows: List<TxnDto>, onOpen: (String) -> Unit) {
    val scroll = rememberScrollState()
    Column(Modifier.horizontalScroll(scroll)) {
        TableHeader(
            stringResource(Res.string.master_order_id),
            stringResource(Res.string.master_transaction_no),
            stringResource(Res.string.master_item_details),
            stringResource(Res.string.master_customer),
            stringResource(Res.string.master_total),
        )
        rows.forEach { t ->
            TableRow(onClick = { onOpen(t.orderId) }) {
                CellText(t.orderId)
                CellText(t.transactionNo)
                CellText(t.itemsSummary)
                CellText("${t.customerName}\n${t.customerPhone}")
                CellText(formatInr(t.total))
            }
        }
    }
}

@Composable
private fun TxnFullTable(
    rows: List<TxnDto>,
    token: String,
    onNavigate: (MasterDest) -> Unit,
    onStatus: (String) -> Unit,
    onChanged: () -> Unit,
) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    Column(Modifier.horizontalScroll(scroll)) {
        TableHeader(
            stringResource(Res.string.master_order_id),
            stringResource(Res.string.master_transaction_no),
            stringResource(Res.string.master_item_details),
            stringResource(Res.string.master_customer),
            stringResource(Res.string.master_shop_details_col),
            stringResource(Res.string.master_offers),
            stringResource(Res.string.master_charges),
            stringResource(Res.string.master_total),
            stringResource(Res.string.master_paid),
            stringResource(Res.string.master_actions),
        )
        rows.forEach { t ->
            TableRow(onClick = { onNavigate(MasterDest.TxnDetail(t.orderId)) }) {
                CellText(t.orderId)
                CellText(t.transactionNo)
                CellText(t.itemsSummary)
                CellText("${t.customerName}\n${t.customerPhone}\n${t.customerAddress}")
                CellText("${t.shopName}\n[${t.shopId}]")
                CellText(formatInr(t.offers))
                CellText(formatInr(t.charges))
                CellText(formatInr(t.total))
                CellText(formatInr(t.paid))
                Row {
                    TextButton(onClick = {
                        scope.launch {
                            runCatching { api.refundOrder(token, t.orderId) }
                                .onSuccess { onChanged() }
                                .onFailure { onStatus(it.message.orEmpty()) }
                        }
                    }) { Text(stringResource(Res.string.master_refund)) }
                    TextButton(onClick = {
                        scope.launch {
                            runCatching { api.cancelOrder(token, t.orderId) }
                                .onSuccess { onChanged() }
                                .onFailure { onStatus(it.message.orEmpty()) }
                        }
                    }) { Text(stringResource(Res.string.master_cancel_order)) }
                }
            }
        }
    }
}

@Composable
internal fun TxnDetailScreen(token: String, orderId: String, onNavigate: (MasterDest) -> Unit, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var txn by remember { mutableStateOf<TxnDto?>(null) }
    LaunchedEffect(orderId) {
        runCatching { txn = api.orderDetail(token, orderId) }.onFailure { onStatus(it.message.orEmpty()) }
    }
    val t = txn
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeaderBar("${stringResource(Res.string.master_txn_details)}: #$orderId") {
            TextButton(onClick = { onNavigate(MasterDest.Transactions) }) { Text(stringResource(Res.string.action_back)) }
        }
        if (t != null) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MasterSectionCard {
                    LabelValue(stringResource(Res.string.master_order_id), t.orderId)
                    LabelValue(stringResource(Res.string.master_transaction_no), t.transactionNo)
                    LabelValue(stringResource(Res.string.master_status), t.status)
                    LabelValue(stringResource(Res.string.master_shop_name), "${t.shopName} [${t.shopId}]")
                    LabelValue(stringResource(Res.string.master_customer), t.customerName)
                    LabelValue(stringResource(Res.string.master_contact), t.customerPhone)
                    LabelValue(stringResource(Res.string.master_address), t.customerAddress)
                    LabelValue(stringResource(Res.string.master_offers), formatInr(t.offers))
                    LabelValue(stringResource(Res.string.master_charges), formatInr(t.charges))
                    LabelValue(stringResource(Res.string.master_total), formatInr(t.total))
                    LabelValue(stringResource(Res.string.master_paid), formatInr(t.paid))
                    if (t.partnerName != null) LabelValue(stringResource(Res.string.master_partner_id), "${t.partnerName} [${t.partnerId}]")
                }
                MasterSectionCard(title = stringResource(Res.string.master_item_details)) {
                    t.items.forEach { item ->
                        Text("${item.productName} · ${item.quantity} ${item.unit} · ${formatInr(item.unitPrice)}")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            runCatching { api.refundOrder(token, orderId) }
                                .onSuccess { txn = api.orderDetail(token, orderId) }
                                .onFailure { onStatus(it.message.orEmpty()) }
                        }
                    }) { Text(stringResource(Res.string.master_refund)) }
                    OutlinedButton(onClick = {
                        scope.launch {
                            runCatching { api.cancelOrder(token, orderId) }
                                .onSuccess { txn = api.orderDetail(token, orderId) }
                                .onFailure { onStatus(it.message.orEmpty()) }
                        }
                    }) { Text(stringResource(Res.string.master_cancel_order)) }
                }
            }
        }
    }
}

@Composable
internal fun UsersScreen(token: String, onNavigate: (MasterDest) -> Unit, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var filter by rememberSaveable { mutableStateOf(AppConstants.DateFilter.YEARLY) }
    var users by remember { mutableStateOf<List<UserDto>>(emptyList()) }
    fun refresh() {
        scope.launch {
            val (from, to) = dateFilterRange(filter)
            runCatching { users = api.users(token, from, to) }.onFailure { onStatus(it.message.orEmpty()) }
        }
    }
    LaunchedEffect(token, filter) { refresh() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeaderBar(stringResource(Res.string.master_users_title))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MasterStatCard(stringResource(Res.string.master_card_users), "${users.size}")
            DateFilterBar(filter) { filter = it }
            MasterSectionCard {
                val scroll = rememberScrollState()
                Column(Modifier.horizontalScroll(scroll)) {
                    TableHeader(
                        stringResource(Res.string.master_user_id),
                        stringResource(Res.string.master_name),
                        stringResource(Res.string.master_contact),
                        stringResource(Res.string.master_address),
                        stringResource(Res.string.master_status),
                        stringResource(Res.string.master_join_date),
                        stringResource(Res.string.master_actions),
                    )
                    users.forEach { u ->
                        TableRow {
                            CellText(u.id)
                            CellText(u.name)
                            CellText(u.phone)
                            CellText(u.address)
                            OnOffToggle(u.active) {
                                scope.launch {
                                    runCatching { api.patchUser(token, u.id, !u.active) }
                                        .onSuccess { refresh() }
                                        .onFailure { onStatus(it.message.orEmpty()) }
                                }
                            }
                            CellText(formatEpochDate(u.joinedAtEpochMs))
                            TextButton(onClick = { onNavigate(MasterDest.UserDetail(u.id)) }) {
                                Text(stringResource(Res.string.master_view_more))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun UserDetailScreen(token: String, userId: String, onNavigate: (MasterDest) -> Unit, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    var detail by remember { mutableStateOf<UserDetailDto?>(null) }
    LaunchedEffect(userId) {
        runCatching { detail = api.userDetail(token, userId) }.onFailure { onStatus(it.message.orEmpty()) }
    }
    val u = detail?.user
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeaderBar("${stringResource(Res.string.master_user_details)}: #$userId") {
            TextButton(onClick = { onNavigate(MasterDest.Users) }) { Text(stringResource(Res.string.action_back)) }
        }
        if (u != null) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MasterSectionCard {
                    LabelValue(stringResource(Res.string.master_name), u.name)
                    LabelValue(stringResource(Res.string.master_contact), u.phone)
                    LabelValue(stringResource(Res.string.master_address), u.address)
                }
                MasterSectionCard(title = stringResource(Res.string.master_order_history)) {
                    TxnMiniTable(detail?.orders.orEmpty()) { onNavigate(MasterDest.TxnDetail(it)) }
                }
            }
        }
    }
}

@Composable
internal fun PartnersScreen(token: String, onNavigate: (MasterDest) -> Unit, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var filter by rememberSaveable { mutableStateOf(AppConstants.DateFilter.YEARLY) }
    var partners by remember { mutableStateOf<List<PartnerDto>>(emptyList()) }
    fun refresh() {
        scope.launch {
            val (from, to) = dateFilterRange(filter)
            runCatching { partners = api.partners(token, from, to) }.onFailure { onStatus(it.message.orEmpty()) }
        }
    }
    LaunchedEffect(token, filter) { refresh() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeaderBar(stringResource(Res.string.master_partners_title))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MasterStatCard(stringResource(Res.string.master_card_partners), "${partners.size}")
            DateFilterBar(filter) { filter = it }
            MasterSectionCard {
                val scroll = rememberScrollState()
                Column(Modifier.horizontalScroll(scroll)) {
                    TableHeader(
                        stringResource(Res.string.master_partner_id),
                        stringResource(Res.string.master_name),
                        stringResource(Res.string.master_contact),
                        stringResource(Res.string.master_address),
                        stringResource(Res.string.master_vehicle_number),
                        stringResource(Res.string.master_vehicle_name),
                        stringResource(Res.string.master_vehicle_brand),
                        stringResource(Res.string.master_vehicle_color),
                        stringResource(Res.string.master_vehicle_type),
                        stringResource(Res.string.master_status),
                        stringResource(Res.string.master_join_date),
                        stringResource(Res.string.master_actions),
                    )
                    partners.forEach { p ->
                        TableRow {
                            CellText(p.id)
                            CellText(p.name)
                            CellText("${p.phone}\n${p.email}")
                            CellText(p.address)
                            CellText(p.vehicleNumber)
                            CellText(p.vehicleName.ifBlank { "—" })
                            CellText(p.vehicleBrand.ifBlank { "—" })
                            CellText(p.vehicleColor.ifBlank { "—" })
                            CellText(p.vehicleType)
                            OnOffToggle(p.active) {
                                scope.launch {
                                    runCatching { api.patchPartner(token, p.id, !p.active) }
                                        .onSuccess { refresh() }
                                        .onFailure { onStatus(it.message.orEmpty()) }
                                }
                            }
                            CellText(formatEpochDate(p.joinedAtEpochMs))
                            TextButton(onClick = { onNavigate(MasterDest.PartnerDetail(p.id)) }) {
                                Text(stringResource(Res.string.master_view_more))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PartnerDetailScreen(token: String, partnerId: String, onNavigate: (MasterDest) -> Unit, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    var detail by remember { mutableStateOf<PartnerDetailDto?>(null) }
    LaunchedEffect(partnerId) {
        runCatching { detail = api.partnerDetail(token, partnerId) }.onFailure { onStatus(it.message.orEmpty()) }
    }
    val p = detail?.partner
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeaderBar("${stringResource(Res.string.master_partner_details)}: #$partnerId") {
            TextButton(onClick = { onNavigate(MasterDest.Partners) }) { Text(stringResource(Res.string.action_back)) }
        }
        if (p != null) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MasterSectionCard {
                    LabelValue(stringResource(Res.string.master_name), p.name)
                    LabelValue(stringResource(Res.string.master_contact), p.phone)
                    LabelValue(stringResource(Res.string.master_email), p.email)
                    LabelValue(stringResource(Res.string.master_address), p.address)
                    LabelValue(stringResource(Res.string.master_vehicle_number), p.vehicleNumber)
                    LabelValue(stringResource(Res.string.master_vehicle_name), p.vehicleName)
                    LabelValue(stringResource(Res.string.master_vehicle_brand), p.vehicleBrand)
                    LabelValue(stringResource(Res.string.master_vehicle_color), p.vehicleColor)
                    LabelValue(stringResource(Res.string.master_vehicle_type), p.vehicleType)
                    LabelValue(stringResource(Res.string.master_join_date), formatEpochDate(p.joinedAtEpochMs))
                }
                MasterSectionCard(title = stringResource(Res.string.master_accepted_orders)) {
                    TxnMiniTable(detail?.acceptedOrders.orEmpty()) { onNavigate(MasterDest.TxnDetail(it)) }
                }
                MasterSectionCard(title = stringResource(Res.string.master_cancelled_orders)) {
                    TxnMiniTable(detail?.cancelledOrders.orEmpty()) { onNavigate(MasterDest.TxnDetail(it)) }
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MasterStatCard(stringResource(Res.string.master_total_delivered), "${detail?.totalDeliveredOrders ?: 0}")
                    MasterStatCard(stringResource(Res.string.master_total_earnings), formatInr(detail?.totalEarnings ?: 0.0))
                }
            }
        }
    }
}

@Composable
internal fun PlatformScreen(token: String, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    var stats by remember { mutableStateOf<AdminDashboardDto?>(null) }
    LaunchedEffect(token) {
        runCatching { stats = api.dashboard(token) }.onFailure { onStatus(it.message.orEmpty()) }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeaderBar(stringResource(Res.string.master_platform_title))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MasterStatCard(stringResource(Res.string.master_card_shops), "${stats?.shopCount ?: 0}")
                MasterStatCard(stringResource(Res.string.master_card_products), "${stats?.productCount ?: 0}")
                MasterStatCard(stringResource(Res.string.master_card_transactions), formatInr(stats?.transactionAmount ?: 0.0))
                MasterStatCard(stringResource(Res.string.master_card_users), "${stats?.userCount ?: 0}")
                MasterStatCard(stringResource(Res.string.master_card_partners), "${stats?.partnerCount ?: 0}")
            }
        }
    }
}

@Composable
internal fun SettingsScreen(token: String, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var categories by remember { mutableStateOf<List<CategoryDto>>(emptyList()) }
    var shopTypes by remember { mutableStateOf<List<ShopTypeDto>>(emptyList()) }
    var catName by rememberSaveable { mutableStateOf("") }
    var parentId by rememberSaveable { mutableStateOf("") }
    var typeName by rememberSaveable { mutableStateOf("") }
    fun refresh() {
        scope.launch {
            runCatching {
                categories = api.categories(token)
                shopTypes = api.shopTypes(token)
            }.onFailure { onStatus(it.message.orEmpty()) }
        }
    }
    LaunchedEffect(token) { refresh() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PageHeaderBar(stringResource(Res.string.master_settings_title))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            MasterSectionCard(title = stringResource(Res.string.master_tab_categories)) {
                Text(stringResource(Res.string.master_new_category), fontWeight = FontWeight.SemiBold)
                OutlinedTextField(catName, { catName = it }, label = { Text(stringResource(Res.string.field_name)) }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(parentId, { parentId = it }, label = { Text(stringResource(Res.string.master_parent_category)) }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    scope.launch {
                        runCatching { api.saveCategory(token, CategoryUpsert(name = catName, parentId = parentId.ifBlank { null })) }
                            .onSuccess { catName = ""; parentId = ""; refresh() }
                            .onFailure { onStatus(it.message.orEmpty()) }
                    }
                }) { Text(stringResource(Res.string.master_save)) }
                categories.forEach { c ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${c.name} [${c.id}] parent=${c.parentId.orEmpty()}")
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { api.deleteCategory(token, c.id) }
                                    .onSuccess { refresh() }
                                    .onFailure { onStatus(it.message.orEmpty()) }
                            }
                        }) { Text(stringResource(Res.string.master_delete)) }
                    }
                }
            }
            MasterSectionCard(title = stringResource(Res.string.master_tab_shop_types)) {
                Text(stringResource(Res.string.master_new_shop_type), fontWeight = FontWeight.SemiBold)
                OutlinedTextField(typeName, { typeName = it }, label = { Text(stringResource(Res.string.field_name)) }, modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    scope.launch {
                        runCatching { api.saveShopType(token, ShopTypeUpsert(name = typeName)) }
                            .onSuccess { typeName = ""; refresh() }
                            .onFailure { onStatus(it.message.orEmpty()) }
                    }
                }) { Text(stringResource(Res.string.master_save)) }
                shopTypes.forEach { t ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${t.name} [${t.id}]")
                        TextButton(onClick = {
                            scope.launch {
                                runCatching { api.deleteShopType(token, t.id) }
                                    .onSuccess { refresh() }
                                    .onFailure { onStatus(it.message.orEmpty()) }
                            }
                        }) { Text(stringResource(Res.string.master_delete)) }
                    }
                }
            }
        }
    }
}
