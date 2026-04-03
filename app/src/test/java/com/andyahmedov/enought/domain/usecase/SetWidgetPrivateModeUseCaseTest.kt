package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.repository.WidgetPrivateModeRepository
import com.andyahmedov.enought.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SetWidgetPrivateModeUseCaseTest {
    @Test
    fun `saves private mode and refreshes widget`() = runTest {
        val repository = FakeWidgetPrivateModeRepository()
        val widgetUpdater = FakeWidgetUpdater()
        val useCase = SetWidgetPrivateModeUseCase(
            widgetPrivateModeRepository = repository,
            widgetUpdater = widgetUpdater,
        )

        useCase(true)

        assertTrue(repository.isEnabled())
        assertEquals(1, widgetUpdater.refreshCalls)
    }

    private class FakeWidgetPrivateModeRepository : WidgetPrivateModeRepository {
        private val flow = MutableStateFlow(false)

        override fun observeIsEnabled(): Flow<Boolean> = flow

        override fun isEnabled(): Boolean = flow.value

        override fun setEnabled(isEnabled: Boolean) {
            flow.value = isEnabled
        }
    }

    private class FakeWidgetUpdater : WidgetUpdater {
        var refreshCalls: Int = 0

        override suspend fun refresh() {
            refreshCalls += 1
        }
    }
}
