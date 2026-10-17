package org.bhargav.pansariwala.feature.delivery

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import org.bhargav.pansariwala.feature.reports.ReportCalculator
import org.bhargav.pansariwala.feature.reports.ReportPeriod
import org.bhargav.pansariwala.feature.reports.ReportRange

enum class PartnerEarningsPeriod {
    TODAY, YESTERDAY, WEEK, MONTH, THREE_MONTHS, SIX_MONTHS, YEAR, CUSTOM;

    /** Week/month/year are calendar-to-date; 3 and 6 months are rolling windows ending today. */
    fun range(today: LocalDate, customFrom: LocalDate?, customTo: LocalDate?): ReportRange {
        val tomorrow = today.plus(1, DateTimeUnit.DAY)
        return when (this) {
            TODAY -> ReportCalculator.range(ReportPeriod.TODAY, today, null, null)
            YESTERDAY -> ReportCalculator.range(ReportPeriod.YESTERDAY, today, null, null)
            WEEK -> ReportCalculator.range(ReportPeriod.WEEK, today, null, null)
            MONTH -> ReportCalculator.range(ReportPeriod.MONTH, today, null, null)
            THREE_MONTHS -> ReportRange(today.minus(3, DateTimeUnit.MONTH), tomorrow)
            SIX_MONTHS -> ReportRange(today.minus(6, DateTimeUnit.MONTH), tomorrow)
            YEAR -> ReportCalculator.range(ReportPeriod.YEAR, today, null, null)
            CUSTOM -> ReportCalculator.range(ReportPeriod.CUSTOM, today, customFrom, customTo)
        }
    }
}
