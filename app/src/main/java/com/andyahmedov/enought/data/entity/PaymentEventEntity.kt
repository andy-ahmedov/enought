package com.andyahmedov.enought.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(tableName = "payment_events")
data class PaymentEventEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "amount_minor")
    val amountMinor: Long,
    val currency: String,
    @ColumnInfo(name = "paid_at")
    val paidAt: Instant,
    @ColumnInfo(name = "merchant_name")
    val merchantName: String?,
    @ColumnInfo(name = "source_kind")
    val sourceKind: String,
    @ColumnInfo(name = "payment_channel")
    val paymentChannel: String,
    val confidence: String,
    val status: String,
    @ColumnInfo(name = "user_edited")
    val userEdited: Boolean,
    @ColumnInfo(name = "duplicate_group_id")
    val duplicateGroupId: String?,
)
