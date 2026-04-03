package com.andyahmedov.enought.ui.review

import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.TodayReviewItem
import java.time.Instant

sealed interface ReviewListItem {
    data class Single(
        val id: String,
        val amountMinor: Long,
        val paidAt: Instant,
        val title: String,
        val sourceKind: PaymentSourceKind,
    ) : ReviewListItem

    data class DuplicateConflict(
        val duplicateGroupId: String,
        val amountMinor: Long,
        val items: List<DuplicateConflictEntry>,
    ) : ReviewListItem

    data class DuplicateConflictEntry(
        val id: String,
        val paidAt: Instant,
        val title: String,
        val sourceKind: PaymentSourceKind,
    )
}

internal fun TodayReviewItem.toReviewListItem(): ReviewListItem {
    return when (this) {
        is TodayReviewItem.Single -> ReviewListItem.Single(
            id = id,
            amountMinor = amountMinor,
            paidAt = paidAt,
            title = title,
            sourceKind = sourceKind,
        )
        is TodayReviewItem.DuplicateConflict -> ReviewListItem.DuplicateConflict(
            duplicateGroupId = duplicateGroupId,
            amountMinor = amountMinor,
            items = items.map { item ->
                ReviewListItem.DuplicateConflictEntry(
                    id = item.id,
                    paidAt = item.paidAt,
                    title = item.title,
                    sourceKind = item.sourceKind,
                )
            },
        )
    }
}
