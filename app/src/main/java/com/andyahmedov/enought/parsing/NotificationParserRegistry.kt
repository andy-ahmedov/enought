package com.andyahmedov.enought.parsing

import com.andyahmedov.enought.domain.model.PaymentCandidate
import com.andyahmedov.enought.domain.model.RawNotificationEvent
import com.andyahmedov.enought.parsing.rules.MirPayNotificationParser

class NotificationParserRegistry(
    private val parsers: List<NotificationParser>,
) {
    fun parse(rawEvent: RawNotificationEvent): PaymentCandidate? {
        for (parser in parsers) {
            if (parser.canParse(rawEvent)) {
                return parser.parse(rawEvent)
            }
        }

        return null
    }

    companion object {
        fun default(): NotificationParserRegistry {
            return NotificationParserRegistry(
                parsers = listOf(
                    MirPayNotificationParser(),
                ),
            )
        }
    }
}
