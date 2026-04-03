package com.andyahmedov.enought.data.repository

import com.andyahmedov.enought.data.entity.RawNotificationEventEntity
import com.andyahmedov.enought.domain.model.RawNotificationEvent

fun RawNotificationEvent.toEntity(): RawNotificationEventEntity {
    return RawNotificationEventEntity(
        id = id,
        sourcePackage = sourcePackage,
        postedAt = postedAt,
        title = title,
        text = text,
        subText = subText,
        bigText = bigText,
        extrasJson = extrasJson,
        payloadHash = payloadHash,
    )
}

fun RawNotificationEventEntity.toDomain(): RawNotificationEvent {
    return RawNotificationEvent(
        id = id,
        sourcePackage = sourcePackage,
        postedAt = postedAt,
        title = title,
        text = text,
        subText = subText,
        bigText = bigText,
        extrasJson = extrasJson,
        payloadHash = payloadHash,
    )
}

