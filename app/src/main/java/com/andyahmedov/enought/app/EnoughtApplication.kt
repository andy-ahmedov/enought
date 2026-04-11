package com.andyahmedov.enought.app

import android.app.Application
import android.content.Context
import android.content.Context.MODE_PRIVATE
import com.andyahmedov.enought.collection.notifications.NotificationAccessStatusReader
import com.andyahmedov.enought.collection.notifications.NotificationAccessSettingsLauncher
import com.andyahmedov.enought.collection.notifications.NotificationSourcePolicy
import com.andyahmedov.enought.collection.notifications.SystemNotificationAccessSettingsLauncher
import com.andyahmedov.enought.collection.notifications.SystemNotificationAccessStatusReader
import com.andyahmedov.enought.common.DiagnosticsLogClipboardWriter
import com.andyahmedov.enought.common.DiagnosticsLogShareLauncher
import com.andyahmedov.enought.common.SystemDiagnosticsLogClipboardWriter
import com.andyahmedov.enought.common.SystemDiagnosticsLogShareLauncher
import com.andyahmedov.enought.data.db.DatabaseFactory
import com.andyahmedov.enought.data.repository.SharedPreferencesDailyLimitRepository
import com.andyahmedov.enought.data.repository.RoomPaymentEventEditRepository
import com.andyahmedov.enought.data.repository.RoomPaymentEventRepository
import com.andyahmedov.enought.data.repository.RoomRawNotificationEventRepository
import com.andyahmedov.enought.data.repository.SharedPreferencesWidgetPrivateModeRepository
import com.andyahmedov.enought.domain.repository.DailyLimitRepository
import com.andyahmedov.enought.domain.repository.PaymentEventEditRepository
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import com.andyahmedov.enought.domain.repository.RawNotificationEventRepository
import com.andyahmedov.enought.domain.repository.WidgetPrivateModeRepository
import com.andyahmedov.enought.domain.usecase.BuildDiagnosticsReportUseCase
import com.andyahmedov.enought.domain.usecase.ClearDailyLimitUseCase
import com.andyahmedov.enought.domain.usecase.ConfirmPaymentEventUseCase
import com.andyahmedov.enought.domain.usecase.CorrectPaymentAmountUseCase
import com.andyahmedov.enought.domain.usecase.DeduplicatePaymentEventUseCase
import com.andyahmedov.enought.domain.usecase.DismissPaymentEventUseCase
import com.andyahmedov.enought.domain.usecase.EnforceDataRetentionUseCase
import com.andyahmedov.enought.domain.usecase.KeepDuplicateConflictSeparateUseCase
import com.andyahmedov.enought.domain.usecase.MergeDuplicateConflictUseCase
import com.andyahmedov.enought.domain.usecase.ObserveDailyLimitUseCase
import com.andyahmedov.enought.domain.usecase.ObserveHistoryPeriodSnapshotUseCase
import com.andyahmedov.enought.domain.usecase.ObserveTodayPaymentEventsUseCase
import com.andyahmedov.enought.domain.usecase.ObserveTodayReviewItemsUseCase
import com.andyahmedov.enought.domain.usecase.ObserveTodaySummaryUseCase
import com.andyahmedov.enought.domain.usecase.ObserveWidgetPrivateModeUseCase
import com.andyahmedov.enought.domain.usecase.ObserveYesterdayTotalUseCase
import com.andyahmedov.enought.domain.usecase.ProcessIncomingRawEventUseCase
import com.andyahmedov.enought.domain.usecase.SetDailyLimitUseCase
import com.andyahmedov.enought.domain.usecase.SetWidgetPrivateModeUseCase
import com.andyahmedov.enought.normalization.DefaultPaymentEventNormalizer
import com.andyahmedov.enought.normalization.PaymentEventNormalizer
import com.andyahmedov.enought.parsing.NotificationParserRegistry
import com.andyahmedov.enought.widget.GetTodayWidgetStateUseCase
import com.andyahmedov.enought.widget.GlanceWidgetUpdater
import com.andyahmedov.enought.widget.WidgetUpdater
import java.time.Clock

class EnoughtApplication : Application() {
    val appContainer: EnoughtAppContainer by lazy {
        DefaultEnoughtAppContainer(applicationContext)
    }
}

