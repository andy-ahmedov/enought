package com.andyahmedov.enought.domain.repository

import com.andyahmedov.enought.domain.model.RawNotificationEvent
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface RawNotificationEventRepository {
    suspend fun saveIfNew(event: RawNotificationEvent): Boolean

    suspend fun getRawEventsBetween(
        startInclusive: Instant,
        endExclusive: Instant,
    ): List<RawNotificationEvent>

    fun observeRawEvents(): Flow<List<RawNotificationEvent>>
}
