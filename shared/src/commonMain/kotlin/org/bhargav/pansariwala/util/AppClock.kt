package org.bhargav.pansariwala.util

import kotlin.random.Random

const val MILLIS_PER_DAY: Long = 86_400_000L

internal expect fun platformNowMillis(): Long

object AppClock {
    fun nowMillis(): Long = platformNowMillis()

    /** Start of the current day (UTC). Good enough for demo day-bucketing. */
    fun startOfTodayMillis(): Long = nowMillis().let { it - (it % MILLIS_PER_DAY) }
}

fun generateId(prefix: String): String =
    prefix + "_" + AppClock.nowMillis() + "_" + Random.nextInt(0, 100_000)
