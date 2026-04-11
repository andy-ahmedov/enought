package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import com.andyahmedov.enought.domain.repository.RawNotificationEventRepository
import java.time.Clock
import java.time.LocalDate

class EnforceDataRetentionUseCase(
    private val paymentEventRepository: PaymentEventRepository,
    private val rawNotificationEventRepository: RawNotificationEventRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke() {
        val cutoffExclusive = LocalDate.now(clock)
            .minusDays(RETENTION_DAYS - 1L)
            .atStartOfDay(clock.zone)
            .toInstant()

        paymentEventRepository.deleteOlderThan(cutoffExclusive)
        rawNotificationEventRepository.deleteOlderThan(cutoffExclusive)
    }

    private companion object {
        const val RETENTION_DAYS = 90L
    }
}
