package org.bhargav.pansariwala.server.db

import com.mongodb.client.model.Filters.and
import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.ReplaceOptions
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.MongoDatabase
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MigrationDoc(@SerialName("_id") val id: String, val appliedAt: Long)

private const val CATEGORY_ALIGNMENT_ID = "category_alignment_v1"

private val GROCERY_TYPES = listOf("GROCERY", "SUPERMARKET", "GENERAL_STORE")

/**
 * Shop types per master root category. A shop sees a root when one of its types is listed;
 * roots missing here keep the shop types an admin gave them.
 */
internal val ROOT_CATEGORY_SHOP_TYPES: Map<String, List<String>> = mapOf(
    "mc_atta_rice_dal" to GROCERY_TYPES + "ORGANIC",
    "mc_oil_ghee_masala" to GROCERY_TYPES + "ORGANIC",
    "mc_dairy_bread_eggs" to GROCERY_TYPES + listOf("DAIRY", "BAKERY"),
    "mc_breakfast" to GROCERY_TYPES + listOf("ORGANIC", "BAKERY"),
    "mc_snacks" to GROCERY_TYPES + listOf("SWEET_SHOP", "BAKERY", "PAAN_SHOP"),
    "mc_biscuits" to GROCERY_TYPES + listOf("BAKERY", "PAAN_SHOP"),
    "mc_sweets" to GROCERY_TYPES + listOf("SWEET_SHOP", "BAKERY", "DAIRY", "PAAN_SHOP", "TOYS_GIFTS"),
    "mc_instant" to GROCERY_TYPES,
    "mc_sauces" to GROCERY_TYPES + "ORGANIC",
    "mc_dry_fruits" to GROCERY_TYPES + listOf("ORGANIC", "SWEET_SHOP", "FRUITS_VEGETABLES"),
    "mc_tea_coffee" to GROCERY_TYPES + "ORGANIC",
    "mc_cold_drinks" to GROCERY_TYPES + listOf("DAIRY", "PAAN_SHOP", "GAS_WATER", "RESTAURANT"),
    "mc_frozen_meat" to GROCERY_TYPES + "MEAT_FISH",
    "mc_baby" to GROCERY_TYPES + listOf("MEDICAL_STORE", "COSMETICS"),
    "mc_personal_care" to GROCERY_TYPES + listOf("COSMETICS", "MEDICAL_STORE", "AYURVEDIC"),
    "mc_home" to GROCERY_TYPES + listOf("HOME_KITCHEN", "FLOWERS"),
    "mc_auto" to listOf("AUTO_PARTS"),
    "mc_electrical" to listOf("ELECTRICAL", "HARDWARE", "ELECTRONICS"),
    "mc_fruits_veg" to listOf("FRUITS_VEGETABLES", "SUPERMARKET", "ORGANIC"),
    "mc_hardware" to listOf("HARDWARE", "PLUMBING", "ELECTRICAL"),
    "mc_home_kitchen" to listOf("HOME_KITCHEN", "SUPERMARKET", "FURNITURE"),
    "mc_medical" to listOf("MEDICAL_STORE", "AYURVEDIC"),
    "mc_mobile" to listOf("MOBILE", "ELECTRONICS"),
    "mc_pets" to listOf("PET_SUPPLIES", "SUPERMARKET"),
    "mc_plumbing" to listOf("PLUMBING", "HARDWARE"),
    "mc_stationery" to listOf("STATIONERY", "BOOKS", "TOYS_GIFTS", "SUPERMARKET", "GENERAL_STORE"),
)

