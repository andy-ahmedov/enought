package com.andyahmedov.enought.data.repository

import com.andyahmedov.enought.data.dao.PaymentEventEditDao
import com.andyahmedov.enought.domain.model.PaymentEventEdit
import com.andyahmedov.enought.domain.repository.PaymentEventEditRepository

class RoomPaymentEventEditRepository(
    private val paymentEventEditDao: PaymentEventEditDao,
) : PaymentEventEditRepository {
    override suspend fun save(edit: PaymentEventEdit) {
        paymentEventEditDao.insert(edit.toEntity())
    }
}
