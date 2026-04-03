package com.andyahmedov.enought.domain.model

import java.time.Instant

sealed interface TodayReviewItem {
    val sortPaidAt: Instant

    data class Single(
        val id: String,
        val amountMinor: Long,
        val paidAt: Instant,
        val title: String,
        val sourceKind: PaymentSourceKind,
    ) : TodayReviewItem {
        override val sortPaidAt: Instant = paidAt
    }

    data class DuplicateConflict(
        val duplicateGroupId: String,
        val amountMinor: Long,
        val items: List<DuplicateConflictEntry>,
    ) : TodayReviewItem {
        override val sortPaidAt: Instant = items.maxOf { item -> item.paidAt }
    }

    data class DuplicateConflictEntry(
        val id: String,
        val paidAt: Instant,
        val title: String,
        val sourceKind: PaymentSourceKind,
    )
}
