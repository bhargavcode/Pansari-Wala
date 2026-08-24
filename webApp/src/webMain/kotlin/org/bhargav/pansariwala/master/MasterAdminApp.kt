package org.bhargav.pansariwala.master

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.navigateToLanding
import org.bhargav.pansariwala.theme.PansariTheme
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.field_category
import pansariwala.shared.generated.resources.field_name
import pansariwala.shared.generated.resources.field_name_hi
import pansariwala.shared.generated.resources.field_unit
import pansariwala.shared.generated.resources.login_password
import pansariwala.shared.generated.resources.login_sign_in
import pansariwala.shared.generated.resources.login_username
import pansariwala.shared.generated.resources.master_active
import pansariwala.shared.generated.resources.master_admin_title
import pansariwala.shared.generated.resources.master_back_to_site
import pansariwala.shared.generated.resources.master_console_title
import pansariwala.shared.generated.resources.master_delete
import pansariwala.shared.generated.resources.master_image_url
import pansariwala.shared.generated.resources.master_inactive
import pansariwala.shared.generated.resources.master_address
import pansariwala.shared.generated.resources.master_new_category
import pansariwala.shared.generated.resources.master_new_product
import pansariwala.shared.generated.resources.master_new_shop
import pansariwala.shared.generated.resources.master_new_shop_type
import pansariwala.shared.generated.resources.master_parent_category
import pansariwala.shared.generated.resources.master_refresh
import pansariwala.shared.generated.resources.master_save
import pansariwala.shared.generated.resources.master_tab_categories
import pansariwala.shared.generated.resources.master_tab_products
import pansariwala.shared.generated.resources.master_tab_shop_types
import pansariwala.shared.generated.resources.master_tab_shops

@Composable
fun MasterAdminApp() {
    var token by rememberSaveable { mutableStateOf<String?>(null) }
    PansariTheme {
        if (token == null) {
            MasterLoginScreen(onLoggedIn = { token = it })
        } else {
            MasterDashboard(token = token!!, onLogout = { token = null })
        }
    }
}

@Composable
private fun MasterLoginScreen(onLoggedIn: (String) -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(Res.string.master_admin_title), style = MaterialTheme.typography.headlineMedium)
        OutlinedTextField(username, { username = it }, label = { Text(stringResource(Res.string.login_username)) })
        OutlinedTextField(password, { password = it }, label = { Text(stringResource(Res.string.login_password)) })
        if (error != null) Text(error!!, color = MaterialTheme.colorScheme.error)
        Button(
            enabled = !busy,
            onClick = {
                busy = true
                error = null
                scope.launch {
                    runCatching { api.login(username.trim(), password) }
                        .onSuccess { onLoggedIn(it.accessToken) }
                        .onFailure { error = it.message ?: "error" }
                    busy = false
                }
            },
            modifier = Modifier.fillMaxWidth(0.5f),
        ) { Text(stringResource(Res.string.login_sign_in)) }
        Button(onClick = ::navigateToLanding) { Text(stringResource(Res.string.master_back_to_site)) }
    }
}

@Composable
private fun MasterDashboard(token: String, onLogout: () -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var status by remember { mutableStateOf("") }
    var products by remember { mutableStateOf<List<ProductDto>>(emptyList()) }
    var categories by remember { mutableStateOf<List<CategoryDto>>(emptyList()) }
    var shopTypes by remember { mutableStateOf<List<ShopTypeDto>>(emptyList()) }
    var shops by remember { mutableStateOf<List<ShopDto>>(emptyList()) }

    fun refresh() {
        scope.launch {
            runCatching {
                products = api.products(token)
                categories = api.categories(token)
                shopTypes = api.shopTypes(token)
                shops = api.shops(token)
                status = ""
            }.onFailure { status = it.message.orEmpty() }
        }
    }

    LaunchedEffect(token) { refresh() }

    val tabs = listOf(
        stringResource(Res.string.master_tab_products),
        stringResource(Res.string.master_tab_categories),
        stringResource(Res.string.master_tab_shop_types),
        stringResource(Res.string.master_tab_shops),
    )

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(Res.string.master_console_title), style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { refresh() }) { Text(stringResource(Res.string.master_refresh)) }
                OutlinedButton(onClick = onLogout) { Text(stringResource(Res.string.master_back_to_site)) }
            }
        }
        if (status.isNotBlank()) Text(status, color = MaterialTheme.colorScheme.error)
        ScrollableTabRow(selectedTabIndex = tab) {
            tabs.forEachIndexed { i, title ->
                Tab(selected = tab == i, onClick = { tab = i }, text = { Text(title) })
            }
        }
        when (tab) {
            0 -> ProductsTab(api, token, products, categories) { refresh(); status = it }
            1 -> CategoriesTab(api, token, categories) { refresh(); status = it }
            2 -> ShopTypesTab(api, token, shopTypes) { refresh(); status = it }
            else -> ShopsTab(api, token, shops, shopTypes) { refresh(); status = it }
        }
    }
}

