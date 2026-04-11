package com.andyahmedov.enought.domain.model

import java.time.LocalDate

data class HistoryDaySummary(
    val date: LocalDate,
    val totalAmountMinor: Long,
    val paymentsCount: Int,
    val lastPaymentAmountMinor: Long?,
) {
    init {
        require(paymentsCount >= 0) {
            "paymentsCount must be non-negative"
        }
    }
}
