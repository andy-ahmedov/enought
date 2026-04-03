package com.andyahmedov.enought.collection.notifications

import android.app.Notification
import android.service.notification.StatusBarNotification
import com.andyahmedov.enought.domain.model.RawNotificationEvent
import java.security.MessageDigest
import java.time.Instant
import java.util.Locale

class RawNotificationEventFactory {
    fun fromStatusBarNotification(statusBarNotification: StatusBarNotification): RawNotificationEvent {
        return fromSnapshot(statusBarNotification.toSnapshot())
    }

    fun fromSnapshot(snapshot: NotificationSnapshot): RawNotificationEvent {
        val payloadJson = buildCanonicalPayloadJson(snapshot)
        val payloadHash = sha256(payloadJson)
        val eventId = sha256(
            listOf(
                snapshot.sourcePackage,
                snapshot.notificationTag.orEmpty(),
                snapshot.notificationId.toString(),
                snapshot.postedAt.toEpochMilli().toString(),
                payloadHash,
            ).joinToString(separator = "|"),
        )

        return RawNotificationEvent(
            id = eventId,
            sourcePackage = snapshot.sourcePackage,
            postedAt = snapshot.postedAt,
            title = snapshot.title,
            text = snapshot.text,
            subText = snapshot.subText,
            bigText = snapshot.bigText,
            extrasJson = payloadJson,
            payloadHash = payloadHash,
        )
    }

    internal fun buildCanonicalPayloadJson(snapshot: NotificationSnapshot): String {
        val fields = linkedMapOf(
            "notificationId" to snapshot.notificationId.toString(),
            "notificationTag" to snapshot.notificationTag,
            "channelId" to snapshot.channelId,
            "category" to snapshot.category,
            "title" to snapshot.title,
            "text" to snapshot.text,
            "subText" to snapshot.subText,
            "bigText" to snapshot.bigText,
        )

        return fields.entries.joinToString(
            prefix = "{",
            postfix = "}",
            separator = ",",
        ) { (key, value) ->
            "\"$key\":${value.toCanonicalJsonValue()}"
        }
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
        return digest.joinToString(separator = "") { byte ->
            String.format(Locale.US, "%02x", byte)
        }
    }
}

private fun StatusBarNotification.toSnapshot(): NotificationSnapshot {
    val extras = notification.extras

    return NotificationSnapshot(
        sourcePackage = packageName,
        postedAt = Instant.ofEpochMilli(postTime),
        notificationId = id,
        notificationTag = tag,
        channelId = notification.channelId,
        category = notification.category,
        title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
        text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
        subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString(),
        bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString(),
    )
}

private fun String?.toCanonicalJsonValue(): String {
    return if (this == null) {
        "null"
    } else {
        "\"${escapeJsonString()}\""
    }
}

private fun String.escapeJsonString(): String {
    val builder = StringBuilder(length)

    for (character in this) {
        when (character) {
            '\\' -> builder.append("\\\\")
            '"' -> builder.append("\\\"")
            '\b' -> builder.append("\\b")
            '\u000C' -> builder.append("\\f")
            '\n' -> builder.append("\\n")
            '\r' -> builder.append("\\r")
            '\t' -> builder.append("\\t")
            else -> {
                if (character.code < 0x20) {
                    builder.append("\\u")
                    builder.append(character.code.toString(16).padStart(4, '0'))
                } else {
                    builder.append(character)
                }
            }
        }
    }

    return builder.toString()
}
