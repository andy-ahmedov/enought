package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.ConfidenceLevel
import com.andyahmedov.enought.domain.model.PaymentChannel
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.PaymentStatus
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeduplicatePaymentEventUseCaseTest {
    @Test
    fun `invoke merges bank and Mir Pay events into one hybrid event`() = runTest {
        val existingBankEvent = paymentEvent(
            id = "bank-1",
            sourceKind = PaymentSourceKind.BANK,
            paymentChannel = PaymentChannel.UNKNOWN,
            confidence = ConfidenceLevel.MEDIUM,
            paidAt = Instant.parse("2026-03-31T10:15:00Z"),
            merchantName = "Coffee Point",
        )
        val incomingMirPayEvent = paymentEvent(
            id = "mir-1",
            sourceKind = PaymentSourceKind.MIR_PAY,
            paymentChannel = PaymentChannel.PHONE,
            confidence = ConfidenceLevel.HIGH,
            paidAt = Instant.parse("2026-03-31T10:16:00Z"),
            merchantName = null,
        )
        val repository = FakePaymentEventRepository(listOf(existingBankEvent))

        val result = DeduplicatePaymentEventUseCase(repository).invoke(incomingMirPayEvent)
        assertTrue(result is DeduplicatePaymentEventResult.AutoMerge)
        val mergedEvent = (result as DeduplicatePaymentEventResult.AutoMerge).mergedEvent

        assertEquals("bank-1", mergedEvent.id)
        assertEquals(PaymentSourceKind.HYBRID, mergedEvent.sourceKind)
        assertEquals(PaymentChannel.PHONE, mergedEvent.paymentChannel)
        assertEquals(ConfidenceLevel.HIGH, mergedEvent.confidence)
        assertEquals(PaymentStatus.CONFIRMED, mergedEvent.status)
        assertEquals(Instant.parse("2026-03-31T10:16:00Z"), mergedEvent.paidAt)
        assertEquals("Coffee Point", mergedEvent.merchantName)
        assertEquals(listOf("bank-1", "mir-1"), mergedEvent.sourceIds)
        assertEquals(Instant.parse("2026-03-31T10:11:00Z"), repository.lastStartInclusive)
        assertEquals(Instant.parse("2026-03-31T10:21:00.000000001Z"), repository.lastEndExclusive)
    }

    @Test
    fun `invoke keeps incoming event when matching event is outside window`() = runTest {
        val repository = FakePaymentEventRepository(
            listOf(
                paymentEvent(
                    id = "bank-1",
                    sourceKind = PaymentSourceKind.BANK,
                    paidAt = Instant.parse("2026-03-31T10:12:59Z"),
                ),
            ),
        )
        val incomingMirPayEvent = paymentEvent(
            id = "mir-1",
            sourceKind = PaymentSourceKind.MIR_PAY,
            paymentChannel = PaymentChannel.PHONE,
            confidence = ConfidenceLevel.HIGH,
            paidAt = Instant.parse("2026-03-31T10:15:00Z"),
        )

        val result = DeduplicatePaymentEventUseCase(repository).invoke(incomingMirPayEvent)

        assertEquals(
            DeduplicatePaymentEventResult.Conflict(
                conflictedEvents = listOf(
                    paymentEvent(
                        id = "bank-1",
                        sourceKind = PaymentSourceKind.BANK,
                        confidence = ConfidenceLevel.MEDIUM,
                        status = PaymentStatus.SUSPECTED,
                        paidAt = Instant.parse("2026-03-31T10:12:59Z"),
                        duplicateGroupId = "ignored",
                    ),
                    paymentEvent(
                        id = "mir-1",
                        sourceKind = PaymentSourceKind.MIR_PAY,
                        paymentChannel = PaymentChannel.PHONE,
                        confidence = ConfidenceLevel.MEDIUM,
                        status = PaymentStatus.SUSPECTED,
                        paidAt = Instant.parse("2026-03-31T10:15:00Z"),
                        duplicateGroupId = "ignored",
                    ),
                ),
            ).copyConflictGroupIdsFrom(result),
            result,
        )
    }

    @Test
    fun `invoke keeps incoming event when source kinds are not mergeable`() = runTest {
        val repository = FakePaymentEventRepository(
            listOf(
                paymentEvent(
                    id = "bank-1",
                    sourceKind = PaymentSourceKind.BANK,
                    paidAt = Instant.parse("2026-03-31T10:15:00Z"),
                ),
            ),
        )
        val incomingBankEvent = paymentEvent(
            id = "bank-2",
            sourceKind = PaymentSourceKind.BANK,
            paidAt = Instant.parse("2026-03-31T10:15:30Z"),
        )

        val result = DeduplicatePaymentEventUseCase(repository).invoke(incomingBankEvent)

        assertEquals(DeduplicatePaymentEventResult.NoMatch(incomingBankEvent), result)
    }

    @Test
    fun `invoke keeps incoming event when one side has low confidence`() = runTest {
        val repository = FakePaymentEventRepository(
            listOf(
                paymentEvent(
                    id = "bank-1",
                    sourceKind = PaymentSourceKind.BANK,
                    confidence = ConfidenceLevel.LOW,
                    status = PaymentStatus.SUSPECTED,
                    paidAt = Instant.parse("2026-03-31T10:15:00Z"),
                ),
            ),
        )
        val incomingMirPayEvent = paymentEvent(
            id = "mir-1",
            sourceKind = PaymentSourceKind.MIR_PAY,
            paymentChannel = PaymentChannel.PHONE,
            confidence = ConfidenceLevel.HIGH,
            paidAt = Instant.parse("2026-03-31T10:15:30Z"),
        )

        val result = DeduplicatePaymentEventUseCase(repository).invoke(incomingMirPayEvent)

        assertEquals(DeduplicatePaymentEventResult.NoMatch(incomingMirPayEvent), result)
    }

    @Test
    fun `invoke keeps incoming event when existing event was user edited`() = runTest {
        val repository = FakePaymentEventRepository(
            listOf(
                paymentEvent(
                    id = "bank-1",
                    sourceKind = PaymentSourceKind.BANK,
                    paidAt = Instant.parse("2026-03-31T10:15:00Z"),
                    userEdited = true,
                ),
            ),
        )
        val incomingMirPayEvent = paymentEvent(
            id = "mir-1",
            sourceKind = PaymentSourceKind.MIR_PAY,
            paymentChannel = PaymentChannel.PHONE,
            confidence = ConfidenceLevel.HIGH,
            paidAt = Instant.parse("2026-03-31T10:15:30Z"),
        )

        val result = DeduplicatePaymentEventUseCase(repository).invoke(incomingMirPayEvent)

        assertEquals(DeduplicatePaymentEventResult.NoMatch(incomingMirPayEvent), result)
    }

    @Test
    fun `invoke keeps incoming event when existing event was corrected or dismissed`() = runTest {
        val correctedRepository = FakePaymentEventRepository(
            listOf(
                paymentEvent(
                    id = "bank-1",
                    sourceKind = PaymentSourceKind.BANK,
                    paidAt = Instant.parse("2026-03-31T10:15:00Z"),
                    status = PaymentStatus.CORRECTED,
                ),
            ),
        )
        val dismissedRepository = FakePaymentEventRepository(
            listOf(
                paymentEvent(
                    id = "bank-2",
                    sourceKind = PaymentSourceKind.BANK,
                    paidAt = Instant.parse("2026-03-31T10:15:00Z"),
                    status = PaymentStatus.DISMISSED,
                ),
            ),
        )
        val incomingMirPayEvent = paymentEvent(
            id = "mir-1",
            sourceKind = PaymentSourceKind.MIR_PAY,
            paymentChannel = PaymentChannel.PHONE,
            confidence = ConfidenceLevel.HIGH,
            paidAt = Instant.parse("2026-03-31T10:15:30Z"),
        )

        assertEquals(DeduplicatePaymentEventResult.NoMatch(incomingMirPayEvent), DeduplicatePaymentEventUseCase(correctedRepository).invoke(incomingMirPayEvent))
        assertEquals(DeduplicatePaymentEventResult.NoMatch(incomingMirPayEvent), DeduplicatePaymentEventUseCase(dismissedRepository).invoke(incomingMirPayEvent))
    }

    private fun paymentEvent(
        id: String,
        sourceKind: PaymentSourceKind,
        paymentChannel: PaymentChannel = PaymentChannel.UNKNOWN,
        confidence: ConfidenceLevel = ConfidenceLevel.MEDIUM,
        status: PaymentStatus = PaymentStatus.CONFIRMED,
        paidAt: Instant,
        merchantName: String? = null,
        userEdited: Boolean = false,
        duplicateGroupId: String? = null,
    ): PaymentEvent {
        return PaymentEvent(
            id = id,
            amountMinor = 34900L,
            currency = "RUB",
            paidAt = paidAt,
            merchantName = merchantName,
            sourceKind = sourceKind,
            paymentChannel = paymentChannel,
            confidence = confidence,
            status = status,
            userEdited = userEdited,
            sourceIds = listOf(id),
            duplicateGroupId = duplicateGroupId,
        )
    }

    private class FakePaymentEventRepository(
        private val events: List<PaymentEvent>,
    ) : PaymentEventRepository {
        var lastStartInclusive: Instant? = null
        var lastEndExclusive: Instant? = null

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
            lastStartInclusive = startInclusive
            lastEndExclusive = endExclusive

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

    private fun DeduplicatePaymentEventResult.Conflict.copyConflictGroupIdsFrom(
        actual: DeduplicatePaymentEventResult,
    ): DeduplicatePaymentEventResult.Conflict {
        val actualConflict = actual as DeduplicatePaymentEventResult.Conflict
        val actualGroupId = actualConflict.conflictedEvents.first().duplicateGroupId

        return copy(
            conflictedEvents = conflictedEvents.map { event ->
                event.copy(duplicateGroupId = actualGroupId)
            },
        )
    }
}
