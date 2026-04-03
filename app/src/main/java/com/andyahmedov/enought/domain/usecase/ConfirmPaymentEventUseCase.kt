package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.PaymentEditType
import com.andyahmedov.enought.domain.model.PaymentEventEdit
import com.andyahmedov.enought.domain.model.PaymentStatus
import com.andyahmedov.enought.domain.repository.PaymentEventEditRepository
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import com.andyahmedov.enought.widget.WidgetUpdater
import java.time.Clock
import java.util.UUID

class ConfirmPaymentEventUseCase(
    private val paymentEventRepository: PaymentEventRepository,
    private val paymentEventEditRepository: PaymentEventEditRepository,
    private val widgetUpdater: WidgetUpdater,
    private val clock: Clock,
) {
    suspend operator fun invoke(eventId: String): Boolean {
        val event = paymentEventRepository.getById(eventId) ?: return false
        if (event.status != PaymentStatus.SUSPECTED || event.duplicateGroupId != null) {
            return false
        }

        val updatedEvent = event.copy(
            status = PaymentStatus.CONFIRMED,
            userEdited = true,
        )
        paymentEventRepository.save(updatedEvent)
        paymentEventEditRepository.save(
            PaymentEventEdit(
                id = UUID.randomUUID().toString(),
                paymentEventId = event.id,
                editedAt = clock.instant(),
                editType = PaymentEditType.CONFIRM,
                previousStatus = event.status,
                newStatus = updatedEvent.status,
                previousAmountMinor = event.amountMinor,
                newAmountMinor = updatedEvent.amountMinor,
            ),
        )
        widgetUpdater.refresh()
        return true
    }
}
