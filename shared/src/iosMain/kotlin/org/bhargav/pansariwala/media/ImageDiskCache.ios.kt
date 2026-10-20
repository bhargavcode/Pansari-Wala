package org.bhargav.pansariwala.media

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.bhargav.pansariwala.util.AppConstants
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy

/** Caches/ is purged by iOS under storage pressure, so no manual size trimming is needed. */
@OptIn(ExperimentalForeignApi::class)
private val cacheDir: String by lazy {
    val base = NSSearchPathForDirectoriesInDomains(NSCachesDirectory, NSUserDomainMask, true).first() as String
    val dir = "$base/${AppConstants.IMAGE_DISK_CACHE_DIR}"
    NSFileManager.defaultManager.createDirectoryAtPath(dir, withIntermediateDirectories = true, attributes = null, error = null)
    dir
}

@OptIn(ExperimentalForeignApi::class)
internal actual suspend fun readImageDiskCache(key: String): ByteArray? = withContext(Dispatchers.IO) {
    val data = NSData.dataWithContentsOfFile("$cacheDir/$key") ?: return@withContext null
    val size = data.length.toInt()
    if (size == 0) return@withContext null
    ByteArray(size).apply { usePinned { memcpy(it.addressOf(0), data.bytes, data.length) } }
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
internal actual suspend fun writeImageDiskCache(key: String, bytes: ByteArray) = withContext(Dispatchers.IO) {
    if (bytes.isEmpty()) return@withContext
    val data = bytes.usePinned { NSData.create(bytes = it.addressOf(0), length = bytes.size.toULong()) }
    data.writeToFile("$cacheDir/$key", atomically = true)
    Unit
}

internal actual val imageFetchViaProxy: Boolean = false
