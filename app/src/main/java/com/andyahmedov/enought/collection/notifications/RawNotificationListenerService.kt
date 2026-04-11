package com.andyahmedov.enought.collection.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.andyahmedov.enought.app.appContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class RawNotificationListenerService : NotificationListenerService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val rawNotificationEventFactory = RawNotificationEventFactory()

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification?) {
        val notification = statusBarNotification ?: return
        val appContainer = applicationContext.appContainer
        val sourcePolicy = appContainer.notificationSourcePolicy

        if (!sourcePolicy.isSupported(notification.packageName)) {
            return
        }

        val rawEvent = rawNotificationEventFactory.fromStatusBarNotification(notification)

        serviceScope.launch {
            val rawEventSaved = runCatching {
                appContainer.rawNotificationEventRepository.saveIfNew(rawEvent)
            }

            if (rawEventSaved.isFailure || rawEventSaved.getOrNull() != true) {
                return@launch
            }

            runCatching {
                appContainer.processIncomingRawEventUseCase(rawEvent)
            }
            runCatching {
                appContainer.enforceDataRetentionUseCase()
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }
}
