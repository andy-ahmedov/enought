package com.andyahmedov.enought.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.andyahmedov.enought.data.entity.PaymentEventEntity
import com.andyahmedov.enought.data.entity.PaymentEventSourceEntity
import com.andyahmedov.enought.data.entity.PaymentEventWithSources
import kotlinx.coroutines.flow.Flow

@Dao
interface PaymentEventDao {
    @Upsert
    suspend fun upsertEvent(event: PaymentEventEntity)

    @Query("DELETE FROM payment_event_sources WHERE payment_event_id = :paymentEventId")
    suspend fun deleteSourcesForEvent(paymentEventId: String)

    @Insert
    suspend fun insertSources(sources: List<PaymentEventSourceEntity>)

    @Transaction
    suspend fun upsertWithSources(
        event: PaymentEventEntity,
        sources: List<PaymentEventSourceEntity>,
    ) {
        upsertEvent(event)
        deleteSourcesForEvent(event.id)

        if (sources.isNotEmpty()) {
            insertSources(sources)
        }
    }

    @Transaction
    suspend fun upsertAllWithSources(
        eventsWithSources: List<Pair<PaymentEventEntity, List<PaymentEventSourceEntity>>>,
    ) {
        eventsWithSources.forEach { (event, sources) ->
            upsertWithSources(
                event = event,
                sources = sources,
            )
        }
    }

    @Transaction
    @Query("SELECT * FROM payment_events ORDER BY paid_at DESC")
    fun observeAllByPaidAtDesc(): Flow<List<PaymentEventWithSources>>

    @Transaction
    @Query("SELECT * FROM payment_events WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): PaymentEventWithSources?

    @Transaction
    @Query(
        """
        SELECT * FROM payment_events
        WHERE duplicate_group_id = :duplicateGroupId
        ORDER BY paid_at DESC
        """,
    )
    suspend fun getByDuplicateGroupId(duplicateGroupId: String): List<PaymentEventWithSources>

    @Transaction
    @Query(
        """
        SELECT * FROM payment_events
        WHERE paid_at >= :startInclusive AND paid_at < :endExclusive
        ORDER BY paid_at DESC
        """,
    )
    suspend fun getByPaidAtBetween(
        startInclusive: java.time.Instant,
        endExclusive: java.time.Instant,
    ): List<PaymentEventWithSources>

    @Transaction
    @Query(
        """
        SELECT * FROM payment_events
        WHERE paid_at >= :startInclusive AND paid_at < :endExclusive
        ORDER BY paid_at DESC
        """,
    )
    fun observeByPaidAtBetween(
        startInclusive: java.time.Instant,
        endExclusive: java.time.Instant,
    ): Flow<List<PaymentEventWithSources>>
}
