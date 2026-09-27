package com.niranjan.ticktick.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "task_lists", indices = [Index(value = ["nameKey"], unique = true)])
data class TaskListEntity(@PrimaryKey val id: String, val name: String, val nameKey: String,
    val symbol: String, val color: Int?, val view: String, val position: Int)

@Entity(tableName = "tasks", foreignKeys = [ForeignKey(entity = TaskListEntity::class,
    parentColumns = ["id"], childColumns = ["listId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("listId"), Index("dueDate"), Index(value = ["completed", "declined"])])
data class TaskEntity(@PrimaryKey val id: String, val title: String, val listId: String,
    val dueDate: String?, val dueTime: String?, val completed: Boolean, val description: String,
    val priority: String, val isNote: Boolean, val reminders: String, val repeat: String,
    val duration: String?, val declined: Boolean, val createdAt: String, val modifiedAt: String,
    val pinned: Boolean, val revision: Long, val position: Int,
    val zonePolicy: String = "device_local")

@Entity(tableName = "task_tags")
data class TaskTagEntity(@PrimaryKey val nameKey: String, val name: String, val color: Int?,
    val explicit: Boolean, val position: Int)

@Entity(tableName = "task_tag_links", primaryKeys = ["taskId", "tagKey"], foreignKeys = [
    ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE),
    ForeignKey(entity = TaskTagEntity::class, parentColumns = ["nameKey"], childColumns = ["tagKey"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("tagKey")])
data class TaskTagLinkEntity(val taskId: String, val tagKey: String, val displayName: String, val position: Int)

@Entity(tableName = "task_checklist", primaryKeys = ["taskId", "id"], foreignKeys = [
    ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE)])
data class ChecklistEntity(val taskId: String, val id: String, val text: String, val completed: Boolean, val position: Int)

@Entity(tableName = "task_attachments", primaryKeys = ["taskId", "id"], foreignKeys = [
    ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE)])
data class AttachmentEntity(val taskId: String, val id: String, val name: String, val uri: String,
    val mimeType: String, val sizeBytes: Long?, val position: Int)

@Entity(tableName = "task_snoozes", foreignKeys = [
    ForeignKey(entity = TaskEntity::class, parentColumns = ["id"], childColumns = ["taskId"], onDelete = ForeignKey.CASCADE)])
data class TaskSnoozeEntity(@PrimaryKey val taskId: String, val untilInstant: String)

@Entity(tableName = "saved_filters", indices = [Index(value = ["nameKey"], unique = true)])
data class SavedFilterEntity(@PrimaryKey val id: String, val name: String, val nameKey: String,
    val matchAll: Boolean, val position: Int)

@Entity(tableName = "filter_rules", primaryKeys = ["filterId", "position"], foreignKeys = [
    ForeignKey(entity = SavedFilterEntity::class, parentColumns = ["id"], childColumns = ["filterId"], onDelete = ForeignKey.CASCADE)])
data class FilterRuleEntity(val filterId: String, val position: Int, val field: String, val value: String)

@Entity(tableName = "task_store_metadata")
data class TaskStoreMetadata(@PrimaryKey val id: Int = 1, val initialized: Boolean = true)
