package com.niranjan.ticktick.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "habits")
data class HabitEntity(@PrimaryKey val id: String, val configuration: String, val position: Int)
@Entity(tableName = "habit_schedule_history", primaryKeys = ["habitId", "effectiveFrom"], foreignKeys = [
    ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)])
data class HabitScheduleEntity(val habitId: String, val effectiveFrom: String, val configuration: String)
@Entity(tableName = "habit_checkins", primaryKeys = ["habitId", "date"], foreignKeys = [
    ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.CASCADE)])
data class HabitCheckInEntity(val habitId: String, val date: String, val quantity: Int, val updatedAt: String)
@Entity(tableName = "habit_sections")
data class HabitSectionEntity(@PrimaryKey val name: String, val position: Int)
@Entity(tableName = "habit_settings")
data class HabitSettingsEntity(@PrimaryKey val id: Int = 1, val ringtone: String, val ringtoneUri: String?,
    val sortByCheckInStatus: Boolean, val showInTodayAndNext7Days: Boolean, val countInAppBadge: Boolean)

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits ORDER BY position") suspend fun habits(): List<HabitEntity>
    @Query("SELECT * FROM habit_schedule_history ORDER BY effectiveFrom") suspend fun history(): List<HabitScheduleEntity>
    @Query("SELECT * FROM habit_checkins ORDER BY date") suspend fun checkIns(): List<HabitCheckInEntity>
    @Query("SELECT * FROM habit_sections ORDER BY position") suspend fun sections(): List<HabitSectionEntity>
    @Query("SELECT * FROM habit_settings WHERE id = 1") suspend fun settings(): HabitSettingsEntity?
    @Upsert suspend fun putHabits(rows: List<HabitEntity>)
    @Upsert suspend fun putHistory(rows: List<HabitScheduleEntity>)
    @Upsert suspend fun putCheckIns(rows: List<HabitCheckInEntity>)
    @Upsert suspend fun putSections(rows: List<HabitSectionEntity>)
    @Upsert suspend fun putSettings(row: HabitSettingsEntity)
    @Query("DELETE FROM habits WHERE id IN (:ids)") suspend fun deleteHabits(ids: List<String>)
    @Query("DELETE FROM habit_checkins WHERE habitId = :id AND date = :date") suspend fun deleteCheckIn(id: String, date: String)
    @Query("DELETE FROM habit_schedule_history WHERE habitId = :id AND effectiveFrom = :date") suspend fun deleteHistory(id: String, date: String)
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `habits` (`id` TEXT NOT NULL, `configuration` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `habit_schedule_history` (`habitId` TEXT NOT NULL, `effectiveFrom` TEXT NOT NULL, `configuration` TEXT NOT NULL, PRIMARY KEY(`habitId`, `effectiveFrom`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE TABLE IF NOT EXISTS `habit_checkins` (`habitId` TEXT NOT NULL, `date` TEXT NOT NULL, `quantity` INTEGER NOT NULL, `updatedAt` TEXT NOT NULL, PRIMARY KEY(`habitId`, `date`), FOREIGN KEY(`habitId`) REFERENCES `habits`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )")
        db.execSQL("CREATE TABLE IF NOT EXISTS `habit_sections` (`name` TEXT NOT NULL, `position` INTEGER NOT NULL, PRIMARY KEY(`name`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `habit_settings` (`id` INTEGER NOT NULL, `ringtone` TEXT NOT NULL, `ringtoneUri` TEXT, `sortByCheckInStatus` INTEGER NOT NULL, `showInTodayAndNext7Days` INTEGER NOT NULL, `countInAppBadge` INTEGER NOT NULL, PRIMARY KEY(`id`))")
    }
}
