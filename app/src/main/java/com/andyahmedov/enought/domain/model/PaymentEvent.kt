package com.andyahmedov.enought.domain.model

import java.time.Instant

data class PaymentEvent(
    val id: String,
    val amountMinor: Long,
    val currency: String,
    val paidAt: Instant,
    val merchantName: String?,
    val sourceKind: PaymentSourceKind,
    val paymentChannel: PaymentChannel,
    val confidence: ConfidenceLevel,
    val status: PaymentStatus,
    val userEdited: Boolean,
    val sourceIds: List<String>,
    val duplicateGroupId: String? = null,
) {
    init {
        require(id.isNotBlank()) {
            "id must not be blank"
        }
        require(currency.isNotBlank()) {
            "currency must not be blank"
        }
    }
}
