package org.bhargav.pansariwala.master

import androidx.compose.material3.Switch
import org.bhargav.pansariwala.i18n.AppLanguage
import pansariwala.shared.generated.resources.master_active
import pansariwala.shared.generated.resources.master_inactive
import pansariwala.shared.generated.resources.master_translated_count
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.bhargav.pansariwala.designsystem.AppAlertDialog
import org.bhargav.pansariwala.designsystem.CatalogCascadeFilter
import org.bhargav.pansariwala.designsystem.DropdownOption
import org.bhargav.pansariwala.feature.catalog.ProductSheetImportDialog
import org.bhargav.pansariwala.feature.catalog.SheetImportUiState
import org.bhargav.pansariwala.i18n.UiText
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.action_edit
import pansariwala.shared.generated.resources.action_next
import pansariwala.shared.generated.resources.action_previous
import pansariwala.shared.generated.resources.master_actions
import pansariwala.shared.generated.resources.master_added_date
import pansariwala.shared.generated.resources.master_brand
import pansariwala.shared.generated.resources.master_cancel
import pansariwala.shared.generated.resources.master_category
import pansariwala.shared.generated.resources.master_category_all_shop_types
import pansariwala.shared.generated.resources.master_company
import pansariwala.shared.generated.resources.master_delete
import pansariwala.shared.generated.resources.master_delete_product_message
import pansariwala.shared.generated.resources.master_delete_product_title
import pansariwala.shared.generated.resources.master_description
import pansariwala.shared.generated.resources.master_image_column
import pansariwala.shared.generated.resources.master_new_product
import pansariwala.shared.generated.resources.master_no_products
import pansariwala.shared.generated.resources.master_page_info
import pansariwala.shared.generated.resources.master_product_name
import pansariwala.shared.generated.resources.master_products_subtitle
import pansariwala.shared.generated.resources.master_products_title
import pansariwala.shared.generated.resources.master_sale_price
import pansariwala.shared.generated.resources.master_search_products
import pansariwala.shared.generated.resources.master_status
import pansariwala.shared.generated.resources.master_translate_action
import pansariwala.shared.generated.resources.master_translations_action
import pansariwala.shared.generated.resources.master_variants_count
import pansariwala.shared.generated.resources.sheet_import_action

/** Shop types, categories and subcategories for the cascade pickers, loaded once per screen. */
internal class MasterCatalogTaxonomy(
    val shopTypes: List<ShopTypeDto> = emptyList(),
    val categories: List<CategoryDto> = emptyList(),
) {
    private val byId = categories.associateBy { it.id }

    fun shopTypeOptions() = shopTypes.filter { it.active }.map { DropdownOption(it.id, it.name) }

    fun categoryOptions(shopTypeId: String?) = categories
        .filter { it.parentId.isNullOrBlank() }
        .filter { shopTypeId == null || it.shopTypeIds.isEmpty() || shopTypeId in it.shopTypeIds }
        .sortedBy { it.name.lowercase() }
        .map { DropdownOption(it.id, it.name) }

    fun subcategoryOptions(categoryId: String?) =
        if (categoryId == null) emptyList()
        else categories.filter { it.parentId == categoryId }.sortedBy { it.name.lowercase() }.map { DropdownOption(it.id, it.name) }

    fun name(id: String?): String? = id?.let { byId[it]?.name }
}

internal suspend fun loadTaxonomy(api: MasterApi, token: String) =
    MasterCatalogTaxonomy(api.shopTypes(token), api.categories(token))

