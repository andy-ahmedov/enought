package com.andyahmedov.enought.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.andyahmedov.enought.collection.notifications.NotificationAccessSettingsLauncher
import com.andyahmedov.enought.collection.notifications.NotificationAccessStatusReader
import com.andyahmedov.enought.collection.notifications.rememberNotificationAccessState

@Composable
fun OnboardingRoute(
    notificationAccessStatusReader: NotificationAccessStatusReader,
    notificationAccessSettingsLauncher: NotificationAccessSettingsLauncher,
    onAccessGranted: () -> Unit,
) {
    val context = LocalContext.current
    var hasOpenedNotificationAccessSettings by rememberSaveable {
        mutableStateOf(false)
    }
    val hasNotificationAccess = rememberNotificationAccessState(
        notificationAccessStatusReader = notificationAccessStatusReader,
    )

    LaunchedEffect(hasNotificationAccess) {
        if (shouldNavigateToToday(hasNotificationAccess)) {
            onAccessGranted()
        }
    }

    OnboardingScreen(
        uiState = onboardingUiState(
            hasNotificationAccess = hasNotificationAccess,
            hasOpenedNotificationAccessSettings = hasOpenedNotificationAccessSettings,
        ),
        onOpenNotificationAccessSettings = {
            hasOpenedNotificationAccessSettings = true
            notificationAccessSettingsLauncher.open(context)
        },
    )
}

internal fun onboardingUiState(
    hasNotificationAccess: Boolean?,
    hasOpenedNotificationAccessSettings: Boolean,
): OnboardingUiState {
    return when (hasNotificationAccess) {
        null, true -> OnboardingUiState.CheckingAccess
        false -> {
            if (hasOpenedNotificationAccessSettings) {
                OnboardingUiState.PermissionStillRequired
            } else {
                OnboardingUiState.PermissionRequired
            }
        }
    }
}

internal fun shouldNavigateToToday(
    hasNotificationAccess: Boolean?,
): Boolean {
    return hasNotificationAccess == true
}
