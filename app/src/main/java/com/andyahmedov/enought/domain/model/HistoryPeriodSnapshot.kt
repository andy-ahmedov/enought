package com.andyahmedov.enought.domain.model

data class HistoryPeriodSnapshot(
    val period: HistoryPeriod,
    val summary: PeriodSpendSummary,
    val paymentEvents: List<PaymentEvent>,
    val daySummaries: List<HistoryDaySummary>,
)
