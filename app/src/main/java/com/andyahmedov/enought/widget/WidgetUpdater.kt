package com.andyahmedov.enought.widget

import android.content.Context
import androidx.glance.appwidget.updateAll

interface WidgetUpdater {
    suspend fun refresh()
}

class GlanceWidgetUpdater(
    private val context: Context,
) : WidgetUpdater {
    override suspend fun refresh() {
        TodaySpendWidget.updateAll(context)
    }
}
