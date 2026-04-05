package com.andyahmedov.enought.domain.usecase

import com.andyahmedov.enought.common.toRubDisplayString
import com.andyahmedov.enought.domain.model.PaymentEvent
import com.andyahmedov.enought.domain.model.PaymentEventEdit
import com.andyahmedov.enought.domain.model.PaymentStatus
import com.andyahmedov.enought.domain.model.RawNotificationEvent
import com.andyahmedov.enought.domain.repository.PaymentEventEditRepository
import com.andyahmedov.enought.domain.repository.PaymentEventRepository
import com.andyahmedov.enought.domain.repository.RawNotificationEventRepository
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

class BuildDiagnosticsReportUseCase(
    private val rawNotificationEventRepository: RawNotificationEventRepository,
    private val paymentEventRepository: PaymentEventRepository,
    private val paymentEventEditRepository: PaymentEventEditRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(): String {
        val generatedAt = clock.instant()
        val today = LocalDate.now(clock)
        val dayRange = today.toInstantRange(clock)
        val rawEvents = rawNotificationEventRepository.getRawEventsBetween(
            startInclusive = dayRange.startInclusive,
            endExclusive = dayRange.endExclusive,
        )
        val paymentEvents = paymentEventRepository.getPaymentEventsBetween(
            startInclusive = dayRange.startInclusive,
            endExclusive = dayRange.endExclusive,
        )
        val manualEdits = paymentEventEditRepository.getByPaymentEventIds(
            paymentEventIds = paymentEvents.map { event -> event.id },
        )

        return buildDiagnosticsReport(
            generatedAt = generatedAt,
            today = today,
            dayRange = dayRange,
            rawEvents = rawEvents,
            paymentEvents = paymentEvents,
            manualEdits = manualEdits,
            timezoneId = clock.zone.id,
        )
    }
}

