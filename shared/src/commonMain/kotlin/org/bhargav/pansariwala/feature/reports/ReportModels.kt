package org.bhargav.pansariwala.feature.reports

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.monthsUntil
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.bhargav.pansariwala.domain.model.OrderChannel
import org.bhargav.pansariwala.domain.model.OrderStatus
import kotlin.time.Instant

enum class ReportChannel { ALL, ONLINE, OFFLINE }

/** Report filters handed to drill-down routes; custom dates are UTC-midnight picker millis. */
@kotlinx.serialization.Serializable
data class SalesWindow(
    val period: String,
    val channel: String,
    val customFromUtc: Long? = null,
    val customToUtc: Long? = null,
)

enum class ReportPeriod { TODAY, YESTERDAY, WEEK, MONTH, YEAR, CUSTOM }

enum class BucketUnit { HOUR, DAY, MONTH }

/** Half-open local-date range: [start, endExclusive). */
data class ReportRange(val start: LocalDate, val endExclusive: LocalDate) {
    val endInclusive: LocalDate get() = endExclusive.minus(1, DateTimeUnit.DAY)
    fun fromMs(tz: TimeZone): Long = start.atStartOfDayIn(tz).toEpochMilliseconds()
    fun toMs(tz: TimeZone): Long = endExclusive.atStartOfDayIn(tz).toEpochMilliseconds()
}

data class ReportOrder(
    val id: String,
    val displayNumber: String,
    val createdAtEpochMs: Long,
    val channel: OrderChannel,
    val customerName: String?,
    val itemCount: Int,
    val total: Double,
    val status: OrderStatus,
) {
    val countsTowardRevenue: Boolean
        get() = status != OrderStatus.CANCELLED && status != OrderStatus.REJECTED && status != OrderStatus.DRAFT
}

data class ReportBucket(
    val unit: BucketUnit,
    /** Hour of day for HOUR buckets; otherwise unused. */
    val hour: Int,
    val day: Int,
    val month: Int,
    val year: Int,
    val onlineOrders: Int = 0,
    val offlineOrders: Int = 0,
    val onlineRevenue: Double = 0.0,
    val offlineRevenue: Double = 0.0,
)

data class ReportSummary(
    val totalOrders: Int = 0,
    val revenue: Double = 0.0,
    val onlineOrders: Int = 0,
    val onlineRevenue: Double = 0.0,
    val offlineOrders: Int = 0,
    val offlineRevenue: Double = 0.0,
    val cancelledOrders: Int = 0,
) {
    val averageOrderValue: Double
        get() {
            val paid = totalOrders - cancelledOrders
            return if (paid <= 0) 0.0 else revenue / paid
        }
}

object ReportCalculator {

    fun range(period: ReportPeriod, today: LocalDate, customFrom: LocalDate?, customTo: LocalDate?): ReportRange {
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        return when (period) {
            ReportPeriod.TODAY -> ReportRange(today, tomorrow)
            ReportPeriod.YESTERDAY -> ReportRange(today.minus(1, DateTimeUnit.DAY), today)
            ReportPeriod.WEEK -> {
                val monday = today.minus(today.dayOfWeek.isoDayNumber - 1, DateTimeUnit.DAY)
                ReportRange(monday, monday.plus(1, DateTimeUnit.WEEK))
            }
            ReportPeriod.MONTH -> {
                val first = LocalDate(today.year, today.month, 1)
                ReportRange(first, first.plus(1, DateTimeUnit.MONTH))
            }
            ReportPeriod.YEAR -> {
                val first = LocalDate(today.year, 1, 1)
                ReportRange(first, first.plus(1, DateTimeUnit.YEAR))
            }
            ReportPeriod.CUSTOM -> {
                val from = customFrom ?: today
                val to = (customTo ?: from).let { if (it < from) from else it }
                ReportRange(from, to.plus(1, DateTimeUnit.DAY))
            }
        }
    }

