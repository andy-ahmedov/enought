package com.andyahmedov.enought.normalization

import com.andyahmedov.enought.domain.model.ConfidenceHint
import com.andyahmedov.enought.domain.model.ConfidenceLevel
import com.andyahmedov.enought.domain.model.PaymentCandidate
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.PaymentStatus
import java.util.Locale

class DefaultPaymentEventNormalizer : PaymentEventNormalizer {
    override fun normalize(candidate: PaymentCandidate): PaymentEvent? {
        val amountMinor = candidate.amountMinor ?: return null
        val currency = candidate.currency
            ?.trim()
            ?.uppercase(Locale.ROOT)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val paidAt = candidate.paidAt ?: return null
        val merchantName = candidate.merchantName?.trim()?.takeIf { it.isNotBlank() }
        val confidence = resolveConfidence(candidate)

        return PaymentEvent(
            id = candidate.rawSourceId,
            amountMinor = amountMinor,
            currency = currency,
            paidAt = paidAt,
            merchantName = merchantName,
            sourceKind = candidate.sourceKind,
            paymentChannel = candidate.paymentChannel,
            confidence = confidence,
            status = resolveStatus(confidence),
            userEdited = false,
            sourceIds = listOf(candidate.rawSourceId),
        )
    }

    private fun resolveConfidence(candidate: PaymentCandidate): ConfidenceLevel {
        val hints = candidate.confidenceHints
        if (ConfidenceHint.POSSIBLE_REVERSAL in hints || ConfidenceHint.PARTIAL_PARSE in hints) {
            return ConfidenceLevel.LOW
        }

        val hasStrongMirPaySignal = candidate.sourceKind == PaymentSourceKind.MIR_PAY &&
            ConfidenceHint.MIR_PAY_MARKER in hints &&
            ConfidenceHint.PHONE_PAYMENT_MARKER in hints &&
            ConfidenceHint.AMOUNT_PARSED in hints &&
            ConfidenceHint.TIMESTAMP_PARSED in hints

        if (hasStrongMirPaySignal) {
            return ConfidenceLevel.HIGH
        }

        return ConfidenceLevel.MEDIUM
    }

    private fun resolveStatus(confidence: ConfidenceLevel): PaymentStatus {
        return when (confidence) {
            ConfidenceLevel.LOW -> PaymentStatus.SUSPECTED
            ConfidenceLevel.MEDIUM,
            ConfidenceLevel.HIGH,
            -> PaymentStatus.CONFIRMED
        }
    }
}
