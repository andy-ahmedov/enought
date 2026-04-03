package com.andyahmedov.enought.collection.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationAccessSettingsLauncherTest {
    @Test
    fun `creates notification listener settings launch spec with expected action`() {
        val launchSpec = notificationAccessSettingsLaunchSpec(
            shouldAddNewTaskFlag = false,
        )

        assertEquals(NOTIFICATION_LISTENER_SETTINGS_ACTION, launchSpec.action)
    }

    @Test
    fun `keeps new task flag when requested`() {
        val launchSpec = notificationAccessSettingsLaunchSpec(
            shouldAddNewTaskFlag = true,
        )

        assertTrue(launchSpec.shouldAddNewTaskFlag)
    }

    @Test
    fun `does not add new task flag when not requested`() {
        val launchSpec = notificationAccessSettingsLaunchSpec(
            shouldAddNewTaskFlag = false,
        )

        assertFalse(launchSpec.shouldAddNewTaskFlag)
    }
}
