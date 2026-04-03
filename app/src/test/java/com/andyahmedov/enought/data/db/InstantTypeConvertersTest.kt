package com.andyahmedov.enought.data.db

import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstantTypeConvertersTest {
    private val converters = InstantTypeConverters()

    @Test
    fun instantToEpochMillis_roundTripsValue() {
        val instant = Instant.parse("2026-03-31T08:30:00Z")

        val epochMillis = converters.instantToEpochMillis(instant)
        val restored = converters.epochMillisToInstant(epochMillis)

        assertEquals(instant, restored)
    }

    @Test
    fun converters_handleNullValues() {
        assertNull(converters.instantToEpochMillis(null))
        assertNull(converters.epochMillisToInstant(null))
    }
}

