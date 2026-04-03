package com.andyahmedov.enought.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.andyahmedov.enought.data.entity.RawNotificationEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RawNotificationEventDao {
    @Upsert
    suspend fun upsert(event: RawNotificationEventEntity)

    @Query("SELECT * FROM raw_notification_events ORDER BY posted_at DESC")
    fun observeAllByPostedAtDesc(): Flow<List<RawNotificationEventEntity>>

    @Query("SELECT * FROM raw_notification_events ORDER BY posted_at DESC")
    suspend fun getAllByPostedAtDesc(): List<RawNotificationEventEntity>
}

