package com.andyahmedov.enought.ui.onboarding

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingRouteTest {
    @Test
    fun `shows checking state while notification access is still unknown`() {
        assertEquals(
            OnboardingUiState.CheckingAccess,
            onboardingUiState(
                hasNotificationAccess = null,
                hasOpenedNotificationAccessSettings = false,
            ),
        )
    }

    @Test
    fun `shows permission required state when access is missing`() {
        assertEquals(
            OnboardingUiState.PermissionRequired,
            onboardingUiState(
                hasNotificationAccess = false,
                hasOpenedNotificationAccessSettings = false,
            ),
        )
    }

    @Test
    fun `shows retry state when access is still missing after opening settings`() {
        assertEquals(
            OnboardingUiState.PermissionStillRequired,
            onboardingUiState(
                hasNotificationAccess = false,
                hasOpenedNotificationAccessSettings = true,
            ),
        )
    }

    @Test
    fun `navigates to today when access becomes available`() {
        assertTrue(shouldNavigateToToday(hasNotificationAccess = true))
    }

    @Test
    fun `stays on onboarding when access is still missing`() {
        assertFalse(shouldNavigateToToday(hasNotificationAccess = false))
    }
}
