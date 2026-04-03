package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.repository.DailyLimitRepository
import com.andyahmedov.enought.widget.WidgetUpdater

class SetDailyLimitUseCase(
    private val dailyLimitRepository: DailyLimitRepository,
    private val widgetUpdater: WidgetUpdater,
) {
    suspend operator fun invoke(amountMinor: Long) {
        dailyLimitRepository.setLimitAmountMinor(amountMinor)
        widgetUpdater.refresh()
    }
}
