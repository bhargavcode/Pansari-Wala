package org.bhargav.pansariwala.util

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.time

@OptIn(ExperimentalForeignApi::class)
internal actual fun platformNowMillis(): Long = time(null) * 1_000L
