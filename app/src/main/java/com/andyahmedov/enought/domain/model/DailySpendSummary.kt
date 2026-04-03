package com.andyahmedov.enought.domain.model

import java.time.LocalDate

data class DailySpendSummary(
    val date: LocalDate,
    val totalAmountMinor: Long,
    val paymentsCount: Int,
    val lastPaymentAmountMinor: Long?,
    val limitAmountMinor: Long?,
    val remainingAmountMinor: Long?,
    val limitWarningLevel: DailyLimitWarningLevel?,
    val hasLowConfidenceItems: Boolean,
) {
    init {
        require(paymentsCount >= 0) {
            "paymentsCount must be non-negative"
        }
    }
}
