package com.andyahmedov.enought.domain.repository

import com.andyahmedov.enought.domain.model.PaymentEventEdit

interface PaymentEventEditRepository {
    suspend fun save(edit: PaymentEventEdit)

    suspend fun getByPaymentEventIds(paymentEventIds: List<String>): List<PaymentEventEdit>
}