@Composable
private fun ProductsTab(
    api: MasterApi,
    token: String,
    products: List<ProductDto>,
    categories: List<CategoryDto>,
    onDone: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var nameHi by rememberSaveable { mutableStateOf("") }
    var categoryId by rememberSaveable { mutableStateOf(categories.firstOrNull()?.id.orEmpty()) }
    var unit by rememberSaveable { mutableStateOf("KG") }
    var imageUrl by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.master_new_product), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(Res.string.field_name)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(nameHi, { nameHi = it }, label = { Text(stringResource(Res.string.field_name_hi)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(categoryId, { categoryId = it }, label = { Text(stringResource(Res.string.field_category)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(unit, { unit = it }, label = { Text(stringResource(Res.string.field_unit)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(imageUrl, { imageUrl = it }, label = { Text(stringResource(Res.string.master_image_url)) }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            scope.launch {
                runCatching {
                    api.saveProduct(
                        token,
                        ProductUpsert(
                            name = name,
                            nameHi = nameHi,
                            categoryId = categoryId,
                            unit = unit,
                            imageUrl = imageUrl.ifBlank { null },
                            thumbnailUrl = imageUrl.ifBlank { null },
                        ),
                    )
                }.onSuccess {
                    name = ""; nameHi = ""; imageUrl = ""
                    onDone("")
                }.onFailure { onDone(it.message.orEmpty()) }
            }
        }) { Text(stringResource(Res.string.master_save)) }
        products.forEach { p ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${p.name} (${p.categoryId}) ${p.imageUrl?.take(40).orEmpty()}")
                OutlinedButton(onClick = {
                    scope.launch {
                        runCatching { api.deleteProduct(token, p.id) }
                            .onSuccess { onDone("") }
                            .onFailure { onDone(it.message.orEmpty()) }
                    }
                }) { Text(stringResource(Res.string.master_delete)) }
            }
        }
    }
}

@Composable
private fun CategoriesTab(api: MasterApi, token: String, categories: List<CategoryDto>, onDone: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var parentId by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.master_new_category), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(Res.string.field_name)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(parentId, { parentId = it }, label = { Text(stringResource(Res.string.master_parent_category)) }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            scope.launch {
                runCatching { api.saveCategory(token, CategoryUpsert(name = name, parentId = parentId.ifBlank { null })) }
                    .onSuccess { name = ""; parentId = ""; onDone("") }
                    .onFailure { onDone(it.message.orEmpty()) }
            }
        }) { Text(stringResource(Res.string.master_save)) }
        categories.forEach { c ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${c.name} [${c.id}] parent=${c.parentId.orEmpty()}")
                OutlinedButton(onClick = {
                    scope.launch {
                        runCatching { api.deleteCategory(token, c.id) }
                            .onSuccess { onDone("") }
                            .onFailure { onDone(it.message.orEmpty()) }
                    }
                }) { Text(stringResource(Res.string.master_delete)) }
            }
        }
    }
}

@Composable
private fun ShopTypesTab(api: MasterApi, token: String, types: List<ShopTypeDto>, onDone: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.master_new_shop_type), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(Res.string.field_name)) }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            scope.launch {
                runCatching { api.saveShopType(token, ShopTypeUpsert(name = name)) }
                    .onSuccess { name = ""; onDone("") }
                    .onFailure { onDone(it.message.orEmpty()) }
            }
        }) { Text(stringResource(Res.string.master_save)) }
        types.forEach { t ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("${t.name} [${t.id}] ${if (t.active) "on" else "off"}")
                OutlinedButton(onClick = {
                    scope.launch {
                        runCatching { api.deleteShopType(token, t.id) }
                            .onSuccess { onDone("") }
                            .onFailure { onDone(it.message.orEmpty()) }
                    }
                }) { Text(stringResource(Res.string.master_delete)) }
            }
        }
    }
}

@Composable
private fun ShopsTab(
    api: MasterApi,
    token: String,
    shops: List<ShopDto>,
    types: List<ShopTypeDto>,
    onDone: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var shopType by rememberSaveable { mutableStateOf(types.firstOrNull()?.id ?: "GENERAL_STORE") }
    var address by rememberSaveable { mutableStateOf("") }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.master_new_shop), style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(name, { name = it }, label = { Text(stringResource(Res.string.field_name)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(shopType, { shopType = it }, label = { Text(stringResource(Res.string.master_tab_shop_types)) }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(address, { address = it }, label = { Text(stringResource(Res.string.master_address)) }, modifier = Modifier.fillMaxWidth())
        Button(onClick = {
            scope.launch {
                runCatching { api.createShop(token, ShopCreate(name = name, shopType = shopType, address = address)) }
                    .onSuccess { name = ""; address = ""; onDone("") }
                    .onFailure { onDone(it.message.orEmpty()) }
            }
        }) { Text(stringResource(Res.string.master_save)) }
        shops.forEach { s ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${s.name} · ${s.shopType} · ${if (s.active) stringResource(Res.string.master_active) else stringResource(Res.string.master_inactive)}")
                OutlinedButton(onClick = {
                    scope.launch {
                        runCatching { api.patchShop(token, s.id, active = !s.active, paymentsEnabled = null) }
                            .onSuccess { onDone("") }
                            .onFailure { onDone(it.message.orEmpty()) }
                    }
                }) {
                    Text(if (s.active) stringResource(Res.string.master_inactive) else stringResource(Res.string.master_active))
                }
            }
        }
    }
}
