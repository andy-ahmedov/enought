package com.andyahmedov.enought.data.repository

import com.andyahmedov.enought.data.entity.PaymentEventEditEntity
import com.andyahmedov.enought.domain.model.PaymentEditType
import com.andyahmedov.enought.domain.model.PaymentEventEdit
import com.andyahmedov.enought.domain.model.PaymentStatus

fun PaymentEventEdit.toEntity(): PaymentEventEditEntity {
    return PaymentEventEditEntity(
        id = id,
        paymentEventId = paymentEventId,
        editedAt = editedAt,
        editType = editType.name,
        previousStatus = previousStatus.name,
        newStatus = newStatus.name,
        previousAmountMinor = previousAmountMinor,
        newAmountMinor = newAmountMinor,
    )
}

fun PaymentEventEditEntity.toDomain(): PaymentEventEdit {
    return PaymentEventEdit(
        id = id,
        paymentEventId = paymentEventId,
        editedAt = editedAt,
        editType = enumValueOf<PaymentEditType>(editType),
        previousStatus = enumValueOf<PaymentStatus>(previousStatus),
        newStatus = enumValueOf<PaymentStatus>(newStatus),
        previousAmountMinor = previousAmountMinor,
        newAmountMinor = newAmountMinor,
    )
}
