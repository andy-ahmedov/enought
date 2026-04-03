package com.andyahmedov.enought.collection.notifications

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
fun rememberNotificationAccessState(
    notificationAccessStatusReader: NotificationAccessStatusReader,
): Boolean? {
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasNotificationAccess by remember(notificationAccessStatusReader) {
        mutableStateOf<Boolean?>(null)
    }
    val latestReader by rememberUpdatedState(notificationAccessStatusReader)

    DisposableEffect(lifecycleOwner, latestReader) {
        fun refresh() {
            hasNotificationAccess = latestReader.hasNotificationAccess()
        }

        refresh()

        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                refresh()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    return hasNotificationAccess
}
