package com.andyahmedov.enought.widget

import com.andyahmedov.enought.collection.notifications.NotificationAccessStatusReader
import com.andyahmedov.enought.domain.model.ConfidenceLevel
import com.andyahmedov.enought.domain.model.DailyLimitWarningLevel
import com.andyahmedov.enought.domain.model.PaymentChannel
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentSourceKind
import com.andyahmedov.enought.domain.model.PaymentStatus
import com.andyahmedov.enought.domain.repository.DailyLimitRepository
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import com.andyahmedov.enought.domain.repository.WidgetPrivateModeRepository
import com.andyahmedov.enought.domain.usecase.ObserveYesterdayTotalUseCase
import com.andyahmedov.enought.domain.usecase.ObserveTodaySummaryUseCase
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GetTodayWidgetStateUseCaseTest {
    @Test
    fun `returns no permission when notification access is missing`() = runTest {
        val useCase = GetTodayWidgetStateUseCase(
            notificationAccessStatusReader = FakeNotificationAccessStatusReader(false),
            observeTodaySummaryUseCase = observeTodaySummaryUseCase(emptyList(), null),
            observeYesterdayTotalUseCase = observeYesterdayTotalUseCase(emptyList()),
            widgetPrivateModeRepository = FakeWidgetPrivateModeRepository(false),
        )

        val state = useCase()

        assertEquals(TodayWidgetState.NoPermission, state)
    }

    @Test
    fun `returns no data when there are no confirmed payments`() = runTest {
        val useCase = GetTodayWidgetStateUseCase(
            notificationAccessStatusReader = FakeNotificationAccessStatusReader(true),
            observeTodaySummaryUseCase = observeTodaySummaryUseCase(
                listOf(
                    paymentEvent(
                        id = "suspected-1",
                        paidAt = Instant.parse("2026-04-01T10:00:00Z"),
                        amountMinor = 52000L,
                        confidence = ConfidenceLevel.LOW,
                        status = PaymentStatus.SUSPECTED,
                    ),
                ),
                limitAmountMinor = null,
            ),
            observeYesterdayTotalUseCase = observeYesterdayTotalUseCase(emptyList()),
            widgetPrivateModeRepository = FakeWidgetPrivateModeRepository(true),
        )

        val state = useCase()

        assertEquals(
            TodayWidgetState.NoData(hasLowConfidenceItems = true),
            state,
        )
    }

    @Test
    fun `returns regular ready with summary values when private mode is disabled`() = runTest {
        val useCase = GetTodayWidgetStateUseCase(
            notificationAccessStatusReader = FakeNotificationAccessStatusReader(true),
            observeTodaySummaryUseCase = observeTodaySummaryUseCase(
                listOf(
                    paymentEvent(
                        id = "event-1",
                        paidAt = Instant.parse("2026-04-01T12:00:00Z"),
                        amountMinor = 125000L,
                    ),
                    paymentEvent(
                        id = "event-2",
                        paidAt = Instant.parse("2026-04-01T18:45:00Z"),
                        amountMinor = 9900L,
                    ),
                    paymentEvent(
                        id = "suspected-1",
                        paidAt = Instant.parse("2026-04-01T19:00:00Z"),
                        amountMinor = 5000L,
                        confidence = ConfidenceLevel.LOW,
                        status = PaymentStatus.SUSPECTED,
                    ),
                ),
                limitAmountMinor = null,
            ),
            observeYesterdayTotalUseCase = observeYesterdayTotalUseCase(
                listOf(
                    paymentEvent(
                        id = "yesterday-1",
                        paidAt = Instant.parse("2026-03-31T12:00:00Z"),
                        amountMinor = 48000L,
                    ),
                ),
            ),
            widgetPrivateModeRepository = FakeWidgetPrivateModeRepository(false),
        )

        val state = useCase()

        assertEquals(
            TodayWidgetState.ReadyRegular(
                totalAmountMinor = 134900L,
                paymentsCount = 2,
                yesterdayTotalAmountMinor = 48000L,
                lastPaymentAmountMinor = 9900L,
                remainingAmountMinor = null,
                limitWarningLevel = null,
                hasLowConfidenceItems = true,
            ),
            state,
        )
    }

    @Test
    fun `returns private ready with count only when private mode is enabled`() = runTest {
        val useCase = GetTodayWidgetStateUseCase(
            notificationAccessStatusReader = FakeNotificationAccessStatusReader(true),
            observeTodaySummaryUseCase = observeTodaySummaryUseCase(
                listOf(
                    paymentEvent(
                        id = "event-1",
                        paidAt = Instant.parse("2026-04-01T12:00:00Z"),
                        amountMinor = 125000L,
                    ),
                    paymentEvent(
                        id = "event-2",
                        paidAt = Instant.parse("2026-04-01T18:45:00Z"),
                        amountMinor = 9900L,
                    ),
                ),
                limitAmountMinor = null,
            ),
            observeYesterdayTotalUseCase = observeYesterdayTotalUseCase(emptyList()),
            widgetPrivateModeRepository = FakeWidgetPrivateModeRepository(true),
        )

        val state = useCase()

        assertEquals(
            TodayWidgetState.ReadyPrivate(
                paymentsCount = 2,
                remainingAmountMinor = null,
                limitWarningLevel = null,
                hasLowConfidenceItems = false,
            ),
            state,
        )
    }

    @Test
    fun `returns private ready with remaining amount when limit is configured`() = runTest {
        val useCase = GetTodayWidgetStateUseCase(
            notificationAccessStatusReader = FakeNotificationAccessStatusReader(true),
            observeTodaySummaryUseCase = observeTodaySummaryUseCase(
                events = listOf(
                    paymentEvent(
                        id = "event-1",
                        paidAt = Instant.parse("2026-04-01T12:00:00Z"),
                        amountMinor = 85000L,
                    ),
                ),
                limitAmountMinor = 100000L,
            ),
            observeYesterdayTotalUseCase = observeYesterdayTotalUseCase(emptyList()),
            widgetPrivateModeRepository = FakeWidgetPrivateModeRepository(true),
        )

        val state = useCase()

        assertEquals(
            TodayWidgetState.ReadyPrivate(
                paymentsCount = 1,
                remainingAmountMinor = 15000L,
                limitWarningLevel = DailyLimitWarningLevel.NEAR_LIMIT,
                hasLowConfidenceItems = false,
            ),
            state,
        )
    }

    private fun observeTodaySummaryUseCase(
        events: List<PaymentEvent>,
        limitAmountMinor: Long?,
    ): ObserveTodaySummaryUseCase {
        return ObserveTodaySummaryUseCase(
            paymentEventRepository = FakePaymentEventRepository(events),
            dailyLimitRepository = FakeDailyLimitRepository(limitAmountMinor),
            clock = Clock.fixed(
                Instant.parse("2026-03-31T21:30:00Z"),
                ZoneId.of("Europe/Moscow"),
            ),
        )
    }

    private fun observeYesterdayTotalUseCase(
        events: List<PaymentEvent>,
    ): ObserveYesterdayTotalUseCase {
        return ObserveYesterdayTotalUseCase(
            paymentEventRepository = FakePaymentEventRepository(events),
            clock = Clock.fixed(
                Instant.parse("2026-03-31T21:30:00Z"),
                ZoneId.of("Europe/Moscow"),
            ),
        )
    }

    private fun paymentEvent(
        id: String,
        paidAt: Instant,
        amountMinor: Long,
        confidence: ConfidenceLevel = ConfidenceLevel.MEDIUM,
        status: PaymentStatus = PaymentStatus.CONFIRMED,
    ): PaymentEvent {
        return PaymentEvent(
            id = id,
            amountMinor = amountMinor,
            currency = "RUB",
            paidAt = paidAt,
            merchantName = null,
            sourceKind = PaymentSourceKind.BANK,
            paymentChannel = PaymentChannel.UNKNOWN,
            confidence = confidence,
            status = status,
            userEdited = false,
            sourceIds = listOf("raw-$id"),
        )
    }

    private class FakeNotificationAccessStatusReader(
        private val hasNotificationAccess: Boolean,
    ) : NotificationAccessStatusReader {
        override fun hasNotificationAccess(): Boolean = hasNotificationAccess
    }

    private class FakePaymentEventRepository(
        private val events: List<PaymentEvent>,
    ) : PaymentEventRepository {
        override suspend fun save(event: PaymentEvent) = Unit

        override suspend fun saveAll(events: List<PaymentEvent>) = Unit

        override suspend fun getById(id: String): PaymentEvent? {
            return events.firstOrNull { event -> event.id == id }
        }

        override suspend fun getByDuplicateGroupId(duplicateGroupId: String): List<PaymentEvent> {
            return events.filter { event -> event.duplicateGroupId == duplicateGroupId }
        }

        override suspend fun getPaymentEventsBetween(
            startInclusive: Instant,
            endExclusive: Instant,
        ): List<PaymentEvent> {
            return events.filter { event ->
                event.paidAt >= startInclusive && event.paidAt < endExclusive
            }.sortedByDescending { it.paidAt }
        }

        override fun observePaymentEvents(): Flow<List<PaymentEvent>> {
            return flowOf(events.sortedByDescending { it.paidAt })
        }

        override fun observePaymentEventsBetween(
            startInclusive: Instant,
            endExclusive: Instant,
        ): Flow<List<PaymentEvent>> {
            return flowOf(
                events.filter { event ->
                    event.paidAt >= startInclusive && event.paidAt < endExclusive
                }.sortedByDescending { it.paidAt },
            )
        }
    }

    private class FakeWidgetPrivateModeRepository(
        initialValue: Boolean,
    ) : WidgetPrivateModeRepository {
        private val flow = MutableStateFlow(initialValue)

        override fun observeIsEnabled(): Flow<Boolean> = flow

        override fun isEnabled(): Boolean = flow.value

        override fun setEnabled(isEnabled: Boolean) {
            flow.value = isEnabled
        }
    }

    private class FakeDailyLimitRepository(
        initialValue: Long?,
    ) : DailyLimitRepository {
        private val flow = MutableStateFlow(initialValue)

        override fun observeLimitAmountMinor(): Flow<Long?> = flow

        override fun getLimitAmountMinor(): Long? = flow.value

        override fun setLimitAmountMinor(amountMinor: Long) {
            flow.value = amountMinor
        }

        override fun clearLimit() {
            flow.value = null
        }
    }
}
