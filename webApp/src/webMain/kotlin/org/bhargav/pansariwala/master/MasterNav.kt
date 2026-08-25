package org.bhargav.pansariwala.master

import org.bhargav.pansariwala.util.AppClock
import org.bhargav.pansariwala.util.AppConstants
import org.bhargav.pansariwala.util.MILLIS_PER_DAY

sealed class MasterDest {
    data object Dashboard : MasterDest()
    data object Shops : MasterDest()
    data object ShopCreate : MasterDest()
    data class ShopDetail(val id: String) : MasterDest()
    data object Products : MasterDest()
    data class ProductEdit(val id: String?) : MasterDest()
    data object Transactions : MasterDest()
    data class TxnDetail(val id: String) : MasterDest()
    data object Users : MasterDest()
    data class UserDetail(val id: String) : MasterDest()
    data object Partners : MasterDest()
    data class PartnerDetail(val id: String) : MasterDest()
    data object Platform : MasterDest()
    data object Settings : MasterDest()
}

fun dateFilterRange(filter: String): Pair<Long?, Long?> {
    val start = AppClock.startOfTodayMillis()
    return when (filter) {
        AppConstants.DateFilter.TODAY -> start to (start + MILLIS_PER_DAY - 1)
        AppConstants.DateFilter.YESTERDAY -> (start - MILLIS_PER_DAY) to (start - 1)
        AppConstants.DateFilter.WEEKLY -> (start - 6 * MILLIS_PER_DAY) to (start + MILLIS_PER_DAY - 1)
        AppConstants.DateFilter.MONTHLY -> (start - 29 * MILLIS_PER_DAY) to (start + MILLIS_PER_DAY - 1)
        AppConstants.DateFilter.YEARLY -> (start - 364 * MILLIS_PER_DAY) to (start + MILLIS_PER_DAY - 1)
        else -> null to null
    }
}

fun formatEpochDate(ms: Long): String {
    if (ms <= 0L) return "—"
    val days = ms / MILLIS_PER_DAY
    var z = days + 719_468
    val era = (if (z >= 0) z else z - 146_096) / 146_097
    val doe = z - era * 146_097
    val yoe = (doe - doe / 1460 + doe / 36524 - doe / 146_096) / 365
    val y = yoe + era * 400
    val doy = doe - (365 * yoe + yoe / 4 - yoe / 100)
    val mp = (5 * doy + 2) / 153
    val d = doy - (153 * mp + 2) / 5 + 1
    val m = mp + (if (mp < 10) 3 else -9)
    val year = y + if (m <= 2) 1 else 0
    return "$year-${m.toString().padStart(2, '0')}-${d.toString().padStart(2, '0')}"
}

fun formatInr(amount: Double): String {
    val paise = (amount * 100).toLong()
    val whole = paise / 100
    val frac = (paise % 100).toString().padStart(2, '0')
    return "₹$whole.$frac"
}
