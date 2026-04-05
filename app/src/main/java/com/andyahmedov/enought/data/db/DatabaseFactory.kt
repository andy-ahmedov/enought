package com.andyahmedov.enought.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object DatabaseFactory {
    private const val DATABASE_NAME = "enought.db"

    fun create(context: Context): EnoughtDatabase {
        return Room.databaseBuilder(context, EnoughtDatabase::class.java, DATABASE_NAME)
            .addMigrations(*ALL_MIGRATIONS)
            .build()
    }

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `payment_events` (
                    `id` TEXT NOT NULL,
                    `amount_minor` INTEGER NOT NULL,
                    `currency` TEXT NOT NULL,
                    `paid_at` INTEGER NOT NULL,
                    `merchant_name` TEXT,
                    `source_kind` TEXT NOT NULL,
                    `payment_channel` TEXT NOT NULL,
                    `confidence` TEXT NOT NULL,
                    `status` TEXT NOT NULL,
                    `user_edited` INTEGER NOT NULL,
                    PRIMARY KEY(`id`)
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `payment_event_sources` (
                    `payment_event_id` TEXT NOT NULL,
                    `source_id` TEXT NOT NULL,
                    PRIMARY KEY(`payment_event_id`, `source_id`),
                    FOREIGN KEY(`payment_event_id`) REFERENCES `payment_events`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_payment_event_sources_payment_event_id`
                ON `payment_event_sources` (`payment_event_id`)
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_payment_event_sources_source_id`
                ON `payment_event_sources` (`source_id`)
                """.trimIndent(),
            )
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `payment_event_edits` (
                    `id` TEXT NOT NULL,
                    `payment_event_id` TEXT NOT NULL,
                    `edited_at` INTEGER NOT NULL,
                    `edit_type` TEXT NOT NULL,
                    `previous_status` TEXT NOT NULL,
                    `new_status` TEXT NOT NULL,
                    `previous_amount_minor` INTEGER,
                    `new_amount_minor` INTEGER,
                    PRIMARY KEY(`id`),
                    FOREIGN KEY(`payment_event_id`) REFERENCES `payment_events`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_payment_event_edits_payment_event_id`
                ON `payment_event_edits` (`payment_event_id`)
                """.trimIndent(),
            )
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                ALTER TABLE `payment_events`
                ADD COLUMN `duplicate_group_id` TEXT
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE INDEX IF NOT EXISTS `index_payment_events_duplicate_group_id`
                ON `payment_events` (`duplicate_group_id`)
                """.trimIndent(),
            )
        }
    }

    private val MIGRATION_4_5 = object : Migration(4, 5) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                DELETE FROM `payment_event_edits`
                WHERE `payment_event_id` IN (
                    SELECT `id` FROM `payment_events` WHERE `source_kind` = 'BANK'
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                DELETE FROM `payment_event_sources`
                WHERE `payment_event_id` IN (
                    SELECT `id` FROM `payment_events` WHERE `source_kind` = 'BANK'
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                DELETE FROM `payment_events`
                WHERE `source_kind` = 'BANK'
                """.trimIndent(),
            )
        }
    }

    private val MIGRATION_5_6 = object : Migration(5, 6) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TEMP TABLE duplicate_raw_notification_map (
                    duplicate_id TEXT NOT NULL PRIMARY KEY,
                    canonical_id TEXT NOT NULL
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT INTO duplicate_raw_notification_map (duplicate_id, canonical_id)
                SELECT duplicate_event.id,
                    (
                        SELECT canonical_event.id
                        FROM raw_notification_events AS canonical_event
                        WHERE canonical_event.source_package = duplicate_event.source_package
                            AND canonical_event.payload_hash = duplicate_event.payload_hash
                        ORDER BY canonical_event.posted_at ASC, canonical_event.id ASC
                        LIMIT 1
                    ) AS canonical_id
                FROM raw_notification_events AS duplicate_event
                WHERE duplicate_event.source_package = 'ru.nspk.mirpay'
                    AND EXISTS (
                        SELECT 1
                        FROM raw_notification_events AS canonical_event
                        WHERE canonical_event.source_package = duplicate_event.source_package
                            AND canonical_event.payload_hash = duplicate_event.payload_hash
                            AND (
                                canonical_event.posted_at < duplicate_event.posted_at
                                OR (
                                    canonical_event.posted_at = duplicate_event.posted_at
                                    AND canonical_event.id < duplicate_event.id
                                )
                            )
                    )
                """.trimIndent(),
            )
            database.execSQL(
                """
                INSERT OR IGNORE INTO payment_event_sources (payment_event_id, source_id)
                SELECT payment_event_sources.payment_event_id, duplicate_raw_notification_map.canonical_id
                FROM payment_event_sources
                JOIN duplicate_raw_notification_map
                    ON duplicate_raw_notification_map.duplicate_id = payment_event_sources.source_id
                """.trimIndent(),
            )
            database.execSQL(
                """
                DELETE FROM payment_event_sources
                WHERE source_id IN (
                    SELECT duplicate_id
                    FROM duplicate_raw_notification_map
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                DELETE FROM payment_event_sources
                WHERE payment_event_id IN (
                    SELECT payment_events.id
                    FROM payment_events
                    JOIN duplicate_raw_notification_map
                        ON duplicate_raw_notification_map.duplicate_id = payment_events.id
                    WHERE payment_events.source_kind = 'MIR_PAY'
                        AND payment_events.user_edited = 0
                        AND payment_events.duplicate_group_id IS NULL
                        AND NOT EXISTS (
                            SELECT 1
                            FROM payment_event_edits
                            WHERE payment_event_edits.payment_event_id = payment_events.id
                        )
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                DELETE FROM payment_events
                WHERE id IN (
                    SELECT payment_events.id
                    FROM payment_events
                    JOIN duplicate_raw_notification_map
                        ON duplicate_raw_notification_map.duplicate_id = payment_events.id
                    WHERE payment_events.source_kind = 'MIR_PAY'
                        AND payment_events.user_edited = 0
                        AND payment_events.duplicate_group_id IS NULL
                        AND NOT EXISTS (
                            SELECT 1
                            FROM payment_event_edits
                            WHERE payment_event_edits.payment_event_id = payment_events.id
                        )
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                DELETE FROM raw_notification_events
                WHERE id IN (
                    SELECT duplicate_id
                    FROM duplicate_raw_notification_map
                )
                """.trimIndent(),
            )
            database.execSQL(
                """
                CREATE UNIQUE INDEX IF NOT EXISTS `index_raw_notification_events_source_package_payload_hash`
                ON `raw_notification_events` (`source_package`, `payload_hash`)
                """.trimIndent(),
            )
            database.execSQL("DROP TABLE duplicate_raw_notification_map")
        }
    }

    internal val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
        MIGRATION_5_6,
    )
}