interface EnoughtAppContainer {
    val rawNotificationEventRepository: RawNotificationEventRepository
    val paymentEventRepository: PaymentEventRepository
    val paymentEventEditRepository: PaymentEventEditRepository
    val dailyLimitRepository: DailyLimitRepository
    val widgetPrivateModeRepository: WidgetPrivateModeRepository
    val notificationSourcePolicy: NotificationSourcePolicy
    val notificationAccessStatusReader: NotificationAccessStatusReader
    val notificationAccessSettingsLauncher: NotificationAccessSettingsLauncher
    val processIncomingRawEventUseCase: ProcessIncomingRawEventUseCase
    val buildDiagnosticsReportUseCase: BuildDiagnosticsReportUseCase
    val enforceDataRetentionUseCase: EnforceDataRetentionUseCase
    val observeHistoryPeriodSnapshotUseCase: ObserveHistoryPeriodSnapshotUseCase
    val observeTodaySummaryUseCase: ObserveTodaySummaryUseCase
    val observeTodayPaymentEventsUseCase: ObserveTodayPaymentEventsUseCase
    val observeTodayReviewItemsUseCase: ObserveTodayReviewItemsUseCase
    val observeYesterdayTotalUseCase: ObserveYesterdayTotalUseCase
    val observeDailyLimitUseCase: ObserveDailyLimitUseCase
    val observeWidgetPrivateModeUseCase: ObserveWidgetPrivateModeUseCase
    val confirmPaymentEventUseCase: ConfirmPaymentEventUseCase
    val correctPaymentAmountUseCase: CorrectPaymentAmountUseCase
    val dismissPaymentEventUseCase: DismissPaymentEventUseCase
    val mergeDuplicateConflictUseCase: MergeDuplicateConflictUseCase
    val keepDuplicateConflictSeparateUseCase: KeepDuplicateConflictSeparateUseCase
    val setDailyLimitUseCase: SetDailyLimitUseCase
    val clearDailyLimitUseCase: ClearDailyLimitUseCase
    val setWidgetPrivateModeUseCase: SetWidgetPrivateModeUseCase
    val getTodayWidgetStateUseCase: GetTodayWidgetStateUseCase
    val widgetUpdater: WidgetUpdater
    val diagnosticsLogClipboardWriter: DiagnosticsLogClipboardWriter
    val diagnosticsLogShareLauncher: DiagnosticsLogShareLauncher
}

