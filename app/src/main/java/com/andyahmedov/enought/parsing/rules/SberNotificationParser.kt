package com.andyahmedov.enought.parsing.rules

import com.andyahmedov.enought.domain.model.ConfidenceHint
import com.andyahmedov.enought.domain.model.PaymentCandidate
import com.andyahmedov.enought.domain.model.PaymentChannel
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.RawNotificationEvent
import com.andyahmedov.enought.parsing.NotificationParser

class SberNotificationParser : NotificationParser {
    override fun canParse(rawEvent: RawNotificationEvent): Boolean {
        if (rawEvent.sourcePackage != SUPPORTED_SOURCE_PACKAGE) {
            return false
        }

        return extractAmountMinor(rawEvent.searchableText()) != null
    }

    override fun parse(rawEvent: RawNotificationEvent): PaymentCandidate? {
        if (rawEvent.sourcePackage != SUPPORTED_SOURCE_PACKAGE) {
            return null
        }

        val searchableText = rawEvent.searchableText()
        val amountMinor = extractAmountMinor(searchableText) ?: return null

        val confidenceHints = linkedSetOf(
            ConfidenceHint.AMOUNT_PARSED,
            ConfidenceHint.TIMESTAMP_PARSED,
        )

        if (containsReversalMarker(searchableText)) {
            confidenceHints += ConfidenceHint.POSSIBLE_REVERSAL
        }

        return PaymentCandidate(
            amountMinor = amountMinor,
            currency = DEFAULT_CURRENCY,
            paidAt = rawEvent.postedAt,
            merchantName = null,
            sourceKind = PaymentSourceKind.BANK,
            paymentChannel = PaymentChannel.UNKNOWN,
            rawSourceId = rawEvent.id,
            confidenceHints = confidenceHints,
        )
    }

    private fun extractAmountMinor(searchableText: String): Long? {
        val match = amountRegex.find(searchableText) ?: return null
        val wholePart = match.groupValues[1]
            .replace(" ", "")
            .replace(NON_BREAKING_SPACE, "")
        val fractionalPart = match.groupValues[2]

        val wholeMinor = wholePart.toLongOrNull() ?: return null
        val fractionalMinor = when (fractionalPart.length) {
            0 -> 0L
            1 -> "${fractionalPart}0".toLongOrNull() ?: return null
            2 -> fractionalPart.toLongOrNull() ?: return null
            else -> return null
        }

        return try {
            Math.addExact(
                Math.multiplyExact(wholeMinor, MINOR_UNITS_MULTIPLIER),
                fractionalMinor,
            )
        } catch (_: ArithmeticException) {
            null
        }
    }

    private fun containsReversalMarker(searchableText: String): Boolean {
        return reversalMarkerRegex.containsMatchIn(searchableText)
    }

    private fun RawNotificationEvent.searchableText(): String {
        return listOfNotNull(
            title?.takeIf { it.isNotBlank() },
            text?.takeIf { it.isNotBlank() },
            subText?.takeIf { it.isNotBlank() },
            bigText?.takeIf { it.isNotBlank() },
        ).joinToString(separator = "\n")
    }

    companion object {
        const val SUPPORTED_SOURCE_PACKAGE = "ru.sberbankmobile"

        private const val DEFAULT_CURRENCY = "RUB"
        private const val MINOR_UNITS_MULTIPLIER = 100L
        private const val NON_BREAKING_SPACE = "\u00A0"

        private val amountRegex = Regex(
            pattern = """(?<!\d)(\d{1,3}(?:[ \u00A0]\d{3})*|\d+)(?:[.,](\d{1,2}))?\s*(₽|руб\.?|RUB)(?!\p{L})""",
            options = setOf(RegexOption.IGNORE_CASE),
        )

        private val reversalMarkerRegex = Regex(
            pattern = """\b(возврат|отмена|refund|reversal)\b""",
            options = setOf(RegexOption.IGNORE_CASE),
        )
    }
}
