package com.andyahmedov.enought.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "payment_event_sources",
    primaryKeys = ["payment_event_id", "source_id"],
    foreignKeys = [
        ForeignKey(
            entity = PaymentEventEntity::class,
            parentColumns = ["id"],
            childColumns = ["payment_event_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["payment_event_id"]),
        Index(value = ["source_id"]),
    ],
)
data class PaymentEventSourceEntity(
    @ColumnInfo(name = "payment_event_id")
    val paymentEventId: String,
    @ColumnInfo(name = "source_id")
    val sourceId: String,
)
