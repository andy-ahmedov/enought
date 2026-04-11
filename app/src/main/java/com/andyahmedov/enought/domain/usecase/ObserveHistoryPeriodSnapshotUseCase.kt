package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.HistoryPeriod
import com.andyahmedov.enought.domain.model.HistoryPeriodSnapshot
import com.andyahmedov.enought.domain.model.PeriodSpendSummary
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import java.time.Clock
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveHistoryPeriodSnapshotUseCase(
    private val paymentEventRepository: PaymentEventRepository,
    private val clock: Clock,
) {
    operator fun invoke(
        period: HistoryPeriod,
    ): Flow<HistoryPeriodSnapshot> {
        val dateRange = period.toSpendDateRange(clock)
        val instantRange = dateRange.toInstantRange(clock)

        return paymentEventRepository.observePaymentEventsBetween(
            startInclusive = instantRange.startInclusive,
            endExclusive = instantRange.endExclusive,
        ).map { events ->
            val aggregate = events.toSpendAggregate()

            HistoryPeriodSnapshot(
                period = period,
                summary = PeriodSpendSummary(
                    startDate = dateRange.startDate,
                    endDateInclusive = dateRange.endDateInclusive,
                    totalAmountMinor = aggregate.totalAmountMinor,
                    paymentsCount = aggregate.paymentsCount,
                    lastPaymentAmountMinor = aggregate.lastPaymentAmountMinor,
                    hasLowConfidenceItems = aggregate.hasLowConfidenceItems,
                ),
                paymentEvents = if (period == HistoryPeriod.TODAY) {
                    events.includedRubEvents()
                } else {
                    emptyList()
                },
                daySummaries = if (period == HistoryPeriod.TODAY) {
                    emptyList()
                } else {
                    events.toHistoryDaySummaries(clock)
                },
            )
        }
    }
}
