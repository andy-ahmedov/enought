package com.andyahmedov.enought.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PaymentEventWithSources(
    @Embedded
    val event: PaymentEventEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "payment_event_id",
    )
    val sourceEntities: List<PaymentEventSourceEntity>,
)
