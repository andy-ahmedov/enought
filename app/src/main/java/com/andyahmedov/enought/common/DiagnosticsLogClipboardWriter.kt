package com.andyahmedov.enought.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context

interface DiagnosticsLogClipboardWriter {
    fun copy(context: Context, reportText: String)
}

class SystemDiagnosticsLogClipboardWriter : DiagnosticsLogClipboardWriter {
    override fun copy(context: Context, reportText: String) {
        val clipboardManager = context.getSystemService(ClipboardManager::class.java) ?: return
        val spec = diagnosticsLogClipboardSpec(reportText)
        clipboardManager.setPrimaryClip(
            ClipData.newPlainText(
                spec.label,
                spec.text,
            ),
        )
    }
}

internal data class DiagnosticsLogClipboardSpec(
    val label: String,
    val text: String,
)

internal fun diagnosticsLogClipboardSpec(reportText: String): DiagnosticsLogClipboardSpec {
    return DiagnosticsLogClipboardSpec(
        label = DIAGNOSTIC_LOG_CLIP_LABEL,
        text = reportText,
    )
}

internal const val DIAGNOSTIC_LOG_CLIP_LABEL = "Enought diagnostic log"
