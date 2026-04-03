package com.andyahmedov.enought.data.repository

import com.andyahmedov.enought.domain.model.PaymentEditType
import com.andyahmedov.enought.domain.model.PaymentEventEdit
import com.andyahmedov.enought.domain.model.PaymentStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentEventEditMappersTest {
    @Test
    fun `toEntity maps payment edit to room entity`() {
        val entity = paymentEventEdit().toEntity()

        assertEquals("edit-1", entity.id)
        assertEquals("event-1", entity.paymentEventId)
        assertEquals("CORRECT_AMOUNT", entity.editType)
        assertEquals("SUSPECTED", entity.previousStatus)
        assertEquals("CORRECTED", entity.newStatus)
        assertEquals(34900L, entity.previousAmountMinor)
        assertEquals(39900L, entity.newAmountMinor)
    }

    @Test
    fun `toDomain restores payment edit`() {
        val edit = paymentEventEdit().toEntity().toDomain()

        assertEquals("edit-1", edit.id)
        assertEquals("event-1", edit.paymentEventId)
        assertEquals(PaymentEditType.CORRECT_AMOUNT, edit.editType)
        assertEquals(PaymentStatus.SUSPECTED, edit.previousStatus)
        assertEquals(PaymentStatus.CORRECTED, edit.newStatus)
        assertEquals(34900L, edit.previousAmountMinor)
        assertEquals(39900L, edit.newAmountMinor)
    }

    private fun paymentEventEdit(): PaymentEventEdit {
        return PaymentEventEdit(
            id = "edit-1",
            paymentEventId = "event-1",
            editedAt = Instant.parse("2026-04-01T10:30:00Z"),
            editType = PaymentEditType.CORRECT_AMOUNT,
            previousStatus = PaymentStatus.SUSPECTED,
            newStatus = PaymentStatus.CORRECTED,
            previousAmountMinor = 34900L,
            newAmountMinor = 39900L,
        )
    }
}
