package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.PaymentEditType
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentEventEdit
import com.andyahmedov.enought.domain.model.PaymentStatus
import com.andyahmedov.enought.domain.repository.PaymentEventEditRepository
import java.time.Instant
import java.util.UUID

internal fun List<PaymentEvent>.isValidDuplicateConflict(duplicateGroupId: String): Boolean {
    return size == 2 &&
        all { event ->
            event.status == PaymentStatus.SUSPECTED &&
                event.duplicateGroupId == duplicateGroupId
        }
}

internal suspend fun PaymentEventEditRepository.saveMergeDuplicateEdit(
    originalEvent: PaymentEvent,
    updatedEvent: PaymentEvent,
    editedAtMillis: Instant,
) {
    save(
        PaymentEventEdit(
            id = UUID.randomUUID().toString(),
            paymentEventId = originalEvent.id,
            editedAt = editedAtMillis,
            editType = PaymentEditType.MERGE_DUPLICATE,
            previousStatus = originalEvent.status,
            newStatus = updatedEvent.status,
            previousAmountMinor = originalEvent.amountMinor,
            newAmountMinor = updatedEvent.takeUnless { event -> event.status == PaymentStatus.DISMISSED }?.amountMinor,
        ),
    )
}

internal suspend fun PaymentEventEditRepository.saveKeepDuplicateSeparateEdit(
    originalEvent: PaymentEvent,
    updatedEvent: PaymentEvent,
    editedAtMillis: Instant,
) {
    save(
        PaymentEventEdit(
            id = UUID.randomUUID().toString(),
            paymentEventId = originalEvent.id,
            editedAt = editedAtMillis,
            editType = PaymentEditType.KEEP_DUPLICATES_SEPARATE,
            previousStatus = originalEvent.status,
            newStatus = updatedEvent.status,
            previousAmountMinor = originalEvent.amountMinor,
            newAmountMinor = updatedEvent.amountMinor,
        ),
    )
}
