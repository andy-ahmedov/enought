package com.andyahmedov.enought.data.repository

import com.andyahmedov.enought.data.entity.PaymentEventEntity
import com.andyahmedov.enought.data.entity.PaymentEventSourceEntity
import com.andyahmedov.enought.data.entity.PaymentEventWithSources
import com.andyahmedov.enought.domain.model.ConfidenceLevel
import com.andyahmedov.enought.domain.model.PaymentChannel
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.PaymentStatus

fun PaymentEvent.toEntity(): PaymentEventEntity {
    return PaymentEventEntity(
        id = id,
        amountMinor = amountMinor,
        currency = currency,
        paidAt = paidAt,
        merchantName = merchantName,
        sourceKind = sourceKind.name,
        paymentChannel = paymentChannel.name,
        confidence = confidence.name,
        status = status.name,
        userEdited = userEdited,
        duplicateGroupId = duplicateGroupId,
    )
}

fun PaymentEvent.toSourceEntities(): List<PaymentEventSourceEntity> {
    return sourceIds
        .distinct()
        .map { sourceId ->
            PaymentEventSourceEntity(
                paymentEventId = id,
                sourceId = sourceId,
            )
        }
}

fun PaymentEventWithSources.toDomain(): PaymentEvent {
    return PaymentEvent(
        id = event.id,
        amountMinor = event.amountMinor,
        currency = event.currency,
        paidAt = event.paidAt,
        merchantName = event.merchantName,
        sourceKind = enumValueOf<PaymentSourceKind>(event.sourceKind),
        paymentChannel = enumValueOf<PaymentChannel>(event.paymentChannel),
        confidence = enumValueOf<ConfidenceLevel>(event.confidence),
        status = enumValueOf<PaymentStatus>(event.status),
        userEdited = event.userEdited,
        sourceIds = sourceEntities.map { it.sourceId }.sorted(),
        duplicateGroupId = event.duplicateGroupId,
    )
}
