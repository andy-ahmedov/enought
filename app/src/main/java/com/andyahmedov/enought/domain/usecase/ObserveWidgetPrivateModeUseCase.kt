package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.repository.WidgetPrivateModeRepository
import kotlinx.coroutines.flow.Flow

class ObserveWidgetPrivateModeUseCase(
    private val widgetPrivateModeRepository: WidgetPrivateModeRepository,
) {
    operator fun invoke(): Flow<Boolean> {
        return widgetPrivateModeRepository.observeIsEnabled()
    }
}
