package org.bhargav.pansariwala.master

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
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
import androidx.compose.foundation.horizontalScroll
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
import org.bhargav.pansariwala.designsystem.AdaptivePane
import org.bhargav.pansariwala.designsystem.WindowWidthClass
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
import org.bhargav.pansariwala.util.AppConstants
import pansariwala.shared.generated.resources.master_add_new_shop_subtitle
import pansariwala.shared.generated.resources.master_add_new_shop_title
import pansariwala.shared.generated.resources.master_create_shop
import pansariwala.shared.generated.resources.master_delivery_info
import pansariwala.shared.generated.resources.master_download_invoice
import pansariwala.shared.generated.resources.master_general_info
import pansariwala.shared.generated.resources.master_inventory_section
import pansariwala.shared.generated.resources.master_issue_refund
import pansariwala.shared.generated.resources.master_items_in_order
import pansariwala.shared.generated.resources.master_last_updated
import pansariwala.shared.generated.resources.master_logistics
import pansariwala.shared.generated.resources.master_order_info
import pansariwala.shared.generated.resources.master_order_timeline
import pansariwala.shared.generated.resources.master_payment_info
import pansariwala.shared.generated.resources.master_pricing
import pansariwala.shared.generated.resources.master_print_summary
import pansariwala.shared.generated.resources.master_shop_image
import pansariwala.shared.generated.resources.master_shop_orders
import pansariwala.shared.generated.resources.master_total_rating
import pansariwala.shared.generated.resources.master_owner_email
import pansariwala.shared.generated.resources.master_owner_name
import pansariwala.shared.generated.resources.master_owner_phone
import pansariwala.shared.generated.resources.master_street_address
import pansariwala.shared.generated.resources.master_city
import pansariwala.shared.generated.resources.master_state
import pansariwala.shared.generated.resources.master_zip
import pansariwala.shared.generated.resources.master_country
import pansariwala.shared.generated.resources.master_business_details
import pansariwala.shared.generated.resources.master_registration_number
import pansariwala.shared.generated.resources.master_tax_id
import pansariwala.shared.generated.resources.master_operating_hours
import pansariwala.shared.generated.resources.master_hours_start
import pansariwala.shared.generated.resources.master_hours_end
import pansariwala.shared.generated.resources.master_effective_price
import pansariwala.shared.generated.resources.master_features
import pansariwala.shared.generated.resources.master_number_of_shops
import pansariwala.shared.generated.resources.master_stars_label
import pansariwala.shared.generated.resources.master_active
import pansariwala.shared.generated.resources.master_sales_overview
import pansariwala.shared.generated.resources.master_txn_trends
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
    val s = stats
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MasterTopBar(stringResource(Res.string.master_console_title))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            DashboardTopCards(
                shopCount = s?.shopCount ?: 0,
                txnAmount = s?.transactionAmount ?: 0.0,
                userCount = s?.userCount ?: 0,
                txnFilter = filter,
                onTxnFilter = { filter = it },
                onAddShop = { onNavigate(MasterDest.ShopCreate) },
                onOpenShops = { onNavigate(MasterDest.Shops) },
                onOpenUsers = { onNavigate(MasterDest.Users) },
                onOpenTransactions = { onNavigate(MasterDest.Transactions) },
            )
            AdaptivePane(Modifier.fillMaxWidth()) { wc ->
                if (wc == WindowWidthClass.Compact) {
                    SimpleBarChart(stringResource(Res.string.master_sales_overview), s?.salesByWeekday.orEmpty())
                    SimpleLineChart(stringResource(Res.string.master_txn_trends), s?.txnTrendByMonth.orEmpty())
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SimpleBarChart(
                            stringResource(Res.string.master_sales_overview),
                            s?.salesByWeekday.orEmpty(),
                            modifier = Modifier.weight(1f),
                        )
                        SimpleLineChart(
                            stringResource(Res.string.master_txn_trends),
                            s?.txnTrendByMonth.orEmpty(),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            MasterSectionCard(title = stringResource(Res.string.master_shops_list)) {
                DashboardShopsTable(shops, onNavigate, token, onStatus) { shops = it }
            }
        }
    }
}

