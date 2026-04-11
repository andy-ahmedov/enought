package com.andyahmedov.enought.domain.repository

import com.andyahmedov.enought.domain.model.PaymentEvent
import kotlinx.coroutines.flow.Flow
import java.time.Instant

interface PaymentEventRepository {
    suspend fun save(event: PaymentEvent)

    suspend fun saveAll(events: List<PaymentEvent>)

    suspend fun getById(id: String): PaymentEvent?

    suspend fun getByDuplicateGroupId(duplicateGroupId: String): List<PaymentEvent>

    suspend fun getPaymentEventsBetween(
        startInclusive: Instant,
        endExclusive: Instant,
    ): List<PaymentEvent>

    fun observePaymentEvents(): Flow<List<PaymentEvent>>

    fun observePaymentEventsBetween(
        startInclusive: Instant,
        endExclusive: Instant,
    ): Flow<List<PaymentEvent>>

    suspend fun deleteOlderThan(
        cutoffExclusive: Instant,
    ) = Unit
}
