package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.ConfidenceLevel
import com.andyahmedov.enought.domain.model.PaymentChannel
import com.andyahmedov.enought.domain.model.PaymentEditType
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentEventEdit
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.PaymentStatus
import com.andyahmedov.enought.domain.model.RawNotificationEvent
import com.andyahmedov.enought.domain.repository.PaymentEventEditRepository
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import com.andyahmedov.enought.domain.repository.RawNotificationEventRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildDiagnosticsReportUseCaseTest {
    @Test
    fun `report includes raw notifications payment snapshot and manual edits`() = runTest {
        val useCase = BuildDiagnosticsReportUseCase(
            rawNotificationEventRepository = FakeRawNotificationEventRepository(
                rawEvents = listOf(
                    rawEvent(
                        id = "raw-mir-1",
                        sourcePackage = "ru.nspk.mirpay",
                        payloadHash = "hash-1",
                        text = "Сумма 523 ₽",
                    ),
                    rawEvent(
                        id = "raw-bank-1",
                        sourcePackage = "ru.sberbankmobile",
                        payloadHash = "hash-2",
                        text = "Оплата 523 ₽",
                    ),
                ),
            ),
            paymentEventRepository = FakePaymentEventRepository(
                events = listOf(
                    paymentEvent(
                        id = "raw-mir-1",
                        sourceIds = listOf("raw-mir-1"),
                        status = PaymentStatus.CONFIRMED,
                        userEdited = true,
                    ),
                ),
            ),
            paymentEventEditRepository = FakePaymentEventEditRepository(
                edits = listOf(
                    paymentEdit(paymentEventId = "raw-mir-1"),
                ),
            ),
            clock = Clock.fixed(
                Instant.parse("2026-04-05T15:00:00Z"),
                ZoneId.of("Europe/Moscow"),
            ),
        )

        val report = useCase()

        assertTrue(report.contains("raw notifications: 2"))
        assertTrue(report.contains("payment events: 1"))
        assertTrue(report.contains("edited payment events: 1"))
        assertTrue(report.contains("source_package=ru.sberbankmobile"))
        assertTrue(report.contains("processing=linked_payment_events=raw-mir-1(CONFIRMED/MIR_PAY)"))
        assertTrue(report.contains("processing=not_promoted_to_payment_event"))
        assertTrue(report.contains("Manual edits:"))
        assertTrue(report.contains("payment_event_id=raw-mir-1"))
    }

    @Test
    fun `report keeps empty sections explicit`() = runTest {
        val useCase = BuildDiagnosticsReportUseCase(
            rawNotificationEventRepository = FakeRawNotificationEventRepository(emptyList()),
            paymentEventRepository = FakePaymentEventRepository(emptyList()),
            paymentEventEditRepository = FakePaymentEventEditRepository(emptyList()),
            clock = Clock.fixed(
                Instant.parse("2026-04-05T15:00:00Z"),
                ZoneId.of("Europe/Moscow"),
            ),
        )

        val report = useCase()

        assertTrue(report.contains("Raw notifications:\n- none"))
        assertTrue(report.contains("Payment events:\n- none"))
        assertTrue(report.contains("Manual edits:\n- none"))
    }

    private fun rawEvent(
        id: String,
        sourcePackage: String,
        payloadHash: String,
        text: String,
    ): RawNotificationEvent {
        return RawNotificationEvent(
            id = id,
            sourcePackage = sourcePackage,
            postedAt = Instant.parse("2026-04-05T12:30:00Z"),
            title = "Title",
            text = text,
            subText = null,
            bigText = "Big text",
            extrasJson = "{\"id\":\"$id\"}",
            payloadHash = payloadHash,
        )
    }

    private fun paymentEvent(
        id: String,
        sourceIds: List<String>,
        status: PaymentStatus,
        userEdited: Boolean,
    ): PaymentEvent {
        return PaymentEvent(
            id = id,
            amountMinor = 52300L,
            currency = "RUB",
            paidAt = Instant.parse("2026-04-05T12:30:00Z"),
            merchantName = null,
            sourceKind = PaymentSourceKind.MIR_PAY,
            paymentChannel = PaymentChannel.PHONE,
            confidence = ConfidenceLevel.HIGH,
            status = status,
            userEdited = userEdited,
            sourceIds = sourceIds,
        )
    }

    private fun paymentEdit(paymentEventId: String): PaymentEventEdit {
        return PaymentEventEdit(
            id = "edit-1",
            paymentEventId = paymentEventId,
            editedAt = Instant.parse("2026-04-05T13:00:00Z"),
            editType = PaymentEditType.CONFIRM,
            previousStatus = PaymentStatus.SUSPECTED,
            newStatus = PaymentStatus.CONFIRMED,
            previousAmountMinor = 52300L,
            newAmountMinor = 52300L,
        )
    }

    private class FakeRawNotificationEventRepository(
        private val rawEvents: List<RawNotificationEvent>,
    ) : RawNotificationEventRepository {
        override suspend fun saveIfNew(event: RawNotificationEvent): Boolean = true

        override suspend fun getRawEventsBetween(
            startInclusive: Instant,
            endExclusive: Instant,
        ): List<RawNotificationEvent> {
            return rawEvents.filter { event ->
                event.postedAt >= startInclusive && event.postedAt < endExclusive
            }.sortedByDescending { event -> event.postedAt }
        }

        override fun observeRawEvents(): Flow<List<RawNotificationEvent>> = emptyFlow()
    }

    private class FakePaymentEventRepository(
        private val events: List<PaymentEvent>,
    ) : PaymentEventRepository {
        override suspend fun save(event: PaymentEvent) = Unit

        override suspend fun saveAll(events: List<PaymentEvent>) = Unit

        override suspend fun getById(id: String): PaymentEvent? = events.firstOrNull { event -> event.id == id }

        override suspend fun getByDuplicateGroupId(duplicateGroupId: String): List<PaymentEvent> {
            return events.filter { event -> event.duplicateGroupId == duplicateGroupId }
        }

        override suspend fun getPaymentEventsBetween(
            startInclusive: Instant,
            endExclusive: Instant,
        ): List<PaymentEvent> {
            return events.filter { event ->
                event.paidAt >= startInclusive && event.paidAt < endExclusive
            }.sortedByDescending { event -> event.paidAt }
        }

        override fun observePaymentEvents(): Flow<List<PaymentEvent>> = emptyFlow()

        override fun observePaymentEventsBetween(
            startInclusive: Instant,
            endExclusive: Instant,
        ): Flow<List<PaymentEvent>> = emptyFlow()
    }

    private class FakePaymentEventEditRepository(
        private val edits: List<PaymentEventEdit>,
    ) : PaymentEventEditRepository {
        override suspend fun save(edit: PaymentEventEdit) = Unit

        override suspend fun getByPaymentEventIds(paymentEventIds: List<String>): List<PaymentEventEdit> {
            return edits.filter { edit -> edit.paymentEventId in paymentEventIds }
                .sortedByDescending { edit -> edit.editedAt }
        }
    }
}
