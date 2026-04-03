package com.andyahmedov.enought.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RubAmountInputTest {
    @Test
    fun `parseRubAmountInput accepts whole rub amount`() {
        assertEquals(250000L, parseRubAmountInput("2500"))
    }

    @Test
    fun `parseRubAmountInput accepts two decimal places`() {
        assertEquals(34950L, parseRubAmountInput("349.50"))
    }

    @Test
    fun `parseRubAmountInput rejects blank zero and malformed input`() {
        assertNull(parseRubAmountInput(""))
        assertNull(parseRubAmountInput("0"))
        assertNull(parseRubAmountInput("-10"))
        assertNull(parseRubAmountInput("12.345"))
        assertNull(parseRubAmountInput("abc"))
    }
}
