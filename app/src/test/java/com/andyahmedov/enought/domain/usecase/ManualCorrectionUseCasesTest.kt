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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualCorrectionUseCasesTest {
    @Test
    fun `confirm updates event saves audit trail and refreshes widget`() = runTest {
        val paymentEventRepository = FakePaymentEventRepository(
            listOf(paymentEvent(id = "event-1", status = PaymentStatus.SUSPECTED)),
        )
        val editRepository = FakePaymentEventEditRepository()
        val widgetUpdater = FakeWidgetUpdater()
        val useCase = ConfirmPaymentEventUseCase(
            paymentEventRepository = paymentEventRepository,
            paymentEventEditRepository = editRepository,
            widgetUpdater = widgetUpdater,
            clock = fixedClock(),
        )

        val result = useCase("event-1")

        assertTrue(result)
        assertEquals(PaymentStatus.CONFIRMED, paymentEventRepository.eventsById.getValue("event-1").status)
        assertTrue(paymentEventRepository.eventsById.getValue("event-1").userEdited)
        assertEquals(PaymentEditType.CONFIRM, editRepository.savedEdits.single().editType)
        assertEquals(1, widgetUpdater.refreshCalls)
    }

    @Test
    fun `correct amount updates amount marks corrected and saves audit trail`() = runTest {
        val paymentEventRepository = FakePaymentEventRepository(
            listOf(paymentEvent(id = "event-1", amountMinor = 34900L, status = PaymentStatus.SUSPECTED)),
        )
        val editRepository = FakePaymentEventEditRepository()
        val widgetUpdater = FakeWidgetUpdater()
        val useCase = CorrectPaymentAmountUseCase(
            paymentEventRepository = paymentEventRepository,
            paymentEventEditRepository = editRepository,
            widgetUpdater = widgetUpdater,
            clock = fixedClock(),
        )

        val result = useCase(
            eventId = "event-1",
            correctedAmountMinor = 39900L,
        )

        assertTrue(result)
        assertEquals(39900L, paymentEventRepository.eventsById.getValue("event-1").amountMinor)
        assertEquals(PaymentStatus.CORRECTED, paymentEventRepository.eventsById.getValue("event-1").status)
        assertTrue(paymentEventRepository.eventsById.getValue("event-1").userEdited)
        assertEquals(PaymentEditType.CORRECT_AMOUNT, editRepository.savedEdits.single().editType)
        assertEquals(34900L, editRepository.savedEdits.single().previousAmountMinor)
        assertEquals(39900L, editRepository.savedEdits.single().newAmountMinor)
        assertEquals(1, widgetUpdater.refreshCalls)
    }

    @Test
    fun `dismiss marks event dismissed and saves audit trail`() = runTest {
        val paymentEventRepository = FakePaymentEventRepository(
            listOf(paymentEvent(id = "event-1", status = PaymentStatus.SUSPECTED)),
        )
        val editRepository = FakePaymentEventEditRepository()
        val widgetUpdater = FakeWidgetUpdater()
        val useCase = DismissPaymentEventUseCase(
            paymentEventRepository = paymentEventRepository,
            paymentEventEditRepository = editRepository,
            widgetUpdater = widgetUpdater,
            clock = fixedClock(),
        )

        val result = useCase("event-1")

        assertTrue(result)
        assertEquals(PaymentStatus.DISMISSED, paymentEventRepository.eventsById.getValue("event-1").status)
        assertTrue(paymentEventRepository.eventsById.getValue("event-1").userEdited)
        assertEquals(PaymentEditType.DISMISS, editRepository.savedEdits.single().editType)
        assertEquals(null, editRepository.savedEdits.single().newAmountMinor)
        assertEquals(1, widgetUpdater.refreshCalls)
    }

    @Test
    fun `manual correction returns false for already resolved event`() = runTest {
        val paymentEventRepository = FakePaymentEventRepository(
            listOf(paymentEvent(id = "event-1", status = PaymentStatus.CONFIRMED)),
        )
        val editRepository = FakePaymentEventEditRepository()
        val widgetUpdater = FakeWidgetUpdater()
        val useCase = ConfirmPaymentEventUseCase(
            paymentEventRepository = paymentEventRepository,
            paymentEventEditRepository = editRepository,
            widgetUpdater = widgetUpdater,
            clock = fixedClock(),
        )

        val result = useCase("event-1")

        assertFalse(result)
        assertTrue(editRepository.savedEdits.isEmpty())
        assertEquals(0, widgetUpdater.refreshCalls)
    }

    private fun fixedClock(): Clock {
        return Clock.fixed(
            Instant.parse("2026-04-01T12:00:00Z"),
            ZoneId.of("Europe/Moscow"),
        )
    }

    private fun paymentEvent(
        id: String,
        amountMinor: Long = 34900L,
        status: PaymentStatus,
    ): PaymentEvent {
        return PaymentEvent(
            id = id,
            amountMinor = amountMinor,
            currency = "RUB",
            paidAt = Instant.parse("2026-04-01T10:00:00Z"),
            merchantName = "Coffee Point",
            sourceKind = PaymentSourceKind.BANK,
            paymentChannel = PaymentChannel.UNKNOWN,
            confidence = ConfidenceLevel.LOW,
            status = status,
            userEdited = false,
            sourceIds = listOf("raw-$id"),
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

        override suspend fun getById(id: String): PaymentEvent? {
            return eventsById[id]
        }

        override suspend fun getByDuplicateGroupId(duplicateGroupId: String): List<PaymentEvent> {
            return eventsById.values.filter { event -> event.duplicateGroupId == duplicateGroupId }
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

        override suspend fun getByPaymentEventIds(paymentEventIds: List<String>): List<PaymentEventEdit> {
            return savedEdits.filter { edit -> edit.paymentEventId in paymentEventIds }
        }
    }

    private class FakeWidgetUpdater : WidgetUpdater {
        var refreshCalls: Int = 0

        override suspend fun refresh() {
            refreshCalls += 1
        }
    }
}
