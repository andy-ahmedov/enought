package com.andyahmedov.enought.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.andyahmedov.enought.app.appContainer
import com.andyahmedov.enought.ui.debug.RawNotificationsRoute
import com.andyahmedov.enought.ui.onboarding.OnboardingRoute
import com.andyahmedov.enought.ui.review.ReviewRoute
import com.andyahmedov.enought.ui.settings.SettingsRoute
import com.andyahmedov.enought.ui.today.TodayRoute

@Composable
fun EnoughtNavHost() {
    val navController = rememberNavController()
    val appContainer = LocalContext.current.appContainer
    val startDestination = remember(appContainer.notificationAccessStatusReader) {
        resolveStartDestination(
            hasNotificationAccess = appContainer.notificationAccessStatusReader.hasNotificationAccess(),
        )
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(route = OnboardingDestination.route) {
            OnboardingRoute(
                notificationAccessStatusReader = appContainer.notificationAccessStatusReader,
                notificationAccessSettingsLauncher = appContainer.notificationAccessSettingsLauncher,
                onAccessGranted = {
                    navController.navigate(TodayDestination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            inclusive = true
                        }
                    }
                },
            )
        }

        composable(route = TodayDestination.route) {
            TodayRoute(
                observeTodayPaymentEventsUseCase = appContainer.observeTodayPaymentEventsUseCase,
                observeTodaySummaryUseCase = appContainer.observeTodaySummaryUseCase,
                observeWidgetPrivateModeUseCase = appContainer.observeWidgetPrivateModeUseCase,
                setWidgetPrivateModeUseCase = appContainer.setWidgetPrivateModeUseCase,
                notificationAccessStatusReader = appContainer.notificationAccessStatusReader,
                notificationAccessSettingsLauncher = appContainer.notificationAccessSettingsLauncher,
                onOpenRawNotifications = {
                    navController.navigate(RawNotificationsDestination.route)
                },
                onOpenReview = {
                    navController.navigate(ReviewDestination.route)
                },
                onOpenSettings = {
                    navController.navigate(SettingsDestination.route)
                },
            )
        }

        composable(route = RawNotificationsDestination.route) {
            RawNotificationsRoute(
                rawNotificationEventRepository = appContainer.rawNotificationEventRepository,
                buildDiagnosticsReportUseCase = appContainer.buildDiagnosticsReportUseCase,
                diagnosticsLogClipboardWriter = appContainer.diagnosticsLogClipboardWriter,
                diagnosticsLogShareLauncher = appContainer.diagnosticsLogShareLauncher,
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(route = ReviewDestination.route) {
            ReviewRoute(
                observeTodayReviewItemsUseCase = appContainer.observeTodayReviewItemsUseCase,
                confirmPaymentEventUseCase = appContainer.confirmPaymentEventUseCase,
                correctPaymentAmountUseCase = appContainer.correctPaymentAmountUseCase,
                dismissPaymentEventUseCase = appContainer.dismissPaymentEventUseCase,
                mergeDuplicateConflictUseCase = appContainer.mergeDuplicateConflictUseCase,
                keepDuplicateConflictSeparateUseCase = appContainer.keepDuplicateConflictSeparateUseCase,
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }

        composable(route = SettingsDestination.route) {
            SettingsRoute(
                observeDailyLimitUseCase = appContainer.observeDailyLimitUseCase,
                setDailyLimitUseCase = appContainer.setDailyLimitUseCase,
                clearDailyLimitUseCase = appContainer.clearDailyLimitUseCase,
                onNavigateBack = {
                    navController.popBackStack()
                },
            )
        }
    }
}

internal fun resolveStartDestination(
    hasNotificationAccess: Boolean,
): String {
    return if (hasNotificationAccess) {
        TodayDestination.route
    } else {
        OnboardingDestination.route
    }
}
