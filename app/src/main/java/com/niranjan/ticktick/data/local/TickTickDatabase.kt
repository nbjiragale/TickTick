package com.niranjan.ticktick.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TaskListEntity::class, TaskEntity::class, TaskTagEntity::class,
    TaskTagLinkEntity::class, ChecklistEntity::class, AttachmentEntity::class, TaskSnoozeEntity::class,
    SavedFilterEntity::class, FilterRuleEntity::class, TaskStoreMetadata::class,
    ReminderOccurrenceEntity::class, ReminderDeliveryEntity::class, ReminderPreferencesEntity::class, TaskRecurrenceEntity::class, TaskOccurrenceHistoryEntity::class, HabitEntity::class, HabitScheduleEntity::class, HabitCheckInEntity::class,
    HabitSectionEntity::class, HabitSettingsEntity::class, UiPreferenceEntity::class, EditorDraftEntity::class], version = 5, exportSchema = true)
abstract class TickTickDatabase : RoomDatabase() {
    abstract fun uiState(): UiStateDao
    abstract fun habits(): HabitDao
    abstract fun tasks(): TaskDao
    abstract fun reminders(): ReminderDao

    companion object {
        fun open(context: Context): TickTickDatabase = Room.databaseBuilder(
            context.applicationContext, TickTickDatabase::class.java, "ticktick.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).build()
    }
}
