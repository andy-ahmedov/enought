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

    override suspend fun getByPaymentEventIds(paymentEventIds: List<String>): List<PaymentEventEdit> {
        if (paymentEventIds.isEmpty()) {
            return emptyList()
        }

        return paymentEventEditDao.getByPaymentEventIds(paymentEventIds).map { entity ->
            entity.toDomain()
        }
    }
}
