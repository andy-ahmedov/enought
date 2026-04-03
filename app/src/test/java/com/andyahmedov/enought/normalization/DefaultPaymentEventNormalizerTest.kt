package com.andyahmedov.enought.normalization

import com.andyahmedov.enought.domain.model.ConfidenceHint
import com.andyahmedov.enought.domain.model.ConfidenceLevel
import com.andyahmedov.enought.domain.model.PaymentCandidate
import com.andyahmedov.enought.domain.model.PaymentChannel
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.PaymentStatus
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DefaultPaymentEventNormalizerTest {
    private val normalizer = DefaultPaymentEventNormalizer()

    @Test
    fun `normalize maps strong Mir Pay signal to high confirmed event`() {
        val event = requireNotNull(
            normalizer.normalize(
                paymentCandidate(
                    sourceKind = PaymentSourceKind.MIR_PAY,
                    paymentChannel = PaymentChannel.PHONE,
                    confidenceHints = setOf(
                        ConfidenceHint.MIR_PAY_MARKER,
                        ConfidenceHint.PHONE_PAYMENT_MARKER,
                        ConfidenceHint.AMOUNT_PARSED,
                        ConfidenceHint.TIMESTAMP_PARSED,
                    ),
                ),
            ),
        )

        assertEquals("raw-1", event.id)
        assertEquals(ConfidenceLevel.HIGH, event.confidence)
        assertEquals(PaymentStatus.CONFIRMED, event.status)
        assertEquals(listOf("raw-1"), event.sourceIds)
        assertEquals(false, event.userEdited)
    }

    @Test
    fun `normalize maps bank candidate to medium confirmed event`() {
        val event = requireNotNull(
            normalizer.normalize(
                paymentCandidate(
                    sourceKind = PaymentSourceKind.BANK,
                    paymentChannel = PaymentChannel.UNKNOWN,
                    confidenceHints = setOf(
                        ConfidenceHint.AMOUNT_PARSED,
                        ConfidenceHint.TIMESTAMP_PARSED,
                    ),
                ),
            ),
        )

        assertEquals(ConfidenceLevel.MEDIUM, event.confidence)
        assertEquals(PaymentStatus.CONFIRMED, event.status)
        assertEquals(PaymentSourceKind.BANK, event.sourceKind)
        assertEquals(PaymentChannel.UNKNOWN, event.paymentChannel)
    }

    @Test
    fun `normalize maps reversal to low suspected event`() {
        val event = requireNotNull(
            normalizer.normalize(
                paymentCandidate(
                    confidenceHints = setOf(
                        ConfidenceHint.MIR_PAY_MARKER,
                        ConfidenceHint.PHONE_PAYMENT_MARKER,
                        ConfidenceHint.AMOUNT_PARSED,
                        ConfidenceHint.TIMESTAMP_PARSED,
                        ConfidenceHint.POSSIBLE_REVERSAL,
                    ),
                ),
            ),
        )

        assertEquals(ConfidenceLevel.LOW, event.confidence)
        assertEquals(PaymentStatus.SUSPECTED, event.status)
    }

    @Test
    fun `normalize trims blank merchant name to null and uppercases currency`() {
        val event = requireNotNull(
            normalizer.normalize(
                paymentCandidate(
                    currency = " rub ",
                    merchantName = "   ",
                    confidenceHints = setOf(
                        ConfidenceHint.AMOUNT_PARSED,
                        ConfidenceHint.TIMESTAMP_PARSED,
                    ),
                ),
            ),
        )

        assertEquals("RUB", event.currency)
        assertNull(event.merchantName)
    }

    @Test
    fun `normalize returns null when required fields are missing`() {
        assertNull(normalizer.normalize(paymentCandidate(amountMinor = null)))
        assertNull(normalizer.normalize(paymentCandidate(currency = null)))
        assertNull(normalizer.normalize(paymentCandidate(paidAt = null)))
    }

    private fun paymentCandidate(
        amountMinor: Long? = 34900L,
        currency: String? = "RUB",
        paidAt: Instant? = Instant.parse("2026-03-31T10:15:30Z"),
        merchantName: String? = null,
        sourceKind: PaymentSourceKind = PaymentSourceKind.MIR_PAY,
        paymentChannel: PaymentChannel = PaymentChannel.PHONE,
        confidenceHints: Set<ConfidenceHint> = emptySet(),
    ): PaymentCandidate {
        return PaymentCandidate(
            amountMinor = amountMinor,
            currency = currency,
            paidAt = paidAt,
            merchantName = merchantName,
            sourceKind = sourceKind,
            paymentChannel = paymentChannel,
            rawSourceId = "raw-1",
            confidenceHints = confidenceHints,
        )
    }
}
