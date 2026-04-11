package com.andyahmedov.enought.data.repository

import com.andyahmedov.enought.data.dao.RawNotificationEventDao
import com.andyahmedov.enought.data.entity.RawNotificationEventEntity
import com.andyahmedov.enought.domain.model.RawNotificationEvent
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomRawNotificationEventRepositoryTest {
    @Test
    fun `saveIfNew returns true for first insert and false for duplicate`() = runTest {
        val dao = FakeRawNotificationEventDao()
        val repository = RoomRawNotificationEventRepository(dao)
        val first = rawEvent(id = "raw-1", payloadHash = "hash-1")
        val duplicate = rawEvent(id = "raw-2", payloadHash = "hash-1")

        assertTrue(repository.saveIfNew(first))
        assertFalse(repository.saveIfNew(duplicate))
        assertEquals(listOf("raw-1"), dao.events.map { event -> event.id })
    }

    @Test
    fun `saveIfNew inserts another event when payload hash differs`() = runTest {
        val dao = FakeRawNotificationEventDao()
        val repository = RoomRawNotificationEventRepository(dao)

        assertTrue(repository.saveIfNew(rawEvent(id = "raw-1", payloadHash = "hash-1")))
        assertTrue(repository.saveIfNew(rawEvent(id = "raw-2", payloadHash = "hash-2")))
        assertEquals(listOf("raw-1", "raw-2"), dao.events.map { event -> event.id })
    }

    @Test
    fun `getRawEventsBetween forwards bounds and maps domain events`() = runTest {
        val dao = FakeRawNotificationEventDao(
            initialEvents = listOf(
                rawEvent(id = "raw-1", payloadHash = "hash-1", postedAt = Instant.parse("2026-04-04T23:59:59Z")).toEntity(),
                rawEvent(id = "raw-2", payloadHash = "hash-2", postedAt = Instant.parse("2026-04-05T13:00:13Z")).toEntity(),
                rawEvent(id = "raw-3", payloadHash = "hash-3", postedAt = Instant.parse("2026-04-05T14:00:13Z")).toEntity(),
            ),
        )
        val repository = RoomRawNotificationEventRepository(dao)
        val startInclusive = Instant.parse("2026-04-05T00:00:00Z")
        val endExclusive = Instant.parse("2026-04-06T00:00:00Z")

        val events = repository.getRawEventsBetween(
            startInclusive = startInclusive,
            endExclusive = endExclusive,
        )

        assertEquals(startInclusive, dao.lastStartInclusive)
        assertEquals(endExclusive, dao.lastEndExclusive)
        assertEquals(listOf("raw-3", "raw-2"), events.map { event -> event.id })
    }

    @Test
    fun `deleteOlderThan forwards cutoff to dao`() = runTest {
        val dao = FakeRawNotificationEventDao()
        val repository = RoomRawNotificationEventRepository(dao)
        val cutoffExclusive = Instant.parse("2026-01-01T21:00:00Z")

        repository.deleteOlderThan(cutoffExclusive)

        assertEquals(cutoffExclusive, dao.lastDeleteOlderThanCutoff)
    }

    private fun rawEvent(
        id: String,
        payloadHash: String,
        postedAt: Instant = Instant.parse("2026-04-05T13:00:13Z"),
    ): RawNotificationEvent {
        return RawNotificationEvent(
            id = id,
            sourcePackage = "ru.nspk.mirpay",
            postedAt = postedAt,
            title = "Оплата покупки",
            text = "Сумма 523 ₽",
            subText = null,
            bigText = "Сумма 523 ₽",
            extrasJson = "{}",
            payloadHash = payloadHash,
        )
    }

    private class FakeRawNotificationEventDao : RawNotificationEventDao {
        constructor(
            initialEvents: List<RawNotificationEventEntity> = emptyList(),
        ) {
            events += initialEvents
        }

        val events = mutableListOf<RawNotificationEventEntity>()
        var lastStartInclusive: Instant? = null
        var lastEndExclusive: Instant? = null
        var lastDeleteOlderThanCutoff: Instant? = null

        override suspend fun insertIgnore(event: RawNotificationEventEntity): Long {
            val duplicate = events.any { existing ->
                existing.sourcePackage == event.sourcePackage && existing.payloadHash == event.payloadHash
            }
            if (duplicate) {
                return INSERT_FAILED
            }

            events += event
            return events.size.toLong()
        }

        override suspend fun getByPostedAtBetween(
            startInclusive: Instant,
            endExclusive: Instant,
        ): List<RawNotificationEventEntity> {
            lastStartInclusive = startInclusive
            lastEndExclusive = endExclusive
            return events.filter { event ->
                event.postedAt >= startInclusive && event.postedAt < endExclusive
            }.sortedByDescending { event -> event.postedAt }
        }

        override fun observeAllByPostedAtDesc(): Flow<List<RawNotificationEventEntity>> {
            return flowOf(events.sortedByDescending { event -> event.postedAt })
        }

        override suspend fun getAllByPostedAtDesc(): List<RawNotificationEventEntity> {
            return events.sortedByDescending { event -> event.postedAt }
        }

        override suspend fun deleteOlderThan(cutoffExclusive: Instant) {
            lastDeleteOlderThanCutoff = cutoffExclusive
        }

        private companion object {
            const val INSERT_FAILED = -1L
        }
    }
}
