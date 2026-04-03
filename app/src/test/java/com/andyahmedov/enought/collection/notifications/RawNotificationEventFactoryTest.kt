package com.andyahmedov.enought.collection.notifications

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RawNotificationEventFactoryTest {
    private val factory = RawNotificationEventFactory()

    @Test
    fun `same snapshot produces stable payload hash and id`() {
        val snapshot = sampleSnapshot()

        val first = factory.fromSnapshot(snapshot)
        val second = factory.fromSnapshot(snapshot)

        assertEquals(first.payloadHash, second.payloadHash)
        assertEquals(first.id, second.id)
        assertEquals(first.extrasJson, second.extrasJson)
    }

    @Test
    fun `changing notification text changes payload hash`() {
        val baseline = sampleSnapshot()
        val changed = baseline.copy(text = "410 RUB debited")

        val baselineEvent = factory.fromSnapshot(baseline)
        val changedEvent = factory.fromSnapshot(changed)

        assertNotEquals(baselineEvent.payloadHash, changedEvent.payloadHash)
        assertNotEquals(baselineEvent.id, changedEvent.id)
    }

    @Test
    fun `canonical payload json stays limited to selected fields`() {
        val snapshot = sampleSnapshot()

        val payloadJson = factory.buildCanonicalPayloadJson(snapshot)

        assertTrue(payloadJson.contains("\"notificationId\":\"42\""))
        assertTrue(payloadJson.contains("\"channelId\":\"payments\""))
        assertTrue(payloadJson.contains("\"title\":\"Mir Pay\""))
        assertTrue(payloadJson.contains("\"bigText\":\"Card ending 4242\""))
        assertTrue(payloadJson.contains("\"notificationTag\":null"))
        assertTrue(payloadJson.startsWith("{"))
        assertTrue(payloadJson.endsWith("}"))
    }

    private fun sampleSnapshot(): NotificationSnapshot {
        return NotificationSnapshot(
            sourcePackage = "ru.mirpay",
            postedAt = Instant.parse("2026-03-31T10:15:30Z"),
            notificationId = 42,
            notificationTag = null,
            channelId = "payments",
            category = "msg",
            title = "Mir Pay",
            text = "409 RUB debited",
            subText = "Coffee Shop",
            bigText = "Card ending 4242",
        )
    }
}
