package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.ConfidenceLevel
import com.andyahmedov.enought.domain.model.PaymentChannel
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.PaymentStatus
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class ObserveTodayPaymentEventsUseCaseTest {
    @Test
    fun `invoke returns confirmed and corrected rub events for current local day`() = runTest {
        val useCase = ObserveTodayPaymentEventsUseCase(
            paymentEventRepository = FakePaymentEventRepository(
                listOf(
                    paymentEvent(id = "before-day", paidAt = Instant.parse("2026-03-31T20:59:59Z")),
                    paymentEvent(id = "confirmed-rub", paidAt = Instant.parse("2026-04-01T08:00:00Z")),
                    paymentEvent(
                        id = "corrected-rub",
                        paidAt = Instant.parse("2026-04-01T08:30:00Z"),
                        status = PaymentStatus.CORRECTED,
                    ),
                    paymentEvent(
                        id = "suspected-rub",
                        paidAt = Instant.parse("2026-04-01T09:00:00Z"),
                        status = PaymentStatus.SUSPECTED,
                    ),
                    paymentEvent(
                        id = "confirmed-usd",
                        paidAt = Instant.parse("2026-04-01T10:00:00Z"),
                        currency = "USD",
                    ),
                    paymentEvent(id = "latest-confirmed-rub", paidAt = Instant.parse("2026-04-01T11:00:00Z")),
                    paymentEvent(id = "next-day", paidAt = Instant.parse("2026-04-01T21:00:00Z")),
                ),
            ),
            clock = fixedClock(),
        )

        val events = useCase().first()

        assertEquals(listOf("latest-confirmed-rub", "corrected-rub", "confirmed-rub"), events.map { it.id })
    }

    @Test
    fun `invoke keeps paidAt descending order from repository`() = runTest {
        val useCase = ObserveTodayPaymentEventsUseCase(
            paymentEventRepository = FakePaymentEventRepository(
                listOf(
                    paymentEvent(id = "early", paidAt = Instant.parse("2026-04-01T08:00:00Z")),
                    paymentEvent(id = "late", paidAt = Instant.parse("2026-04-01T18:00:00Z")),
                ),
            ),
            clock = fixedClock(),
        )

        val events = useCase().first()

        assertEquals(listOf("late", "early"), events.map { it.id })
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
        currency: String = "RUB",
        status: PaymentStatus = PaymentStatus.CONFIRMED,
    ): PaymentEvent {
        return PaymentEvent(
            id = id,
            amountMinor = 10000L,
            currency = currency,
            paidAt = paidAt,
            merchantName = null,
            sourceKind = PaymentSourceKind.BANK,
            paymentChannel = PaymentChannel.UNKNOWN,
            confidence = ConfidenceLevel.MEDIUM,
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
}
