package com.andyahmedov.enought.app.navigation

sealed interface AppDestination {
    val route: String
}

data object TodayDestination : AppDestination {
    override val route: String = "today"
}

data object HistoryDestination : AppDestination {
    override val route: String = "history"
}

data object OnboardingDestination : AppDestination {
    override val route: String = "onboarding"
}

data object RawNotificationsDestination : AppDestination {
    override val route: String = "raw-notifications"
}

data object ReviewDestination : AppDestination {
    override val route: String = "review"
}

data object SettingsDestination : AppDestination {
    override val route: String = "settings"
}
