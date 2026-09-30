package org.bhargav.pansariwala.server.storage

/**
 * Converts between what Mongo stores (object keys) and what clients see (public URLs).
 * Inbound URLs from any known base (current backend or [legacyBaseUrls]) collapse to keys;
 * values that are not our assets (external https links, legacy base64) pass through unchanged.
 */
class AssetRefs(
    private val storage: AssetStorage,
    legacyBaseUrls: List<String>,
) {
    private val knownBases: List<String> = (storage.publicBaseUrls() + legacyBaseUrls)
        .map { it.trim().trimEnd('/') + "/" }
        .filter { it.length > 1 }
        .distinct()

    fun isKey(value: String): Boolean {
        val v = value.trim()
        return !v.contains("://") && ALLOWED_PREFIXES.any { v.startsWith(it) }
    }

    /** Accepts a key or an http(s) URL; rejects blanks and inline blobs. */
    fun isValidRef(value: String): Boolean {
        val v = value.trim()
        return isKey(v) || v.startsWith("http://", ignoreCase = true) || v.startsWith("https://", ignoreCase = true)
    }

    fun toKey(value: String): String {
        val v = value.trim()
        if (v.isBlank() || isKey(v)) return v
        val base = knownBases.firstOrNull { v.startsWith(it, ignoreCase = true) } ?: return v
        val key = v.substring(base.length).substringBefore('?').substringBefore('#')
        return if (isKey(key)) key else v
    }

    fun toKeyOrNull(value: String?): String? = value?.let(::toKey)?.ifBlank { null }

    fun toKeys(values: List<String>): List<String> = values.map(::toKey).filter { it.isNotBlank() }

    fun toUrl(value: String): String {
        val v = value.trim()
        return if (isKey(v)) storage.publicUrl(v) else v
    }

    fun toUrlOrNull(value: String?): String? = value?.let(::toUrl)?.ifBlank { null }

    fun toUrls(values: List<String>): List<String> = values.map(::toUrl)

    companion object {
        val ALLOWED_PREFIXES: Set<String> = setOf(
            "master/product-images/",
            "master/shop-images/",
            "master/shop-verification/",
            "users/user-image/",
            "users/vehicle-image/",
            "users/user-ids/",
            "partners/user-image/",
            "partners/vehicle-image/",
            "partners/user-ids/",
            "shops/delivery-packets/",
            "shops/shop/",
            "shops/product-images/",
        )
    }
}