@Composable
internal fun ShopsListScreen(token: String, onNavigate: (MasterDest) -> Unit, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var shops by remember { mutableStateOf<List<ShopDto>>(emptyList()) }
    fun refresh() {
        scope.launch {
            runCatching { shops = api.shops(token) }.onFailure { onStatus(it.message.orEmpty()) }
        }
    }
    LaunchedEffect(token) { refresh() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MasterTopBar(stringResource(Res.string.master_shops_list))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MasterStatCard(stringResource(Res.string.master_number_of_shops), "${shops.size}")
                AddProductPromoCard(
                    onClick = { onNavigate(MasterDest.ShopCreate) },
                    title = stringResource(Res.string.master_new_shop),
                )
            }
            MasterSectionCard(title = stringResource(Res.string.master_shops_list)) {
                DashboardShopsTable(shops, onNavigate, token, onStatus) { shops = it }
            }
        }
    }
}

@Composable
internal fun ShopCreateScreen(token: String, onNavigate: (MasterDest) -> Unit, onStatus: (String) -> Unit) {
    ShopEditorScreen(token = token, shopId = null, onNavigate = onNavigate, onStatus = onStatus)
}

@Composable
internal fun ShopEditScreen(token: String, shopId: String, onNavigate: (MasterDest) -> Unit, onStatus: (String) -> Unit) {
    ShopEditorScreen(token = token, shopId = shopId, onNavigate = onNavigate, onStatus = onStatus)
}

@Composable
private fun HoursRow(day: String, start: String, end: String, onStart: (String) -> Unit, onEnd: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(day, modifier = Modifier.width(36.dp), fontWeight = FontWeight.Medium)
        OutlinedTextField(start, onStart, label = { Text(stringResource(Res.string.master_hours_start)) }, modifier = Modifier.weight(1f), singleLine = true)
        OutlinedTextField(end, onEnd, label = { Text(stringResource(Res.string.master_hours_end)) }, modifier = Modifier.weight(1f), singleLine = true)
    }
}

