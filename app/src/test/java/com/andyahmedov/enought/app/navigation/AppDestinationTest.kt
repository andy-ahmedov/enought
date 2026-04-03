package com.andyahmedov.enought.app.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class AppDestinationTest {
    @Test
    fun todayRoute_staysStable() {
        assertEquals("today", TodayDestination.route)
    }

    @Test
    fun rawNotificationsRoute_staysStable() {
        assertEquals("raw-notifications", RawNotificationsDestination.route)
    }

    @Test
    fun reviewRoute_staysStable() {
        assertEquals("review", ReviewDestination.route)
    }

    @Test
    fun settingsRoute_staysStable() {
        assertEquals("settings", SettingsDestination.route)
    }
}
