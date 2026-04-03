package com.andyahmedov.enought.common

import java.math.BigDecimal
import java.math.RoundingMode

fun parseRubAmountInput(input: String): Long? {
    val normalized = input.trim().replace(',', '.')
    if (normalized.isBlank()) {
        return null
    }
    if (!normalized.matches(Regex("\\d+(\\.\\d{1,2})?"))) {
        return null
    }

    val amount = normalized.toBigDecimalOrNull()
        ?.setScale(2, RoundingMode.UNNECESSARY)
        ?.movePointRight(2)
        ?: return null
    if (amount <= BigDecimal.ZERO) {
        return null
    }

    return amount.toLong()
}

fun Long.toRubInputString(): String {
    val whole = this / 100L
    val fraction = this % 100L

    return if (fraction == 0L) {
        whole.toString()
    } else {
        "%d.%02d".format(whole, fraction)
    }
}
