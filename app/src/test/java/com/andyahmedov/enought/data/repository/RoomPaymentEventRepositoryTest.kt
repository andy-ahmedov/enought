package com.andyahmedov.enought.data.repository

import com.andyahmedov.enought.data.dao.PaymentEventDao
import com.andyahmedov.enought.data.entity.PaymentEventEntity
import com.andyahmedov.enought.data.entity.PaymentEventSourceEntity
import com.andyahmedov.enought.data.entity.PaymentEventWithSources
import com.andyahmedov.enought.domain.model.ConfidenceLevel
import com.andyahmedov.enought.domain.model.PaymentChannel
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.PaymentStatus
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomPaymentEventRepositoryTest {
    @Test
    fun `getPaymentEventsBetween forwards bounds and maps domain events`() = runTest {
        val paymentEventDao = FakePaymentEventDao(
            relations = listOf(
                paymentEvent(id = "event-1", paidAt = Instant.parse("2026-03-31T10:00:00Z"), amountMinor = 10000L).toRelation(),
                paymentEvent(id = "event-2", paidAt = Instant.parse("2026-04-01T10:00:00Z"), amountMinor = 20000L).toRelation(),
                paymentEvent(id = "event-3", paidAt = Instant.parse("2026-04-01T11:00:00Z"), amountMinor = 30000L).toRelation(),
            ),
        )
        val repository = RoomPaymentEventRepository(paymentEventDao)
        val startInclusive = Instant.parse("2026-04-01T00:00:00Z")
        val endExclusive = Instant.parse("2026-04-02T00:00:00Z")

        val events = repository.getPaymentEventsBetween(
            startInclusive = startInclusive,
            endExclusive = endExclusive,
        )

        assertEquals(startInclusive, paymentEventDao.lastSuspendStartInclusive)
        assertEquals(endExclusive, paymentEventDao.lastSuspendEndExclusive)
        assertEquals(listOf("event-3", "event-2"), events.map { it.id })
    }

    @Test
    fun `observePaymentEventsBetween forwards bounds and maps domain events`() = runTest {
        val paymentEventDao = FakePaymentEventDao(
            relations = listOf(
                paymentEvent(id = "event-1", paidAt = Instant.parse("2026-03-31T10:00:00Z"), amountMinor = 10000L).toRelation(),
                paymentEvent(id = "event-2", paidAt = Instant.parse("2026-04-01T10:00:00Z"), amountMinor = 20000L).toRelation(),
                paymentEvent(id = "event-3", paidAt = Instant.parse("2026-04-01T11:00:00Z"), amountMinor = 30000L).toRelation(),
            ),
        )
        val repository = RoomPaymentEventRepository(paymentEventDao)
        val startInclusive = Instant.parse("2026-04-01T00:00:00Z")
        val endExclusive = Instant.parse("2026-04-02T00:00:00Z")

        val events = repository.observePaymentEventsBetween(
            startInclusive = startInclusive,
            endExclusive = endExclusive,
        ).first()

        assertEquals(startInclusive, paymentEventDao.lastStartInclusive)
        assertEquals(endExclusive, paymentEventDao.lastEndExclusive)
        assertEquals(listOf("event-3", "event-2"), events.map { it.id })
        assertEquals(listOf(30000L, 20000L), events.map { it.amountMinor })
    }

    private fun paymentEvent(
        id: String,
        paidAt: Instant,
        amountMinor: Long,
    ): PaymentEvent {
        return PaymentEvent(
            id = id,
            amountMinor = amountMinor,
            currency = "RUB",
            paidAt = paidAt,
            merchantName = null,
            sourceKind = PaymentSourceKind.BANK,
            paymentChannel = PaymentChannel.UNKNOWN,
            confidence = ConfidenceLevel.MEDIUM,
            status = PaymentStatus.CONFIRMED,
            userEdited = false,
            sourceIds = listOf("raw-$id"),
        )
    }

    private fun PaymentEvent.toRelation(): PaymentEventWithSources {
        return PaymentEventWithSources(
            event = toEntity(),
            sourceEntities = toSourceEntities(),
        )
    }

    private class FakePaymentEventDao(
        private val relations: List<PaymentEventWithSources>,
    ) : PaymentEventDao {
        var lastStartInclusive: Instant? = null
        var lastEndExclusive: Instant? = null
        var lastSuspendStartInclusive: Instant? = null
        var lastSuspendEndExclusive: Instant? = null

        override suspend fun upsertEvent(event: PaymentEventEntity) = Unit

        override suspend fun deleteSourcesForEvent(paymentEventId: String) = Unit

        override suspend fun insertSources(sources: List<PaymentEventSourceEntity>) = Unit

        override suspend fun getById(id: String): PaymentEventWithSources? {
            return relations.firstOrNull { relation -> relation.event.id == id }
        }

        override suspend fun getByDuplicateGroupId(duplicateGroupId: String): List<PaymentEventWithSources> {
            return relations.filter { relation ->
                relation.event.duplicateGroupId == duplicateGroupId
            }.sortedByDescending { it.event.paidAt }
        }

        override suspend fun getByPaidAtBetween(
            startInclusive: Instant,
            endExclusive: Instant,
        ): List<PaymentEventWithSources> {
            lastSuspendStartInclusive = startInclusive
            lastSuspendEndExclusive = endExclusive

            return relations.filter { relation ->
                relation.event.paidAt >= startInclusive && relation.event.paidAt < endExclusive
            }.sortedByDescending { it.event.paidAt }
        }

        override fun observeAllByPaidAtDesc(): Flow<List<PaymentEventWithSources>> {
            return flowOf(relations.sortedByDescending { it.event.paidAt })
        }

        override fun observeByPaidAtBetween(
            startInclusive: Instant,
            endExclusive: Instant,
        ): Flow<List<PaymentEventWithSources>> {
            lastStartInclusive = startInclusive
            lastEndExclusive = endExclusive

            return flowOf(
                relations.filter { relation ->
                    relation.event.paidAt >= startInclusive && relation.event.paidAt < endExclusive
                }.sortedByDescending { it.event.paidAt },
            )
        }
    }
}
