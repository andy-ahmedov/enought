package com.andyahmedov.enought.common

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

fun Long.toRubDisplayString(): String {
    val amount = this / 100.0
    val formatter = NumberFormat.getCurrencyInstance(Locale("ru", "RU")).apply {
        currency = Currency.getInstance(RUB_CURRENCY)
        minimumFractionDigits = if (this@toRubDisplayString % 100L == 0L) 0 else 2
        maximumFractionDigits = 2
    }

    return formatter.format(amount)
}

private const val RUB_CURRENCY = "RUB"
