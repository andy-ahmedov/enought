package com.andyahmedov.enought.data.repository

import android.content.SharedPreferences
import com.andyahmedov.enought.domain.repository.WidgetPrivateModeRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SharedPreferencesWidgetPrivateModeRepository(
    private val sharedPreferences: SharedPreferences,
) : WidgetPrivateModeRepository {
    private val isEnabledFlow = MutableStateFlow(readIsEnabled())

    override fun observeIsEnabled(): Flow<Boolean> = isEnabledFlow

    override fun isEnabled(): Boolean = readIsEnabled()

    override fun setEnabled(isEnabled: Boolean) {
        sharedPreferences.edit()
            .putBoolean(PRIVATE_MODE_ENABLED_KEY, isEnabled)
            .apply()
        isEnabledFlow.value = isEnabled
    }

    private fun readIsEnabled(): Boolean {
        return sharedPreferences.getBoolean(PRIVATE_MODE_ENABLED_KEY, false)
    }

    private companion object {
        private const val PRIVATE_MODE_ENABLED_KEY = "widget_private_mode_enabled"
    }
}
