package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.domain.model.RawNotificationEvent
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import com.andyahmedov.enought.normalization.PaymentEventNormalizer
import com.andyahmedov.enought.parsing.NotificationParserRegistry
import com.andyahmedov.enought.widget.WidgetUpdater

class ProcessIncomingRawEventUseCase(
    private val parserRegistry: NotificationParserRegistry,
    private val paymentEventNormalizer: PaymentEventNormalizer,
    private val deduplicatePaymentEventUseCase: DeduplicatePaymentEventUseCase,
    private val paymentEventRepository: PaymentEventRepository,
    private val widgetUpdater: WidgetUpdater,
) {
    suspend operator fun invoke(rawEvent: RawNotificationEvent) {
        val candidate = parserRegistry.parse(rawEvent) ?: return
        val paymentEvent = paymentEventNormalizer.normalize(candidate) ?: return
        val deduplicationResult = deduplicatePaymentEventUseCase(paymentEvent)

        paymentEventRepository.saveAll(deduplicationResult.eventsToSave)
        widgetUpdater.refresh()
    }
}
