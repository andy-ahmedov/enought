package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveYesterdayTotalUseCase(
    private val paymentEventRepository: PaymentEventRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<Long> {
        val yesterday = LocalDate.now(clock).minusDays(1L)
        val instantRange = yesterday.toSpendDateRange().toInstantRange(clock)

        return paymentEventRepository.observePaymentEventsBetween(
            startInclusive = instantRange.startInclusive,
            endExclusive = instantRange.endExclusive,
        ).map { events ->
            events.toSpendAggregate().totalAmountMinor
        }
    }
}
