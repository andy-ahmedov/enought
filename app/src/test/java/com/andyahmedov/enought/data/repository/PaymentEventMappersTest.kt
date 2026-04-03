package com.andyahmedov.enought.data.repository

import com.andyahmedov.enought.data.entity.PaymentEventSourceEntity
import com.andyahmedov.enought.data.entity.PaymentEventWithSources
import com.andyahmedov.enought.domain.model.ConfidenceLevel
import com.andyahmedov.enought.domain.model.PaymentChannel
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.PaymentStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentEventMappersTest {
    @Test
    fun `toEntity maps payment event to room entity`() {
        val entity = paymentEvent().toEntity()

        assertEquals("event-1", entity.id)
        assertEquals(34900L, entity.amountMinor)
        assertEquals("RUB", entity.currency)
        assertEquals("MIR_PAY", entity.sourceKind)
        assertEquals("PHONE", entity.paymentChannel)
        assertEquals("HIGH", entity.confidence)
        assertEquals("CONFIRMED", entity.status)
        assertEquals(false, entity.userEdited)
        assertEquals(null, entity.duplicateGroupId)
    }

    @Test
    fun `toSourceEntities deduplicates duplicate source ids`() {
        val sourceEntities = paymentEvent(sourceIds = listOf("raw-2", "raw-1", "raw-2")).toSourceEntities()

        assertEquals(
            listOf(
                PaymentEventSourceEntity(paymentEventId = "event-1", sourceId = "raw-2"),
                PaymentEventSourceEntity(paymentEventId = "event-1", sourceId = "raw-1"),
            ),
            sourceEntities,
        )
    }

    @Test
    fun `toDomain restores payment event and sorts source ids for stable reads`() {
        val relation = PaymentEventWithSources(
            event = paymentEvent().toEntity(),
            sourceEntities = listOf(
                PaymentEventSourceEntity(paymentEventId = "event-1", sourceId = "raw-2"),
                PaymentEventSourceEntity(paymentEventId = "event-1", sourceId = "raw-1"),
            ),
        )

        val event = relation.toDomain()

        assertEquals("event-1", event.id)
        assertEquals(34900L, event.amountMinor)
        assertEquals("RUB", event.currency)
        assertEquals(PaymentSourceKind.MIR_PAY, event.sourceKind)
        assertEquals(PaymentChannel.PHONE, event.paymentChannel)
        assertEquals(ConfidenceLevel.HIGH, event.confidence)
        assertEquals(PaymentStatus.CONFIRMED, event.status)
        assertEquals(listOf("raw-1", "raw-2"), event.sourceIds)
        assertEquals(null, event.duplicateGroupId)
    }

    private fun paymentEvent(
        sourceIds: List<String> = listOf("raw-1"),
    ): PaymentEvent {
        return PaymentEvent(
            id = "event-1",
            amountMinor = 34900L,
            currency = "RUB",
            paidAt = Instant.parse("2026-03-31T10:15:30Z"),
            merchantName = null,
            sourceKind = PaymentSourceKind.MIR_PAY,
            paymentChannel = PaymentChannel.PHONE,
            confidence = ConfidenceLevel.HIGH,
            status = PaymentStatus.CONFIRMED,
            userEdited = false,
            sourceIds = sourceIds,
        )
    }
}
