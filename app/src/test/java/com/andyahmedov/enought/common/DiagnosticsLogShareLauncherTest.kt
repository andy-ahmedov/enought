package com.andyahmedov.enought.common

import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DiagnosticsLogShareLauncherTest {
    @Test
    fun `creates share launch spec with text plain payload`() {
        val spec = diagnosticsLogShareLaunchSpec(
            shouldAddNewTaskFlag = false,
            reportText = "diagnostic log",
        )

        assertEquals(Intent.ACTION_SEND, spec.action)
        assertEquals(DIAGNOSTIC_LOG_MIME_TYPE, spec.mimeType)
        assertEquals(DIAGNOSTIC_LOG_CHOOSER_TITLE, spec.chooserTitle)
        assertEquals("diagnostic log", spec.reportText)
    }

    @Test
    fun `share launch spec keeps requested new task flag`() {
        assertTrue(
            diagnosticsLogShareLaunchSpec(
                shouldAddNewTaskFlag = true,
                reportText = "diagnostic log",
            ).shouldAddNewTaskFlag,
        )
        assertFalse(
            diagnosticsLogShareLaunchSpec(
                shouldAddNewTaskFlag = false,
                reportText = "diagnostic log",
            ).shouldAddNewTaskFlag,
        )
    }
}
