package com.andyahmedov.enought.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.andyahmedov.enought.data.entity.PaymentEventEditEntity

@Dao
interface PaymentEventEditDao {
    @Insert
    suspend fun insert(edit: PaymentEventEditEntity)

    @Query(
        """
        SELECT * FROM payment_event_edits
        WHERE payment_event_id IN (:paymentEventIds)
        ORDER BY edited_at DESC
        """,
    )
    suspend fun getByPaymentEventIds(paymentEventIds: List<String>): List<PaymentEventEditEntity>
}
