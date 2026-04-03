package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.ConfidenceLevel
import com.andyahmedov.enought.domain.model.PaymentChannel
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.PaymentStatus
import com.andyahmedov.enought.domain.model.TodayReviewItem
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ObserveTodayReviewItemsUseCaseTest {
    @Test
    fun `invoke returns only suspected rub events for current local day`() = runTest {
        val useCase = ObserveTodayReviewItemsUseCase(
            paymentEventRepository = FakePaymentEventRepository(
                listOf(
                    paymentEvent(id = "before-day", paidAt = Instant.parse("2026-03-31T20:59:59Z"), status = PaymentStatus.SUSPECTED),
                    paymentEvent(id = "suspected-rub", paidAt = Instant.parse("2026-04-01T08:00:00Z"), status = PaymentStatus.SUSPECTED),
                    paymentEvent(id = "confirmed-rub", paidAt = Instant.parse("2026-04-01T09:00:00Z"), status = PaymentStatus.CONFIRMED),
                    paymentEvent(id = "corrected-rub", paidAt = Instant.parse("2026-04-01T10:00:00Z"), status = PaymentStatus.CORRECTED),
                    paymentEvent(id = "dismissed-rub", paidAt = Instant.parse("2026-04-01T11:00:00Z"), status = PaymentStatus.DISMISSED),
                    paymentEvent(id = "suspected-usd", paidAt = Instant.parse("2026-04-01T12:00:00Z"), currency = "USD", status = PaymentStatus.SUSPECTED),
                    paymentEvent(id = "latest-suspected-rub", paidAt = Instant.parse("2026-04-01T18:00:00Z"), status = PaymentStatus.SUSPECTED),
                ),
            ),
            clock = fixedClock(),
        )

        val events = useCase().first()

        assertEquals(
            listOf("latest-suspected-rub", "suspected-rub"),
            events.filterIsInstance<TodayReviewItem.Single>().map { item -> item.id },
        )
    }

    @Test
    fun `invoke groups duplicate conflicts into one pair item`() = runTest {
        val useCase = ObserveTodayReviewItemsUseCase(
            paymentEventRepository = FakePaymentEventRepository(
                listOf(
                    paymentEvent(
                        id = "bank-1",
                        paidAt = Instant.parse("2026-04-01T08:00:00Z"),
                        status = PaymentStatus.SUSPECTED,
                        sourceKind = PaymentSourceKind.BANK,
                        duplicateGroupId = "dup-1",
                    ),
                    paymentEvent(
                        id = "mir-1",
                        paidAt = Instant.parse("2026-04-01T08:02:00Z"),
                        status = PaymentStatus.SUSPECTED,
                        sourceKind = PaymentSourceKind.MIR_PAY,
                        duplicateGroupId = "dup-1",
                    ),
                ),
            ),
            clock = fixedClock(),
        )

        val items = useCase().first()

        assertEquals(1, items.size)
        val duplicateItem = items.single() as TodayReviewItem.DuplicateConflict
        assertEquals("dup-1", duplicateItem.duplicateGroupId)
        assertEquals(10000L, duplicateItem.amountMinor)
        assertEquals(listOf("mir-1", "bank-1"), duplicateItem.items.map { item -> item.id })
        assertTrue(duplicateItem.items.all { item -> item.sourceKind in setOf(PaymentSourceKind.MIR_PAY, PaymentSourceKind.BANK) })
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
        status: PaymentStatus,
        sourceKind: PaymentSourceKind = PaymentSourceKind.BANK,
        duplicateGroupId: String? = null,
    ): PaymentEvent {
        return PaymentEvent(
            id = id,
            amountMinor = 10000L,
            currency = currency,
            paidAt = paidAt,
            merchantName = null,
            sourceKind = sourceKind,
            paymentChannel = PaymentChannel.UNKNOWN,
            confidence = ConfidenceLevel.LOW,
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
}
