package com.niranjan.ticktick.data.local

import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Entity(tableName = "ui_preferences")
data class UiPreferenceEntity(@PrimaryKey val key: String, val format: Int = 1, val payload: String)
@Entity(tableName = "editor_drafts")
data class EditorDraftEntity(@PrimaryKey val key: String, val sessionId: String, val version: Long,
    val ownerId: String, val baseRevision: Long, val format: Int = 1, val payload: String?)

@Dao
interface UiStateDao {
    @Query("SELECT * FROM ui_preferences") suspend fun preferences(): List<UiPreferenceEntity>
    @Query("SELECT * FROM ui_preferences WHERE `key` = :key") suspend fun preference(key: String): UiPreferenceEntity?
    @Upsert suspend fun putPreference(row: UiPreferenceEntity)
    @Query("SELECT * FROM editor_drafts") suspend fun drafts(): List<EditorDraftEntity>
    @Query("SELECT * FROM editor_drafts WHERE `key` = :key") suspend fun draft(key: String): EditorDraftEntity?
    @Upsert suspend fun putDraft(row: EditorDraftEntity)
    // Retain a tombstone so delayed writes cannot resurrect a saved/discarded version.
    @Query("UPDATE editor_drafts SET payload = NULL WHERE `key` = :key AND sessionId = :session AND version <= :version")
    suspend fun clearDraft(key: String, session: String, version: Long)
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `ui_preferences` (`key` TEXT NOT NULL, `format` INTEGER NOT NULL, `payload` TEXT NOT NULL, PRIMARY KEY(`key`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `editor_drafts` (`key` TEXT NOT NULL, `sessionId` TEXT NOT NULL, `version` INTEGER NOT NULL, `ownerId` TEXT NOT NULL, `baseRevision` INTEGER NOT NULL, `format` INTEGER NOT NULL, `payload` TEXT, PRIMARY KEY(`key`))")
    }
}
