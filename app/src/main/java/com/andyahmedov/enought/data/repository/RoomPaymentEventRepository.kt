package com.andyahmedov.enought.data.repository

import com.andyahmedov.enought.data.dao.PaymentEventDao
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class RoomPaymentEventRepository(
    private val paymentEventDao: PaymentEventDao,
) : PaymentEventRepository {
    override suspend fun save(event: PaymentEvent) {
        saveAll(listOf(event))
    }

    override suspend fun saveAll(events: List<PaymentEvent>) {
        paymentEventDao.upsertAllWithSources(
            eventsWithSources = events.map { event ->
                event.toEntity() to event.toSourceEntities()
            },
        )
    }

    override suspend fun getById(id: String): PaymentEvent? {
        return paymentEventDao.getById(id)?.toDomain()
    }

    override suspend fun getByDuplicateGroupId(duplicateGroupId: String): List<PaymentEvent> {
        return paymentEventDao.getByDuplicateGroupId(duplicateGroupId).map { relation ->
            relation.toDomain()
        }
    }

    override suspend fun getPaymentEventsBetween(
        startInclusive: Instant,
        endExclusive: Instant,
    ): List<PaymentEvent> {
        return paymentEventDao.getByPaidAtBetween(
            startInclusive = startInclusive,
            endExclusive = endExclusive,
        ).map { entity ->
            entity.toDomain()
        }
    }

    override fun observePaymentEvents(): Flow<List<PaymentEvent>> {
        return paymentEventDao.observeAllByPaidAtDesc().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun observePaymentEventsBetween(
        startInclusive: Instant,
        endExclusive: Instant,
    ): Flow<List<PaymentEvent>> {
        return paymentEventDao.observeByPaidAtBetween(
            startInclusive = startInclusive,
            endExclusive = endExclusive,
        ).map { entities ->
            entities.map { it.toDomain() }
        }
    }
}
