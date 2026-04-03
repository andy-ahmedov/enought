package com.andyahmedov.enought.domain.model

import java.time.Instant

data class PaymentEventEdit(
    val id: String,
    val paymentEventId: String,
    val editedAt: Instant,
    val editType: PaymentEditType,
    val previousStatus: PaymentStatus,
    val newStatus: PaymentStatus,
    val previousAmountMinor: Long?,
    val newAmountMinor: Long?,
) {
    init {
        require(id.isNotBlank()) {
            "id must not be blank"
        }
        require(paymentEventId.isNotBlank()) {
            "paymentEventId must not be blank"
        }
    }
}