internal fun buildDiagnosticsReport(
    generatedAt: Instant,
    today: LocalDate,
    dayRange: DiagnosticsInstantRange,
    rawEvents: List<RawNotificationEvent>,
    paymentEvents: List<PaymentEvent>,
    manualEdits: List<PaymentEventEdit>,
    timezoneId: String,
): String {
    val rawToPayments = paymentEvents
        .flatMap { paymentEvent ->
            paymentEvent.sourceIds.map { sourceId -> sourceId to paymentEvent }
        }
        .groupBy(
            keySelector = { (sourceId, _) -> sourceId },
            valueTransform = { (_, paymentEvent) -> paymentEvent },
        )
    val paymentEventsById = paymentEvents.associateBy { event -> event.id }
    val editedPaymentIds = manualEdits.map { edit -> edit.paymentEventId }.toSet()
    val editedPaymentCount = paymentEvents.count { event ->
        event.userEdited || event.id in editedPaymentIds
    }

    return buildString {
        appendLine("Enought diagnostics report")
        appendLine("Generated at: $generatedAt")
        appendLine("Timezone: $timezoneId")
        appendLine("Window date: $today")
        appendLine("Window start: ${dayRange.startInclusive}")
        appendLine("Window end: ${dayRange.endExclusive}")
        appendLine()
        appendLine("Counts:")
        appendLine("- raw notifications: ${rawEvents.size}")
        appendLine("- payment events: ${paymentEvents.size}")
        appendLine("- suspected payment events: ${paymentEvents.count { event -> event.status == PaymentStatus.SUSPECTED }}")
        appendLine("- edited payment events: $editedPaymentCount")
        appendLine()
        appendLine("Raw notifications:")
        if (rawEvents.isEmpty()) {
            appendLine("- none")
        } else {
            rawEvents.forEachIndexed { index, rawEvent ->
                val linkedPayments = rawToPayments[rawEvent.id].orEmpty().sortedByDescending { event -> event.paidAt }
                appendLine("${index + 1}. raw_id=${rawEvent.id}")
                appendLine("   source_package=${rawEvent.sourcePackage}")
                appendLine("   posted_at=${rawEvent.postedAt}")
                appendLine("   title=${rawEvent.title.toDiagnosticsValue()}")
                appendLine("   text=${rawEvent.text.toDiagnosticsValue()}")
                appendLine("   sub_text=${rawEvent.subText.toDiagnosticsValue()}")
                appendLine("   big_text=${rawEvent.bigText.toDiagnosticsValue()}")
                appendLine("   extras_json=${rawEvent.extrasJson.toDiagnosticsValue()}")
                appendLine("   payload_hash=${rawEvent.payloadHash}")
                appendLine("   processing=${linkedPayments.toProcessingOutcome()}")
            }
        }
        appendLine()
        appendLine("Payment events:")
        if (paymentEvents.isEmpty()) {
            appendLine("- none")
        } else {
            paymentEvents.forEachIndexed { index, paymentEvent ->
                appendLine("${index + 1}. payment_event_id=${paymentEvent.id}")
                appendLine("   amount=${paymentEvent.toAmountDisplayString()}")
                appendLine("   currency=${paymentEvent.currency}")
                appendLine("   paid_at=${paymentEvent.paidAt}")
                appendLine("   merchant_name=${paymentEvent.merchantName.toDiagnosticsValue()}")
                appendLine("   source_kind=${paymentEvent.sourceKind}")
                appendLine("   payment_channel=${paymentEvent.paymentChannel}")
                appendLine("   confidence=${paymentEvent.confidence}")
                appendLine("   status=${paymentEvent.status}")
                appendLine("   user_edited=${paymentEvent.userEdited}")
                appendLine("   duplicate_group_id=${paymentEvent.duplicateGroupId.toDiagnosticsValue()}")
                appendLine("   source_ids=${paymentEvent.sourceIds.joinToString(separator = ", ")}")
            }
        }
        appendLine()
        appendLine("Manual edits:")
        if (manualEdits.isEmpty()) {
            appendLine("- none")
        } else {
            manualEdits.forEachIndexed { index, manualEdit ->
                val paymentEvent = paymentEventsById[manualEdit.paymentEventId]
                appendLine("${index + 1}. edit_id=${manualEdit.id}")
                appendLine("   payment_event_id=${manualEdit.paymentEventId}")
                appendLine("   edited_at=${manualEdit.editedAt}")
                appendLine("   edit_type=${manualEdit.editType}")
                appendLine("   previous_status=${manualEdit.previousStatus}")
                appendLine("   new_status=${manualEdit.newStatus}")
                appendLine("   previous_amount=${manualEdit.previousAmountMinor.toAmountDisplayString(paymentEvent?.currency)}")
                appendLine("   new_amount=${manualEdit.newAmountMinor.toAmountDisplayString(paymentEvent?.currency)}")
            }
        }
    }.trimEnd()
}

internal data class DiagnosticsInstantRange(
    val startInclusive: Instant,
    val endExclusive: Instant,
)

private fun LocalDate.toInstantRange(clock: Clock): DiagnosticsInstantRange {
    val zoneId = clock.zone
    return DiagnosticsInstantRange(
        startInclusive = atStartOfDay(zoneId).toInstant(),
        endExclusive = plusDays(1).atStartOfDay(zoneId).toInstant(),
    )
}

private fun List<PaymentEvent>.toProcessingOutcome(): String {
    if (isEmpty()) {
        return "not_promoted_to_payment_event"
    }

    return joinToString(
        prefix = "linked_payment_events=",
        separator = "; ",
    ) { event ->
        "${event.id}(${event.status}/${event.sourceKind})"
    }
}

private fun PaymentEvent.toAmountDisplayString(): String {
    return amountMinor.toAmountDisplayString(currency)
}

private fun Long?.toAmountDisplayString(currency: String?): String {
    val amountMinor = this ?: return "<null>"
    return if (currency == "RUB") {
        amountMinor.toRubDisplayString()
    } else {
        "$amountMinor minor ${currency ?: "<unknown>"}"
    }
}

private fun String?.toDiagnosticsValue(): String {
    return when {
        this == null -> "<null>"
        isBlank() -> "<blank>"
        else -> replace("\n", "\\n")
    }
}
