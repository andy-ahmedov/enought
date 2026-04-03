package com.andyahmedov.enought.data.repository

import android.content.SharedPreferences
import com.andyahmedov.enought.domain.repository.DailyLimitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SharedPreferencesDailyLimitRepository(
    private val sharedPreferences: SharedPreferences,
) : DailyLimitRepository {
    private val limitAmountMinorFlow = MutableStateFlow(readLimitAmountMinor())

    override fun observeLimitAmountMinor(): Flow<Long?> = limitAmountMinorFlow

    override fun getLimitAmountMinor(): Long? = readLimitAmountMinor()

    override fun setLimitAmountMinor(amountMinor: Long) {
        require(amountMinor > 0L) {
            "daily limit must be positive"
        }

        sharedPreferences.edit()
            .putLong(DAILY_LIMIT_AMOUNT_MINOR_KEY, amountMinor)
            .apply()
        limitAmountMinorFlow.value = amountMinor
    }

    override fun clearLimit() {
        sharedPreferences.edit()
            .remove(DAILY_LIMIT_AMOUNT_MINOR_KEY)
            .apply()
        limitAmountMinorFlow.value = null
    }

    private fun readLimitAmountMinor(): Long? {
        if (!sharedPreferences.contains(DAILY_LIMIT_AMOUNT_MINOR_KEY)) {
            return null
        }

        return sharedPreferences.getLong(DAILY_LIMIT_AMOUNT_MINOR_KEY, 0L)
            .takeIf { amountMinor -> amountMinor > 0L }
    }

    private companion object {
        private const val DAILY_LIMIT_AMOUNT_MINOR_KEY = "daily_limit_amount_minor"
    }
}
