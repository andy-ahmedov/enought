package com.andyahmedov.enought.collection.notifications

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

interface NotificationAccessStatusReader {
    fun hasNotificationAccess(): Boolean
}

class SystemNotificationAccessStatusReader(
    private val context: Context,
    private val listenerComponent: ComponentName =
        ComponentName(context, RawNotificationListenerService::class.java),
) : NotificationAccessStatusReader {
    override fun hasNotificationAccess(): Boolean {
        val enabledNotificationListeners = Settings.Secure.getString(
            context.contentResolver,
            ENABLED_NOTIFICATION_LISTENERS_KEY,
        )

        return isNotificationListenerEnabled(
            enabledNotificationListeners = enabledNotificationListeners,
            expectedComponentName = listenerComponent.flattenToString(),
        )
    }
}

private const val ENABLED_NOTIFICATION_LISTENERS_KEY = "enabled_notification_listeners"

internal fun isNotificationListenerEnabled(
    enabledNotificationListeners: String?,
    expectedComponentName: String,
): Boolean {
    return enabledNotificationListeners
        ?.split(':')
        ?.asSequence()
        ?.any { componentName -> componentName == expectedComponentName }
        ?: false
}
