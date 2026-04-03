package com.andyahmedov.enought.domain.repository

import kotlinx.coroutines.flow.Flow

interface DailyLimitRepository {
    fun observeLimitAmountMinor(): Flow<Long?>

    fun getLimitAmountMinor(): Long?

    fun setLimitAmountMinor(amountMinor: Long)

    fun clearLimit()
}
