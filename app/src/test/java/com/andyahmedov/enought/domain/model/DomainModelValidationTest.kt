package com.andyahmedov.enought.domain.model

import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertNull
import org.junit.Test

class DomainModelValidationTest {
    @Test(expected = IllegalArgumentException::class)
    fun rawNotificationEvent_rejectsBlankSourcePackage() {
        RawNotificationEvent(
            id = "raw-1",
            sourcePackage = "",
            postedAt = Instant.parse("2026-03-31T08:30:00Z"),
            title = "Mir Pay",
            text = "Payment completed",
            subText = null,
            bigText = null,
            extrasJson = null,
            payloadHash = "hash-1",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun paymentCandidate_rejectsBlankRawSourceId() {
        PaymentCandidate(
            amountMinor = 34900L,
            currency = "RUB",
            paidAt = Instant.parse("2026-03-31T08:30:00Z"),
            merchantName = "Coffee Shop",
            sourceKind = PaymentSourceKind.MIR_PAY,
            paymentChannel = PaymentChannel.PHONE,
            rawSourceId = "",
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun dailySpendSummary_rejectsNegativePaymentsCount() {
        DailySpendSummary(
            date = LocalDate.parse("2026-03-31"),
            totalAmountMinor = 34900L,
            paymentsCount = -1,
            lastPaymentAmountMinor = 34900L,
            limitAmountMinor = 150000L,
            remainingAmountMinor = 115100L,
            limitWarningLevel = null,
            hasLowConfidenceItems = false,
        )
    }

    @Test
    fun paymentEvent_keepsTypedStateAndSourceIds() {
        val paymentEvent = PaymentEvent(
            id = "payment-1",
            amountMinor = 34900L,
            currency = "RUB",
            paidAt = Instant.parse("2026-03-31T08:30:00Z"),
            merchantName = null,
            sourceKind = PaymentSourceKind.BANK,
            paymentChannel = PaymentChannel.PHONE,
            confidence = ConfidenceLevel.MEDIUM,
            status = PaymentStatus.SUSPECTED,
            userEdited = false,
            sourceIds = listOf("raw-1", "raw-2"),
        )

        assertEquals(PaymentStatus.SUSPECTED, paymentEvent.status)
        assertEquals(2, paymentEvent.sourceIds.size)
        assertTrue(paymentEvent.sourceIds.contains("raw-2"))
    }

    @Test
    fun dailySpendSummary_acceptsNullableLimitFields() {
        val summary = DailySpendSummary(
            date = LocalDate.parse("2026-03-31"),
            totalAmountMinor = 34900L,
            paymentsCount = 1,
            lastPaymentAmountMinor = 34900L,
            limitAmountMinor = null,
            remainingAmountMinor = null,
            limitWarningLevel = null,
            hasLowConfidenceItems = false,
        )

        assertNull(summary.limitAmountMinor)
        assertNull(summary.limitWarningLevel)
    }
}
