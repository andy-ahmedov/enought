package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.ConfidenceLevel
import com.andyahmedov.enought.domain.model.ConfidenceHint
import com.andyahmedov.enought.domain.model.PaymentCandidate
import com.andyahmedov.enought.domain.model.PaymentChannel
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentStatus
import com.andyahmedov.enought.domain.model.RawNotificationEvent
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import com.andyahmedov.enought.normalization.PaymentEventNormalizer
import com.andyahmedov.enought.normalization.DefaultPaymentEventNormalizer
import com.andyahmedov.enought.parsing.NotificationParserRegistry
import com.andyahmedov.enought.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProcessIncomingRawEventUseCaseTest {
    @Test
    fun `invoke parses normalizes and saves supported raw event`() = runTest {
        val paymentEventRepository = FakePaymentEventRepository()
        val widgetUpdater = FakeWidgetUpdater()
        val useCase = ProcessIncomingRawEventUseCase(
            parserRegistry = NotificationParserRegistry.default(),
            paymentEventNormalizer = DefaultPaymentEventNormalizer(),
            deduplicatePaymentEventUseCase = DeduplicatePaymentEventUseCase(paymentEventRepository),
            paymentEventRepository = paymentEventRepository,
            widgetUpdater = widgetUpdater,
        )
        val rawEvent = rawNotificationEvent(
            sourcePackage = "ru.nspk.mirpay",
            title = "Mir Pay",
            text = "Покупка 349 ₽",
        )

        useCase(rawEvent)

        val savedEvent = paymentEventRepository.savedEvents.single()
        assertEquals(rawEvent.id, savedEvent.id)
        assertEquals(34900L, savedEvent.amountMinor)
        assertEquals(ConfidenceLevel.HIGH, savedEvent.confidence)
        assertEquals(PaymentStatus.CONFIRMED, savedEvent.status)
        assertEquals(1, widgetUpdater.refreshCalls)
    }

    @Test
    fun `invoke skips allowlisted Alfa-Bank promotional raw event`() = runTest {
        val paymentEventRepository = FakePaymentEventRepository()
        val useCase = ProcessIncomingRawEventUseCase(
            parserRegistry = NotificationParserRegistry.default(),
            paymentEventNormalizer = DefaultPaymentEventNormalizer(),
            deduplicatePaymentEventUseCase = DeduplicatePaymentEventUseCase(paymentEventRepository),
            paymentEventRepository = paymentEventRepository,
            widgetUpdater = FakeWidgetUpdater(),
        )

        useCase(
            rawNotificationEvent(
                sourcePackage = "ru.alfabank.mobile.android",
                title = "Альфа-Банк",
                text = "Откройте счёт для инвестиций и получите портфель акций до 15 000 ₽ за первые сделки",
            ),
        )

        assertTrue(paymentEventRepository.savedEvents.isEmpty())
    }

    @Test
    fun `invoke skips allowlisted Sber raw event while bank parsing is disabled`() = runTest {
        val paymentEventRepository = FakePaymentEventRepository()
        val useCase = ProcessIncomingRawEventUseCase(
            parserRegistry = NotificationParserRegistry.default(),
            paymentEventNormalizer = DefaultPaymentEventNormalizer(),
            deduplicatePaymentEventUseCase = DeduplicatePaymentEventUseCase(paymentEventRepository),
            paymentEventRepository = paymentEventRepository,
            widgetUpdater = FakeWidgetUpdater(),
        )

        useCase(
            rawNotificationEvent(
                sourcePackage = "ru.sberbankmobile",
                title = "СберБанк",
                text = "Оплата 349 ₽",
            ),
        )

        assertTrue(paymentEventRepository.savedEvents.isEmpty())
    }

    @Test
    fun `invoke marks near duplicate pair as suspected conflict instead of auto merging`() = runTest {
        val existingBankEvent = PaymentEvent(
            id = "bank-1",
            amountMinor = 34900L,
            currency = "RUB",
            paidAt = Instant.parse("2026-03-31T10:12:59Z"),
            merchantName = "Coffee Point",
            sourceKind = PaymentSourceKind.BANK,
            paymentChannel = PaymentChannel.UNKNOWN,
            confidence = ConfidenceLevel.MEDIUM,
            status = PaymentStatus.CONFIRMED,
            userEdited = false,
            sourceIds = listOf("bank-1"),
        )
        val paymentEventRepository = FakePaymentEventRepository(initialEvents = listOf(existingBankEvent))
        val widgetUpdater = FakeWidgetUpdater()
        val useCase = ProcessIncomingRawEventUseCase(
            parserRegistry = NotificationParserRegistry.default(),
            paymentEventNormalizer = DefaultPaymentEventNormalizer(),
            deduplicatePaymentEventUseCase = DeduplicatePaymentEventUseCase(paymentEventRepository),
            paymentEventRepository = paymentEventRepository,
            widgetUpdater = widgetUpdater,
        )

        useCase(
            rawNotificationEvent(
                sourcePackage = "ru.nspk.mirpay",
                title = "Mir Pay",
                text = "Покупка 349 ₽",
            ),
        )

        assertEquals(2, paymentEventRepository.savedEvents.size)
        assertEquals(
            listOf(PaymentStatus.SUSPECTED, PaymentStatus.SUSPECTED),
            paymentEventRepository.savedEvents.map { event -> event.status },
        )
        assertEquals(1, paymentEventRepository.savedEvents.map { event -> event.duplicateGroupId }.distinct().size)
        assertEquals(2, paymentEventRepository.currentEvents.size)
        assertEquals(1, widgetUpdater.refreshCalls)
    }

    @Test
    fun `invoke skips unknown raw event`() = runTest {
        val paymentEventRepository = FakePaymentEventRepository()
        val useCase = ProcessIncomingRawEventUseCase(
            parserRegistry = NotificationParserRegistry.default(),
            paymentEventNormalizer = DefaultPaymentEventNormalizer(),
            deduplicatePaymentEventUseCase = DeduplicatePaymentEventUseCase(paymentEventRepository),
            paymentEventRepository = paymentEventRepository,
            widgetUpdater = FakeWidgetUpdater(),
        )

        useCase(
            rawNotificationEvent(
                sourcePackage = "com.example.other",
                title = "Unknown",
                text = "Покупка 349 ₽",
            ),
        )

        assertTrue(paymentEventRepository.savedEvents.isEmpty())
    }

    @Test
    fun `invoke skips candidate that cannot be normalized`() = runTest {
        val paymentEventRepository = FakePaymentEventRepository()
        val useCase = ProcessIncomingRawEventUseCase(
            parserRegistry = NotificationParserRegistry.default(),
            paymentEventNormalizer = NullReturningNormalizer(),
            deduplicatePaymentEventUseCase = DeduplicatePaymentEventUseCase(paymentEventRepository),
            paymentEventRepository = paymentEventRepository,
            widgetUpdater = FakeWidgetUpdater(),
        )

        useCase(
            rawNotificationEvent(
                sourcePackage = "ru.nspk.mirpay",
                title = "Mir Pay",
                text = "Покупка 349 ₽",
            ),
        )

        assertTrue(paymentEventRepository.savedEvents.isEmpty())
    }

    private fun rawNotificationEvent(
        sourcePackage: String,
        title: String?,
        text: String?,
    ): RawNotificationEvent {
        return RawNotificationEvent(
            id = "raw-1",
            sourcePackage = sourcePackage,
            postedAt = Instant.parse("2026-03-31T10:15:30Z"),
            title = title,
            text = text,
            subText = null,
            bigText = "Карта *4242",
            extrasJson = """{"notificationId":"42"}""",
            payloadHash = "hash-1",
        )
    }

    private class FakePaymentEventRepository(
        initialEvents: List<PaymentEvent> = emptyList(),
    ) : PaymentEventRepository {
        val savedEvents = mutableListOf<PaymentEvent>()
        private val eventsById = initialEvents.associateBy { event -> event.id }.toMutableMap()
        val currentEvents: List<PaymentEvent>
            get() = eventsById.values.sortedByDescending { event -> event.paidAt }
        private val eventsFlow = MutableStateFlow(currentEvents)

        override suspend fun save(event: PaymentEvent) {
            savedEvents += event
            eventsById[event.id] = event
            eventsFlow.value = currentEvents
        }

        override suspend fun saveAll(events: List<PaymentEvent>) {
            events.forEach { event -> save(event) }
        }

        override suspend fun getById(id: String): PaymentEvent? {
            return eventsById[id]
        }

        override suspend fun getByDuplicateGroupId(duplicateGroupId: String): List<PaymentEvent> {
            return currentEvents.filter { event -> event.duplicateGroupId == duplicateGroupId }
        }

        override suspend fun getPaymentEventsBetween(
            startInclusive: Instant,
            endExclusive: Instant,
        ): List<PaymentEvent> {
            return currentEvents.filter { event ->
                event.paidAt >= startInclusive && event.paidAt < endExclusive
            }
        }

        override fun observePaymentEvents(): Flow<List<PaymentEvent>> = eventsFlow

        override fun observePaymentEventsBetween(
            startInclusive: Instant,
            endExclusive: Instant,
        ): Flow<List<PaymentEvent>> = eventsFlow
    }

    private class NullReturningNormalizer : PaymentEventNormalizer {
        override fun normalize(candidate: PaymentCandidate): PaymentEvent? {
            return null
        }
    }

    private class FakeWidgetUpdater : WidgetUpdater {
        var refreshCalls: Int = 0

        override suspend fun refresh() {
            refreshCalls += 1
        }
    }
}
