package com.andyahmedov.enought.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class EnoughtNavHostTest {
    @Test
    fun `starts in onboarding when notification access is missing`() {
        assertEquals(
            OnboardingDestination.route,
            resolveStartDestination(hasNotificationAccess = false),
        )
    }

    @Test
    fun `starts in today when notification access is granted`() {
        assertEquals(
            TodayDestination.route,
            resolveStartDestination(hasNotificationAccess = true),
        )
    }
}
