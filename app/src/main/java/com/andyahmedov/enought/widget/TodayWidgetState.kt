package com.andyahmedov.enought.widget

import com.andyahmedov.enought.domain.model.DailyLimitWarningLevel

sealed interface TodayWidgetState {
    data object NoPermission : TodayWidgetState

    data class NoData(
        val hasLowConfidenceItems: Boolean,
    ) : TodayWidgetState

    data class ReadyRegular(
        val totalAmountMinor: Long,
        val paymentsCount: Int,
        val lastPaymentAmountMinor: Long?,
        val remainingAmountMinor: Long?,
        val limitWarningLevel: DailyLimitWarningLevel?,
        val hasLowConfidenceItems: Boolean,
    ) : TodayWidgetState

    data class ReadyPrivate(
        val paymentsCount: Int,
        val remainingAmountMinor: Long?,
        val limitWarningLevel: DailyLimitWarningLevel?,
        val hasLowConfidenceItems: Boolean,
    ) : TodayWidgetState
}
