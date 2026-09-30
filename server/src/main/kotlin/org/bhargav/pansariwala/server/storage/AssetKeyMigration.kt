package org.bhargav.pansariwala.server.storage

import com.mongodb.client.model.Filters.eq
import com.mongodb.client.model.Filters.or
import com.mongodb.client.model.Filters.regex
import com.mongodb.client.model.Projections.include
import com.mongodb.client.model.Updates.combine
import com.mongodb.client.model.Updates.set
import com.mongodb.kotlin.client.MongoDatabase
import org.bson.Document
import org.bson.conversions.Bson

/** Idempotent: rewrites stored asset URLs from any known base into plain object keys. */
class AssetKeyMigration(
    private val db: MongoDatabase,
    private val refs: AssetRefs,
) {
    fun run(): Int = TARGETS.entries.sumOf { (collection, fields) -> migrate(collection, fields) }

    private fun migrate(collection: String, fields: List<String>): Int {
        val col = db.getCollection<Document>(collection)
        val filter = or(fields.map { regex(it, "^https?://") })
        var changed = 0
        col.find(filter).projection(include(fields)).forEach { doc ->
            val updates = fields.mapNotNull { field -> rewrite(field, doc[field]) }
            if (updates.isNotEmpty()) {
                col.updateOne(eq("_id", doc["_id"]), combine(updates))
                changed++
            }
        }
        return changed
    }

    private fun rewrite(field: String, value: Any?): Bson? = when (value) {
        is String -> refs.toKey(value).takeIf { it != value }?.let { set(field, it) }
        is List<*> -> {
            val next = value.map { item -> (item as? String)?.let(refs::toKey) ?: item }
            if (next != value) set(field, next) else null
        }
        else -> null
    }

    private companion object {
        val TARGETS: Map<String, List<String>> = mapOf(
            "shops" to listOf("imageUrl"),
            "customers" to listOf("imageUrl"),
            "partners" to listOf("platePhoto", "vehiclePhoto", "profilePhoto", "dlPhoto", "idPhoto"),
            "master_products" to listOf("imageUrl", "thumbnailUrl", "imageUrls"),
            "products" to listOf("imageUrls"),
            "orders" to listOf("pickupPhotos"),
        )
    }
}
