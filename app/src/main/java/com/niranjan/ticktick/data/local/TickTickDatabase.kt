package com.niranjan.ticktick.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [TaskListEntity::class, TaskEntity::class, TaskTagEntity::class,
    TaskTagLinkEntity::class, ChecklistEntity::class, AttachmentEntity::class, TaskSnoozeEntity::class,
    SavedFilterEntity::class, FilterRuleEntity::class, TaskStoreMetadata::class,
    ReminderOccurrenceEntity::class, ReminderDeliveryEntity::class, ReminderPreferencesEntity::class, TaskRecurrenceEntity::class, TaskOccurrenceHistoryEntity::class, HabitEntity::class, HabitScheduleEntity::class, HabitCheckInEntity::class,
    HabitSectionEntity::class, HabitSettingsEntity::class, UiPreferenceEntity::class, EditorDraftEntity::class, FocusSessionEntity::class, FocusHistoryEntity::class], version = 6, exportSchema = true)
abstract class TickTickDatabase : RoomDatabase() {
    val operationLock = kotlinx.coroutines.sync.Mutex()
    val restoring = kotlinx.coroutines.flow.MutableStateFlow(false)
    @Volatile var generation: Long = 0
    suspend fun <T> writeLocked(action: suspend () -> T): T {
        val expected = generation
        operationLock.lock()
        try {
            check(!restoring.value && generation == expected) { "Saved data is being restored. Reopen this screen and retry." }
            return action()
        } finally { operationLock.unlock() }
    }
    abstract fun focus(): FocusDao
    abstract fun uiState(): UiStateDao
    abstract fun habits(): HabitDao
    abstract fun tasks(): TaskDao
    abstract fun reminders(): ReminderDao

    companion object {
        fun open(context: Context): TickTickDatabase = Room.databaseBuilder(
            context.applicationContext, TickTickDatabase::class.java, "ticktick.db",
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6).build()
    }
}