internal fun ProductDto.toUpsert(active: Boolean = this.active) = ProductUpsert(
    id = id,
    name = name,
    nameHi = nameHi,
    categoryId = categoryId,
    unit = unit,
    barcode = barcode,
    imageUrl = imageUrl,
    thumbnailUrl = thumbnailUrl,
    imageUrls = imageUrls,
    brandName = brandName,
    companyName = companyName,
    subcategoryId = subcategoryId,
    salePrice = salePrice,
    cost = cost,
    active = active,
    description = description,
    sku = sku,
    stockQty = stockQty,
    lowStockThreshold = lowStockThreshold,
    tags = tags,
    weightKg = weightKg,
    dimensions = dimensions,
    variants = variants,
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ProductsListScreen(token: String, onNavigate: (MasterDest) -> Unit, onStatus: (String) -> Unit) {
    val api = remember { MasterApi() }
    val scope = rememberCoroutineScope()
    var taxonomy by remember { mutableStateOf(MasterCatalogTaxonomy()) }
    var pageData by remember { mutableStateOf(ProductPageDto()) }
    var page by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var shopTypeId by rememberSaveable { mutableStateOf<String?>(null) }
    var categoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var subcategoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var sheet by remember { mutableStateOf<SheetImportUiState?>(null) }
    var sheetFile by remember { mutableStateOf<WebPickedFile?>(null) }
    var translating by remember { mutableStateOf<ProductDto?>(null) }
    var deleting by remember { mutableStateOf<ProductDto?>(null) }
    fun refresh() { reload++ }

    LaunchedEffect(token) {
        runCatching { taxonomy = loadTaxonomy(api, token) }.onFailure { onStatus(it.message.orEmpty()) }
    }
    LaunchedEffect(token, query, page, reload, shopTypeId, categoryId, subcategoryId) {
        if (query.isNotBlank()) delay(MASTER_SEARCH_DEBOUNCE_MS)
        runCatching {
            pageData = api.productPage(token, query.trim(), categoryId, page, MASTER_PAGE_SIZE, shopTypeId, subcategoryId)
        }.onFailure { onStatus(it.message.orEmpty()) }
    }

    translating?.let { p ->
        TranslationDialog(
            title = p.name,
            fields = listOf(
                TranslatableField(Res.string.master_product_name, p.name, p.translations),
                TranslatableField(Res.string.master_brand, p.brandName, p.brandTranslations),
                TranslatableField(Res.string.master_company, p.companyName, p.companyTranslations),
                TranslatableField(Res.string.master_description, p.description, p.descriptionTranslations),
            ),
            onSave = { lang, v ->
                api.saveProductTranslation(token, p.id, ProductFieldsTranslationBody(lang, v[0], v[1], v[2], v[3]))
                refresh()
            },
            onStatus = onStatus,
            onDismiss = { translating = null },
        )
    }
    deleting?.let { p ->
        AppAlertDialog(
            onDismissRequest = { deleting = null },
            icon = { Icon(Icons.Default.Delete, null, tint = MasterAction.DELETE.color()) },
            title = { Text(stringResource(Res.string.master_delete_product_title)) },
            text = { Text(stringResource(Res.string.master_delete_product_message, p.name)) },
            confirmButton = {
                Button(
                    onClick = {
                        deleting = null
                        scope.launch {
                            runCatching { api.deleteProduct(token, p.id) }
                                .onSuccess { refresh() }
                                .onFailure { onStatus(it.message.orEmpty()) }
                        }
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MasterAction.DELETE.color()),
                ) { Text(stringResource(Res.string.master_delete)) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text(stringResource(Res.string.master_cancel)) } },
        )
    }
    sheet?.let { current ->
        val uriHandler = LocalUriHandler.current
        fun busy(block: suspend () -> Unit) {
            scope.launch {
                sheet = sheet?.copy(busy = true, error = null)
                runCatching { block() }
                    .onSuccess { sheet = sheet?.copy(busy = false) }
                    .onFailure { e -> sheet = sheet?.copy(busy = false, error = UiText.Plain(e.message.orEmpty())) }
            }
        }
        fun upload(dryRun: Boolean) {
            val file = sheetFile ?: return
            sheet = sheet?.copy(fileName = file.name)
            busy {
                val report = api.importProducts(token, file.name, file.bytes, dryRun)
                sheet = sheet?.copy(report = report)
                if (!report.dryRun) refresh()
            }
        }
        ProductSheetImportDialog(
            state = current,
            onDownloadSample = { uriHandler.openUri(api.productTemplateUrl) },
            onExport = { busy { uriHandler.openUri(api.productsExportUrl(token)) } },
            onPickFile = {
                scope.launch {
                    sheetFile = pickWebFile(AppConstants.ProductSheet.PICKER_EXTENSIONS, AppConstants.ProductSheet.MIME_XLSX)
                        ?: return@launch
                    upload(dryRun = true)
                }
            },
            onApply = { upload(dryRun = false) },
            onDismiss = {
                sheet = null
                sheetFile = null
            },
        )
    }

    val pad = AppConstants.MasterWeb.CONTENT_PADDING_DP.dp
    val pageCount = ((pageData.totalCount + MASTER_PAGE_SIZE - 1) / MASTER_PAGE_SIZE).coerceAtLeast(1)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        MasterTopBar(
            stringResource(Res.string.master_products_title),
            subtitle = stringResource(Res.string.master_products_subtitle, pageData.totalCount),
        )
        Column(Modifier.padding(pad), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(onClick = { onNavigate(MasterDest.ProductEdit(null)) }, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.master_new_product))
                }
                OutlinedButton(onClick = { sheet = SheetImportUiState() }, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.FileUpload, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.sheet_import_action))
                }
                OutlinedButton(onClick = { onNavigate(MasterDest.ProductTranslations()) }, shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Default.Translate, null, Modifier.size(18.dp), tint = MasterAction.TRANSLATE.color())
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(Res.string.master_translations_action))
                }
            }
            MasterSectionCard {
                OutlinedTextField(
                    query,
                    {
                        query = it
                        page = 0
                    },
                    placeholder = { Text(stringResource(Res.string.master_search_products)) },
                    leadingIcon = { Icon(Icons.Default.Search, null) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                )
                CatalogCascadeFilter(
                    shopTypes = taxonomy.shopTypeOptions(),
                    shopTypeId = shopTypeId,
                    onShopType = {
                        shopTypeId = it
                        categoryId = null
                        subcategoryId = null
                        page = 0
                    },
                    categories = taxonomy.categoryOptions(shopTypeId),
                    categoryId = categoryId,
                    onCategory = {
                        categoryId = it
                        subcategoryId = null
                        page = 0
                    },
                    subcategories = taxonomy.subcategoryOptions(categoryId),
                    subcategoryId = subcategoryId,
                    onSubcategory = {
                        subcategoryId = it
                        page = 0
                    },
                )
            }
            MasterSectionCard {
                ResponsiveTable(columns = 8) {
                    TableHeader(
                        stringResource(Res.string.master_image_column),
                        stringResource(Res.string.master_product_name),
                        stringResource(Res.string.master_category),
                        stringResource(Res.string.master_variants_count),
                        stringResource(Res.string.master_sale_price),
                        stringResource(Res.string.master_status),
                        stringResource(Res.string.master_added_date),
                        stringResource(Res.string.master_actions),
                    )
                    if (pageData.items.isEmpty()) {
                        EmptyProducts()
                    }
                    pageData.items.forEach { p ->
                        ProductRow(
                            p = p,
                            categoryLabel = listOfNotNull(
                                taxonomy.name(p.categoryId) ?: p.categoryId,
                                taxonomy.name(p.subcategoryId),
                            ).joinToString(" › "),
                            onOpen = { onNavigate(MasterDest.ProductEdit(p.id)) },
                            onToggle = {
                                scope.launch {
                                    runCatching { api.saveProduct(token, p.toUpsert(active = !p.active)) }
                                        .onSuccess { refresh() }
                                        .onFailure { onStatus(it.message.orEmpty()) }
                                }
                            },
                            onTranslate = { translating = p },
                            onDelete = { deleting = p },
                        )
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    stringResource(Res.string.master_page_info, page + 1, pageCount, pageData.totalCount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp),
                )
                IconButton(onClick = { page-- }, enabled = page > 0) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(Res.string.action_previous))
                }
                IconButton(onClick = { page++ }, enabled = pageData.hasMore) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(Res.string.action_next))
                }
            }
        }
    }
}

