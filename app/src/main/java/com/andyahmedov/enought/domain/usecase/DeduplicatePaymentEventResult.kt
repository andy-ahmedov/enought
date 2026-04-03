package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.PaymentEvent

sealed interface DeduplicatePaymentEventResult {
    val eventsToSave: List<PaymentEvent>

    data class AutoMerge(
        val mergedEvent: PaymentEvent,
    ) : DeduplicatePaymentEventResult {
        override val eventsToSave: List<PaymentEvent> = listOf(mergedEvent)
    }

    data class Conflict(
        val conflictedEvents: List<PaymentEvent>,
    ) : DeduplicatePaymentEventResult {
        override val eventsToSave: List<PaymentEvent> = conflictedEvents
    }

    data class NoMatch(
        val event: PaymentEvent,
    ) : DeduplicatePaymentEventResult {
        override val eventsToSave: List<PaymentEvent> = listOf(event)
    }
}
