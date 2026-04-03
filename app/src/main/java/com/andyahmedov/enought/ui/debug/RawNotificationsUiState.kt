package com.andyahmedov.enought.ui.debug

import com.andyahmedov.enought.domain.model.RawNotificationEvent

data class RawNotificationsUiState(
    val items: List<RawNotificationListItem> = emptyList(),
)

data class RawNotificationListItem(
    val id: String,
    val sourcePackage: String,
    val postedAt: String,
    val title: String?,
    val text: String?,
    val subText: String?,
    val bigText: String?,
    val payloadHash: String,
)

fun RawNotificationEvent.toListItem(): RawNotificationListItem {
    return RawNotificationListItem(
        id = id,
        sourcePackage = sourcePackage,
        postedAt = postedAt.toString(),
        title = title,
        text = text,
        subText = subText,
        bigText = bigText,
        payloadHash = payloadHash,
    )
}
