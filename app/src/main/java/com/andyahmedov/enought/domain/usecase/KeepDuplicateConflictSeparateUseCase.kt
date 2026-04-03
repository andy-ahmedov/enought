package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.PaymentEditType
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentEventEdit
import com.andyahmedov.enought.domain.model.PaymentStatus
import com.andyahmedov.enought.domain.repository.PaymentEventEditRepository
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import com.andyahmedov.enought.widget.WidgetUpdater
import java.time.Clock
import java.util.UUID

class KeepDuplicateConflictSeparateUseCase(
    private val paymentEventRepository: PaymentEventRepository,
    private val paymentEventEditRepository: PaymentEventEditRepository,
    private val widgetUpdater: WidgetUpdater,
    private val clock: Clock,
) {
    suspend operator fun invoke(duplicateGroupId: String): Boolean {
        val duplicateEvents = paymentEventRepository.getByDuplicateGroupId(duplicateGroupId)
            .takeIf { events -> events.isValidDuplicateConflict(duplicateGroupId) }
            ?: return false
        val resolvedEvents = duplicateEvents.map { event ->
            event.copy(
                status = PaymentStatus.CONFIRMED,
                userEdited = true,
                duplicateGroupId = null,
            )
        }

        paymentEventRepository.saveAll(resolvedEvents)
        resolvedEvents.forEachIndexed { index, updatedEvent ->
            paymentEventEditRepository.saveKeepDuplicateSeparateEdit(
                originalEvent = duplicateEvents[index],
                updatedEvent = updatedEvent,
                editedAtMillis = clock.instant(),
            )
        }
        widgetUpdater.refresh()
        return true
    }
}
