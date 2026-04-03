package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.ConfidenceLevel
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.PaymentStatus
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import java.time.Duration
import java.util.UUID

class DeduplicatePaymentEventUseCase(
    private val paymentEventRepository: PaymentEventRepository,
) {
    suspend operator fun invoke(incomingEvent: PaymentEvent): DeduplicatePaymentEventResult {
        if (!incomingEvent.isEligibleIncomingEvent()) {
            return DeduplicatePaymentEventResult.NoMatch(incomingEvent)
        }

        val candidates = paymentEventRepository.getPaymentEventsBetween(
            startInclusive = incomingEvent.paidAt.minus(CONFLICT_WINDOW),
            endExclusive = incomingEvent.paidAt.plus(CONFLICT_WINDOW).plusNanos(1),
        )
        val matchingCandidates = candidates
            .asSequence()
            .filter { candidate -> candidate.id != incomingEvent.id }
            .filter { candidate -> isEligibleExistingCandidate(candidate, incomingEvent) }
            .filter { candidate -> candidate.amountMinor == incomingEvent.amountMinor }
            .filter { candidate -> candidate.currency == incomingEvent.currency }
            .map { candidate ->
                MatchCandidate(
                    event = candidate,
                    distance = Duration.between(candidate.paidAt, incomingEvent.paidAt).abs(),
                )
            }
            .sortedWith(compareBy<MatchCandidate> { it.distance }.thenByDescending { it.event.sourceKind == PaymentSourceKind.MIR_PAY })
            .toList()

        val autoMergeCandidates = matchingCandidates.filter { candidate ->
            candidate.distance <= AUTO_MERGE_WINDOW
        }

        return when {
            matchingCandidates.size == 1 && autoMergeCandidates.size == 1 -> {
                DeduplicatePaymentEventResult.AutoMerge(
                    mergedEvent = mergeDuplicatePair(
                        canonicalEvent = autoMergeCandidates.single().event,
                        siblingEvent = incomingEvent,
                        userEdited = false,
                    ),
                )
            }
            matchingCandidates.isNotEmpty() -> {
                val duplicateGroupId = UUID.randomUUID().toString()
                val conflictCandidate = (autoMergeCandidates.ifEmpty { matchingCandidates }).first().event
                DeduplicatePaymentEventResult.Conflict(
                    conflictedEvents = listOf(
                        conflictCandidate.toConflictDuplicate(duplicateGroupId),
                        incomingEvent.toConflictDuplicate(duplicateGroupId),
                    ),
                )
            }
            else -> DeduplicatePaymentEventResult.NoMatch(incomingEvent)
        }
    }

    private fun isEligibleExistingCandidate(
        existingEvent: PaymentEvent,
        incomingEvent: PaymentEvent,
    ): Boolean {
        if (existingEvent.sourceKind == PaymentSourceKind.HYBRID || incomingEvent.sourceKind == PaymentSourceKind.HYBRID) {
            return false
        }
        if (existingEvent.duplicateGroupId != null || incomingEvent.duplicateGroupId != null) {
            return false
        }
        if (existingEvent.userEdited || incomingEvent.userEdited) {
            return false
        }
        if (existingEvent.status != PaymentStatus.CONFIRMED) {
            return false
        }
        if (existingEvent.confidence == ConfidenceLevel.LOW || incomingEvent.confidence == ConfidenceLevel.LOW) {
            return false
        }
        if (Duration.between(existingEvent.paidAt, incomingEvent.paidAt).abs() > CONFLICT_WINDOW) {
            return false
        }

        return setOf(existingEvent.sourceKind, incomingEvent.sourceKind) == MERGEABLE_SOURCE_KINDS
    }

    private fun PaymentEvent.isEligibleIncomingEvent(): Boolean {
        if (sourceKind == PaymentSourceKind.HYBRID) {
            return false
        }
        if (duplicateGroupId != null) {
            return false
        }
        if (userEdited) {
            return false
        }
        if (status != PaymentStatus.CONFIRMED) {
            return false
        }
        return confidence != ConfidenceLevel.LOW
    }

    private fun PaymentEvent.toConflictDuplicate(duplicateGroupId: String): PaymentEvent {
        return copy(
            confidence = ConfidenceLevel.MEDIUM,
            status = PaymentStatus.SUSPECTED,
            duplicateGroupId = duplicateGroupId,
        )
    }

    private data class MatchCandidate(
        val event: PaymentEvent,
        val distance: Duration,
    )

    companion object {
        private val AUTO_MERGE_WINDOW: Duration = Duration.ofMinutes(2)
        private val CONFLICT_WINDOW: Duration = Duration.ofMinutes(5)
        private val MERGEABLE_SOURCE_KINDS: Set<PaymentSourceKind> = setOf(
            PaymentSourceKind.MIR_PAY,
            PaymentSourceKind.BANK,
        )
    }
}