@Composable
private fun DashboardShopsTable(
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
            stringResource(Res.string.master_shop_image),
            stringResource(Res.string.master_actions),
        )
        shops.forEach { s ->
            TableRow {
                CellText(s.id)
                CellText(s.name)
                RatingText(s.rating)
                CellText(s.address.ifBlank { "Location" })
                CellText(formatEpochDate(s.joinedAtEpochMs))
                CellText(shopTypeDisplayName(s.shopType))
                StatusChip(s.active)
                ImageThumb(s.imageUrl)
                ViewMoreButton { onNavigate(MasterDest.ShopDetail(s.id)) }
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
        MasterTopBar("${stringResource(Res.string.master_shop_details)}: ${shop?.name ?: shopId}")
        if (shop != null) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MasterSectionCard {
                    AdaptivePane(Modifier.fillMaxWidth()) { wc ->
                        if (wc == WindowWidthClass.Compact) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                ShopInfoGrid(shop)
                                ImageThumb(shop.imageUrl, Modifier.size(160.dp).fillMaxWidth())
                            }
                        } else {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    ShopInfoGrid(shop)
                                }
                                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                                    ImageThumb(shop.imageUrl, Modifier.size(160.dp))
                                }
                            }
                        }
                    }
                }
                ResponsiveColumns(
                    left = {
                        MasterSectionCard(title = stringResource(Res.string.master_features)) {
                            FeatureToggleRow(stringResource(Res.string.master_feature_voice), shop.features.voiceSearch) {
                                scope.launch {
                                    val f = shop.features.copy(voiceSearch = !shop.features.voiceSearch)
                                    runCatching { api.patchShop(token, shop.id, features = f) }.onSuccess { refresh() }.onFailure { onStatus(it.message.orEmpty()) }
                                }
                            }
                            FeatureToggleRow(stringResource(Res.string.master_feature_barcode), shop.features.barcodeSearch) {
                                scope.launch {
                                    val f = shop.features.copy(barcodeSearch = !shop.features.barcodeSearch)
                                    runCatching { api.patchShop(token, shop.id, features = f) }.onSuccess { refresh() }.onFailure { onStatus(it.message.orEmpty()) }
                                }
                            }
                            FeatureToggleRow(stringResource(Res.string.master_feature_reports), shop.features.reportGeneration) {
                                scope.launch {
                                    val f = shop.features.copy(reportGeneration = !shop.features.reportGeneration)
                                    runCatching { api.patchShop(token, shop.id, features = f) }.onSuccess { refresh() }.onFailure { onStatus(it.message.orEmpty()) }
                                }
                            }
                            FeatureToggleRow(stringResource(Res.string.master_feature_online), shop.features.onlineOrders) {
                                scope.launch {
                                    val f = shop.features.copy(onlineOrders = !shop.features.onlineOrders)
                                    runCatching { api.patchShop(token, shop.id, features = f) }.onSuccess { refresh() }.onFailure { onStatus(it.message.orEmpty()) }
                                }
                            }
                            FeatureToggleRow(stringResource(Res.string.master_feature_inventory), shop.features.inventoryAlerts) {
                                scope.launch {
                                    val f = shop.features.copy(inventoryAlerts = !shop.features.inventoryAlerts)
                                    runCatching { api.patchShop(token, shop.id, features = f) }.onSuccess { refresh() }.onFailure { onStatus(it.message.orEmpty()) }
                                }
                            }
                        }
                    },
                    right = {
                        MasterSectionCard(title = stringResource(Res.string.master_transactions_title)) {
                            StatsGrid(
                                listOf(
                                    stringResource(Res.string.master_shop_id) to shop.id,
                                    stringResource(Res.string.master_total_rating) to "${shop.ratingCount}",
                                    stringResource(Res.string.master_shop_orders) to "${shop.rating} ★",
                                    stringResource(Res.string.master_card_users) to "${detail?.uniqueCustomers ?: 0}",
                                ),
                            )
                        }
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onNavigate(MasterDest.ShopEdit(shop.id)) }) {
                        Text(stringResource(Res.string.action_edit))
                    }
                    OutlinedButton(onClick = { onNavigate(MasterDest.Shops) }) {
                        Text(stringResource(Res.string.action_back))
                    }
                }
            }
        }
    }
}

@Composable
private fun ShopInfoGrid(shop: ShopDto) {
    LabelValue(stringResource(Res.string.master_shop_id), shop.id)
    LabelValue(stringResource(Res.string.master_shop_name), shop.name)
    LabelValue(stringResource(Res.string.master_shop_location), shop.address.ifBlank { "Location" })
    LabelValue(stringResource(Res.string.master_shop_rating), "${shop.rating} ${stringResource(Res.string.master_stars_label)}")
    LabelValue(stringResource(Res.string.master_join_date), formatEpochDate(shop.joinedAtEpochMs))
    LabelValue(stringResource(Res.string.master_shop_type), shopTypeDisplayName(shop.shopType))
}

