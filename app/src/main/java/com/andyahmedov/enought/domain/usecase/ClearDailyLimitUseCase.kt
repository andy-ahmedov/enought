package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.repository.DailyLimitRepository
import com.andyahmedov.enought.widget.WidgetUpdater

class ClearDailyLimitUseCase(
    private val dailyLimitRepository: DailyLimitRepository,
    private val widgetUpdater: WidgetUpdater,
) {
    suspend operator fun invoke() {
        dailyLimitRepository.clearLimit()
        widgetUpdater.refresh()
    }
}
