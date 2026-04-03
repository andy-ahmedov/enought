package com.andyahmedov.enought.collection.notifications

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationAccessStatusReaderTest {
    private val expectedComponentName =
        "com.andyahmedov.enought/com.andyahmedov.enought.collection.notifications.RawNotificationListenerService"

    @Test
    fun `returns true when expected listener component is enabled`() {
        val enabledListeners = listOf(
            "com.example/.OtherListener",
            expectedComponentName,
        ).joinToString(separator = ":")

        assertTrue(
            isNotificationListenerEnabled(
                enabledNotificationListeners = enabledListeners,
                expectedComponentName = expectedComponentName,
            ),
        )
    }

    @Test
    fun `returns false when expected listener component is missing`() {
        val enabledListeners = listOf(
            "com.example/.OtherListener",
            "com.example/.AnotherListener",
        ).joinToString(separator = ":")

        assertFalse(
            isNotificationListenerEnabled(
                enabledNotificationListeners = enabledListeners,
                expectedComponentName = expectedComponentName,
            ),
        )
    }

    @Test
    fun `returns false when enabled listener string is empty`() {
        assertFalse(
            isNotificationListenerEnabled(
                enabledNotificationListeners = null,
                expectedComponentName = expectedComponentName,
            ),
        )
    }
}