@Composable
private fun FeatureToggle(label: String, checked: Boolean, onToggle: () -> Unit) {
    FeatureToggleRow(label, checked, onToggle)
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
        MasterTopBar(stringResource(Res.string.master_products_title))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MasterStatCard(title = stringResource(Res.string.master_card_products), value = "${products.size}")
                AddProductPromoCard(onClick = { onNavigate(MasterDest.ProductEdit(null)) })
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
    var variants by remember { mutableStateOf<List<ProductVariantDto>>(emptyList()) }
    var loadedId by remember { mutableStateOf<String?>(null) }
    val saleVal = sale.toDoubleOrNull() ?: 0.0
    val effective = saleVal
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
                variants = p.variants
            }.onFailure { onStatus(it.message.orEmpty()) }
        }
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MasterTopBar(
            if (productId == null) stringResource(Res.string.master_new_product)
            else "${stringResource(Res.string.master_edit_product)}: $name",
        )
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AdaptivePane(Modifier.fillMaxWidth()) { wc ->
                val cols = if (wc == WindowWidthClass.Compact) 1 else 3
                FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp), maxItemsInEachRow = cols) {
                    Column(Modifier.widthIn(min = 240.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ImageThumb(imageUrl.ifBlank { null }, Modifier.size(120.dp))
                        OutlinedTextField(imageUrl, { imageUrl = it }, label = { Text(stringResource(Res.string.master_image_url)) }, modifier = Modifier.fillMaxWidth())
                        Text(stringResource(Res.string.master_general_info), fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(name, { name = it }, label = { Text(stringResource(Res.string.master_product_name)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(nameHi, { nameHi = it }, label = { Text(stringResource(Res.string.field_name_hi)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(brand, { brand = it }, label = { Text(stringResource(Res.string.master_brand)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(company, { company = it }, label = { Text(stringResource(Res.string.master_company)) }, modifier = Modifier.fillMaxWidth())
                        Text(stringResource(Res.string.master_pricing), fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(sale, { sale = it }, label = { Text(stringResource(Res.string.master_sale_price)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(cost, { cost = it }, label = { Text(stringResource(Res.string.master_cost)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(unit, { unit = it }, label = { Text(stringResource(Res.string.field_unit)) }, modifier = Modifier.fillMaxWidth())
                        Text("${stringResource(Res.string.master_effective_price)}: ${formatInr(effective)}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Column(Modifier.widthIn(min = 240.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(categoryId, { categoryId = it }, label = { Text(stringResource(Res.string.master_category)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(subcategoryId, { subcategoryId = it }, label = { Text(stringResource(Res.string.master_subcategory)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(tags, { tags = it }, label = { Text(stringResource(Res.string.master_tags)) }, modifier = Modifier.fillMaxWidth())
                        VariantsEditor(variants = variants, onChange = { variants = it })
                        DescriptionEditor(value = description, onValueChange = { description = it })
                    }
                    Column(Modifier.widthIn(min = 240.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(stringResource(Res.string.master_inventory_section), fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(sku, { sku = it }, label = { Text(stringResource(Res.string.master_sku)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(stock, { stock = it }, label = { Text(stringResource(Res.string.master_stock)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(lowStock, { lowStock = it }, label = { Text(stringResource(Res.string.master_low_stock)) }, modifier = Modifier.fillMaxWidth())
                        FeatureToggle(stringResource(Res.string.master_status), active) { active = !active }
                        Text(stringResource(Res.string.master_logistics), fontWeight = FontWeight.SemiBold)
                        OutlinedTextField(weight, { weight = it }, label = { Text(stringResource(Res.string.master_weight)) }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(dimensions, { dimensions = it }, label = { Text(stringResource(Res.string.master_dimensions)) }, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
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
                                    variants = variants.filter { it.name.isNotBlank() },
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
        MasterTopBar(stringResource(Res.string.master_transactions_title))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MasterStatCard(stringResource(Res.string.master_card_transactions), formatInr(summary?.amount ?: 0.0))
                DateFilterSummaryCard(
                    title = stringResource(Res.string.master_date_filter),
                    value = "${summary?.count ?: 0} orders",
                    selected = filter,
                    onSelect = { filter = it },
                )
            }
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
    Column(Modifier.horizontalScroll(scroll).widthIn(min = 1100.dp)) {
        NestedTxnTableHeader()
        rows.forEach { t ->
            NestedTxnTableRow(
                txn = t,
                onClick = { onNavigate(MasterDest.TxnDetail(t.orderId)) },
                actions = {
                    Column {
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
                        }) { Text(stringResource(Res.string.master_cancel_order), color = MaterialTheme.colorScheme.error) }
                    }
                },
            )
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
        MasterTopBar("${stringResource(Res.string.master_txn_details)}: #$orderId")
        if (t != null) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ResponsiveColumns(
                    left = {
                        MasterSectionCard(title = stringResource(Res.string.master_order_info)) {
                            LabelValue(stringResource(Res.string.master_order_id), t.orderId)
                            LabelValue(stringResource(Res.string.master_transaction_no), t.transactionNo)
                            LabelValue(stringResource(Res.string.master_status), t.status)
                            LabelValue(stringResource(Res.string.master_shop_name), "${t.shopName} [${t.shopId}]")
                        }
                        MasterSectionCard(title = stringResource(Res.string.master_customer)) {
                            LabelValue(stringResource(Res.string.master_name), t.customerName)
                            LabelValue(stringResource(Res.string.master_contact), t.customerPhone)
                            LabelValue(stringResource(Res.string.master_address), t.customerAddress)
                        }
                        MasterSectionCard(title = stringResource(Res.string.master_payment_info)) {
                            LabelValue(stringResource(Res.string.master_offers), formatInr(t.offers))
                            LabelValue(stringResource(Res.string.master_charges), formatInr(t.charges))
                            LabelValue(stringResource(Res.string.master_total), formatInr(t.total))
                            LabelValue(stringResource(Res.string.master_paid), formatInr(t.paid))
                        }
                        MasterSectionCard(title = stringResource(Res.string.master_delivery_info)) {
                            if (t.partnerName != null) LabelValue(stringResource(Res.string.master_partner_id), "${t.partnerName} [${t.partnerId}]")
                            t.partnerVehicleReg?.let { LabelValue(stringResource(Res.string.master_vehicle_number), it) }
                            t.deliveryDurationMin?.let { LabelValue(stringResource(Res.string.master_delivery_info), "${it} min") }
                        }
                    },
                    right = {
                        MasterSectionCard(title = stringResource(Res.string.master_items_in_order)) {
                            t.items.forEach { item ->
                                Text("${item.productName} · ${item.quantity} ${item.unit} · ${formatInr(item.unitPrice)}")
                            }
                        }
                        MasterSectionCard(title = stringResource(Res.string.master_order_timeline)) {
                            Text("${formatEpochDate(t.createdAtEpochMs)} · ${t.status}")
                            if (t.refundId != null) Text("Refund: ${t.refundId}")
                        }
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        scope.launch {
                            runCatching { api.refundOrder(token, orderId) }
                                .onSuccess { txn = api.orderDetail(token, orderId) }
                                .onFailure { onStatus(it.message.orEmpty()) }
                        }
                    }) { Text(stringResource(Res.string.master_issue_refund)) }
                    OutlinedButton(onClick = { /* invoice stub */ }) { Text(stringResource(Res.string.master_download_invoice)) }
                    OutlinedButton(onClick = { /* print stub */ }) { Text(stringResource(Res.string.master_print_summary)) }
                    Button(onClick = {
                        scope.launch {
                            runCatching { api.cancelOrder(token, orderId) }
                                .onSuccess { txn = api.orderDetail(token, orderId) }
                                .onFailure { onStatus(it.message.orEmpty()) }
                        }
                    }) { Text(stringResource(Res.string.master_cancel_order)) }
                }
                OutlinedButton(onClick = { onNavigate(MasterDest.Transactions) }) { Text(stringResource(Res.string.action_back)) }
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
        MasterTopBar(stringResource(Res.string.master_users_title))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MasterStatCard(stringResource(Res.string.master_card_users), "${users.size}")
                DateFilterSummaryCard(
                    title = stringResource(Res.string.master_date_filter),
                    value = "${users.size}",
                    selected = filter,
                    onSelect = { filter = it },
                )
            }
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
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<UserDetailDto?>(null) }
    var selectedOrder by remember { mutableStateOf<TxnDto?>(null) }
    LaunchedEffect(userId) {
        runCatching { detail = api.userDetail(token, userId) }.onFailure { onStatus(it.message.orEmpty()) }
    }
    val u = detail?.user
    OrderDetailDialog(
        txn = selectedOrder,
        onDismiss = { selectedOrder = null },
        onRefund = { id ->
            scope.launch {
                runCatching { api.refundOrder(token, id) }.onSuccess {
                    detail = api.userDetail(token, userId)
                    selectedOrder = null
                }.onFailure { onStatus(it.message.orEmpty()) }
            }
        },
        onCancel = { id ->
            scope.launch {
                runCatching { api.cancelOrder(token, id) }.onSuccess {
                    detail = api.userDetail(token, userId)
                    selectedOrder = null
                }.onFailure { onStatus(it.message.orEmpty()) }
            }
        },
    )
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MasterTopBar("${stringResource(Res.string.master_user_details)}: #$userId")
        if (u != null) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ResponsiveColumns(
                    left = {
                        MasterSectionCard {
                            LabelValue(stringResource(Res.string.master_name), u.name)
                            LabelValue(stringResource(Res.string.master_contact), u.phone)
                            LabelValue(stringResource(Res.string.master_address), u.address)
                        }
                    },
                    right = {
                        MasterSectionCard(title = stringResource(Res.string.master_order_history)) {
                            detail?.orders.orEmpty().forEach { order ->
                                TableRow(onClick = { selectedOrder = order }) {
                                    CellText(order.orderId)
                                    CellText(formatEpochDate(order.createdAtEpochMs))
                                    CellText(formatInr(order.total))
                                    CellText(order.status)
                                }
                            }
                        }
                    },
                )
                OutlinedButton(onClick = { onNavigate(MasterDest.Users) }) { Text(stringResource(Res.string.action_back)) }
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
        MasterTopBar(stringResource(Res.string.master_partners_title))
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MasterStatCard(stringResource(Res.string.master_card_partners), "${partners.size}")
                DateFilterSummaryCard(
                    title = stringResource(Res.string.master_date_filter),
                    value = "${partners.size}",
                    selected = filter,
                    onSelect = { filter = it },
                )
            }
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
        MasterTopBar("${stringResource(Res.string.master_partner_details)}: #$partnerId")
        if (p != null) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ResponsiveColumns(
                    left = {
                        MasterSectionCard {
                            LabelValue(stringResource(Res.string.master_name), p.name)
                            LabelValue(stringResource(Res.string.master_contact), p.phone)
                            LabelValue(stringResource(Res.string.master_email), p.email)
                            LabelValue(stringResource(Res.string.master_address), p.address)
                            ImageThumb(p.profileImageUrl.ifBlank { p.idImageUrl }, Modifier.size(80.dp))
                            LabelValue(stringResource(Res.string.master_vehicle_number), p.vehicleNumber)
                            LabelValue(stringResource(Res.string.master_vehicle_name), p.vehicleName)
                            LabelValue(stringResource(Res.string.master_vehicle_brand), p.vehicleBrand)
                            LabelValue(stringResource(Res.string.master_vehicle_color), p.vehicleColor)
                            LabelValue(stringResource(Res.string.master_vehicle_type), p.vehicleType)
                            LabelValue(stringResource(Res.string.master_join_date), formatEpochDate(p.joinedAtEpochMs))
                            OnOffToggle(p.active) { /* read-only in detail */ }
                        }
                    },
                    right = {
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
                    },
                )
                OutlinedButton(onClick = { onNavigate(MasterDest.Partners) }) { Text(stringResource(Res.string.action_back)) }
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
        MasterTopBar(stringResource(Res.string.master_platform_title))
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
        MasterTopBar(stringResource(Res.string.master_settings_title))
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
