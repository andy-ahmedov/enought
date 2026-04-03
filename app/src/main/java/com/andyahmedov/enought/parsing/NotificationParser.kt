package com.andyahmedov.enought.parsing

import com.andyahmedov.enought.domain.model.PaymentCandidate
import com.andyahmedov.enought.domain.model.RawNotificationEvent

interface NotificationParser {
    fun canParse(rawEvent: RawNotificationEvent): Boolean

    fun parse(rawEvent: RawNotificationEvent): PaymentCandidate?
}
