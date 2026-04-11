package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.DailySpendSummary
import com.andyahmedov.enought.domain.model.DailyLimitWarningLevel
import com.andyahmedov.enought.domain.repository.DailyLimitRepository
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import java.time.Clock
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
        val dayRange = today.toSpendDateRange().toInstantRange(clock)

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

    private fun List<com.andyahmedov.enought.domain.model.PaymentEvent>.toDailySpendSummary(
        today: LocalDate,
        limitAmountMinor: Long?,
    ): DailySpendSummary {
        val aggregate = toSpendAggregate()
        val totalAmountMinor = aggregate.totalAmountMinor
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
            paymentsCount = aggregate.paymentsCount,
            lastPaymentAmountMinor = aggregate.lastPaymentAmountMinor,
            limitAmountMinor = limitAmountMinor,
            remainingAmountMinor = remainingAmountMinor,
            limitWarningLevel = limitWarningLevel,
            hasLowConfidenceItems = aggregate.hasLowConfidenceItems,
        )
    }

    private companion object {
        const val PERCENT_BASE = 100L
        const val NEAR_LIMIT_PERCENT = 80L
    }
}
