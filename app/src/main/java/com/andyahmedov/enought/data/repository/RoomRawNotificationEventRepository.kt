package com.andyahmedov.enought.data.repository

import com.andyahmedov.enought.data.dao.RawNotificationEventDao
import com.andyahmedov.enought.domain.model.RawNotificationEvent
import com.andyahmedov.enought.domain.repository.RawNotificationEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant

class RoomRawNotificationEventRepository(
    private val rawNotificationEventDao: RawNotificationEventDao,
) : RawNotificationEventRepository {
    override suspend fun saveIfNew(event: RawNotificationEvent): Boolean {
        return rawNotificationEventDao.insertIgnore(event.toEntity()) != INSERT_FAILED
    }

    override suspend fun getRawEventsBetween(
        startInclusive: Instant,
        endExclusive: Instant,
    ): List<RawNotificationEvent> {
        return rawNotificationEventDao.getByPostedAtBetween(
            startInclusive = startInclusive,
            endExclusive = endExclusive,
        ).map { entity ->
            entity.toDomain()
        }
    }

    override fun observeRawEvents(): Flow<List<RawNotificationEvent>> {
        return rawNotificationEventDao.observeAllByPostedAtDesc().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    private companion object {
        const val INSERT_FAILED = -1L
    }
}