    fun bucketUnit(range: ReportRange): BucketUnit {
        val days = range.start.daysUntil(range.endExclusive)
        return when {
            days <= 1 -> BucketUnit.HOUR
            days <= MAX_DAILY_BUCKETS -> BucketUnit.DAY
            else -> BucketUnit.MONTH
        }
    }

    /** Single pass over [orders]; empty buckets are kept so the x-axis is continuous. */
    fun buckets(orders: List<ReportOrder>, range: ReportRange, unit: BucketUnit, tz: TimeZone): List<ReportBucket> {
        val base = when (unit) {
            BucketUnit.HOUR -> List(HOURS_PER_DAY) { h ->
                ReportBucket(unit, h, range.start.day, range.start.month.number, range.start.year)
            }
            BucketUnit.DAY -> List(range.start.daysUntil(range.endExclusive)) { i ->
                val d = range.start.plus(i, DateTimeUnit.DAY)
                ReportBucket(unit, 0, d.day, d.month.number, d.year)
            }
            BucketUnit.MONTH -> {
                val first = LocalDate(range.start.year, range.start.month, 1)
                List(first.monthsUntil(range.endInclusive) + 1) { i ->
                    val d = first.plus(i, DateTimeUnit.MONTH)
                    ReportBucket(unit, 0, 1, d.month.number, d.year)
                }
            }
        }
        if (base.isEmpty()) return base
        val onlineCount = IntArray(base.size)
        val offlineCount = IntArray(base.size)
        val onlineRev = DoubleArray(base.size)
        val offlineRev = DoubleArray(base.size)
        val monthOrigin = LocalDate(range.start.year, range.start.month, 1)
        orders.forEach { order ->
            val local = Instant.fromEpochMilliseconds(order.createdAtEpochMs).toLocalDateTime(tz)
            val index = when (unit) {
                BucketUnit.HOUR -> local.hour
                BucketUnit.DAY -> range.start.daysUntil(local.date)
                BucketUnit.MONTH -> monthOrigin.monthsUntil(local.date)
            }
            if (index !in base.indices) return@forEach
            val revenue = if (order.countsTowardRevenue) order.total else 0.0
            if (order.channel == OrderChannel.ONLINE) {
                onlineCount[index]++
                onlineRev[index] += revenue
            } else {
                offlineCount[index]++
                offlineRev[index] += revenue
            }
        }
        return base.mapIndexed { i, b ->
            b.copy(
                onlineOrders = onlineCount[i],
                offlineOrders = offlineCount[i],
                onlineRevenue = onlineRev[i],
                offlineRevenue = offlineRev[i],
            )
        }
    }

    fun summary(orders: List<ReportOrder>): ReportSummary {
        var onlineOrders = 0
        var offlineOrders = 0
        var onlineRevenue = 0.0
        var offlineRevenue = 0.0
        var cancelled = 0
        orders.forEach { o ->
            val revenue = if (o.countsTowardRevenue) o.total else 0.0
            if (!o.countsTowardRevenue) cancelled++
            if (o.channel == OrderChannel.ONLINE) {
                onlineOrders++
                onlineRevenue += revenue
            } else {
                offlineOrders++
                offlineRevenue += revenue
            }
        }
        return ReportSummary(
            totalOrders = orders.size,
            revenue = onlineRevenue + offlineRevenue,
            onlineOrders = onlineOrders,
            onlineRevenue = onlineRevenue,
            offlineOrders = offlineOrders,
            offlineRevenue = offlineRevenue,
            cancelledOrders = cancelled,
        )
    }

    fun filterChannel(orders: List<ReportOrder>, channel: ReportChannel): List<ReportOrder> = when (channel) {
        ReportChannel.ALL -> orders
        ReportChannel.ONLINE -> orders.filter { it.channel == OrderChannel.ONLINE }
        ReportChannel.OFFLINE -> orders.filter { it.channel != OrderChannel.ONLINE }
    }

    private const val HOURS_PER_DAY = 24
    private const val MAX_DAILY_BUCKETS = 62
}
