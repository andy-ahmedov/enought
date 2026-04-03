package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.repository.WidgetPrivateModeRepository
import com.andyahmedov.enought.widget.WidgetUpdater

class SetWidgetPrivateModeUseCase(
    private val widgetPrivateModeRepository: WidgetPrivateModeRepository,
    private val widgetUpdater: WidgetUpdater,
) {
    suspend operator fun invoke(isEnabled: Boolean) {
        widgetPrivateModeRepository.setEnabled(isEnabled)
        widgetUpdater.refresh()
    }
}
