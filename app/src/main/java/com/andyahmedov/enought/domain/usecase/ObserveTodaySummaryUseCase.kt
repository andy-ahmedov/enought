package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.DailySpendSummary
import com.andyahmedov.enought.domain.model.DailyLimitWarningLevel
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentStatus
import com.andyahmedov.enought.domain.repository.DailyLimitRepository
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class ObserveTodaySummaryUseCase(
    private val paymentEventRepository: PaymentEventRepository,
    private val dailyLimitRepository: DailyLimitRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<DailySpendSummary> {
        val today = LocalDate.now(clock)
        val dayRange = today.toInstantRange(clock)

        return paymentEventRepository.observePaymentEventsBetween(
            startInclusive = dayRange.startInclusive,
            endExclusive = dayRange.endExclusive,
        ).combine(dailyLimitRepository.observeLimitAmountMinor()) { events, limitAmountMinor ->
            events.toDailySpendSummary(
                today = today,
                limitAmountMinor = limitAmountMinor,
            )
        }
    }

    private fun List<PaymentEvent>.toDailySpendSummary(
        today: LocalDate,
        limitAmountMinor: Long?,
    ): DailySpendSummary {
        val includedRubEvents = filter { event ->
            event.status in INCLUDED_STATUSES && event.currency == RUB_CURRENCY
        }
        val hasLowConfidenceItems = any { event ->
            event.status == PaymentStatus.SUSPECTED
        }
        val totalAmountMinor = includedRubEvents.sumOf { it.amountMinor }
        val remainingAmountMinor = limitAmountMinor?.minus(totalAmountMinor)
        val limitWarningLevel = when {
            limitAmountMinor == null -> null
            remainingAmountMinor == null -> null
            remainingAmountMinor <= 0L -> DailyLimitWarningLevel.LIMIT_REACHED
            totalAmountMinor * PERCENT_BASE >= limitAmountMinor * NEAR_LIMIT_PERCENT -> {
                DailyLimitWarningLevel.NEAR_LIMIT
            }
            else -> null
        }

        return DailySpendSummary(
            date = today,
            totalAmountMinor = totalAmountMinor,
            paymentsCount = includedRubEvents.size,
            lastPaymentAmountMinor = includedRubEvents.firstOrNull()?.amountMinor,
            limitAmountMinor = limitAmountMinor,
            remainingAmountMinor = remainingAmountMinor,
            limitWarningLevel = limitWarningLevel,
            hasLowConfidenceItems = hasLowConfidenceItems,
        )
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
        const val PERCENT_BASE = 100L
        const val NEAR_LIMIT_PERCENT = 80L
        val INCLUDED_STATUSES = setOf(
            PaymentStatus.CONFIRMED,
            PaymentStatus.CORRECTED,
        )
    }
}
