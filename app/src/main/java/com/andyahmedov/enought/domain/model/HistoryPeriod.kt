package com.andyahmedov.enought.domain.model

enum class HistoryPeriod(
    val retainedDays: Long,
) {
    TODAY(retainedDays = 1L),
    LAST_7_DAYS(retainedDays = 7L),
    LAST_30_DAYS(retainedDays = 30L),
    LAST_90_DAYS(retainedDays = 90L),
}
