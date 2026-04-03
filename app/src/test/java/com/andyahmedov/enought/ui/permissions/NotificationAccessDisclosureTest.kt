package com.andyahmedov.enought.ui.permissions

import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationAccessDisclosureTest {
    @Test
    fun `disclosure keeps supported sources aligned with permission copy`() {
        val disclosure = notificationAccessDisclosure()

        assertTrue(disclosure.supportedSourcesText.contains("Mir Pay"))
        assertTrue(disclosure.supportedSourcesText.contains("raw samples"))
        assertTrue(disclosure.todayNoPermissionText.contains("Mir Pay"))
        assertTrue(!disclosure.todayNoPermissionText.contains("Alfa-Bank"))
    }

    @Test
    fun `disclosure keeps local first and privacy messaging explicit`() {
        val disclosure = notificationAccessDisclosure()

        assertTrue(disclosure.localProcessingText.contains("device"))
        assertTrue(disclosure.localProcessingText.contains("private mode"))
        assertTrue(disclosure.limitationsText.contains("cloud sync"))
        assertTrue(disclosure.limitationsText.contains("bank account"))
    }
}
