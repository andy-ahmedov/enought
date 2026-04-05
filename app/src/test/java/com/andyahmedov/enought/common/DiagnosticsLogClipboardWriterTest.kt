package com.andyahmedov.enought.common

import org.junit.Assert.assertEquals
import org.junit.Test

class DiagnosticsLogClipboardWriterTest {
    @Test
    fun `clipboard spec keeps expected label and report text`() {
        val spec = diagnosticsLogClipboardSpec(
            reportText = "raw notifications report",
        )

        assertEquals(DIAGNOSTIC_LOG_CLIP_LABEL, spec.label)
        assertEquals("raw notifications report", spec.text)
    }
}
