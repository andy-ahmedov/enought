package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.repository.DailyLimitRepository
import com.andyahmedov.enought.widget.WidgetUpdater
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DailyLimitUseCasesTest {
    @Test
    fun `setDailyLimit saves amount and refreshes widget`() = runTest {
        val repository = FakeDailyLimitRepository()
        val widgetUpdater = FakeWidgetUpdater()
        val useCase = SetDailyLimitUseCase(
            dailyLimitRepository = repository,
            widgetUpdater = widgetUpdater,
        )

        useCase(150000L)

        assertEquals(150000L, repository.getLimitAmountMinor())
        assertEquals(1, widgetUpdater.refreshCalls)
    }

    @Test
    fun `clearDailyLimit removes amount and refreshes widget`() = runTest {
        val repository = FakeDailyLimitRepository().apply {
            setLimitAmountMinor(150000L)
        }
        val widgetUpdater = FakeWidgetUpdater()
        val useCase = ClearDailyLimitUseCase(
            dailyLimitRepository = repository,
            widgetUpdater = widgetUpdater,
        )

        useCase()

        assertNull(repository.getLimitAmountMinor())
        assertEquals(1, widgetUpdater.refreshCalls)
    }

    private class FakeDailyLimitRepository : DailyLimitRepository {
        private val flow = MutableStateFlow<Long?>(null)

        override fun observeLimitAmountMinor(): Flow<Long?> = flow

        override fun getLimitAmountMinor(): Long? = flow.value

        override fun setLimitAmountMinor(amountMinor: Long) {
            flow.value = amountMinor
        }

        override fun clearLimit() {
            flow.value = null
        }
    }

    private class FakeWidgetUpdater : WidgetUpdater {
        var refreshCalls: Int = 0

        override suspend fun refresh() {
            refreshCalls += 1
        }
    }
}
