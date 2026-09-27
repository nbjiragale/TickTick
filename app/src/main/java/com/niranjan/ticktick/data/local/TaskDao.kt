package com.niranjan.ticktick.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

/** Aggregate reads/writes are enclosed in a database transaction by the repository. */
@Dao
interface TaskDao {
    @Query("SELECT * FROM task_lists ORDER BY position") suspend fun lists(): List<TaskListEntity>
    @Query("SELECT * FROM tasks ORDER BY position") suspend fun tasks(): List<TaskEntity>
    @Query("SELECT * FROM task_tags ORDER BY position") suspend fun tags(): List<TaskTagEntity>
    @Query("SELECT * FROM task_tag_links ORDER BY position") suspend fun links(): List<TaskTagLinkEntity>
    @Query("SELECT * FROM task_checklist ORDER BY position") suspend fun checklist(): List<ChecklistEntity>
    @Query("SELECT * FROM task_attachments ORDER BY position") suspend fun attachments(): List<AttachmentEntity>
    @Query("SELECT * FROM task_snoozes") suspend fun snoozes(): List<TaskSnoozeEntity>
    @Query("SELECT * FROM saved_filters ORDER BY position") suspend fun filters(): List<SavedFilterEntity>
    @Query("SELECT * FROM filter_rules ORDER BY position") suspend fun rules(): List<FilterRuleEntity>
    @Query("SELECT * FROM task_store_metadata WHERE id = 1") suspend fun metadata(): TaskStoreMetadata?

    @Query("SELECT * FROM task_recurrence") suspend fun recurrences(): List<TaskRecurrenceEntity>
    @Query("SELECT * FROM task_occurrence_history ORDER BY recordedAt, id") suspend fun history(): List<TaskOccurrenceHistoryEntity>
    @Upsert suspend fun putRecurrences(rows: List<TaskRecurrenceEntity>)
    @Upsert suspend fun putHistory(rows: List<TaskOccurrenceHistoryEntity>)
    @Query("DELETE FROM task_recurrence WHERE taskId IN (:ids)") suspend fun deleteRecurrences(ids: List<String>)

    @Upsert suspend fun putLists(rows: List<TaskListEntity>)
    @Upsert suspend fun putTasks(rows: List<TaskEntity>)
    @Upsert suspend fun putTags(rows: List<TaskTagEntity>)
    @Upsert suspend fun putLinks(rows: List<TaskTagLinkEntity>)
    @Upsert suspend fun putChecklist(rows: List<ChecklistEntity>)
    @Upsert suspend fun putAttachments(rows: List<AttachmentEntity>)
    @Upsert suspend fun putSnoozes(rows: List<TaskSnoozeEntity>)
    @Upsert suspend fun putFilters(rows: List<SavedFilterEntity>)
    @Upsert suspend fun putRules(rows: List<FilterRuleEntity>)
    @Upsert suspend fun putMetadata(row: TaskStoreMetadata)

    @Query("DELETE FROM tasks WHERE id IN (:ids)") suspend fun deleteTasks(ids: List<String>)
    @Query("DELETE FROM task_tag_links WHERE taskId = :id") suspend fun clearLinks(id: String)
    @Query("DELETE FROM task_checklist WHERE taskId = :id") suspend fun clearChecklist(id: String)
    @Query("DELETE FROM task_attachments WHERE taskId = :id") suspend fun clearAttachments(id: String)
    @Query("DELETE FROM task_snoozes") suspend fun clearSnoozes()
    @Query("DELETE FROM filter_rules WHERE filterId = :id") suspend fun clearRules(id: String)
    @Query("DELETE FROM task_tags WHERE explicit = 0 AND nameKey NOT IN (SELECT tagKey FROM task_tag_links)")
    suspend fun deleteUnusedTags()
}
