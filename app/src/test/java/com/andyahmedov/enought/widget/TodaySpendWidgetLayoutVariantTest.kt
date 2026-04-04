package com.andyahmedov.enought.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class TodaySpendWidgetLayoutVariantTest {
    @Test
    fun `uses compact layout below wide threshold`() {
        val variant = resolveWidgetLayoutVariant(
            DpSize(width = 180.dp, height = 120.dp),
        )

        assertEquals(WidgetLayoutVariant.Compact, variant)
    }

    @Test
    fun `uses wide layout for 4 by 2 sized widget`() {
        val variant = resolveWidgetLayoutVariant(
            DpSize(width = 260.dp, height = 120.dp),
        )

        assertEquals(WidgetLayoutVariant.Wide, variant)
    }
}
