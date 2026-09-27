package com.niranjan.ticktick.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "task_recurrence", foreignKeys = [ForeignKey(entity = TaskEntity::class,
    parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE)])
data class TaskRecurrenceEntity(@PrimaryKey val taskId: String, val seriesRevision: String,
    val occurrenceId: String, val anchor: String)

@Entity(tableName = "task_occurrence_history", foreignKeys = [ForeignKey(entity = TaskEntity::class,
    parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE)], indices = [Index("taskId"), Index("date")])
data class TaskOccurrenceHistoryEntity(@PrimaryKey val id: String, val taskId: String, val seriesRevision: String,
    val date: String, val time: String?, val duration: String?, val title: String, val outcome: String,
    val recordedAt: String, val zoneId: String, val offsetSeconds: Int)

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `task_recurrence` (`taskId` TEXT NOT NULL, `seriesRevision` TEXT NOT NULL, `occurrenceId` TEXT NOT NULL, `anchor` TEXT NOT NULL, PRIMARY KEY(`taskId`), FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE TABLE IF NOT EXISTS `task_occurrence_history` (`id` TEXT NOT NULL, `taskId` TEXT NOT NULL, `seriesRevision` TEXT NOT NULL, `date` TEXT NOT NULL, `time` TEXT, `duration` TEXT, `title` TEXT NOT NULL, `outcome` TEXT NOT NULL, `recordedAt` TEXT NOT NULL, `zoneId` TEXT NOT NULL, `offsetSeconds` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`taskId`) REFERENCES `tasks`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_occurrence_history_taskId` ON `task_occurrence_history` (`taskId`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_task_occurrence_history_date` ON `task_occurrence_history` (`date`)")
    }
}
