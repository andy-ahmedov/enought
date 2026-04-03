package com.andyahmedov.enought.ui.today

import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class TodayPaymentEventListItem(
    val id: String,
    val amountMinor: Long,
    val paidAt: Instant,
    val title: String,
    val sourceKind: PaymentSourceKind,
    val userEdited: Boolean,
)

internal fun PaymentEvent.toTodayPaymentEventListItem(): TodayPaymentEventListItem {
    return TodayPaymentEventListItem(
        id = id,
        amountMinor = amountMinor,
        paidAt = paidAt,
        title = merchantName?.trim().takeUnless { it.isNullOrBlank() } ?: sourceKind.toFallbackTitle(),
        sourceKind = sourceKind,
        userEdited = userEdited,
    )
}

fun PaymentSourceKind.toSourceLabel(): String {
    return when (this) {
        PaymentSourceKind.MIR_PAY -> "Mir Pay"
        PaymentSourceKind.BANK -> "Bank"
        PaymentSourceKind.HYBRID -> "Mir Pay + bank"
        PaymentSourceKind.MANUAL -> "Manual"
    }
}

fun Instant.toLocalTimeDisplayString(
    zoneId: ZoneId = ZoneId.systemDefault(),
): String {
    return atZone(zoneId).format(TIME_FORMATTER)
}

private fun PaymentSourceKind.toFallbackTitle(): String {
    return when (this) {
        PaymentSourceKind.MIR_PAY -> "Mir Pay payment"
        PaymentSourceKind.BANK -> "Bank notification"
        PaymentSourceKind.HYBRID -> "Mir Pay + bank"
        PaymentSourceKind.MANUAL -> "Manual payment"
    }
}

private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