@Composable
private fun ProductRow(
    p: ProductDto,
    categoryLabel: String,
    onOpen: () -> Unit,
    onToggle: () -> Unit,
    onTranslate: () -> Unit,
    onDelete: () -> Unit,
) {
    TableRow(onClick = onOpen) {
        Cell {
            ImageThumb(
                p.imageUrls.firstOrNull() ?: p.imageUrl ?: p.variants.firstNotNullOfOrNull { it.imageUrls.firstOrNull() },
                Modifier.size(52.dp).clip(RoundedCornerShape(10.dp)),
            )
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(p.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val meta = listOf(p.brandName, p.companyName).filter { it.isNotBlank() }.joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(meta, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(p.id, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline, maxLines = 1)
        }
        Cell {
            Text(
                categoryLabel,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
        CellText(if (p.variants.isEmpty()) "—" else "${p.variants.size}")
        Column(Modifier.weight(1f)) {
            Text(formatInr(p.salePrice), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(formatInr(p.cost), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Cell { OnOffToggle(p.active, onToggle) }
        CellText(formatEpochDate(p.addedAtEpochMs))
        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MasterActionIcon(MasterAction.EDIT, Icons.Default.Edit, stringResource(Res.string.action_edit), onOpen)
            MasterActionIcon(MasterAction.TRANSLATE, Icons.Default.Translate, stringResource(Res.string.master_translate_action), onTranslate)
            MasterActionIcon(MasterAction.DELETE, Icons.Default.Delete, stringResource(Res.string.master_delete), onDelete)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CategoryManageRow(
    category: CategoryDto,
    shopTypeLabels: List<String>,
    indent: Boolean,
    onEdit: () -> Unit,
    onTranslate: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = if (indent) 32.dp else 0.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (indent) MaterialTheme.colorScheme.surfaceContainerLow else MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    category.name,
                    style = if (indent) MaterialTheme.typography.bodyMedium else MaterialTheme.typography.titleSmall,
                    fontWeight = if (indent) FontWeight.Medium else FontWeight.SemiBold,
                )
                Text(category.id, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            if (!indent) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (shopTypeLabels.isEmpty()) {
                        TagPill(stringResource(Res.string.master_category_all_shop_types), MaterialTheme.colorScheme.outline)
                    }
                    shopTypeLabels.forEach { TagPill(it, MaterialTheme.colorScheme.primary) }
                }
            }
        }
        MasterActionIcon(MasterAction.EDIT, Icons.Default.Edit, stringResource(Res.string.action_edit), onEdit)
        MasterActionIcon(MasterAction.TRANSLATE, Icons.Default.Translate, stringResource(Res.string.master_translate_action), onTranslate)
        MasterActionIcon(MasterAction.DELETE, Icons.Default.Delete, stringResource(Res.string.master_delete), onDelete)
    }
}

@Composable
internal fun ShopTypeManageRow(
    shopType: ShopTypeDto,
    onToggleActive: (Boolean) -> Unit,
    onTranslate: () -> Unit,
    onDelete: () -> Unit,
) {
    val languages = AppLanguage.entries.filter { it != AppLanguage.ENGLISH }
    val translated = languages.filter { !shopType.translations[it.code].isNullOrBlank() }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(shopType.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                Text(shopType.id, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TagPill(
                    stringResource(Res.string.master_translated_count, translated.size, languages.size),
                    if (translated.size == languages.size) MasterAction.ACTIVATE.color() else MasterAction.TRANSLATE.color(),
                )
                translated.forEach { lang ->
                    TagPill(shopType.translations[lang.code].orEmpty(), MaterialTheme.colorScheme.outline)
                }
            }
        }
        Text(
            stringResource(if (shopType.active) Res.string.master_active else Res.string.master_inactive),
            style = MaterialTheme.typography.labelMedium,
            color = if (shopType.active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        Switch(checked = shopType.active, onCheckedChange = onToggleActive)
        MasterActionIcon(MasterAction.TRANSLATE, Icons.Default.Translate, stringResource(Res.string.master_translate_action), onTranslate)
        MasterActionIcon(MasterAction.DELETE, Icons.Default.Delete, stringResource(Res.string.master_delete), onDelete)
    }
}

@Composable
private fun TagPill(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun EmptyProducts() {
    Box(Modifier.fillMaxWidth().padding(vertical = 48.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Default.Inventory2, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.outline)
            Text(stringResource(Res.string.master_no_products), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
