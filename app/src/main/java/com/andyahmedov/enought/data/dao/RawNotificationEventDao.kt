package com.andyahmedov.enought.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.andyahmedov.enought.data.entity.RawNotificationEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RawNotificationEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(event: RawNotificationEventEntity): Long

    @Query("SELECT * FROM raw_notification_events ORDER BY posted_at DESC")
    fun observeAllByPostedAtDesc(): Flow<List<RawNotificationEventEntity>>

    @Query("SELECT * FROM raw_notification_events ORDER BY posted_at DESC")
    suspend fun getAllByPostedAtDesc(): List<RawNotificationEventEntity>
}
