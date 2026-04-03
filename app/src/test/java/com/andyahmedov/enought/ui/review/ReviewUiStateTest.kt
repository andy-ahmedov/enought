package com.andyahmedov.enought.ui.review

import com.andyahmedov.enought.common.parseRubAmountInput
import com.andyahmedov.enought.common.toRubInputString
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.TodayReviewItem
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReviewUiStateTest {
    @Test
    fun `returns loading while review items have not loaded`() {
        assertEquals(
            ReviewUiState.Loading,
            ReviewUiStateFactory.create(reviewItems = null),
        )
    }

    @Test
    fun `returns empty when no suspected items remain`() {
        assertEquals(
            ReviewUiState.Empty,
            ReviewUiStateFactory.create(reviewItems = emptyList()),
        )
    }

    @Test
    fun `returns ready with mapped review items`() {
        val uiState = ReviewUiStateFactory.create(
            reviewItems = listOf(
                TodayReviewItem.Single(
                    id = "event-1",
                    amountMinor = 34900L,
                    paidAt = Instant.parse("2026-04-01T10:15:00Z"),
                    title = "Coffee Point",
                    sourceKind = PaymentSourceKind.BANK,
                ),
            ),
        )

        assertEquals(
            ReviewUiState.Ready(
                items = listOf(
                    ReviewListItem.Single(
                        id = "event-1",
                        amountMinor = 34900L,
                        paidAt = Instant.parse("2026-04-01T10:15:00Z"),
                        title = "Coffee Point",
                        sourceKind = PaymentSourceKind.BANK,
                    ),
                ),
            ),
            uiState,
        )
    }

    @Test
    fun `parses rub amount input from dot and comma separated text`() {
        assertEquals(34900L, parseRubAmountInput("349"))
        assertEquals(34950L, parseRubAmountInput("349.50"))
        assertEquals(34950L, parseRubAmountInput("349,50"))
    }

    @Test
    fun `returns null for invalid rub amount input`() {
        assertNull(parseRubAmountInput(""))
        assertNull(parseRubAmountInput("0"))
        assertNull(parseRubAmountInput("12.345"))
        assertNull(parseRubAmountInput("abc"))
    }

    @Test
    fun `formats minor units for amount edit input`() {
        assertEquals("349", 34900L.toRubInputString())
        assertEquals("349.50", 34950L.toRubInputString())
    }
}
