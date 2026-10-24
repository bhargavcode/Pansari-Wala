package org.bhargav.pansariwala.feature.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import org.bhargav.pansariwala.api.MasterCategoryDto
import org.bhargav.pansariwala.api.PansariApi
import org.bhargav.pansariwala.api.ShopTypeOptionDto
import org.bhargav.pansariwala.api.rethrowIfStructuredCancellation
import org.bhargav.pansariwala.designsystem.DropdownOption
import org.bhargav.pansariwala.designsystem.SearchableDropdown
import org.bhargav.pansariwala.domain.model.CategoryStock
import org.bhargav.pansariwala.domain.model.Product
import org.bhargav.pansariwala.i18n.LocalAppLocale
import org.bhargav.pansariwala.i18n.labelRes
import org.bhargav.pansariwala.util.AppConstants
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pansariwala.shared.generated.resources.Res
import pansariwala.shared.generated.resources.field_category
import pansariwala.shared.generated.resources.field_sub_category
import pansariwala.shared.generated.resources.master_all
import pansariwala.shared.generated.resources.master_none
import pansariwala.shared.generated.resources.master_shop_type

/**
 * Master categories plus the shop types this shop sells as. Categories offered to the shop are the
 * roots pinned to one of [shopTypeIds] (or to no type at all).
 */
data class ProductTaxonomy(
    val categories: List<MasterCategoryDto> = emptyList(),
    /** This shop's own types (primary first), with display names. */
    val shopTypes: List<ShopTypeOptionDto> = emptyList(),
) {
    private val byId = categories.associateBy { it.id }
    val shopTypeIds: Set<String> = shopTypes.map { it.id }.toSet()

    fun name(id: String?): String? = id?.let { byId[it]?.name }

    /** Roots for [shopTypeId], or for all of this shop's types when null. */
    fun roots(shopTypeId: String?): List<MasterCategoryDto> {
        val wanted = shopTypeId?.let(::setOf) ?: shopTypeIds
        return categories
            .filter { it.parentId.isNullOrBlank() }
            .filter { wanted.isEmpty() || it.shopTypeIds.isEmpty() || it.shopTypeIds.any(wanted::contains) }
            .sortedBy { it.name.lowercase() }
    }

    fun children(rootId: String?): List<MasterCategoryDto> =
        if (rootId == null) emptyList()
        else categories.filter { it.parentId == rootId }.sortedBy { it.name.lowercase() }

    /** "Category › Sub-category", or null when the product has no master category. */
    fun label(categoryId: String?, subCategoryId: String?): String? {
        val root = name(categoryId) ?: return null
        return name(subCategoryId)?.let { "$root › $it" } ?: root
    }
}

/** Loads [ProductTaxonomy] once per app language and shares it across POS screens. */
class ProductTaxonomyStore(private val api: PansariApi) {
    private val _taxonomy = MutableStateFlow(ProductTaxonomy())
    val taxonomy: StateFlow<ProductTaxonomy> = _taxonomy.asStateFlow()
    private val mutex = Mutex()
    private var loadedFor: String? = null

    suspend fun load(languageCode: String?, force: Boolean = false) = mutex.withLock {
        val key = languageCode.orEmpty()
        if (!force && loadedFor == key && _taxonomy.value.categories.isNotEmpty()) return@withLock
        runCatching {
            coroutineScope {
                val categories = async { api.masterCategories() }
                val allTypes = async { runCatching { api.shopTypes() }.getOrDefault(emptyList()) }
                val mine = async {
                    runCatching {
                        withTimeoutOrNull(AppConstants.SHOP_TYPE_LOOKUP_TIMEOUT_MS) {
                            api.shopProfile().shop.let { listOf(it.shopType) + it.shopTypes }
                        }
                    }.getOrNull().orEmpty().filter { it.isNotBlank() }.distinct()
                }
                val names = allTypes.await().associateBy { it.id }
                ProductTaxonomy(
                    categories = categories.await(),
                    shopTypes = mine.await().map { id -> names[id] ?: ShopTypeOptionDto(id, id) },
                )
            }
        }.onSuccess {
            _taxonomy.value = it
            loadedFor = key
        }.onFailure { it.rethrowIfStructuredCancellation() }
    }
}

@Composable
fun rememberProductTaxonomy(store: ProductTaxonomyStore = koinInject()): ProductTaxonomy {
    val language = LocalAppLocale.current
    LaunchedEffect(language) { store.load(language) }
    val taxonomy by store.taxonomy.collectAsState()
    return taxonomy
}

/** Master category label for [product], falling back to its legacy shop bucket. */
@Composable
fun productCategoryLabel(product: Product): String =
    rememberProductTaxonomy().label(product.categoryId, product.subCategoryId)
        ?: stringResource(product.category.labelRes)

/** Master category name for a stock bucket, falling back to its legacy shop bucket. */
@Composable
fun categoryStockLabel(item: CategoryStock, taxonomy: ProductTaxonomy = rememberProductTaxonomy()): String =
    taxonomy.name(item.categoryId) ?: stringResource(item.category.labelRes)

/**
 * Shop type → category → sub-category dropdowns for a product form. The shop type list holds
 * "All" plus only this shop's types; it narrows the categories and is not saved.
 */
@Composable
fun ProductCategoryPicker(
    categoryId: String?,
    subCategoryId: String?,
    onChange: (categoryId: String?, subCategoryId: String?) -> Unit,
    modifier: Modifier = Modifier,
    taxonomy: ProductTaxonomy = rememberProductTaxonomy(),
) {
    if (taxonomy.categories.isEmpty()) return
    var shopTypeId by rememberSaveable { mutableStateOf<String?>(null) }
    val roots = remember(taxonomy, shopTypeId, categoryId) {
        val visible = taxonomy.roots(shopTypeId)
        val current = taxonomy.categories.firstOrNull { it.id == categoryId }
        if (current == null || current in visible) visible else listOf(current) + visible
    }
    val children = remember(taxonomy, categoryId) { taxonomy.children(categoryId) }
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.field_category), style = MaterialTheme.typography.labelLarge)
        if (taxonomy.shopTypes.isNotEmpty()) {
            SearchableDropdown(
                label = stringResource(Res.string.master_shop_type),
                options = taxonomy.shopTypes.map { DropdownOption(it.id, it.name) },
                selectedId = shopTypeId,
                onSelect = { picked ->
                    shopTypeId = picked
                    if (categoryId != null && taxonomy.roots(picked).none { it.id == categoryId }) onChange(null, null)
                },
                allLabel = stringResource(Res.string.master_all),
                leadingIcon = Icons.Default.Storefront,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        SearchableDropdown(
            label = stringResource(Res.string.field_category),
            options = roots.map { DropdownOption(it.id, it.name) },
            selectedId = categoryId,
            onSelect = { picked -> if (picked != categoryId) onChange(picked, null) },
            allLabel = stringResource(Res.string.master_none),
            leadingIcon = Icons.Default.Category,
            modifier = Modifier.fillMaxWidth(),
        )
        if (children.isNotEmpty()) {
            SearchableDropdown(
                label = stringResource(Res.string.field_sub_category),
                options = children.map { DropdownOption(it.id, it.name) },
                selectedId = subCategoryId,
                onSelect = { picked -> onChange(categoryId, picked) },
                allLabel = stringResource(Res.string.master_none),
                leadingIcon = Icons.Default.AccountTree,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
