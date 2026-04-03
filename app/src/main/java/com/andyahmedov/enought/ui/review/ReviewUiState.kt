package com.andyahmedov.enought.ui.review

import com.andyahmedov.enought.domain.model.PaymentEvent

import com.andyahmedov.enought.domain.model.TodayReviewItem

sealed interface ReviewUiState {
    data object Loading : ReviewUiState

    data object Empty : ReviewUiState

    data class Ready(
        val items: List<ReviewListItem>,
    ) : ReviewUiState
}

internal object ReviewUiStateFactory {
    fun create(
        reviewItems: List<TodayReviewItem>?,
    ): ReviewUiState {
        if (reviewItems == null) {
            return ReviewUiState.Loading
        }

        if (reviewItems.isEmpty()) {
            return ReviewUiState.Empty
        }

        return ReviewUiState.Ready(
            items = reviewItems.map { event ->
                event.toReviewListItem()
            },
        )
    }
}
