package com.andyahmedov.enought.ui.history

import com.andyahmedov.enought.domain.model.ConfidenceLevel
import com.andyahmedov.enought.domain.model.HistoryDaySummary
import com.andyahmedov.enought.domain.model.HistoryPeriod
import com.andyahmedov.enought.domain.model.HistoryPeriodSnapshot
import com.andyahmedov.enought.domain.model.PaymentChannel
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.PaymentStatus
import com.andyahmedov.enought.domain.model.PeriodSpendSummary
import java.time.Instant
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryUiStateTest {
    @Test
    fun `create returns no permission when access is missing`() {
        val state = HistoryUiStateFactory.create(
            selectedPeriod = HistoryPeriod.LAST_7_DAYS,
            hasNotificationAccess = false,
            snapshot = null,
        )

        assertEquals(
            HistoryUiState.NoPermission(selectedPeriod = HistoryPeriod.LAST_7_DAYS),
            state,
        )
    }

    @Test
    fun `create returns empty state when period has no confirmed items`() {
        val state = HistoryUiStateFactory.create(
            selectedPeriod = HistoryPeriod.LAST_30_DAYS,
            hasNotificationAccess = true,
            snapshot = HistoryPeriodSnapshot(
                period = HistoryPeriod.LAST_30_DAYS,
                summary = PeriodSpendSummary(
                    startDate = LocalDate.parse("2026-03-03"),
                    endDateInclusive = LocalDate.parse("2026-04-01"),
                    totalAmountMinor = 0L,
                    paymentsCount = 0,
                    lastPaymentAmountMinor = null,
                    hasLowConfidenceItems = true,
                ),
                paymentEvents = emptyList(),
                daySummaries = emptyList(),
            ),
        )

        assertEquals(
            HistoryUiState.Empty(
                selectedPeriod = HistoryPeriod.LAST_30_DAYS,
                hasLowConfidenceItems = true,
            ),
            state,
        )
    }

    @Test
    fun `create maps today snapshot to payment items`() {
        val state = HistoryUiStateFactory.create(
            selectedPeriod = HistoryPeriod.TODAY,
            hasNotificationAccess = true,
            snapshot = HistoryPeriodSnapshot(
                period = HistoryPeriod.TODAY,
                summary = PeriodSpendSummary(
                    startDate = LocalDate.parse("2026-04-01"),
                    endDateInclusive = LocalDate.parse("2026-04-01"),
                    totalAmountMinor = 34900L,
                    paymentsCount = 1,
                    lastPaymentAmountMinor = 34900L,
                    hasLowConfidenceItems = false,
                ),
                paymentEvents = listOf(
                    PaymentEvent(
                        id = "payment-1",
                        amountMinor = 34900L,
                        currency = "RUB",
                        paidAt = Instant.parse("2026-04-01T08:30:00Z"),
                        merchantName = "Coffee Shop",
                        sourceKind = PaymentSourceKind.MIR_PAY,
                        paymentChannel = PaymentChannel.PHONE,
                        confidence = ConfidenceLevel.MEDIUM,
                        status = PaymentStatus.CONFIRMED,
                        userEdited = false,
                        sourceIds = listOf("raw-1"),
                    ),
                ),
                daySummaries = emptyList(),
            ),
        )

        val readyState = state as HistoryUiState.Ready
        assertEquals(1, readyState.paymentItems.size)
        assertTrue(readyState.dayItems.isEmpty())
        assertEquals("Coffee Shop", readyState.paymentItems.single().title)
    }

    @Test
    fun `create maps non-today snapshot to grouped day items`() {
        val state = HistoryUiStateFactory.create(
            selectedPeriod = HistoryPeriod.LAST_7_DAYS,
            hasNotificationAccess = true,
            snapshot = HistoryPeriodSnapshot(
                period = HistoryPeriod.LAST_7_DAYS,
                summary = PeriodSpendSummary(
                    startDate = LocalDate.parse("2026-03-26"),
                    endDateInclusive = LocalDate.parse("2026-04-01"),
                    totalAmountMinor = 82900L,
                    paymentsCount = 2,
                    lastPaymentAmountMinor = 34900L,
                    hasLowConfidenceItems = false,
                ),
                paymentEvents = emptyList(),
                daySummaries = listOf(
                    HistoryDaySummary(
                        date = LocalDate.parse("2026-04-01"),
                        totalAmountMinor = 34900L,
                        paymentsCount = 1,
                        lastPaymentAmountMinor = 34900L,
                    ),
                ),
            ),
        )

        val readyState = state as HistoryUiState.Ready
        assertTrue(readyState.paymentItems.isEmpty())
        assertEquals(1, readyState.dayItems.size)
        assertEquals(LocalDate.parse("2026-04-01"), readyState.dayItems.single().date)
    }
}
