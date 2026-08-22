@file:OptIn(ExperimentalTime::class)

package org.bhargav.pansariwala.util

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

internal actual fun platformNowMillis(): Long = Clock.System.now().toEpochMilliseconds()
