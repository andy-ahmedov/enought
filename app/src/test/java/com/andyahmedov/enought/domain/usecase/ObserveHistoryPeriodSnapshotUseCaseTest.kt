package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.ConfidenceLevel
import com.andyahmedov.enought.domain.model.HistoryPeriod
import com.andyahmedov.enought.domain.model.PaymentChannel
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.PaymentStatus
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveHistoryPeriodSnapshotUseCaseTest {
    @Test
    fun `today period returns payment list and no grouped day summaries`() = runTest {
        val useCase = ObserveHistoryPeriodSnapshotUseCase(
            paymentEventRepository = FakePaymentEventRepository(
                listOf(
                    paymentEvent(
                        id = "event-1",
                        paidAt = Instant.parse("2026-04-01T12:00:00Z"),
                        amountMinor = 125000L,
                    ),
                    paymentEvent(
                        id = "event-2",
                        paidAt = Instant.parse("2026-04-01T18:45:00Z"),
                        amountMinor = 9900L,
                    ),
                ),
            ),
            clock = fixedClock(),
        )

        val snapshot = useCase(HistoryPeriod.TODAY).first()

        assertEquals(2, snapshot.summary.paymentsCount)
        assertEquals(listOf("event-2", "event-1"), snapshot.paymentEvents.map { it.id })
        assertTrue(snapshot.daySummaries.isEmpty())
    }

    @Test
    fun `last 7 days groups included events by local day and keeps suspected review flag`() = runTest {
        val useCase = ObserveHistoryPeriodSnapshotUseCase(
            paymentEventRepository = FakePaymentEventRepository(
                listOf(
                    paymentEvent(
                        id = "day-1-event-1",
                        paidAt = Instant.parse("2026-04-01T18:45:00Z"),
                        amountMinor = 9900L,
                    ),
                    paymentEvent(
                        id = "day-1-event-2",
                        paidAt = Instant.parse("2026-04-01T12:00:00Z"),
                        amountMinor = 125000L,
                    ),
                    paymentEvent(
                        id = "day-2-event-1",
                        paidAt = Instant.parse("2026-03-31T09:00:00Z"),
                        amountMinor = 48000L,
                    ),
                    paymentEvent(
                        id = "suspected-1",
                        paidAt = Instant.parse("2026-03-31T10:00:00Z"),
                        amountMinor = 53000L,
                        confidence = ConfidenceLevel.LOW,
                        status = PaymentStatus.SUSPECTED,
                    ),
                ),
            ),
            clock = fixedClock(),
        )

        val snapshot = useCase(HistoryPeriod.LAST_7_DAYS).first()

        assertEquals(182900L, snapshot.summary.totalAmountMinor)
        assertEquals(3, snapshot.summary.paymentsCount)
        assertTrue(snapshot.summary.hasLowConfidenceItems)
        assertTrue(snapshot.paymentEvents.isEmpty())
        assertEquals(listOf("2026-04-01", "2026-03-31"), snapshot.daySummaries.map { it.date.toString() })
        assertEquals(listOf(134900L, 48000L), snapshot.daySummaries.map { it.totalAmountMinor })
        assertEquals(listOf(2, 1), snapshot.daySummaries.map { it.paymentsCount })
    }

    @Test
    fun `last 30 days uses local zone boundaries`() = runTest {
        val useCase = ObserveHistoryPeriodSnapshotUseCase(
            paymentEventRepository = FakePaymentEventRepository(
                listOf(
                    paymentEvent(
                        id = "outside-window",
                        paidAt = Instant.parse("2026-03-02T20:59:59Z"),
                        amountMinor = 10000L,
                    ),
                    paymentEvent(
                        id = "window-start",
                        paidAt = Instant.parse("2026-03-02T21:00:00Z"),
                        amountMinor = 20000L,
                    ),
                ),
            ),
            clock = fixedClock(),
        )

        val snapshot = useCase(HistoryPeriod.LAST_30_DAYS).first()

        assertEquals(20000L, snapshot.summary.totalAmountMinor)
        assertEquals(1, snapshot.summary.paymentsCount)
        assertFalse(snapshot.summary.hasLowConfidenceItems)
    }

    private fun fixedClock(): Clock {
        return Clock.fixed(
            Instant.parse("2026-03-31T21:30:00Z"),
            ZoneId.of("Europe/Moscow"),
        )
    }

    private fun paymentEvent(
        id: String,
        paidAt: Instant,
        amountMinor: Long,
        confidence: ConfidenceLevel = ConfidenceLevel.MEDIUM,
        status: PaymentStatus = PaymentStatus.CONFIRMED,
    ): PaymentEvent {
        return PaymentEvent(
            id = id,
            amountMinor = amountMinor,
            currency = "RUB",
            paidAt = paidAt,
            merchantName = null,
            sourceKind = PaymentSourceKind.BANK,
            paymentChannel = PaymentChannel.UNKNOWN,
            confidence = confidence,
            status = status,
            userEdited = false,
            sourceIds = listOf("raw-$id"),
        )
    }

    private class FakePaymentEventRepository(
        private val events: List<PaymentEvent>,
    ) : PaymentEventRepository {
        override suspend fun save(event: PaymentEvent) = Unit

        override suspend fun saveAll(events: List<PaymentEvent>) = Unit

        override suspend fun getById(id: String): PaymentEvent? {
            return events.firstOrNull { event -> event.id == id }
        }

        override suspend fun getByDuplicateGroupId(duplicateGroupId: String): List<PaymentEvent> {
            return events.filter { event -> event.duplicateGroupId == duplicateGroupId }
        }

        override suspend fun getPaymentEventsBetween(
            startInclusive: Instant,
            endExclusive: Instant,
        ): List<PaymentEvent> {
            return filtered(startInclusive, endExclusive)
        }

        override fun observePaymentEvents(): Flow<List<PaymentEvent>> {
            return flowOf(events.sortedByDescending { it.paidAt })
        }

        override fun observePaymentEventsBetween(
            startInclusive: Instant,
            endExclusive: Instant,
        ): Flow<List<PaymentEvent>> {
            return flowOf(filtered(startInclusive, endExclusive))
        }

        private fun filtered(
            startInclusive: Instant,
            endExclusive: Instant,
        ): List<PaymentEvent> {
            return events.filter { event ->
                event.paidAt >= startInclusive && event.paidAt < endExclusive
            }.sortedByDescending { it.paidAt }
        }
    }
}
