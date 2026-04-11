package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.HistoryDaySummary
import com.andyahmedov.enought.domain.model.HistoryPeriod
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentStatus
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

internal data class SpendDateRange(
    val startDate: LocalDate,
    val endDateExclusive: LocalDate,
) {
    val endDateInclusive: LocalDate
        get() = endDateExclusive.minusDays(1L)
}

internal data class SpendInstantRange(
    val startInclusive: Instant,
    val endExclusive: Instant,
)

internal data class SpendAggregate(
    val totalAmountMinor: Long,
    val paymentsCount: Int,
    val lastPaymentAmountMinor: Long?,
    val hasLowConfidenceItems: Boolean,
)

internal fun HistoryPeriod.toSpendDateRange(
    clock: Clock,
): SpendDateRange {
    val today = LocalDate.now(clock)
    val startDate = when (this) {
        HistoryPeriod.TODAY -> today
        HistoryPeriod.LAST_7_DAYS -> today.minusDays(6L)
        HistoryPeriod.LAST_30_DAYS -> today.minusDays(29L)
        HistoryPeriod.LAST_90_DAYS -> today.minusDays(89L)
    }

    return SpendDateRange(
        startDate = startDate,
        endDateExclusive = today.plusDays(1L),
    )
}

internal fun LocalDate.toSpendDateRange(): SpendDateRange {
    return SpendDateRange(
        startDate = this,
        endDateExclusive = plusDays(1L),
    )
}

internal fun SpendDateRange.toInstantRange(
    clock: Clock,
): SpendInstantRange {
    val zoneId = clock.zone

    return SpendInstantRange(
        startInclusive = startDate.atStartOfDay(zoneId).toInstant(),
        endExclusive = endDateExclusive.atStartOfDay(zoneId).toInstant(),
    )
}

internal fun List<PaymentEvent>.toSpendAggregate(): SpendAggregate {
    val includedRubEvents = includedRubEvents()
    return SpendAggregate(
        totalAmountMinor = includedRubEvents.sumOf { event -> event.amountMinor },
        paymentsCount = includedRubEvents.size,
        lastPaymentAmountMinor = includedRubEvents.firstOrNull()?.amountMinor,
        hasLowConfidenceItems = any { event -> event.status == PaymentStatus.SUSPECTED },
    )
}

internal fun List<PaymentEvent>.toHistoryDaySummaries(
    clock: Clock,
): List<HistoryDaySummary> {
    return includedRubEvents()
        .groupBy { event -> event.paidAt.atZone(clock.zone).toLocalDate() }
        .toSortedMap(compareByDescending { it })
        .map { (date, dayEvents) ->
            val aggregate = dayEvents.toSpendAggregate()
            HistoryDaySummary(
                date = date,
                totalAmountMinor = aggregate.totalAmountMinor,
                paymentsCount = aggregate.paymentsCount,
                lastPaymentAmountMinor = aggregate.lastPaymentAmountMinor,
            )
        }
}

internal fun List<PaymentEvent>.includedRubEvents(): List<PaymentEvent> {
    return filter { event ->
        event.status in INCLUDED_SUMMARY_STATUSES && event.currency == RUB_CURRENCY
    }
}

internal const val RUB_CURRENCY = "RUB"

internal val INCLUDED_SUMMARY_STATUSES = setOf(
    PaymentStatus.CONFIRMED,
    PaymentStatus.CORRECTED,
)
