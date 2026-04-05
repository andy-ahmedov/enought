package com.andyahmedov.enought.ui.debug

import com.andyahmedov.enought.domain.model.RawNotificationEvent
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RawNotificationsUiStateTest {
    @Test
    fun `raw notification maps to debug list item`() {
        val rawEvent = RawNotificationEvent(
            id = "raw-1",
            sourcePackage = "ru.nspk.mirpay",
            postedAt = Instant.parse("2026-03-31T13:05:00Z"),
            title = "Mir Pay",
            text = "409 RUB debited",
            subText = null,
            bigText = "Coffee Shop",
            extrasJson = "{\"ignored\":true}",
            payloadHash = "hash-1",
        )

        val listItem = rawEvent.toListItem()

        assertEquals("raw-1", listItem.id)
        assertEquals("ru.nspk.mirpay", listItem.sourcePackage)
        assertEquals("2026-03-31T13:05:00Z", listItem.postedAt)
        assertEquals("Mir Pay", listItem.title)
        assertEquals("409 RUB debited", listItem.text)
        assertNull(listItem.subText)
        assertEquals("Coffee Shop", listItem.bigText)
        assertEquals("hash-1", listItem.payloadHash)
    }

    @Test
    fun `raw notifications ui state keeps Mir Pay items only`() {
        val uiState = rawNotificationsUiState(
            listOf(
                rawEvent(id = "mir-1", sourcePackage = "ru.nspk.mirpay"),
                rawEvent(id = "sber-1", sourcePackage = "ru.sberbankmobile"),
                rawEvent(id = "alfa-1", sourcePackage = "ru.alfabank.mobile.android"),
            ),
        )

        assertEquals(listOf("mir-1"), uiState.items.map { item -> item.id })
    }

    private fun rawEvent(
        id: String,
        sourcePackage: String,
    ): RawNotificationEvent {
        return RawNotificationEvent(
            id = id,
            sourcePackage = sourcePackage,
            postedAt = Instant.parse("2026-03-31T13:05:00Z"),
            title = "Title",
            text = "Text",
            subText = null,
            bigText = "Big text",
            extrasJson = "{\"ignored\":true}",
            payloadHash = "hash-$id",
        )
    }
}
