package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.ConfidenceLevel
import com.andyahmedov.enought.domain.model.PaymentChannel
import com.andyahmedov.enought.domain.model.PaymentEditType
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentEventEdit
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.PaymentStatus
import com.andyahmedov.enought.domain.repository.PaymentEventEditRepository
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import com.andyahmedov.enought.widget.WidgetUpdater
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuplicateConflictResolutionUseCasesTest {
    @Test
    fun `merge duplicate conflict creates one hybrid event and dismisses sibling`() = runTest {
        val paymentEventRepository = FakePaymentEventRepository(
            listOf(
                paymentEvent(
                    id = "bank-1",
                    sourceKind = PaymentSourceKind.BANK,
                    paymentChannel = PaymentChannel.UNKNOWN,
                    paidAt = Instant.parse("2026-04-01T10:15:00Z"),
                    duplicateGroupId = "dup-1",
                ),
                paymentEvent(
                    id = "mir-1",
                    sourceKind = PaymentSourceKind.MIR_PAY,
                    paymentChannel = PaymentChannel.PHONE,
                    paidAt = Instant.parse("2026-04-01T10:16:00Z"),
                    duplicateGroupId = "dup-1",
                ),
            ),
        )
        val editRepository = FakePaymentEventEditRepository()
        val widgetUpdater = FakeWidgetUpdater()
        val useCase = MergeDuplicateConflictUseCase(
            paymentEventRepository = paymentEventRepository,
            paymentEventEditRepository = editRepository,
            widgetUpdater = widgetUpdater,
            clock = fixedClock(),
        )

        val result = useCase("dup-1")

        assertTrue(result)
        val mergedEvent = paymentEventRepository.eventsById.getValue("bank-1")
        val dismissedEvent = paymentEventRepository.eventsById.getValue("mir-1")
        assertEquals(PaymentSourceKind.HYBRID, mergedEvent.sourceKind)
        assertEquals(PaymentStatus.CONFIRMED, mergedEvent.status)
        assertTrue(mergedEvent.userEdited)
        assertEquals(listOf("bank-1", "mir-1"), mergedEvent.sourceIds)
        assertEquals(null, mergedEvent.duplicateGroupId)
        assertEquals(PaymentStatus.DISMISSED, dismissedEvent.status)
        assertEquals(null, dismissedEvent.duplicateGroupId)
        assertEquals(
            listOf(PaymentEditType.MERGE_DUPLICATE, PaymentEditType.MERGE_DUPLICATE),
            editRepository.savedEdits.map { edit -> edit.editType },
        )
        assertEquals(1, widgetUpdater.refreshCalls)
    }

    @Test
    fun `keep duplicate conflict separate confirms both events and clears group`() = runTest {
        val paymentEventRepository = FakePaymentEventRepository(
            listOf(
                paymentEvent(
                    id = "bank-1",
                    sourceKind = PaymentSourceKind.BANK,
                    paidAt = Instant.parse("2026-04-01T10:15:00Z"),
                    duplicateGroupId = "dup-1",
                ),
                paymentEvent(
                    id = "mir-1",
                    sourceKind = PaymentSourceKind.MIR_PAY,
                    paymentChannel = PaymentChannel.PHONE,
                    paidAt = Instant.parse("2026-04-01T10:16:00Z"),
                    duplicateGroupId = "dup-1",
                ),
            ),
        )
        val editRepository = FakePaymentEventEditRepository()
        val widgetUpdater = FakeWidgetUpdater()
        val useCase = KeepDuplicateConflictSeparateUseCase(
            paymentEventRepository = paymentEventRepository,
            paymentEventEditRepository = editRepository,
            widgetUpdater = widgetUpdater,
            clock = fixedClock(),
        )

        val result = useCase("dup-1")

        assertTrue(result)
        assertEquals(PaymentStatus.CONFIRMED, paymentEventRepository.eventsById.getValue("bank-1").status)
        assertEquals(PaymentStatus.CONFIRMED, paymentEventRepository.eventsById.getValue("mir-1").status)
        assertEquals(null, paymentEventRepository.eventsById.getValue("bank-1").duplicateGroupId)
        assertEquals(null, paymentEventRepository.eventsById.getValue("mir-1").duplicateGroupId)
        assertEquals(
            listOf(PaymentEditType.KEEP_DUPLICATES_SEPARATE, PaymentEditType.KEEP_DUPLICATES_SEPARATE),
            editRepository.savedEdits.map { edit -> edit.editType },
        )
        assertEquals(1, widgetUpdater.refreshCalls)
    }

    private fun fixedClock(): Clock {
        return Clock.fixed(
            Instant.parse("2026-04-01T12:00:00Z"),
            ZoneId.of("Europe/Moscow"),
        )
    }

    private fun paymentEvent(
        id: String,
        sourceKind: PaymentSourceKind,
        paidAt: Instant,
        paymentChannel: PaymentChannel = PaymentChannel.UNKNOWN,
        duplicateGroupId: String,
    ): PaymentEvent {
        return PaymentEvent(
            id = id,
            amountMinor = 34900L,
            currency = "RUB",
            paidAt = paidAt,
            merchantName = "Coffee Point",
            sourceKind = sourceKind,
            paymentChannel = paymentChannel,
            confidence = ConfidenceLevel.MEDIUM,
            status = PaymentStatus.SUSPECTED,
            userEdited = false,
            sourceIds = listOf(id),
            duplicateGroupId = duplicateGroupId,
        )
    }

    private class FakePaymentEventRepository(
        events: List<PaymentEvent>,
    ) : PaymentEventRepository {
        val eventsById = events.associateBy { event -> event.id }.toMutableMap()
        private val eventsFlow = MutableStateFlow(events.sortedByDescending { event -> event.paidAt })

        override suspend fun save(event: PaymentEvent) {
            eventsById[event.id] = event
            eventsFlow.value = eventsById.values.sortedByDescending { savedEvent -> savedEvent.paidAt }
        }

        override suspend fun saveAll(events: List<PaymentEvent>) {
            events.forEach { event -> save(event) }
        }

        override suspend fun getById(id: String): PaymentEvent? = eventsById[id]

        override suspend fun getByDuplicateGroupId(duplicateGroupId: String): List<PaymentEvent> {
            return eventsById.values
                .filter { event -> event.duplicateGroupId == duplicateGroupId }
                .sortedByDescending { event -> event.paidAt }
        }

        override suspend fun getPaymentEventsBetween(
            startInclusive: Instant,
            endExclusive: Instant,
        ): List<PaymentEvent> {
            return eventsById.values.filter { event ->
                event.paidAt >= startInclusive && event.paidAt < endExclusive
            }.sortedByDescending { it.paidAt }
        }

        override fun observePaymentEvents(): Flow<List<PaymentEvent>> = eventsFlow

        override fun observePaymentEventsBetween(
            startInclusive: Instant,
            endExclusive: Instant,
        ): Flow<List<PaymentEvent>> = eventsFlow
    }

    private class FakePaymentEventEditRepository : PaymentEventEditRepository {
        val savedEdits = mutableListOf<PaymentEventEdit>()

        override suspend fun save(edit: PaymentEventEdit) {
            savedEdits += edit
        }
    }

    private class FakeWidgetUpdater : WidgetUpdater {
        var refreshCalls: Int = 0

        override suspend fun refresh() {
            refreshCalls += 1
        }
    }
}
