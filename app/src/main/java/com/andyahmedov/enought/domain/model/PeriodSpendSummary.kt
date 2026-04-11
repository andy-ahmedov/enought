package com.andyahmedov.enought.domain.model

import java.time.LocalDate

data class PeriodSpendSummary(
    val startDate: LocalDate,
    val endDateInclusive: LocalDate,
    val totalAmountMinor: Long,
    val paymentsCount: Int,
    val lastPaymentAmountMinor: Long?,
    val hasLowConfidenceItems: Boolean,
) {
    init {
        require(paymentsCount >= 0) {
            "paymentsCount must be non-negative"
        }
    }
}
