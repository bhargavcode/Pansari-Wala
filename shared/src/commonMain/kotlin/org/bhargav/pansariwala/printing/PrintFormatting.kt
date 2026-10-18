package org.bhargav.pansariwala.printing

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** `dd/MM/yyyy HH:mm` in the device time zone. */
fun formatPrintDateTime(epochMs: Long): String {
    val t = Instant.fromEpochMilliseconds(epochMs).toLocalDateTime(TimeZone.currentSystemDefault())
    fun Int.two() = toString().padStart(2, '0')
    return "${t.day.two()}/${t.month.ordinal.plus(1).two()}/${t.year} ${t.hour.two()}:${t.minute.two()}"
}

/** `dd/MM/yyyy` in the device time zone. */
fun formatPrintDate(epochMs: Long): String = formatPrintDateTime(epochMs).substringBefore(' ')