/** Name keyword → (root, sub) for shop products that have no master product to copy from. */
private val KEYWORD_CATEGORIES: List<Pair<Regex, Pair<String, String>>> = listOf(
    Regex("\\b(atta|wheat flour|maida|besan)\\b") to ("mc_atta_rice_dal" to "mc_atta_rice_dal__atta_flours"),
    Regex("\\b(rice|basmati)\\b") to ("mc_atta_rice_dal" to "mc_atta_rice_dal__rice"),
    Regex("\\b(dal|daal|toor|moong|chana|rajma)\\b") to ("mc_atta_rice_dal" to "mc_atta_rice_dal__dals"),
    Regex("\\b(poha|suji|sooji|rawa|daliya)\\b") to ("mc_atta_rice_dal" to "mc_atta_rice_dal__poha_suji"),
    Regex("\\b(oil)\\b") to ("mc_oil_ghee_masala" to "mc_oil_ghee_masala__oils"),
    Regex("\\b(ghee|vanaspati)\\b") to ("mc_oil_ghee_masala" to "mc_oil_ghee_masala__ghee"),
    Regex("\\b(salt|namak|sugar|cheeni|chini|jaggery|gud)\\b") to ("mc_oil_ghee_masala" to "mc_oil_ghee_masala__salt_sugar"),
    Regex("\\b(turmeric|haldi|masala|chilli powder|mirch)\\b") to ("mc_oil_ghee_masala" to "mc_oil_ghee_masala__masala"),
    Regex("\\b(papad|fryums|namkeen|bhujia)\\b") to ("mc_snacks" to "mc_snacks__namkeen"),
    Regex("\\b(cement|rope|wd-?40)\\b") to ("mc_hardware" to "mc_hardware__misc"),
    Regex("\\b(saw|blade|hammer|plier|spanner|screwdriver)\\b") to ("mc_hardware" to "mc_hardware__hand_tools"),
)

/**
 * One-time fix-up: pins master root categories to the right shop types, makes every master-linked
 * shop product use its master's category/sub-category, and fills unlinked products whose category
 * is missing or unknown (master match by barcode/name, then [KEYWORD_CATEGORIES]).
 */
internal fun alignCategories(db: MongoDatabase) {
    val migrations = db.getCollection<MigrationDoc>("app_migrations")
    if (migrations.find(eq("_id", CATEGORY_ALIGNMENT_ID)).firstOrNull() != null) return

    val categoryCol = db.getCollection<CategoryDoc>("master_categories")
    val categories = categoryCol.find().toList().associateBy { it.id }
    ROOT_CATEGORY_SHOP_TYPES.forEach { (rootId, types) ->
        val root = categories[rootId] ?: return@forEach
        if (root.parentId == null && root.shopTypeIds.toSet() != types.toSet()) {
            categoryCol.updateOne(eq("_id", rootId), set("shopTypeIds", types.distinct()))
        }
    }

    val masters = db.getCollection<MasterProductDoc>("master_products").find().toList()
    val masterById = masters.associateBy { it.id }
    val masterByBarcode = masters.filter { !it.barcode.isNullOrBlank() }.associateBy { it.barcode!! }
    val masterByName = masters.associateBy { it.name.trim().lowercase() }
    val products = db.getCollection<ProductDoc>("products")
    var changed = 0
    products.find().toList().forEach { p ->
        val linked = p.masterProductId?.let(masterById::get)
        val target: Pair<String, String?>? = when {
            linked != null -> linked.categoryId to linked.subcategoryId
            p.categoryId != null && categories[p.categoryId]?.parentId == null && p.categoryId in categories -> null
            else -> {
                val match = p.barcode?.takeIf { it.isNotBlank() }?.let(masterByBarcode::get)
                    ?: masterByName[p.name.trim().lowercase()]
                match?.let { it.categoryId to it.subcategoryId }
                    ?: KEYWORD_CATEGORIES.firstOrNull { it.first.containsMatchIn(p.name.lowercase()) }?.second
            }
        }
        if (target != null && (p.categoryId != target.first || p.subCategoryId != target.second)) {
            products.updateOne(eq("_id", p.id), combine(set("categoryId", target.first), set("subCategoryId", target.second)))
            changed++
        }
    }
    HARDWARE_FIXES.forEach { (name, sub) ->
        products.updateMany(
            and(eq("name", name), eq("categoryId", "mc_home")),
            combine(set("categoryId", "mc_hardware"), set("subCategoryId", sub)),
        ).modifiedCount.let { changed += it.toInt() }
    }
    migrations.replaceOne(
        eq("_id", CATEGORY_ALIGNMENT_ID),
        MigrationDoc(CATEGORY_ALIGNMENT_ID, System.currentTimeMillis()),
        ReplaceOptions().upsert(true),
    )
    println("INFO: category alignment updated $changed shop products")
}

/** Hardware items saved under Home while the POS picker only offered grocery categories. */
private val HARDWARE_FIXES = listOf(
    "ACC Cement" to "mc_hardware__misc",
    "hack saw blade" to "mc_hardware__hand_tools",
)
