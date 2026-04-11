package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.RawNotificationEvent
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import com.andyahmedov.enought.domain.repository.RawNotificationEventRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class EnforceDataRetentionUseCaseTest {
    @Test
    fun `invoke deletes payment and raw data older than 90 local days`() = runTest {
        val paymentEventRepository = FakePaymentEventRepository()
        val rawNotificationEventRepository = FakeRawNotificationEventRepository()
        val useCase = EnforceDataRetentionUseCase(
            paymentEventRepository = paymentEventRepository,
            rawNotificationEventRepository = rawNotificationEventRepository,
            clock = Clock.fixed(
                Instant.parse("2026-03-31T21:30:00Z"),
                ZoneId.of("Europe/Moscow"),
            ),
        )

        useCase()

        val expectedCutoff = Instant.parse("2026-01-01T21:00:00Z")
        assertEquals(expectedCutoff, paymentEventRepository.lastDeleteOlderThanCutoff)
        assertEquals(expectedCutoff, rawNotificationEventRepository.lastDeleteOlderThanCutoff)
    }

    private class FakePaymentEventRepository : PaymentEventRepository {
        var lastDeleteOlderThanCutoff: Instant? = null

        override suspend fun save(event: PaymentEvent) = Unit

        override suspend fun saveAll(events: List<PaymentEvent>) = Unit

        override suspend fun getById(id: String): PaymentEvent? = null

        override suspend fun getByDuplicateGroupId(duplicateGroupId: String): List<PaymentEvent> = emptyList()

        override suspend fun getPaymentEventsBetween(
            startInclusive: Instant,
            endExclusive: Instant,
        ): List<PaymentEvent> = emptyList()

        override fun observePaymentEvents(): Flow<List<PaymentEvent>> = flowOf(emptyList())

        override fun observePaymentEventsBetween(
            startInclusive: Instant,
            endExclusive: Instant,
        ): Flow<List<PaymentEvent>> = flowOf(emptyList())

        override suspend fun deleteOlderThan(cutoffExclusive: Instant) {
            lastDeleteOlderThanCutoff = cutoffExclusive
        }
    }

    private class FakeRawNotificationEventRepository : RawNotificationEventRepository {
        var lastDeleteOlderThanCutoff: Instant? = null

        override suspend fun saveIfNew(event: RawNotificationEvent): Boolean = true

        override suspend fun getRawEventsBetween(
            startInclusive: Instant,
            endExclusive: Instant,
        ): List<RawNotificationEvent> = emptyList()

        override fun observeRawEvents(): Flow<List<RawNotificationEvent>> = flowOf(emptyList())

        override suspend fun deleteOlderThan(cutoffExclusive: Instant) {
            lastDeleteOlderThanCutoff = cutoffExclusive
        }
    }
}
