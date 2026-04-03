package com.andyahmedov.enought.data.dao

import androidx.room.Dao
import androidx.room.Insert
import com.andyahmedov.enought.data.entity.PaymentEventEditEntity

@Dao
interface PaymentEventEditDao {
    @Insert
    suspend fun insert(edit: PaymentEventEditEntity)
}
