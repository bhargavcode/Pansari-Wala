package org.bhargav.pansariwala.feature.inventory

import kotlinx.serialization.Serializable
import org.bhargav.pansariwala.api.ProductVariantDto
import org.bhargav.pansariwala.domain.model.ProductCategory
import org.bhargav.pansariwala.domain.model.ProductUnit

/** Local autosave of the add/edit product form so a kill / back without save can resume later. */
@Serializable
data class ProductFormDraft(
    val shopId: String = "",
    /** Null = new product draft; set when editing an existing product. */
    val productId: String? = null,
    val name: String = "",
    val nameHi: String = "",
    val brandName: String = "",
    val category: String = ProductCategory.GENERAL_GROCERY.name,
    val categoryId: String? = null,
    val subCategoryId: String? = null,
    val unit: String = ProductUnit.PIECE.name,
    val barcode: String = "",
    val sellingPrice: String = "",
    val costPrice: String = "",
    val stockQty: String = "",
    val lowStockThreshold: String = "",
    val maxPerOrder: String = "",
    val voiceAlias: String = "",
    val hasVariants: Boolean = false,
    val variants: List<ProductVariantDto> = emptyList(),
    val mfgDate: Long? = null,
    val expiryDate: Long? = null,
    val translationLanguages: Set<String> = emptySet(),
    val nameTranslations: Map<String, String> = emptyMap(),
    val brandTranslations: Map<String, String> = emptyMap(),
    /** Uploaded / already-hosted product image URLs only (in-flight picks are not durable). */
    val imageUrls: List<String> = emptyList(),
) {
    fun matches(shopId: String, productId: String?): Boolean =
        this.shopId == shopId && this.productId == productId

    fun isBlank(): Boolean =
        name.isBlank() &&
            nameHi.isBlank() &&
            brandName.isBlank() &&
            barcode.isBlank() &&
            sellingPrice.isBlank() &&
            costPrice.isBlank() &&
            stockQty.isBlank() &&
            voiceAlias.isBlank() &&
            variants.isEmpty() &&
            imageUrls.isEmpty() &&
            nameTranslations.isEmpty() &&
            brandTranslations.isEmpty() &&
            mfgDate == null &&
            expiryDate == null
}
