package com.andyahmedov.enought.ui.history

import com.andyahmedov.enought.domain.model.HistoryDaySummary
import com.andyahmedov.enought.domain.model.HistoryPeriod
import com.andyahmedov.enought.domain.model.HistoryPeriodSnapshot
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.ui.today.TodayPaymentEventListItem
import com.andyahmedov.enought.ui.today.toTodayPaymentEventListItem

sealed interface HistoryUiState {
    val selectedPeriod: HistoryPeriod

    data class Loading(
        override val selectedPeriod: HistoryPeriod,
    ) : HistoryUiState

    data class NoPermission(
        override val selectedPeriod: HistoryPeriod,
    ) : HistoryUiState

    data class Empty(
        override val selectedPeriod: HistoryPeriod,
        val hasLowConfidenceItems: Boolean,
    ) : HistoryUiState

    data class Ready(
        override val selectedPeriod: HistoryPeriod,
        val totalAmountMinor: Long,
        val paymentsCount: Int,
        val lastPaymentAmountMinor: Long?,
        val hasLowConfidenceItems: Boolean,
        val paymentItems: List<TodayPaymentEventListItem>,
        val dayItems: List<HistoryDaySummary>,
    ) : HistoryUiState
}

internal object HistoryUiStateFactory {
    fun create(
        selectedPeriod: HistoryPeriod,
        hasNotificationAccess: Boolean?,
        snapshot: HistoryPeriodSnapshot?,
    ): HistoryUiState {
        if (hasNotificationAccess == null) {
            return HistoryUiState.Loading(selectedPeriod = selectedPeriod)
        }

        if (!hasNotificationAccess) {
            return HistoryUiState.NoPermission(selectedPeriod = selectedPeriod)
        }

        if (snapshot == null) {
            return HistoryUiState.Loading(selectedPeriod = selectedPeriod)
        }

        if (snapshot.summary.paymentsCount == 0) {
            return HistoryUiState.Empty(
                selectedPeriod = selectedPeriod,
                hasLowConfidenceItems = snapshot.summary.hasLowConfidenceItems,
            )
        }

        return HistoryUiState.Ready(
            selectedPeriod = selectedPeriod,
            totalAmountMinor = snapshot.summary.totalAmountMinor,
            paymentsCount = snapshot.summary.paymentsCount,
            lastPaymentAmountMinor = snapshot.summary.lastPaymentAmountMinor,
            hasLowConfidenceItems = snapshot.summary.hasLowConfidenceItems,
            paymentItems = snapshot.paymentEvents.map(PaymentEvent::toTodayPaymentEventListItem),
            dayItems = snapshot.daySummaries,
        )
    }
}
