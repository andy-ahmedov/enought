package com.andyahmedov.enought.parsing.rules

import com.andyahmedov.enought.domain.model.ConfidenceHint
import com.andyahmedov.enought.domain.model.PaymentChannel
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.RawNotificationEvent
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MirPayNotificationParserTest {
    private val parser = MirPayNotificationParser()

    @Test
    fun `canParse accepts Mir Pay event with ruble amount`() {
        val rawEvent = rawNotificationEvent(
            text = "Покупка 349 ₽",
        )

        assertTrue(parser.canParse(rawEvent))
    }

    @Test
    fun `parse maps Mir Pay event to payment candidate`() {
        val rawEvent = rawNotificationEvent(
            text = "Списано 1 249,50 ₽",
        )

        val candidate = requireNotNull(parser.parse(rawEvent))

        assertEquals(124950L, candidate.amountMinor)
        assertEquals("RUB", candidate.currency)
        assertEquals(rawEvent.postedAt, candidate.paidAt)
        assertEquals(null, candidate.merchantName)
        assertEquals(PaymentSourceKind.MIR_PAY, candidate.sourceKind)
        assertEquals(PaymentChannel.PHONE, candidate.paymentChannel)
        assertEquals(rawEvent.id, candidate.rawSourceId)
        assertTrue(candidate.confidenceHints.contains(ConfidenceHint.MIR_PAY_MARKER))
        assertTrue(candidate.confidenceHints.contains(ConfidenceHint.PHONE_PAYMENT_MARKER))
        assertTrue(candidate.confidenceHints.contains(ConfidenceHint.AMOUNT_PARSED))
        assertTrue(candidate.confidenceHints.contains(ConfidenceHint.TIMESTAMP_PARSED))
    }

    @Test
    fun `parse supports dot decimal with RUB marker`() {
        val candidate = requireNotNull(
            parser.parse(
                rawNotificationEvent(
                    text = "Покупка на 1249.50 RUB",
                ),
            ),
        )

        assertEquals(124950L, candidate.amountMinor)
    }

    @Test
    fun `parse adds reversal hint for refund markers`() {
        val candidate = requireNotNull(
            parser.parse(
                rawNotificationEvent(
                    text = "Возврат 349 ₽",
                ),
            ),
        )

        assertTrue(candidate.confidenceHints.contains(ConfidenceHint.POSSIBLE_REVERSAL))
    }

    @Test
    fun `parse returns null for unsupported package`() {
        val candidate = parser.parse(
            rawNotificationEvent(
                sourcePackage = "com.example.bank",
                text = "Покупка 349 ₽",
            ),
        )

        assertNull(candidate)
    }

    @Test
    fun `canParse rejects event without supported currency marker`() {
        val rawEvent = rawNotificationEvent(
            text = "Покупка 349",
        )

        assertFalse(parser.canParse(rawEvent))
        assertNull(parser.parse(rawEvent))
    }

    @Test
    fun `canParse ignores extrasJson when visible text has no amount`() {
        val rawEvent = rawNotificationEvent(
            text = "Оплата прошла",
            extrasJson = """{"text":"349 ₽"}""",
        )

        assertFalse(parser.canParse(rawEvent))
        assertNull(parser.parse(rawEvent))
    }

    private fun rawNotificationEvent(
        sourcePackage: String = "ru.nspk.mirpay",
        text: String?,
        extrasJson: String? = """{"notificationId":"42"}""",
    ): RawNotificationEvent {
        return RawNotificationEvent(
            id = "raw-1",
            sourcePackage = sourcePackage,
            postedAt = Instant.parse("2026-03-31T10:15:30Z"),
            title = "Mir Pay",
            text = text,
            subText = null,
            bigText = "Карта *4242",
            extrasJson = extrasJson,
            payloadHash = "hash-1",
        )
    }
}
