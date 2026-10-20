package org.bhargav.pansariwala.media

internal actual suspend fun readImageDiskCache(key: String): ByteArray? = null

internal actual suspend fun writeImageDiskCache(key: String, bytes: ByteArray) = Unit

internal actual val imageFetchViaProxy: Boolean = true
