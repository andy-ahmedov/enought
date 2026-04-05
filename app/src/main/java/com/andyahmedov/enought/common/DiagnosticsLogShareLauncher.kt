package com.andyahmedov.enought.common

import android.app.Activity
import android.content.Context
import android.content.Intent

interface DiagnosticsLogShareLauncher {
    fun share(context: Context, reportText: String)
}

class SystemDiagnosticsLogShareLauncher : DiagnosticsLogShareLauncher {
    override fun share(context: Context, reportText: String) {
        val launchSpec = diagnosticsLogShareLaunchSpec(
            shouldAddNewTaskFlag = context !is Activity,
            reportText = reportText,
        )
        context.startActivity(
            createDiagnosticsLogShareChooserIntent(launchSpec),
        )
    }
}

internal fun createDiagnosticsLogShareChooserIntent(
    launchSpec: DiagnosticsLogShareLaunchSpec,
): Intent {
    return Intent.createChooser(
        createDiagnosticsLogShareIntent(launchSpec),
        launchSpec.chooserTitle,
    ).apply {
        if (launchSpec.shouldAddNewTaskFlag) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}

internal fun createDiagnosticsLogShareIntent(
    launchSpec: DiagnosticsLogShareLaunchSpec,
): Intent {
    return Intent(launchSpec.action).apply {
        type = launchSpec.mimeType
        putExtra(Intent.EXTRA_TEXT, launchSpec.reportText)
    }
}

internal fun diagnosticsLogShareLaunchSpec(
    shouldAddNewTaskFlag: Boolean,
    reportText: String,
): DiagnosticsLogShareLaunchSpec {
    return DiagnosticsLogShareLaunchSpec(
        action = Intent.ACTION_SEND,
        mimeType = DIAGNOSTIC_LOG_MIME_TYPE,
        chooserTitle = DIAGNOSTIC_LOG_CHOOSER_TITLE,
        reportText = reportText,
        shouldAddNewTaskFlag = shouldAddNewTaskFlag,
    )
}

internal data class DiagnosticsLogShareLaunchSpec(
    val action: String,
    val mimeType: String,
    val chooserTitle: String,
    val reportText: String,
    val shouldAddNewTaskFlag: Boolean,
)

internal const val DIAGNOSTIC_LOG_MIME_TYPE = "text/plain"
internal const val DIAGNOSTIC_LOG_CHOOSER_TITLE = "Share diagnostic log"
