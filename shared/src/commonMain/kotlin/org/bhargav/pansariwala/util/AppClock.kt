@file:OptIn(ExperimentalTime::class)

package org.bhargav.pansariwala.util

import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

const val MILLIS_PER_DAY: Long = 86_400_000L

object AppClock {
    fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    /** Start of the current day (UTC). Good enough for demo day-bucketing. */
    fun startOfTodayMillis(): Long = nowMillis().let { it - (it % MILLIS_PER_DAY) }
}

fun generateId(prefix: String): String =
    prefix + "_" + AppClock.nowMillis() + "_" + Random.nextInt(0, 100_000)
