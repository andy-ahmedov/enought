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

    internal val ALL_MIGRATIONS = arrayOf(
        MIGRATION_1_2,
        MIGRATION_2_3,
        MIGRATION_3_4,
        MIGRATION_4_5,
    )
}
