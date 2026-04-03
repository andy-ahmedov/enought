package com.andyahmedov.enought.ui.permissions

internal data class NotificationAccessDisclosure(
    val supportedSourcesText: String,
    val localProcessingText: String,
    val limitationsText: String,
    val todayNoPermissionText: String,
    val todayNoPermissionSupportingText: String,
    val firstAttemptSettingsHint: String,
    val retrySettingsHint: String,
)

internal fun notificationAccessDisclosure(): NotificationAccessDisclosure {
    return NotificationAccessDisclosure(
        supportedSourcesText = "Smart Spend Widget uses notification access to read Mir Pay payment notifications and build today's phone spend. Bank notifications may still be captured as raw samples for debugging.",
        localProcessingText = "Processing and storage stay on this device. The widget also has a private mode, so the exact amount does not have to appear on the home screen.",
        limitationsText = "There is no account or cloud sync in this version. The app does not connect to your bank account and only works with supported payment notifications.",
        todayNoPermissionText = "Grant notification access so the app can read Mir Pay payment notifications and calculate today's phone spend.",
        todayNoPermissionSupportingText = "Processing stays on this device, and the home screen widget can hide the exact amount in private mode.",
        firstAttemptSettingsHint = "Open system settings, enable Smart Spend Widget in notification access, then return to the app. As soon as access is granted, you will be taken to Today.",
        retrySettingsHint = "Notification access is still off. In system settings, find Smart Spend Widget in the notification access list, turn it on, then return to the app.",
    )
}
