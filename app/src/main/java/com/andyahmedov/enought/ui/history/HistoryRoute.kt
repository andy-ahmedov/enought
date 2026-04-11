package com.andyahmedov.enought.ui.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.andyahmedov.enought.collection.notifications.NotificationAccessSettingsLauncher
import com.andyahmedov.enought.collection.notifications.NotificationAccessStatusReader
import com.andyahmedov.enought.collection.notifications.rememberNotificationAccessState
import com.andyahmedov.enought.domain.model.HistoryPeriod
import com.andyahmedov.enought.domain.model.HistoryPeriodSnapshot
import com.andyahmedov.enought.domain.usecase.ObserveHistoryPeriodSnapshotUseCase

@Composable
fun HistoryRoute(
    observeHistoryPeriodSnapshotUseCase: ObserveHistoryPeriodSnapshotUseCase,
    notificationAccessStatusReader: NotificationAccessStatusReader,
    notificationAccessSettingsLauncher: NotificationAccessSettingsLauncher,
    onNavigateBack: () -> Unit,
    onOpenReview: () -> Unit,
) {
    val context = LocalContext.current
    var selectedPeriod by rememberSaveable { mutableStateOf(HistoryPeriod.LAST_7_DAYS) }
    val snapshotFlow = remember(observeHistoryPeriodSnapshotUseCase, selectedPeriod) {
        observeHistoryPeriodSnapshotUseCase(selectedPeriod)
    }
    val snapshot by snapshotFlow.collectAsState(initial = null as HistoryPeriodSnapshot?)
    val hasNotificationAccess = rememberNotificationAccessState(
        notificationAccessStatusReader = notificationAccessStatusReader,
    )
    val uiState = remember(selectedPeriod, hasNotificationAccess, snapshot) {
        HistoryUiStateFactory.create(
            selectedPeriod = selectedPeriod,
            hasNotificationAccess = hasNotificationAccess,
            snapshot = snapshot,
        )
    }

    HistoryScreen(
        uiState = uiState,
        onSelectPeriod = { period ->
            selectedPeriod = period
        },
        onNavigateBack = onNavigateBack,
        onOpenReview = onOpenReview,
        onOpenNotificationAccessSettings = {
            notificationAccessSettingsLauncher.open(context)
        },
    )
}
