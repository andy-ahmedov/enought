package com.andyahmedov.enought.domain.repository

import com.andyahmedov.enought.domain.model.RawNotificationEvent
import kotlinx.coroutines.flow.Flow

interface RawNotificationEventRepository {
    suspend fun save(event: RawNotificationEvent)

    fun observeRawEvents(): Flow<List<RawNotificationEvent>>
}

