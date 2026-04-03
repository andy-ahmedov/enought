package com.andyahmedov.enought.data.repository

import com.andyahmedov.enought.data.entity.RawNotificationEventEntity
import com.andyahmedov.enought.domain.model.RawNotificationEvent
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Test

class RawNotificationEventMappersTest {
    @Test
    fun domainEvent_mapsToEntity() {
        val domainEvent = RawNotificationEvent(
            id = "raw-1",
            sourcePackage = "ru.bank.app",
            postedAt = Instant.parse("2026-03-31T08:30:00Z"),
            title = "Bank",
            text = "349.00 RUB",
            subText = "Card purchase",
            bigText = null,
            extrasJson = "{\"channel\":\"phone\"}",
            payloadHash = "hash-1",
        )

        val entity = domainEvent.toEntity()

        assertEquals(domainEvent.id, entity.id)
        assertEquals(domainEvent.sourcePackage, entity.sourcePackage)
        assertEquals(domainEvent.postedAt, entity.postedAt)
        assertEquals(domainEvent.payloadHash, entity.payloadHash)
    }

    @Test
    fun entity_mapsToDomainEvent() {
        val entity = RawNotificationEventEntity(
            id = "raw-2",
            sourcePackage = "ru.mirpay",
            postedAt = Instant.parse("2026-03-31T09:45:00Z"),
            title = "Mir Pay",
            text = "Payment completed",
            subText = null,
            bigText = "Coffee shop",
            extrasJson = null,
            payloadHash = "hash-2",
        )

        val domainEvent = entity.toDomain()

        assertEquals(entity.id, domainEvent.id)
        assertEquals(entity.sourcePackage, domainEvent.sourcePackage)
        assertEquals(entity.postedAt, domainEvent.postedAt)
        assertEquals(entity.bigText, domainEvent.bigText)
        assertEquals(entity.payloadHash, domainEvent.payloadHash)
    }
}
