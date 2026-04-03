package com.andyahmedov.enought.ui.today

import com.andyahmedov.enought.common.toRubDisplayString
import com.andyahmedov.enought.domain.model.DailyLimitWarningLevel
import com.andyahmedov.enought.domain.model.DailySpendSummary
import com.andyahmedov.enought.domain.model.ConfidenceLevel
import com.andyahmedov.enought.domain.model.PaymentChannel
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.PaymentStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayUiStateTest {
    @Test
    fun `returns loading while notification access state is still unknown`() {
        val uiState = TodayUiStateFactory.create(
            hasNotificationAccess = null,
            summary = null,
            events = null,
            isWidgetPrivateModeEnabled = false,
        )

        assertEquals(TodayUiState.Loading, uiState)
    }

    @Test
    fun `returns no permission even when summary is already available`() {
        val uiState = TodayUiStateFactory.create(
            hasNotificationAccess = false,
            summary = summary(paymentsCount = 2),
            events = listOf(paymentEvent(id = "event-1")),
            isWidgetPrivateModeEnabled = false,
        )

        assertEquals(TodayUiState.NoPermission, uiState)
    }

    @Test
    fun `returns loading while summary has not emitted yet`() {
        val uiState = TodayUiStateFactory.create(
            hasNotificationAccess = true,
            summary = null,
            events = null,
            isWidgetPrivateModeEnabled = false,
        )

        assertEquals(TodayUiState.Loading, uiState)
    }

    @Test
    fun `returns empty when there are no confirmed payments`() {
        val uiState = TodayUiStateFactory.create(
            hasNotificationAccess = true,
            summary = summary(
                paymentsCount = 0,
                totalAmountMinor = 0L,
                lastPaymentAmountMinor = null,
                hasLowConfidenceItems = true,
            ),
            events = emptyList(),
            isWidgetPrivateModeEnabled = true,
        )

        assertEquals(
            TodayUiState.Empty(
                hasLowConfidenceItems = true,
                limitAmountMinor = null,
                remainingAmountMinor = null,
                limitWarningLevel = null,
                isWidgetPrivateModeEnabled = true,
            ),
            uiState,
        )
    }

    @Test
    fun `returns ready with summary values when confirmed payments exist`() {
        val event = paymentEvent(
            id = "event-1",
            amountMinor = 29900L,
            merchantName = "Coffee Point",
        )
        val uiState = TodayUiStateFactory.create(
            hasNotificationAccess = true,
            summary = summary(
                paymentsCount = 1,
                totalAmountMinor = 29900L,
                lastPaymentAmountMinor = 29900L,
                hasLowConfidenceItems = true,
            ),
            events = listOf(event),
            isWidgetPrivateModeEnabled = true,
        )

        assertEquals(
            TodayUiState.Ready(
                totalAmountMinor = 29900L,
                paymentsCount = 1,
                lastPaymentAmountMinor = 29900L,
                limitAmountMinor = null,
                remainingAmountMinor = null,
                limitWarningLevel = null,
                hasLowConfidenceItems = true,
                isWidgetPrivateModeEnabled = true,
                events = listOf(
                    TodayPaymentEventListItem(
                        id = "event-1",
                        amountMinor = 29900L,
                        paidAt = Instant.parse("2026-04-01T10:15:00Z"),
                        title = "Coffee Point",
                        sourceKind = PaymentSourceKind.BANK,
                        userEdited = false,
                    ),
                ),
            ),
            uiState,
        )
    }

    @Test
    fun `returns loading when ready summary exists but events have not emitted yet`() {
        val uiState = TodayUiStateFactory.create(
            hasNotificationAccess = true,
            summary = summary(
                paymentsCount = 1,
                totalAmountMinor = 29900L,
                lastPaymentAmountMinor = 29900L,
            ),
            events = null,
            isWidgetPrivateModeEnabled = false,
        )

        assertEquals(TodayUiState.Loading, uiState)
    }

    @Test
    fun `list item uses fallback title when merchant is missing`() {
        val item = paymentEvent(
            id = "event-1",
            merchantName = null,
            sourceKind = PaymentSourceKind.MIR_PAY,
        ).toTodayPaymentEventListItem()

        assertEquals("Mir Pay payment", item.title)
        assertEquals(PaymentSourceKind.MIR_PAY, item.sourceKind)
    }

    @Test
    fun `formats local time for today list item`() {
        val formatted = Instant.parse("2026-04-01T10:15:00Z").toLocalTimeDisplayString(
            zoneId = ZoneId.of("Europe/Moscow"),
        )

        assertEquals("13:15", formatted)
    }

    @Test
    fun `formats rub amount from minor units`() {
        val formatted = 124950L.toRubDisplayString()

        assertTrue(formatted.contains("₽"))
        assertTrue(formatted.contains("1"))
    }

    @Test
    fun `returns ready with daily limit data when limit is configured`() {
        val uiState = TodayUiStateFactory.create(
            hasNotificationAccess = true,
            summary = summary(
                paymentsCount = 1,
                totalAmountMinor = 85000L,
                lastPaymentAmountMinor = 85000L,
                limitAmountMinor = 100000L,
                remainingAmountMinor = 15000L,
                limitWarningLevel = DailyLimitWarningLevel.NEAR_LIMIT,
            ),
            events = listOf(paymentEvent(id = "event-1")),
            isWidgetPrivateModeEnabled = false,
        )

        assertEquals(DailyLimitWarningLevel.NEAR_LIMIT, (uiState as TodayUiState.Ready).limitWarningLevel)
        assertEquals(100000L, uiState.limitAmountMinor)
        assertEquals(15000L, uiState.remainingAmountMinor)
    }

    private fun summary(
        paymentsCount: Int,
        totalAmountMinor: Long = 50000L,
        lastPaymentAmountMinor: Long? = 50000L,
        limitAmountMinor: Long? = null,
        remainingAmountMinor: Long? = null,
        limitWarningLevel: DailyLimitWarningLevel? = null,
        hasLowConfidenceItems: Boolean = false,
    ): DailySpendSummary {
        return DailySpendSummary(
            date = LocalDate.parse("2026-04-01"),
            totalAmountMinor = totalAmountMinor,
            paymentsCount = paymentsCount,
            lastPaymentAmountMinor = lastPaymentAmountMinor,
            limitAmountMinor = limitAmountMinor,
            remainingAmountMinor = remainingAmountMinor,
            limitWarningLevel = limitWarningLevel,
            hasLowConfidenceItems = hasLowConfidenceItems,
        )
    }

    private fun paymentEvent(
        id: String,
        amountMinor: Long = 29900L,
        merchantName: String? = null,
        sourceKind: PaymentSourceKind = PaymentSourceKind.BANK,
    ): PaymentEvent {
        return PaymentEvent(
            id = id,
            amountMinor = amountMinor,
            currency = "RUB",
            paidAt = Instant.parse("2026-04-01T10:15:00Z"),
            merchantName = merchantName,
            sourceKind = sourceKind,
            paymentChannel = PaymentChannel.UNKNOWN,
            confidence = ConfidenceLevel.MEDIUM,
            status = PaymentStatus.CONFIRMED,
            userEdited = false,
            sourceIds = listOf("raw-$id"),
        )
    }
}
