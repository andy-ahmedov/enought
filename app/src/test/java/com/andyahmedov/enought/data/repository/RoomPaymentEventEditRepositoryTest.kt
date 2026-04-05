package com.andyahmedov.enought.data.repository

import com.andyahmedov.enought.data.dao.PaymentEventEditDao
import com.andyahmedov.enought.data.entity.PaymentEventEditEntity
import com.andyahmedov.enought.domain.model.PaymentEditType
import com.andyahmedov.enought.domain.model.PaymentEventEdit
import com.andyahmedov.enought.domain.model.PaymentStatus
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomPaymentEventEditRepositoryTest {
    @Test
    fun `getByPaymentEventIds returns mapped edits for matching event ids only`() = runTest {
        val dao = FakePaymentEventEditDao(
            edits = listOf(
                edit(id = "edit-1", paymentEventId = "event-1", editedAt = Instant.parse("2026-04-05T12:00:00Z")).toEntity(),
                edit(id = "edit-2", paymentEventId = "event-2").toEntity(),
                edit(id = "edit-3", paymentEventId = "event-3", editedAt = Instant.parse("2026-04-05T13:00:00Z")).toEntity(),
            ),
        )
        val repository = RoomPaymentEventEditRepository(dao)

        val edits = repository.getByPaymentEventIds(listOf("event-3", "event-1"))

        assertEquals(listOf("event-3", "event-1"), edits.map { edit -> edit.paymentEventId })
    }

    @Test
    fun `getByPaymentEventIds skips dao query for empty ids`() = runTest {
        val dao = FakePaymentEventEditDao(edits = emptyList())
        val repository = RoomPaymentEventEditRepository(dao)

        val edits = repository.getByPaymentEventIds(emptyList())

        assertEquals(emptyList<PaymentEventEdit>(), edits)
        assertEquals(0, dao.queryCount)
    }

    private fun edit(
        id: String,
        paymentEventId: String,
        editedAt: Instant = Instant.parse("2026-04-05T12:00:00Z"),
    ): PaymentEventEdit {
        return PaymentEventEdit(
            id = id,
            paymentEventId = paymentEventId,
            editedAt = editedAt,
            editType = PaymentEditType.CONFIRM,
            previousStatus = PaymentStatus.SUSPECTED,
            newStatus = PaymentStatus.CONFIRMED,
            previousAmountMinor = 52300L,
            newAmountMinor = 52300L,
        )
    }

    private class FakePaymentEventEditDao(
        private val edits: List<PaymentEventEditEntity>,
    ) : PaymentEventEditDao {
        var queryCount: Int = 0

        override suspend fun insert(edit: PaymentEventEditEntity) = Unit

        override suspend fun getByPaymentEventIds(paymentEventIds: List<String>): List<PaymentEventEditEntity> {
            queryCount += 1
            return edits.filter { edit -> edit.paymentEventId in paymentEventIds }
                .sortedByDescending { edit -> edit.editedAt }
        }
    }
}
