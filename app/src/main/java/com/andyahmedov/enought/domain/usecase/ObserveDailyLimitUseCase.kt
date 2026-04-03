package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.repository.DailyLimitRepository
import kotlinx.coroutines.flow.Flow

class ObserveDailyLimitUseCase(
    private val dailyLimitRepository: DailyLimitRepository,
) {
    operator fun invoke(): Flow<Long?> {
        return dailyLimitRepository.observeLimitAmountMinor()
    }
}
