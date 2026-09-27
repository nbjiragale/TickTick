package com.niranjan.ticktick.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `reminder_occurrences` (`taskId` TEXT NOT NULL, `id` TEXT NOT NULL, `fingerprint` TEXT NOT NULL, PRIMARY KEY(`taskId`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `reminder_deliveries` (`id` TEXT NOT NULL, `taskId` TEXT NOT NULL, `occurrenceId` TEXT NOT NULL, `revision` INTEGER NOT NULL, `originalAt` INTEGER NOT NULL, `alarmAt` INTEGER, `status` TEXT NOT NULL, `constant` INTEGER NOT NULL, `repeatCount` INTEGER NOT NULL, `deliveredAt` INTEGER, `missed` INTEGER NOT NULL, `dirty` INTEGER NOT NULL, `alertAgain` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_deliveries_taskId` ON `reminder_deliveries` (`taskId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_deliveries_alarmAt` ON `reminder_deliveries` (`alarmAt`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_reminder_deliveries_dirty` ON `reminder_deliveries` (`dirty`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `reminder_preferences` (`id` INTEGER NOT NULL, `resumeDate` TEXT, `legacyImported` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    }
}
