package com.andyahmedov.enought.ui.today

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.andyahmedov.enought.collection.notifications.NotificationAccessSettingsLauncher
import com.andyahmedov.enought.collection.notifications.NotificationAccessStatusReader
import com.andyahmedov.enought.collection.notifications.rememberNotificationAccessState
import com.andyahmedov.enought.domain.model.DailySpendSummary
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.usecase.ObserveTodayPaymentEventsUseCase
import com.andyahmedov.enought.domain.usecase.ObserveTodaySummaryUseCase
import com.andyahmedov.enought.domain.usecase.ObserveWidgetPrivateModeUseCase
import com.andyahmedov.enought.domain.usecase.SetWidgetPrivateModeUseCase
import kotlinx.coroutines.launch

@Composable
fun TodayRoute(
    observeTodayPaymentEventsUseCase: ObserveTodayPaymentEventsUseCase,
    observeTodaySummaryUseCase: ObserveTodaySummaryUseCase,
    observeWidgetPrivateModeUseCase: ObserveWidgetPrivateModeUseCase,
    setWidgetPrivateModeUseCase: SetWidgetPrivateModeUseCase,
    notificationAccessStatusReader: NotificationAccessStatusReader,
    notificationAccessSettingsLauncher: NotificationAccessSettingsLauncher,
    onOpenRawNotifications: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val eventsFlow = remember(observeTodayPaymentEventsUseCase) {
        observeTodayPaymentEventsUseCase()
    }
    val summaryFlow = remember(observeTodaySummaryUseCase) {
        observeTodaySummaryUseCase()
    }
    val widgetPrivateModeFlow = remember(observeWidgetPrivateModeUseCase) {
        observeWidgetPrivateModeUseCase()
    }
    val events by eventsFlow.collectAsState(initial = null as List<PaymentEvent>?)
    val summary by summaryFlow.collectAsState(initial = null as DailySpendSummary?)
    val isWidgetPrivateModeEnabled by widgetPrivateModeFlow.collectAsState(initial = false)
    val hasNotificationAccess = rememberNotificationAccessState(
        notificationAccessStatusReader = notificationAccessStatusReader,
    )
    val uiState = remember(hasNotificationAccess, summary, events, isWidgetPrivateModeEnabled) {
        TodayUiStateFactory.create(
            hasNotificationAccess = hasNotificationAccess,
            summary = summary,
            events = events,
            isWidgetPrivateModeEnabled = isWidgetPrivateModeEnabled,
        )
    }

    TodayScreen(
        uiState = uiState,
        onSetWidgetPrivateModeEnabled = { isEnabled ->
            coroutineScope.launch {
                setWidgetPrivateModeUseCase(isEnabled)
            }
        },
        onOpenNotificationAccessSettings = {
            notificationAccessSettingsLauncher.open(context)
        },
        onOpenRawNotifications = onOpenRawNotifications,
        onOpenReview = onOpenReview,
        onOpenSettings = onOpenSettings,
    )
}
