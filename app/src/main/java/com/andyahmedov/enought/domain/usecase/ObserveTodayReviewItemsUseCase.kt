package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentStatus
import com.andyahmedov.enought.domain.model.TodayReviewItem
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ObserveTodayReviewItemsUseCase(
    private val paymentEventRepository: PaymentEventRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<List<TodayReviewItem>> {
        val today = LocalDate.now(clock)
        val dayRange = today.toInstantRange(clock)

        return paymentEventRepository.observePaymentEventsBetween(
            startInclusive = dayRange.startInclusive,
            endExclusive = dayRange.endExclusive,
        ).map { events ->
            events.toTodayReviewItems()
        }
    }

    private fun List<PaymentEvent>.toTodayReviewItems(): List<TodayReviewItem> {
        val reviewEvents = filter { event ->
                event.status == PaymentStatus.SUSPECTED && event.currency == RUB_CURRENCY
            }
        val groupedEvents = reviewEvents
            .filter { event -> event.duplicateGroupId != null }
            .groupBy { event -> event.duplicateGroupId.orEmpty() }
        val groupedIds = groupedEvents.values.flatten().map { event -> event.id }.toSet()
        val items = mutableListOf<TodayReviewItem>()

        reviewEvents
            .filter { event -> event.id !in groupedIds }
            .mapTo(items) { event ->
                event.toSingleReviewItem()
            }

        groupedEvents.values.forEach { groupedConflictEvents ->
            if (groupedConflictEvents.size == DUPLICATE_CONFLICT_SIZE) {
                items += groupedConflictEvents.toDuplicateConflictReviewItem()
            } else {
                groupedConflictEvents.forEach { event ->
                    items += event.toSingleReviewItem()
                }
            }
        }

        return items.sortedByDescending { item -> item.sortPaidAt }
    }

    private fun List<PaymentEvent>.toDuplicateConflictReviewItem(): TodayReviewItem.DuplicateConflict {
        val sortedEvents = sortedByDescending { event -> event.paidAt }

        return TodayReviewItem.DuplicateConflict(
            duplicateGroupId = sortedEvents.first().duplicateGroupId.orEmpty(),
            amountMinor = sortedEvents.first().amountMinor,
            items = sortedEvents.map { event ->
                TodayReviewItem.DuplicateConflictEntry(
                    id = event.id,
                    paidAt = event.paidAt,
                    title = event.reviewTitle(),
                    sourceKind = event.sourceKind,
                )
            },
        )
    }

    private fun PaymentEvent.toSingleReviewItem(): TodayReviewItem.Single {
        return TodayReviewItem.Single(
            id = id,
            amountMinor = amountMinor,
            paidAt = paidAt,
            title = reviewTitle(),
            sourceKind = sourceKind,
        )
    }

    private fun PaymentEvent.reviewTitle(): String {
        return merchantName?.trim().takeUnless { it.isNullOrBlank() } ?: sourceKind.toFallbackTitle()
    }

    private fun com.andyahmedov.enought.domain.model.PaymentSourceKind.toFallbackTitle(): String {
        return when (this) {
            com.andyahmedov.enought.domain.model.PaymentSourceKind.MIR_PAY -> "Mir Pay payment"
            com.andyahmedov.enought.domain.model.PaymentSourceKind.BANK -> "Bank notification"
            com.andyahmedov.enought.domain.model.PaymentSourceKind.HYBRID -> "Mir Pay + bank"
            com.andyahmedov.enought.domain.model.PaymentSourceKind.MANUAL -> "Manual payment"
        }
    }

    private fun LocalDate.toInstantRange(clock: Clock): InstantRange {
        val zoneId = clock.zone
        val startInclusive = atStartOfDay(zoneId).toInstant()
        val endExclusive = plusDays(1).atStartOfDay(zoneId).toInstant()

        return InstantRange(
            startInclusive = startInclusive,
            endExclusive = endExclusive,
        )
    }

    private data class InstantRange(
        val startInclusive: Instant,
        val endExclusive: Instant,
    )

    private companion object {
        const val RUB_CURRENCY = "RUB"
        const val DUPLICATE_CONFLICT_SIZE = 2
    }
}
