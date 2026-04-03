package com.andyahmedov.enought.domain.model

import java.time.Instant

data class PaymentCandidate(
    val amountMinor: Long?,
    val currency: String?,
    val paidAt: Instant?,
    val merchantName: String?,
    val sourceKind: PaymentSourceKind,
    val paymentChannel: PaymentChannel,
    val rawSourceId: String,
    val confidenceHints: Set<ConfidenceHint> = emptySet(),
) {
    init {
        require(rawSourceId.isNotBlank()) {
            "rawSourceId must not be blank"
        }
    }
}

