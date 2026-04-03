package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.ConfidenceLevel
import com.andyahmedov.enought.domain.model.PaymentChannel
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.PaymentStatus

internal fun mergeDuplicatePair(
    canonicalEvent: PaymentEvent,
    siblingEvent: PaymentEvent,
    userEdited: Boolean,
): PaymentEvent {
    val mergedMerchantName = canonicalEvent.merchantName?.takeIf { it.isNotBlank() }
        ?: siblingEvent.merchantName?.takeIf { it.isNotBlank() }
    val mergedPaidAt = when {
        canonicalEvent.sourceKind == PaymentSourceKind.MIR_PAY -> canonicalEvent.paidAt
        siblingEvent.sourceKind == PaymentSourceKind.MIR_PAY -> siblingEvent.paidAt
        canonicalEvent.paidAt <= siblingEvent.paidAt -> canonicalEvent.paidAt
        else -> siblingEvent.paidAt
    }

    return canonicalEvent.copy(
        amountMinor = canonicalEvent.amountMinor,
        currency = canonicalEvent.currency,
        paidAt = mergedPaidAt,
        merchantName = mergedMerchantName,
        sourceKind = PaymentSourceKind.HYBRID,
        paymentChannel = if (
            canonicalEvent.paymentChannel == PaymentChannel.PHONE ||
            siblingEvent.paymentChannel == PaymentChannel.PHONE
        ) {
            PaymentChannel.PHONE
        } else {
            PaymentChannel.UNKNOWN
        },
        confidence = ConfidenceLevel.HIGH,
        status = PaymentStatus.CONFIRMED,
        userEdited = userEdited,
        sourceIds = (canonicalEvent.sourceIds + siblingEvent.sourceIds).distinct().sorted(),
        duplicateGroupId = null,
    )
}

internal fun selectManualDuplicateCanonicalEvent(
    firstEvent: PaymentEvent,
    secondEvent: PaymentEvent,
): PaymentEvent {
    return when {
        firstEvent.paidAt < secondEvent.paidAt -> firstEvent
        secondEvent.paidAt < firstEvent.paidAt -> secondEvent
        firstEvent.sourceKind == PaymentSourceKind.BANK && secondEvent.sourceKind == PaymentSourceKind.MIR_PAY -> firstEvent
        secondEvent.sourceKind == PaymentSourceKind.BANK && firstEvent.sourceKind == PaymentSourceKind.MIR_PAY -> secondEvent
        else -> minOf(firstEvent, secondEvent, compareBy { event -> event.id })
    }
}
