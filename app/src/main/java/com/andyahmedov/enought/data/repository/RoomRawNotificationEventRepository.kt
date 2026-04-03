package com.andyahmedov.enought.data.repository

import com.andyahmedov.enought.data.dao.RawNotificationEventDao
import com.andyahmedov.enought.domain.model.RawNotificationEvent
import com.andyahmedov.enought.domain.repository.RawNotificationEventRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomRawNotificationEventRepository(
    private val rawNotificationEventDao: RawNotificationEventDao,
) : RawNotificationEventRepository {
    override suspend fun save(event: RawNotificationEvent) {
        rawNotificationEventDao.upsert(event.toEntity())
    }

    override fun observeRawEvents(): Flow<List<RawNotificationEvent>> {
        return rawNotificationEventDao.observeAllByPostedAtDesc().map { entities ->
            entities.map { it.toDomain() }
        }
    }
}

