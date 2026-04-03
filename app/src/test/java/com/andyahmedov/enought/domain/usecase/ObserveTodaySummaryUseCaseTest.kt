package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.ConfidenceLevel
import com.andyahmedov.enought.domain.model.DailyLimitWarningLevel
import com.andyahmedov.enought.domain.model.PaymentChannel
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.PaymentStatus
import com.andyahmedov.enought.domain.repository.DailyLimitRepository
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveTodaySummaryUseCaseTest {
    @Test
    fun `invoke returns empty summary for day without confirmed events`() = runTest {
        val useCase = ObserveTodaySummaryUseCase(
            paymentEventRepository = FakePaymentEventRepository(emptyList()),
            dailyLimitRepository = FakeDailyLimitRepository(null),
            clock = fixedClock(),
        )

        val summary = useCase().first()

        assertEquals("2026-04-01", summary.date.toString())
        assertEquals(0L, summary.totalAmountMinor)
        assertEquals(0, summary.paymentsCount)
        assertNull(summary.lastPaymentAmountMinor)
        assertNull(summary.limitAmountMinor)
        assertNull(summary.remainingAmountMinor)
        assertNull(summary.limitWarningLevel)
        assertFalse(summary.hasLowConfidenceItems)
    }

    @Test
    fun `invoke aggregates confirmed events and uses latest confirmed payment as last amount`() = runTest {
        val useCase = ObserveTodaySummaryUseCase(
            paymentEventRepository = FakePaymentEventRepository(
                listOf(
                    paymentEvent(id = "event-1", paidAt = Instant.parse("2026-03-31T21:10:00Z"), amountMinor = 34900L),
                    paymentEvent(id = "event-2", paidAt = Instant.parse("2026-04-01T12:00:00Z"), amountMinor = 125000L),
                    paymentEvent(id = "event-3", paidAt = Instant.parse("2026-04-01T18:45:00Z"), amountMinor = 9900L),
                ),
            ),
            dailyLimitRepository = FakeDailyLimitRepository(null),
            clock = fixedClock(),
        )

        val summary = useCase().first()

        assertEquals(169800L, summary.totalAmountMinor)
        assertEquals(3, summary.paymentsCount)
        assertEquals(9900L, summary.lastPaymentAmountMinor)
        assertFalse(summary.hasLowConfidenceItems)
    }

    @Test
    fun `invoke excludes suspected event from totals but keeps review flag`() = runTest {
        val useCase = ObserveTodaySummaryUseCase(
            paymentEventRepository = FakePaymentEventRepository(
                listOf(
                    paymentEvent(
                        id = "event-1",
                        paidAt = Instant.parse("2026-04-01T08:00:00Z"),
                        amountMinor = 50000L,
                    ),
                    paymentEvent(
                        id = "event-2",
                        paidAt = Instant.parse("2026-04-01T09:00:00Z"),
                        amountMinor = 70000L,
                        confidence = ConfidenceLevel.LOW,
                        status = PaymentStatus.SUSPECTED,
                    ),
                ),
            ),
            dailyLimitRepository = FakeDailyLimitRepository(null),
            clock = fixedClock(),
        )

        val summary = useCase().first()

        assertEquals(50000L, summary.totalAmountMinor)
        assertEquals(1, summary.paymentsCount)
        assertEquals(50000L, summary.lastPaymentAmountMinor)
        assertTrue(summary.hasLowConfidenceItems)
    }

    @Test
    fun `invoke excludes duplicate conflict pair from total but keeps review flag`() = runTest {
        val useCase = ObserveTodaySummaryUseCase(
            paymentEventRepository = FakePaymentEventRepository(
                listOf(
                    paymentEvent(
                        id = "confirmed-1",
                        paidAt = Instant.parse("2026-04-01T08:00:00Z"),
                        amountMinor = 10000L,
                    ),
                    paymentEvent(
                        id = "bank-1",
                        paidAt = Instant.parse("2026-04-01T10:00:00Z"),
                        amountMinor = 34900L,
                        status = PaymentStatus.SUSPECTED,
                        duplicateGroupId = "dup-1",
                    ),
                    paymentEvent(
                        id = "mir-1",
                        paidAt = Instant.parse("2026-04-01T10:02:00Z"),
                        amountMinor = 34900L,
                        status = PaymentStatus.SUSPECTED,
                        duplicateGroupId = "dup-1",
                    ),
                ),
            ),
            dailyLimitRepository = FakeDailyLimitRepository(null),
            clock = fixedClock(),
        )

        val summary = useCase().first()

        assertEquals(10000L, summary.totalAmountMinor)
        assertEquals(1, summary.paymentsCount)
        assertEquals(10000L, summary.lastPaymentAmountMinor)
        assertTrue(summary.hasLowConfidenceItems)
    }

    @Test
    fun `invoke includes corrected event in totals and clears review flag when no suspected items remain`() = runTest {
        val useCase = ObserveTodaySummaryUseCase(
            paymentEventRepository = FakePaymentEventRepository(
                listOf(
                    paymentEvent(
                        id = "event-1",
                        paidAt = Instant.parse("2026-04-01T08:00:00Z"),
                        amountMinor = 50000L,
                    ),
                    paymentEvent(
                        id = "event-2",
                        paidAt = Instant.parse("2026-04-01T09:00:00Z"),
                        amountMinor = 72500L,
                        confidence = ConfidenceLevel.LOW,
                        status = PaymentStatus.CORRECTED,
                    ),
                ),
            ),
            dailyLimitRepository = FakeDailyLimitRepository(null),
            clock = fixedClock(),
        )

        val summary = useCase().first()

        assertEquals(122500L, summary.totalAmountMinor)
        assertEquals(2, summary.paymentsCount)
        assertEquals(72500L, summary.lastPaymentAmountMinor)
        assertFalse(summary.hasLowConfidenceItems)
    }

    @Test
    fun `invoke uses clock zone for day boundaries`() = runTest {
        val useCase = ObserveTodaySummaryUseCase(
            paymentEventRepository = FakePaymentEventRepository(
                listOf(
                    paymentEvent(id = "before-day", paidAt = Instant.parse("2026-03-31T20:59:59Z"), amountMinor = 10000L),
                    paymentEvent(id = "day-start", paidAt = Instant.parse("2026-03-31T21:00:00Z"), amountMinor = 20000L),
                    paymentEvent(id = "day-end", paidAt = Instant.parse("2026-04-01T20:59:59Z"), amountMinor = 30000L),
                    paymentEvent(id = "next-day", paidAt = Instant.parse("2026-04-01T21:00:00Z"), amountMinor = 40000L),
                ),
            ),
            dailyLimitRepository = FakeDailyLimitRepository(null),
            clock = fixedClock(),
        )

        val summary = useCase().first()

        assertEquals(50000L, summary.totalAmountMinor)
        assertEquals(2, summary.paymentsCount)
        assertEquals(30000L, summary.lastPaymentAmountMinor)
    }

    @Test
    fun `invoke adds remaining amount without warning below threshold`() = runTest {
        val useCase = ObserveTodaySummaryUseCase(
            paymentEventRepository = FakePaymentEventRepository(
                listOf(
                    paymentEvent(
                        id = "event-1",
                        paidAt = Instant.parse("2026-04-01T08:00:00Z"),
                        amountMinor = 50000L,
                    ),
                ),
            ),
            dailyLimitRepository = FakeDailyLimitRepository(100000L),
            clock = fixedClock(),
        )

        val summary = useCase().first()

        assertEquals(100000L, summary.limitAmountMinor)
        assertEquals(50000L, summary.remainingAmountMinor)
        assertNull(summary.limitWarningLevel)
    }

    @Test
    fun `invoke marks near limit at 80 percent or more`() = runTest {
        val useCase = ObserveTodaySummaryUseCase(
            paymentEventRepository = FakePaymentEventRepository(
                listOf(
                    paymentEvent(
                        id = "event-1",
                        paidAt = Instant.parse("2026-04-01T08:00:00Z"),
                        amountMinor = 80000L,
                    ),
                ),
            ),
            dailyLimitRepository = FakeDailyLimitRepository(100000L),
            clock = fixedClock(),
        )

        val summary = useCase().first()

        assertEquals(20000L, summary.remainingAmountMinor)
        assertEquals(DailyLimitWarningLevel.NEAR_LIMIT, summary.limitWarningLevel)
    }

    @Test
    fun `invoke marks limit reached and keeps negative remaining amount when over limit`() = runTest {
        val useCase = ObserveTodaySummaryUseCase(
            paymentEventRepository = FakePaymentEventRepository(
                listOf(
                    paymentEvent(
                        id = "event-1",
                        paidAt = Instant.parse("2026-04-01T08:00:00Z"),
                        amountMinor = 125000L,
                    ),
                ),
            ),
            dailyLimitRepository = FakeDailyLimitRepository(100000L),
            clock = fixedClock(),
        )

        val summary = useCase().first()

        assertEquals(-25000L, summary.remainingAmountMinor)
        assertEquals(DailyLimitWarningLevel.LIMIT_REACHED, summary.limitWarningLevel)
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
        duplicateGroupId: String? = null,
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
            duplicateGroupId = duplicateGroupId,
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
            return events.filter { event ->
                event.paidAt >= startInclusive && event.paidAt < endExclusive
            }.sortedByDescending { it.paidAt }
        }

        override fun observePaymentEvents(): Flow<List<PaymentEvent>> {
            return flowOf(events.sortedByDescending { it.paidAt })
        }

        override fun observePaymentEventsBetween(
            startInclusive: Instant,
            endExclusive: Instant,
        ): Flow<List<PaymentEvent>> {
            return flowOf(
                events.filter { event ->
                    event.paidAt >= startInclusive && event.paidAt < endExclusive
                }.sortedByDescending { it.paidAt },
            )
        }
    }

    private class FakeDailyLimitRepository(
        private val initialValue: Long?,
    ) : DailyLimitRepository {
        private val flow = flowOf(initialValue)

        override fun observeLimitAmountMinor(): Flow<Long?> = flow

        override fun getLimitAmountMinor(): Long? = initialValue

        override fun setLimitAmountMinor(amountMinor: Long) = Unit

        override fun clearLimit() = Unit
    }
}
