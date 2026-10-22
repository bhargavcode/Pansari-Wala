package org.bhargav.pansariwala.data.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    indices = [
        Index(value = ["shopId"]),
        Index(value = ["barcode"]),
        Index(value = ["category"]),
        Index(value = ["masterProductId"]),
    ],
)
data class ProductEntity(
    @PrimaryKey val id: String,
    val shopId: String,
    val name: String,
    val nameHi: String,
    val category: String,
    val unit: String,
    val barcode: String?,
    val sellingPrice: Double,
    val costPrice: Double,
    val stockQty: Double,
    val lowStockThreshold: Double,
    val voiceAlias: String?,
    /** Pipe-separated HTTPS image URLs (max 4). */
    val imageUrls: String = "",
    /** 1 while a local edit waits for server confirmation of [syncRevision]. */
    @ColumnInfo(defaultValue = "0")
    val pendingSync: Int = 0,
    @ColumnInfo(defaultValue = "0")
    val syncRevision: Long = 0,
    /** JSON array of `ProductVariantDto`; empty when the product has no variants. */
    @ColumnInfo(defaultValue = "")
    val variantsJson: String = "",
    @ColumnInfo(defaultValue = "")
    val brandName: String = "",
    val masterProductId: String? = null,
    @ColumnInfo(defaultValue = "1")
    val isActive: Boolean = true,
    val categoryId: String? = null,
    val subCategoryId: String? = null,
    /** JSON object language code → product name. */
    @ColumnInfo(defaultValue = "")
    val translationsJson: String = "",
)

/** Pull cursors and other small sync bookmarks. */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val syncKey: String,
    val syncValue: String,
)
