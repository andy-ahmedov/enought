package com.andyahmedov.enought.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "payment_event_edits",
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
    ],
)
data class PaymentEventEditEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "payment_event_id")
    val paymentEventId: String,
    @ColumnInfo(name = "edited_at")
    val editedAt: Instant,
    @ColumnInfo(name = "edit_type")
    val editType: String,
    @ColumnInfo(name = "previous_status")
    val previousStatus: String,
    @ColumnInfo(name = "new_status")
    val newStatus: String,
    @ColumnInfo(name = "previous_amount_minor")
    val previousAmountMinor: Long?,
    @ColumnInfo(name = "new_amount_minor")
    val newAmountMinor: Long?,
)
