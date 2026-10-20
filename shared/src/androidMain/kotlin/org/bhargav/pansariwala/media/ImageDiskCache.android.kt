package org.bhargav.pansariwala.media

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.bhargav.pansariwala.util.AppConstants
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File

private object ImageCacheDir : KoinComponent {
    private val context: Context by inject()
    val dir: File by lazy { File(context.applicationContext.cacheDir, AppConstants.IMAGE_DISK_CACHE_DIR).apply { mkdirs() } }
}

internal actual suspend fun readImageDiskCache(key: String): ByteArray? = withContext(Dispatchers.IO) {
    runCatching {
        val file = File(ImageCacheDir.dir, key)
        if (!file.isFile) return@runCatching null
        file.setLastModified(System.currentTimeMillis())
        file.readBytes()
    }.getOrNull()
}

internal actual suspend fun writeImageDiskCache(key: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
    runCatching {
        val dir = ImageCacheDir.dir
        val tmp = File(dir, "$key.tmp")
        tmp.writeBytes(bytes)
        tmp.renameTo(File(dir, key))
        trim(dir)
    }
    Unit
}

/** Drops least-recently-used files once the directory exceeds the configured budget. */
private fun trim(dir: File) {
    val files = dir.listFiles()?.filter { it.isFile } ?: return
    var total = files.sumOf { it.length() }
    if (total <= AppConstants.IMAGE_DISK_CACHE_BYTES) return
    files.sortedBy { it.lastModified() }.forEach { file ->
        if (total <= AppConstants.IMAGE_DISK_CACHE_BYTES * 3 / 4) return
        total -= file.length()
        file.delete()
    }
}

internal actual val imageFetchViaProxy: Boolean = false
