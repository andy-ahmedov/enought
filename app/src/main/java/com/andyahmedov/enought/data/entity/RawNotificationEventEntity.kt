package com.andyahmedov.enought.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "raw_notification_events",
    indices = [
        Index(
            value = ["source_package", "payload_hash"],
            unique = true,
        ),
    ],
)
data class RawNotificationEventEntity(
    @PrimaryKey
    val id: String,
    @ColumnInfo(name = "source_package")
    val sourcePackage: String,
    @ColumnInfo(name = "posted_at")
    val postedAt: Instant,
    val title: String?,
    val text: String?,
    @ColumnInfo(name = "sub_text")
    val subText: String?,
    @ColumnInfo(name = "big_text")
    val bigText: String?,
    @ColumnInfo(name = "extras_json")
    val extrasJson: String?,
    @ColumnInfo(name = "payload_hash")
    val payloadHash: String,
)
