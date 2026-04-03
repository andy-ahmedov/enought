package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentStatus
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveTodayPaymentEventsUseCase(
    private val paymentEventRepository: PaymentEventRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<List<PaymentEvent>> {
        val today = LocalDate.now(clock)
        val dayRange = today.toInstantRange(clock)

        return paymentEventRepository.observePaymentEventsBetween(
            startInclusive = dayRange.startInclusive,
            endExclusive = dayRange.endExclusive,
        ).map { events ->
            events.filter { event ->
                event.status in INCLUDED_STATUSES && event.currency == RUB_CURRENCY
            }
        }
    }

    private fun LocalDate.toInstantRange(clock: Clock): InstantRange {
        val zoneId = clock.zone
        val startInclusive = atStartOfDay(zoneId).toInstant()
        val endExclusive = plusDays(1).atStartOfDay(zoneId).toInstant()

        return InstantRange(
            startInclusive = startInclusive,
            endExclusive = endExclusive,
        )
    }

    private data class InstantRange(
        val startInclusive: Instant,
        val endExclusive: Instant,
    )

    private companion object {
        const val RUB_CURRENCY = "RUB"
        val INCLUDED_STATUSES = setOf(
            PaymentStatus.CONFIRMED,
            PaymentStatus.CORRECTED,
        )
    }
}
