package com.andyahmedov.enought.domain.repository

import kotlinx.coroutines.flow.Flow

interface WidgetPrivateModeRepository {
    fun observeIsEnabled(): Flow<Boolean>

    fun isEnabled(): Boolean

    fun setEnabled(isEnabled: Boolean)
}
