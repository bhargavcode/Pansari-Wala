package org.bhargav.pansariwala.server.storage

import org.bhargav.pansariwala.server.ServerConfig

/**
 * Object storage backend. Mongo stores only object keys (e.g. `partners/user-image/<id>.jpg`);
 * the active backend turns a key into a public URL at response time, so switching backends
 * needs no data migration beyond copying the objects.
 */
interface AssetStorage {
    val provider: StorageProvider
    val description: String

    fun put(key: String, bytes: ByteArray, contentType: String)

    fun publicUrl(key: String): String

    /** URL prefixes this backend has served objects under; used to turn inbound URLs back into keys. */
    fun publicBaseUrls(): List<String>
}

enum class StorageProvider(val id: String) {
    S3("s3"),
    ;

    companion object {
        fun from(value: String): StorageProvider =
            entries.firstOrNull { it.id.equals(value.trim(), ignoreCase = true) }
                ?: error("Unsupported STORAGE_PROVIDER=$value (supported: ${entries.joinToString { it.id }})")
    }
}

object AssetStorageFactory {
    fun create(config: ServerConfig): AssetStorage = when (StorageProvider.from(config.storageProvider)) {
        StorageProvider.S3 -> S3AssetStorage(config)
    }
}
