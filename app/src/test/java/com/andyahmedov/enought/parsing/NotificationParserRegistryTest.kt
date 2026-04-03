package com.andyahmedov.enought.parsing

import com.andyahmedov.enought.domain.model.RawNotificationEvent
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NotificationParserRegistryTest {
    private val registry = NotificationParserRegistry.default()

    @Test
    fun `registry returns parsed candidate for supported Mir Pay event`() {
        val candidate = registry.parse(
            rawNotificationEvent(
                sourcePackage = "ru.nspk.mirpay",
                text = "Покупка 349 ₽",
            ),
        )

        requireNotNull(candidate)

        assertEquals(34900L, candidate.amountMinor)
        assertEquals(PaymentSourceKind.MIR_PAY, candidate.sourceKind)
    }

    @Test
    fun `registry returns null for Alfa-Bank promotional notification`() {
        val candidate = registry.parse(
            rawNotificationEvent(
                sourcePackage = "ru.alfabank.mobile.android",
                title = "Альфа-Банк",
                text = "Откройте счёт для инвестиций и получите портфель акций до 15 000 ₽ за первые сделки",
            ),
        )

        assertNull(candidate)
    }

    @Test
    fun `registry returns null for allowlisted Sber notification while bank parsing is disabled`() {
        val candidate = registry.parse(
            rawNotificationEvent(
                sourcePackage = "ru.sberbankmobile",
                title = "СберБанк",
                text = "Оплата 1 249,50 ₽",
            ),
        )

        assertNull(candidate)
    }

    @Test
    fun `registry returns null when no parser matches`() {
        val candidate = registry.parse(
            rawNotificationEvent(
                sourcePackage = "com.example.other",
                text = "Покупка 349 ₽",
            ),
        )

        assertNull(candidate)
    }

    private fun rawNotificationEvent(
        sourcePackage: String,
        title: String = "Mir Pay",
        text: String?,
    ): RawNotificationEvent {
        return RawNotificationEvent(
            id = "raw-1",
            sourcePackage = sourcePackage,
            postedAt = Instant.parse("2026-03-31T08:30:00Z"),
            title = title,
            text = text,
            subText = null,
            bigText = null,
            extrasJson = """{"text":"349 ₽"}""",
            payloadHash = "hash-1",
        )
    }
}
