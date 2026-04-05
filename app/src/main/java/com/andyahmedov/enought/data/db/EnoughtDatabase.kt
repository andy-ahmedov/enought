package com.andyahmedov.enought.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.andyahmedov.enought.data.dao.PaymentEventEditDao
import com.andyahmedov.enought.data.dao.PaymentEventDao
import com.andyahmedov.enought.data.dao.RawNotificationEventDao
import com.andyahmedov.enought.data.entity.PaymentEventEditEntity
import com.andyahmedov.enought.data.entity.PaymentEventEntity
import com.andyahmedov.enought.data.entity.PaymentEventSourceEntity
import com.andyahmedov.enought.data.entity.RawNotificationEventEntity

@Database(
    entities = [
        RawNotificationEventEntity::class,
        PaymentEventEntity::class,
        PaymentEventSourceEntity::class,
        PaymentEventEditEntity::class,
    ],
    version = 6,
    exportSchema = false,
)
@TypeConverters(InstantTypeConverters::class)
abstract class EnoughtDatabase : RoomDatabase() {
    abstract fun rawNotificationEventDao(): RawNotificationEventDao

    abstract fun paymentEventDao(): PaymentEventDao

    abstract fun paymentEventEditDao(): PaymentEventEditDao
}
