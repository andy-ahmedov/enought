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

class MergeDuplicateConflictUseCase(
    private val paymentEventRepository: PaymentEventRepository,
    private val paymentEventEditRepository: PaymentEventEditRepository,
    private val widgetUpdater: WidgetUpdater,
    private val clock: Clock,
) {
    suspend operator fun invoke(duplicateGroupId: String): Boolean {
        val duplicateEvents = paymentEventRepository.getByDuplicateGroupId(duplicateGroupId)
            .takeIf { events -> events.isValidDuplicateConflict(duplicateGroupId) }
            ?: return false
        val canonicalEvent = selectManualDuplicateCanonicalEvent(
            firstEvent = duplicateEvents[0],
            secondEvent = duplicateEvents[1],
        )
        val siblingEvent = duplicateEvents.first { event -> event.id != canonicalEvent.id }
        val mergedEvent = mergeDuplicatePair(
            canonicalEvent = canonicalEvent,
            siblingEvent = siblingEvent,
            userEdited = true,
        )
        val dismissedEvent = siblingEvent.copy(
            status = PaymentStatus.DISMISSED,
            userEdited = true,
            duplicateGroupId = null,
        )

        paymentEventRepository.saveAll(
            listOf(mergedEvent, dismissedEvent),
        )
        paymentEventEditRepository.saveMergeDuplicateEdit(
            originalEvent = canonicalEvent,
            updatedEvent = mergedEvent,
            editedAtMillis = clock.instant(),
        )
        paymentEventEditRepository.saveMergeDuplicateEdit(
            originalEvent = siblingEvent,
            updatedEvent = dismissedEvent,
            editedAtMillis = clock.instant(),
        )
        widgetUpdater.refresh()
        return true
    }
}
