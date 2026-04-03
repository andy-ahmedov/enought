package com.andyahmedov.enought.widget

import com.andyahmedov.enought.collection.notifications.NotificationAccessStatusReader
import com.andyahmedov.enought.domain.repository.WidgetPrivateModeRepository
import com.andyahmedov.enought.domain.usecase.ObserveTodaySummaryUseCase
import kotlinx.coroutines.flow.first

class GetTodayWidgetStateUseCase(
    private val notificationAccessStatusReader: NotificationAccessStatusReader,
    private val observeTodaySummaryUseCase: ObserveTodaySummaryUseCase,
    private val widgetPrivateModeRepository: WidgetPrivateModeRepository,
) {
    suspend operator fun invoke(): TodayWidgetState {
        if (!notificationAccessStatusReader.hasNotificationAccess()) {
            return TodayWidgetState.NoPermission
        }

        val summary = observeTodaySummaryUseCase().first()
        return if (summary.paymentsCount == 0) {
            TodayWidgetState.NoData(
                hasLowConfidenceItems = summary.hasLowConfidenceItems,
            )
        } else {
            if (widgetPrivateModeRepository.isEnabled()) {
                TodayWidgetState.ReadyPrivate(
                    paymentsCount = summary.paymentsCount,
                    remainingAmountMinor = summary.remainingAmountMinor,
                    limitWarningLevel = summary.limitWarningLevel,
                    hasLowConfidenceItems = summary.hasLowConfidenceItems,
                )
            } else {
                TodayWidgetState.ReadyRegular(
                    totalAmountMinor = summary.totalAmountMinor,
                    paymentsCount = summary.paymentsCount,
                    lastPaymentAmountMinor = summary.lastPaymentAmountMinor,
                    remainingAmountMinor = summary.remainingAmountMinor,
                    limitWarningLevel = summary.limitWarningLevel,
                    hasLowConfidenceItems = summary.hasLowConfidenceItems,
                )
            }
        }
    }
}
