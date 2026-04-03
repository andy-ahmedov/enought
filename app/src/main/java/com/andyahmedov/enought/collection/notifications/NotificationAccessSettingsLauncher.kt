package com.andyahmedov.enought.collection.notifications

import android.app.Activity
import android.content.Context
import android.content.Intent

interface NotificationAccessSettingsLauncher {
    fun open(context: Context)
}

class SystemNotificationAccessSettingsLauncher : NotificationAccessSettingsLauncher {
    override fun open(context: Context) {
        val launchSpec = notificationAccessSettingsLaunchSpec(
            shouldAddNewTaskFlag = context !is Activity,
        )
        context.startActivity(
            createNotificationAccessSettingsIntent(launchSpec),
        )
    }
}

internal fun createNotificationAccessSettingsIntent(
    launchSpec: NotificationAccessSettingsLaunchSpec,
): Intent {
    return Intent(launchSpec.action).apply {
        if (launchSpec.shouldAddNewTaskFlag) {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}

internal fun notificationAccessSettingsLaunchSpec(
    shouldAddNewTaskFlag: Boolean,
): NotificationAccessSettingsLaunchSpec {
    return NotificationAccessSettingsLaunchSpec(
        action = NOTIFICATION_LISTENER_SETTINGS_ACTION,
        shouldAddNewTaskFlag = shouldAddNewTaskFlag,
    )
}

internal data class NotificationAccessSettingsLaunchSpec(
    val action: String,
    val shouldAddNewTaskFlag: Boolean,
)

internal const val NOTIFICATION_LISTENER_SETTINGS_ACTION =
    "android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"
