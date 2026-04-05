package com.andyahmedov.enought.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DatabaseMigrationTest {
    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EnoughtDatabase::class.java,
        emptyList(),
    )

    @Test
    fun migration4To5DeletesLegacyBankPaymentEventsOnly() {
        helper.createDatabase(TEST_DATABASE, 4).apply {
            execSQL(
                """
                INSERT INTO payment_events (
                    id, amount_minor, currency, paid_at, merchant_name, source_kind,
                    payment_channel, confidence, status, user_edited, duplicate_group_id
                ) VALUES
                    ('bank-1', 1500000, 'RUB', 1712185200000, NULL, 'BANK', 'UNKNOWN', 'MEDIUM', 'CONFIRMED', 0, NULL),
                    ('mir-1', 5999, 'RUB', 1712185718000, NULL, 'MIR_PAY', 'PHONE', 'HIGH', 'CONFIRMED', 0, NULL),
                    ('hybrid-1', 34900, 'RUB', 1712185800000, NULL, 'HYBRID', 'PHONE', 'HIGH', 'CONFIRMED', 0, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO payment_event_sources (payment_event_id, source_id) VALUES
                    ('bank-1', 'raw-bank-1'),
                    ('mir-1', 'raw-mir-1'),
                    ('hybrid-1', 'raw-mir-2')
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO payment_event_edits (
                    id, payment_event_id, edited_at, edit_type, previous_status, new_status,
                    previous_amount_minor, new_amount_minor
                ) VALUES
                    ('edit-bank-1', 'bank-1', 1712185900000, 'DISMISS', 'CONFIRMED', 'DISMISSED', 1500000, NULL),
                    ('edit-mir-1', 'mir-1', 1712186000000, 'CONFIRM', 'SUSPECTED', 'CONFIRMED', NULL, NULL)
                """.trimIndent(),
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DATABASE,
            5,
            true,
            *DatabaseFactory.ALL_MIGRATIONS,
        )

        migratedDb.query("SELECT COUNT(*) FROM payment_events WHERE source_kind = 'BANK'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        migratedDb.query("SELECT COUNT(*) FROM payment_events WHERE source_kind = 'MIR_PAY'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        migratedDb.query("SELECT COUNT(*) FROM payment_events WHERE source_kind = 'HYBRID'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        migratedDb.query("SELECT COUNT(*) FROM payment_event_sources WHERE payment_event_id = 'bank-1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        migratedDb.query("SELECT COUNT(*) FROM payment_event_edits WHERE payment_event_id = 'bank-1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        migratedDb.query("SELECT COUNT(*) FROM payment_event_edits WHERE payment_event_id = 'mir-1'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        migratedDb.close()
    }

    @Test
    fun migration5To6DeduplicatesExactMirPayRawReplays() {
        helper.createDatabase(REPLAY_TEST_DATABASE, 5).apply {
            execSQL(
                """
                INSERT INTO raw_notification_events (
                    id, source_package, posted_at, title, text, sub_text, big_text, extras_json, payload_hash
                ) VALUES
                    ('raw-mir-1', 'ru.nspk.mirpay', 1743858013076, 'Оплата покупки', 'Сумма 523 ₽. 05.04.2026, 15:43.', NULL, 'Сумма 523 ₽. 05.04.2026, 15:43.', '{}', 'hash-523'),
                    ('raw-mir-2', 'ru.nspk.mirpay', 1743858013218, 'Оплата покупки', 'Сумма 523 ₽. 05.04.2026, 15:43.', NULL, 'Сумма 523 ₽. 05.04.2026, 15:43.', '{}', 'hash-523'),
                    ('raw-mir-3', 'ru.nspk.mirpay', 1743859093216, 'Оплата покупки', 'Сумма 50 ₽. 05.04.2026, 15:58.', NULL, 'Сумма 50 ₽. 05.04.2026, 15:58.', '{}', 'hash-50'),
                    ('raw-mir-4', 'ru.nspk.mirpay', 1743859093076, 'Оплата покупки', 'Сумма 50 ₽. 05.04.2026, 15:58.', NULL, 'Сумма 50 ₽. 05.04.2026, 15:58.', '{}', 'hash-50')
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO payment_events (
                    id, amount_minor, currency, paid_at, merchant_name, source_kind,
                    payment_channel, confidence, status, user_edited, duplicate_group_id
                ) VALUES
                    ('raw-mir-1', 52300, 'RUB', 1743858180000, NULL, 'MIR_PAY', 'PHONE', 'HIGH', 'CONFIRMED', 0, NULL),
                    ('raw-mir-2', 52300, 'RUB', 1743858180000, NULL, 'MIR_PAY', 'PHONE', 'HIGH', 'CONFIRMED', 0, NULL),
                    ('raw-mir-3', 5000, 'RUB', 1743859080000, NULL, 'MIR_PAY', 'PHONE', 'HIGH', 'CONFIRMED', 0, NULL),
                    ('raw-mir-4', 5000, 'RUB', 1743859080000, NULL, 'MIR_PAY', 'PHONE', 'HIGH', 'CORRECTED', 0, NULL)
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO payment_event_sources (payment_event_id, source_id) VALUES
                    ('raw-mir-1', 'raw-mir-1'),
                    ('raw-mir-2', 'raw-mir-2'),
                    ('raw-mir-3', 'raw-mir-3'),
                    ('raw-mir-4', 'raw-mir-4')
                """.trimIndent(),
            )
            execSQL(
                """
                INSERT INTO payment_event_edits (
                    id, payment_event_id, edited_at, edit_type, previous_status, new_status,
                    previous_amount_minor, new_amount_minor
                ) VALUES
                    ('edit-mir-4', 'raw-mir-4', 1743859200000, 'CORRECT_AMOUNT', 'CONFIRMED', 'CORRECTED', 5000, 5000)
                """.trimIndent(),
            )
            close()
        }

        val migratedDb = helper.runMigrationsAndValidate(
            REPLAY_TEST_DATABASE,
            6,
            true,
            *DatabaseFactory.ALL_MIGRATIONS,
        )

        migratedDb.query("SELECT COUNT(*) FROM raw_notification_events WHERE source_package = 'ru.nspk.mirpay' AND payload_hash = 'hash-523'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        migratedDb.query("SELECT COUNT(*) FROM payment_events WHERE id = 'raw-mir-2'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        migratedDb.query("SELECT COUNT(*) FROM raw_notification_events WHERE source_package = 'ru.nspk.mirpay' AND payload_hash = 'hash-50'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        migratedDb.query("SELECT COUNT(*) FROM payment_events WHERE id = 'raw-mir-4'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        migratedDb.query("SELECT source_id FROM payment_event_sources WHERE payment_event_id = 'raw-mir-4'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("raw-mir-4", cursor.getString(0))
        }
        migratedDb.query("PRAGMA index_list(`raw_notification_events`)").use { cursor ->
            var hasUniquePayloadIndex = false
            while (cursor.moveToNext()) {
                if (cursor.getString(1) == "index_raw_notification_events_source_package_payload_hash") {
                    hasUniquePayloadIndex = cursor.getInt(2) == 1
                }
            }
            assertTrue(hasUniquePayloadIndex)
        }
        migratedDb.close()
    }

    companion object {
        private const val TEST_DATABASE = "migration-test"
        private const val REPLAY_TEST_DATABASE = "migration-test-replay"
    }
}
