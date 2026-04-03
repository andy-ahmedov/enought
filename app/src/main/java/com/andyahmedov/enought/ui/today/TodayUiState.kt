package com.andyahmedov.enought.ui.today

import com.andyahmedov.enought.domain.model.DailySpendSummary
import com.andyahmedov.enought.domain.model.DailyLimitWarningLevel
import com.andyahmedov.enought.domain.model.PaymentEvent

sealed interface TodayUiState {
    data object Loading : TodayUiState

    data object NoPermission : TodayUiState

    data class Empty(
        val hasLowConfidenceItems: Boolean,
        val limitAmountMinor: Long?,
        val remainingAmountMinor: Long?,
        val limitWarningLevel: DailyLimitWarningLevel?,
        val isWidgetPrivateModeEnabled: Boolean,
    ) : TodayUiState

    data class Ready(
        val totalAmountMinor: Long,
        val paymentsCount: Int,
        val lastPaymentAmountMinor: Long?,
        val limitAmountMinor: Long?,
        val remainingAmountMinor: Long?,
        val limitWarningLevel: DailyLimitWarningLevel?,
        val hasLowConfidenceItems: Boolean,
        val isWidgetPrivateModeEnabled: Boolean,
        val events: List<TodayPaymentEventListItem>,
    ) : TodayUiState
}

internal object TodayUiStateFactory {
    fun create(
        hasNotificationAccess: Boolean?,
        summary: DailySpendSummary?,
        events: List<PaymentEvent>?,
        isWidgetPrivateModeEnabled: Boolean,
    ): TodayUiState {
        if (hasNotificationAccess == null) {
            return TodayUiState.Loading
        }

        if (!hasNotificationAccess) {
            return TodayUiState.NoPermission
        }

        if (summary == null) {
            return TodayUiState.Loading
        }

        if (summary.paymentsCount == 0) {
            return TodayUiState.Empty(
                hasLowConfidenceItems = summary.hasLowConfidenceItems,
                limitAmountMinor = summary.limitAmountMinor,
                remainingAmountMinor = summary.remainingAmountMinor,
                limitWarningLevel = summary.limitWarningLevel,
                isWidgetPrivateModeEnabled = isWidgetPrivateModeEnabled,
            )
        }

        if (events == null) {
            return TodayUiState.Loading
        }

        return TodayUiState.Ready(
            totalAmountMinor = summary.totalAmountMinor,
            paymentsCount = summary.paymentsCount,
            lastPaymentAmountMinor = summary.lastPaymentAmountMinor,
            limitAmountMinor = summary.limitAmountMinor,
            remainingAmountMinor = summary.remainingAmountMinor,
            limitWarningLevel = summary.limitWarningLevel,
            hasLowConfidenceItems = summary.hasLowConfidenceItems,
            isWidgetPrivateModeEnabled = isWidgetPrivateModeEnabled,
            events = events.map { event ->
                event.toTodayPaymentEventListItem()
            },
        )
    }
}
