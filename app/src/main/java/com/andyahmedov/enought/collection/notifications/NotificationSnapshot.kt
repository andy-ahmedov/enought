package com.andyahmedov.enought.collection.notifications

import java.time.Instant

data class NotificationSnapshot(
    val sourcePackage: String,
    val postedAt: Instant,
    val notificationId: Int,
    val notificationTag: String?,
    val channelId: String?,
    val category: String?,
    val title: String?,
    val text: String?,
    val subText: String?,
    val bigText: String?,
)
