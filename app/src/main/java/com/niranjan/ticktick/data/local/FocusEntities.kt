package com.niranjan.ticktick.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "focus_session")
data class FocusSessionEntity(@PrimaryKey val id: Int = 1, val payload: String)
// Task identity is historical metadata. Deleting a task must not delete focus history.
@Entity(tableName = "focus_history")
data class FocusHistoryEntity(@PrimaryKey val id: String, val mode: String, val elapsedMs: Long,
    val startedAt: Long, val endedAt: Long, val note: String, val taskId: String?, val completed: Boolean)
@Dao
interface FocusDao {
    @Query("SELECT * FROM focus_session WHERE id = 1") suspend fun session(): FocusSessionEntity?
    @Upsert suspend fun putSession(row: FocusSessionEntity)
    @Query("SELECT * FROM focus_history ORDER BY endedAt DESC") suspend fun history(): List<FocusHistoryEntity>
    @Upsert suspend fun putHistory(row: FocusHistoryEntity)
}
val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `focus_session` (`id` INTEGER NOT NULL, `payload` TEXT NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `focus_history` (`id` TEXT NOT NULL, `mode` TEXT NOT NULL, `elapsedMs` INTEGER NOT NULL, `startedAt` INTEGER NOT NULL, `endedAt` INTEGER NOT NULL, `note` TEXT NOT NULL, `taskId` TEXT, `completed` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    }
}