private class DefaultEnoughtAppContainer(
    context: Context,
) : EnoughtAppContainer {
    private val database = DatabaseFactory.create(context)
    private val parserRegistry = NotificationParserRegistry.default()
    private val paymentEventNormalizer: PaymentEventNormalizer = DefaultPaymentEventNormalizer()
    private val sharedPreferences = context.getSharedPreferences(PREFERENCES_NAME, MODE_PRIVATE)

    override val rawNotificationEventRepository: RawNotificationEventRepository =
        RoomRawNotificationEventRepository(database.rawNotificationEventDao())

    override val paymentEventRepository: PaymentEventRepository =
        RoomPaymentEventRepository(database.paymentEventDao())

    private val deduplicatePaymentEventUseCase = DeduplicatePaymentEventUseCase(
        paymentEventRepository = paymentEventRepository,
    )

    override val paymentEventEditRepository: PaymentEventEditRepository =
        RoomPaymentEventEditRepository(database.paymentEventEditDao())

    override val dailyLimitRepository: DailyLimitRepository =
        SharedPreferencesDailyLimitRepository(
            sharedPreferences = sharedPreferences,
        )

    override val widgetPrivateModeRepository: WidgetPrivateModeRepository =
        SharedPreferencesWidgetPrivateModeRepository(
            sharedPreferences = sharedPreferences,
        )

    override val notificationSourcePolicy: NotificationSourcePolicy =
        NotificationSourcePolicy.default()

    override val notificationAccessStatusReader: NotificationAccessStatusReader =
        SystemNotificationAccessStatusReader(context)

    override val notificationAccessSettingsLauncher: NotificationAccessSettingsLauncher =
        SystemNotificationAccessSettingsLauncher()

    override val diagnosticsLogClipboardWriter: DiagnosticsLogClipboardWriter =
        SystemDiagnosticsLogClipboardWriter()

    override val diagnosticsLogShareLauncher: DiagnosticsLogShareLauncher =
        SystemDiagnosticsLogShareLauncher()

    override val observeTodaySummaryUseCase: ObserveTodaySummaryUseCase =
        ObserveTodaySummaryUseCase(
            paymentEventRepository = paymentEventRepository,
            dailyLimitRepository = dailyLimitRepository,
            clock = Clock.systemDefaultZone(),
        )

    override val buildDiagnosticsReportUseCase: BuildDiagnosticsReportUseCase =
        BuildDiagnosticsReportUseCase(
            rawNotificationEventRepository = rawNotificationEventRepository,
            paymentEventRepository = paymentEventRepository,
            paymentEventEditRepository = paymentEventEditRepository,
            clock = Clock.systemDefaultZone(),
        )

    override val enforceDataRetentionUseCase: EnforceDataRetentionUseCase =
        EnforceDataRetentionUseCase(
            paymentEventRepository = paymentEventRepository,
            rawNotificationEventRepository = rawNotificationEventRepository,
            clock = Clock.systemDefaultZone(),
        )

    override val observeTodayPaymentEventsUseCase: ObserveTodayPaymentEventsUseCase =
        ObserveTodayPaymentEventsUseCase(
            paymentEventRepository = paymentEventRepository,
            clock = Clock.systemDefaultZone(),
        )

    override val observeHistoryPeriodSnapshotUseCase: ObserveHistoryPeriodSnapshotUseCase =
        ObserveHistoryPeriodSnapshotUseCase(
            paymentEventRepository = paymentEventRepository,
            clock = Clock.systemDefaultZone(),
        )

    override val observeTodayReviewItemsUseCase: ObserveTodayReviewItemsUseCase =
        ObserveTodayReviewItemsUseCase(
            paymentEventRepository = paymentEventRepository,
            clock = Clock.systemDefaultZone(),
        )

    override val observeYesterdayTotalUseCase: ObserveYesterdayTotalUseCase =
        ObserveYesterdayTotalUseCase(
            paymentEventRepository = paymentEventRepository,
            clock = Clock.systemDefaultZone(),
        )

    override val getTodayWidgetStateUseCase: GetTodayWidgetStateUseCase =
        GetTodayWidgetStateUseCase(
            notificationAccessStatusReader = notificationAccessStatusReader,
            observeTodaySummaryUseCase = observeTodaySummaryUseCase,
            observeYesterdayTotalUseCase = observeYesterdayTotalUseCase,
            widgetPrivateModeRepository = widgetPrivateModeRepository,
        )

    override val widgetUpdater: WidgetUpdater =
        GlanceWidgetUpdater(context)

    override val observeWidgetPrivateModeUseCase: ObserveWidgetPrivateModeUseCase =
        ObserveWidgetPrivateModeUseCase(
            widgetPrivateModeRepository = widgetPrivateModeRepository,
        )

    override val observeDailyLimitUseCase: ObserveDailyLimitUseCase =
        ObserveDailyLimitUseCase(
            dailyLimitRepository = dailyLimitRepository,
        )

    override val setWidgetPrivateModeUseCase: SetWidgetPrivateModeUseCase =
        SetWidgetPrivateModeUseCase(
            widgetPrivateModeRepository = widgetPrivateModeRepository,
            widgetUpdater = widgetUpdater,
        )

    override val setDailyLimitUseCase: SetDailyLimitUseCase =
        SetDailyLimitUseCase(
            dailyLimitRepository = dailyLimitRepository,
            widgetUpdater = widgetUpdater,
        )

    override val clearDailyLimitUseCase: ClearDailyLimitUseCase =
        ClearDailyLimitUseCase(
            dailyLimitRepository = dailyLimitRepository,
            widgetUpdater = widgetUpdater,
        )

    override val confirmPaymentEventUseCase: ConfirmPaymentEventUseCase =
        ConfirmPaymentEventUseCase(
            paymentEventRepository = paymentEventRepository,
            paymentEventEditRepository = paymentEventEditRepository,
            widgetUpdater = widgetUpdater,
            clock = Clock.systemDefaultZone(),
        )

    override val correctPaymentAmountUseCase: CorrectPaymentAmountUseCase =
        CorrectPaymentAmountUseCase(
            paymentEventRepository = paymentEventRepository,
            paymentEventEditRepository = paymentEventEditRepository,
            widgetUpdater = widgetUpdater,
            clock = Clock.systemDefaultZone(),
        )

    override val dismissPaymentEventUseCase: DismissPaymentEventUseCase =
        DismissPaymentEventUseCase(
            paymentEventRepository = paymentEventRepository,
            paymentEventEditRepository = paymentEventEditRepository,
            widgetUpdater = widgetUpdater,
            clock = Clock.systemDefaultZone(),
        )

    override val mergeDuplicateConflictUseCase: MergeDuplicateConflictUseCase =
        MergeDuplicateConflictUseCase(
            paymentEventRepository = paymentEventRepository,
            paymentEventEditRepository = paymentEventEditRepository,
            widgetUpdater = widgetUpdater,
            clock = Clock.systemDefaultZone(),
        )

    override val keepDuplicateConflictSeparateUseCase: KeepDuplicateConflictSeparateUseCase =
        KeepDuplicateConflictSeparateUseCase(
            paymentEventRepository = paymentEventRepository,
            paymentEventEditRepository = paymentEventEditRepository,
            widgetUpdater = widgetUpdater,
            clock = Clock.systemDefaultZone(),
        )

    override val processIncomingRawEventUseCase: ProcessIncomingRawEventUseCase =
        ProcessIncomingRawEventUseCase(
            parserRegistry = parserRegistry,
            paymentEventNormalizer = paymentEventNormalizer,
            deduplicatePaymentEventUseCase = deduplicatePaymentEventUseCase,
            paymentEventRepository = paymentEventRepository,
            widgetUpdater = widgetUpdater,
        )
}

val Context.appContainer: EnoughtAppContainer
    get() = (applicationContext as EnoughtApplication).appContainer

private const val PREFERENCES_NAME = "widget_preferences"
